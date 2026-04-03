package com.aihos.ui.render.datagraph

import kotlin.math.sqrt

/**
 * CognitiveNode — A single node in the cognitive reasoning graph.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  CognitiveNode Memory Layout (pre-allocated, mutable struct)   │
 * │                                                                 │
 * │  position[3]    — world-space xyz                               │
 * │  velocity[3]    — physics velocity for force-directed layout    │
 * │  targetPos[3]   — spring target (initial layout position)       │
 * │  color[4]       — rgba tint (derived from nodeType + state)     │
 * │  radius          — visual scale                                 │
 * │  energy          — glow intensity [0,1]                         │
 * │  nodeType        — DECISION/MEMORY/BELIEF/INFERENCE/INSIGHT     │
 * │  weight          — importance / confidence [0,1]                │
 * │  active          — visibility flag                              │
 * │  age             — seconds since activation                     │
 * │  phase           — animation phase offset                       │
 * │  connectionCount — number of connected edges                    │
 * │  highlighted     — selection/focus state for interaction        │
 * │  highlightAlpha  — smooth highlight transition [0,1]            │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * All fields pre-allocated. Zero heap allocation during frame updates.
 * Nodes stored in fixed-size array (object pool pattern).
 */
class CognitiveNode {

    val position = FloatArray(3)
    val velocity = FloatArray(3)
    val targetPos = FloatArray(3)
    val color = floatArrayOf(0f, 0.8f, 1f, 1f)

    var radius = 0.08f
    var energy = 0.5f
    var nodeType = NodeType.BELIEF
    var weight = 0.5f
    var active = false
    var age = 0f
    var phase = 0f
    var connectionCount = 0
    var highlighted = false
    var highlightAlpha = 0f
    var clusterGroup = 0

    fun activate(
        x: Float, y: Float, z: Float,
        type: NodeType, initialWeight: Float, nodePhase: Float
    ) {
        position[0] = x; position[1] = y; position[2] = z
        targetPos[0] = x; targetPos[1] = y; targetPos[2] = z
        velocity[0] = 0f; velocity[1] = 0f; velocity[2] = 0f
        nodeType = type
        weight = initialWeight
        phase = nodePhase
        active = true
        age = 0f
        connectionCount = 0
        highlighted = false
        highlightAlpha = 0f
        energy = initialWeight.coerceIn(0.2f, 1f)

        // Set color and radius based on type
        when (type) {
            NodeType.DECISION -> {
                color[0] = 1f; color[1] = 0.4f; color[2] = 0.1f; color[3] = 1f
                radius = 0.12f
            }
            NodeType.MEMORY -> {
                color[0] = 0.2f; color[1] = 0.6f; color[2] = 1f; color[3] = 1f
                radius = 0.07f
            }
            NodeType.BELIEF -> {
                color[0] = 0f; color[1] = 0.9f; color[2] = 0.7f; color[3] = 1f
                radius = 0.08f
            }
            NodeType.INFERENCE -> {
                color[0] = 0.8f; color[1] = 0.3f; color[2] = 1f; color[3] = 1f
                radius = 0.09f
            }
            NodeType.INSIGHT -> {
                color[0] = 1f; color[1] = 0.9f; color[2] = 0.2f; color[3] = 1f
                radius = 0.1f
            }
            NodeType.RULE -> {
                color[0] = 0.3f; color[1] = 1f; color[2] = 0.4f; color[3] = 1f
                radius = 0.06f
            }
        }
    }

    fun deactivate() {
        active = false
        connectionCount = 0
        energy = 0f
        highlighted = false
        highlightAlpha = 0f
    }

    fun distanceTo(other: CognitiveNode): Float {
        val dx = position[0] - other.position[0]
        val dy = position[1] - other.position[1]
        val dz = position[2] - other.position[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceToSq(other: CognitiveNode): Float {
        val dx = position[0] - other.position[0]
        val dy = position[1] - other.position[1]
        val dz = position[2] - other.position[2]
        return dx * dx + dy * dy + dz * dz
    }
}

/**
 * Node types representing different cognitive processes.
 * Each type has distinct visual appearance and force-layout behavior.
 *
 * typeIndex used as shader uniform for GPU-side type-based effects.
 */
enum class NodeType(val typeIndex: Int) {
    DECISION(0),     // Orange — high-importance decision points
    MEMORY(1),       // Blue — memory references
    BELIEF(2),       // Cyan — belief/knowledge nodes
    INFERENCE(3),    // Purple — inference chain nodes
    INSIGHT(4),      // Gold — learning insights
    RULE(5);         // Green — rule nodes

    companion object {
        fun fromIndex(index: Int): NodeType = entries.getOrElse(index) { BELIEF }
    }
}

