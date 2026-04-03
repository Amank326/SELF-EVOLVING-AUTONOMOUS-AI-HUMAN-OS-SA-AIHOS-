package com.aihos.ui.render.pass

import android.opengl.GLES30
import com.aihos.ui.gl.CinematicShaders
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.cognition.CognitiveShaders
import com.aihos.ui.render.cognition.CognitiveVisualOutput
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber

/**
 * CompositePass — Final pass that combines scene + bloom → screen.
 *
 * Pipeline position: LAST
 * Input:  Scene texture + bloom texture + cognitive visual output
 * Output: Final image to default framebuffer (screen)
 *
 * Effects:
 *   - ACES Film tonemapping
 *   - Vignette
 *   - Cognitive-driven chromatic aberration
 *   - Radial pulse waves
 *   - Film grain
 *   - Saturation adjustment
 *   - Gamma correction
 */
class CompositePass(
    private val sceneTextureProvider: () -> Int,
    private val bloomTextureProvider: () -> Int,
    private val bloomPassCountProvider: () -> Int,
    private val cognitiveOutputProvider: (() -> CognitiveVisualOutput?)? = null
) : RenderPass {

    override val name = "CompositePass"

    private var compositeProgram: ShaderProgram? = null
    private var fsQuad: MeshGenerator.MeshData? = null

    // Tuning (set by animation engine)
    var bloomStrength = 0.4f
    var vignetteIntensity = 0.35f
    var exposure = 1.2f
    var gamma = 2.2f
    var saturation = 1.1f

    private var screenWidth = 1
    private var screenHeight = 1

    override fun initialize() {
        compositeProgram = ShaderProgram(CinematicShaders.COMPOSITE_VERTEX, CognitiveShaders.COGNITIVE_COMPOSITE_FRAGMENT)
        fsQuad = MeshGenerator.generateFullScreenQuad()
        Timber.d("$name: initialized (cognitive composite)")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        screenWidth = width
        screenHeight = height
    }

    override fun execute(state: RenderState) {
        val prog = compositeProgram ?: return
        val quad = fsQuad ?: return
        val bloomPasses = bloomPassCountProvider()

        // Render to screen
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        prog.use()

        // Scene texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTextureProvider())
        prog.setInt("uSceneTexture", 0)

        // Bloom texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTextureProvider())
        prog.setInt("uBloomTexture", 1)

        // Uniforms
        prog.setFloat("uBloomStrength", if (bloomPasses > 0) bloomStrength else 0f)
        prog.setFloat("uVignetteIntensity", vignetteIntensity)
        prog.setFloat("uExposure", exposure)
        prog.setFloat("uGamma", gamma)
        prog.setFloat("uSaturation", saturation)
        prog.setFloat("uTime", state.elapsedTime)

        // Cognitive composite uniforms
        val co = cognitiveOutputProvider?.invoke()
        prog.setFloat("uChromaticAberration", co?.chromaticAberration ?: 0.002f)
        prog.setFloat("uRadialPulseIntensity", co?.radialPulseIntensity ?: 0f)
        prog.setFloat("uCognitiveLoad", state.aiMetrics.cognitiveLoad)

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    override fun release() {
        compositeProgram?.release(); compositeProgram = null
        fsQuad?.release(); fsQuad = null
        Timber.d("$name: released")
    }
}

