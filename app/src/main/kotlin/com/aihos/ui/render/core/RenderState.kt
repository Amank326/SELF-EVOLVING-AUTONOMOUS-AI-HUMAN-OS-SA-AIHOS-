package com.aihos.ui.render.core

/**
 * RenderState — Immutable snapshot of the entire render engine state for one frame.
 *
 * Created once at the beginning of onDrawFrame() from the double-buffered
 * AI state bridge. Every subsystem reads from this snapshot — never from
 * volatile fields or atomic references during the frame.
 *
 * This is the single source of truth for the GL thread within a frame.
 */
data class RenderState(
    /** Seconds since renderer initialization. */
    val elapsedTime: Float = 0f,
    /** Seconds since last frame (clamped to [0.001, 0.1]). */
    val deltaTime: Float = 0.016f,
    /** Screen width in pixels. */
    val screenWidth: Int = 1,
    /** Screen height in pixels. */
    val screenHeight: Int = 1,
    /** Current quality tier. */
    val qualityLevel: QualityLevel = QualityLevel.HIGH,
    /** AI cognitive metrics snapshot. */
    val aiMetrics: AIMetricsSnapshot = AIMetricsSnapshot()
)

/**
 * AIMetricsSnapshot — Immutable capture of all AI metrics for one frame.
 * Pushed from ViewModel/coroutine thread, consumed on GL thread.
 * All values normalized to [0, 1].
 */
data class AIMetricsSnapshot(
    val cognitiveLoad: Float = 0.5f,
    val confidence: Float = 0.5f,
    val evolutionRate: Float = 0.0f,
    val selfAwareness: Float = 0.5f,
    val autonomyLevel: Float = 0.5f,
    val systemHealth: Float = 0.8f,
    val memoryLoad: Float = 0.3f,
    val animationIntensity: Float = 0.5f
) {
    /** Clamp all values into valid range. */
    fun sanitized(): AIMetricsSnapshot = AIMetricsSnapshot(
        cognitiveLoad = cognitiveLoad.coerceIn(0f, 1f),
        confidence = confidence.coerceIn(0f, 1f),
        evolutionRate = evolutionRate.coerceIn(0f, 1f),
        selfAwareness = selfAwareness.coerceIn(0f, 1f),
        autonomyLevel = autonomyLevel.coerceIn(0f, 1f),
        systemHealth = systemHealth.coerceIn(0f, 1f),
        memoryLoad = memoryLoad.coerceIn(0f, 1f),
        animationIntensity = animationIntensity.coerceIn(0f, 1f)
    )
}

/**
 * Quality level for dynamic scaling.
 */
enum class QualityLevel {
    HIGH, MEDIUM, LOW;

    val bloomPasses: Int get() = when (this) {
        HIGH -> 4; MEDIUM -> 2; LOW -> 0
    }

    val resolutionScale: Float get() = when (this) {
        HIGH -> 1.0f; MEDIUM -> 0.75f; LOW -> 0.5f
    }

    val maxPointLights: Int get() = when (this) {
        HIGH -> 4; MEDIUM -> 3; LOW -> 2
    }

    val bloomEnabled: Boolean get() = this != LOW
}

