package com.aihos.ui.render.datagraph

import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CognitiveGraph — Pre-allocated pool of cognitive nodes + edges with
 * integrated force-directed layout simulation.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Cognitive Graph Architecture                                    │
 * │                                                                  │
 * │  nodes[MAX_NODES=300]   — fixed array, object pool               │
 * │  edges[MAX_EDGES=600]   — fixed array, object pool               │
 * │  activeNodeCount        — live count                              │
 * │  activeEdgeCount        — live count                              │
 * │                                                                  │
 * │  Force-Directed Layout:                                          │
 * │    Repulsion: Coulomb's law between all active nodes             │
 * │    Attraction: Hooke's law on connected edges                    │
 * │    Centering: gentle pull toward origin                          │
 * │    Z-Stratification: nodeType → Z-layer separation               │
 * │    Damping: confidence-based velocity damping                    │
 * │                                                                  │
 * │  Particle System:                                                │
 * │    Particles travel along edges to visualize reasoning flow.     │
 * │    memoryActivity drives emission rate.                          │
 * │    cognitiveLoad drives particle speed.                          │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * No allocation after initialization. All operations reuse pool objects.
 */
class CognitiveGraph {

    companion object {
        const val MAX_NODES = 300
        const val MAX_EDGES = 600

        // Force-directed layout constants
        private const val REPULSION_STRENGTH = 0.08f
        private const val REPULSION_RANGE_SQ = 4.0f     // Skip pairs beyond 2.0 units
        private const val ATTRACTION_STRENGTH = 0.4f
        private const val CENTERING_STRENGTH = 0.15f
        private const val DAMPING_BASE = 0.92f
        private const val DAMPING_CONFIDENCE_SCALE = 0.07f
        private const val MAX_VELOCITY = 2.0f
        private const val MIN_DISTANCE_SQ = 0.0001f

        // Z-layer offsets per node type
        private val TYPE_Z_OFFSETS = floatArrayOf(
            0.0f,   // DECISION  — center
            -0.6f,  // MEMORY    — behind
            0.3f,   // BELIEF    — slightly forward
            0.6f,   // INFERENCE — forward
            0.0f,   // INSIGHT   — center
            -0.3f   // RULE      — slightly behind
        )

        // Rest lengths per edge type
        private val EDGE_REST_LENGTHS = floatArrayOf(
            0.8f,   // CAUSAL
            1.0f,   // EVIDENTIAL
            1.2f,   // TEMPORAL
            0.9f,   // REINFORCEMENT
            1.1f    // CONFLICT
        )

        // Particle spawn rate scale per quality level
        private const val PARTICLE_SPEED_BASE = 0.6f
        private const val PARTICLE_SPAWN_INTERVAL_BASE = 0.4f // seconds
    }

    val nodes = Array(MAX_NODES) { CognitiveNode() }
    val edges = Array(MAX_EDGES) { CognitiveEdge() }

    var activeNodeCount = 0; private set
    var activeEdgeCount = 0; private set

    // Pre-allocated scratch arrays for physics (zero-alloc)
    private val force = FloatArray(3)
    private val diff = FloatArray(3)

    // Particle spawn timing
    private var particleSpawnAccumulator = 0f
    private var layoutIterations = 2

    // ════════════════════════════════════════════════════════════════
    // Pool management
    // ════════════════════════════════════════════════════════════════

    fun findFreeNode(): Int {
        for (i in nodes.indices) {
            if (!nodes[i].active) return i
        }
        return -1
    }

    fun findFreeEdge(): Int {
        for (i in edges.indices) {
            if (!edges[i].active) return i
        }
        return -1
    }

    fun addNode(
        x: Float, y: Float, z: Float,
        type: NodeType, weight: Float, phase: Float
    ): Int {
        val idx = findFreeNode()
        if (idx < 0) return -1
        nodes[idx].activate(x, y, z, type, weight, phase)
        activeNodeCount++
        return idx
    }

    fun removeNode(index: Int) {
        if (index < 0 || index >= MAX_NODES || !nodes[index].active) return
        nodes[index].deactivate()
        activeNodeCount--
        // Deactivate all edges connected to this node
        for (e in edges) {
            if (!e.active) continue
            if (e.fromIndex == index || e.toIndex == index) {
                e.deactivate()
                activeEdgeCount--
            }
        }
    }

