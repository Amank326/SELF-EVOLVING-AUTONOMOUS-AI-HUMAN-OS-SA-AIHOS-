package com.aihos.ui.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.cognition.CognitiveMotionEngine
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.AIStateBridge
import com.aihos.ui.render.core.FrameTimer
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import com.aihos.ui.render.datagraph.CognitiveGraphBridge
import com.aihos.ui.render.datagraph.CognitiveGraphPass
import com.aihos.ui.render.hud.HolographicHUDPass
import com.aihos.ui.render.immersive.ImmersiveDepthController
import com.aihos.ui.render.immersive.ImmersiveDepthPass
import com.aihos.ui.render.lattice.NeuralLatticePass
import com.aihos.ui.render.lighting.LightingController
import com.aihos.ui.render.pass.BloomPass
import com.aihos.ui.render.pass.CompositePass
import com.aihos.ui.render.pass.GeometryPass
import com.aihos.ui.render.scene.SceneManager
import com.aihos.ui.render.universe.ProceduralUniversePass
import com.aihos.ui.render.audio.AudioResonanceBridge
import com.aihos.ui.render.audio.AudioResonanceController
import com.aihos.ui.render.audio.AudioResonanceSnapshot
import timber.log.Timber
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * RenderEngine — Production-grade modular OpenGL ES 3.0 renderer.
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     RenderEngine                            │
 * │                                                             │
 * │  ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐ │
 * │  │ AIStateBridge │  │  FrameTimer   │  │  SceneManager    │ │
 * │  │ (thread-safe) │  │  (zero-alloc) │  │  (scene graph)   │ │
 * │  └──────┬───────┘  └──────┬────────┘  └──────┬───────────┘ │
 * │         │                 │                   │             │
 * │         ▼                 ▼                   ▼             │
 * │  ┌──────────────────────────────────────────────────────┐   │
 * │  │              RenderState (immutable snapshot)        │   │
 * │  └───────────────────────┬──────────────────────────────┘   │
 * │                          │                                  │
 * │  ┌───────────────────────▼──────────────────────────────┐   │
 * │  │              Render Pass Pipeline                    │   │
 * │  │                                                      │   │
 * │  │  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │   │
 * │  │  │ Geometry  │→ │  Bloom   │→ │    Composite      │  │   │
 * │  │  │  Pass     │  │  Pass    │  │    Pass           │  │   │
 * │  │  │ (FBO)     │  │ (FBOs)  │  │    (screen)       │  │   │
 * │  │  └──────────┘  └──────────┘  └───────────────────┘  │   │
 * │  └──────────────────────────────────────────────────────┘   │
 * │                                                             │
 * │  ┌────────────────────┐  ┌──────────────────────────────┐   │
 * │  │  CameraController  │  │  LightingController          │   │
 * │  └────────────────────┘  └──────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Thread model:
 *   - pushState() from ViewModel (Main/IO thread)
 *   - onDrawFrame() reads state atomically on GL thread
 *   - No locks, no synchronization, no torn reads
 *
 * Memory model:
 *   - All buffers pre-allocated at init
 *   - Zero GC inside onDrawFrame()
 *   - FloatBuffers reused, never reallocated
 *   - GPU resources released deterministically
 *
 * Lifecycle:
 *   onSurfaceCreated → initialize all passes
 *   onSurfaceChanged → resize all passes
 *   onDrawFrame      → frame timer → build state → execute passes
 *   release()        → release all passes (call from queueEvent)
 */
class RenderEngine : GLSurfaceView.Renderer {

    // ═══════════════════════════════════════════════════════════════
    // Subsystems
    // ═══════════════════════════════════════════════════════════════

    val aiStateBridge = AIStateBridge()
    private val frameTimer = FrameTimer()
    val camera = CameraController()
    val lighting = LightingController()
    val sceneManager = SceneManager()
    val cognitiveMotion = CognitiveMotionEngine()
    val immersive = ImmersiveDepthController()
    lateinit var neuralLattice: NeuralLatticePass
        private set
    lateinit var holographicHUD: HolographicHUDPass
        private set
    lateinit var cognitiveGraph: CognitiveGraphPass
        private set
    val cognitiveGraphBridge = CognitiveGraphBridge()
    lateinit var proceduralUniverse: ProceduralUniversePass
        private set

    // ═══════════════════════════════════════════════════════════════
    // Audio Resonance Subsystem
    // ═══════════════════════════════════════════════════════════════

    val audioResonanceBridge = AudioResonanceBridge()
    val audioResonanceController = AudioResonanceController()

