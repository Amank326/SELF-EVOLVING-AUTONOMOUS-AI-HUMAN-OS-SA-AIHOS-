package com.aihos.rendering.lighting

import timber.log.Timber
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// PIPELINE INTEGRATION — Glue between the existing multi-pass renderer and
// the new volumetric lighting system.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Performance budget tracker.
 *  
 * Monitors frame timing and provides real-time recommendations to the
 * volumetric engine for quality adjustments.
 */
class PerformanceBudget(
    private val targetFps: Int = 60,
    private val historySize: Int = 30
) {
    private val frameTimes = FloatArray(historySize)
    private var index = 0
    private var frameCount = 0

    val targetFrameMs = 1000f / targetFps
    val budgetMs: Float get() = targetFrameMs * 0.25f   // Volumetric gets 25% of budget

    fun recordFrame(deltaMs: Float) {
        frameTimes[index % historySize] = deltaMs
        index++
        frameCount++
    }

    fun averageFrameMs(): Float {
        val count = minOf(frameCount, historySize)
        if (count == 0) return targetFrameMs
        return frameTimes.take(count).average().toFloat()
    }

    fun currentFps(): Float = 1000f / averageFrameMs().coerceAtLeast(1f)
    fun isOverBudget(): Boolean = averageFrameMs() > targetFrameMs * 1.1f
    fun isUnderBudget(): Boolean = averageFrameMs() < targetFrameMs * 0.85f

    fun recommendedQuality(): VolumetricQuality {
        val fps = currentFps()
        return when {
            fps >= 58f -> VolumetricQuality.HIGH
            fps >= 50f -> VolumetricQuality.MEDIUM
            fps >= 40f -> VolumetricQuality.LOW
            else       -> VolumetricQuality.MINIMAL
        }
    }
}

/**
 * Manages the integration of the volumetric lighting engine with the
 * existing SA-AIHOS multi-pass renderer.
 * 
 * Usage in GLSurfaceView.Renderer:
 * ```
 * class SAIHOSRenderer : GLSurfaceView.Renderer {
 *     private lateinit var integration: VolumetricPipelineIntegration
 *     
 *     override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
 *         integration = VolumetricPipelineIntegration(width, height)
 *         integration.initialize()
 *     }
 *     
 *     override fun onDrawFrame(gl: GL10?) {
 *         val dt = calculateDeltaTime()
 *         
 *         // 1. Render scene to FBO (your existing passes)
 *         renderSceneToFBO()  
 *         
 *         // 2. Run volumetric lighting
 *         integration.onFrameBegin(dt, viewProjectionMatrix)
 *         integration.render(sceneColorTexture, sceneDepthTexture)
 *         
 *         // 3. Render HUD overlay on top
 *         renderHUD()
 *     }
 * }
 * ```
 */
class VolumetricPipelineIntegration(
    private val screenWidth: Int,
    private val screenHeight: Int
) {
    private lateinit var engine: VolumetricLightingEngine
    private val performanceBudget = PerformanceBudget(targetFps = 60)

    // Cached VP matrix
    private var vpMatrix = FloatArray(16)

    fun initialize() {
        engine = VolumetricLightingEngine(screenWidth, screenHeight)
        engine.initialize()
        setupDefaultLights()
    }

    private fun setupDefaultLights() {
        // AI Core — central pulsating light
        engine.addLight(LightFactory.createAICoreLight(Vec3(0f, 0f, 0f)))

        // Cosmic directional — distant star illumination
        engine.addLight(LightFactory.createCosmicDirectional())

        // Three orbiting energy lights (120° apart)
        for (i in 0..2) {
            val angle = i * (2f * PI.toFloat() / 3f)
            engine.addLight(
                LightFactory.createOrbitingEnergy(
                    colorPreset = when (i) {
                        0 -> LightColor.NEURAL_VIOLET
                        1 -> LightColor.ENERGY_GOLD
                        else -> LightColor.CORE_CYAN
                    },
                    orbitRadius = 5f + i * 1.5f,
                    initialAngle = angle
                )
            )
        }

        Timber.i("Default volumetric lights configured (${engine.getLights().size} lights)")
    }

    /**
     * Called at the start of each frame before rendering.
     */
    fun onFrameBegin(deltaTime: Float, viewProjectionMatrix: FloatArray) {
        vpMatrix = viewProjectionMatrix
        performanceBudget.recordFrame(deltaTime * 1000f)
        engine.update(deltaTime, vpMatrix)
    }

    /**
     * Execute all volumetric rendering passes.
     */
    fun render(sceneColorTexture: Int, sceneDepthTexture: Int) {
        engine.render(sceneColorTexture, sceneDepthTexture)
    }

    /**
     * Notify the engine of AI cognitive activity (0.0 = idle, 1.0 = max).
     * Wire this to the AutonomyController or ReasoningEngine output.
     */
    fun updateAIActivity(level: Float) {
        engine.setAIActivityLevel(level)
    }

    /**
     * Trigger visual energy pulse — call this from cognitive spike events.
     */
    fun triggerCognitivePulse(intensity: Float = 1.0f) {
        engine.triggerEnergyPulse(intensity)
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        engine.onSurfaceChanged(width, height)
    }

    fun destroy() {
        engine.destroy()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// MATH UTILITIES used by the lighting pipeline
// ─────────────────────────────────────────────────────────────────────────────

object LightingMath {

    /**
     * Project world position to screen UV [0..1].
     */
    fun worldToScreen(pos: Vec3, vpMatrix: FloatArray): FloatArray {
        val cx = vpMatrix[0]*pos.x + vpMatrix[4]*pos.y + vpMatrix[8]*pos.z + vpMatrix[12]
        val cy = vpMatrix[1]*pos.x + vpMatrix[5]*pos.y + vpMatrix[9]*pos.z + vpMatrix[13]
        val cw = vpMatrix[3]*pos.x + vpMatrix[7]*pos.y + vpMatrix[11]*pos.z + vpMatrix[15]
        return if (cw > 0.0001f) {
            floatArrayOf((cx / cw) * 0.5f + 0.5f, (cy / cw) * 0.5f + 0.5f)
        } else {
            floatArrayOf(0.5f, 0.5f)
        }
    }

    /**
     * Compute attenuation at a given distance.
     */
    fun computeAttenuation(distance: Float, att: Attenuation): Float {
        return 1.0f / (att.constant + att.linear * distance + att.quadratic * distance * distance)
    }

    /**
     * Smooth pulse function for animation.
     * Returns value in [0, 1] with configurable attack/decay.
     */
    fun smoothPulse(t: Float, attack: Float = 0.1f, sustain: Float = 0.3f, decay: Float = 0.6f): Float {
        val total = attack + sustain + decay
        val norm = (t % total) / total
        return when {
            norm < attack / total -> norm / (attack / total)                // Attack
            norm < (attack + sustain) / total -> 1.0f                      // Sustain
            else -> 1.0f - (norm - (attack + sustain) / total) / (decay / total)  // Decay
        }.coerceIn(0f, 1f)
    }
}
