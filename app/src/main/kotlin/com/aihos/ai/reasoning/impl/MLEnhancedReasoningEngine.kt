package com.aihos.ai.reasoning.impl

import com.aihos.ai.ml.*
import com.aihos.ai.reasoning.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ML-Enhanced Reasoning Engine
 * Wraps rule-based reasoning with optional ML confidence augmentation
 *
 * Design:
 * 1. Base reasoning layer (DefaultReasoningLayer) generates options and scores
 * 2. ML augmentation layer provides confidence adjustments
 * 3. Final confidence = rule confidence * (1 + ML adjustment/3)
 * 4. Transparent explanation shows both reasoning paths
 * 5. Graceful fallback if ML unavailable
 *
 * Key Properties:
 * - Rule-based reasoning remains authoritative (primary)
 * - ML acts as secondary signal (bounded influence)
 * - ML adjustment bounded to [-0.3, 0.3]
 * - Clear separation of concerns
 * - Full transparency in decision explanation
 */
class MLEnhancedReasoningEngine(
    private val baseReasoner: ReasoningEngine = HeuristicReasoningEngine(),
    private val mlManager: MLInterpreterManager? = null,
    private val mlAugmenter: MLConfidenceAugmenter? = null
) : ReasoningEngine {
    
    /**
     * Generate options with ML augmentation
     */
    override suspend fun generateOptions(context: ReasoningContext): List<Option> {
        return withContext(Dispatchers.Default) {
            // Base reasoning generates options
            val baseOptions = baseReasoner.generateOptions(context)
            Timber.d("Base reasoner generated ${baseOptions.size} options")
            return@withContext baseOptions
        }
    }
    
    /**
     * Score option with ML augmentation
     * Primary: rule-based confidence from baseReasoner
     * Secondary: ML adjustment from mlManager
     */
    override suspend fun scoreOption(
        option: Option,
        context: ReasoningContext
    ): Float {
        return withContext(Dispatchers.Default) {
            try {
                // Get base rule confidence
                val ruleConfidence = baseReasoner.scoreOption(option, context)
                Timber.d("Base rule confidence for ${option.action}: $ruleConfidence")
                
                // If ML not available, return rule confidence only
                if (mlManager == null || mlAugmenter == null) {
                    Timber.d("ML manager unavailable, returning rule confidence only")
                    return@withContext ruleConfidence
                }
                
                if (!mlManager.isAvailable()) {
                    Timber.d("ML not ready, returning rule confidence only")
                    return@withContext ruleConfidence
                }
                
                // Convert ReasoningContext to DeviceContext for ML
                val deviceContext = contextToDeviceContext(context)
                
                // Get ML augmentation
                val augmented = mlAugmenter.augmentConfidence(ruleConfidence, deviceContext)
                Timber.d("ML augmentation for ${option.action}: adjustment=${augmented.mlAdjustment}, final=${augmented.finalConfidence}")
                
                return@withContext augmented.finalConfidence
                
            } catch (e: Exception) {
                Timber.w(e, "Error during ML-enhanced scoring, falling back to rule confidence")
                return@withContext baseReasoner.scoreOption(option, context)
            }
        }
    }
    
    /**
     * Explain decision with both rule and ML paths
     */
    override suspend fun explainDecision(
        chosen: Option,
        alternatives: List<Option>,
        context: ReasoningContext
    ): String {
        return withContext(Dispatchers.Default) {
            // Get base explanation
            val baseExplanation = baseReasoner.explainDecision(chosen, alternatives, context)
            
            // If ML available, add augmentation explanation
            if (mlManager != null && mlAugmenter != null && mlManager.isAvailable()) {
                try {
                    val deviceContext = contextToDeviceContext(context)
                    val ruleConfidence = baseReasoner.scoreOption(chosen, context)
                    val augmented = mlAugmenter.augmentConfidence(ruleConfidence, deviceContext)
                    val mlExplanation = mlAugmenter.explainAugmentation(augmented)
                    
                    return@withContext buildString {
                        appendLine("=== DECISION EXPLANATION ===")
                        appendLine()
                        appendLine("PRIMARY (Rule-Based Reasoning):")
                        appendLine(baseExplanation.prependIndent("  "))
                        appendLine()
                        appendLine("SECONDARY (ML-Based Augmentation):")
                        appendLine(mlExplanation.prependIndent("  "))
                        appendLine()
                        appendLine("FINAL DECISION:")
                        appendLine("  Action: ${chosen.action}")
                        appendLine("  Confidence: ${(augmented.finalConfidence * 100).toInt()}%")
                        appendLine("  Decision Authority: Rule-based (primary), with ML boost/dampening")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error generating ML-augmented explanation, using base only")
                    return@withContext baseExplanation
                }
            }
            
            return@withContext baseExplanation
        }
    }
    
    /**
     * Convert ReasoningContext to DeviceContext for ML inference
     * Maps available reasoning context fields to device context
     */
    private fun contextToDeviceContext(context: ReasoningContext): DeviceContext {
        return DeviceContext(
            // Extract hour from currentTime string "HH:MM"
            batteryPercent = context.batteryPercent,
            screenOn = true,  // Default to on (would need to integrate from system signals)
            networkConnected = true,  // Default to connected
            temperatureCelsius = 30f,  // Default room temperature
            timeOfDay = extractTimeOfDay(context.currentTime),
            foregroundApp = if (context.appUsageDurationMinutes > 0) "productive" else "system",
            idleTimeSeconds = 0,  // Assume active since generating options
            recentDecisionCount = context.recentDecisions.size,
            timestamp = context.timestamp
        )
    }
    
    /**
     * Extract time of day from HH:MM format
     * Return 0.0-1.0 where 0.0=midnight, 0.5=noon, 1.0=next midnight
     */
    private fun extractTimeOfDay(timeStr: String): Float {
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 12
                val minute = parts[1].toIntOrNull() ?: 0
                ((hour * 60 + minute) / 1440f).coerceIn(0f, 1f)
            } else {
                0.5f  // Default to noon
            }
        } catch (e: Exception) {
            Timber.w("Error parsing time $timeStr, defaulting to 0.5")
            0.5f
        }
    }
}

