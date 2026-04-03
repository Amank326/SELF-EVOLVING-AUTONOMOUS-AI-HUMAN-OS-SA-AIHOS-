package com.aihos.ui.render.cognition

import kotlin.math.sqrt

/**
 * DampedSpringSystem — Second-order damped harmonic oscillator.
 *
 * Instead of direct value assignment (x_target → x), this uses physically
 * correct spring dynamics that produce smooth, organic transitions.
 *
 * Equation of motion:
 *   ẍ = -ω²(x - x_target) - 2ζω·ẋ
 *
 * Where:
 *   ω  = natural frequency (rad/s) — controls speed of convergence
 *   ζ  = damping ratio — controls overshoot
 *       ζ < 1: underdamped (oscillates)
 *       ζ = 1: critically damped (fastest without overshoot)
 *       ζ > 1: overdamped (sluggish)
 *
 * For cognitive motion:
 *   Confidence  → critically damped (ζ ≈ 1.0, ω ≈ 4)  — stable, precise
 *   CognitiveLoad → slightly underdamped (ζ ≈ 0.7, ω ≈ 6) — energetic, bouncy
 *   Evolution    → underdamped (ζ ≈ 0.5, ω ≈ 3) — dramatic transitions
 *   Reflection   → overdamped (ζ ≈ 1.5, ω ≈ 2) — slow, contemplative
 *
 * Zero allocation. Pre-allocated per-channel.
 */
class DampedSpring(
    /** Natural frequency in rad/s. Higher = faster convergence. */
    private val omega: Float = 4.0f,
    /** Damping ratio. 1.0 = critical, <1 = bouncy, >1 = sluggish. */
    private val zeta: Float = 1.0f
) {
    var value: Float = 0f
        private set
    var velocity: Float = 0f
        private set
    var target: Float = 0f

    /**
     * Semi-implicit Euler integration. Stable for variable dt.
     * Must be called once per frame with clamped dt.
     */
    fun update(dt: Float) {
        // Acceleration: ẍ = -ω²(x - target) - 2ζω·ẋ
        val omegaSq = omega * omega
        val acceleration = -omegaSq * (value - target) - 2f * zeta * omega * velocity

        // Semi-implicit Euler (update velocity first, then position)
        velocity += acceleration * dt
        value += velocity * dt
    }

    fun set(v: Float) {
        value = v
        target = v
        velocity = 0f
    }

    fun snap() {
        value = target
        velocity = 0f
    }
}

/**
 * DampedSpringBank — A fixed-size bank of DampedSpring channels.
 *
 * Each cognitive metric gets its own spring with tuned parameters.
 * Pre-allocated. Zero GC.
 *
 * Channel layout (6 channels):
 *   [0] cognitiveLoad   — ω=6, ζ=0.7 (energetic, slightly bouncy)
 *   [1] confidence       — ω=4, ζ=1.0 (critically damped, stable)
 *   [2] evolutionRate    — ω=3, ζ=0.5 (dramatic, oscillating)
 *   [3] reflectionDepth  — ω=2, ζ=1.5 (overdamped, slow, contemplative)
 *   [4] memoryActivity   — ω=5, ζ=0.8 (responsive but smooth)
 *   [5] decisionComplexity — ω=4, ζ=0.6 (moderate bounce)
 */
class DampedSpringBank {

    val cognitiveLoad = DampedSpring(omega = 6f, zeta = 0.7f)
    val confidence = DampedSpring(omega = 4f, zeta = 1.0f)
    val evolutionRate = DampedSpring(omega = 3f, zeta = 0.5f)
    val reflectionDepth = DampedSpring(omega = 2f, zeta = 1.5f)
    val memoryActivity = DampedSpring(omega = 5f, zeta = 0.8f)
    val decisionComplexity = DampedSpring(omega = 4f, zeta = 0.6f)

    private val springs = arrayOf(
        cognitiveLoad, confidence, evolutionRate,
        reflectionDepth, memoryActivity, decisionComplexity
    )

    /**
     * Push new cognitive targets. Springs will animate toward them.
     */
    fun pushTargets(state: CognitiveState) {
        cognitiveLoad.target = state.cognitiveLoad
        confidence.target = state.confidence
        evolutionRate.target = state.evolutionRate
        reflectionDepth.target = state.reflectionDepth
        memoryActivity.target = state.memoryActivity
        decisionComplexity.target = state.decisionComplexity
    }

    /**
     * Step all springs forward by dt seconds.
     * Produces smooth second-order transitions.
     */
    fun update(dt: Float) {
        for (spring in springs) {
            spring.update(dt)
        }
    }

    /**
     * Snap all springs to their targets instantly (for initialization).
     */
    fun snapAll() {
        for (spring in springs) {
            spring.snap()
        }
    }

    /**
     * Build a snapshot of current smoothed values.
     */
    fun snapshot(): SmoothedCognitiveState = SmoothedCognitiveState(
        cognitiveLoad = cognitiveLoad.value.coerceIn(0f, 1f),
        confidence = confidence.value.coerceIn(0f, 1f),
        evolutionRate = evolutionRate.value.coerceIn(0f, 1f),
        reflectionDepth = reflectionDepth.value.coerceIn(0f, 1f),
        memoryActivity = memoryActivity.value.coerceIn(0f, 1f),
        decisionComplexity = decisionComplexity.value.coerceIn(0f, 1f),
        // Derivative metrics (useful for jitter/impulse detection)
        cognitiveLoadVelocity = cognitiveLoad.velocity,
        confidenceVelocity = confidence.velocity
    )
}

/**
 * SmoothedCognitiveState — Physics-smoothed cognitive values + derivatives.
 * Read-only struct produced each frame by the spring bank.
 */
data class SmoothedCognitiveState(
    val cognitiveLoad: Float,
    val confidence: Float,
    val evolutionRate: Float,
    val reflectionDepth: Float,
    val memoryActivity: Float,
    val decisionComplexity: Float,
    /** Rate of change of cognitive load (for jitter detection). */
    val cognitiveLoadVelocity: Float = 0f,
    /** Rate of change of confidence (for stability detection). */
    val confidenceVelocity: Float = 0f
) {
    /** True when the system is undergoing rapid cognitive transitions. */
    val isTurbulent: Boolean get() {
        val velMag = sqrt(cognitiveLoadVelocity * cognitiveLoadVelocity +
                confidenceVelocity * confidenceVelocity)
        return velMag > 0.5f
    }
}

