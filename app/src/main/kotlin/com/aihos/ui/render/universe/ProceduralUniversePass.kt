package com.aihos.ui.render.universe

import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber

/**
 * ProceduralUniversePass — Render pass integrating the procedural universe
 * into the multi-pass render pipeline.
 *
 * Pipeline position: INSIDE GeometryPass (called as sub-renderer),
 * or standalone before GeometryPass for background layers.
 *
 * Render order within this pass:
 *   1. Nebula background (full-screen, farthest depth)
 *   2. Starfield (instanced points, far depth)
 *   3. Ambient particles (instanced points, mid depth)
 *   4. [other geometry rendered by GeometryPass]
 *   5. Energy field overlay (full-screen, composited on top)
 *
 * This pass does NOT own an FBO — it renders into whatever FBO is
 * currently bound (GeometryPass FBO for standard mode, or eye FBO
 * for stereo mode).
 *
 * Thread safety:
 *   - All rendering on GL thread only
 *   - State read from immutable RenderState
 *   - Zero allocation in render loop
 */
class ProceduralUniversePass(
    private val camera: CameraController
) : RenderPass {

    override val name = "ProceduralUniversePass"

    private lateinit var renderer: ProceduralUniverseRenderer
    var isVisible = true

    // ═══════════════════════════════════════════════════════════════
    // RenderPass Lifecycle
    // ═══════════════════════════════════════════════════════════════

    override fun initialize() {
        renderer = ProceduralUniverseRenderer(camera)
        renderer.initialize()
        Timber.d("$name: initialized")
    }

    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        if (::renderer.isInitialized) {
            renderer.resize(width, height, qualityLevel)
        }
    }

    override fun execute(state: RenderState) {
        // This pass is invoked by GeometryPass sub-renders, not the main pipeline directly.
        // Use renderBackground() and renderOverlay() individually.
    }

    override fun release() {
        if (::renderer.isInitialized) {
            renderer.release()
        }
        Timber.d("$name: released")
    }

    // ═══════════════════════════════════════════════════════════════
    // Sub-render methods (called by GeometryPass at correct moments)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Render background layers: nebula + starfield.
     * Call BEFORE any 3D geometry in GeometryPass.
     */
    fun renderBackground(state: RenderState) {
        if (!isVisible || !::renderer.isInitialized) return

        val metrics = state.aiMetrics
        val time = state.elapsedTime

        // Layer 0: Nebula clouds (farthest)
        renderer.renderNebulaBackground(time, metrics)

        // Layer 1: Starfield
        renderer.renderStarfield(time, metrics)
    }

    /**
     * Render mid-depth layers: ambient particles.
     * Call AFTER background but BEFORE core AI geometry.
     */
    fun renderMidLayers(state: RenderState) {
        if (!isVisible || !::renderer.isInitialized) return

        // Layer 2: Ambient data particles
        renderer.renderAmbientParticles(state.elapsedTime, state.aiMetrics)
    }

    /**
     * Render overlay layers: energy field distortion.
     * Call AFTER all 3D geometry (before post-processing).
     */
    fun renderOverlay(state: RenderState) {
        if (!isVisible || !::renderer.isInitialized) return

        // Layer 3: Energy field around AI core
        renderer.renderEnergyField(state.elapsedTime, state.aiMetrics)
    }

    // ═══════════════════════════════════════════════════════════════
    // Performance API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Adjust rendering quality based on current FPS.
     * Called by RenderEngine's frame timer.
     */
    fun adjustForPerformance(fps: Float) {
        if (::renderer.isInitialized) {
            renderer.adjustForPerformance(fps)
        }
    }

    /**
     * Get current stats for debugging.
     */
    fun getStats(): String {
        if (!::renderer.isInitialized) return "$name: not initialized"
        return "$name: visible=$isVisible nebulaIntensity=${renderer.nebulaIntensity}"
    }
}