    fun connect(from: Int, to: Int, strength: Float, type: EdgeType, direction: Float = 1f): Int {
        if (from < 0 || to < 0 || from == to) return -1
        if (!nodes[from].active || !nodes[to].active) return -1
        if (edgeExists(from, to)) return -1
        val idx = findFreeEdge()
        if (idx < 0) return -1
        edges[idx].activate(from, to, strength, type, direction, (from + to).toFloat() * 0.3f)
        nodes[from].connectionCount++
        nodes[to].connectionCount++
        activeEdgeCount++
        return idx
    }

    fun disconnect(edgeIndex: Int) {
        if (edgeIndex < 0 || edgeIndex >= MAX_EDGES || !edges[edgeIndex].active) return
        val from = edges[edgeIndex].fromIndex
        val to = edges[edgeIndex].toIndex
        if (from >= 0 && from < MAX_NODES && nodes[from].active) nodes[from].connectionCount--
        if (to >= 0 && to < MAX_NODES && nodes[to].active) nodes[to].connectionCount--
        edges[edgeIndex].deactivate()
        activeEdgeCount--
    }

    fun edgeExists(from: Int, to: Int): Boolean {
        for (e in edges) {
            if (!e.active) continue
            if ((e.fromIndex == from && e.toIndex == to) ||
                (e.fromIndex == to && e.toIndex == from)) return true
        }
        return false
    }

    // ════════════════════════════════════════════════════════════════
    // Default initialization — creates a sample reasoning graph
    // ════════════════════════════════════════════════════════════════

    fun initialize() {
        deactivateAll()
        val twoPi = (2.0 * PI).toFloat()

        // Central decision node
        addNode(0f, 0f, 0f, NodeType.DECISION, 1f, 0f)

        // Inner ring: 6 inference nodes
        for (i in 0 until 6) {
            val angle = twoPi * i / 6f
            val x = cos(angle) * 1.5f
            val y = sin(angle * 0.5f + i.toFloat()) * 0.2f
            val z = sin(angle) * 1.5f + TYPE_Z_OFFSETS[NodeType.INFERENCE.typeIndex]
            addNode(x, y, z, NodeType.INFERENCE, 0.7f, angle)
        }

        // Middle ring: 8 belief nodes
        for (i in 0 until 8) {
            val angle = twoPi * i / 8f + 0.2f
            val x = cos(angle) * 2.5f
            val y = sin(angle * 0.7f + i * 0.3f) * 0.3f
            val z = sin(angle) * 2.5f + TYPE_Z_OFFSETS[NodeType.BELIEF.typeIndex]
            addNode(x, y, z, NodeType.BELIEF, 0.5f, angle)
        }

        // Outer ring: 6 memory nodes
        for (i in 0 until 6) {
            val angle = twoPi * i / 6f + 0.4f
            val x = cos(angle) * 3.5f
            val y = sin(angle * 0.3f + i * 0.5f) * 0.15f
            val z = sin(angle) * 3.5f + TYPE_Z_OFFSETS[NodeType.MEMORY.typeIndex]
            addNode(x, y, z, NodeType.MEMORY, 0.4f, angle)
        }

        // 4 insight nodes scattered
        for (i in 0 until 4) {
            val angle = twoPi * i / 4f + 0.8f
            val x = cos(angle) * 2.0f
            val y = 0.8f + sin(i.toFloat()) * 0.3f
            val z = sin(angle) * 2.0f + TYPE_Z_OFFSETS[NodeType.INSIGHT.typeIndex]
            addNode(x, y, z, NodeType.INSIGHT, 0.6f, angle)
        }

        // 3 rule nodes
        for (i in 0 until 3) {
            val angle = twoPi * i / 3f + 1.2f
            val x = cos(angle) * 1.8f
            val y = -0.5f + sin(i.toFloat() * 0.7f) * 0.2f
            val z = sin(angle) * 1.8f + TYPE_Z_OFFSETS[NodeType.RULE.typeIndex]
            addNode(x, y, z, NodeType.RULE, 0.55f, angle)
        }

        // Connect decision → inference nodes (CAUSAL)
        for (i in 1..6) {
            connect(0, i, 0.9f, EdgeType.CAUSAL)
        }

        // Connect inference → belief nodes (EVIDENTIAL)
        for (i in 0 until 6) {
            val inferIdx = 1 + i
            val beliefIdx = 7 + (i % 8)
            connect(inferIdx, beliefIdx, 0.6f, EdgeType.EVIDENTIAL)
            // Also connect to adjacent belief
            connect(inferIdx, 7 + ((i + 1) % 8), 0.4f, EdgeType.EVIDENTIAL)
        }

        // Connect belief → memory nodes (TEMPORAL)
        for (i in 0 until 6) {
            val beliefIdx = 7 + i
            val memIdx = 15 + (i % 6)
            connect(beliefIdx, memIdx, 0.5f, EdgeType.TEMPORAL)
        }

        // Connect decision → insight nodes (REINFORCEMENT)
        for (i in 0 until 4) {
            connect(0, 21 + i, 0.7f, EdgeType.REINFORCEMENT)
        }

        // Connect insight → rule nodes (CAUSAL)
        for (i in 0 until 3) {
            val insightIdx = 21 + (i % 4)
            connect(insightIdx, 25 + i, 0.55f, EdgeType.CAUSAL)
        }

        // A few cross-links for visual interest
        connect(2, 4, 0.3f, EdgeType.CONFLICT)
        connect(8, 12, 0.35f, EdgeType.REINFORCEMENT)
    }

