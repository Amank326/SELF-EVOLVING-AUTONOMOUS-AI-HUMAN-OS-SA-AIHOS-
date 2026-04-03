package com.aihos.ui.gl

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import timber.log.Timber

/**
 * CinematicGLSurfaceView — Enhanced GLSurfaceView for the SA-AIHOS cinematic pipeline.
 *
 * Features:
 *   - OpenGL ES 3.0 context with RGBA8888 + Depth24
 *   - Automatic ES 3.0 capability detection
 *   - Thread-safe AI state input via [updateAIState]
 *   - Quality preset control
 *   - Transparent background support (GL behind WebView)
 *
 * Usage from Activity:
 *   val glView = CinematicGLSurfaceView(context)
 *   layout.addView(glView)
 *   // In metrics pump:
 *   glView.updateAIState(cognitiveLoad, confidence, ...)
 *   // Lifecycle:
 *   glView.onPause() / glView.onResume()
 *   glView.release()
 */
class CinematicGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val cinematicRenderer: CinematicRenderer
    private val fallbackRenderer: NeuralGLRenderer

    /** True only if the device actually supports ES 3.0. */
    val isES30Supported: Boolean

    /** Whether cinematic mode is active (vs fallback particle mode). */
    var isCinematicMode: Boolean = true
        private set

    init {
        isES30Supported = detectES30(context)

        cinematicRenderer = CinematicRenderer()
        fallbackRenderer = NeuralGLRenderer()

        if (isES30Supported) {
            // Request ES 3.0 context
            setEGLContextClientVersion(3)

            // RGBA8 + Depth24 + Stencil0
            setEGLConfigChooser(8, 8, 8, 8, 24, 0)

            // Translucent surface so WebView content behind is visible
            setZOrderOnTop(false)
            holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

            // Default to cinematic renderer
            setRenderer(cinematicRenderer)

            // Continuous rendering for smooth animation
            renderMode = RENDERMODE_CONTINUOUSLY

            Timber.i("CinematicGLSurfaceView: ES 3.0 cinematic context created")
        } else {
            Timber.w("CinematicGLSurfaceView: ES 3.0 NOT supported, GL layer disabled")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AI State Input
    // ═══════════════════════════════════════════════════════════════

    /**
     * Push AI system metrics to the renderer. Safe to call from any thread.
     *
     * @param cognitiveLoad   [0..1] Reasoning/processing intensity
     * @param confidence      [0..1] System confidence in its decisions
     * @param evolutionRate   [0..1] Rate of self-evolution/learning
     * @param selfAwareness   [0..1] Depth of self-reflection
     * @param autonomyLevel   [0..1] Level of autonomous operation
     * @param systemHealth    [0..1] Overall system health
     * @param memoryLoad      [0..1] Memory utilization
     * @param animationIntensity [0..1] Animation energy level
     */
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
        if (isCinematicMode) {
            cinematicRenderer.updateAIState(
                cognitiveLoad = cognitiveLoad,
                confidence = confidence,
                evolutionRate = evolutionRate,
                selfAwareness = selfAwareness,
                autonomyLevel = autonomyLevel,
                systemHealth = systemHealth,
                memoryLoad = memoryLoad,
                animationIntensity = animationIntensity
            )
        } else {
            // Fallback: map to simpler NeuralGLRenderer metrics
            fallbackRenderer.confidence = confidence
            fallbackRenderer.memoryLoad = memoryLoad
            fallbackRenderer.autonomyLevel = autonomyLevel
            fallbackRenderer.systemHealth = systemHealth
        }
    }

    /**
     * Convenience: update from a full AISystemState-like map.
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
        updateAIState(
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
    // Quality Control
    // ═══════════════════════════════════════════════════════════════

    /**
     * Force a quality preset on the cinematic renderer.
     */
    fun setQualityPreset(level: PerformanceMonitor.QualityLevel) {
        if (isCinematicMode) {
            cinematicRenderer.forceQuality(level)
        }
    }

    /**
     * Get current performance stats.
     */
    fun getPerformanceStats(): String {
        return if (isCinematicMode) {
            cinematicRenderer.getPerformanceStats()
        } else {
            "Fallback mode (NeuralGLRenderer)"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    fun release() {
        if (isES30Supported) {
            queueEvent {
                if (isCinematicMode) {
                    cinematicRenderer.release()
                } else {
                    fallbackRenderer.release()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ES 3.0 Detection
    // ═══════════════════════════════════════════════════════════════

    private fun detectES30(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = am?.deviceConfigurationInfo ?: return false
        return info.reqGlEsVersion >= 0x00030000
    }
}

