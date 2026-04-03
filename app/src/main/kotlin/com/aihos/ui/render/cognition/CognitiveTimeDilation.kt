package com.aihos.ui.render.cognition

import kotlin.math.sin
import kotlin.math.PI

/**
 * CognitiveTimeDilation — Time scaling based on the AI's cognitive mode.
 *
 * ┌─────────────────────┬──────────────────┬───────────────────────┐
 * │  Cognitive Mode     │  Time Scale      │  Visual Effect        │
 * ├─────────────────────┼──────────────────┼───────────────────────┤
 * │  Deep reflection    │  0.3 – 0.5       │  Slow-motion cinema   │
 * │  Idle / ambient     │  0.7 – 1.0       │  Slow breathing       │
 * │  Normal processing  │  1.0             │  Standard 60 FPS feel │
 * │  High execution     │  1.2 – 1.5       │  Accelerated motion   │
 * │  Turbulent decision │  0.8 (pulsing)   │  Stuttered time       │
 * └─────────────────────┴──────────────────┴───────────────────────┘
 *
 * Integration into render loop:
 *   cognitiveTime += wallDt * timeScale
 *
 * All visual systems read cognitiveTime instead of wall-clock time,
 * while FPS remains locked at 60. Physics (springs) still use wall-clock dt.
 *
 * Safety:
 *   timeScale is clamped to [0.1, 2.0] to prevent instability.
 *   dt is always wall-clock for spring physics.
 *   Only visual time (shader time, camera drift, particle phase) is dilated.
 */
class CognitiveTimeDilation {

    /** Current time scale factor. */
    var timeScale: Float = 1.0f
        private set

    /** Accumulated cognitive time (integrated with time scaling). */
    var cognitiveTime: Float = 0f
        private set

    // Internal smoothing spring for time scale itself
    // (prevents jarring time-scale jumps)
    private val timeScaleSpring = DampedSpring(omega = 3f, zeta = 1.2f)

    init {
        timeScaleSpring.set(1.0f)
    }

    /**
     * Compute time scale from smoothed cognitive state.
     * Must be called once per frame with wall-clock dt.
     *
     * @param smoothed  current smoothed cognitive values
     * @param wallDt    wall-clock delta time (NOT dilated)
     * @param wallTime  wall-clock elapsed time (for modulation)
     */
    fun update(smoothed: SmoothedCognitiveState, wallDt: Float, wallTime: Float) {
        val targetScale = computeTargetScale(smoothed, wallTime)
        timeScaleSpring.target = targetScale
        timeScaleSpring.update(wallDt)
        timeScale = timeScaleSpring.value.coerceIn(MIN_SCALE, MAX_SCALE)

        // Integrate cognitive time
        cognitiveTime += wallDt * timeScale
    }

    /**
     * Determine target time scale from cognitive state.
     *
     * Formula:
     *   baseScale = 1.0
     *   reflectionPull = -reflectionDepth * 0.7    → slows during reflection
     *   executionPush  = +cognitiveLoad * 0.3      → speeds during high load
     *   idleBreathing  = sin(t * 0.3) * 0.1        → gentle ambient oscillation
     *   turbulencePulse = isTurbulent ? sin(t*8)*0.15 : 0   → stutter during turbulence
     *
     *   targetScale = baseScale + reflectionPull + executionPush + idleBreathing + turbulencePulse
     */
    private fun computeTargetScale(s: SmoothedCognitiveState, wallTime: Float): Float {
        val base = 1.0f

        // Reflection pulls time down (deep introspection = slow-motion)
        val reflectionPull = -s.reflectionDepth * 0.7f

        // High cognitive load accelerates perceived time
        val executionPush = s.cognitiveLoad * 0.3f

        // Low activity → ambient breathing modulation
        val activityLevel = (s.cognitiveLoad + s.memoryActivity) * 0.5f
        val idleWeight = (1f - activityLevel).coerceIn(0f, 1f)
        val idleBreathing = sin(wallTime * 0.3f * 2f * PI.toFloat()) * 0.1f * idleWeight

        // Turbulent decision-making → temporal stutter
        val turbulencePulse = if (s.isTurbulent) {
            sin(wallTime * 8f * 2f * PI.toFloat()) * 0.15f
        } else 0f

        return base + reflectionPull + executionPush + idleBreathing + turbulencePulse
    }

    fun reset() {
        timeScale = 1.0f
        cognitiveTime = 0f
        timeScaleSpring.set(1.0f)
    }

    companion object {
        private const val MIN_SCALE = 0.1f
        private const val MAX_SCALE = 2.0f
    }
}

