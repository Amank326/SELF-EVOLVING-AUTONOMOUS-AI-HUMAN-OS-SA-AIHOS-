package com.aihos.ui.render.hud
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import com.aihos.ui.render.core.RenderPass
import com.aihos.ui.render.core.RenderState
import timber.log.Timber
/**
 * HolographicHUDPass — Render pass for the floating holographic HUD system.
 *
 * Lifecycle:
 *   initialize() -> creates HUD layout + GPU resources
 *   update()     -> updates panel positions/data (called before execute)
 *   execute()    -> draws glass panels + data overlays (2 instanced draw calls)
 *   release()    -> frees all GPU resources
 *
 * Render position: AFTER core geometry, BEFORE particles in GeometryPass FBO.
 *                  Alternatively, rendered as post-geometry overlay.
 *
 * This pass does NOT own an FBO — renders into whatever is currently bound.
 */
class HolographicHUDPass(
    private val camera: CameraController
) : RenderPass {
    override val name = "HolographicHUDPass"
    val hudManager = HUDManager()
    val raycaster = HUDRaycaster()
    private val renderer = HUDRenderer()
    private var activePanelCount = 0
    private var cameraAzimuth = 0f
    override fun initialize() {
        hudManager.initialize()
        renderer.initialize()
        Timber.d("$name: initialized (${hudManager.activePanelCount} panels)")
    }
    override fun resize(width: Int, height: Int, qualityLevel: QualityLevel) {
        // No FBO to resize
    }
    /**
     * Update HUD state: positions, data, hover transitions.
     * Must be called once per frame before execute().
     */
    fun update(metrics: AIMetricsSnapshot, dt: Float, time: Float, camAzimuth: Float) {
        cameraAzimuth = camAzimuth
        hudManager.update(cameraAzimuth, metrics, dt, time)
        // Count active panels
        activePanelCount = 0
        for (p in hudManager.panels) { if (p.active) activePanelCount++ }
        // Upload to GPU
        renderer.uploadInstanceData(hudManager, time)
    }
    override fun execute(state: RenderState) {
        if (activePanelCount <= 0) return
        // Draw glass panels first (semi-transparent)
        renderer.drawGlassPanels(camera, state, activePanelCount)
        // Draw data overlays on top (additive blend)
        renderer.drawDataOverlays(camera, state, activePanelCount)
    }
    /**
     * Process touch event for panel interaction.
     * Call from GLSurfaceView's touch handler via queueEvent.
     */
    fun onTouch(screenX: Float, screenY: Float, screenW: Int, screenH: Int): Int {
        return raycaster.raycast(screenX, screenY, screenW, screenH, camera, hudManager)
    }
    fun onTouchRelease() {
        raycaster.clearHover(hudManager)
    }
    override fun release() {
        renderer.release()
        Timber.d("$name: released")
    }
}
