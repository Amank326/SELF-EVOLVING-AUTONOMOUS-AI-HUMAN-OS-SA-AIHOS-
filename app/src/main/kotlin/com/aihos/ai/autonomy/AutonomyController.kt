package com.aihos.ai.autonomy

import com.aihos.ai.memory.Episode
import com.aihos.ai.memory.MemoryRepository
import com.aihos.ai.memory.Outcome
import com.aihos.ai.reasoning.*
import com.aihos.ai.reflection.ReflectionEngine
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.evolution.EvolutionFeedback
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.*

/**
 * Autonomy Controller: Orchestrates the entire THINK → ACT → REFLECT → EVOLVE cycle
 * This is the heart of SA-AIHOS
 */
interface AutonomyController {
    /**
     * Main decision loop - continuously runs and makes autonomous decisions
     */
    suspend fun startDecisionLoop()
    
    /**
     * Stop the decision loop
     */
    suspend fun stopDecisionLoop()
    
    /**
     * Manually trigger a decision cycle (for testing or user-initiated)
     */
    suspend fun triggerDecisionCycle(context: ReasoningContext): DecisionOutcome
    
    /**
     * Report outcome of a decision
     */
    suspend fun reportOutcome(decisionId: String, outcome: Outcome, feedback: String = "")
    
    /**
     * Get autonomy settings
     */
    fun getSettings(): AutonomySettings
    
    /**
     * Update autonomy settings
     */
    fun updateSettings(settings: AutonomySettings)
}

/**
 * Autonomy settings and permissions
 */
@Serializable
enum class AutonomyLevel {
    DISABLED,        // No autonomous actions
    ADVISORY,        // Propose actions to user
    INTERACTIVE,     // Ask for approval
    CONSTRAINED,     // Limited autonomous domain
    FULL_AUTONOMOUS  // Full autonomy within safe boundaries
}

@Serializable
data class AutonomySettings(
    val level: AutonomyLevel = AutonomyLevel.CONSTRAINED,
    val allowedActions: Set<String> = setOf(
        "send_focus_reminder",
        "suggest_mindfulness_pause",
        "log_insight"
    ),
    val decisionIntervalMs: Long = 60_000L, // 1 minute
    val maxDecisionsPerHour: Int = 10,
    val requiresUserApprovalFor: Set<String> = setOf(),
    val ruleModificationEnabled: Boolean = true,
    val evolutionEnabled: Boolean = true
)

/**
 * Outcome of a decision execution
 */
