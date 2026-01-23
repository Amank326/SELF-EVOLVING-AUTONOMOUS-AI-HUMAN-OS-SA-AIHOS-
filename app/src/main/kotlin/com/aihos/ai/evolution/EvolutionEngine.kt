package com.aihos.ai.evolution

import com.aihos.ai.memory.BehavioralRule
import com.aihos.ai.reflection.Insight
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Evolution engine: modifies behavioral rules based on learning
 * This is how SA-AIHOS becomes smarter over time
 */
interface EvolutionEngine {
    /**
     * Update a rule's weight based on feedback
     */
    suspend fun updateRuleWeight(
        ruleId: String,
        feedback: EvolutionFeedback
    ): BehavioralRule?
    
    /**
     * Create a new rule from an insight
     */
    suspend fun createNewRule(insight: Insight): BehavioralRule
    
    /**
     * Deprecate a rule (phase out over time)
     */
    suspend fun deprecateRule(ruleId: String)
    
    /**
     * Get evolution report
     */
    suspend fun getEvolutionReport(): EvolutionReport
}

/**
 * Feedback signal for evolution
 */
@Serializable
data class EvolutionFeedback(
    val ruleId: String,
    val isSuccess: Boolean,
    val confidenceLevel: Float, // 0.0 to 1.0
    val context: String = "",
    val userFeedback: String? = null
)

/**
 * Report of evolution activities
 */
@Serializable
data class EvolutionReport(
    val timestamp: Long = System.currentTimeMillis(),
    val totalRulesCount: Int,
    val activeRulesCount: Int,
    val deprecatedRulesCount: Int,
    val newRulesCreatedThisSession: Int,
    val weightUpdatesThisSession: Int,
    val topPerformingRules: List<BehavioralRule> = emptyList(),
    val underperformingRules: List<BehavioralRule> = emptyList()
)

/**
 * Log entry for evolution operations
 */
@Serializable
data class EvolutionLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val ruleId: String,
    val changeType: ChangeType,
    val oldValue: String?,
    val newValue: String?,
    val reflection: String
)

enum class ChangeType {
    WEIGHT_UPDATE,
    CREATION,
    DEPRECATION,
    REACTIVATION
}

/**
 * Default implementation of EvolutionEngine
 */
class DefaultEvolutionEngine(
    private val memoryRepository: com.aihos.ai.memory.MemoryRepository
) : EvolutionEngine {
    
    // Constant for evolution dynamics
    companion object {
        // Exponential moving average momentum: how much to favor recent outcomes
        // 0.7 = 70% recent, 30% historical
        private const val MOMENTUM = 0.7f
        
        // Minimum success rate threshold for active rule
        private const val MIN_SUCCESS_THRESHOLD = 0.3f
        
        // Starting weight for new rules (conservative)
        private const val NEW_RULE_WEIGHT = 0.3f
    }
    
    override suspend fun updateRuleWeight(
        ruleId: String,
        feedback: EvolutionFeedback
    ): BehavioralRule? {
        val rule = memoryRepository.getRule(ruleId) ?: return null
        
        // Update outcome counts
        if (feedback.isSuccess) {
            memoryRepository.updateRuleOutcome(ruleId, isSuccess = true)
        } else {
            memoryRepository.updateRuleOutcome(ruleId, isSuccess = false)
        }
        
        // Calculate success rate
        val successRate = rule.successRate
        
        // Exponential moving average weight update
        val newWeight = (rule.weight * MOMENTUM) + (successRate * (1 - MOMENTUM))
        
        // Apply confidence dampening (low confidence feedback has less impact)
        val adjustedWeight = newWeight * feedback.confidenceLevel + rule.weight * (1 - feedback.confidenceLevel)
        
        // Deprecate if falling below threshold
        if (adjustedWeight < MIN_SUCCESS_THRESHOLD && rule.isActive) {
            deprecateRule(ruleId)
            return null
        }
        
        // Update the rule
        memoryRepository.updateRuleWeight(ruleId, adjustedWeight)
        
        return rule.copy(weight = adjustedWeight, evolvedAt = System.currentTimeMillis())
    }
    
    override suspend fun createNewRule(insight: Insight): BehavioralRule {
        // Generate rule from insight
        val rule = BehavioralRule(
            condition = generateConditionFromInsight(insight),
            action = generateActionFromInsight(insight),
            weight = NEW_RULE_WEIGHT, // Start conservative
            successCount = 0,
            failureCount = 0,
            isActive = true
        )
        
        memoryRepository.storeRule(rule)
        return rule
    }
    
    override suspend fun deprecateRule(ruleId: String) {
        val rule = memoryRepository.getRule(ruleId) ?: return
        
        // Mark as inactive (gradual phase-out)
        memoryRepository.storeRule(rule.copy(isActive = false, evolvedAt = System.currentTimeMillis()))
    }
    
    override suspend fun getEvolutionReport(): EvolutionReport {
        val allRules = memoryRepository.getAllRules()
        val activeRules = allRules.filter { it.isActive }
        val deprecatedRules = allRules.filter { !it.isActive }
        
        val topPerformers = activeRules.sortedByDescending { it.successRate }.take(5)
        val underperformers = activeRules.sortedBy { it.successRate }.take(5)
        
        return EvolutionReport(
            totalRulesCount = allRules.size,
            activeRulesCount = activeRules.size,
            deprecatedRulesCount = deprecatedRules.size,
            newRulesCreatedThisSession = 0, // TODO: track from log
            weightUpdatesThisSession = 0,   // TODO: track from log
            topPerformingRules = topPerformers,
            underperformingRules = underperformers
        )
    }
    
    // Helper: Generate condition string from insight
    private fun generateConditionFromInsight(insight: Insight): String {
        return when (insight.type) {
            com.aihos.ai.reflection.InsightType.TIMING_ISSUE ->
                "time >= 20:00 AND time <= 23:59"
            
            com.aihos.ai.reflection.InsightType.ACTION_REFINEMENT ->
                "context == 'in_active_conversation'"
            
            com.aihos.ai.reflection.InsightType.CONTEXT_MISSED ->
                insight.description.take(100) // Placeholder condition
            
            else ->
                "generic_condition"
        }
    }
    
    // Helper: Generate action string from insight
    private fun generateActionFromInsight(insight: Insight): String {
        return when (insight.type) {
            com.aihos.ai.reflection.InsightType.ACTION_REFINEMENT ->
                "suggest_pause_in_conversation"
            
            com.aihos.ai.reflection.InsightType.TIMING_ISSUE ->
                "send_gentle_evening_reminder"
            
            com.aihos.ai.reflection.InsightType.USER_PREFERENCE ->
                "respect_user_preference"
            
            else ->
                "generic_action"
        }
    }
}

/**
 * Extension: Implement this in actual repository
 */
interface MemoryRepository {
    suspend fun getRule(id: String): BehavioralRule?
    suspend fun getAllRules(): List<BehavioralRule>
    suspend fun storeRule(rule: BehavioralRule)
    suspend fun updateRuleWeight(ruleId: String, newWeight: Float)
    suspend fun updateRuleOutcome(ruleId: String, isSuccess: Boolean)
}
