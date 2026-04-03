package com.aihos.ui.render.immersive

import android.opengl.GLES30
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber

/**
 * ImmersiveDepthPass — Stereo composite + depth-based effects.
 *
 * Pipeline:
 *
 *   NORMAL MODE (immersive disabled):
 *     GeometryPass → Bloom → Composite → Screen
 *     (this pass is a no-op, just passes through)
 *
 *   IMMERSIVE MODE (enabled):
 *     GeometryPass renders TWICE:
 *       → Left Eye FBO (left view/proj matrices)
 *       → Right Eye FBO (right view/proj matrices)
 *     This pass then:
 *       1. Applies depth fog + DoF to each eye
 *       2. Applies god rays
 *       3. Blends left+right into stereo composite
 *       → Output to screen or to bloom input
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  Immersive Render Pipeline                                   │
 * │                                                              │
 * │  Scene ──┬──→ Left Eye FBO  ──→ DepthFog+DoF ──┐            │
 * │          │                                       │            │
 * │          └──→ Right Eye FBO ──→ DepthFog+DoF ──┤            │
 * │                                                  │            │
 * │                                    Stereo Blend ◄┘            │
 * │                                        │                      │
 * │                                  God Rays (optional)          │
 * │                                        │                      │
 * │                                      Screen                   │
 * └──────────────────────────────────────────────────────────────┘
 *
 * FBO reuse strategy:
 *   - Left/right eye FBOs created at stereoResolutionScale
 *   - Depth fog uses ping-pong with existing FBOs
 *   - All FBOs released when immersive mode disabled
 */
