package com.aihos.ui.render.hud

import android.opengl.Matrix
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * HUDPanel — A single floating holographic panel in 3D space.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  HUDPanel Memory Layout (pre-allocated, mutable)               │
 * │                                                                 │
 * │  position[3]    — world-space center                            │
 * │  rotation[3]    — euler angles (pitch, yaw, roll)               │
 * │  scale[2]       — width, height in world units                  │
 * │  modelMatrix[16]— computed transform                            │
 * │  color[4]       — base tint rgba                                │
 * │  opacity         — master alpha [0,1]                           │
 * │  glowIntensity   — edge glow strength [0,1]                    │
 * │  dataValue       — displayed metric [0,1]                       │
 * │  scanlineSpeed   — animated scanline rate                       │
 * │  layer           — depth layer (0=near, 1=mid, 2=far)          │
 * │  panelType       — visual type (metric, arc, bar, graph)       │
 * │  active          — visibility flag                              │
 * │  hovered         — touch hover state                            │
 * │  hoverAlpha      — smooth hover transition [0,1]               │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * All fields pre-allocated. Zero allocation during updates.
 * Panels stored in fixed-size pool (HUDManager).
 */
class HUDPanel {
    val position = FloatArray(3)
    val rotation = FloatArray(3)          // pitch, yaw, roll in radians
    val scale = floatArrayOf(0.8f, 0.5f) // width, height
    val modelMatrix = FloatArray(16)
    val color = floatArrayOf(0f, 0.7f, 1f, 0.3f) // cyan semi-transparent
    var opacity = 0.6f
    var glowIntensity = 0.5f
    var dataValue = 0.5f
    var scanlineSpeed = 1f
    var layer = 0                // 0=near, 1=mid, 2=far
    var panelType = PanelType.METRIC
    var active = false
    var hovered = false
    var hoverAlpha = 0f          // smooth 0→1 on hover
    var phase = 0f               // animation phase offset
    var anchorAngle = 0f         // angle around camera orbit

    // Computed: world-space corners for raycasting (updated each frame)
    val cornerTL = FloatArray(3)
    val cornerTR = FloatArray(3)
    val cornerBL = FloatArray(3)
    val cornerBR = FloatArray(3)
    val normal = FloatArray(3)

    fun activate(
        px: Float, py: Float, pz: Float,
        w: Float, h: Float,
        panelLayer: Int, type: PanelType, angle: Float
    ) {
        position[0] = px; position[1] = py; position[2] = pz
        scale[0] = w; scale[1] = h
        layer = panelLayer; panelType = type; anchorAngle = angle
        active = true; hovered = false; hoverAlpha = 0f
        opacity = when (panelLayer) {
            0 -> 0.65f   // near: more opaque
            1 -> 0.45f   // mid
            else -> 0.25f // far: ghostly
        }
        glowIntensity = when (panelLayer) {
            0 -> 0.7f; 1 -> 0.5f; else -> 0.3f
        }
        phase = angle * 0.5f
    }

    fun deactivate() {
        active = false; hovered = false; hoverAlpha = 0f
    }

    /**
     * Recompute model matrix from position/rotation/scale.
     * Called once per frame per active panel.
     */
    fun computeModelMatrix() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2])
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(rotation[1].toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(rotation[0].toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(rotation[2].toDouble()).toFloat(), 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scale[0], scale[1], 1f)

        // Compute world-space corners
        computeCorners()
    }

    private fun computeCorners() {
        val hw = 0.5f; val hh = 0.5f
        transformPoint(-hw, hh, 0f, cornerTL)
        transformPoint(hw, hh, 0f, cornerTR)
        transformPoint(-hw, -hh, 0f, cornerBL)
        transformPoint(hw, -hh, 0f, cornerBR)
        // Normal = cross(TR-TL, BL-TL), normalized
        val ax = cornerTR[0] - cornerTL[0]; val ay = cornerTR[1] - cornerTL[1]; val az = cornerTR[2] - cornerTL[2]
        val bx = cornerBL[0] - cornerTL[0]; val by = cornerBL[1] - cornerTL[1]; val bz = cornerBL[2] - cornerTL[2]
        normal[0] = ay * bz - az * by
        normal[1] = az * bx - ax * bz
        normal[2] = ax * by - ay * bx
        val len = kotlin.math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2])
        if (len > 0.0001f) { normal[0] /= len; normal[1] /= len; normal[2] /= len }
    }

    private val tempVec = FloatArray(4)
    private val tempOut = FloatArray(4)

    private fun transformPoint(x: Float, y: Float, z: Float, out: FloatArray) {
        tempVec[0] = x; tempVec[1] = y; tempVec[2] = z; tempVec[3] = 1f
        Matrix.multiplyMV(tempOut, 0, modelMatrix, 0, tempVec, 0)
        out[0] = tempOut[0]; out[1] = tempOut[1]; out[2] = tempOut[2]
    }
}

enum class PanelType {
    METRIC,       // Single value display (number + label)
    ARC,          // Circular progress arc
    BAR,          // Horizontal/vertical bar
    GRAPH,        // Mini scrolling graph
    STATUS,       // Status indicator (dot + text)
    AMBIENT       // Far-layer floating data fragment
}

/**
 * HUDManager — Pre-allocated pool of HUD panels + layout controller.
 *
 * Layout strategy:
 *   Panels are arranged in 3 concentric arcs around the camera:
 *     Near (layer 0): 4 panels — cognitive load, confidence, health, memory
 *     Mid (layer 1):  4 panels — evolution rate, autonomy, self-awareness, animation
 *     Far (layer 2):  8 panels — ambient floating data fragments
 *
 * Panels track camera: they orbit with the camera's azimuth
 * but with slight parallax offset per layer.
 */
