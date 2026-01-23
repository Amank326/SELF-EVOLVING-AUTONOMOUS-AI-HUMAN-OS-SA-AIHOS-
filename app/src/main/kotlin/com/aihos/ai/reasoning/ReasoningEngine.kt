package com.aihos.ai.reasoning

import com.aihos.ai.memory.Episode
import kotlinx.serialization.Serializable

/**
 * Core reasoning engine interface
 * This is where the AI thinks about what to do
 */
interface ReasoningEngine {
    /**
     * Generate possible actions given current context
     */
    suspend fun generateOptions(context: ReasoningContext): List<Option>
    
    /**
     * Score an option: how good is this decision given our goals?
     * Returns float 0.0 to 1.0
     */
    suspend fun scoreOption(option: Option, context: ReasoningContext): Float
    
    /**
     * Explain why an option was chosen over alternatives
     */
    suspend fun explainDecision(
        chosen: Option,
        alternatives: List<Option>,
        context: ReasoningContext
    ): String
}

/**
 * Context passed to reasoning engine
 * Contains everything the AI needs to think about
 */
@Serializable
data class ReasoningContext(
    val timestamp: Long = System.currentTimeMillis(),
    val currentTime: String, // "22:45"
    val dayOfWeek: String, // "FRIDAY"
    val appUsageDurationMinutes: Int,
    val recentInteractionCount: Int,
    val userIsFocused: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val recentDecisions: List<String> = emptyList(),
    val userGoals: List<String> = emptyList(),
    val userPreferences: Map<String, String> = emptyMap(),
    val availableActions: List<String> = emptyList()
)

/**
 * Possible action option
 */
@Serializable
data class Option(
    val id: String,
    val action: String, // "send_focus_reminder", "pause_and_breathe", etc.
    val expectedOutcome: String,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val reversible: Boolean = true
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

/**
 * Decision record: what the AI decided and why
 */
@Serializable
data class DecisionRecord(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val context: ReasoningContext,
    val chosenOption: Option,
    val allOptions: List<Option>,
    val reasoning: String,
    val assumptions: List<String> = emptyList(),
    val confidenceLevel: Float, // 0.0 to 1.0
    val executionStatus: ExecutionStatus = ExecutionStatus.PENDING
)

enum class ExecutionStatus {
    PENDING, EXECUTING, EXECUTED, FAILED, DEFERRED, USER_DECLINED
}

/**
 * Default reasoning engine implementation
 * Uses heuristic-based scoring with learned behavioral rules
 */
class HeuristicReasoningEngine(
    private val llmProvider: LocalLLMProvider? = null
) : ReasoningEngine {
    
    override suspend fun generateOptions(context: ReasoningContext): List<Option> {
        // In Phase 1, return hardcoded options
        // Phase 2: Generate options from LLM
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
        // Heuristic scoring
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
            appendLine()
            appendLine("Assumptions made:")
            appendLine("  - User engagement = fatigue level")
            appendLine("  - Intervention will be well-received")
            appendLine("  - External factors haven't changed")
        }
    }
}

/**
 * Local LLM Provider interface
 * Allows swapping different LLM backends
 */
interface LocalLLMProvider {
    suspend fun generate(
        prompt: String,
        context: Map<String, String> = emptyMap(),
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): LLMResponse
    
    fun isAvailable(): Boolean
    fun getCapabilities(): LLMCapabilities
    fun getModelName(): String
}

@Serializable
data class LLMResponse(
    val text: String,
    val tokensUsed: Int,
    val processingTimeMs: Long,
    val confidence: Float // Model's confidence in response
)

@Serializable
data class LLMCapabilities(
    val maxContextLength: Int,
    val maxOutputTokens: Int,
    val supportsStreaming: Boolean,
    val supportsFunctionCalling: Boolean,
    val approxLatencyMs: Int
)

/**
 * Stub implementation for Phase 1 (before LLM integration)
 */
class StubLLMProvider : LocalLLMProvider {
    override suspend fun generate(
        prompt: String,
        context: Map<String, String>,
        maxTokens: Int,
        temperature: Float
    ): LLMResponse {
        return LLMResponse(
            text = "This is a stub response. Connect a real LLM in Phase 2.",
            tokensUsed = 10,
            processingTimeMs = 50,
            confidence = 0.5f
        )
    }
    
    override fun isAvailable(): Boolean = false
    
    override fun getCapabilities(): LLMCapabilities {
        return LLMCapabilities(
            maxContextLength = 0,
            maxOutputTokens = 0,
            supportsStreaming = false,
            supportsFunctionCalling = false,
            approxLatencyMs = 0
        )
    }
    
    override fun getModelName(): String = "StubProvider"
}
