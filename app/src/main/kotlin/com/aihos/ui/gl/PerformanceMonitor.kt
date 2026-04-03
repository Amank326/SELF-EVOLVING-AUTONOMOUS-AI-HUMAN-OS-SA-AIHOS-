package com.aihos.ui.gl

import timber.log.Timber

/**
 * PerformanceMonitor — Zero-allocation FPS tracker with dynamic quality scaling.
 *
 * Call [beginFrame] at the start and [endFrame] at the end of onDrawFrame().
 * Reads [qualityLevel] to adjust rendering fidelity.
 *
 * Quality tiers:
 *   HIGH   — full-res bloom, 4 blur passes, all effects
 *   MEDIUM — half-res bloom, 2 blur passes, reduced particles
 *   LOW    — no bloom, minimal effects, 30 FPS target
 *
 * No allocations per frame. Pure arithmetic.
 */
class PerformanceMonitor {

    enum class QualityLevel { HIGH, MEDIUM, LOW }

    // ── Ring buffer for frame times (pre-allocated) ──────────────
    private val frameTimes = FloatArray(RING_SIZE)
    private var ringIndex = 0
    private var ringFilled = false

    // ── Timing ───────────────────────────────────────────────────
    private var frameStartNs = 0L
    private var lastFrameNs = 0L
    var deltaTime: Float = 0.016f       // seconds since last frame
        private set
    var elapsedTime: Float = 0f         // total seconds since init
        private set
    private var initTimeNs = System.nanoTime()

    // ── FPS ──────────────────────────────────────────────────────
    var currentFPS: Float = 60f
        private set
    var averageFPS: Float = 60f
        private set

    // ── Quality ──────────────────────────────────────────────────
    var qualityLevel: QualityLevel = QualityLevel.HIGH
        private set
    private var qualityCheckCounter = 0

    // ── Bloom control ────────────────────────────────────────────
    /** Number of blur passes the post-processing pipeline should use. */
    val bloomPasses: Int
        get() = when (qualityLevel) {
            QualityLevel.HIGH -> 4
            QualityLevel.MEDIUM -> 2
            QualityLevel.LOW -> 0
        }

    /** FBO resolution scale factor (1.0 = full resolution). */
    val resolutionScale: Float
        get() = when (qualityLevel) {
            QualityLevel.HIGH -> 1.0f
            QualityLevel.MEDIUM -> 0.75f
            QualityLevel.LOW -> 0.5f
        }

    /** Whether bloom should be enabled at all. */
    val bloomEnabled: Boolean
        get() = qualityLevel != QualityLevel.LOW

    // ── Frame lifecycle ──────────────────────────────────────────

    fun beginFrame() {
        frameStartNs = System.nanoTime()
        if (lastFrameNs == 0L) {
            lastFrameNs = frameStartNs
            initTimeNs = frameStartNs
        }
        deltaTime = ((frameStartNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        elapsedTime = (frameStartNs - initTimeNs) / 1_000_000_000f
    }

    fun endFrame() {
        val now = System.nanoTime()
        val frameMs = (now - frameStartNs) / 1_000_000f

        // Write to ring buffer
        frameTimes[ringIndex] = frameMs
        ringIndex = (ringIndex + 1) % RING_SIZE
        if (ringIndex == 0) ringFilled = true

        // Compute FPS
        currentFPS = if (frameMs > 0f) 1000f / frameMs else 60f

        val count = if (ringFilled) RING_SIZE else ringIndex.coerceAtLeast(1)
        var sum = 0f
        for (i in 0 until count) {
            sum += frameTimes[i]
        }
        val avgMs = sum / count
        averageFPS = if (avgMs > 0f) 1000f / avgMs else 60f

        lastFrameNs = frameStartNs

        // Quality scaling check every N frames
        qualityCheckCounter++
        if (qualityCheckCounter >= QUALITY_CHECK_INTERVAL) {
            qualityCheckCounter = 0
            adjustQuality()
        }
    }

    // ── Dynamic quality adjustment ───────────────────────────────

    private fun adjustQuality() {
        val prev = qualityLevel
        qualityLevel = when {
            averageFPS >= 55f -> QualityLevel.HIGH
            averageFPS >= 40f -> QualityLevel.MEDIUM
            else -> QualityLevel.LOW
        }
        if (qualityLevel != prev) {
            Timber.i("PerformanceMonitor: quality $prev -> $qualityLevel (avg ${averageFPS.toInt()} FPS)")
        }
    }

    /** Force a specific quality level (overrides auto-scaling). */
    fun forceQuality(level: QualityLevel) {
        qualityLevel = level
        Timber.i("PerformanceMonitor: quality forced to $level")
    }

    // ── Diagnostics ──────────────────────────────────────────────

    fun getStats(): String {
        return "FPS: ${currentFPS.toInt()} avg: ${averageFPS.toInt()} quality: $qualityLevel"
    }

    fun reset() {
        frameTimes.fill(0f)
        ringIndex = 0
        ringFilled = false
        lastFrameNs = 0L
        qualityCheckCounter = 0
        qualityLevel = QualityLevel.HIGH
    }

    companion object {
        private const val RING_SIZE = 120           // ~2 seconds at 60 FPS
        private const val QUALITY_CHECK_INTERVAL = 60  // check every 60 frames
    }
}

