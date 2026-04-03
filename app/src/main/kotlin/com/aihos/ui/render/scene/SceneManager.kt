package com.aihos.ui.render.scene

import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.RenderState

/**
 * SceneManager — Owns the scene graph root and all nodes.
 *
 * Responsibilities:
 *   - Holds the root SceneNode
 *   - Manages node lifecycle (add / remove / find)
 *   - Runs per-frame animation on all nodes
 *   - Updates transform hierarchy
 *
 * Does NOT hold GL resources — those belong to render passes.
 */
class SceneManager {

    val root = SceneNode("scene_root")

    // Named node registry for fast lookup
    private val nodeRegistry = HashMap<String, SceneNode>(16)

    // ── Pre-built standard scene nodes ───────────────────────────
    val brainNode = AIBrainNode()
    val particleNode = ParticleFieldNode()

    init {
        root.addChild(brainNode)
        root.addChild(particleNode)
        register(brainNode)
        register(particleNode)
    }

    // ── Node management ──────────────────────────────────────────

    fun register(node: SceneNode) {
        nodeRegistry[node.id] = node
    }

    fun findNode(id: String): SceneNode? = nodeRegistry[id]

    fun addToRoot(node: SceneNode) {
        root.addChild(node)
        register(node)
    }

    fun removeFromRoot(node: SceneNode) {
        root.removeChild(node)
        nodeRegistry.remove(node.id)
    }

    // ── Per-frame update ─────────────────────────────────────────

    /**
     * Animate all nodes, then update transform hierarchy.
     * Call once per frame before rendering.
     */
    fun update(state: RenderState) {
        // Animate all registered nodes
        animateRecursive(root, state.elapsedTime, state.deltaTime, state.aiMetrics)

        // Propagate transforms
        root.updateTransforms(parentWorld = null)
    }

    private fun animateRecursive(node: SceneNode, time: Float, dt: Float, ai: AIMetricsSnapshot) {
        node.animate(time, dt, ai)
        for (child in node.children) {
            animateRecursive(child, time, dt, ai)
        }
    }

    // ── Traversal helpers ────────────────────────────────────────

    /**
     * Collect all visible nodes of a given type.
     */
    inline fun <reified T : SceneNode> getVisibleNodesOfType(): List<T> {
        val result = mutableListOf<T>()
        collectVisible(root, T::class.java, result)
        return result
    }

    fun <T : SceneNode> collectVisible(node: SceneNode, type: Class<T>, out: MutableList<T>) {
        if (!node.visible) return
        if (type.isInstance(node)) {
            @Suppress("UNCHECKED_CAST")
            out.add(node as T)
        }
        for (child in node.children) collectVisible(child, type, out)
    }
}

