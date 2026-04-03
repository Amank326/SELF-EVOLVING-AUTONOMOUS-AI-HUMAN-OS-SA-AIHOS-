package com.aihos.ui.gl

import android.opengl.GLES30
import timber.log.Timber

/**
 * PostProcessingPipeline — FBO-based bloom, vignette, and color grading.
 *
 * Pipeline flow:
 *   1. Scene renders into [sceneFBO] (color + depth)
 *   2. Bright-pass extract → [brightFBO]
 *   3. Ping-pong Gaussian blur → [blurFBO_A] ↔ [blurFBO_B]
 *   4. Composite (scene + bloom + vignette + color grade) → screen
 *
 * Blur FBOs run at half resolution for performance.
 * Dynamic quality: blur iterations and FBO scale controlled by PerformanceMonitor.
 *
 * Zero allocations after [initialize]. All textures and FBOs are pre-created.
 */
class PostProcessingPipeline {

    // ── FBO handles ──────────────────────────────────────────────
    private var sceneFBO = 0
    private var sceneColorTex = 0
    private var sceneDepthRBO = 0

    private var brightFBO = 0
    private var brightColorTex = 0

    private var blurFBO_A = 0
    private var blurColorTex_A = 0

    private var blurFBO_B = 0
    private var blurColorTex_B = 0

    // ── Shader programs ──────────────────────────────────────────
    private var bloomExtractProgram: ShaderProgram? = null
    private var blurProgram: ShaderProgram? = null
    private var compositeProgram: ShaderProgram? = null
    private var backgroundProgram: ShaderProgram? = null

    // ── Full-screen quad ─────────────────────────────────────────
    private var fsQuad: MeshGenerator.MeshData? = null

    // ── Dimensions ───────────────────────────────────────────────
    private var screenWidth = 0
    private var screenHeight = 0
    private var blurWidth = 0
    private var blurHeight = 0

    // ── Tuning parameters ────────────────────────────────────────
    var bloomThreshold = 0.7f
    var bloomStrength = 0.4f
    var vignetteIntensity = 0.35f
    var exposure = 1.2f
    var gamma = 2.2f
    var saturation = 1.1f
    var blurScale = 1.5f

    private var isInitialized = false

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize the post-processing pipeline. Call from onSurfaceCreated.
     */
    fun initialize() {
        // Compile shader programs
        bloomExtractProgram = ShaderProgram(
            CinematicShaders.BLOOM_EXTRACT_VERTEX,
            CinematicShaders.BLOOM_EXTRACT_FRAGMENT
        )
        blurProgram = ShaderProgram(
            CinematicShaders.BLUR_VERTEX,
            CinematicShaders.BLUR_FRAGMENT
        )
        compositeProgram = ShaderProgram(
            CinematicShaders.COMPOSITE_VERTEX,
            CinematicShaders.COMPOSITE_FRAGMENT
        )
        backgroundProgram = ShaderProgram(
            CinematicShaders.BACKGROUND_VERTEX,
            CinematicShaders.BACKGROUND_FRAGMENT
        )

        // Generate full-screen quad
        fsQuad = MeshGenerator.generateFullScreenQuad()

        isInitialized = true
        Timber.d("PostProcessingPipeline: initialized")
    }

