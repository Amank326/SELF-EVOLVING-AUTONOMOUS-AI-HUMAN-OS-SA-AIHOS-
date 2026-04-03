package com.aihos.ai.autonomy
/**
 * SA-AIHOS Autonomy Models - Pure Domain Layer
 *
 * Every type here is immutable, has no Android imports,
 * no Room annotations, and no coroutine constructs.
 * Same input -> same output, always.
 *
 * THREADING MODEL:
 *
 *  UI Thread (Main)            IO Dispatcher          Default Dispatcher
 *  +------------------+        +--------------+       +-----------------+
 *  |  ViewModel       |--flow->|  Room DAO    |       |  DecisionLoop   |
 *  |  observes        |        |  (LOG phase) |       |  SENSE -> THINK |
 *  |  StateFlow       |        +--------------+       |  -> ACT -> LOG  |
 *  +--------+---------+              ^                +---------+-------+
 *           |                        |                          |
 *           |        +---------------+--------------------------+
 *           |        | withContext(IO) for DB writes
 *           v        v
 *     +-------------------+
 *     | AutonomyUiState   |  (immutable snapshot via StateFlow)
 *     +-------------------+
 */
data class SenseSnapshot(
    val timestamp: Long,
    val cycleNumber: Long,
    val memoryLoad: Float,
    val reasoningConfidence: Float,
    val reasoningComplexity: Float,
    val evolutionRate: Float,
    val selfAwareness: Float,
    val activeGoalCount: Int,
    val executedActionCount: Int,
    val systemHealthEstimate: Float
) {
    init {
        require(memoryLoad in 0f..1f) { "memoryLoad=$memoryLoad not in [0,1]" }
        require(reasoningConfidence in 0f..1f) { "reasoningConfidence not in [0,1]" }
        require(reasoningComplexity in 0f..1f) { "reasoningComplexity not in [0,1]" }
        require(evolutionRate in 0f..1f) { "evolutionRate not in [0,1]" }
        require(selfAwareness in 0f..1f) { "selfAwareness not in [0,1]" }
        require(systemHealthEstimate in 0f..1f) { "systemHealthEstimate not in [0,1]" }
    }
}
enum class GoalStatus { ACTIVE, COMPLETED, ABANDONED }
data class Goal(
    val id: String,
    val description: String,
    val priority: Float,
    val deadline: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progress: Float = 0f
) {
    init {
        require(priority in 0f..1f)
        require(progress in 0f..1f)
    }
}
data class CandidateAction(
    val id: String,
    val name: String,
    val description: String,
    val prerequisites: List<String> = emptyList(),
    val baseUtility: Float = 0.5f,
    val cost: Float = 0.1f,
    val riskLevel: Float = 0.2f
) {
    init {
        require(baseUtility in 0f..1f)
        require(cost in 0f..1f)
        require(riskLevel in 0f..1f)
    }
}
data class EvaluatedAction(
    val action: CandidateAction,
    val computedUtility: Float,
    val contextMatchScore: Float
)
data class DecisionResult(
    val id: String,
    val cycleNumber: Long,
    val timestamp: Long,
    val senseSnapshot: SenseSnapshot,
    val evaluatedActions: List<EvaluatedAction>,
    val selectedAction: EvaluatedAction?,
    val reasoning: String,
    val confidence: Float,
    val autonomyLevel: Float
)
data class ActionOutcome(
    val decisionId: String,
    val actionId: String,
    val executedAt: Long,
    val success: Boolean,
    val effectDescription: String
)
sealed class PhaseResult<out T> {
    data class Success<T>(val data: T, val durationMs: Long) : PhaseResult<T>()
    data class Failure(val phase: String, val error: Throwable, val durationMs: Long) : PhaseResult<Nothing>()
    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}
data class CycleLog(
    val cycleNumber: Long,
    val startedAt: Long,
    val senseResult: PhaseResult<SenseSnapshot>,
    val thinkResult: PhaseResult<DecisionResult>,
    val actResult: PhaseResult<ActionOutcome?>,
    val persistResult: PhaseResult<Unit>,
    val completedAt: Long,
    val totalDurationMs: Long
)
data class AutonomyUiState(
    val isRunning: Boolean = false,
    val cycleCount: Long = 0,
    val autonomyLevel: Float = 0.5f,
    val activeGoals: List<Goal> = emptyList(),
    val lastDecision: DecisionResult? = null,
    val lastCycleLog: CycleLog? = null,
    val consecutiveFailures: Int = 0
)
