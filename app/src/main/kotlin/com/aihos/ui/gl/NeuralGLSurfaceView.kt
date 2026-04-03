package com.aihos.ui.gl

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import timber.log.Timber

/**
 * NeuralGLSurfaceView — GLSurfaceView pre-configured for OpenGL ES 3.0.
 *
 * Usage from Activity / Fragment:
 *     val glView = NeuralGLSurfaceView(context)
 *     // parent layout adds it, then:
 *     glView.updateMetrics(confidence, memoryLoad, autonomy, health)
 *
 * Lifecycle: call onPause() / onResume() from the hosting Activity.
 */
class NeuralGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: NeuralGLRenderer

    /** True only if the device actually supports ES 3.0. */
    val isES30Supported: Boolean

    init {
        isES30Supported = detectES30(context)

        if (isES30Supported) {
            // Request ES 3.0 context
            setEGLContextClientVersion(3)

            // Translucent surface so WebView behind it is visible
            setZOrderOnTop(false)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)  // RGBA8 + depth16
            holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

            renderer = NeuralGLRenderer()
            setRenderer(renderer)

            // Only render when we request it (saves battery), but for
            // continuous animation we use CONTINUOUSLY
            renderMode = RENDERMODE_CONTINUOUSLY

            Timber.i("NeuralGLSurfaceView: ES 3.0 context created ✓")
        } else {
            renderer = NeuralGLRenderer()  // never used but field must init
            Timber.w("NeuralGLSurfaceView: ES 3.0 NOT supported, GL layer disabled")
        }
    }

    // ── Public API for feeding AI metrics into the renderer ──────────

    /**
     * Push new AI system metrics to the GL renderer.
     * Safe to call from any thread (renderer reads volatiles).
     */
    fun updateMetrics(
        confidence: Float,
        memoryLoad: Float,
        autonomyLevel: Float,
        systemHealth: Float
    ) {
        renderer.confidence = confidence.coerceIn(0f, 1f)
        renderer.memoryLoad = memoryLoad.coerceIn(0f, 1f)
        renderer.autonomyLevel = autonomyLevel.coerceIn(0f, 1f)
        renderer.systemHealth = systemHealth.coerceIn(0f, 1f)
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    fun release() {
        if (isES30Supported) {
            queueEvent { renderer.release() }
        }
    }

    // ── ES 3.0 detection ─────────────────────────────────────────────

    private fun detectES30(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = am?.deviceConfigurationInfo ?: return false
        return info.reqGlEsVersion >= 0x00030000
    }
}

