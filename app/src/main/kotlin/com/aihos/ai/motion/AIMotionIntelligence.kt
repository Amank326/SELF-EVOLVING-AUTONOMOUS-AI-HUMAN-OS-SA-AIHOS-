package com.aihos.ai.motion

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * AI Motion Intelligence: Maps AI internal states to 3D animation parameters
 * This is the bridge between cognitive processes and visual representation
 * 
 * Philosophy:
 * - Animations are NOT predetermined sequences
 * - Animations ARE emergent from real AI state
 * - Every visual change reflects actual cognitive activity
 * - The 3D system becomes a window into AI consciousness
 */

/**
 * Primary AI cognitive states (mutually exclusive)
 */
@Serializable
enum class AICognitiveState {
    IDLE,           // Waiting for input, minimal processing
    THINKING,       // Actively reasoning about a decision
    DELIBERATING,   // Weighing options carefully, high cognitive load
    REFLECTING,     // Looking backward at past decisions
    EVOLVING,       // Updating rules and learning from experience
    UNCERTAIN,      // Low confidence, exploring multiple paths
    EXECUTING,      // Putting a decision into action
    ERROR,          // Encountered an issue or contradiction
}

/**
 * Detailed AI operational mode (can combine with state)
 */
@Serializable
enum class AIOperationalMode {
    CONSERVATIVE,   // Risk-averse, careful decisions
    BALANCED,       // Normal operation, balanced approach
    EXPLORATORY,    // Testing new strategies, higher risk tolerance
    ADAPTIVE,       // Learning from recent feedback
    STRATEGIC,      // Long-term planning mode
}

/**
 * Confidence level affecting visual intensity
 */
@Serializable
data class ConfidenceMetrics(
    val decisionConfidence: Float = 0.5f,    // 0.0 to 1.0: How sure about decision
    val predictionConfidence: Float = 0.5f,  // 0.0 to 1.0: How sure about prediction
    val knowledgeConfidence: Float = 0.5f,   // 0.0 to 1.0: How well understood domain
    val averageConfidence: Float = 0.5f      // Overall confidence level
)

/**
 * Real-time metrics about AI processing
 */
@Serializable
data class AIProcessingMetrics(
    val cognitiveLoad: Float = 0f,           // 0.0 to 1.0: Current processing intensity
    val decisionComplexity: Float = 0f,      // 0.0 to 1.0: How complex the decision
    val uncertaintyLevel: Float = 0f,        // 0.0 to 1.0: How uncertain we are
    val learningRate: Float = 0f,            // 0.0 to 1.0: Current rate of adaptation
    val successMetrics: Float = 0f,          // 0.0 to 1.0: Recent success rate
    val memoryLoad: Float = 0f,              // 0.0 to 1.0: How much memory is active
    val adaptationIntensity: Float = 0f      // 0.0 to 1.0: How much rules are changing
)

/**
 * Complete AI state snapshot sent to 3D system
 * This contains everything the 3D system needs to drive animations
 */
@Serializable
data class AIMotionState(
    val timestamp: Long = System.currentTimeMillis(),
    
    // Core state
    val primaryState: AICognitiveState = AICognitiveState.IDLE,
    val operationalMode: AIOperationalMode = AIOperationalMode.BALANCED,
    
    // Metrics that drive animations
    val confidence: ConfidenceMetrics = ConfidenceMetrics(),
    val processing: AIProcessingMetrics = AIProcessingMetrics(),
    
    // Animation parameters (derived from above)
    val breathingRate: Float = 1.0f,         // 0.1 to 3.0: Hz (cycles per second)
    val rotationSpeed: Float = 0.3f,         // 0.0 to 2.0: multiplier
    val colorTheme: AIColorTheme = AIColorTheme.CYAN,
    val glowIntensity: Float = 1.0f,         // 0.0 to 2.0: glow brightness
    val particleEmissionRate: Float = 1.0f,  // 0.0 to 2.0: particle count
    val morphingIntensity: Float = 0.0f,     // 0.0 to 1.0: geometry deformation
    
    // State transitions
    val isStateTransitioning: Boolean = false,
    val transitionDuration: Float = 0.5f,    // seconds
    val previousState: AICognitiveState? = null,
)

/**
 * Color themes representing AI personality/state
 */
@Serializable
enum class AIColorTheme {
    CYAN,           // Calm, analytical thinking
    PURPLE,         // Balanced, reflective
    RED,            // High intensity, urgent processing
    BLUE,           // Serene, stable state
    GREEN,          // Learning, growth, adaptation
    AMBER,          // Warning, uncertainty, exploration
}

/**
 * AI Motion Controller: Computes animation parameters from AI state
 * This is where cognitive state translates to visual behavior
 */
class AIMotionController {
    