@Serializable
data class DecisionOutcome(
    val decisionId: String,
    val action: String,
    val executed: Boolean,
    val userApprovalNeeded: Boolean,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Default implementation of AutonomyController
 */
class DefaultAutonomyController(
    private val memoryRepository: MemoryRepository,
    private val reasoningEngine: ReasoningEngine,
    private val reflectionEngine: ReflectionEngine,
    private val evolutionEngine: EvolutionEngine,
    private val contextProvider: ContextProvider,
    private val actionExecutor: ActionExecutor
) : AutonomyController {
    
    private var isRunning = false
    private var settings = AutonomySettings()
    private val decisionHistory = mutableListOf<DecisionRecord>()
    private val decisionsThisHour = mutableListOf<Long>()
    
    override suspend fun startDecisionLoop() {
        isRunning = true
        Timber.i("Autonomy Controller started")
        
        while (isRunning) {
            try {
                // PHASE 1: SENSE
                val context = contextProvider.getCurrentContext()
                if (shouldConsiderAction(context)) {
                    // PHASE 2: THINK
                    val outcome = triggerDecisionCycle(context)
                    
                    // PHASE 3 & 4: ACT + monitoring setup
                    if (outcome.executed) {
                        scheduleOutcomeMonitoring(outcome.decisionId)
                    }
                }
                
                // PHASE 5: REFLECT (async, periodically)
                scheduleReflectionBatch()
                
                // PHASE 5: EVOLVE (async, periodically)
                scheduleEvolutionBatch()
                
                // Wait before next cycle
                delay(settings.decisionIntervalMs)
                
            } catch (e: Exception) {
                Timber.e(e, "Error in autonomy loop")
                delay(5000) // Backoff on error
            }
        }
    }
    
    override suspend fun stopDecisionLoop() {
        isRunning = false
        Timber.i("Autonomy Controller stopped")
    }
    
    override suspend fun triggerDecisionCycle(context: ReasoningContext): DecisionOutcome {
        // Check rate limiting
        cleanupOldDecisions()
        if (decisionsThisHour.size >= settings.maxDecisionsPerHour) {
            Timber.d("Rate limit exceeded for this hour")
            return DecisionOutcome(
                decisionId = UUID.randomUUID().toString(),
                action = "none",
                executed = false,
                userApprovalNeeded = false,
                reasoning = "Rate limit exceeded"
            )
        }
        
        // THINK: Generate options and score them
        val options = reasoningEngine.generateOptions(context)
        val selectedOption = selectBestOption(options, context)
        val reasoning = reasoningEngine.explainDecision(selectedOption, options, context)
        
        // Check constraints
        if (!isActionAllowed(selectedOption)) {
            Timber.d("Action not allowed: ${selectedOption.action}")
            return DecisionOutcome(
                decisionId = UUID.randomUUID().toString(),
                action = selectedOption.action,
                executed = false,
                userApprovalNeeded = false,
                reasoning = "Action not allowed by autonomy settings"
            )
        }
        
        // Create decision record
        val decision = DecisionRecord(
            id = UUID.randomUUID().toString(),
            context = context,
            chosenOption = selectedOption,
            allOptions = options,
            reasoning = reasoning,
            confidenceLevel = reasoningEngine.scoreOption(selectedOption, context),
            executionStatus = ExecutionStatus.PENDING
        )
        
        decisionHistory.add(decision)
        
        // ACT: Execute action if allowed
        val needsApproval = selectedOption.action in settings.requiresUserApprovalFor
        val executed = when {
            settings.level == AutonomyLevel.DISABLED -> false
            settings.level == AutonomyLevel.ADVISORY -> false
            needsApproval && settings.level == AutonomyLevel.INTERACTIVE -> {
                actionExecutor.requestUserApproval(decision)
                false
            }
            settings.level == AutonomyLevel.FULL_AUTONOMOUS || settings.level == AutonomyLevel.CONSTRAINED -> {
                executeAction(decision)
            }
            else -> false
        }
        
        // Log the decision
        val episode = Episode(
            decision = selectedOption.action,
            action = selectedOption.action,
            context = mapOf(
                "time" to context.currentTime,
                "confidence" to decision.confidenceLevel.toString()
            ),
            outcome = if (executed) Outcome.PENDING else Outcome.FAILURE,
            reasoning = reasoning
        )
        memoryRepository.storeEpisode(episode)
        
        decisionsThisHour.add(System.currentTimeMillis())
        
        return DecisionOutcome(
            decisionId = decision.id,
            action = selectedOption.action,
            executed = executed,
            userApprovalNeeded = needsApproval,
            reasoning = reasoning
        )
    }
    
    override suspend fun reportOutcome(
        decisionId: String,
        outcome: Outcome,
        feedback: String
    ) {
        val decision = decisionHistory.find { it.id == decisionId } ?: return
        
        Timber.d("Decision outcome: $decisionId -> $outcome")
        
        // REFLECT: Analyze what happened
        val reflection = reflectionEngine.analyzeOutcome(
            decision = decision,
            actualOutcome = outcome,
            outcomeFeedback = feedback
        )
        
        // EVOLVE: Update rules based on reflection
        if (settings.evolutionEnabled) {
            reflection.insights.forEach { insight ->
                if (insight.importance > 0.7f) {
                    val newRule = evolutionEngine.createNewRule(insight)
                    Timber.i("Created new rule: ${newRule.id}")
                }
            }
            
            // Update rule weights
            val ruleId = decision.chosenOption.id
            val feedback = EvolutionFeedback(
                ruleId = ruleId,
                isSuccess = outcome == Outcome.SUCCESS,
                confidenceLevel = reflection.confidenceInAnalysis,
                context = reflection.recommendation
            )
            evolutionEngine.updateRuleWeight(ruleId, feedback)
        }
    }
    
    override fun getSettings(): AutonomySettings = settings
    
    override fun updateSettings(settings: AutonomySettings) {
        this.settings = settings
        Timber.i("Autonomy settings updated: ${settings.level}")
    }
    
    // HELPERS
    
    private suspend fun executeAction(decision: DecisionRecord): Boolean {
        return try {
            actionExecutor.execute(decision.chosenOption.action)
            Timber.i("Action executed: ${decision.chosenOption.action}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute action")
            false
        }
    }
    
    private fun shouldConsiderAction(context: ReasoningContext): Boolean {
        // Don't act if disabled
        if (settings.level == AutonomyLevel.DISABLED) return false
        
        // Could add more heuristics here
        return true
    }
    
    private fun isActionAllowed(option: Option): Boolean {
        return option.action in settings.allowedActions
    }
    
    private suspend fun selectBestOption(
        options: List<Option>,
        context: ReasoningContext
    ): Option {
        // Score all options and select highest
        val scored = options.map { option ->
            option to reasoningEngine.scoreOption(option, context)
        }
        return scored.maxByOrNull { it.second }?.first ?: options.first()
    }
    
    private fun cleanupOldDecisions() {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000
        decisionsThisHour.removeAll { it < oneHourAgo }
    }
    
    private suspend fun scheduleOutcomeMonitoring(decisionId: String) {
        // TODO: Set up monitoring to detect outcome
        // After some time, call reportOutcome()
    }
    
    private suspend fun scheduleReflectionBatch() {
        // Periodic reflection on recent decisions
        // Every 5 minutes or N decisions
    }
    
    private suspend fun scheduleEvolutionBatch() {
        // Periodic evolution of rules
        // Every hour or after N reflections
    }
}

/**
 * Provider for current system context
 */
interface ContextProvider {
    suspend fun getCurrentContext(): ReasoningContext
}

/**
 * Executor for autonomous actions
 */
interface ActionExecutor {
    suspend fun execute(action: String): Boolean
    suspend fun requestUserApproval(decision: DecisionRecord): Boolean
}
