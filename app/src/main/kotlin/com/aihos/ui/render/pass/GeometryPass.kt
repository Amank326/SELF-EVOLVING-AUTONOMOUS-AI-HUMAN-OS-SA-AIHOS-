package com.aihos.ui.render.pass

import android.opengl.GLES30
import com.aihos.ui.gl.CinematicShaders
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.cognition.CognitiveMotionEngine
import com.aihos.ui.render.cognition.CognitiveShaders
import com.aihos.ui.render.core.FBORenderPass
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderState
import com.aihos.ui.render.datagraph.CognitiveGraphPass
import com.aihos.ui.render.hud.HolographicHUDPass
import com.aihos.ui.render.lattice.NeuralLatticePass
import com.aihos.ui.render.lighting.LightingController
import com.aihos.ui.render.scene.SceneManager
import com.aihos.ui.render.universe.ProceduralUniversePass
import com.aihos.ui.render.audio.AudioResonanceController
import timber.log.Timber

/**
 * GeometryPass — Renders all 3D scene geometry into an HDR FBO.
 *
 * Pipeline position: FIRST
 * Input:  Scene graph, camera, lighting, AI state, cognitive motion output, audio resonance
 * Output: HDR color texture + depth buffer (for downstream bloom/composite)
 *
 * Draws:
 *   1. Procedural universe background (nebula + starfield)
 *   2. Universe ambient particles
 *   3. Neural lattice (beams + nodes)
 *   4. Cognitive data graph (reasoning nodes + edges + flow particles)
 *   5. AI brain icosphere (cognitive shaders)
 *   6. Holographic HUD (glass panels + data overlays)
 *   7. Neural particle field
 *   8. Universe energy field overlay
 */
