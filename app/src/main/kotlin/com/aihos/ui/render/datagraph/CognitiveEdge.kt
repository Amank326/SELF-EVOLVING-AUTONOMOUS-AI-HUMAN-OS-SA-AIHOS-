package com.aihos.ui.render.datagraph

/**
 * CognitiveEdge — A directed link between two cognitive nodes.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  CognitiveEdge Memory Layout (pre-allocated, mutable)          │
 * │                                                                 │
 * │  fromIndex       — source node index in pool                    │
 * │  toIndex         — target node index in pool                    │
 * │  active          — visibility flag                              │
 * │  strength        — visual beam width/brightness [0,1]           │
 * │  edgeType        — CAUSAL/EVIDENTIAL/TEMPORAL/REINFORCEMENT     │
 * │  flowDirection   — +1 forward, -1 reverse, 0 bidirectional      │
 * │  age             — seconds since activation                     │
 * │  phase           — animation phase offset                       │
 * │                                                                 │
 * │  particles[MAX_PARTICLES_PER_EDGE]                              │
 * │    — pre-allocated particle slots for data flow animation       │
 * │    — each particle has progress [0,1], speed, active flag       │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Stored in a fixed-size array. No allocation during topology changes.
 */
class CognitiveEdge {

    companion object {
        const val MAX_PARTICLES_PER_EDGE = 8
    }

    var fromIndex = -1
    var toIndex = -1
    var active = false
    var strength = 0.5f
    var edgeType = EdgeType.CAUSAL
    var flowDirection = 1f   // +1 = from→to, -1 = to→from, 0 = bidirectional
    var age = 0f
    var phase = 0f

    // Pre-allocated particle data for data flow animation along this edge
    val particleProgress = FloatArray(MAX_PARTICLES_PER_EDGE)   // [0,1] along edge
    val particleSpeed = FloatArray(MAX_PARTICLES_PER_EDGE)      // units/sec
    val particleActive = BooleanArray(MAX_PARTICLES_PER_EDGE)   // alive flag
    val particleAlpha = FloatArray(MAX_PARTICLES_PER_EDGE)      // fade factor

    fun activate(from: Int, to: Int, initialStrength: Float, type: EdgeType, dir: Float, edgePhase: Float) {
        fromIndex = from
        toIndex = to
        active = true
        strength = initialStrength
        edgeType = type
        flowDirection = dir
        phase = edgePhase
        age = 0f

        // Reset all particle slots
        for (i in 0 until MAX_PARTICLES_PER_EDGE) {
            particleProgress[i] = 0f
            particleSpeed[i] = 0f
            particleActive[i] = false
            particleAlpha[i] = 0f
        }
    }

    fun deactivate() {
        active = false
        fromIndex = -1
        toIndex = -1
        strength = 0f
        for (i in 0 until MAX_PARTICLES_PER_EDGE) {
            particleActive[i] = false
        }
    }

    /**
     * Spawn a particle at the source end of this edge.
     * Returns the slot index, or -1 if all slots are occupied.
     */
    fun spawnParticle(speed: Float): Int {
        for (i in 0 until MAX_PARTICLES_PER_EDGE) {
            if (!particleActive[i]) {
                particleActive[i] = true
                particleProgress[i] = 0f
                particleSpeed[i] = speed
                particleAlpha[i] = 1f
                return i
            }
        }
        return -1
    }

    /**
     * Advance all active particles along the edge.
     * Particles that reach the destination (progress >= 1.0) are deactivated.
     */
    fun updateParticles(dt: Float) {
        for (i in 0 until MAX_PARTICLES_PER_EDGE) {
            if (!particleActive[i]) continue
            particleProgress[i] += particleSpeed[i] * dt
            // Fade out near destination
            particleAlpha[i] = if (particleProgress[i] > 0.8f) {
                1f - (particleProgress[i] - 0.8f) * 5f
            } else {
                (particleProgress[i] * 5f).coerceAtMost(1f) // Fade in near source
            }
            if (particleProgress[i] >= 1f) {
                particleActive[i] = false
                particleProgress[i] = 0f
                particleAlpha[i] = 0f
            }
        }
    }

    /** Count currently active particles on this edge. */
    fun activeParticleCount(): Int {
        var count = 0
        for (i in 0 until MAX_PARTICLES_PER_EDGE) {
            if (particleActive[i]) count++
        }
        return count
    }
}

/**
 * Edge types representing different cognitive relationships.
 * Each type has a distinct visual appearance (color, pulse pattern).
 */
enum class EdgeType(val typeIndex: Int) {
    CAUSAL(0),          // White-cyan — direct causal inference
    EVIDENTIAL(1),      // Blue — evidence supporting a belief
    TEMPORAL(2),        // Green — temporal/sequential relationship
    REINFORCEMENT(3),   // Orange — reinforcement learning connection
    CONFLICT(4);        // Red — conflict resolution path

    companion object {
        fun fromIndex(index: Int): EdgeType = entries.getOrElse(index) { CAUSAL }
    }
}

