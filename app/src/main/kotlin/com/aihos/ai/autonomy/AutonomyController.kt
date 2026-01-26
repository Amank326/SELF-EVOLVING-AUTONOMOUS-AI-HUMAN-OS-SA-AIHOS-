package com.aihos.ai.autonomy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Autonomy Controller - Self-directed decision making and action execution
 * Enables:
 * - Goal-driven behavior
 * - Autonomous action selection
 * - Decision trees and planning
 * - Constraint satisfaction
 * - Priority management
 */

data class Goal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val priority: Float = 0.5f, // 0.0 to 1.0
    val deadline: Long? = null,
    val status: String = "active", // active, completed, abandoned
    val progress: Float = 0f
)

data class Action(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val prerequisites: List<String> = emptyList(),
    val expectedUtility: Float = 0.5f,
    val cost: Float = 0.1f,
    val riskLevel: Float = 0.2f,
    val timestamp: Long = Instant.now().toEpochMilli()
)

data class Decision(
    val id: String = java.util.UUID.randomUUID().toString(),
    val context: String,
    val options: List<Action> = emptyList(),
    val selectedAction: String? = null,
    val confidence: Float = 0.5f,
    val reasoning: String = "",
    val executedAt: Long? = null
)

data class AutonomyState(
    val activeGoals: List<Goal> = emptyList(),
    val pendingDecisions: List<Decision> = emptyList(),
    val executedActions: List<Action> = emptyList(),
    val autonomyLevel: Float = 0.5f, // 0.0 (manual) to 1.0 (fully autonomous)
    val adaptability: Float = 0.5f
)

/**
 * Autonomy Controller - Makes autonomous decisions
 */
class AutonomyController {
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals
    
    private val _decisions = MutableStateFlow<List<Decision>>(emptyList())
    val decisions: StateFlow<List<Decision>> = _decisions
    
    private val _executedActions = MutableStateFlow<List<Action>>(emptyList())
    val executedActions: StateFlow<List<Action>> = _executedActions
    
    private val _autonomyLevel = MutableStateFlow(0.5f)
    val autonomyLevel: StateFlow<Float> = _autonomyLevel
    
    private val _autonomyState = MutableStateFlow(AutonomyState())
    val autonomyState: StateFlow<AutonomyState> = _autonomyState
    
    private val _recentDecision = MutableStateFlow<Decision?>(null)
    val recentDecision: StateFlow<Decision?> = _recentDecision
    
    // Decision history
    private val decisionHistory = mutableListOf<Decision>()

    /**
     * Add a new goal
     */
    suspend fun setGoal(description: String, priority: Float = 0.5f, deadline: Long? = null) {
        val goal = Goal(
            description = description,
            priority = priority.coerceIn(0f, 1f),
            deadline = deadline
        )
        val current = _goals.value.toMutableList()
        current.add(goal)
        _goals.emit(current)
        updateAutonomyState()
    }

    /**
     * Update goal progress
     */
    suspend fun updateGoalProgress(goalId: String, progress: Float) {
        val current = _goals.value.toMutableList()
        val index = current.indexOfFirst { it.id == goalId }
        if (index >= 0) {
            val goal = current[index]
            val status = when {
                progress >= 1.0f -> "completed"
                progress <= 0f -> "abandoned"
                else -> "active"
            }
            current[index] = goal.copy(progress = progress.coerceIn(0f, 1f), status = status)
            _goals.emit(current)
            updateAutonomyState()
        }
    }

    /**
     * Autonomous decision making - Choose best action
     */
    suspend fun makeDecision(context: String, possibleActions: List<Action>): Action? {
        if (possibleActions.isEmpty()) return null
        
        // Evaluate utility for each action
        val evaluatedActions = possibleActions.map { action ->
            val utility = calculateUtility(action, context)
            action.copy(expectedUtility = utility)
        }.sortedByDescending { it.expectedUtility }
        
        // Select best action
        val selectedAction = evaluatedActions.firstOrNull()
        
        if (selectedAction != null) {
            val decision = Decision(
                context = context,
                options = evaluatedActions,
                selectedAction = selectedAction.id,
                confidence = selectedAction.expectedUtility,
                reasoning = "Selected based on utility: ${selectedAction.expectedUtility}"
            )
            
            val current = _decisions.value.toMutableList()
            current.add(decision)
            _decisions.emit(current)
            decisionHistory.add(decision)
            _recentDecision.emit(decision)
            
            return selectedAction
        }
        
        return null
    }

    /**
     * Execute selected action
     */
    suspend fun executeAction(action: Action) {
        val executedAction = action.copy(timestamp = Instant.now().toEpochMilli())
        val current = _executedActions.value.toMutableList()
        current.add(executedAction)
        _executedActions.emit(current)
        updateAutonomyState()
    }