    // ═══════════════════════════════════════════════════════════════
    // Render passes (ordered)
    // ═══════════════════════════════════════════════════════════════

    private lateinit var geometryPass: GeometryPass
    private lateinit var bloomPass: BloomPass
    private lateinit var compositePass: CompositePass
    private lateinit var immersivePass: ImmersiveDepthPass

    private val passes = mutableListOf<RenderPass>()

    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════

    private var screenWidth = 1
    private var screenHeight = 1
    private var isInitialized = false

    // ═══════════════════════════════════════════════════════════════
    // GLSurfaceView.Renderer implementation
    // ═══════════════════════════════════════════════════════════════

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Timber.i("RenderEngine: onSurfaceCreated (OpenGL ES 3.0)")

        // Global GL state
        GLES30.glClearColor(0.01f, 0.01f, 0.03f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        // Build pass pipeline
        neuralLattice = NeuralLatticePass(camera)
        holographicHUD = HolographicHUDPass(camera)
        cognitiveGraph = CognitiveGraphPass(camera)
        proceduralUniverse = ProceduralUniversePass(camera)
        geometryPass = GeometryPass(sceneManager, camera, lighting, cognitiveMotion, neuralLattice, holographicHUD, cognitiveGraph, proceduralUniverse, audioResonanceController)
        bloomPass = BloomPass(sceneTextureProvider = { geometryPass.colorTexture })
        compositePass = CompositePass(
            sceneTextureProvider = { geometryPass.colorTexture },
            bloomTextureProvider = { bloomPass.getBloomTexture(frameTimer.qualityLevel.bloomPasses) },
            bloomPassCountProvider = { frameTimer.qualityLevel.bloomPasses },
            cognitiveOutputProvider = { cognitiveMotion.output }
        )
        immersivePass = ImmersiveDepthPass(
            config = immersive.config,
            leftEyeTextureProvider = { immersive.leftEyeColorTex },
            rightEyeTextureProvider = { immersive.rightEyeColorTex },
            sceneTextureProvider = { geometryPass.colorTexture }
        )

        passes.clear()
        passes.add(geometryPass)
        passes.add(bloomPass)
        passes.add(compositePass)
        passes.add(immersivePass)

        // Initialize all passes
        for (pass in passes) {
            pass.initialize()
        }

        frameTimer.reset()
        isInitialized = true
        Timber.i("RenderEngine: ${passes.size} render passes initialized")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        GLES30.glViewport(0, 0, width, height)

        camera.setProjection(width, height)

        val quality = frameTimer.qualityLevel
        lighting.setPointLightCount(quality.maxPointLights)

        for (pass in passes) {
            pass.resize(width, height, quality)
        }

        // Immersive mode eye FBOs
        immersive.resizeEyeFBOs(width, height, quality)

        Timber.i("RenderEngine: surface ${width}x${height} quality=$quality")
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!isInitialized) return

        // ── 1. Frame timing ──────────────────────────────────
        frameTimer.beginFrame()

        // ── 2. Read AI state (atomic, once) ──────────────────
        val aiMetrics = aiStateBridge.consumeState()

        // ── 3. Cognitive Motion Intelligence ─────────────────
        cognitiveMotion.update(
            aiMetrics = aiMetrics,
            wallDt = frameTimer.deltaTime,
            wallTime = frameTimer.elapsedTime,
            quality = frameTimer.qualityLevel
        )

        // ── 3b. Audio Resonance Blending ─────────────────────
        val audioSnapshot = audioResonanceBridge.consumeState()
        audioResonanceController.update(
            audio = audioSnapshot,
            aiMetrics = aiMetrics,
            cognitiveOutput = cognitiveMotion.output,
            deltaTime = frameTimer.deltaTime,
            elapsedTime = frameTimer.elapsedTime
        )

        // ── 4. Build immutable render state ──────────────────
        val state = frameTimer.buildRenderState(screenWidth, screenHeight, aiMetrics)

        // ── 5. Update subsystems (driven by cognitive output) ─
        camera.update(frameTimer.deltaTime, aiMetrics)
        lighting.update(frameTimer.deltaTime, cognitiveMotion.output.cognitiveTime, aiMetrics)
        sceneManager.update(state)

        // ── 6. Immersive depth system ────────────────────────
        immersive.update(frameTimer.deltaTime, camera, frameTimer.qualityLevel)

        // ── 6b. Neural lattice update ────────────────────────
        neuralLattice.update(aiMetrics, frameTimer.deltaTime, frameTimer.elapsedTime)