    /**
     * Create/recreate FBOs for the given screen size.
     * Call from onSurfaceChanged.
     *
     * @param resolutionScale from PerformanceMonitor (0.5 to 1.0)
     */
    fun resize(width: Int, height: Int, resolutionScale: Float = 1.0f) {
        // Release old FBOs
        releaseFBOs()

        screenWidth = width
        screenHeight = height

        // Scene FBO at scaled resolution
        val sceneW = (width * resolutionScale).toInt().coerceAtLeast(1)
        val sceneH = (height * resolutionScale).toInt().coerceAtLeast(1)

        // Blur FBOs at half the scene resolution
        blurWidth = (sceneW / 2).coerceAtLeast(1)
        blurHeight = (sceneH / 2).coerceAtLeast(1)

        // Create scene FBO (with depth)
        val scenePair = createFBOWithDepth(sceneW, sceneH)
        sceneFBO = scenePair.first
        sceneColorTex = scenePair.second
        sceneDepthRBO = scenePair.third

        // Create bright-pass FBO (half-res, no depth)
        val brightPair = createFBO(blurWidth, blurHeight)
        brightFBO = brightPair.first
        brightColorTex = brightPair.second

        // Create blur ping-pong FBOs (half-res)
        val blurAPair = createFBO(blurWidth, blurHeight)
        blurFBO_A = blurAPair.first
        blurColorTex_A = blurAPair.second

        val blurBPair = createFBO(blurWidth, blurHeight)
        blurFBO_B = blurBPair.first
        blurColorTex_B = blurBPair.second

        Timber.d("PostProcessingPipeline: resized scene=${sceneW}x${sceneH} blur=${blurWidth}x${blurHeight}")
    }

    // ═══════════════════════════════════════════════════════════════
    // Render passes
    // ═══════════════════════════════════════════════════════════════

