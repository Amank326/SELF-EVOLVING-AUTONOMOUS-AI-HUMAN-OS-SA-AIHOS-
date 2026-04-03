package com.aihos.ui.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * CinematicRenderer — Production-grade OpenGL ES 3.0 renderer for SA-AIHOS.
 *
 * Rendering pipeline per frame:
 *
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  PerformanceMonitor.beginFrame()                        │
 *   │  ProceduralAnimationEngine.update(aiState)              │
 *   │  CinematicCamera.update(dt, aiMetrics)                  │
 *   │  DynamicLightingSystem.update(dt, time, aiMetrics)      │
 *   │                                                         │
 *   │  PostProcessingPipeline.beginScenePass()  ← FBO bind    │
 *   │    ├── Draw background (procedural nebula)              │
 *   │    ├── Draw icosphere (AI brain core)                   │
 *   │    └── Draw particles (neural field)                    │
 *   │                                                         │
 *   │  PostProcessingPipeline.executePostProcessing()         │
 *   │    ├── Bloom extract                                    │
 *   │    ├── Gaussian blur (ping-pong)                        │
 *   │    └── Composite (scene + bloom + vignette + grading)   │
 *   │                                                         │
 *   │  PerformanceMonitor.endFrame()                          │
 *   └─────────────────────────────────────────────────────────┘
 *
 * Thread safety:
 *   AI state is pushed via AtomicReference from any thread.
 *   The GL thread reads the snapshot once per frame.
 *
 * Memory:
 *   All buffers, meshes, and matrices are pre-allocated.
 *   Zero GC pressure inside onDrawFrame().
 */
class CinematicRenderer : GLSurfaceView.Renderer {

    // ═══════════════════════════════════════════════════════════════
    // AI State Input (thread-safe)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Push AI metrics from any thread. The renderer reads this atomically
     * once per frame. Use [updateAIState] for convenience.
     */
    private val aiStateRef = AtomicReference(ProceduralAnimationEngine.VisualInputState())

