package com.aihos.ui.render.pass

import android.opengl.GLES30
import com.aihos.ui.gl.CinematicShaders
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.core.FBORenderPass
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderState
import timber.log.Timber

/**
 * BloomPass — Multi-pass bloom with bright extract + separable Gaussian blur.
 *
 * Pipeline position: AFTER GeometryPass
 * Input:  Scene HDR color texture (from GeometryPass)
 * Output: Blurred bloom texture
 *
 * Steps:
 *   1. Bright-pass extract (threshold luminance)
 *   2. Ping-pong Gaussian blur (separable H/V, half resolution)
 */
class BloomPass(
    private val sceneTextureProvider: () -> Int  // lambda returning geometry pass color tex
) : FBORenderPass() {

    override val name = "BloomPass"

    // Shaders
    private var extractProgram: ShaderProgram? = null
    private var blurProgram: ShaderProgram? = null

    // Full-screen quad
    private var fsQuad: MeshGenerator.MeshData? = null

    // Ping-pong FBOs (second pair beyond base class)
    private var brightFBO = 0
    private var brightTex = 0
    private var blurFBO_B = 0
    private var blurTex_B = 0
    private var blurWidth = 1
    private var blurHeight = 1

    // Tuning
    var threshold = 0.7f
    var blurScale = 1.5f

    override fun initialize() {
        extractProgram = ShaderProgram(CinematicShaders.BLOOM_EXTRACT_VERTEX, CinematicShaders.BLOOM_EXTRACT_FRAGMENT)
        blurProgram = ShaderProgram(CinematicShaders.BLUR_VERTEX, CinematicShaders.BLUR_FRAGMENT)
        fsQuad = MeshGenerator.generateFullScreenQuad()
        Timber.d("$name: initialized")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        releaseBloomFBOs()

        blurWidth = (width * qualityLevel.resolutionScale / 2f).toInt().coerceAtLeast(1)
        blurHeight = (height * qualityLevel.resolutionScale / 2f).toInt().coerceAtLeast(1)

        // Bright extract FBO
        val bPair = createColorFBO(blurWidth, blurHeight)
        brightFBO = bPair.first; brightTex = bPair.second

        // Blur ping-pong A (use base class FBO)
        createFBO(blurWidth, blurHeight, withDepth = false)

        // Blur ping-pong B
        val bBPair = createColorFBO(blurWidth, blurHeight)
        blurFBO_B = bBPair.first; blurTex_B = bBPair.second

        Timber.d("$name: resized blur=${blurWidth}x${blurHeight}")
    }

    override fun execute(state: RenderState) {
        val quad = fsQuad ?: return
        val passes = state.qualityLevel.bloomPasses
        if (passes == 0) return // LOW quality = no bloom

        val sceneTex = sceneTextureProvider()
        GLES30.glViewport(0, 0, blurWidth, blurHeight)

        // ── Step 1: Bright extract ───────────────────────────
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, brightFBO)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val ep = extractProgram!!
        ep.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTex)
        ep.setInt("uSceneTexture", 0)
        ep.setFloat("uThreshold", threshold)
        drawQuad(quad)

        // ── Step 2: Ping-pong blur ───────────────────────────
        val bp = blurProgram!!
        bp.use()
        bp.setFloat("uBlurScale", blurScale)

        var readTex = brightTex
        var writeFBO = fbo
        @Suppress("UNUSED_VALUE")
        var writeTex = colorTexture

        for (_p in 0 until passes) {
            // Horizontal
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, writeFBO)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, readTex)
            bp.setInt("uTexture", 0)
            bp.setVec2("uDirection", 1f / blurWidth, 0f)
            drawQuad(quad)

            // Swap for vertical
            if (writeFBO == fbo) {
                readTex = colorTexture; writeFBO = blurFBO_B; writeTex = blurTex_B
            } else {
                readTex = blurTex_B; writeFBO = fbo; writeTex = colorTexture
            }

            // Vertical
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, writeFBO)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, readTex)
            bp.setVec2("uDirection", 0f, 1f / blurHeight)
            drawQuad(quad)

            readTex = writeTex
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /**
     * Get the final bloom texture handle for the composite pass.
     */
    fun getBloomTexture(passes: Int): Int {
        // After ping-pong, the last written buffer determines which tex holds the result
        return if (passes % 2 == 0) blurTex_B else colorTexture
    }

    private fun drawQuad(quad: MeshGenerator.MeshData) {
        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)
    }

    private fun createColorFBO(w: Int, h: Int): Pair<Int, Int> {
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

        return Pair(fbos[0], texs[0])
    }

    private fun releaseBloomFBOs() {
        if (brightFBO != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(brightFBO), 0); brightFBO = 0 }
        if (brightTex != 0) { GLES30.glDeleteTextures(1, intArrayOf(brightTex), 0); brightTex = 0 }
        if (blurFBO_B != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(blurFBO_B), 0); blurFBO_B = 0 }
        if (blurTex_B != 0) { GLES30.glDeleteTextures(1, intArrayOf(blurTex_B), 0); blurTex_B = 0 }
    }

    override fun release() {
        releaseBloomFBOs()
        super.release()
        extractProgram?.release(); extractProgram = null
        blurProgram?.release(); blurProgram = null
        fsQuad?.release(); fsQuad = null
        Timber.d("$name: released")
    }
}

