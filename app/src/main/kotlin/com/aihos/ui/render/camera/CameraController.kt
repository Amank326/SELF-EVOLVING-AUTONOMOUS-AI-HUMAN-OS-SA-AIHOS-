package com.aihos.ui.render.camera

import android.opengl.Matrix
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * CameraController — Modular orbital camera with AI-driven motion.
 *
 * Decoupled from renderer. Reads AIMetricsSnapshot per frame.
 * All matrices pre-allocated. Zero allocation per frame.
 *
 * AI mapping:
 *   autonomyLevel    → orbit speed
 *   cognitiveLoad    → breathing depth
 *   confidence       → damping (smoothness)
 *   selfAwareness    → reflection zoom-in
 *   evolutionRate    → elevation drift
 *
 * Future: stereoscopic VR via dual projection matrices.
 */
class CameraController {

    // ── Output matrices (read by render passes) ──────────────────
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)
    val position = FloatArray(3) // eye position in world space

    // Projection for future VR (left/right eye)
    val projectionMatrixLeft = FloatArray(16)
    val projectionMatrixRight = FloatArray(16)
    var stereoEnabled = false
        private set

    // ── Orbital state ────────────────────────────────────────────
    private var azimuth = 0f
    private var elevation = 0.3f
    private var radius = 4.0f
    private var targetRadius = 4.0f

    private var breathPhase = 0f
    private var breathAmplitude = 0.15f
    private var breathFrequency = 0.4f

    private var driftSeed = 0f
    private var driftAzimuth = 0f
    private var driftElevation = 0f

    private var damping = 0.95f
    private var azimuthVelocity = 0f
    private var elevationVelocity = 0f

    private var targetX = 0f
    private var targetY = 0f
    private var targetZ = 0f

    private var fov = 45f
    private var aspect = 1.0f
    private var nearPlane = 0.1f
    private var farPlane = 100f

    private val tempMatrix = FloatArray(16)

    val distanceToOrigin: Float get() = radius
    val currentAzimuth: Float get() = azimuth

    // ═══════════════════════════════════════════════════════════════

    fun setProjection(width: Int, height: Int) {
        aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, nearPlane, farPlane)

        // VR stereo projections (slight horizontal offset)
        if (stereoEnabled) {
            val ipd = 0.063f  // inter-pupillary distance in meters
            Matrix.perspectiveM(projectionMatrixLeft, 0, fov, aspect, nearPlane, farPlane)
            Matrix.perspectiveM(projectionMatrixRight, 0, fov, aspect, nearPlane, farPlane)
            projectionMatrixLeft[12] += ipd / 2f
            projectionMatrixRight[12] -= ipd / 2f
        }
    }

    fun update(dt: Float, aiMetrics: AIMetricsSnapshot) {
        val twoPi = (2.0 * PI).toFloat()

        // Orbit
        val orbitSpeed = 0.05f + aiMetrics.autonomyLevel * 0.2f
        azimuthVelocity += orbitSpeed * dt
        azimuth += azimuthVelocity * dt
        damping = 0.92f + aiMetrics.confidence * 0.07f
        azimuthVelocity *= damping
        if (azimuth > twoPi) azimuth -= twoPi
        if (azimuth < 0f) azimuth += twoPi

        // Breathing
        breathPhase += dt * breathFrequency * twoPi
        if (breathPhase > twoPi) breathPhase -= twoPi
        breathAmplitude = 0.05f + aiMetrics.cognitiveLoad * 0.2f
        val breathOffset = sin(breathPhase) * breathAmplitude

        // Reflection zoom
        targetRadius = 4.0f - aiMetrics.selfAwareness * 1.5f
        radius += (targetRadius + breathOffset - radius) * dt * 2.0f

        // Drift
        driftSeed += dt * 0.7f
        driftAzimuth = sin(driftSeed * 1.1f) * 0.02f
        driftElevation = sin(driftSeed * 0.8f + 1.5f) * 0.015f

        // Elevation
        val baseElev = 0.25f + sin(driftSeed * 0.3f) * aiMetrics.evolutionRate * 0.15f
        elevationVelocity += (baseElev - elevation) * dt * 0.5f
        elevation += elevationVelocity * dt
        elevationVelocity *= damping
        elevation = elevation.coerceIn(-0.8f, 1.2f)

        // Compute position
        val finalAz = azimuth + driftAzimuth
        val finalEl = elevation + driftElevation
        val cosEl = cos(finalEl)
        position[0] = cos(finalAz) * cosEl * radius
        position[1] = sin(finalEl) * radius
        position[2] = sin(finalAz) * cosEl * radius

        // View matrix
        Matrix.setLookAtM(viewMatrix, 0,
            position[0], position[1], position[2],
            targetX, targetY, targetZ,
            0f, 1f, 0f)

        // VP
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    /**
     * Compute normal matrix from the given model matrix.
     * Extracts upper-left 3x3 of (view * model).
     */
    fun computeNormalMatrix(modelMatrix: FloatArray, out: FloatArray) {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        out[0] = tempMatrix[0]; out[1] = tempMatrix[1]; out[2] = tempMatrix[2]
        out[3] = tempMatrix[4]; out[4] = tempMatrix[5]; out[5] = tempMatrix[6]
        out[6] = tempMatrix[8]; out[7] = tempMatrix[9]; out[8] = tempMatrix[10]
    }

    fun setTarget(x: Float, y: Float, z: Float) { targetX = x; targetY = y; targetZ = z }
    fun easeToRadius(r: Float) { targetRadius = r.coerceIn(1.5f, 10f) }
    fun enableStereo(enable: Boolean) { stereoEnabled = enable }

    fun reset() {
        azimuth = 0f; elevation = 0.3f; radius = 4.0f; targetRadius = 4.0f
        breathPhase = 0f; driftSeed = 0f
        azimuthVelocity = 0f; elevationVelocity = 0f
        targetX = 0f; targetY = 0f; targetZ = 0f
    }
}

