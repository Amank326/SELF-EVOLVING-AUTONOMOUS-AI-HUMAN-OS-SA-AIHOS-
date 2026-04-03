package com.aihos.ui.render.lattice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * NeuralNode — A single node in the neural lattice.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  NeuralNode Memory Layout (pre-allocated, mutable struct)      │
 * │                                                                 │
 * │  position[3]    — world-space xyz                               │
 * │  velocity[3]    — physics velocity                              │
 * │  targetPos[3]   — spring target position                        │
 * │  color[4]       — rgba tint                                     │
 * │  radius          — visual scale                                 │
 * │  energy          — glow intensity [0,1]                         │
 * │  layer           — depth layer (0=core, 1=inner, 2=outer)       │
 * │  active          — visibility flag                              │
 * │  connectionCount — current number of connections                │
 * │  phase           — animation phase offset                       │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * All fields are pre-allocated. No heap allocation during frame updates.
 * Nodes are stored in a fixed-size array (object pool pattern).
 */
class NeuralNode {
    val position = FloatArray(3)
    val velocity = FloatArray(3)
    val targetPos = FloatArray(3)
    val color = floatArrayOf(0f, 0.85f, 1f, 1f)
    var radius = 0.06f
    var energy = 0.5f
    var layer = 0          // 0=core, 1=inner ring, 2=outer ring
    var active = false
    var connectionCount = 0
    var phase = 0f
    var age = 0f           // seconds since activation
    var clusterGroup = 0   // clustering affinity

    fun activate(x: Float, y: Float, z: Float, nodeLayer: Int, nodePhase: Float) {
        position[0] = x; position[1] = y; position[2] = z
        targetPos[0] = x; targetPos[1] = y; targetPos[2] = z
        velocity[0] = 0f; velocity[1] = 0f; velocity[2] = 0f
        layer = nodeLayer; phase = nodePhase; active = true
        age = 0f; connectionCount = 0; energy = 0.5f
        radius = when (nodeLayer) {
            0 -> 0.12f   // core nodes are larger
            1 -> 0.07f
            else -> 0.04f
        }
    }

    fun deactivate() {
        active = false; connectionCount = 0; energy = 0f
    }

