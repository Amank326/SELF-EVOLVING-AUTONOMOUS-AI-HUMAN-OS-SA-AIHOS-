package com.aihos.ui.render.datagraph

import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber

/**
 * CognitiveGraphPass — Render pass for cognitive data visualization.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Pipeline Position: Inside GeometryPass FBO                      │
 * │  (renders between neural lattice and core geometry draws)        │
 * │                                                                  │
 * │  Render order:                                                   │
 * │    1. Edges (additive blend, behind nodes)                       │
 * │    2. Nodes (depth tested, Fresnel glow)                         │
 * │    3. Particles (additive, no depth write)                       │
 * │                                                                  │
 * │  Update sequence (called before execute):                        │
 * │    1. Force-directed layout simulation                           │
 * │    2. Particle flow animation                                    │
 * │    3. Node dynamics (energy, highlight)                          │
 * │    4. GPU instance data upload                                   │
 * │                                                                  │
 * │  Performance:                                                    │
 * │    3 draw calls total (instanced)                                │
 * │    Max 300 nodes, 600 edges, 4800 particles                      │
 * │    Quality-gated layout iterations                               │
 * │    Particles disabled on QualityLevel.LOW                        │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * This pass does NOT own its own FBO — it renders into whatever FBO
 * is currently bound (GeometryPass FBO).
 */
class CognitiveGraphPass(
    private val camera: CameraController
) : RenderPass {

    override val name = "CognitiveGraphPass"

    val graph = CognitiveGraph()
    private val renderer = CognitiveGraphRenderer()
    val timeline = EvolutionTimeline()

    var isVisible = true
    var particlesEnabled = true
    var maxVisibleNodes = CognitiveGraph.MAX_NODES

    // ════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════

    override fun initialize() {
        graph.initialize()
        renderer.initialize()
        Timber.d("$name: initialized (${graph.activeNodeCount} nodes, ${graph.activeEdgeCount} edges)")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        // No FBO to resize — renders into GeometryPass FBO
        when (qualityLevel) {
            QualityLevel.HIGH -> {
                maxVisibleNodes = CognitiveGraph.MAX_NODES
                particlesEnabled = true
            }
            QualityLevel.MEDIUM -> {
                maxVisibleNodes = 200
                particlesEnabled = true
            }
            QualityLevel.LOW -> {
                maxVisibleNodes = 100
                particlesEnabled = false
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Update (called before execute from RenderEngine)
    // ════════════════════════════════════════════════════════════════

    /**
     * Update graph physics, particles, and upload to GPU.
     * Must be called from GL thread before execute().
     */
    fun update(metrics: AIMetricsSnapshot, dt: Float, time: Float, quality: QualityLevel) {
        if (!isVisible) return

        // Update force-directed layout + particles
        graph.update(metrics, dt, time, quality)

        // Sample evolution timeline
        timeline.sample(metrics, time)

        // Upload instance data to GPU
        renderer.uploadInstanceData(graph)
    }

    // ════════════════════════════════════════════════════════════════
    // Render
    // ════════════════════════════════════════════════════════════════

    override fun execute(state: RenderState) {
        if (!isVisible) return

        // 1. Draw edges (additive blend, behind nodes)
        renderer.drawEdges(camera, state, state.aiMetrics)

        // 2. Draw nodes (depth tested, on top of edges)
        renderer.drawNodes(camera, state, state.aiMetrics)

        // 3. Draw particles (additive, no depth write)
        if (particlesEnabled) {
            renderer.drawParticles(camera, state)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════

    /** Highlight a specific node and its connected neighbors. */
    fun highlightNode(nodeIndex: Int) {
        if (nodeIndex < 0 || nodeIndex >= CognitiveGraph.MAX_NODES) return
        // Clear previous
        for (n in graph.nodes) n.highlighted = false
        val target = graph.nodes[nodeIndex]
        if (!target.active) return
        target.highlighted = true
        // Highlight connected nodes
        for (e in graph.edges) {
            if (!e.active) continue
            if (e.fromIndex == nodeIndex && graph.nodes[e.toIndex].active) {
                graph.nodes[e.toIndex].highlighted = true
            } else if (e.toIndex == nodeIndex && graph.nodes[e.fromIndex].active) {
                graph.nodes[e.fromIndex].highlighted = true
            }
        }
    }

    /** Clear all node highlights. */
    fun clearHighlights() {
        for (n in graph.nodes) n.highlighted = false
    }

    /** Performance stats string. */
    fun getStats(): String =
        "CogGraph: ${graph.activeNodeCount}N/${graph.activeEdgeCount}E " +
        "drawn=${renderer.uploadedNodeCount}/${renderer.uploadedEdgeCount}/${renderer.uploadedParticleCount}P"

    // ════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════

    override fun release() {
        renderer.release()
        Timber.d("$name: released")
    }

    fun reset() {
        graph.reset()
        graph.initialize()
        timeline.reset()
    }
}

