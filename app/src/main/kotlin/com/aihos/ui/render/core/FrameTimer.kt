package com.aihos.ui.render.core

import timber.log.Timber

/**
 * FrameTimer — Zero-allocation frame timing utility.
 *
 * Provides:
 *   - deltaTime: seconds since last frame (clamped)
 *   - elapsedTime: seconds since initialization
 *   - currentFPS: instantaneous frames per second
 *   - averageFPS: rolling average over 2 seconds
 *
 * Uses a pre-allocated ring buffer. No GC pressure.
 */
class FrameTimer {

    private val frameTimes = FloatArray(RING_SIZE)
    private var ringIndex = 0
    private var ringFilled = false

    private var frameStartNs = 0L
    private var lastFrameNs = 0L
    private var initTimeNs = 0L
    private var qualityCheckCounter = 0

    var deltaTime: Float = 0.016f
        private set
    var elapsedTime: Float = 0f
        private set
    var currentFPS: Float = 60f
        private set
    var averageFPS: Float = 60f
        private set
    var qualityLevel: QualityLevel = QualityLevel.HIGH
        private set

    fun beginFrame() {
        frameStartNs = System.nanoTime()
        if (initTimeNs == 0L) {
            initTimeNs = frameStartNs
            lastFrameNs = frameStartNs
        }
        deltaTime = ((frameStartNs - lastFrameNs) / 1_000_000_000f).coerceIn(MIN_DT, MAX_DT)
        elapsedTime = (frameStartNs - initTimeNs) / 1_000_000_000f
    }

    fun endFrame() {
        val now = System.nanoTime()
        val frameMs = (now - frameStartNs) / 1_000_000f

        frameTimes[ringIndex] = frameMs
        ringIndex = (ringIndex + 1) % RING_SIZE
        if (ringIndex == 0) ringFilled = true

        currentFPS = if (frameMs > 0f) 1000f / frameMs else 60f

        val count = if (ringFilled) RING_SIZE else ringIndex.coerceAtLeast(1)
        var sum = 0f
        for (i in 0 until count) sum += frameTimes[i]
        averageFPS = if (sum > 0f) 1000f * count / sum else 60f

        lastFrameNs = frameStartNs

        qualityCheckCounter++
        if (qualityCheckCounter >= QUALITY_CHECK_INTERVAL) {
            qualityCheckCounter = 0
            adjustQuality()
        }
    }

    private fun adjustQuality() {
        val prev = qualityLevel
        qualityLevel = when {
            averageFPS >= 55f -> QualityLevel.HIGH
            averageFPS >= 40f -> QualityLevel.MEDIUM
            else -> QualityLevel.LOW
        }
        if (qualityLevel != prev) {
            Timber.i("FrameTimer: quality $prev -> $qualityLevel (avg ${averageFPS.toInt()} FPS)")
        }
    }

    fun forceQuality(level: QualityLevel) {
        qualityLevel = level
    }

    fun buildRenderState(screenWidth: Int, screenHeight: Int, aiMetrics: AIMetricsSnapshot): RenderState {
        return RenderState(
            elapsedTime = elapsedTime,
            deltaTime = deltaTime,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            qualityLevel = qualityLevel,
            aiMetrics = aiMetrics
        )
    }

    fun reset() {
        frameTimes.fill(0f)
        ringIndex = 0
        ringFilled = false
        lastFrameNs = 0L
        initTimeNs = 0L
        qualityCheckCounter = 0
        qualityLevel = QualityLevel.HIGH
    }

    fun getStats(): String =
        "FPS: ${currentFPS.toInt()} avg: ${averageFPS.toInt()} quality: $qualityLevel"

    companion object {
        private const val RING_SIZE = 120
        private const val QUALITY_CHECK_INTERVAL = 60
        private const val MIN_DT = 0.001f
        private const val MAX_DT = 0.1f
    }
}

