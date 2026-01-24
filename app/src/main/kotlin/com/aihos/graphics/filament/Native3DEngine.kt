package com.aihos.graphics.filament

import android.content.Context
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native3DEngine - High-Performance 3D Rendering using Filament
 *
 * This engine renders the AI Core visualization using Google's Filament,
 * a physically-based rendering library optimized for mobile.
 *
 * Optimizations:
 * - Adaptive quality scaling based on device and frame times
 * - Precise frame pacing to maintain target FPS
 * - Lifecycle-aware rendering (pause/resume/destroy)
 * - Time-based animations (no frame-rate dependency)
 * - Efficient memory management and resource cleanup
 *
 * Performance Target:
 * - 60 FPS on mid-range devices (45-60 on lower-end)
 * - <16.67ms frame time with <1ms jitter
 * - Efficient GPU memory usage
 * - Graceful degradation on low-end devices
 *
 * Responsibilities:
 * - Initialize and manage Filament rendering context
 * - Create and manage 3D geometry (AI Core orb)
 * - Apply PBR materials and lighting
 * - Handle lifecycle events (resume/pause/destroy)
 * - Provide procedural animation capabilities
 * - Manage performance and memory efficiently
 */
class Native3DEngine(
    private val context: Context,
    private val surfaceView: SurfaceView,
    private val scope: CoroutineScope
) {
    // ==================== FILAMENT COMPONENTS ====================
    
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private lateinit var camera: Camera
    private lateinit var uiHelper: UiHelper
    
    // ==================== SCENE COMPONENTS ====================
    
    private var entityAICore: Int = 0 // Entity for the AI Core orb
    private var lightEntity: Int = 0  // Main directional light
    private var lightAmbient: Int = 0 // Ambient light
    
    // ==================== MATERIAL & RENDERING ====================
    
    private var aiCoreMaterial: AICoreMaterial? = null
    private var renderingActive = false
    private var renderingJob: Job? = null
    
    // ==================== ANIMATION STATE ====================
    
    private var animationTime: Float = 0f
    private var targetRotation = FloatArray(3) // Target rotation in radians
    private var currentRotation = FloatArray(3) // Current rotation
    private var targetScale = 1.0f
    private var currentScale = 1.0f
    
    // ==================== OPTIMIZATION MANAGERS ====================
    
    private lateinit var qualityManager: AdaptiveQualityManager
    private lateinit var framePacer: FramePacingManager
    private var currentQualitySettings: AdaptiveQualityManager.QualitySettings? = null
    
    // ==================== PERFORMANCE MONITORING ====================
    
    private var frameCounter = 0
    private var lastStatsUpdateNanos = System.nanoTime()
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Initialize the 3D engine and Filament context
     * Must be called on the main thread
     */
    fun initialize() {
        Timber.d("Native3DEngine: Initializing Filament rendering")
        
        try {
            // Initialize quality and frame pacing managers
            qualityManager = AdaptiveQualityManager(context)
            currentQualitySettings = qualityManager.getQualitySettings()
            framePacer = FramePacingManager(currentQualitySettings!!.targetFps)
            
            Timber.i("Native3DEngine: ${qualityManager.getDeviceCapabilitiesReport()}")
            
            // Create Filament engine
            engine = Engine.create()
            renderer = engine.createRenderer()
            scene = engine.createScene()
            view = engine.createView()
            camera = engine.createCamera().apply {
                setProjection(45.0, 16f / 9f, 0.1, 1000.0)
                setPosition(0f, 2f, 4f)
                lookAt(0.0, 0.0, 0.0)
            }
            view.camera = camera
            view.scene = scene
            
            // Set up UI helper for surface management
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                renderCallback = SurfaceCallback()
            }
            uiHelper.attachTo(surfaceView)
            
            // Create materials
            aiCoreMaterial = AICoreMaterial(engine)
            
            // Create scene geometry and lights with quality settings
            createAICoreGeometry()
            createLighting()
            
            // Start rendering loop
            startRendering()
            
            Timber.i("Native3DEngine: Initialization complete (Quality: ${currentQualitySettings!!.qualityLevel})")
        } catch (e: Exception) {
            Timber.e(e, "Native3DEngine: Initialization failed")
            throw e
        }
    }
    
    /**
     * Create the AI Core geometry (central orb)
     * Uses procedurally generated mesh with adaptive quality
     */
    private fun createAICoreGeometry() {
        // Create entity for AI Core
        entityAICore = EntityManager.get().create()
        
        // Use quality-dependent segment count
        val segments = currentQualitySettings?.sphereSegments ?: 32
        
        // Create a sphere mesh (adaptive segments based on quality)
        val sphereMesh = createSphereMesh(engine, radius = 1f, segments = segments)
        
        // Add renderable to entity
        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, sphereMesh)
            .material(0, aiCoreMaterial!!.getMaterial())
            .culling(false)
            .castShadows(true)
            .receiveShadows(true)
            .build(engine, entityAICore)
        
        // Add to scene
        scene.addEntity(entityAICore)
        
        Timber.d("Native3DEngine: AI Core geometry created (segments=$segments, entity=$entityAICore)")
    }
    
    /**
     * Create lighting setup for the scene
     * Adapts quality based on device capabilities
     */
    private fun createLighting() {
        val settings = currentQualitySettings ?: return
        
        // Main directional light (always created)
        if (settings.enableDynamicLighting) {
            lightEntity = EntityManager.get().create()
            val shadowSize = settings.shadowResolution
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1f, 1f, 1f)
                .intensity(50000f) // Lux
                .direction(0.6f, 1f, 0.8f)
                .castShadows(true)
                .shadowMapSize(shadowSize)
                .build(engine, lightEntity)
            scene.addEntity(lightEntity)
            Timber.d("Native3DEngine: Main light created (shadowSize=$shadowSize)")
        }
        
        // Ambient light for fill (quality-dependent)
        if (settings.enableAmbientLight) {
            lightAmbient = EntityManager.get().create()
            LightManager.Builder(LightManager.Type.INDIRECT)
                .intensity(5000f)
                .build(engine, lightAmbient)
            scene.addEntity(lightAmbient)
            Timber.d("Native3DEngine: Ambient light created")
        }
        
        // Skybox (always created)
        Skybox.Builder()
            .color(0.03f, 0.03f, 0.1f, 1f) // Dark blue space
            .build(engine)
            .let { scene.skybox = it }
        
        Timber.d("Native3DEngine: Lighting created (lights: ${if (settings.enableDynamicLighting) 1 else 0} main, ${if (settings.enableAmbientLight) 1 else 0} ambient)")
    }
    
    // ==================== RENDERING LOOP ====================
    
    /**
     * Start the main rendering loop with optimized frame pacing
     * Runs at adaptive FPS on a dedicated coroutine
     */
    private fun startRendering() {
        renderingActive = true
        renderingJob = scope.launch {
            lastStatsUpdateNanos = System.nanoTime()
            
            while (renderingActive && isActive) {
                try {
                    // Begin frame with precise delta time
                    val deltaTime = framePacer.beginFrame()
                    
                    // Update animation state with time-based delta
                    updateAnimation(deltaTime)
                    
                    // Update material pulse animations
                    aiCoreMaterial?.updatePulseAnimation(deltaTime)
                    
                    // Render frame if surface is ready
                    if (uiHelper.isReadyToRender()) {
                        renderFrame()
                    }
                    
                    // Record frame time for adaptive quality
                    qualityManager.recordFrameTime(framePacer.getLastFrameTimeNanos())
                    frameCounter++
                    
                    // Check if quality adjustment needed (every 3 seconds)
                    val now = System.nanoTime()
                    if (now - lastStatsUpdateNanos > 3_000_000_000L) {
                        val stats = framePacer.getFrameStats()
                        val oldQuality = currentQualitySettings?.qualityLevel
                        
                        // Get updated quality settings
                        currentQualitySettings = qualityManager.getQualitySettings()
                        if (currentQualitySettings?.qualityLevel != oldQuality) {
                            Timber.i("Native3DEngine: Quality changed to ${currentQualitySettings?.qualityLevel} (P95=${stats.p95FrameTimeMs.toInt()}ms, score=${qualityManager.getPerformanceScore()})")
                            // Could trigger geometry/light rebuild here if needed
                        }
                        
                        Timber.d(framePacer.getDebugString())
                        lastStatsUpdateNanos = now
                    }
                    
                    // Maintain target frame rate
                    framePacer.endFrame()
                    
                } catch (e: Exception) {
                    Timber.e(e, "Native3DEngine: Rendering error")
                }
            }
        }
    }
    
    /**
     * Render a single frame
     * Called on rendering thread
     */
    private fun renderFrame() {
        if (!renderer.beginFrame(uiHelper.swapChain)) {
            return
        }
        
        renderer.render(view)
        renderer.endFrame()
    }
    
    /**
     * Update procedural animation based on elapsed time (not frame count)
     * Time-based ensures consistency across all frame rates
     */
    private fun updateAnimation(deltaTime: Float) {
        // Clamp delta to prevent large jumps (e.g., after pause)
        val clampedDelta = deltaTime.coerceAtMost(0.033f)  // Max 33ms
        animationTime += clampedDelta
        
        // Smooth interpolation towards target values
        val lerpFactor = (clampedDelta * 3f).coerceAtMost(1f) // Smooth but responsive
        
        // Update rotation with time-based easing
        for (i in 0..2) {
            currentRotation[i] += (targetRotation[i] - currentRotation[i]) * lerpFactor
        }
        
        // Update scale with time-based easing
        currentScale += (targetScale - currentScale) * lerpFactor
        
        // Apply transformations to entity
        TransformManager.getInstance().setTransform(
            EntityManager.get(),
            entityAICore,
            floatArrayOf(
                cos(currentRotation[1]) * cos(currentRotation[2]),
                sin(currentRotation[0]),
                sin(currentRotation[1]) * cos(currentRotation[2]),
                0f,
                
                cos(currentRotation[1]) * sin(currentRotation[2]),
                cos(currentRotation[0]) * cos(currentRotation[2]) - sin(currentRotation[0]) * sin(currentRotation[1]) * sin(currentRotation[2]),
                -sin(currentRotation[0]) * cos(currentRotation[2]) - cos(currentRotation[0]) * sin(currentRotation[1]) * sin(currentRotation[2]),
                0f,
                
                -sin(currentRotation[1]),
                sin(currentRotation[0]) * cos(currentRotation[1]),
                cos(currentRotation[0]) * cos(currentRotation[1]),
                0f,
                
                0f, 0f, 0f, 1f
            )
        )
        
        // Update scale
        val matrixScale = floatArrayOf(
            currentScale, 0f, 0f, 0f,
            0f, currentScale, 0f, 0f,
            0f, 0f, currentScale, 0f,
            0f, 0f, 0f, 1f
        )
        TransformManager.getInstance().setTransform(
            EntityManager.get(),
            entityAICore,
            matrixScale
        )
    }
    
    // ==================== PROCEDURAL ANIMATION API ====================
    
    /**
     * Set rotation animation target
     * rotationX, rotationY, rotationZ in radians
     */
    fun setRotationTarget(rotationX: Float, rotationY: Float, rotationZ: Float) {
        targetRotation[0] = rotationX
        targetRotation[1] = rotationY
        targetRotation[2] = rotationZ
    }
    
    /**
     * Set scale animation target
     * Default scale is 1.0
     */
    fun setScaleTarget(scale: Float) {
        targetScale = maxOf(0.5f, minOf(2f, scale))
    }
    
    /**
     * Get the AI Core material for state-based updates
     */
    fun getAICoreMaterial(): AICoreMaterial? = aiCoreMaterial
    
    /**
     * Update light intensity based on AI state
     * Used to indicate cognitive activity
     */
    fun setLightIntensity(intensity: Float) {
        if (::engine.isInitialized && lightEntity != 0) {
            LightManager.getInstance().setIntensity(lightEntity, intensity)
        }
    }
    
    /**
     * Get current frame rate statistics
     */
    fun getFrameStats(): FramePacingManager.FrameStats? {
        return if (::framePacer.isInitialized) framePacer.getFrameStats() else null
    }
    
    /**
     * Get current quality level
     */
    fun getCurrentQualityLevel(): AdaptiveQualityManager.QualityLevel? {
        return if (::qualityManager.isInitialized) qualityManager.getCurrentQualityLevel() else null
    }
    
    /**
     * Get device capabilities report for debugging
     */
    fun getDeviceCapabilitiesReport(): String {
        return if (::qualityManager.isInitialized) {
            qualityManager.getDeviceCapabilitiesReport()
        } else {
            "Quality manager not initialized"
        }
    }
    
    /**
     * Get total frames rendered since initialization
     */
    fun getFrameCount(): Int = frameCounter
    
    // ==================== LIFECYCLE ====================
    
    /**
     * Resume rendering (called when activity resumes)
     */
    fun resume() {
        Timber.d("Native3DEngine: Resume")
        renderingActive = true
        if (renderingJob?.isCompleted != false) {
            startRendering()
        }
    }
    
    /**
     * Pause rendering (called when activity pauses)
     * Preserves state for quick resume
     */
    fun pause() {
        Timber.d("Native3DEngine: Pause")
        renderingActive = false
    }
    
    /**
     * Destroy the engine and clean up resources
     * Called when activity is destroyed
     */
    fun destroy() {
        Timber.d("Native3DEngine: Destroying")
        
        try {
            // Stop rendering loop
            renderingActive = false
            renderingJob?.cancel()
            
            // Detach UI helper
            if (::uiHelper.isInitialized) {
                uiHelper.detach()
            }
            
            // Clean up scene entities
            if (entityAICore != 0) {
                scene.remove(entityAICore)
                EntityManager.get().destroy(entityAICore)
            }
            if (lightEntity != 0) {
                scene.remove(lightEntity)
                EntityManager.get().destroy(lightEntity)
            }
            if (lightAmbient != 0) {
                scene.remove(lightAmbient)
                EntityManager.get().destroy(lightAmbient)
            }
            
            // Clean up materials
            aiCoreMaterial?.destroy()
            
            // Destroy Filament objects
            if (::camera.isInitialized) {
                engine.destroyCameraComponent(camera)
            }
            if (::view.isInitialized) {
                engine.destroyView(view)
            }
            if (::scene.isInitialized) {
                engine.destroyScene(scene)
            }
            if (::renderer.isInitialized) {
                engine.destroyRenderer(renderer)
            }
            if (::engine.isInitialized) {
                engine.destroy()
            }
            
            Timber.i("Native3DEngine: Destroyed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Native3DEngine: Error during destroy")
        }
    }
    
    // ==================== SURFACE CALLBACK ====================
    
    /**
     * Handles surface lifecycle events from Filament
     */
    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: android.view.Surface) {
            uiHelper.setSwapChain(surface)
        }
        
        override fun onDetachedFromSurface() {
            uiHelper.detachSwapChain()
        }
    }
    
    // ==================== HELPERS ====================
    
    companion object {
        /**
         * Create a sphere mesh procedurally
         */
        fun createSphereMesh(
            engine: Engine,
            radius: Float = 1f,
            segments: Int = 32
        ): VertexBuffer {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Short>()
            
            // Generate vertices
            for (lat in 0..segments) {
                val theta = lat * PI.toFloat() / segments
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)
                
                for (lon in 0..segments) {
                    val phi = lon * 2 * PI.toFloat() / segments
                    val sinPhi = sin(phi)
                    val cosPhi = cos(phi)
                    
                    // Position
                    vertices.add(radius * sinTheta * cosPhi)
                    vertices.add(radius * cosTheta)
                    vertices.add(radius * sinTheta * sinPhi)
                    
                    // Normal (for sphere, normal = position / radius)
                    vertices.add(sinTheta * cosPhi)
                    vertices.add(cosTheta)
                    vertices.add(sinTheta * sinPhi)
                }
            }
            
            // Generate indices
            for (lat in 0 until segments) {
                for (lon in 0 until segments) {
                    val first = (lat * (segments + 1) + lon).toShort()
                    val second = (first.toInt() + segments + 1).toShort()
                    
                    indices.add(first)
                    indices.add(second)
                    indices.add((first.toInt() + 1).toShort())
                    
                    indices.add(second)
                    indices.add((second.toInt() + 1).toShort())
                    indices.add((first.toInt() + 1).toShort())
                }
            }
            
            // Create vertex buffer
            return VertexBuffer.Builder()
                .vertexCount(vertices.size / 6)
                .bufferCount(1)
                .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 24)
                .attribute(VertexAttribute.NORMAL, 0, VertexBuffer.AttributeType.FLOAT3, 12, 24)
                .build(engine)
                .also { vb ->
                    vb.setBufferAt(engine, 0, FloatBuffer.wrap(vertices.toFloatArray()))
                }
        }
    }
}