    fun distanceTo(other: NeuralNode): Float {
        val dx = position[0] - other.position[0]
        val dy = position[1] - other.position[1]
        val dz = position[2] - other.position[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

/**
 * NeuralConnection — A directed energy beam between two nodes.
 *
 * Stored as indices into the node pool.
 * Pre-allocated in a fixed-size array.
 */
class NeuralConnection {
    var fromIndex = -1
    var toIndex = -1
    var active = false
    var strength = 1f        // [0,1] visual beam width/brightness
    var pulsePhase = 0f      // animation phase for energy pulse
    var age = 0f

    fun activate(from: Int, to: Int, initialStrength: Float, phase: Float) {
        fromIndex = from; toIndex = to; active = true
        strength = initialStrength; pulsePhase = phase; age = 0f
    }

    fun deactivate() {
        active = false; fromIndex = -1; toIndex = -1; strength = 0f
    }
}

/**
 * NeuralLattice — Pre-allocated pool of nodes + connections.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  Object Pool Architecture                                │
 * │                                                          │
 * │  nodes[MAX_NODES]         — fixed array, reused          │
 * │  connections[MAX_CONNS]   — fixed array, reused          │
 * │  activeNodeCount          — how many are currently alive  │
 * │  activeConnCount          — how many beams are alive      │
 * │                                                          │
 * │  No allocation during topology changes.                   │
 * │  activate() / deactivate() toggle the active flag.        │
 * └──────────────────────────────────────────────────────────┘
 */
class NeuralLattice {

    companion object {
        const val MAX_NODES = 64
        const val MAX_CONNECTIONS = 128
        const val CORE_NODES = 1
        const val INITIAL_INNER_NODES = 6
        const val INITIAL_OUTER_NODES = 12
    }

    val nodes = Array(MAX_NODES) { NeuralNode() }
    val connections = Array(MAX_CONNECTIONS) { NeuralConnection() }

    var activeNodeCount = 0; private set
    var activeConnCount = 0; private set

    /**
     * Initialize default lattice topology:
     *   - 1 central core node at origin
     *   - 6 inner ring nodes (hexagonal)
     *   - 12 outer ring nodes
     *   - Core → inner connections
     *   - Inner → outer connections (nearest)
     */
    fun initialize() {
        deactivateAll()
        val twoPi = (2.0 * PI).toFloat()

        // Core node
        nodes[0].activate(0f, 0f, 0f, 0, 0f)
        activeNodeCount = 1

        // Inner ring (hexagonal, radius ~1.2)
        for (i in 0 until INITIAL_INNER_NODES) {
            val angle = twoPi * i / INITIAL_INNER_NODES
            val x = cos(angle) * 1.2f
            val z = sin(angle) * 1.2f
            val y = sin(angle * 2f + i.toFloat()) * 0.15f
            val idx = CORE_NODES + i
            nodes[idx].activate(x, y, z, 1, angle)
            activeNodeCount++
        }

        // Outer ring (radius ~2.2)
        for (i in 0 until INITIAL_OUTER_NODES) {
            val angle = twoPi * i / INITIAL_OUTER_NODES + twoPi / (INITIAL_OUTER_NODES * 2f)
            val x = cos(angle) * 2.2f
            val z = sin(angle) * 2.2f
            val y = sin(angle * 3f + i.toFloat()) * 0.25f
            val idx = CORE_NODES + INITIAL_INNER_NODES + i
            nodes[idx].activate(x, y, z, 2, angle)
            activeNodeCount++
        }

        // Connect core → all inner nodes
        var connIdx = 0
        for (i in 0 until INITIAL_INNER_NODES) {
            if (connIdx >= MAX_CONNECTIONS) break
            connections[connIdx].activate(0, CORE_NODES + i, 1f, i.toFloat() * 0.5f)
            nodes[0].connectionCount++
            nodes[CORE_NODES + i].connectionCount++
            connIdx++
        }

        // Connect inner → nearest outer nodes
        for (i in 0 until INITIAL_INNER_NODES) {
            val innerIdx = CORE_NODES + i
            val outerStart = CORE_NODES + INITIAL_INNER_NODES
            // Each inner connects to 2 nearest outer
            val o1 = outerStart + (i * 2) % INITIAL_OUTER_NODES
            val o2 = outerStart + (i * 2 + 1) % INITIAL_OUTER_NODES
            if (connIdx < MAX_CONNECTIONS) {
                connections[connIdx].activate(innerIdx, o1, 0.7f, i.toFloat())
                nodes[innerIdx].connectionCount++
                nodes[o1].connectionCount++
                connIdx++
            }
            if (connIdx < MAX_CONNECTIONS) {
                connections[connIdx].activate(innerIdx, o2, 0.5f, i.toFloat() + 0.3f)
                nodes[innerIdx].connectionCount++
                nodes[o2].connectionCount++
                connIdx++
            }
        }

        activeConnCount = connIdx
    }

    fun findFreeNode(): Int {
        for (i in nodes.indices) {
            if (!nodes[i].active) return i
        }
        return -1
    }

    fun findFreeConnection(): Int {
        for (i in connections.indices) {
            if (!connections[i].active) return i
        }
        return -1
    }

    fun connectionExists(from: Int, to: Int): Boolean {
        for (c in connections) {
            if (!c.active) continue
            if ((c.fromIndex == from && c.toIndex == to) ||
                (c.fromIndex == to && c.toIndex == from)) return true
        }
        return false
    }

    private fun deactivateAll() {
        for (n in nodes) n.deactivate()
        for (c in connections) c.deactivate()
        activeNodeCount = 0; activeConnCount = 0
    }
}