    /**
     * Compute full motion state from AI metrics
     * This is the core logic that drives all 3D animations
     */
    fun computeMotionState(
        cognitiveState: AICognitiveState,
        operationalMode: AIOperationalMode,
        confidence: ConfidenceMetrics,
        processing: AIProcessingMetrics,
        previousState: AIMotionState? = null
    ): AIMotionState {
        
        // Compute animation parameters based on state
        val breathingRate = computeBreathingRate(cognitiveState, processing.cognitiveLoad)
        val rotationSpeed = computeRotationSpeed(cognitiveState, processing.decisionComplexity)
        val colorTheme = computeColorTheme(cognitiveState, operationalMode, processing.uncertaintyLevel)
        val glowIntensity = computeGlowIntensity(confidence.averageConfidence, cognitiveState)
        val particleEmissionRate = computeParticleRate(cognitiveState, processing.memoryLoad)
        val morphingIntensity = computeMorphingIntensity(cognitiveState, processing.adaptationIntensity)
        
        // Detect state transition
        val isTransitioning = previousState != null && previousState.primaryState != cognitiveState
        val transitionDuration = computeTransitionDuration(previousState?.primaryState, cognitiveState)
        
        return AIMotionState(
            timestamp = System.currentTimeMillis(),
            primaryState = cognitiveState,
            operationalMode = operationalMode,
            confidence = confidence,
            processing = processing,
            breathingRate = breathingRate,
            rotationSpeed = rotationSpeed,
            colorTheme = colorTheme,
            glowIntensity = glowIntensity,
            particleEmissionRate = particleEmissionRate,
            morphingIntensity = morphingIntensity,
            isStateTransitioning = isTransitioning,
            transitionDuration = transitionDuration,
            previousState = previousState?.primaryState
        )
    }
    
    /**
     * Breathing rate: slow when idle, fast when thinking intensely
     * Typical range: 0.5 Hz (calm) to 2.5 Hz (intense)
     */
    private fun computeBreathingRate(state: AICognitiveState, cognitiveLoad: Float): Float {
        val baseRate = when (state) {
            AICognitiveState.IDLE -> 0.5f         // Very slow breathing
            AICognitiveState.THINKING -> 1.0f      // Normal breathing
            AICognitiveState.DELIBERATING -> 1.8f  // Fast breathing
            AICognitiveState.REFLECTING -> 0.7f    // Slow, introspective
            AICognitiveState.EVOLVING -> 1.5f      // Growth breathing
            AICognitiveState.UNCERTAIN -> 1.3f     // Tentative breathing
            AICognitiveState.EXECUTING -> 1.2f     // Focused breathing
            AICognitiveState.ERROR -> 2.5f         // Agitated breathing
        }
        
        // Modulate by cognitive load (adds ±0.5 variation)
        return baseRate + (cognitiveLoad * 0.5f)
    }
    
    /**
     * Rotation speed: indicates processing velocity
     * Idle = static, thinking = slow rotation, deliberating = fast rotation
     */
    private fun computeRotationSpeed(state: AICognitiveState, complexity: Float): Float {
        val baseSpeed = when (state) {
            AICognitiveState.IDLE -> 0.1f          // Nearly still
            AICognitiveState.THINKING -> 0.4f      // Gentle rotation
            AICognitiveState.DELIBERATING -> 1.0f  // Vigorous rotation
            AICognitiveState.REFLECTING -> 0.2f    // Introspective stillness
            AICognitiveState.EVOLVING -> 0.7f      // Growth rotation
            AICognitiveState.UNCERTAIN -> 0.6f     // Searching rotation
            AICognitiveState.EXECUTING -> 0.5f     // Purposeful rotation
            AICognitiveState.ERROR -> 1.5f         // Erratic rotation
        }
        
        // Modulate by complexity (complexity adds up to 0.8x variation)
        return baseSpeed * (1.0f + complexity * 0.8f)
    }
    
    /**
     * Color theme: reflects emotional/operational tone
     * High uncertainty = amber/red, confident = cyan/blue, learning = green
     */
    private fun computeColorTheme(
        state: AICognitiveState,
        mode: AIOperationalMode,
        uncertainty: Float
    ): AIColorTheme {
        // If very uncertain, shift to warning colors
        if (uncertainty > 0.7f) {
            return when (state) {
                AICognitiveState.ERROR -> AIColorTheme.RED
                AICognitiveState.UNCERTAIN -> AIColorTheme.AMBER
                AICognitiveState.EVOLVING -> AIColorTheme.GREEN
                else -> AIColorTheme.AMBER
            }
        }
        
        // Otherwise, choose based on state and mode
        return when (state) {
            AICognitiveState.IDLE -> AIColorTheme.CYAN
            AICognitiveState.THINKING -> AIColorTheme.CYAN
            AICognitiveState.DELIBERATING -> AIColorTheme.PURPLE
            AICognitiveState.REFLECTING -> AIColorTheme.BLUE
            AICognitiveState.EVOLVING -> AIColorTheme.GREEN
            AICognitiveState.UNCERTAIN -> AIColorTheme.AMBER
            AICognitiveState.EXECUTING -> AIColorTheme.CYAN
            AICognitiveState.ERROR -> AIColorTheme.RED
        }
    }
    
