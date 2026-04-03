package com.aihos.ui.render.cognition

import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * CognitiveMotionEngine — Master orchestrator for AI-to-Visual motion intelligence.
 *
 * Pipeline:
 *
 *   AIMetricsSnapshot
 *        │
 *        ▼
 *   CognitiveState.fromMetrics()          ← Derive 6-metric cognitive model
 *        │
 *        ▼
 *   DampedSpringBank.pushTargets()        ← Set spring targets
 *   DampedSpringBank.update(wallDt)       ← Physics simulation (wall-clock)
 *        │
 *        ▼
 *   SmoothedCognitiveState                ← Physics-smoothed values + derivatives
 *        │
 *        ├──→ CognitiveTimeDilation.update()  ← Compute cognitive time scale
 *        │
 *        ▼
 *   mapToVisualOutput()                   ← Map smoothed state → visual parameters
 *        │
 *        ▼
 *   CognitiveVisualOutput                 ← Consumed by camera, lighting, passes
 *
 * Mathematical foundation for each mapping:
 *
 *   cognitiveLoad → pulsationFreq:
 *     f = 0.4 + CL * 2.6          (range [0.4, 3.0] Hz)
 *     amplitude = 0.01 + CL * 0.06
 *
 *   confidence → camera damping:
 *     ζ_camera = 0.88 + conf * 0.11   (range [0.88, 0.99])
 *     jitter = (1 - conf)² * 0.008     (quadratic decay)
 *
 *   evolutionRate → morph amplitude:
 *     morph = evo * 0.2 * (1 + sin(t * 0.7) * 0.3)   (time-modulated)
 *     noise = evo * 0.15 + CL * 0.05
 *
 *   reflectionDepth → time dilation:
 *     timeScale ≈ 1 - ref * 0.7   (handled by CognitiveTimeDilation)
 *     fogDensity = 0.1 + ref * 0.15
 *
 *   memoryActivity → particle emission:
 *     emission = 0.2 + mem * 2.8   (range [0.2, 3.0])
 *     velocity = 0.5 + (1 - mem) * 1.0  (memory heavy = slower particles)
 *
 *   decisionComplexity → multi-axis rotation:
 *     rotX = dec * 0.15
 *     rotZ = dec * 0.1
 *     pulseWave = dec * 0.4
 *
 * Zero allocation per frame. All output written to pre-allocated struct.
 */
class CognitiveMotionEngine {

    // ── Subsystems ───────────────────────────────────────────────
    private val springBank = DampedSpringBank()
    val timeDilation = CognitiveTimeDilation()

    // ── Output (pre-allocated, updated in-place) ─────────────────
    val output = CognitiveVisualOutput()

    // ── Internal state ───────────────────────────────────────────
    private var initialized = false
    private var lastSmoothed: SmoothedCognitiveState? = null

    // ── Performance scaling ──────────────────────────────────────
    private var distortionEnabled = true
    private var particleFullRate = true

    /**
     * Update the cognitive motion engine.
     *
     * @param aiMetrics raw AI metrics from the bridge (immutable)
     * @param wallDt wall-clock delta time in seconds (clamped by FrameTimer)
     * @param wallTime wall-clock elapsed time in seconds
     * @param quality current dynamic quality level
     */
    fun update(
        aiMetrics: AIMetricsSnapshot,
        wallDt: Float,
        wallTime: Float,
        quality: QualityLevel
    ) {
        // ── 1. Derive cognitive state from raw metrics ───────
        val cognitive = CognitiveState.fromMetrics(aiMetrics)

        // ── 2. Push targets into spring bank ─────────────────
        springBank.pushTargets(cognitive)

        // First frame: snap to avoid initial spring animation
        if (!initialized) {
            springBank.snapAll()
            initialized = true
        }

        // ── 3. Physics simulation (always wall-clock dt) ─────
        springBank.update(wallDt)
        val smoothed = springBank.snapshot()
        lastSmoothed = smoothed

        // ── 4. Time dilation ─────────────────────────────────
        timeDilation.update(smoothed, wallDt, wallTime)

        // ── 5. Performance gating ────────────────────────────
        distortionEnabled = quality != QualityLevel.LOW
        particleFullRate = quality == QualityLevel.HIGH

        // ── 6. Map smoothed state to visual output ───────────
        mapToVisualOutput(smoothed, wallTime)
    }

