package com.aihos.ui.gl

import kotlin.math.PI
import kotlin.math.sin

/**
 * ProceduralAnimationEngine — Maps AI system metrics to visual parameters.
 *
 * Input:  AISystemState (via VisualInputState snapshot)
 * Output: VisualParams consumed by CinematicRenderer every frame
 *
 * Mapping:
 *   CognitiveLoad    → glow intensity, color warmth, breathing depth
 *   Confidence       → smoothness, brightness, point size
 *   EvolutionRate    → noise distortion, orbit speed, color accent
 *   ReflectionDepth  → camera slow-motion zoom, reduced animation speed
 *   AutonomyLevel    → rotation speed, rim light, particle emission
 *   SystemHealth     → ambient level, particle visibility, overall clarity
 *   MemoryLoad       → color shift (cyan → magenta), noise frequency
 *
 * All computations use:
 *   - Sine waves (periodic oscillation)
 *   - Smoothstep (smooth transitions)
 *   - Lightweight Perlin-style noise (deterministic variation)
 *
 * Zero allocations. VisualParams is a mutable struct updated in-place.
 */
class ProceduralAnimationEngine {

    /**
     * Visual parameters consumed by the renderer. Updated in-place each frame.
     */
    class VisualParams {
        // ── Glow & Emissive ──────────────────────────────────
        var glowIntensity = 0.5f
        var emissivePulseSpeed = 1.0f
        var emissiveStrength = 0.3f

        // ── Color ────────────────────────────────────────────
        var colorWarmth = 0.0f       // 0=cool(cyan), 1=warm(magenta)
        var accentStrength = 0.0f    // gold/evolution accent
        var saturationBoost = 1.0f

        // ── Distortion ───────────────────────────────────────
        var noiseDistortion = 0.0f
        var noiseFrequency = 3.0f
        var vertexDisplacement = 0.02f

        // ── Motion ───────────────────────────────────────────
        var rotationSpeed = 0.3f     // model rotation rad/sec
        var breathingDepth = 0.1f    // vertex breathing amplitude
        var particleSpeed = 1.0f

        // ── Camera influence ─────────────────────────────────
        var cameraOrbitSpeed = 0.1f
        var cameraBreathDepth = 0.15f
        var cameraDamping = 0.95f
        var cameraZoomTarget = 4.0f

        // ── Lighting influence ───────────────────────────────
        var keyLightBrightness = 1.0f
        var rimLightIntensity = 0.5f
        var ambientLevel = 0.3f
        var orbitLightSpeed = 0.5f

        // ── Post-processing influence ────────────────────────
        var bloomStrength = 0.4f
        var bloomThreshold = 0.7f
        var vignetteStrength = 0.35f
        var exposureLevel = 1.2f
    }

    /** Snapshot of AI metrics to avoid reading volatile fields multiple times. */
    data class VisualInputState(
        val cognitiveLoad: Float = 0.5f,
        val confidence: Float = 0.5f,
        val evolutionRate: Float = 0.0f,
        val selfAwareness: Float = 0.5f,
        val autonomyLevel: Float = 0.5f,
        val systemHealth: Float = 0.8f,
        val memoryLoad: Float = 0.3f,
        val animationIntensity: Float = 0.5f
    )

    // Pre-allocated output — no GC
    val params = VisualParams()

    // Internal smoothing state
    private var smoothedCognitiveLoad = 0.5f
    private var smoothedConfidence = 0.5f
    private var smoothedEvolutionRate = 0.0f
    private var smoothedAutonomy = 0.5f
    private var smoothedHealth = 0.8f
    private var smoothedMemoryLoad = 0.3f

