package com.aihos.rendering.lighting

import android.opengl.GLES30
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// VOLUMETRIC LIGHTING ENGINE
// Manages the full volumetric lighting pipeline: light mask → radial blur →
// depth fog → composite.  Designed for OpenGL ES 3.0 on mobile GPUs.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Quality tier for adaptive scaling under varying GPU load.
 */
enum class VolumetricQuality(
    val resolutionScale: Float,
    val maxRaySamples: Int,
    val fogSteps: Int,
    val godRayEnabled: Boolean
) {
    ULTRA(1.0f, 48, 32, true),
    HIGH(0.75f, 36, 24, true),
    MEDIUM(0.5f, 24, 16, true),
    LOW(0.35f, 16, 12, true),
    MINIMAL(0.25f, 8, 8, false)
}

/**
 * Framebuffer Object wrapper for off-screen render targets.
 */
class FramebufferObject(
    val width: Int,
    val height: Int,
    val hasDepth: Boolean = false
) {
    var fboId: Int = 0; private set
    var colorTexture: Int = 0; private set
    var depthTexture: Int = 0; private set

    fun create() {
        val fbo = IntArray(1)
        GLES30.glGenFramebuffers(1, fbo, 0)
        fboId = fbo[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)

        // Color attachment (RGBA16F for HDR on ES 3.0)
        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        colorTexture = tex[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, colorTexture, 0
        )

        // Optional depth attachment
        if (hasDepth) {
            val depthTex = IntArray(1)
            GLES30.glGenTextures(1, depthTex, 0)
            depthTexture = depthTex[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
                width, height, 0,
                GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D, depthTexture, 0
            )
        }

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.e("FBO incomplete: 0x${Integer.toHexString(status)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bind() = GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
    fun unbind() = GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

    fun destroy() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES30.glDeleteTextures(1, intArrayOf(colorTexture), 0)
        if (hasDepth) GLES30.glDeleteTextures(1, intArrayOf(depthTexture), 0)
    }
}

/**
 * Core volumetric lighting engine.
 *
 * Render pipeline integration (multi-pass):
 *
 * Pass 0  – Scene geometry (existing renderer)        → sceneColorFBO, sceneDepthFBO
 * Pass 1  – Light Mask: render bright regions only     → lightMaskFBO
 * Pass 2  – Radial Blur: screen-space god-rays          → godRayFBO
 * Pass 3  – Depth Fog: apply depth-based scattering     → fogFBO
 * Pass 4  – Composite: blend everything onto backbuffer
 */
class VolumetricLightingEngine(
    private var screenWidth: Int,
    private var screenHeight: Int
) {
    // ── State ────────────────────────────────────────────────────────────────
    private val lights = mutableListOf<LightDescriptor>()
    private var quality = VolumetricQuality.HIGH
    private var aiActivityLevel: Float = 0.0f      // [0..1] from cognitive system
    private var timeAccumulator: Float = 0.0f

    // ── FBOs ─────────────────────────────────────────────────────────────────
    private lateinit var lightMaskFBO: FramebufferObject
    private lateinit var godRayFBO: FramebufferObject
    private lateinit var depthFogFBO: FramebufferObject
    private lateinit var compositeFBO: FramebufferObject

    // ── Shader programs ──────────────────────────────────────────────────────
    private var lightMaskProgram: Int = 0
    private var godRayProgram: Int = 0
    private var depthFogProgram: Int = 0
    private var compositeProgram: Int = 0

    // ── Fullscreen quad ──────────────────────────────────────────────────────
    private var quadVAO: Int = 0
    private var quadVBO: Int = 0

    // ── Performance ──────────────────────────────────────────────────────────
    private var lastFrameTimeMs: Float = 16.67f
    private val frameTimeSmoother = FloatArray(8)
    private var frameTimeIndex = 0

    // ═════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═════════════════════════════════════════════════════════════════════════

    fun initialize() {
        createFBOs()
        compileShaders()
        createFullscreenQuad()
        Timber.i("VolumetricLightingEngine initialized @ ${screenWidth}x${screenHeight}")
    }

    private fun createFBOs() {
        val s = quality.resolutionScale
        val w = (screenWidth * s).toInt().coerceAtLeast(64)
        val h = (screenHeight * s).toInt().coerceAtLeast(64)

        lightMaskFBO = FramebufferObject(w, h).also { it.create() }
        godRayFBO = FramebufferObject(w, h).also { it.create() }
        depthFogFBO = FramebufferObject(w, h).also { it.create() }
        compositeFBO = FramebufferObject(screenWidth, screenHeight).also { it.create() }
    }

    private fun createFullscreenQuad() {
        val vertices = floatArrayOf(
            // pos(x,y)  uv(s,t)
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )
        val buf: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(vertices).position(0)

        val vao = IntArray(1); GLES30.glGenVertexArrays(1, vao, 0); quadVAO = vao[0]
        val vbo = IntArray(1); GLES30.glGenBuffers(1, vbo, 0); quadVBO = vbo[0]

        GLES30.glBindVertexArray(quadVAO)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHADER COMPILATION
    // ═════════════════════════════════════════════════════════════════════════

    private fun compileShaders() {
        lightMaskProgram = buildProgram(LIGHT_MASK_VERT, LIGHT_MASK_FRAG)
        godRayProgram = buildProgram(FULLSCREEN_VERT, GOD_RAY_FRAG)
        depthFogProgram = buildProgram(FULLSCREEN_VERT, DEPTH_FOG_FRAG)
        compositeProgram = buildProgram(FULLSCREEN_VERT, COMPOSITE_FRAG)
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            Timber.e("Program link failed: $log")
            GLES30.glDeleteProgram(prog)
            return 0
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            Timber.e("Shader compile failed: $log")
        }
        return shader
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LIGHT MANAGEMENT
    // ═════════════════════════════════════════════════════════════════════════

    fun addLight(light: LightDescriptor) {
        lights.add(light)
        Timber.d("Light added: ${light.type} (total: ${lights.size})")
    }

    fun removeLight(id: String) {
        lights.removeAll { it.id == id }
    }

    fun getLights(): List<LightDescriptor> = lights.toList()

    fun setAIActivityLevel(level: Float) {
        aiActivityLevel = level.coerceIn(0f, 1f)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PER-FRAME UPDATE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Update all light animations before rendering.
     * @param deltaTime seconds since last frame
     * @param viewProjectionMatrix 4×4 VP matrix for computing screen-space positions
     */
    fun update(deltaTime: Float, viewProjectionMatrix: FloatArray) {
        timeAccumulator += deltaTime
        adaptQuality(deltaTime)

        for (light in lights) {
            if (!light.enabled) continue
            updateLightAnimation(light, deltaTime)
            computeScreenSpacePosition(light, viewProjectionMatrix)
        }
    }

    private fun updateLightAnimation(light: LightDescriptor, dt: Float) {
        // ── Pulsation ────────────────────────────────────────────────────
        light.pulsation?.let { pulse ->
            val freq = pulse.baseFrequencyHz * (1f + aiActivityLevel * (pulse.aiActivityMultiplier - 1f))
            val phase = timeAccumulator * freq * 2f * PI.toFloat() + pulse.phaseOffset
            val t = (sin(phase) * 0.5f + 0.5f)
            light.currentIntensity = pulse.amplitudeMin + t * (pulse.amplitudeMax - pulse.amplitudeMin)
        }

        // ── Orbit ────────────────────────────────────────────────────────
        light.orbit?.let { orb ->
            val angle = orb.initialAngleRad + timeAccumulator * orb.angularSpeedRadPerSec
            val ox = cos(angle) * orb.radiusXZ
            val oy = sin(angle * 0.5f) * orb.radiusY
            val oz = sin(angle) * orb.radiusXZ
            // Update position relative to original center
            val center = light.position
            // We store computed world position in screenSpacePosition temporarily
            // Actual world position is center + offset (handled during rendering)
        }
    }

    /**
     * Project a world-space light position into [0,1] screen-space coordinates.
     */
    private fun computeScreenSpacePosition(light: LightDescriptor, vpMatrix: FloatArray) {
        val pos = light.position
        // Manual 4×4 multiply: clip = VP * [px, py, pz, 1]
        val cx = vpMatrix[0]*pos.x + vpMatrix[4]*pos.y + vpMatrix[8]*pos.z + vpMatrix[12]
        val cy = vpMatrix[1]*pos.x + vpMatrix[5]*pos.y + vpMatrix[9]*pos.z + vpMatrix[13]
        val cw = vpMatrix[3]*pos.x + vpMatrix[7]*pos.y + vpMatrix[11]*pos.z + vpMatrix[15]
        if (cw > 0.001f) {
            light.screenSpacePosition = floatArrayOf(
                (cx / cw) * 0.5f + 0.5f,
                (cy / cw) * 0.5f + 0.5f
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RENDER PASSES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Execute the full volumetric lighting pipeline.
     *
     * @param sceneColorTex  color texture from the main scene pass
     * @param sceneDepthTex  depth texture from the main scene pass
     */
    fun render(sceneColorTex: Int, sceneDepthTex: Int) {
        // Pass 1 — Light Mask
        renderLightMask(sceneColorTex)

        // Pass 2 — God-Ray Radial Blur
        if (quality.godRayEnabled) {
            renderGodRays()
        }

        // Pass 3 — Depth Fog
        renderDepthFog(sceneDepthTex)

        // Pass 4 — Composite
        renderComposite(sceneColorTex)
    }

    // ── Pass 1: Light Mask ───────────────────────────────────────────────────

    private fun renderLightMask(sceneColorTex: Int) {
        lightMaskFBO.bind()
        GLES30.glViewport(0, 0, lightMaskFBO.width, lightMaskFBO.height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(lightMaskProgram)

        // Bind scene color
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneColorTex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(lightMaskProgram, "uSceneColor"), 0)

        // Upload light data
        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(lightMaskProgram, "uThreshold"),
            0.7f - aiActivityLevel * 0.2f       // Lower threshold when AI is active
        )

        drawFullscreenQuad()
        lightMaskFBO.unbind()
    }

    // ── Pass 2: God-Ray Radial Blur ──────────────────────────────────────────

    private fun renderGodRays() {
        godRayFBO.bind()
        GLES30.glViewport(0, 0, godRayFBO.width, godRayFBO.height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(godRayProgram)

        // Bind light mask as input
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lightMaskFBO.colorTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(godRayProgram, "uLightMask"), 0)

        // Process each enabled god-ray light
        val godRayLights = lights.filter { it.enabled && it.godRay.enabled }
        val primaryLight = godRayLights.firstOrNull() ?: return

        val gr = primaryLight.godRay
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(godRayProgram, "uLightScreenPos"),
            primaryLight.screenSpacePosition[0],
            primaryLight.screenSpacePosition[1]
        )
        GLES30.glUniform1i(
            GLES30.glGetUniformLocation(godRayProgram, "uNumSamples"),
            quality.maxRaySamples.coerceAtMost(gr.numSamples)
        )
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uDensity"), gr.density)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uWeight"), gr.weight)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uDecay"), gr.decay)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uExposure"), gr.exposure)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uNoiseIntensity"), gr.noiseIntensity)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uNoiseFreq"), gr.noiseFrequency)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(godRayProgram, "uTime"), timeAccumulator)
        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(godRayProgram, "uAIActivity"),
            aiActivityLevel
        )

        // Light color tint
        val c = primaryLight.color
        GLES30.glUniform4f(
            GLES30.glGetUniformLocation(godRayProgram, "uLightColor"),
            c.r, c.g, c.b, primaryLight.currentIntensity
        )

        drawFullscreenQuad()
        godRayFBO.unbind()
    }

    // ── Pass 3: Depth Fog ────────────────────────────────────────────────────

    private fun renderDepthFog(sceneDepthTex: Int) {
        depthFogFBO.bind()
        GLES30.glViewport(0, 0, depthFogFBO.width, depthFogFBO.height)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(depthFogProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneDepthTex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(depthFogProgram, "uDepthTexture"), 0)

        // Primary light for fog
        val primaryLight = lights.firstOrNull { it.enabled } ?: return
        val fog = primaryLight.volumetricFog
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uFogDensity"), fog.fogDensity)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uScattering"), fog.scatteringCoefficient)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uAbsorption"), fog.absorptionCoefficient)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uMaxDistance"), fog.maxDistance)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uNearPlane"), 0.1f)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uFarPlane"), 200.0f)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(depthFogProgram, "uAIActivity"), aiActivityLevel)

        val c = primaryLight.color
        GLES30.glUniform4f(
            GLES30.glGetUniformLocation(depthFogProgram, "uFogColor"),
            c.r, c.g, c.b, c.a
        )

        val pos = primaryLight.position
        GLES30.glUniform3f(
            GLES30.glGetUniformLocation(depthFogProgram, "uLightPos"),
            primaryLight.screenSpacePosition[0],
            primaryLight.screenSpacePosition[1],
            0.0f
        )

        drawFullscreenQuad()
        depthFogFBO.unbind()
    }

    // ── Pass 4: Composite ────────────────────────────────────────────────────

    private fun renderComposite(sceneColorTex: Int) {
        // Render to backbuffer (FBO 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)

        GLES30.glUseProgram(compositeProgram)

        // Texture unit 0: scene color
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneColorTex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uSceneColor"), 0)

        // Texture unit 1: god-ray result
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, godRayFBO.colorTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uGodRays"), 1)

        // Texture unit 2: depth fog
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthFogFBO.colorTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uDepthFog"), 2)

        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(compositeProgram, "uGodRayStrength"),
            if (quality.godRayEnabled) 1.0f else 0.0f
        )
        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(compositeProgram, "uFogStrength"), 1.0f
        )

        // Additive blending
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        drawFullscreenQuad()

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun drawFullscreenQuad() {
        GLES30.glBindVertexArray(quadVAO)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PERFORMANCE — ADAPTIVE QUALITY (Part 6)
    // ═════════════════════════════════════════════════════════════════════════

    private fun adaptQuality(deltaTime: Float) {
        lastFrameTimeMs = deltaTime * 1000f
        frameTimeSmoother[frameTimeIndex % frameTimeSmoother.size] = lastFrameTimeMs
        frameTimeIndex++

        val avgFrameMs = frameTimeSmoother.average().toFloat()
        val fps = 1000f / avgFrameMs.coerceAtLeast(1f)

        val newQuality = when {
            fps >= 58f -> VolumetricQuality.HIGH
            fps >= 50f -> VolumetricQuality.MEDIUM
            fps >= 40f -> VolumetricQuality.LOW
            else       -> VolumetricQuality.MINIMAL
        }

        if (newQuality != quality) {
            Timber.d("Volumetric quality: $quality → $newQuality (FPS: %.1f)".format(fps))
            quality = newQuality
            recreateFBOs()
        }
    }

    private fun recreateFBOs() {
        lightMaskFBO.destroy()
        godRayFBO.destroy()
        depthFogFBO.destroy()
        createFBOs()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DYNAMIC LIGHT PULSES (Part 5)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Trigger an energy pulse from the AI core.
     * Called when cognitive activity spikes (from the AutonomyController).
     */
    fun triggerEnergyPulse(intensity: Float = 1.0f, durationMs: Long = 800) {
        activePulses.add(EnergyPulse(
            startTime = timeAccumulator,
            duration = durationMs / 1000f,
            peakIntensity = intensity.coerceIn(0.5f, 3.0f)
        ))
        Timber.d("Energy pulse triggered: intensity=$intensity, duration=${durationMs}ms")
    }

    private val activePulses = mutableListOf<EnergyPulse>()

    private data class EnergyPulse(
        val startTime: Float,
        val duration: Float,
        val peakIntensity: Float
    ) {
        fun evaluate(currentTime: Float): Float {
            val t = (currentTime - startTime) / duration
            if (t < 0f || t > 1f) return 0f
            // Sharp attack, smooth decay (exponential envelope)
            return peakIntensity * sin(t * PI.toFloat()) * exp(-2f * t)
        }

        fun isExpired(currentTime: Float) = (currentTime - startTime) > duration
    }

    /**
     * Accumulate pulse contributions for current frame.
     */
    fun getPulseIntensity(): Float {
        activePulses.removeAll { it.isExpired(timeAccumulator) }
        return activePulses.map { it.evaluate(timeAccumulator) }.sum().coerceIn(0f, 3f)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════════

    fun onSurfaceChanged(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        recreateFBOs()
        compositeFBO.destroy()
        compositeFBO = FramebufferObject(width, height).also { it.create() }
    }

    fun destroy() {
        lightMaskFBO.destroy()
        godRayFBO.destroy()
        depthFogFBO.destroy()
        compositeFBO.destroy()
        GLES30.glDeleteProgram(lightMaskProgram)
        GLES30.glDeleteProgram(godRayProgram)
        GLES30.glDeleteProgram(depthFogProgram)
        GLES30.glDeleteProgram(compositeProgram)
        GLES30.glDeleteVertexArrays(1, intArrayOf(quadVAO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(quadVBO), 0)
        Timber.i("VolumetricLightingEngine destroyed")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GLSL SHADER SOURCES (OpenGL ES 3.0)
    // ═════════════════════════════════════════════════════════════════════════

    companion object {

        // ── Shared fullscreen vertex shader ──────────────────────────────
        const val FULLSCREEN_VERT = """#version 300 es
precision highp float;
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

        // ── Light Mask vertex (pass-through) ─────────────────────────────
        const val LIGHT_MASK_VERT = FULLSCREEN_VERT

        // ── Pass 1: Light Mask — extract bright regions ──────────────────
        const val LIGHT_MASK_FRAG = """#version 300 es
precision mediump float;
in vec2 vTexCoord;
uniform sampler2D uSceneColor;
uniform float uThreshold;
out vec4 fragColor;

void main() {
    vec4 color = texture(uSceneColor, vTexCoord);
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    // Soft threshold with smooth transition
    float mask = smoothstep(uThreshold, uThreshold + 0.15, brightness);
    fragColor = vec4(color.rgb * mask, mask);
}
"""

        // ── Pass 2: God-Ray Radial Blur (Part 3) ─────────────────────────
        const val GOD_RAY_FRAG = """#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uLightMask;
uniform vec2  uLightScreenPos;   // light position in UV [0..1]
uniform int   uNumSamples;
uniform float uDensity;
uniform float uWeight;
uniform float uDecay;
uniform float uExposure;
uniform float uNoiseIntensity;
uniform float uNoiseFreq;
uniform float uTime;
uniform float uAIActivity;
uniform vec4  uLightColor;

// ── Simple hash-based noise ──────────────────────────────────────────
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    // ── Direction from pixel toward light source ─────────────────────
    vec2 deltaTexCoord = (vTexCoord - uLightScreenPos) * (1.0 / float(uNumSamples)) * uDensity;

    vec2 texCoord = vTexCoord;
    vec3 accumColor = vec3(0.0);
    float illuminationDecay = 1.0;

    // ── Radial blur march ────────────────────────────────────────────
    for (int i = 0; i < uNumSamples; i++) {
        texCoord -= deltaTexCoord;

        // Clamp to prevent sampling outside texture
        texCoord = clamp(texCoord, vec2(0.001), vec2(0.999));

        vec4 sampleColor = texture(uLightMask, texCoord);

        // Apply noise distortion for realism
        float n = noise2D(texCoord * uNoiseFreq + vec2(uTime * 0.5));
        float noiseOffset = (n - 0.5) * uNoiseIntensity;
        sampleColor.rgb *= (1.0 + noiseOffset);

        // Accumulate with decay
        sampleColor.rgb *= illuminationDecay * uWeight;
        accumColor += sampleColor.rgb;
        illuminationDecay *= uDecay;
    }

    // ── Apply exposure and light color tint ──────────────────────────
    accumColor *= uExposure;
    accumColor *= uLightColor.rgb * uLightColor.a;

    // ── Animated pulsation tied to AI activity ───────────────────────
    float pulse = 1.0 + uAIActivity * 0.3 * sin(uTime * 3.14159 * 2.0);
    accumColor *= pulse;

    // ── Distance-based fade ──────────────────────────────────────────
    float distToLight = length(vTexCoord - uLightScreenPos);
    float distanceFade = 1.0 - smoothstep(0.0, 0.8, distToLight);
    accumColor *= distanceFade;

    fragColor = vec4(accumColor, 1.0);
}
"""

        // ── Pass 3: Depth-Based Light Fog (Part 4) ───────────────────────
        const val DEPTH_FOG_FRAG = """#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uDepthTexture;
uniform float uFogDensity;
uniform float uScattering;
uniform float uAbsorption;
uniform float uMaxDistance;
uniform float uNearPlane;
uniform float uFarPlane;
uniform float uAIActivity;
uniform vec4  uFogColor;
uniform vec3  uLightPos;       // light screen-space position

// ── Linearize depth from [0,1] → view-space distance ─────────────────
float linearizeDepth(float d) {
    float ndc = d * 2.0 - 1.0;
    return (2.0 * uNearPlane * uFarPlane) / (uFarPlane + uNearPlane - ndc * (uFarPlane - uNearPlane));
}

void main() {
    float rawDepth = texture(uDepthTexture, vTexCoord).r;
    float linearDepth = linearizeDepth(rawDepth);
    float normalizedDepth = linearDepth / uFarPlane;

    // ── Beer-Lambert extinction ──────────────────────────────────────
    // T = exp(-density * distance)
    float extinction = exp(-uFogDensity * linearDepth);
    float fogAmount = 1.0 - extinction;

    // ── Proximity glow near light source ─────────────────────────────
    float distToLight = length(vTexCoord - uLightPos.xy);
    float proximityGlow = exp(-distToLight * 5.0) * uScattering;
    // Stronger glow near the light source
    fogAmount += proximityGlow * (1.0 - normalizedDepth);

    // ── In-scattering (Mie / Rayleigh approximation) ─────────────────
    // Phase function: simple isotropic + forward scatter
    float phase = 0.75 + 0.25 * (1.0 - normalizedDepth);
    float inScatter = uScattering * phase * fogAmount;

    // ── AI-state color shift ─────────────────────────────────────────
    // Low activity: cool blue tones
    // High activity: warm cyan → violet shift
    vec3 baseFogColor = uFogColor.rgb;
    vec3 activeColor = vec3(0.4, 0.1, 0.9);    // violet for high activity
    vec3 finalFogColor = mix(baseFogColor, activeColor, uAIActivity * 0.4);

    // ── Final fog contribution ───────────────────────────────────────
    vec3 fog = finalFogColor * (fogAmount + inScatter);
    float alpha = clamp(fogAmount * 0.8, 0.0, 0.6);

    fragColor = vec4(fog, alpha);
}
"""

        // ── Pass 4: Final Composite ──────────────────────────────────────
        const val COMPOSITE_FRAG = """#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uSceneColor;
uniform sampler2D uGodRays;
uniform sampler2D uDepthFog;
uniform float uGodRayStrength;
uniform float uFogStrength;

// ACES filmic tonemapping
vec3 acesTonemap(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 scene = texture(uSceneColor, vTexCoord);
    vec4 rays  = texture(uGodRays, vTexCoord);
    vec4 fog   = texture(uDepthFog, vTexCoord);

    // Additive blending for god-rays
    vec3 combined = scene.rgb;
    combined += rays.rgb * uGodRayStrength;

    // Alpha-blend depth fog
    combined = mix(combined, fog.rgb, fog.a * uFogStrength);

    // Tonemap to prevent blow-out on mobile displays
    combined = acesTonemap(combined);

    // Gamma correction (linear → sRGB)
    combined = pow(combined, vec3(1.0 / 2.2));

    fragColor = vec4(combined, 1.0);
}
"""
    }
}