    // ═══════════════════════════════════════════════════════════════
    // Visual Mapping (mathematical formulas documented inline)
    // ═══════════════════════════════════════════════════════════════

    @Suppress("UNUSED_PARAMETER")
    private fun mapToVisualOutput(s: SmoothedCognitiveState, wallTime: Float) {
        val o = output
        val ct = timeDilation.cognitiveTime

        // ── Time ─────────────────────────────────────────────
        o.timeScale = timeDilation.timeScale
        o.cognitiveTime = ct

        // ── Core Pulsation ───────────────────────────────────
        // f = 0.4 + CL * 2.6  →  [0.4 Hz at rest, 3.0 Hz at max load]
        o.pulsationFrequency = 0.4f + s.cognitiveLoad * 2.6f
        // amplitude = 0.01 + CL * 0.06  →  [0.01, 0.07] world units
        o.pulsationAmplitude = 0.01f + s.cognitiveLoad * 0.06f

        // ── Glow & Emissive ──────────────────────────────────
        // Glow rises with load, with a sine overlay for organic feel
        // I(t) = 0.3 + CL * 1.2 + sin(ct * pulsFreq * 2π) * 0.1
        o.glowIntensity = 0.3f + s.cognitiveLoad * 1.2f +
                sin(ct * o.pulsationFrequency * TWO_PI) * 0.1f
        // Fresnel amplification: stronger edge glow at high load
        // F = 1.0 + CL * 2.0
        o.fresnelAmplification = 1.0f + s.cognitiveLoad * 2.0f
        o.emissivePulseSpeed = 1.0f + s.cognitiveLoad * 2.0f

        // ── Geometry Deformation ─────────────────────────────
        if (distortionEnabled) {
            // morph = evo * 0.2 * (1 + sin(ct * 0.7) * 0.3)
            o.morphAmplitude = s.evolutionRate * 0.2f *
                    (1f + sin(ct * 0.7f) * 0.3f)
            // noise = evo * 0.15 + CL * 0.05
            o.noiseDistortion = s.evolutionRate * 0.15f + s.cognitiveLoad * 0.05f
        } else {
            o.morphAmplitude = 0f
            o.noiseDistortion = 0f
        }
        // Noise spatial frequency increases with memory activity
        o.noiseFrequency = 2.0f + s.memoryActivity * 6.0f

        // ── Rotation ─────────────────────────────────────────
        // Y-axis: base + autonomy-driven (from cognitiveLoad as proxy)
        o.rotationSpeedY = 0.2f + s.cognitiveLoad * 0.3f +
                sine(ct, 0.1f) * 0.05f
        // Multi-axis rotation from decision complexity:
        // rotX = complexity * 0.15
        // rotZ = complexity * 0.1
        if (distortionEnabled) {
            o.rotationSpeedX = s.decisionComplexity * 0.15f
            o.rotationSpeedZ = s.decisionComplexity * 0.1f
        } else {
            o.rotationSpeedX = 0f
            o.rotationSpeedZ = 0f
        }

        // ── Color ────────────────────────────────────────────
        // Warmth maps from memory load: smoothstep(0.2, 0.8, memoryActivity)
        o.colorWarmth = smoothstep(0.2f, 0.8f, s.memoryActivity)
        // Accent from evolution + time modulation
        o.accentStrength = s.evolutionRate * 0.6f +
                sine(ct, 0.3f) * s.evolutionRate * 0.2f
        // Saturation correlates with system health (more desaturated when unhealthy)
        o.saturationBoost = 0.85f + s.confidence * 0.3f

        // ── Camera (second-order already handled by CameraController) ──
        o.cameraOrbitSpeed = 0.05f + s.cognitiveLoad * 0.15f
        o.cameraBreathDepth = 0.05f + s.cognitiveLoad * 0.2f
        // Camera damping: ζ = 0.88 + conf * 0.11  → [0.88, 0.99]
        o.cameraDamping = 0.88f + s.confidence * 0.11f
        // Reflection zoom: 4.0 - reflectionDepth * 1.5
        o.cameraZoomTarget = 4.0f - s.reflectionDepth * 1.5f
        // Jitter: (1-conf)² * 0.008  (quadratic decay = fast stabilization)
        val uncertainty = (1f - s.confidence)
        o.cameraJitter = uncertainty * uncertainty * 0.008f

        // ── Lighting ─────────────────────────────────────────
        o.keyLightBrightness = 0.6f + s.confidence * 0.8f
        o.rimLightIntensity = 0.3f + s.cognitiveLoad * 0.7f
        o.ambientLevel = 0.15f + s.confidence * 0.2f
        o.orbitLightSpeed = 0.3f + s.evolutionRate * 0.8f

        // ── Particles ────────────────────────────────────────
        // Emission = 0.2 + mem * 2.8  → [0.2, 3.0]
        val rawEmission = 0.2f + s.memoryActivity * 2.8f
        o.particleEmissionRate = if (particleFullRate) rawEmission else rawEmission * 0.6f
        // Velocity = 0.5 + (1-mem) * 1.0  (high memory = slower particles → denser field)
        o.particleVelocity = 0.5f + (1f - s.memoryActivity) * 1.0f
        // Brightness from confidence
        o.particleBrightness = 0.4f + s.confidence * 0.8f
        o.particleSpeed = 0.5f + s.cognitiveLoad * 0.8f + s.confidence * 0.3f

        // ── Post-Processing ──────────────────────────────────
        // Bloom: stronger with cognitive load
        o.bloomStrength = 0.3f + s.cognitiveLoad * 0.3f +
                sine(ct, 0.2f) * 0.05f
        // Threshold inversely proportional to load (more bloom at high load)
        o.bloomThreshold = 0.8f - s.cognitiveLoad * 0.2f
        // Vignette: stronger when system is stressed
        o.vignetteStrength = 0.25f + (1f - s.confidence) * 0.3f
        o.exposureLevel = 1.0f + s.confidence * 0.3f

        // Radial pulse: driven by decision complexity
        // pulse = complexity * 0.4 * |sin(ct * complexity * 3)|
        if (distortionEnabled) {
            o.radialPulseIntensity = s.decisionComplexity * 0.4f *
                    abs(sin(ct * s.decisionComplexity * 3f * TWO_PI))
            // Heat map: appears during high cognitive load + evolution
            o.heatMapIntensity = (s.cognitiveLoad * 0.5f + s.evolutionRate * 0.5f)
                .coerceIn(0f, 1f) * 0.3f
            // Chromatic aberration: turbulence → stronger
            o.chromaticAberration = 0.001f + if (s.isTurbulent) 0.004f else 0f
        } else {
            o.radialPulseIntensity = 0f
            o.heatMapIntensity = 0f
            o.chromaticAberration = 0.001f
        }

        // ── Fog ──────────────────────────────────────────────
        // Deeper fog during reflection
        o.fogDensity = 0.1f + s.reflectionDepth * 0.15f
    }

    // ═══════════════════════════════════════════════════════════════
    // Math utilities (zero-allocation)
    // ═══════════════════════════════════════════════════════════════

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun sine(time: Float, frequency: Float, amplitude: Float = 1f): Float =
        sin(time * frequency * TWO_PI) * amplitude

    fun reset() {
        springBank.snapAll()
        timeDilation.reset()
        initialized = false
    }

    companion object {
        private const val TWO_PI = (2.0 * PI).toFloat()
    }
}

