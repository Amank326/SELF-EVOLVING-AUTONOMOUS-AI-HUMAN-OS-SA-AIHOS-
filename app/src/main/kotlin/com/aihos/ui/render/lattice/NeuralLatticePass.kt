package com.aihos.ui.render.lattice
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber
/**
 * NeuralLatticePass — Render pass that manages the full lattice lifecycle.
 *
 * Render order in the pipeline:
 *   1. Background (GeometryPass draws nebula)
 *   2. Neural Lattice (THIS PASS — beams first, then nodes)
 *   3. Core geometry (GeometryPass draws icosphere)
 *   4. Particles (GeometryPass draws particle cloud)
 *   5. Post-processing (Bloom + Composite)
 *
 * This pass does NOT use its own FBO — it renders into whatever
 * FBO is currently bound (typically GeometryPass's FBO).
 * It is called explicitly by GeometryPass between background and core draws.
 */
class NeuralLatticePass(
    private val camera: CameraController
) : RenderPass {
    override val name = "NeuralLatticePass"
    val lattice = NeuralLattice()
    private val evolver = TopologyEvolver()
    private val physics = LatticePhysics()
    private val renderer = NeuralLatticeRenderer()
    private var activeNodeCount = 0
    private var activeBeamCount = 0
    override fun initialize() {
        lattice.initialize()
        renderer.initialize()
        Timber.d("$name: initialized (${lattice.activeNodeCount} nodes, ${lattice.activeConnCount} connections)")
    }
    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        // No FBO to resize — we render into the current bound FBO
    }
    /**
     * Update lattice state: topology evolution + physics + GPU upload.
     * Must be called before execute().
     */
    fun update(metrics: AIMetricsSnapshot, dt: Float, time: Float) {
        // 1. Evolve topology (at most 1 mutation per cooldown)
        evolver.update(lattice, metrics, dt, time)
        // 2. Physics simulation (spring-damper forces)
        physics.update(lattice, metrics, dt, time)
        // 3. Count active elements
        activeNodeCount = 0
        activeBeamCount = 0
        for (n in lattice.nodes) { if (n.active) activeNodeCount++ }
        for (c in lattice.connections) {
            if (!c.active) continue
            val fi = c.fromIndex; val ti = c.toIndex
            if (fi < 0 || ti < 0 || fi >= lattice.nodes.size || ti >= lattice.nodes.size) continue
            if (lattice.nodes[fi].active && lattice.nodes[ti].active) activeBeamCount++
        }
        // 4. Upload to GPU (glBufferSubData, no realloc)
        renderer.uploadInstanceData(lattice)
    }
    override fun execute(state: RenderState) {
        // Draw beams first (additive blend, behind nodes)
        renderer.drawBeams(camera, state, state.aiMetrics, activeBeamCount)
        // Draw nodes on top
        renderer.drawNodes(camera, state, activeNodeCount)
    }
    override fun release() {
        renderer.release()
        Timber.d("$name: released")
    }
    fun reset() {
        lattice.initialize()
        evolver.reset()
    }
}
