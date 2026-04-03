package com.aihos.ui.render.datagraph

import com.aihos.ui.render.core.AIMetricsSnapshot

/**
 * EvolutionTimeline — Pre-allocated ring buffer recording AI evolution history.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Evolution Timeline Architecture                                 │
 * │                                                                  │
 * │  Ring buffer of CAPACITY samples, each storing:                  │
 * │    timestamp    — wall clock time (seconds)                      │
 * │    confidence   — AI confidence at that moment                   │
 * │    complexity   — decision complexity at that moment             │
 * │    evolutionRate— evolution rate at that moment                  │
 * │    nodeCount    — active cognitive graph nodes                   │
 * │                                                                  │
 * │  Sampled every SAMPLE_INTERVAL seconds (default 0.5s).          │
 * │  Oldest entries overwritten when buffer is full.                 │
 * │                                                                  │
 * │  Used by HUD panel to render a floating evolution history graph. │
 * │  Zero allocation after init.                                     │
 * └──────────────────────────────────────────────────────────────────┘
 */
class EvolutionTimeline {

    companion object {
        const val CAPACITY = 256
        const val FIELDS_PER_SAMPLE = 5
        const val SAMPLE_INTERVAL = 0.5f // seconds between samples
    }

    // Flat array: [timestamp, confidence, complexity, evolutionRate, nodeCount] × CAPACITY
    private val data = FloatArray(CAPACITY * FIELDS_PER_SAMPLE)

    /** Number of valid samples stored (up to CAPACITY). */
    var sampleCount = 0; private set

    /** Write head index (wraps around). */
    private var writeIndex = 0

    /** Time of last sample. */
    private var lastSampleTime = -SAMPLE_INTERVAL

    // ════════════════════════════════════════════════════════════════
    // Sampling
    // ════════════════════════════════════════════════════════════════

    /**
     * Record a sample if enough time has elapsed since the last one.
     * Called from the GL thread during CognitiveGraphPass.update().
     */
    fun sample(metrics: AIMetricsSnapshot, time: Float) {
        if (time - lastSampleTime < SAMPLE_INTERVAL) return
        lastSampleTime = time

        val offset = writeIndex * FIELDS_PER_SAMPLE
        data[offset] = time
        data[offset + 1] = metrics.confidence
        data[offset + 2] = (metrics.cognitiveLoad * 0.4f + metrics.autonomyLevel * 0.3f +
                metrics.evolutionRate * 0.3f).coerceIn(0f, 1f)
        data[offset + 3] = metrics.evolutionRate
        data[offset + 4] = metrics.memoryLoad // proxy for activity level

        writeIndex = (writeIndex + 1) % CAPACITY
        if (sampleCount < CAPACITY) sampleCount++
    }

    // ════════════════════════════════════════════════════════════════
    // Read API
    // ════════════════════════════════════════════════════════════════

    /**
     * Get a sample by index (0 = oldest available, sampleCount-1 = newest).
     * Writes into the provided output array: [timestamp, confidence, complexity, evolutionRate, nodeCount].
     * Returns false if index is out of range.
     */
    fun getSample(index: Int, output: FloatArray): Boolean {
        if (index < 0 || index >= sampleCount) return false
        val actualIndex = if (sampleCount < CAPACITY) {
            index
        } else {
            (writeIndex + index) % CAPACITY
        }
        val offset = actualIndex * FIELDS_PER_SAMPLE
        System.arraycopy(data, offset, output, 0, FIELDS_PER_SAMPLE)
        return true
    }

    /**
     * Get the newest sample. Returns false if no samples exist.
     */
    fun getLatest(output: FloatArray): Boolean {
        if (sampleCount == 0) return false
        return getSample(sampleCount - 1, output)
    }

    /**
     * Get the raw data array for direct GPU upload (e.g., as a texture or SSBO).
     * The array is [CAPACITY * FIELDS_PER_SAMPLE] floats.
     */
    fun getRawData(): FloatArray = data

    /**
     * Get the write index for proper ring buffer reading.
     */
    fun getWriteIndex(): Int = writeIndex

    /**
     * Compute the average of a specific field over the last N samples.
     * fieldIndex: 0=timestamp, 1=confidence, 2=complexity, 3=evolutionRate, 4=nodeCount
     */
    fun averageField(fieldIndex: Int, lastN: Int): Float {
        if (sampleCount == 0 || fieldIndex < 0 || fieldIndex >= FIELDS_PER_SAMPLE) return 0f
        val count = lastN.coerceAtMost(sampleCount)
        var sum = 0f
        val sampleBuf = FloatArray(FIELDS_PER_SAMPLE)
        for (i in (sampleCount - count) until sampleCount) {
            if (getSample(i, sampleBuf)) {
                sum += sampleBuf[fieldIndex]
            }
        }
        return sum / count
    }

    /**
     * Compute trend (positive = increasing, negative = decreasing) for a field
     * over the last N samples. Simple linear regression slope.
     */
    fun trendField(fieldIndex: Int, lastN: Int): Float {
        if (sampleCount < 2 || fieldIndex < 0 || fieldIndex >= FIELDS_PER_SAMPLE) return 0f
        val count = lastN.coerceAtMost(sampleCount)
        if (count < 2) return 0f

        var sumX = 0f; var sumY = 0f; var sumXY = 0f; var sumX2 = 0f
        val sampleBuf = FloatArray(FIELDS_PER_SAMPLE)
        for (i in 0 until count) {
            val sampleIdx = sampleCount - count + i
            if (getSample(sampleIdx, sampleBuf)) {
                val x = i.toFloat()
                val y = sampleBuf[fieldIndex]
                sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x
            }
        }
        val n = count.toFloat()
        val denom = n * sumX2 - sumX * sumX
        return if (denom > 0.0001f) (n * sumXY - sumX * sumY) / denom else 0f
    }

    // ════════════════════════════════════════════════════════════════
    // Reset
    // ════════════════════════════════════════════════════════════════

    fun reset() {
        sampleCount = 0
        writeIndex = 0
        lastSampleTime = -SAMPLE_INTERVAL
        data.fill(0f)
    }
}