        // ── 6c. Holographic HUD update ───────────────────────
        holographicHUD.update(aiMetrics, frameTimer.deltaTime, frameTimer.elapsedTime, camera.currentAzimuth)

        // ── 6d. Cognitive data graph update ──────────────────
        val graphSnapshot = cognitiveGraphBridge.consumeSnapshot()
        if (graphSnapshot != null) {
            cognitiveGraphBridge.applyToGraph(graphSnapshot, cognitiveGraph.graph)
        }
        cognitiveGraph.update(aiMetrics, frameTimer.deltaTime, frameTimer.elapsedTime, frameTimer.qualityLevel)

        // ── 7. Render ────────────────────────────────────────
        if (immersive.shouldRenderStereo) {
            // Stereo mode: render scene into left/right eye FBOs
            val sc = immersive.stereoCamera

            // Left eye
            immersive.bindLeftEye()
            renderSceneWithOverrides(state, sc.leftViewMatrix, sc.leftProjectionMatrix, sc.leftPosition)

            // Right eye
            immersive.bindRightEye()
            renderSceneWithOverrides(state, sc.rightViewMatrix, sc.rightProjectionMatrix, sc.rightPosition)

            // Restore default framebuffer
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, screenWidth, screenHeight)

            // Bloom on mono scene (still uses main geometry pass FBO)
            bloomPass.execute(state)

            // Composite main scene
            compositePass.execute(state)