class ImmersiveDepthPass(
    private val config: ImmersiveDepthConfig,
    private val leftEyeTextureProvider: () -> Int,
    private val rightEyeTextureProvider: () -> Int,
    private val leftDepthTextureProvider: (() -> Int)? = null,
    private val sceneTextureProvider: () -> Int
) : RenderPass {

    override val name = "ImmersiveDepthPass"

    // Shaders
    private var stereoBlendProgram: ShaderProgram? = null
    private var depthFogProgram: ShaderProgram? = null
    private var godRayProgram: ShaderProgram? = null

    // Full-screen quad
    private var fsQuad: MeshGenerator.MeshData? = null

    // Intermediate FBO for depth fog processing
    private var intermediateFbo = 0
    private var intermediateTex = 0
    private var intermediateWidth = 1
    private var intermediateHeight = 1

    private var screenWidth = 1
    private var screenHeight = 1

    // Stereo mode: 0=side-by-side, 1=anaglyph, 2=subtle blend (default)
    var stereoMode: Int = 2

    // Performance auto-disable tracking
    private var stereoAutoDisabled = false
    private var dofAutoDisabled = false

    override fun initialize() {
        stereoBlendProgram = ShaderProgram(
            ImmersiveShaders.FULLSCREEN_VERTEX,
            ImmersiveShaders.STEREO_BLEND_FRAGMENT
        )
        depthFogProgram = ShaderProgram(
            ImmersiveShaders.FULLSCREEN_VERTEX,
            ImmersiveShaders.DEPTH_FOG_DOF_FRAGMENT
        )
        godRayProgram = ShaderProgram(
            ImmersiveShaders.FULLSCREEN_VERTEX,
            ImmersiveShaders.GOD_RAY_FRAGMENT
        )
        fsQuad = MeshGenerator.generateFullScreenQuad()
        Timber.d("$name: initialized (3 shaders)")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        screenWidth = width
        screenHeight = height

        // Intermediate FBO at stereo resolution scale
        val scale = config.stereoResolutionScale * qualityLevel.resolutionScale
        intermediateWidth = (width * scale).toInt().coerceAtLeast(1)
        intermediateHeight = (height * scale).toInt().coerceAtLeast(1)

        releaseIntermediateFbo()
        createIntermediateFbo(intermediateWidth, intermediateHeight)

        Timber.d("$name: resized intermediate=${intermediateWidth}x${intermediateHeight}")
    }

    override fun execute(state: RenderState) {
        if (!config.enabled) return // No-op in normal mode

        // Performance auto-scaling
        performanceGating(state)

        val quad = fsQuad ?: return

        // Step 1: Stereo blend (left + right → screen)
        if (config.stereoEnabled && !stereoAutoDisabled) {
            executeStereoBlend(state, quad)
        }

        // Step 2: God rays (if enabled and FPS allows)
        if (config.godRaysEnabled && !stereoAutoDisabled) {
            executeGodRays(state, quad)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun executeStereoBlend(state: RenderState, quad: MeshGenerator.MeshData) {
        val prog = stereoBlendProgram ?: return

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        prog.use()

        // Left eye
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, leftEyeTextureProvider())
        prog.setInt("uLeftEyeTexture", 0)

        // Right eye
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, rightEyeTextureProvider())
        prog.setInt("uRightEyeTexture", 1)

        prog.setFloat("uStereoIntensity", config.stereoIntensity)
        prog.setInt("uStereoMode", stereoMode)
        prog.setVec2("uTexelSize", 1f / screenWidth, 1f / screenHeight)

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun executeGodRays(state: RenderState, quad: MeshGenerator.MeshData) {
        val prog = godRayProgram ?: return

        // Render god rays as a blend on top of current framebuffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, intermediateFbo)
        GLES30.glViewport(0, 0, intermediateWidth, intermediateHeight)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        prog.use()

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTextureProvider())
        prog.setInt("uSceneTexture", 0)

        // Light position in screen space (center = origin of AI core)
        prog.setVec2("uLightScreenPos", 0.5f, 0.5f)
        prog.setFloat("uIntensity", config.godRayIntensity)
        prog.setFloat("uDecay", config.godRayDecay)
        prog.setFloat("uDensity", 0.5f)
        prog.setInt("uNumSamples", config.godRaySamples)

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * Auto-disable expensive features when FPS drops.
     */
    private fun performanceGating(state: RenderState) {
        // This is called every frame but actual check uses quality level
        when (state.qualityLevel) {
            QualityLevel.LOW -> {
                if (!stereoAutoDisabled) {
                    stereoAutoDisabled = true
                    Timber.i("$name: auto-disabled stereo (LOW quality)")
                }
                if (!dofAutoDisabled) {
                    dofAutoDisabled = true
                    Timber.i("$name: auto-disabled DoF (LOW quality)")
                }
            }
            QualityLevel.MEDIUM -> {
                stereoAutoDisabled = false
                if (!dofAutoDisabled) {
                    dofAutoDisabled = true
                    Timber.i("$name: auto-disabled DoF (MEDIUM quality)")
                }
            }
            QualityLevel.HIGH -> {
                stereoAutoDisabled = false
                dofAutoDisabled = false
            }
        }
    }

    /** Whether stereo is currently active (config enabled + not auto-disabled). */
    val isStereoActive: Boolean get() = config.enabled && config.stereoEnabled && !stereoAutoDisabled

    private fun createIntermediateFbo(w: Int, h: Int) {
        val fbos = IntArray(1); GLES30.glGenFramebuffers(1, fbos, 0)
        val texs = IntArray(1); GLES30.glGenTextures(1, texs, 0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texs[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, texs[0], 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        intermediateFbo = fbos[0]
        intermediateTex = texs[0]
    }

    private fun releaseIntermediateFbo() {
        if (intermediateFbo != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(intermediateFbo), 0); intermediateFbo = 0 }
        if (intermediateTex != 0) { GLES30.glDeleteTextures(1, intArrayOf(intermediateTex), 0); intermediateTex = 0 }
    }

    override fun release() {
        releaseIntermediateFbo()
        stereoBlendProgram?.release(); stereoBlendProgram = null
        depthFogProgram?.release(); depthFogProgram = null
        godRayProgram?.release(); godRayProgram = null
        fsQuad?.release(); fsQuad = null
        Timber.d("$name: released")
    }
}