/**
 * Heuristic Reasoning Engine (Original Implementation)
 * Pure rule-based reasoning without ML
 */
class HeuristicReasoningEngine(
    private val llmProvider: LocalLLMProvider? = null
) : ReasoningEngine {
    
    override suspend fun generateOptions(context: ReasoningContext): List<Option> {
        return listOf(
            Option(
                id = "opt_1",
                action = "send_focus_reminder",
                expectedOutcome = "User takes a break to reset focus",
                riskLevel = RiskLevel.MEDIUM
            ),
            Option(
                id = "opt_2",
                action = "suggest_mindfulness_pause",
                expectedOutcome = "User practices breathing exercise",
                riskLevel = RiskLevel.LOW
            ),
            Option(
                id = "opt_3",
                action = "do_nothing",
                expectedOutcome = "Observe user behavior without intervention",
                riskLevel = RiskLevel.LOW
            )
        )
    }
    
    override suspend fun scoreOption(option: Option, context: ReasoningContext): Float {
        var score = 0.5f
        
        // Increase score if user has been engaged long
        score += (context.appUsageDurationMinutes / 300f) * 0.2f
        
        // Decrease score for high-risk actions
        score -= when (option.riskLevel) {
            RiskLevel.HIGH -> 0.3f
            RiskLevel.MEDIUM -> 0.1f
            RiskLevel.LOW -> 0f
        }
        
        // Do nothing is safer but less helpful
        if (option.action == "do_nothing") {
            score = 0.3f
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    override suspend fun explainDecision(
        chosen: Option,
        alternatives: List<Option>,
        context: ReasoningContext
    ): String {
        return buildString {
            appendLine("Decision: ${chosen.action}")
            appendLine("Confidence: ${(scoreOption(chosen, context) * 100).toInt()}%")
            appendLine()
            appendLine("Reasoning:")
            appendLine("- User engagement: ${context.appUsageDurationMinutes} minutes")
            appendLine("- Recent interactions: ${context.recentInteractionCount}")
            appendLine("- User is focused: ${context.userIsFocused}")
            appendLine()
            appendLine("This action chosen because:")
            appendLine("  1. User has been engaged for extended period")
            appendLine("  2. Action is reversible and low-risk")
            appendLine("  3. Aligns with user goals: ${context.userGoals.joinToString(", ")}")
            appendLine()
            appendLine("Alternatives considered:")
            alternatives.forEach { alt ->
                appendLine("  - ${alt.action} (risk: ${alt.riskLevel})")
            }
        }
    }
}
