package com.aihos.ui.render.core

import java.util.concurrent.atomic.AtomicReference

/**
 * AIStateBridge — Thread-safe double-buffered bridge between ViewModel and GL thread.
 *
 * ┌──────────────────┐       ┌────────────────────┐       ┌──────────────────┐
 * │  ViewModel       │       │  AIStateBridge      │       │  GL Thread       │
 * │  (Main/IO)       │──────▶│  AtomicReference    │──────▶│  consumeState()  │
 * │  pushState()     │       │  (lock-free)        │       │  once per frame  │
 * └──────────────────┘       └────────────────────┘       └──────────────────┘
 *
 * Guarantees:
 * - No race conditions: AtomicReference is lock-free and thread-safe
 * - No torn reads: AIMetricsSnapshot is an immutable data class
 * - No direct UI thread access from GL thread
 * - GL thread reads exactly once per frame via consumeState()
 * - No allocation on GL thread (reuses the same data class instance)
 *
 * Usage:
 *   // From ViewModel (any thread):
 *   bridge.pushState(AIMetricsSnapshot(cognitiveLoad = 0.7f, ...))
 *
 *   // From GL onDrawFrame (GL thread):
 *   val snapshot = bridge.consumeState()
 */
class AIStateBridge {

    private val stateRef = AtomicReference(AIMetricsSnapshot())

    /**
     * Push new AI metrics from any thread. Lock-free, wait-free.
     * The snapshot is immutable — safe to share across threads.
     */
    fun pushState(snapshot: AIMetricsSnapshot) {
        stateRef.set(snapshot.sanitized())
    }

    /**
     * Convenience push with named parameters.
     */
    fun pushState(
        cognitiveLoad: Float = 0.5f,
        confidence: Float = 0.5f,
        evolutionRate: Float = 0.0f,
        selfAwareness: Float = 0.5f,
        autonomyLevel: Float = 0.5f,
        systemHealth: Float = 0.8f,
        memoryLoad: Float = 0.3f,
        animationIntensity: Float = 0.5f
    ) {
        pushState(
            AIMetricsSnapshot(
                cognitiveLoad, confidence, evolutionRate, selfAwareness,
                autonomyLevel, systemHealth, memoryLoad, animationIntensity
            )
        )
    }

    /**
     * Read the latest AI state from GL thread. Call exactly once per frame.
     * Returns an immutable snapshot — zero allocation (reuses existing object).
     */
    fun consumeState(): AIMetricsSnapshot = stateRef.get()
}