            // Stereo blend overlay
            immersivePass.execute(state)
        } else {
            // Standard mono pipeline
            for (pass in passes) {
                pass.execute(state)
            }
        }

        // ── 8. End frame ─────────────────────────────────────
        frameTimer.endFrame()

        // ── 9. Dynamic universe quality ──────────────────────
        if (::proceduralUniverse.isInitialized) {
            proceduralUniverse.adjustForPerformance(frameTimer.currentFPS)
        }
    }

    /**
     * Render scene geometry using overridden view/projection matrices.
     * Used for stereo eye rendering.
     */
    private fun renderSceneWithOverrides(
        state: RenderState,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        eyePosition: FloatArray
    ) {
        // Save original camera matrices
        val savedView = camera.viewMatrix.copyOf()
        val savedProj = camera.projectionMatrix.copyOf()
        val savedPos = camera.position.copyOf()

        // Override with stereo eye matrices
        System.arraycopy(viewMatrix, 0, camera.viewMatrix, 0, 16)
        System.arraycopy(projectionMatrix, 0, camera.projectionMatrix, 0, 16)
        System.arraycopy(eyePosition, 0, camera.position, 0, 3)

        // Render geometry pass (into current bound FBO)
        geometryPass.execute(state)

        // Restore original matrices
        System.arraycopy(savedView, 0, camera.viewMatrix, 0, 16)
        System.arraycopy(savedProj, 0, camera.projectionMatrix, 0, 16)
        System.arraycopy(savedPos, 0, camera.position, 0, 3)
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════

    /** Push AI metrics from any thread. */
    fun pushAIState(snapshot: AIMetricsSnapshot) {
        aiStateBridge.pushState(snapshot)
    }

    /** Push AI metrics with named parameters. */
    fun pushAIState(
        cognitiveLoad: Float = 0.5f,
        confidence: Float = 0.5f,
        evolutionRate: Float = 0.0f,
        selfAwareness: Float = 0.5f,
        autonomyLevel: Float = 0.5f,
        systemHealth: Float = 0.8f,
        memoryLoad: Float = 0.3f,
        animationIntensity: Float = 0.5f
    ) {
        aiStateBridge.pushState(
            cognitiveLoad, confidence, evolutionRate, selfAwareness,
            autonomyLevel, systemHealth, memoryLoad, animationIntensity
        )
    }

    fun getPerformanceStats(): String = frameTimer.getStats()
    fun forceQuality(level: QualityLevel) { frameTimer.forceQuality(level) }
    val qualityLevel: QualityLevel get() = frameTimer.qualityLevel
    val fps: Float get() = frameTimer.currentFPS

    // ═══════════════════════════════════════════════════════════════
    // Immersive Depth Mode API
    // ═══════════════════════════════════════════════════════════════

    /** Enable/disable immersive depth mode with smooth transition. */
    fun setImmersiveMode(enabled: Boolean) {
        immersive.config.enabled = enabled
        Timber.i("RenderEngine: immersive mode ${if (enabled) "ON" else "OFF"}")
    }

    /** Toggle immersive mode. */
    fun toggleImmersiveMode() = setImmersiveMode(!immersive.config.enabled)

    /** Whether immersive mode is currently active. */
    val isImmersiveMode: Boolean get() = immersive.config.enabled

    /** Adjust stereo intensity [0..1]. 0 = mono, 1 = full stereo. */
    fun setStereoIntensity(intensity: Float) {
        immersive.config.stereoIntensity = intensity.coerceIn(0f, 1f)
    }

    /** Set stereo rendering mode: 0=side-by-side, 1=anaglyph, 2=subtle blend. */
    fun setStereoMode(mode: Int) {
        if (::immersivePass.isInitialized) immersivePass.stereoMode = mode.coerceIn(0, 2)
    }

    // ═══════════════════════════════════════════════════════════════
    // Holographic HUD API
    // ═══════════════════════════════════════════════════════════════

    /** Process touch for HUD panel interaction. Call on GL thread. */
    fun hudOnTouch(screenX: Float, screenY: Float): Int {
        if (!::holographicHUD.isInitialized) return -1
        return holographicHUD.onTouch(screenX, screenY, screenWidth, screenHeight)
    }

    /** Release HUD touch. Call on GL thread. */
    fun hudOnTouchRelease() {
        if (::holographicHUD.isInitialized) holographicHUD.onTouchRelease()
    }

    /** Get currently hovered HUD panel index, or -1. */
    val hoveredHUDPanel: Int get() =
        if (::holographicHUD.isInitialized) holographicHUD.raycaster.hoveredPanelIndex else -1

    // ═══════════════════════════════════════════════════════════════
    // Cognitive Data Graph API
    // ═══════════════════════════════════════════════════════════════

    /** Set cognitive graph visibility. */
    fun setCognitiveGraphVisible(visible: Boolean) {
        if (::cognitiveGraph.isInitialized) cognitiveGraph.isVisible = visible
    }

    /** Push a graph topology snapshot from any thread. */
    fun pushCognitiveGraph(snapshot: CognitiveGraphBridge.GraphSnapshot) {
        cognitiveGraphBridge.pushSnapshot(snapshot)
    }

    /** Highlight a cognitive graph node. Call on GL thread. */
    fun cognitiveGraphHighlight(nodeIndex: Int) {
        if (::cognitiveGraph.isInitialized) cognitiveGraph.highlightNode(nodeIndex)
    }

    /** Get cognitive graph stats. */
    val cognitiveGraphStats: String get() =
        if (::cognitiveGraph.isInitialized) cognitiveGraph.getStats() else "CogGraph: not init"

    // ═══════════════════════════════════════════════════════════════
    // Procedural Universe API
    // ═══════════════════════════════════════════════════════════════

    /** Set procedural universe visibility. */
    fun setUniverseVisible(visible: Boolean) {
        if (::proceduralUniverse.isInitialized) proceduralUniverse.isVisible = visible
    }

    /** Whether the procedural universe is visible. */
    val isUniverseVisible: Boolean get() =
        if (::proceduralUniverse.isInitialized) proceduralUniverse.isVisible else false

    /** Get procedural universe stats for debugging. */
    val universeStats: String get() =
        if (::proceduralUniverse.isInitialized) proceduralUniverse.getStats() else "Universe: not init"

    // ═══════════════════════════════════════════════════════════════
    // Audio Resonance API
    // ═══════════════════════════════════════════════════════════════

    /** Push audio analysis data from any thread. */
    fun pushAudioState(snapshot: AudioResonanceSnapshot) {
        audioResonanceBridge.pushState(snapshot)
    }

    /** Set audio-to-cognitive blending weights. */
    fun setAudioBlendWeights(audioWeight: Float, cognitiveWeight: Float) {
        audioResonanceController.audioWeight = audioWeight.coerceIn(0f, 1f)
        audioResonanceController.cognitiveWeight = cognitiveWeight.coerceIn(0f, 1f)
    }

    /** Whether audio is currently active and driving visuals. */
    val isAudioResonanceActive: Boolean get() = audioResonanceController.output.isAudioActive

    /** Get the audio visual output for external consumers. */
    val audioVisualOutput get() = audioResonanceController.output

    // ═══════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════

    /**
     * Release all GL resources. MUST be called from GL thread
     * (e.g., via GLSurfaceView.queueEvent).
     */
    fun release() {
        for (pass in passes.reversed()) {
            pass.release()
        }
        passes.clear()
        immersive.release()
        isInitialized = false
        Timber.i("RenderEngine: all resources released")
    }
}

