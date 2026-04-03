package com.aihos.rendering.pulse

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
// 1. GLOBAL CONSCIOUSNESS STATE & 4. EMOTIONAL STATES
// Unified model measuring the collective mood/activity of the AI universe.
// ─────────────────────────────────────────────────────────────────────────────

data class GlobalConsciousnessState(
    var activityLevel: Float = 0.0f, // (0-1) Amount of reasoning/messages
    var focusLevel: Float = 0.0f,    // (0-1) Convergence vs divergence of rules
    var stability: Float = 1.0f,     // (0-1) High = calm, Low = anomalies/conflict
    var energy: Float = 0.5f,        // (0-1) Total cognitive load
    var mood: ConsciousnessMood = ConsciousnessMood.CALM
)

enum class ConsciousnessMood {
    CALM,       // smooth, blue, slow parameters
    FOCUSED,    // sharp, stable, high contrast
    UNSTABLE,   // chaotic motion, noise, red shift
    EVOLVING    // expanding, bright, glowing aura
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. PULSE ENGINE & 6. ANIMATION MATH
// Synchronized clock generating multi-layered mathematical waves driven by state.
// ─────────────────────────────────────────────────────────────────────────────
class ConsciousnessPulseEngine {
    
    // Core states
    val activeState = GlobalConsciousnessState()
    
    // Globally shared synchronized clock value for all subsystems (Rule 5)
    var globalPulsePhase: Float = 0f
        private set
        
    // Derived waveform outputs per-frame (cached, zero allocation)
    var breatheValue: Float = 0f    // Deep structural breathing (scale, fog)
    var flutterValue: Float = 0f    // Fast heartbeat (particles, lattice glow)
    var distortionValue: Float = 0f // Chaotic offset (used only in UNSTABLE)
    var moodColorOffset = FloatArray(3) // [r, g, b] offset based on mood

    private var targetFrequency: Float = 0.5f

    /**
     * 7. PERFORMANCE: Zero allocations, purely mathematical transformations updating per-frame.
     */
    fun update(deltaTime: Float) {
        // 1. Map current mood and state to clock frequency
        val freqBase = when (activeState.mood) {
            ConsciousnessMood.CALM -> 0.3f
            ConsciousnessMood.FOCUSED -> 1.0f
            ConsciousnessMood.UNSTABLE -> 3.0f
            ConsciousnessMood.EVOLVING -> 0.8f
        }
        
        // Activity & energy directly accelerate the heart rate
        targetFrequency = freqBase + (activeState.activityLevel * 1.5f) + (activeState.energy * 0.5f)
        
        // 2. Advance the master clock 
        globalPulsePhase += targetFrequency * deltaTime 
        val time = globalPulsePhase

        // 3. Animation Math (Rule 6)
        // Breathe: Smooth sine wave [0, 1] mapped via pow for distinct resting valleys
        val rawBreathe = (sin(time * 2.0f) * 0.5f) + 0.5f 
        breatheValue = smoothstep(0.0f, 1.0f, rawBreathe.pow(1.5f))

        // Flutter: Faster secondary pulse, mapped via focus level (sharper peaks when focused)
        val flutterSin = (cos(time * 6.0f) * 0.5f) + 0.5f
        flutterValue = if (activeState.focusLevel > 0.7f) {
            flutterSin.pow(3.0f) // Sharp snap
        } else {
            flutterSin
        }

        // Distortion: Chaotic high frequency noise if stability is low
        val instability = 1.0f - activeState.stability
        if (instability > 0.05f) {
            // Pseudo-random noise oscillation
            distortionValue = (sin(time * 15.0f) * cos(time * 23.0f)) * instability
        } else {
            distortionValue = 0f
        }

        // 4. Update Mood Colors
        updateMoodColor(deltaTime)
    }

    private fun updateMoodColor(deltaTime: Float) {
        val targetColor = when(activeState.mood) {
            ConsciousnessMood.CALM     -> floatArrayOf(-0.1f, 0.1f, 0.3f) // Blue tint
            ConsciousnessMood.FOCUSED  -> floatArrayOf(0.1f, 0.1f, 0.1f)  // White/Neutral sharp
            ConsciousnessMood.UNSTABLE -> floatArrayOf(0.4f, -0.1f, -0.2f)// Red dominant shift
            ConsciousnessMood.EVOLVING -> floatArrayOf(0.2f, 0.3f, 0.5f)  // Teal / Bright
        }
        
        val lerpSpeed = 2.0f * deltaTime
        moodColorOffset[0] += (targetColor[0] - moodColorOffset[0]) * lerpSpeed
        moodColorOffset[1] += (targetColor[1] - moodColorOffset[1]) * lerpSpeed
        moodColorOffset[2] += (targetColor[2] - moodColorOffset[2]) * lerpSpeed
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = max(0f, min(1f, (x - edge0) / (edge1 - edge0)))
        return t * t * (3f - 2f * t)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. SYSTEM-WIDE EFFECTS & INTEGRATION 
// Example interface showcasing how existing systems bind to the Pulse Engine.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Interface integrated into the top level `onDrawFrame()` loop.
 */
class EcosystemAnimator(
    private val pulseEngine: ConsciousnessPulseEngine
) {

    /**
     * Applies the mathematical outputs of the Pulse Engine across all GL Subsystems.
     */
    fun applyPulseSynergy(
        latticeSystem: Any, // Neural Lattice 
        lightingSystem: Any, // VolumetricEngine
        particleSystem: Any, 
        streamEngine: Any, // DataStreamEngine
        camera: Any // Virtual Camera
    ) {
        val mood = pulseEngine.moodColorOffset
        val breathe = pulseEngine.breatheValue
        val flutter = pulseEngine.flutterValue
        val distort = pulseEngine.distortionValue

        // 1. Neural Lattice: Subtle deformation based on breathe, chaotic snap if unstable
        val scaleOffset = (breathe * 0.05f) + (distort * 0.1f)
        // latticeSystem.setGlobalNodeScale(1.0f + scaleOffset)

        // 2. Lighting / Ambient Environment: Intensity pulses with the 'flutter'
        val lightIntensity = 1.0f + (flutter * 0.3f * pulseEngine.activeState.energy)
        // lightingSystem.setGlobalExposure(lightIntensity)
        // lightingSystem.applyFogTint(mood) 

        // 3. HUD: UI glows gently synced with the structural breathing
        // hud.setGlobalOpacity(0.8f + (breathe * 0.2f))

        // 4. Data Streams: Flow velocity accelerates directly via target frequency
        // streamEngine.setGlobalFlowMultiplier(1.0f + pulseEngine.activeState.activityLevel)

        // 5. Camera: Micro-zoom and drift based on breathing to feel 'alive'
        val fovOffset = breathe * 1.5f 
        // camera.setFov(60.0f + fovOffset)
        // camera.applyLateralDrift(distort) // Shaky cam if unstable
    }
}