    /**
     * Update visual parameters based on AI state. Call once per frame.
     *
     * @param input current AI metrics snapshot
     * @param time elapsed time in seconds
     * @param dt delta time in seconds
     */
    fun update(input: VisualInputState, time: Float, dt: Float) {
        // ── Smooth all inputs (prevents visual jitter) ───────
        val smoothFactor = (1.0 - kotlin.math.exp((-dt * 3f).toDouble())).toFloat()  // ~3 Hz cutoff
        smoothedCognitiveLoad = lerp(smoothedCognitiveLoad, input.cognitiveLoad, smoothFactor)
        smoothedConfidence = lerp(smoothedConfidence, input.confidence, smoothFactor)
        smoothedEvolutionRate = lerp(smoothedEvolutionRate, input.evolutionRate, smoothFactor)
        smoothedAutonomy = lerp(smoothedAutonomy, input.autonomyLevel, smoothFactor)
        smoothedHealth = lerp(smoothedHealth, input.systemHealth, smoothFactor)
        smoothedMemoryLoad = lerp(smoothedMemoryLoad, input.memoryLoad, smoothFactor)

        val cl = smoothedCognitiveLoad
        val conf = smoothedConfidence
        val evo = smoothedEvolutionRate
        val auto = smoothedAutonomy
        val health = smoothedHealth
        val mem = smoothedMemoryLoad

        // ── Glow & Emissive ──────────────────────────────────
        params.glowIntensity = 0.3f + cl * 1.2f + sine(time, 0.5f) * 0.1f
        params.emissivePulseSpeed = 1.0f + cl * 2.0f
        params.emissiveStrength = 0.2f + cl * 0.5f + conf * 0.2f

        // ── Color ────────────────────────────────────────────
        params.colorWarmth = smoothstep(0.2f, 0.8f, mem)
        params.accentStrength = evo * 0.6f + sine(time, 0.3f) * evo * 0.2f
        params.saturationBoost = 0.9f + health * 0.3f

        // ── Distortion ───────────────────────────────────────
        params.noiseDistortion = evo * 0.15f + cl * 0.05f
        params.noiseFrequency = 2.0f + cl * 4.0f
        params.vertexDisplacement = 0.01f + cl * 0.04f

        // ── Motion ───────────────────────────────────────────
        params.rotationSpeed = 0.2f + auto * 0.4f + sine(time, 0.1f) * 0.05f
        params.breathingDepth = 0.05f + cl * 0.2f
        params.particleSpeed = 0.5f + auto * 0.8f + conf * 0.3f

        // ── Camera influence ─────────────────────────────────
        params.cameraOrbitSpeed = 0.05f + auto * 0.15f
        params.cameraBreathDepth = 0.05f + cl * 0.2f
        params.cameraDamping = 0.92f + conf * 0.07f  // higher confidence = smoother
        params.cameraZoomTarget = 4.0f - input.selfAwareness * 1.5f  // reflection = zoom in

        // ── Lighting influence ───────────────────────────────
        params.keyLightBrightness = 0.6f + conf * 0.8f
        params.rimLightIntensity = 0.3f + auto * 0.7f
        params.ambientLevel = 0.15f + health * 0.25f
        params.orbitLightSpeed = 0.3f + evo * 0.8f

        // ── Post-processing influence ────────────────────────
        params.bloomStrength = 0.3f + cl * 0.3f + sine(time, 0.2f) * 0.05f
        params.bloomThreshold = 0.8f - cl * 0.2f  // lower threshold = more bloom at high load
        params.vignetteStrength = 0.25f + (1f - health) * 0.3f  // more vignette when unhealthy
        params.exposureLevel = 1.0f + conf * 0.3f
    }

    /**
     * Reset to defaults.
     */
    fun reset() {
        smoothedCognitiveLoad = 0.5f
        smoothedConfidence = 0.5f
        smoothedEvolutionRate = 0.0f
        smoothedAutonomy = 0.5f
        smoothedHealth = 0.8f
        smoothedMemoryLoad = 0.3f
    }

    // ═══════════════════════════════════════════════════════════════
    // Math utilities (inlined, zero-allocation)
    // ═══════════════════════════════════════════════════════════════

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Sine wave: returns [-amplitude, +amplitude] */
    private fun sine(time: Float, frequency: Float, amplitude: Float = 1f): Float {
        return sin(time * frequency * 2f * PI.toFloat()) * amplitude
    }

    /**
     * Lightweight deterministic noise (no hash table, no allocation).
     * Good for subtle visual variation, NOT for cryptography.
     */
    @Suppress("unused")
    private fun cheapNoise(x: Float, y: Float): Float {
        val ix = x.toInt()
        val iy = y.toInt()
        val fx = x - ix
        val fy = y - iy
        val a = pseudoRandom(ix, iy)
        val b = pseudoRandom(ix + 1, iy)
        val c = pseudoRandom(ix, iy + 1)
        val d = pseudoRandom(ix + 1, iy + 1)
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        return lerp(lerp(a, b, sx), lerp(c, d, sx), sy)
    }

    private fun pseudoRandom(x: Int, y: Int): Float {
        var n = x + y * 57
        n = (n shl 13) xor n
        return (1f - ((n * (n * n * 15731 + 789221) + 1376312589) and 0x7fffffff).toFloat() / 1073741824f)
    }
}