    /**
     * Glow intensity: confidence visualization
     * Low confidence = dim glow, high confidence = bright glow
     */
    private fun computeGlowIntensity(confidence: Float, state: AICognitiveState): Float {
        val baseIntensity = when (state) {
            AICognitiveState.IDLE -> 0.4f
            AICognitiveState.THINKING -> 0.8f
            AICognitiveState.DELIBERATING -> 1.0f
            AICognitiveState.REFLECTING -> 0.6f
            AICognitiveState.EVOLVING -> 0.9f
            AICognitiveState.UNCERTAIN -> 0.5f
            AICognitiveState.EXECUTING -> 0.9f
            AICognitiveState.ERROR -> 1.2f
        }
        
        // Scale by confidence: confident = brighter, uncertain = dimmer
        return baseIntensity * confidence
    }
    
    /**
     * Particle emission: more particles when thinking, fewer when idle
     * Reflects mental activity intensity
     */
    private fun computeParticleRate(state: AICognitiveState, memoryLoad: Float): Float {
        val baseRate = when (state) {
            AICognitiveState.IDLE -> 0.3f
            AICognitiveState.THINKING -> 1.0f
            AICognitiveState.DELIBERATING -> 1.5f
            AICognitiveState.REFLECTING -> 0.8f
            AICognitiveState.EVOLVING -> 1.8f
            AICognitiveState.UNCERTAIN -> 1.2f
            AICognitiveState.EXECUTING -> 0.9f
            AICognitiveState.ERROR -> 2.0f
        }
        
        // Modulate by how much memory is actively used
        return baseRate * (1.0f + memoryLoad * 0.5f)
    }
    
    /**
     * Morphing intensity: geometry deformation during evolution/adaptation
     * Reflects how much AI rules and understanding are changing
     */
    private fun computeMorphingIntensity(state: AICognitiveState, adaptationRate: Float): Float {
        val baseIntensity = when (state) {
            AICognitiveState.IDLE -> 0.0f
            AICognitiveState.THINKING -> 0.1f
            AICognitiveState.DELIBERATING -> 0.2f
            AICognitiveState.REFLECTING -> 0.3f          // Self-analysis causes morphing
            AICognitiveState.EVOLVING -> 0.8f             // High morphing during evolution
            AICognitiveState.UNCERTAIN -> 0.4f            // Uncertainty causes shape instability
            AICognitiveState.EXECUTING -> 0.0f
            AICognitiveState.ERROR -> 0.5f
        }
        
        // Strongly modulate by actual adaptation rate
        return (baseIntensity + adaptationRate * 0.7f).coerceIn(0f, 1f)
    }
    
    /**
     * Transition duration: how long to animate between states
     * Important events get longer transitions to emphasize them
     */
    private fun computeTransitionDuration(from: AICognitiveState?, to: AICognitiveState): Float {
        if (from == null) return 0.3f
        
        // Significant state transitions get longer animations
        return when {
            // Evolution is a major event
            to == AICognitiveState.EVOLVING -> 1.5f
            // Error demands attention
            to == AICognitiveState.ERROR -> 1.0f
            // Reflection is introspective
            to == AICognitiveState.REFLECTING -> 0.8f
            // Quick transitions for routine operations
            from == AICognitiveState.IDLE && to == AICognitiveState.THINKING -> 0.4f
            else -> 0.6f
        }
    }
}

/**
 * Helper to create metrics from raw AI data
 */
object AIMetricsBuilder {
    
    /**
     * Build confidence metrics from decision record
     */
    fun buildConfidenceMetrics(
        decisionConfidence: Float,
        predictionConfidence: Float,
        knowledgeConfidence: Float
    ): ConfidenceMetrics {
        val avg = (decisionConfidence + predictionConfidence + knowledgeConfidence) / 3f
        return ConfidenceMetrics(
            decisionConfidence = decisionConfidence.coerceIn(0f, 1f),
            predictionConfidence = predictionConfidence.coerceIn(0f, 1f),
            knowledgeConfidence = knowledgeConfidence.coerceIn(0f, 1f),
            averageConfidence = avg.coerceIn(0f, 1f)
        )
    }
    
    /**
     * Build processing metrics from AI engine data
     */
    fun buildProcessingMetrics(
        cognitiveLoad: Float,
        decisionComplexity: Float,
        uncertaintyLevel: Float,
        learningRate: Float,
        successRate: Float,
        memoryLoad: Float,
        adaptationIntensity: Float
    ): AIProcessingMetrics {
        return AIProcessingMetrics(
            cognitiveLoad = cognitiveLoad.coerceIn(0f, 1f),
            decisionComplexity = decisionComplexity.coerceIn(0f, 1f),
            uncertaintyLevel = uncertaintyLevel.coerceIn(0f, 1f),
            learningRate = learningRate.coerceIn(0f, 1f),
            successMetrics = successRate.coerceIn(0f, 1f),
            memoryLoad = memoryLoad.coerceIn(0f, 1f),
            adaptationIntensity = adaptationIntensity.coerceIn(0f, 1f)
        )
    }
}