class GeometryPass(
    private val sceneManager: SceneManager,
    private val camera: CameraController,
    private val lighting: LightingController,
    private val cognitiveMotion: CognitiveMotionEngine? = null,
    private val latticePass: NeuralLatticePass? = null,
    private val hudPass: HolographicHUDPass? = null,
    private val cognitiveGraphPass: CognitiveGraphPass? = null,
    private val universePass: ProceduralUniversePass? = null,
    private val audioController: AudioResonanceController? = null
) : FBORenderPass() {

    override val name = "GeometryPass"

    // ── Shaders ──────────────────────────────────────────────────
    private var sceneProgram: ShaderProgram? = null
    private var particleProgram: ShaderProgram? = null
    private var backgroundProgram: ShaderProgram? = null

    // ── Meshes ───────────────────────────────────────────────────
    private var icosphereMesh: MeshGenerator.MeshData? = null
    private var particleMesh: MeshGenerator.MeshData? = null
    private var fsQuad: MeshGenerator.MeshData? = null

    override fun initialize() {
        sceneProgram = ShaderProgram(CognitiveShaders.COGNITIVE_SCENE_VERTEX, CognitiveShaders.COGNITIVE_SCENE_FRAGMENT)
        particleProgram = ShaderProgram(CognitiveShaders.COGNITIVE_PARTICLE_VERTEX, CognitiveShaders.COGNITIVE_PARTICLE_FRAGMENT)
        backgroundProgram = ShaderProgram(CinematicShaders.BACKGROUND_VERTEX, CinematicShaders.BACKGROUND_FRAGMENT)

        icosphereMesh = MeshGenerator.generateIcosphere(subdivisions = 3, radius = 1.0f)
        particleMesh = MeshGenerator.generateParticleCloud(count = 1200)
        fsQuad = MeshGenerator.generateFullScreenQuad()

        // Initialize neural lattice sub-pass
        latticePass?.initialize()

        // Initialize holographic HUD sub-pass
        hudPass?.initialize()

        // Initialize cognitive data graph sub-pass
        cognitiveGraphPass?.initialize()

        // Initialize procedural universe sub-pass
        universePass?.initialize()

        Timber.d("$name: initialized (3 shaders, 3 meshes, lattice=${latticePass != null}, hud=${hudPass != null}, cogGraph=${cognitiveGraphPass != null}, universe=${universePass != null})")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        val w = (width * qualityLevel.resolutionScale).toInt().coerceAtLeast(1)
        val h = (height * qualityLevel.resolutionScale).toInt().coerceAtLeast(1)
        createFBO(w, h, withDepth = true)
        latticePass?.resize(width, height, qualityLevel)
        hudPass?.resize(width, height, qualityLevel)
        cognitiveGraphPass?.resize(width, height, qualityLevel)
        universePass?.resize(width, height, qualityLevel)
        Timber.d("$name: FBO resized to ${w}x${h}")
    }

    override fun execute(state: RenderState) {
        // Bind our FBO
        bindFBO()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // ── Universe background (nebula + starfield) ─────────
        if (universePass != null) {
            universePass.renderBackground(state)
        } else {
            drawBackground(state)
        }

        // ── Universe ambient particles (mid-depth) ──────────
        universePass?.renderMidLayers(state)

        // ── Neural lattice (beams + nodes) ───────────────────
        latticePass?.execute(state)

        // ── Cognitive data graph (reasoning visualization) ──
        cognitiveGraphPass?.execute(state)

        // ── Icosphere (AI brain core) ────────────────────────
        drawIcosphere(state)

        // ── Holographic HUD (glass panels + data overlays) ──
        hudPass?.execute(state)

        // ── Particles (neural field) ─────────────────────────
        drawParticles(state)

        // ── Universe energy field overlay ────────────────────
        universePass?.renderOverlay(state)

        // Unbind
        unbindFBO()
    }

    // ── Background ───────────────────────────────────────────────

    private fun drawBackground(state: RenderState) {
        val prog = backgroundProgram ?: return
        val quad = fsQuad ?: return

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        prog.use()
        prog.setFloat("uTime", state.elapsedTime)
        prog.setFloat("uCognitiveLoad", state.aiMetrics.cognitiveLoad)
        prog.setFloat("uConfidence", state.aiMetrics.confidence)
        prog.setVec2("uResolution", state.screenWidth.toFloat(), state.screenHeight.toFloat())

        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(quad.drawMode, 0, quad.vertexCount)
        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    // ── Icosphere ────────────────────────────────────────────────

    private fun drawIcosphere(state: RenderState) {
        val prog = sceneProgram ?: return
        val mesh = icosphereMesh ?: return
        val brain = sceneManager.brainNode
        val co = cognitiveMotion?.output

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        prog.use()

        // Matrices
        prog.setMat4("uModel", brain.worldMatrix)
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setMat3("uNormalMatrix", brain.normalMatrix)

        // Cognitive uniforms
        if (co != null) {
            prog.setFloat("uCognitiveTime", co.cognitiveTime)
            prog.setFloat("uWallTime", state.elapsedTime)
            prog.setFloat("uPulsationFreq", co.pulsationFrequency)
            prog.setFloat("uPulsationAmplitude", co.pulsationAmplitude)
            prog.setFloat("uMorphAmplitude", co.morphAmplitude)
            prog.setFloat("uNoiseDistortion", co.noiseDistortion)
            prog.setFloat("uNoiseFrequency", co.noiseFrequency)
            prog.setFloat("uDecisionComplexity", state.aiMetrics.let {
                (it.cognitiveLoad * 0.4f + it.autonomyLevel * 0.3f + it.evolutionRate * 0.3f).coerceIn(0f, 1f)
            })
            prog.setFloat("uGlowIntensity", co.glowIntensity)
            prog.setFloat("uFresnelAmplification", co.fresnelAmplification)
            prog.setFloat("uColorWarmth", co.colorWarmth)
            prog.setFloat("uAccentStrength", co.accentStrength)
            prog.setFloat("uHeatMapIntensity", co.heatMapIntensity)
            prog.setFloat("uRadialPulseIntensity", co.radialPulseIntensity)
            prog.setFloat("uFogDensity", co.fogDensity)
            prog.setFloat("uReflectionDepth", state.aiMetrics.selfAwareness)
            prog.setFloat("uMemoryActivity", state.aiMetrics.memoryLoad)
        } else {
            // Fallback: use wall time and defaults
            prog.setFloat("uCognitiveTime", state.elapsedTime)
            prog.setFloat("uWallTime", state.elapsedTime)
            prog.setFloat("uPulsationFreq", 0.8f)
            prog.setFloat("uPulsationAmplitude", 0.02f)
            prog.setFloat("uMorphAmplitude", 0f)
            prog.setFloat("uNoiseDistortion", 0f)
            prog.setFloat("uNoiseFrequency", 3f)
            prog.setFloat("uDecisionComplexity", 0.3f)
            prog.setFloat("uGlowIntensity", 0.5f)
            prog.setFloat("uFresnelAmplification", 1f)
            prog.setFloat("uColorWarmth", 0f)
            prog.setFloat("uAccentStrength", 0f)
            prog.setFloat("uHeatMapIntensity", 0f)
            prog.setFloat("uRadialPulseIntensity", 0f)
            prog.setFloat("uFogDensity", 0.15f)
            prog.setFloat("uReflectionDepth", 0f)
            prog.setFloat("uMemoryActivity", 0.3f)
        }

        // Standard AI uniforms (still needed by shader)
        prog.setFloat("uCognitiveLoad", state.aiMetrics.cognitiveLoad)
        prog.setFloat("uConfidence", state.aiMetrics.confidence)
        prog.setFloat("uEvolutionRate", state.aiMetrics.evolutionRate)
        prog.setFloat("uCameraDistance", camera.distanceToOrigin)
        prog.setVec3("uCameraPos", camera.position[0], camera.position[1], camera.position[2])

        // Audio resonance uniforms (gracefully ignored if not in shader)
        val av = audioController?.output
        prog.setFloat("uBassEnergy", av?.shaderBassEnergy ?: 0f)
        prog.setFloat("uMidEnergy", av?.shaderMidEnergy ?: 0f)
        prog.setFloat("uHighEnergy", av?.shaderHighEnergy ?: 0f)
        prog.setFloat("uAmplitude", av?.shaderAmplitude ?: 0f)
        prog.setFloat("uAudioPulsation", av?.pulsationBoost ?: 0f)
        prog.setFloat("uAudioGlow", av?.glowBoost ?: 0f)

        // Lighting
        lighting.uploadToShader(prog)

        // Draw
        GLES30.glBindVertexArray(mesh.vao)
        GLES30.glDrawElements(mesh.drawMode, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    // ── Particles ────────────────────────────────────────────────

    private fun drawParticles(state: RenderState) {
        val prog = particleProgram ?: return
        val mesh = particleMesh ?: return
        val co = cognitiveMotion?.output

        // Additive blend, no depth write
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)

        // Cognitive particle uniforms
        if (co != null) {
            prog.setFloat("uCognitiveTime", co.cognitiveTime)
            prog.setFloat("uParticleEmissionRate", co.particleEmissionRate)
            prog.setFloat("uParticleVelocity", co.particleVelocity)
            prog.setFloat("uParticleBrightness", co.particleBrightness)
            prog.setFloat("uTimeScale", co.timeScale)
        } else {
            prog.setFloat("uCognitiveTime", state.elapsedTime)
            prog.setFloat("uParticleEmissionRate", 1f)
            prog.setFloat("uParticleVelocity", 1f)
            prog.setFloat("uParticleBrightness", 1f)
            prog.setFloat("uTimeScale", 1f)
        }

        prog.setFloat("uCognitiveLoad", state.aiMetrics.cognitiveLoad)
        prog.setFloat("uConfidence", state.aiMetrics.confidence)
        prog.setFloat("uEvolutionRate", state.aiMetrics.evolutionRate)
        prog.setFloat("uMemoryActivity", state.aiMetrics.memoryLoad)
        prog.setFloat("uDecisionComplexity",
            (state.aiMetrics.cognitiveLoad * 0.4f + state.aiMetrics.autonomyLevel * 0.3f +
                    state.aiMetrics.evolutionRate * 0.3f).coerceIn(0f, 1f))
        prog.setVec2("uResolution", state.screenWidth.toFloat(), state.screenHeight.toFloat())

        // Audio resonance uniforms for particles
        val pav = audioController?.output
        prog.setFloat("uBassEnergy", pav?.shaderBassEnergy ?: 0f)
        prog.setFloat("uMidEnergy", pav?.shaderMidEnergy ?: 0f)
        prog.setFloat("uHighEnergy", pav?.shaderHighEnergy ?: 0f)
        prog.setFloat("uAmplitude", pav?.shaderAmplitude ?: 0f)
        prog.setFloat("uParticleBurstIntensity", pav?.particleBurstIntensity ?: 0f)

        GLES30.glBindVertexArray(mesh.vao)
        GLES30.glDrawArrays(mesh.drawMode, 0, mesh.vertexCount)
        GLES30.glBindVertexArray(0)

        // Restore
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    // ── Cleanup ──────────────────────────────────────────────────

    override fun release() {
        super.release()
        sceneProgram?.release(); sceneProgram = null
        particleProgram?.release(); particleProgram = null
        backgroundProgram?.release(); backgroundProgram = null
        icosphereMesh?.release(); icosphereMesh = null
        particleMesh?.release(); particleMesh = null
        fsQuad?.release(); fsQuad = null
        latticePass?.release()
        hudPass?.release()
        cognitiveGraphPass?.release()
        universePass?.release()
        Timber.d("$name: released")
    }
}

