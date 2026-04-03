package com.aihos.ui.render.cognition

import com.aihos.ui.render.core.AIMetricsSnapshot

/**
 * CognitiveState — Unified immutable snapshot of the AI's cognitive condition.
 *
 * This is NOT the raw AI metrics. It is a derived, physics-smoothed,
 * second-order model that represents the *felt* cognitive state.
 *
 * Every value is normalized [0.0, 1.0].
 *
 * ┌─────────────────────────┬─────────────────────────────────────────────────┐
 * │  Metric                 │  Visual Influence                              │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  cognitiveLoad          │  Core pulsation frequency, glow intensity,     │
 * │                         │  vertex displacement, warm color shift          │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  confidence             │  Camera stability, light brightness, surface   │
 * │                         │  smoothness, particle uniformity               │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  evolutionRate          │  Geometric morph amplitude, noise distortion,  │
 * │                         │  multi-axis rotation, color accent intensity   │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  reflectionDepth        │  Time dilation (slow-mo), camera zoom-in,     │
 * │                         │  reduced particle speed, deeper fog            │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  memoryActivity         │  Particle emission rate, neural field density, │
 * │                         │  secondary glow pulses                         │
 * ├─────────────────────────┼─────────────────────────────────────────────────┤
 * │  decisionComplexity     │  Multi-axis rotation, camera jitter, radial   │
 * │                         │  pulse waves, chromatic aberration             │
 * └─────────────────────────┴─────────────────────────────────────────────────┘
 */
data class CognitiveState(
    val cognitiveLoad: Float = 0.5f,
    val confidence: Float = 0.5f,
    val evolutionRate: Float = 0.0f,
    val reflectionDepth: Float = 0.0f,
    val memoryActivity: Float = 0.3f,
    val decisionComplexity: Float = 0.3f
) {
    companion object {
        /**
         * Derive CognitiveState from raw AIMetricsSnapshot.
         * Maps the 8-field AI metrics into the 6-field cognitive model.
         */
        fun fromMetrics(m: AIMetricsSnapshot): CognitiveState = CognitiveState(
            cognitiveLoad = m.cognitiveLoad,
            confidence = m.confidence,
            evolutionRate = m.evolutionRate,
            reflectionDepth = m.selfAwareness,
            memoryActivity = m.memoryLoad,
            decisionComplexity = (m.cognitiveLoad * 0.4f + m.autonomyLevel * 0.3f +
                    m.evolutionRate * 0.3f).coerceIn(0f, 1f)
        )
    }
}

/**
 * CognitiveVisualOutput — The complete set of derived visual parameters
 * produced by the CognitiveMotionEngine each frame.
 *
 * These are consumed by camera, lighting, geometry, particles, and shaders.
 * Mutable struct updated in-place. Zero allocation per frame.
 */
class CognitiveVisualOutput {
    // ── Time ─────────────────────────────────────────────────────
    /** Cognitive time scale: <1 = slow-motion, >1 = accelerated. */
    var timeScale: Float = 1.0f
    /** Accumulated cognitive time (time * timeScale integrated). */
    var cognitiveTime: Float = 0f

    // ── Core Pulsation ───────────────────────────────────────────
    /** Hertz. Base oscillation frequency of the core sphere. */
    var pulsationFrequency: Float = 0.8f
    /** [0,1] How much the surface breathes. */
    var pulsationAmplitude: Float = 0.02f

    // ── Glow & Emissive ──────────────────────────────────────────
    var glowIntensity: Float = 0.5f
    var fresnelAmplification: Float = 1.0f
    var emissivePulseSpeed: Float = 1.5f

    // ── Geometry Deformation ─────────────────────────────────────
    /** Morph amplitude for evolution-based shape changes. */
    var morphAmplitude: Float = 0.0f
    /** Noise-based vertex displacement. */
    var noiseDistortion: Float = 0.0f
    /** Noise spatial frequency. */
    var noiseFrequency: Float = 3.0f

    // ── Rotation ─────────────────────────────────────────────────
    var rotationSpeedY: Float = 0.3f
    var rotationSpeedX: Float = 0.0f
    var rotationSpeedZ: Float = 0.0f

    // ── Color ────────────────────────────────────────────────────
    /** 0 = cool/cyan, 1 = warm/magenta. */
    var colorWarmth: Float = 0.0f
    /** Gold accent from evolution. */
    var accentStrength: Float = 0.0f
    var saturationBoost: Float = 1.0f

    // ── Camera ───────────────────────────────────────────────────
    var cameraOrbitSpeed: Float = 0.1f
    var cameraBreathDepth: Float = 0.15f
    var cameraDamping: Float = 0.95f
    var cameraZoomTarget: Float = 4.0f
    /** Micro-jitter amplitude for uncertainty. */
    var cameraJitter: Float = 0.0f

    // ── Lighting ─────────────────────────────────────────────────
    var keyLightBrightness: Float = 1.0f
    var rimLightIntensity: Float = 0.5f
    var ambientLevel: Float = 0.3f
    var orbitLightSpeed: Float = 0.5f

    // ── Particles ────────────────────────────────────────────────
    /** Emission multiplier [0.2, 3.0]. */
    var particleEmissionRate: Float = 1.0f
    /** Particle velocity multiplier. */
    var particleVelocity: Float = 1.0f
    /** Particle brightness. */
    var particleBrightness: Float = 1.0f
    var particleSpeed: Float = 1.0f

    // ── Post-Processing ──────────────────────────────────────────
    var bloomStrength: Float = 0.4f
    var bloomThreshold: Float = 0.7f
    var vignetteStrength: Float = 0.35f
    var exposureLevel: Float = 1.2f
    /** Radial pulse wave intensity. */
    var radialPulseIntensity: Float = 0.0f
    /** Cognitive heat map intensity. */
    var heatMapIntensity: Float = 0.0f
    var chromaticAberration: Float = 0.002f

    // ── Fog ──────────────────────────────────────────────────────
    var fogDensity: Float = 0.15f
}