    /**
     * Plan sequence of actions to achieve goal
     */
    suspend fun planActions(goalId: String, availableActions: List<Action>): List<Action>? {
        val goal = _goals.value.find { it.id == goalId } ?: return null
        
        // Simple planning: select actions with relevant prerequisites
        val plan = mutableListOf<Action>()
        var satisfiedPrerequisites = setOf<String>()
        
        for (action in availableActions.sortedByDescending { it.expectedUtility }) {
            // Check if action prerequisites are satisfied
            val prereqsSatisfied = action.prerequisites.all { satisfiedPrerequisites.contains(it) }
            if (prereqsSatisfied) {
                plan.add(action)
                // Add action name to satisfied prerequisites for next iteration
                satisfiedPrerequisites = satisfiedPrerequisites + action.name
                
                // Stop if plan becomes complex (prevent infinite loops)
                if (plan.size >= 5) break
            }
        }
        
        return if (plan.isNotEmpty()) plan else null
    }

    /**
     * Adjust autonomy level based on confidence
     */
    suspend fun adjustAutonomyLevel(confidence: Float, complexity: Float) {
        // Higher confidence and lower complexity → higher autonomy
        val baseAutonomy = confidence * (1f - complexity.coerceIn(0f, 1f))
        val newLevel = (baseAutonomy * 0.8f + _autonomyLevel.value * 0.2f).coerceIn(0f, 1f)
        _autonomyLevel.emit(newLevel)
        updateAutonomyState()
    }

    /**
     * Evaluate action utility based on context
     */
    private fun calculateUtility(action: Action, context: String): Float {
        // Utility = benefit - cost - risk
        val contextMatch = if (context.contains(action.name, ignoreCase = true)) 1f else 0.5f
        val netUtility = (contextMatch * action.expectedUtility) - action.cost - (action.riskLevel * 0.5f)
        return netUtility.coerceIn(0f, 1f)
    }

    /**
     * Get active goals prioritized
     */
    fun getActivePrioritizedGoals(): List<Goal> {
        return _goals.value
            .filter { it.status == "active" }
            .sortedByDescending { it.priority }
    }

    /**
     * Should operate autonomously
     */
    fun isAutonomous(): Boolean {
        return _autonomyLevel.value > 0.5f
    }

    /**
     * Get autonomy metrics
     */
    fun getAutonomyMetrics(): Map<String, Any> {
        val completedGoals = _goals.value.count { it.status == "completed" }
        val executedCount = _executedActions.value.size
        val avgActionUtility = _executedActions.value
            .mapNotNull { action ->
                _decisions.value.find { it.selectedAction == action.id }?.confidence
            }
            .average()
        
        return mapOf(
            "autonomyLevel" to _autonomyLevel.value,
            "activeGoals" to _goals.value.count { it.status == "active" },
            "completedGoals" to completedGoals,
            "executedActions" to executedCount,
            "averageActionUtility" to avgActionUtility,
            "decisionCount" to decisionHistory.size,
            "isAutonomous" to isAutonomous()
        )
    }

    /**
     * Clear autonomy state
     */
    suspend fun reset() {
        _goals.emit(emptyList())
        _decisions.emit(emptyList())
        _executedActions.emit(emptyList())
        _autonomyLevel.emit(0.5f)
        decisionHistory.clear()
    }

    /**
     * Update overall autonomy state
     */
    private suspend fun updateAutonomyState() {
        val newState = AutonomyState(
            activeGoals = _goals.value.filter { it.status == "active" },
            pendingDecisions = _decisions.value.filter { it.executedAt == null },
            executedActions = _executedActions.value,
            autonomyLevel = _autonomyLevel.value,
            adaptability = calculateAdaptability()
        )
        _autonomyState.emit(newState)
    }

    /**
     * Calculate system adaptability
     */
    private fun calculateAdaptability(): Float {
        val goalChangeFrequency = if (_goals.value.isNotEmpty()) {
            _goals.value.count { it.status == "abandoned" }.toFloat() / _goals.value.size
        } else {
            0.5f
        }
        
        val decisionDiversity = if (decisionHistory.isNotEmpty()) {
            decisionHistory.map { it.context }.distinct().size.toFloat() / decisionHistory.size
        } else {
            0.5f
        }
        
        return ((goalChangeFrequency + decisionDiversity) / 2f).coerceIn(0f, 1f)
    }

    /**
     * Learn from decision outcomes
     */
    suspend fun learnFromOutcome(decisionId: String, outcome: Float) {
        val decision = decisionHistory.find { it.id == decisionId }
        if (decision != null) {
            // Adjust autonomy based on outcome
            val adjustment = when {
                outcome > 0.8f -> 0.05f // Good outcome, increase autonomy
                outcome < 0.3f -> -0.05f // Bad outcome, decrease autonomy
                else -> 0f // Neutral outcome
            }
            
            val newLevel = (_autonomyLevel.value + adjustment).coerceIn(0f, 1f)
            _autonomyLevel.emit(newLevel)
        }
    }
}
