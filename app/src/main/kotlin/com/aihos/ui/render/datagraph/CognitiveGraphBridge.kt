package com.aihos.ui.render.datagraph

import java.util.concurrent.atomic.AtomicReference

/**
 * CognitiveGraphBridge — Thread-safe bridge for pushing graph topology
 * mutations from ViewModel/coroutine threads to the GL thread.
 *
 * ┌──────────────────┐       ┌─────────────────────┐       ┌──────────────┐
 * │  ViewModel       │       │ CognitiveGraphBridge │       │  GL Thread   │
 * │  (Main/IO)       │──────▶│  AtomicReference     │──────▶│  consume()   │
 * │  pushSnapshot()  │       │  (lock-free)         │       │  per frame   │
 * └──────────────────┘       └─────────────────────┘       └──────────────┘
 *
 * Snapshot contains topology delta commands (add/remove nodes/edges).
 * Rate-limited: at most 1 mutation batch consumed per frame.
 * Immutable snapshot data class — safe to share across threads.
 */
class CognitiveGraphBridge {

    /**
     * Immutable snapshot of graph topology commands.
     * Pushed from any thread, consumed on GL thread once per frame.
     */
    data class GraphSnapshot(
        val addNodes: List<NodeCommand> = emptyList(),
        val removeNodeIndices: List<Int> = emptyList(),
        val addEdges: List<EdgeCommand> = emptyList(),
        val removeEdgeIndices: List<Int> = emptyList(),
        val highlightNodeIndex: Int = -1,
        val clearHighlights: Boolean = false,
        val resetGraph: Boolean = false,
        val timestamp: Long = System.nanoTime()
    )

    data class NodeCommand(
        val x: Float, val y: Float, val z: Float,
        val type: NodeType, val weight: Float, val phase: Float
    )

    data class EdgeCommand(
        val fromIndex: Int, val toIndex: Int,
        val strength: Float, val type: EdgeType, val direction: Float = 1f
    )

    private val snapshotRef = AtomicReference<GraphSnapshot?>(null)

    /**
     * Push a graph topology mutation snapshot from any thread.
     * Lock-free, wait-free. Latest snapshot wins (overwrites previous if not consumed).
     */
    fun pushSnapshot(snapshot: GraphSnapshot) {
        snapshotRef.set(snapshot)
    }

    /**
     * Consume the latest snapshot on the GL thread. Returns null if no new snapshot.
     * Call once per frame from CognitiveGraphPass.update().
     */
    fun consumeSnapshot(): GraphSnapshot? {
        return snapshotRef.getAndSet(null)
    }

    /**
     * Apply a consumed snapshot to the cognitive graph.
     * Called on GL thread after consumeSnapshot().
     */
    fun applyToGraph(snapshot: GraphSnapshot, graph: CognitiveGraph) {
        if (snapshot.resetGraph) {
            graph.reset()
            graph.initialize()
            return
        }

        // Remove edges first (before removing nodes that might be endpoints)
        for (edgeIdx in snapshot.removeEdgeIndices) {
            graph.disconnect(edgeIdx)
        }

        // Remove nodes
        for (nodeIdx in snapshot.removeNodeIndices) {
            graph.removeNode(nodeIdx)
        }

        // Add new nodes
        for (cmd in snapshot.addNodes) {
            graph.addNode(cmd.x, cmd.y, cmd.z, cmd.type, cmd.weight, cmd.phase)
        }

        // Add new edges
        for (cmd in snapshot.addEdges) {
            graph.connect(cmd.fromIndex, cmd.toIndex, cmd.strength, cmd.type, cmd.direction)
        }

        // Highlight handling
        if (snapshot.clearHighlights) {
            for (n in graph.nodes) n.highlighted = false
        }
        if (snapshot.highlightNodeIndex >= 0) {
            val target = graph.nodes.getOrNull(snapshot.highlightNodeIndex)
            if (target?.active == true) {
                target.highlighted = true
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Convenience builders for common operations
    // ════════════════════════════════════════════════════════════════

    companion object {
        /** Create a snapshot that adds a single reasoning event node. */
        fun reasoningEvent(
            x: Float, y: Float, z: Float,
            type: NodeType = NodeType.INFERENCE,
            weight: Float = 0.6f
        ): GraphSnapshot = GraphSnapshot(
            addNodes = listOf(NodeCommand(x, y, z, type, weight, System.nanoTime().toFloat() * 1e-9f))
        )

        /** Create a snapshot that resets the entire graph. */
        fun resetAll(): GraphSnapshot = GraphSnapshot(resetGraph = true)

        /** Create a snapshot that highlights a specific node. */
        fun highlight(nodeIndex: Int): GraphSnapshot = GraphSnapshot(
            highlightNodeIndex = nodeIndex, clearHighlights = true
        )
    }
}