    fun updateAIState(
        cognitiveLoad: Float = 0.5f,
        confidence: Float = 0.5f,
        evolutionRate: Float = 0.0f,
        selfAwareness: Float = 0.5f,
        autonomyLevel: Float = 0.5f,
        systemHealth: Float = 0.8f,
        memoryLoad: Float = 0.3f,
        animationIntensity: Float = 0.5f
    ) {
        aiStateRef.set(
            ProceduralAnimationEngine.VisualInputState(
                cognitiveLoad = cognitiveLoad.coerceIn(0f, 1f),
                confidence = confidence.coerceIn(0f, 1f),
                evolutionRate = evolutionRate.coerceIn(0f, 1f),
                selfAwareness = selfAwareness.coerceIn(0f, 1f),
                autonomyLevel = autonomyLevel.coerceIn(0f, 1f),
                systemHealth = systemHealth.coerceIn(0f, 1f),
                memoryLoad = memoryLoad.coerceIn(0f, 1f),
                animationIntensity = animationIntensity.coerceIn(0f, 1f)
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Subsystems (all pre-allocated, no per-frame instantiation)
    // ═══════════════════════════════════════════════════════════════

    private val perfMonitor = PerformanceMonitor()
    private val camera = CinematicCamera()
    private val lighting = DynamicLightingSystem()
    private val animationEngine = ProceduralAnimationEngine()
    private val postProcessing = PostProcessingPipeline()

    // ── Shader programs ──────────────────────────────────────────
    private var sceneProgram: ShaderProgram? = null
    private var particleProgram: ShaderProgram? = null

    // ── Meshes ───────────────────────────────────────────────────
    private var icosphereMesh: MeshGenerator.MeshData? = null
    private var particleMesh: MeshGenerator.MeshData? = null

    // ── Pre-allocated matrices ───────────────────────────────────
    private val modelMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(9)

    // ── Screen dimensions ────────────────────────────────────────
    private var screenWidth = 1
    private var screenHeight = 1

    // ── Model rotation state ─────────────────────────────────────
    private var modelRotation = 0f

    // ═══════════════════════════════════════════════════════════════
    // GLSurfaceView.Renderer callbacks
    // ═══════════════════════════════════════════════════════════════

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Timber.i("CinematicRenderer: onSurfaceCreated (OpenGL ES 3.0)")

        // ── GL state ─────────────────────────────────────────
        GLES30.glClearColor(0.01f, 0.01f, 0.03f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        // ── Compile shaders ──────────────────────────────────
        sceneProgram = ShaderProgram(CinematicShaders.SCENE_VERTEX, CinematicShaders.SCENE_FRAGMENT)
        particleProgram = ShaderProgram(CinematicShaders.PARTICLE_VERTEX, CinematicShaders.PARTICLE_FRAGMENT)

        if (sceneProgram?.isValid != true) {
            Timber.e("CinematicRenderer: scene shader compilation FAILED")
        }
        if (particleProgram?.isValid != true) {
            Timber.e("CinematicRenderer: particle shader compilation FAILED")
        }

        // ── Generate meshes ──────────────────────────────────
        icosphereMesh = MeshGenerator.generateIcosphere(subdivisions = 3, radius = 1.0f)
        particleMesh = MeshGenerator.generateParticleCloud(count = 1200)

        // ── Initialize post-processing ───────────────────────
        postProcessing.initialize()

        // ── Initialize identity model matrix ─────────────────
        Matrix.setIdentityM(modelMatrix, 0)

        // ── Reset perf monitor ───────────────────────────────
        perfMonitor.reset()

        GLShaderUtil.checkGLError("CinematicRenderer.onSurfaceCreated")
        Timber.i("CinematicRenderer: initialization complete")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        GLES30.glViewport(0, 0, width, height)

        camera.setProjection(width, height)
        postProcessing.resize(width, height, perfMonitor.resolutionScale)

        // Adjust point light count based on quality
        lighting.setPointLightCount(
            when (perfMonitor.qualityLevel) {
                PerformanceMonitor.QualityLevel.HIGH -> 4
                PerformanceMonitor.QualityLevel.MEDIUM -> 3
                PerformanceMonitor.QualityLevel.LOW -> 2
            }
        )

        Timber.i("CinematicRenderer: surface ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        // ── TIMING ───────────────────────────────────────────
        perfMonitor.beginFrame()
        val dt = perfMonitor.deltaTime
        val time = perfMonitor.elapsedTime

        // ── READ AI STATE (atomic, once per frame) ───────────
        val aiState = aiStateRef.get()

        // ── ANIMATION ENGINE ─────────────────────────────────
        animationEngine.update(aiState, time, dt)
        val vp = animationEngine.params

        // ── CAMERA UPDATE ────────────────────────────────────
        camera.update(
            dt = dt,
            autonomyLevel = aiState.autonomyLevel,
            cognitiveLoad = aiState.cognitiveLoad,
            confidence = aiState.confidence,
            reflectionDepth = aiState.selfAwareness,
            evolutionRate = aiState.evolutionRate
        )

        // ── LIGHTING UPDATE ──────────────────────────────────
        lighting.update(
            dt = dt,
            time = time,
            confidence = aiState.confidence,
            cognitiveLoad = aiState.cognitiveLoad,
            systemHealth = aiState.systemHealth,
            evolutionRate = aiState.evolutionRate,
            autonomyLevel = aiState.autonomyLevel
        )

        // ── MODEL ROTATION ───────────────────────────────────
        modelRotation += vp.rotationSpeed * dt
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(modelRotation.toDouble()).toFloat(), 0f, 1f, 0f)
        // Subtle tilt
        val tilt = kotlin.math.sin(time * 0.3f) * 5f
        Matrix.rotateM(modelMatrix, 0, tilt, 1f, 0f, 0f)

        // ── Update post-processing params from animation ─────
        postProcessing.bloomStrength = vp.bloomStrength
        postProcessing.bloomThreshold = vp.bloomThreshold
        postProcessing.vignetteIntensity = vp.vignetteStrength
        postProcessing.exposure = vp.exposureLevel
        postProcessing.saturation = vp.saturationBoost

        // ══════════════════════════════════════════════════════
        // RENDER PASS 1: Scene to FBO
        // ══════════════════════════════════════════════════════

        postProcessing.beginScenePass()

        // ── Background nebula ────────────────────────────────
        postProcessing.drawBackground(
            time, aiState.cognitiveLoad, aiState.confidence,
            screenWidth.toFloat(), screenHeight.toFloat()
        )

        // ── Draw icosphere (AI brain core) ───────────────────
        drawIcosphere(time, aiState)

        // ── Draw particles (neural field) ────────────────────
        drawParticles(time, aiState)

        // ══════════════════════════════════════════════════════
        // RENDER PASS 2: Post-processing (bloom + composite)
        // ══════════════════════════════════════════════════════

        postProcessing.executePostProcessing(perfMonitor.bloomPasses, time)

        // ── TIMING END ───────────────────────────────────────
        perfMonitor.endFrame()
    }

    // ═══════════════════════════════════════════════════════════════
    // Scene Drawing
    // ═══════════════════════════════════════════════════════════════

    private fun drawIcosphere(time: Float, aiState: ProceduralAnimationEngine.VisualInputState) {
        val prog = sceneProgram ?: return
        val mesh = icosphereMesh ?: return

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        prog.use()

        // ── Matrices ─────────────────────────────────────────
        prog.setMat4("uModel", modelMatrix)
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)

        // Normal matrix
        camera.computeNormalMatrix(modelMatrix, normalMatrix)
        prog.setMat3("uNormalMatrix", normalMatrix)

        // ── AI uniforms ──────────────────────────────────────
        prog.setFloat("uTime", time)
        prog.setFloat("uCognitiveLoad", aiState.cognitiveLoad)
        prog.setFloat("uConfidence", aiState.confidence)
        prog.setFloat("uEvolutionRate", aiState.evolutionRate)
        prog.setFloat("uCameraDistance", camera.getDistanceToOrigin())
        prog.setVec3("uCameraPos", camera.position[0], camera.position[1], camera.position[2])

        // ── Lighting ─────────────────────────────────────────
        lighting.uploadToShader(prog)

        // ── Draw ─────────────────────────────────────────────
        GLES30.glBindVertexArray(mesh.vao)
        GLES30.glDrawElements(mesh.drawMode, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawParticles(time: Float, aiState: ProceduralAnimationEngine.VisualInputState) {
        val prog = particleProgram ?: return
        val mesh = particleMesh ?: return

        // Particles are additive-blended, no depth write
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        prog.use()

        // ── Matrices ─────────────────────────────────────────
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)

        // ── AI uniforms ──────────────────────────────────────
        prog.setFloat("uTime", time)
        prog.setFloat("uCognitiveLoad", aiState.cognitiveLoad)
        prog.setFloat("uConfidence", aiState.confidence)
        prog.setFloat("uEvolutionRate", aiState.evolutionRate)
        prog.setFloat("uAutonomyLevel", aiState.autonomyLevel)
        prog.setFloat("uSystemHealth", aiState.systemHealth)
        prog.setVec2("uResolution", screenWidth.toFloat(), screenHeight.toFloat())

        // ── Draw ─────────────────────────────────────────────
        GLES30.glBindVertexArray(mesh.vao)
        GLES30.glDrawArrays(mesh.drawMode, 0, mesh.vertexCount)
        GLES30.glBindVertexArray(0)

        // Restore state
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════

    /** Get performance stats string. */
    fun getPerformanceStats(): String = perfMonitor.getStats()

    /** Force quality level (overrides auto-scaling). */
    fun forceQuality(level: PerformanceMonitor.QualityLevel) {
        perfMonitor.forceQuality(level)
    }

    /** Current quality level. */
    val qualityLevel: PerformanceMonitor.QualityLevel
        get() = perfMonitor.qualityLevel

    /** Current FPS. */
    val fps: Float
        get() = perfMonitor.currentFPS

    // ═══════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════

    fun release() {
        sceneProgram?.release()
        particleProgram?.release()
        icosphereMesh?.release()
        particleMesh?.release()
        postProcessing.release()

        sceneProgram = null
        particleProgram = null
        icosphereMesh = null
        particleMesh = null

        Timber.i("CinematicRenderer: released all GL resources")
    }
}