    // ════════════════════════════════════════════════════════════════
    // Force-directed layout + particle update
    // ════════════════════════════════════════════════════════════════

    /**
     * Run the full physics + particle update step.
     *
     * @param metrics Current AI state for state-driven behavior
     * @param dt Frame delta time (clamped externally)
     * @param time Wall clock time (seconds)
     * @param quality Current quality level for iteration count
     */
    fun update(metrics: AIMetricsSnapshot, dt: Float, time: Float, quality: QualityLevel) {
        layoutIterations = when (quality) {
            QualityLevel.HIGH -> 3
            QualityLevel.MEDIUM -> 2
            QualityLevel.LOW -> 1
        }

        // Run layout multiple iterations for convergence
        for (iter in 0 until layoutIterations) {
            updateForceDirectedLayout(metrics, dt / layoutIterations, time)
        }

        // Update node ages and animation
        updateNodeDynamics(metrics, dt, time)

        // Update particles along edges
        updateParticles(metrics, dt, time)
    }

    private fun updateForceDirectedLayout(metrics: AIMetricsSnapshot, dt: Float, time: Float) {
        val confidence = metrics.confidence
        val damping = DAMPING_BASE + confidence * DAMPING_CONFIDENCE_SCALE

        for (i in nodes.indices) {
            val n = nodes[i]
            if (!n.active) continue

            // Decision node at index 0: gentle ambient motion only
            if (i == 0 && n.nodeType == NodeType.DECISION) {
                n.position[0] = sin(time * 0.25f) * 0.03f
                n.position[1] = sin(time * 0.18f + 1f) * 0.03f
                n.position[2] = cos(time * 0.2f) * 0.03f
                continue
            }

            force[0] = 0f; force[1] = 0f; force[2] = 0f

            // ── Repulsion (Coulomb) ──────────────────────────────
            for (j in nodes.indices) {
                if (i == j) continue
                val o = nodes[j]
                if (!o.active) continue

                diff[0] = n.position[0] - o.position[0]
                diff[1] = n.position[1] - o.position[1]
                diff[2] = n.position[2] - o.position[2]

                val dSq = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2]
                if (dSq > REPULSION_RANGE_SQ || dSq < MIN_DISTANCE_SQ) continue

                val d = sqrt(dSq)
                val repForce = REPULSION_STRENGTH / dSq
                val inv = 1f / d

                force[0] += diff[0] * inv * repForce
                force[1] += diff[1] * inv * repForce
                force[2] += diff[2] * inv * repForce
            }

            // ── Centering force ──────────────────────────────────
            force[0] -= n.position[0] * CENTERING_STRENGTH
            force[1] -= n.position[1] * CENTERING_STRENGTH
            force[2] -= n.position[2] * CENTERING_STRENGTH

            // ── Z-stratification force ───────────────────────────
            val targetZ = TYPE_Z_OFFSETS[n.nodeType.typeIndex.coerceIn(0, TYPE_Z_OFFSETS.size - 1)]
            force[2] += (targetZ - n.position[2]) * 0.1f

            // ── Velocity damping ─────────────────────────────────
            force[0] -= n.velocity[0] * 2f
            force[1] -= n.velocity[1] * 2f
            force[2] -= n.velocity[2] * 2f

            // ── Integrate ────────────────────────────────────────
            n.velocity[0] += force[0] * dt
            n.velocity[1] += force[1] * dt
            n.velocity[2] += force[2] * dt

            // Clamp velocity
            val vSq = n.velocity[0] * n.velocity[0] + n.velocity[1] * n.velocity[1] + n.velocity[2] * n.velocity[2]
            if (vSq > MAX_VELOCITY * MAX_VELOCITY) {
                val scale = MAX_VELOCITY / sqrt(vSq)
                n.velocity[0] *= scale
                n.velocity[1] *= scale
                n.velocity[2] *= scale
            }

            n.position[0] += n.velocity[0] * dt
            n.position[1] += n.velocity[1] * dt
            n.position[2] += n.velocity[2] * dt

            // Apply damping
            n.velocity[0] *= damping
            n.velocity[1] *= damping
            n.velocity[2] *= damping
        }

