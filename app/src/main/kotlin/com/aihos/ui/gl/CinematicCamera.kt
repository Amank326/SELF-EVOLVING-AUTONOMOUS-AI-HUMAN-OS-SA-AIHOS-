package com.aihos.ui.gl

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * CinematicCamera — Orbital camera with breathing, drift, easing, and AI-driven motion.
 *
 * Motion modes:
 *   • ORBIT       — Slow auto-rotation around Y axis
 *   • BREATHING   — Sinusoidal radius oscillation
 *   • DRIFT       — Micro random-walk on azimuth/elevation
 *   • REFLECTION  — Slow zoom-in for deep introspection
 *
 * All matrices are pre-allocated. Zero allocation per frame.
 *
 * AI metric mapping:
 *   autonomyLevel    → orbit speed
 *   cognitiveLoad    → breathing depth
 *   confidence       → camera smoothness (damping)
 *   reflectionDepth  → slow-motion zoom factor
 */
class CinematicCamera {

    // ── Pre-allocated matrices (16 floats each) ──────────────────
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    // ── Camera position (computed each frame, readable externally) ─
    val position = FloatArray(3) // x, y, z

    // ── Orbital parameters ───────────────────────────────────────
    private var azimuth = 0f            // radians around Y axis
    private var elevation = 0.3f        // radians above/below XZ plane
    private var radius = 4.0f           // distance from origin
    private var targetRadius = 4.0f

    // ── Breathing parameters ─────────────────────────────────────
    private var breathPhase = 0f
    private var breathAmplitude = 0.15f
    private var breathFrequency = 0.4f  // Hz

    // ── Drift parameters ─────────────────────────────────────────
    private var driftAzimuth = 0f
    private var driftElevation = 0f
    private var driftSeed = 0f

    // ── Look-at target ───────────────────────────────────────────
    private var targetX = 0f
    private var targetY = 0f
    private var targetZ = 0f

    // ── Damping ──────────────────────────────────────────────────
    private var damping = 0.95f  // higher = smoother = slower response
    private var azimuthVelocity = 0f
    private var elevationVelocity = 0f

    // ── Projection ───────────────────────────────────────────────
    private var fov = 45f
    private var aspect = 1.0f
    private var nearPlane = 0.1f
    private var farPlane = 100f

    // ═══════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize projection. Call from onSurfaceChanged.
     */
    fun setProjection(width: Int, height: Int) {
        aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, nearPlane, farPlane)
    }

    /**
     * Update camera each frame. Call from onDrawFrame BEFORE drawing.
     *
     * @param dt delta time in seconds
     * @param autonomyLevel  [0..1] — controls orbit speed
     * @param cognitiveLoad  [0..1] — controls breathing depth
     * @param confidence     [0..1] — controls smoothness (higher = smoother)
     * @param reflectionDepth [0..1] — controls slow zoom-in
     * @param evolutionRate  [0..1] — controls elevation drift speed
     */
    fun update(
        dt: Float,
        autonomyLevel: Float = 0.5f,
        cognitiveLoad: Float = 0.5f,
        confidence: Float = 0.5f,
        reflectionDepth: Float = 0.0f,
        evolutionRate: Float = 0.5f
    ) {
        // ── Orbit rotation (AI-driven speed) ─────────────────
        val orbitSpeed = 0.05f + autonomyLevel * 0.2f  // rad/sec
        azimuthVelocity += orbitSpeed * dt
        azimuth += azimuthVelocity * dt

        // Apply damping (higher confidence = smoother)
        damping = 0.92f + confidence * 0.07f  // range [0.92, 0.99]
        azimuthVelocity *= damping

        // Normalize azimuth to [0, 2π]
        val twoPi = (2.0 * PI).toFloat()
        if (azimuth > twoPi) azimuth -= twoPi
        if (azimuth < 0f) azimuth += twoPi

        // ── Breathing (sinusoidal radius oscillation) ────────
        breathPhase += dt * breathFrequency * twoPi
        if (breathPhase > twoPi) breathPhase -= twoPi
        breathAmplitude = 0.05f + cognitiveLoad * 0.2f  // deeper breath with higher load

        val breathOffset = sin(breathPhase) * breathAmplitude

        // ── Reflection zoom ──────────────────────────────────
        targetRadius = 4.0f - reflectionDepth * 1.5f  // zoom in during reflection
        radius += (targetRadius + breathOffset - radius) * dt * 2.0f  // smooth lerp

        // ── Micro drift ──────────────────────────────────────
        driftSeed += dt * 0.7f
        driftAzimuth = sin(driftSeed * 1.1f) * 0.02f
        driftElevation = sin(driftSeed * 0.8f + 1.5f) * 0.015f

        // ── Elevation drift (subtle, evolution-driven) ───────
        val baseElevation = 0.25f + sin(driftSeed * 0.3f) * evolutionRate * 0.15f
        elevationVelocity += (baseElevation - elevation) * dt * 0.5f
        elevation += elevationVelocity * dt
        elevationVelocity *= damping
        elevation = elevation.coerceIn(-0.8f, 1.2f)

        // ── Compute camera position ─────────────────────────
        val finalAzimuth = azimuth + driftAzimuth
        val finalElevation = elevation + driftElevation

        val cosElev = cos(finalElevation)
        position[0] = cos(finalAzimuth) * cosElev * radius
        position[1] = sin(finalElevation) * radius
        position[2] = sin(finalAzimuth) * cosElev * radius

        // ── Build view matrix ────────────────────────────────
        Matrix.setLookAtM(
            viewMatrix, 0,
            position[0], position[1], position[2],  // eye
            targetX, targetY, targetZ,                // center
            0f, 1f, 0f                                // up
        )

        // ── Precompute VP matrix ─────────────────────────────
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    /**
     * Get the distance from camera to origin (for fog/LOD).
     */
    fun getDistanceToOrigin(): Float = radius

    /**
     * Set the look-at target point.
     */
    fun setTarget(x: Float, y: Float, z: Float) {
        targetX = x
        targetY = y
        targetZ = z
    }

    /**
     * Smoothly ease the camera to a new radius.
     */
    fun easeToRadius(target: Float) {
        targetRadius = target.coerceIn(1.5f, 10f)
    }

    /**
     * Reset camera to default state.
     */
    fun reset() {
        azimuth = 0f
        elevation = 0.3f
        radius = 4.0f
        targetRadius = 4.0f
        breathPhase = 0f
        driftSeed = 0f
        azimuthVelocity = 0f
        elevationVelocity = 0f
        targetX = 0f
        targetY = 0f
        targetZ = 0f
    }

    /**
     * Get the normal matrix (inverse-transpose of upper 3x3 of model*view).
     * Caller provides the model matrix. Result written into [out] (9 floats).
     */
    fun computeNormalMatrix(modelMatrix: FloatArray, out: FloatArray) {
        // modelView = view * model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Extract upper-left 3x3 and compute inverse-transpose
        // For uniform-scale models, the upper-left 3x3 itself works
        out[0] = tempMatrix[0]; out[1] = tempMatrix[1]; out[2] = tempMatrix[2]
        out[3] = tempMatrix[4]; out[4] = tempMatrix[5]; out[5] = tempMatrix[6]
        out[6] = tempMatrix[8]; out[7] = tempMatrix[9]; out[8] = tempMatrix[10]
    }
}