class HUDManager {

    companion object {
        const val MAX_PANELS = 16
        // Near layer: smaller, closer, more opaque
        const val NEAR_RADIUS = 2.0f
        const val NEAR_Y_OFFSET = 0.5f
        // Mid layer
        const val MID_RADIUS = 3.2f
        const val MID_Y_OFFSET = 0.2f
        // Far layer
        const val FAR_RADIUS = 5.0f
        const val FAR_Y_OFFSET = -0.1f
    }

    val panels = Array(MAX_PANELS) { HUDPanel() }
    var activePanelCount = 0; private set

    /**
     * Initialize default HUD layout.
     */
    fun initialize() {
        deactivateAll()
        val twoPi = (2.0 * PI).toFloat()

        // ── Near layer: 4 metric panels ──────────────────────
        val nearTypes = arrayOf(PanelType.ARC, PanelType.ARC, PanelType.BAR, PanelType.BAR)
        for (i in 0 until 4) {
            val angle = twoPi * i / 4 + twoPi / 8 // offset by 45°
            val x = cos(angle) * NEAR_RADIUS
            val z = sin(angle) * NEAR_RADIUS
            panels[i].activate(x, NEAR_Y_OFFSET, z, 0.55f, 0.4f, 0, nearTypes[i], angle)
            activePanelCount++
        }

        // ── Mid layer: 4 panels ──────────────────────────────
        val midTypes = arrayOf(PanelType.METRIC, PanelType.METRIC, PanelType.GRAPH, PanelType.STATUS)
        for (i in 0 until 4) {
            val angle = twoPi * i / 4  // aligned to cardinal directions
            val x = cos(angle) * MID_RADIUS
            val z = sin(angle) * MID_RADIUS
            panels[4 + i].activate(x, MID_Y_OFFSET, z, 0.7f, 0.45f, 1, midTypes[i], angle)
            activePanelCount++
        }

        // ── Far layer: 8 ambient panels ──────────────────────
        for (i in 0 until 8) {
            val angle = twoPi * i / 8 + twoPi / 16
            val x = cos(angle) * FAR_RADIUS
            val z = sin(angle) * FAR_RADIUS
            val y = FAR_Y_OFFSET + sin(angle * 2f) * 0.4f
            panels[8 + i].activate(x, y, z, 0.4f, 0.25f, 2, PanelType.AMBIENT, angle)
            activePanelCount++
        }
    }

    /**
     * Update panel positions + data values each frame.
     *
     * @param cameraAzimuth current camera orbit angle
     * @param metrics current AI state
     * @param dt delta time
     * @param time elapsed time
     */
    fun update(
        cameraAzimuth: Float,
        metrics: AIMetricsSnapshot,
        dt: Float,
        time: Float
    ) {
        // Parallax offsets per layer
        val parallax = floatArrayOf(0.85f, 0.6f, 0.3f) // near tracks camera more

        for (p in panels) {
            if (!p.active) continue

            // Orbit position: follow camera azimuth with parallax
            val layerParallax = parallax[p.layer.coerceIn(0, 2)]
            val effectiveAngle = p.anchorAngle + cameraAzimuth * layerParallax
            val radius = when (p.layer) {
                0 -> NEAR_RADIUS; 1 -> MID_RADIUS; else -> FAR_RADIUS
            }
            val baseY = when (p.layer) {
                0 -> NEAR_Y_OFFSET; 1 -> MID_Y_OFFSET; else -> FAR_Y_OFFSET
            }

            p.position[0] = cos(effectiveAngle) * radius
            p.position[2] = sin(effectiveAngle) * radius
            p.position[1] = baseY + sin(time * 0.5f + p.phase) * 0.05f // subtle bob

            // Face camera center (billboard toward origin)
            p.rotation[1] = effectiveAngle + (PI.toFloat()) // face inward

            // Hover smooth transition
            p.hoverAlpha += ((if (p.hovered) 1f else 0f) - p.hoverAlpha) * dt * 8f

            // Recompute transform
            p.computeModelMatrix()
        }

        // Update data values from metrics
        updateDataFromMetrics(metrics, time)
    }

    private fun updateDataFromMetrics(m: AIMetricsSnapshot, time: Float) {
        // Near layer (0-3): core metrics
        if (panels[0].active) panels[0].dataValue = m.cognitiveLoad
        if (panels[1].active) panels[1].dataValue = m.confidence
        if (panels[2].active) panels[2].dataValue = m.systemHealth
        if (panels[3].active) panels[3].dataValue = m.memoryLoad

        // Mid layer (4-7): secondary metrics
        if (panels[4].active) panels[4].dataValue = m.evolutionRate
        if (panels[5].active) panels[5].dataValue = m.autonomyLevel
        if (panels[6].active) panels[6].dataValue = m.selfAwareness
        if (panels[7].active) panels[7].dataValue = m.animationIntensity

        // Far layer (8-15): ambient pulsing values
        for (i in 8 until MAX_PANELS) {
            if (!panels[i].active) continue
            panels[i].dataValue = (sin(time * 0.8f + i * 0.7f) * 0.5f + 0.5f)
        }
    }

    private fun deactivateAll() {
        for (p in panels) p.deactivate()
        activePanelCount = 0
    }
}

