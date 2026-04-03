package com.aihos.ui.render

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.immersive.GyroscopeTracker
import timber.log.Timber

/**
 * RenderSurfaceView — Production GLSurfaceView backed by RenderEngine.
 *
 * Drop-in replacement for CinematicGLSurfaceView with proper
 * lifecycle, thread-safety, and modular architecture.
 *
 * Usage:
 *   val view = RenderSurfaceView(context)
 *   layout.addView(view)
 *
 *   // From any thread:
 *   view.pushAIState(cognitiveLoad = 0.7f, confidence = 0.9f)
 *
 *   // Lifecycle:
 *   view.onPause() / view.onResume() / view.release()
 */
class RenderSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val engine: RenderEngine
    val isES30Supported: Boolean
    private var gyroTracker: GyroscopeTracker? = null

    init {
        isES30Supported = detectES30(context)
        engine = RenderEngine()

        if (isES30Supported) {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 24, 0)
            setZOrderOnTop(false)
            holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
            setRenderer(engine)
            renderMode = RENDERMODE_CONTINUOUSLY

            // Initialize gyroscope tracker for immersive mode
            gyroTracker = GyroscopeTracker(context)
            engine.immersive.gyroTracker = gyroTracker

            Timber.i("RenderSurfaceView: ES 3.0 engine initialized")
        } else {
            Timber.w("RenderSurfaceView: ES 3.0 NOT supported")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AI State API (thread-safe, call from any thread)
    // ═══════════════════════════════════════════════════════════════

    fun pushAIState(snapshot: AIMetricsSnapshot) {
        engine.pushAIState(snapshot)
    }

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
        engine.pushAIState(
            cognitiveLoad, confidence, evolutionRate, selfAwareness,
            autonomyLevel, systemHealth, memoryLoad, animationIntensity
        )
    }

    /**
     * Convenience for pushing from AISystemState.
     */
    fun updateFromSystemState(
        overallConfidence: Float,
        memoryLoad: Float,
        autonomyLevel: Float,
        systemHealth: Float,
        reasoningComplexity: Float = 0.5f,
        bestFitness: Float = 0f,
        selfAwareness: Float = 0.5f,
        animationIntensity: Float = 0.5f
    ) {
        pushAIState(
            cognitiveLoad = reasoningComplexity,
            confidence = overallConfidence,
            evolutionRate = bestFitness,
            selfAwareness = selfAwareness,
            autonomyLevel = autonomyLevel,
            systemHealth = systemHealth,
            memoryLoad = memoryLoad,
            animationIntensity = animationIntensity
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Quality / diagnostics
    // ═══════════════════════════════════════════════════════════════

    fun setQualityPreset(level: QualityLevel) = engine.forceQuality(level)
    fun getPerformanceStats(): String = engine.getPerformanceStats()

    // ═══════════════════════════════════════════════════════════════
    // Immersive Depth Mode API
    // ═══════════════════════════════════════════════════════════════

    /** Enable/disable immersive depth mode. */
    fun setImmersiveMode(enabled: Boolean) = engine.setImmersiveMode(enabled)

    /** Toggle immersive mode on/off. */
    fun toggleImmersiveMode() = engine.toggleImmersiveMode()

    /** Adjust stereo intensity [0..1]. */
    fun setStereoIntensity(intensity: Float) = engine.setStereoIntensity(intensity)

    /** Set stereo mode: 0=side-by-side, 1=anaglyph, 2=subtle blend. */
    fun setStereoMode(mode: Int) = engine.setStereoMode(mode)

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    fun release() {
        if (isES30Supported) {
            gyroTracker?.stop()
            queueEvent { engine.release() }
        }
    }

    /**
     * Call from Activity.onResume(). Starts gyroscope if immersive mode is on.
     */
    override fun onResume() {
        super.onResume()
        if (engine.isImmersiveMode && engine.immersive.config.headTrackingEnabled) {
            gyroTracker?.start(engine.immersive.config)
        }
    }

    /**
     * Call from Activity.onPause(). Stops gyroscope.
     */
    override fun onPause() {
        gyroTracker?.stop()
        super.onPause()
    }

    private fun detectES30(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return (am?.deviceConfigurationInfo?.reqGlEsVersion ?: 0) >= 0x00030000
    }
}