        // ── Edge attraction (Hooke's law) ────────────────────────
        for (e in edges) {
            if (!e.active) continue
            val fi = e.fromIndex; val ti = e.toIndex
            if (fi < 0 || ti < 0 || fi >= MAX_NODES || ti >= MAX_NODES) continue
            val a = nodes[fi]; val b = nodes[ti]
            if (!a.active || !b.active) continue

            diff[0] = b.position[0] - a.position[0]
            diff[1] = b.position[1] - a.position[1]
            diff[2] = b.position[2] - a.position[2]

            val d = sqrt(diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2])
            if (d < 0.001f) continue

            val restLength = EDGE_REST_LENGTHS[e.edgeType.typeIndex.coerceIn(0, EDGE_REST_LENGTHS.size - 1)]
            val displacement = d - restLength
            val springForce = ATTRACTION_STRENGTH * e.strength * displacement / d

            // Apply to both nodes (unless it's the central decision node)
            if (fi != 0) {
                a.velocity[0] += diff[0] * springForce * dt
                a.velocity[1] += diff[1] * springForce * dt
                a.velocity[2] += diff[2] * springForce * dt
            }
            if (ti != 0) {
                b.velocity[0] -= diff[0] * springForce * dt
                b.velocity[1] -= diff[1] * springForce * dt
                b.velocity[2] -= diff[2] * springForce * dt
            }
        }
    }

    private fun updateNodeDynamics(metrics: AIMetricsSnapshot, dt: Float, time: Float) {
        for (n in nodes) {
            if (!n.active) continue
            n.age += dt

            // Pulsating energy based on cognitive load
            val basePulse = sin(time * 2.5f + n.phase * 3f) * 0.5f + 0.5f
            n.energy = (n.weight * 0.6f + basePulse * 0.3f + metrics.cognitiveLoad * 0.1f)
                .coerceIn(0.1f, 1f)

            // Smooth highlight transition
            val targetHighlight = if (n.highlighted) 1f else 0f
            n.highlightAlpha += (targetHighlight - n.highlightAlpha) * min(dt * 8f, 1f)
        }
    }

    private fun updateParticles(metrics: AIMetricsSnapshot, dt: Float, time: Float) {
        // Spawn rate driven by memoryActivity
        val spawnInterval = PARTICLE_SPAWN_INTERVAL_BASE / max(metrics.memoryLoad * 3f, 0.3f)
        val particleSpeed = PARTICLE_SPEED_BASE + metrics.cognitiveLoad * 0.8f

        particleSpawnAccumulator += dt
        if (particleSpawnAccumulator >= spawnInterval) {
            particleSpawnAccumulator -= spawnInterval

            // Spawn a particle on a random active edge
            var spawned = false
            for (e in edges) {
                if (!e.active) continue
                if (e.activeParticleCount() < CognitiveEdge.MAX_PARTICLES_PER_EDGE / 2) {
                    // Probabilistic spawn based on edge strength
                    val hashVal = (e.fromIndex * 31 + e.toIndex * 17 + (time * 7f).toInt()) and 0xFF
                    if (hashVal < (e.strength * 128f).toInt()) {
                        e.spawnParticle(particleSpeed * (0.8f + e.strength * 0.4f))
                        spawned = true
                    }
                }
                if (spawned) break
            }
        }

        // Update all particle positions
        for (e in edges) {
            if (!e.active) continue
            e.updateParticles(dt)
        }
    }

    private fun deactivateAll() {
        for (n in nodes) n.deactivate()
        for (e in edges) e.deactivate()
        activeNodeCount = 0
        activeEdgeCount = 0
        particleSpawnAccumulator = 0f
    }

    fun reset() {
        deactivateAll()
    }
}

