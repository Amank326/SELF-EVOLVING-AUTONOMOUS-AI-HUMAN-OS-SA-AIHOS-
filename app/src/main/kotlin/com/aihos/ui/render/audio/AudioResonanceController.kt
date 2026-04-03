package com.aihos.ui.render.audio
import com.aihos.ui.render.cognition.CognitiveVisualOutput
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
/**
 * AudioResonanceController - Blends audio energy with AI cognitive state
 * to produce unified visual drive parameters.
 *
 * Blending formula:
 *   finalValue = audioWeight * audioContribution + cognitiveWeight * cognitiveContribution
 *
 * Default: audio 0.6, cognitive 0.4 (audio-dominant when active).
 * When audio is inactive: cognitive contributes 100%.
 *
 * Zero allocation per frame. All output in pre-allocated AudioVisualOutput.
 */
class AudioResonanceController {
    val output = AudioVisualOutput()
    // Blending weights
    var audioWeight = 0.6f
    var cognitiveWeight = 0.4f
    // Damped spring state for smooth transitions
    private var dampedBass = 0f
    private var dampedMid = 0f
    private var dampedHigh = 0f
    private var dampedAmplitude = 0f
    private var dampedFlux = 0f
    // Burst system state
    private var burstEnergy = 0f
    private var burstDecay = 0.92f
    private var burstAccumulator = 0f
    // Peak hold for smooth envelope
    private var peakBass = 0f
    private var peakMid = 0f
    private var peakAmplitude = 0f
    private val PEAK_DECAY = 0.97f
    private val SPRING_FACTOR = 0.15f
    fun update(
        audio: AudioResonanceSnapshot,
        aiMetrics: AIMetricsSnapshot,
        cognitiveOutput: CognitiveVisualOutput,
        deltaTime: Float,
        elapsedTime: Float
    ) {
        val o = output
        val active = audio.isActive
        // Effective weights: full cognitive when audio not active
        val aw = if (active) audioWeight else 0f
        val cw = if (active) cognitiveWeight else 1f
        // -- Damped spring smoothing of audio values --
        dampedBass += (audio.bassEnergy - dampedBass) * SPRING_FACTOR
        dampedMid += (audio.midEnergy - dampedMid) * SPRING_FACTOR
        dampedHigh += (audio.highEnergy - dampedHigh) * SPRING_FACTOR
        dampedAmplitude += (audio.amplitude - dampedAmplitude) * SPRING_FACTOR
        dampedFlux += (audio.spectralFlux - dampedFlux) * SPRING_FACTOR * 0.5f
        // Peak hold (slow decay for envelope following)
        peakBass = max(peakBass * PEAK_DECAY, dampedBass)
        peakMid = max(peakMid * PEAK_DECAY, dampedMid)
        peakAmplitude = max(peakAmplitude * PEAK_DECAY, dampedAmplitude)
        // -- Burst system: accumulates high energy, triggers on onset --
        burstAccumulator += dampedHigh * deltaTime * 2f
        if (audio.onsetDetected && burstAccumulator > 0.05f) {
            burstEnergy = min(burstAccumulator * 3f, 1f)
            burstAccumulator = 0f
        }
        burstEnergy *= burstDecay
        // ===============================================================
        // Blend audio and cognitive into unified output
        // ===============================================================
        // Core pulsation: bass drives primary pulse
        o.pulsationBoost = aw * peakBass * 2.5f + cw * cognitiveOutput.pulsationAmplitude * 10f
        // Glow intensity: amplitude + cognitive glow
        o.glowBoost = aw * peakAmplitude * 1.8f + cw * cognitiveOutput.glowIntensity * 0.5f
        // Lattice deformation: mid frequencies drive lattice
        o.latticeDeformation = aw * dampedMid * 0.8f + cw * aiMetrics.cognitiveLoad * 0.3f
        // Particle emission rate boost
        o.particleEmissionBoost = aw * (dampedMid + dampedHigh * 0.5f) * 2f + cw * cognitiveOutput.particleEmissionRate * 0.3f
        // Particle burst: triggered by high-frequency onset
        o.particleBurstIntensity = burstEnergy
        // Camera micro-vibration from bass
        o.cameraVibration = aw * dampedBass * 0.004f + cw * cognitiveOutput.cameraJitter
        // Energy field distortion: spectral flux
        o.energyFieldBoost = aw * dampedFlux * 1.5f + cw * aiMetrics.evolutionRate * 0.4f
        // Nebula motion speed boost
        o.nebulaMotionBoost = aw * dampedBass * 0.3f + cw * aiMetrics.cognitiveLoad * 0.1f
        // HUD glow: amplitude + cognitive confidence
        o.hudGlowBoost = aw * dampedAmplitude * 1.2f + cw * aiMetrics.confidence * 0.3f
        // Starfield twinkle speed
        o.starfieldTwinkleBoost = aw * dampedHigh * 0.8f
        // Shader audio uniforms (direct pass-through for GPU)
        o.shaderBassEnergy = dampedBass
        o.shaderMidEnergy = dampedMid
        o.shaderHighEnergy = dampedHigh
        o.shaderAmplitude = dampedAmplitude
        // Active flag
        o.isAudioActive = active
    }
    fun reset() {
        dampedBass = 0f; dampedMid = 0f; dampedHigh = 0f
        dampedAmplitude = 0f; dampedFlux = 0f
        burstEnergy = 0f; burstAccumulator = 0f
        peakBass = 0f; peakMid = 0f; peakAmplitude = 0f
    }
}
/**
 * AudioVisualOutput - Pre-allocated mutable struct holding blended
 * audio-cognitive visual drive parameters. Updated in-place per frame.
 */
class AudioVisualOutput {
    // Blended visual modifiers
    var pulsationBoost: Float = 0f
    var glowBoost: Float = 0f
    var latticeDeformation: Float = 0f
    var particleEmissionBoost: Float = 0f
    var particleBurstIntensity: Float = 0f
    var cameraVibration: Float = 0f
    var energyFieldBoost: Float = 0f
    var nebulaMotionBoost: Float = 0f
    var hudGlowBoost: Float = 0f
    var starfieldTwinkleBoost: Float = 0f
    // Direct shader uniforms (normalized 0-1)
    var shaderBassEnergy: Float = 0f
    var shaderMidEnergy: Float = 0f
    var shaderHighEnergy: Float = 0f
    var shaderAmplitude: Float = 0f
    var isAudioActive: Boolean = false
}