    /**
     * Begin the scene render pass. Binds sceneFBO.
     * After this call, draw your 3D scene. Then call [executePostProcessing].
     */
    fun beginScenePass() {
        if (!isInitialized) return
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFBO)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    }

    /**
     * Draw the procedural background nebula. Call after [beginScenePass]
     * but before drawing 3D objects (so it's behind everything).
     */
    fun drawBackground(time: Float, cognitiveLoad: Float, confidence: Float, resX: Float, resY: Float) {
        val prog = backgroundProgram ?: return
        val quad = fsQuad ?: return

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        prog.use()
        prog.setFloat("uTime", time)
        prog.setFloat("uCognitiveLoad", cognitiveLoad)
        prog.setFloat("uConfidence", confidence)
        prog.setVec2("uResolution", resX, resY)

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * Execute bloom + composite to screen.
     *
     * @param bloomPasses number of blur iterations (0 = skip bloom)
     * @param time current elapsed time (for film grain)
     */
    fun executePostProcessing(bloomPasses: Int, time: Float) {
        if (!isInitialized) return

        val quad = fsQuad ?: return

        // Unbind scene FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        if (bloomPasses > 0) {
            // ── Pass 1: Bloom extract (scene → bright) ───────
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, brightFBO)
            GLES30.glViewport(0, 0, blurWidth, blurHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            val extractProg = bloomExtractProgram!!
            extractProg.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneColorTex)
            extractProg.setInt("uSceneTexture", 0)
            extractProg.setFloat("uThreshold", bloomThreshold)

            GLES30.glBindVertexArray(quad.vao)
            GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
            GLES30.glBindVertexArray(0)

            // ── Pass 2: Ping-pong Gaussian blur ──────────────
            val blurProg = blurProgram!!
            blurProg.use()
            blurProg.setFloat("uBlurScale", blurScale)

            var readTex = brightColorTex
            var writeFBO = blurFBO_A
            @Suppress("UNUSED_VALUE")
            var writeTex = blurColorTex_A

            for (_pass in 0 until bloomPasses) {
                // Horizontal pass
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, writeFBO)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, readTex)
                blurProg.setInt("uTexture", 0)
                blurProg.setVec2("uDirection", 1f / blurWidth, 0f)

                GLES30.glBindVertexArray(quad.vao)
                GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
                GLES30.glBindVertexArray(0)

                // Vertical pass (swap ping-pong)
                if (writeFBO == blurFBO_A) {
                    readTex = blurColorTex_A
                    writeFBO = blurFBO_B
                    writeTex = blurColorTex_B
                } else {
                    readTex = blurColorTex_B
                    writeFBO = blurFBO_A
                    writeTex = blurColorTex_A
                }

                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, writeFBO)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, readTex)
                blurProg.setVec2("uDirection", 0f, 1f / blurHeight)

                GLES30.glBindVertexArray(quad.vao)
                GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
                GLES30.glBindVertexArray(0)

                // For next iteration, read from what we just wrote
                readTex = writeTex
            }
        }

        // ── Pass 3: Composite to screen ──────────────────────
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        val compProg = compositeProgram!!
        compProg.use()

        // Bind scene texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneColorTex)
        compProg.setInt("uSceneTexture", 0)

        // Bind bloom texture (last written blur buffer)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        if (bloomPasses > 0) {
            // The last write alternated, so figure out which buffer has the final result
            val finalBloomTex = if (bloomPasses % 2 == 0) blurColorTex_B else blurColorTex_A
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, finalBloomTex)
        } else {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, brightColorTex) // empty/black
        }
        compProg.setInt("uBloomTexture", 1)

        // Composite uniforms
        compProg.setFloat("uBloomStrength", if (bloomPasses > 0) bloomStrength else 0f)
        compProg.setFloat("uVignetteIntensity", vignetteIntensity)
        compProg.setFloat("uExposure", exposure)
        compProg.setFloat("uGamma", gamma)
        compProg.setFloat("uSaturation", saturation)
        compProg.setFloat("uTime", time)

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    // ═══════════════════════════════════════════════════════════════
    // FBO creation helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create FBO with color attachment only (for blur / bright pass).
     * Returns (fboHandle, colorTexHandle).
     */
    private fun createFBO(width: Int, height: Int): Pair<Int, Int> {
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        val fbo = fbos[0]

        val texs = IntArray(1)
        GLES30.glGenTextures(1, texs, 0)
        val tex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, tex, 0
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.e("PostProcessing: FBO incomplete! status=0x${Integer.toHexString(status)}")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return Pair(fbo, tex)
    }

    /**
     * Create FBO with color + depth attachment (for scene pass).
     * Returns Triple(fboHandle, colorTexHandle, depthRBOHandle).
     */
    private fun createFBOWithDepth(width: Int, height: Int): Triple<Int, Int, Int> {
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        val fbo = fbos[0]

        // Color texture (RGBA16F for HDR)
        val texs = IntArray(1)
        GLES30.glGenTextures(1, texs, 0)
        val tex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            width, height, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Depth renderbuffer
        val rbos = IntArray(1)
        GLES30.glGenRenderbuffers(1, rbos, 0)
        val rbo = rbos[0]

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, width, height)

        // Assemble FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, tex, 0
        )
        GLES30.glFramebufferRenderbuffer(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_RENDERBUFFER, rbo
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.e("PostProcessing: Scene FBO incomplete! status=0x${Integer.toHexString(status)}")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return Triple(fbo, tex, rbo)
    }

    // ═══════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════

    private fun releaseFBOs() {
        val fbos = intArrayOf(sceneFBO, brightFBO, blurFBO_A, blurFBO_B)
        val texs = intArrayOf(sceneColorTex, brightColorTex, blurColorTex_A, blurColorTex_B)
        val rbos = intArrayOf(sceneDepthRBO)

        for (fbo in fbos) {
            if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        }
        for (tex in texs) {
            if (tex != 0) GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
        }
        for (rbo in rbos) {
            if (rbo != 0) GLES30.glDeleteRenderbuffers(1, intArrayOf(rbo), 0)
        }

        sceneFBO = 0; sceneColorTex = 0; sceneDepthRBO = 0
        brightFBO = 0; brightColorTex = 0
        blurFBO_A = 0; blurColorTex_A = 0
        blurFBO_B = 0; blurColorTex_B = 0
    }

    fun release() {
        releaseFBOs()
        bloomExtractProgram?.release()
        blurProgram?.release()
        compositeProgram?.release()
        backgroundProgram?.release()
        fsQuad?.release()

        bloomExtractProgram = null
        blurProgram = null
        compositeProgram = null
        backgroundProgram = null
        fsQuad = null
        isInitialized = false

        Timber.d("PostProcessingPipeline: released")
    }
}

