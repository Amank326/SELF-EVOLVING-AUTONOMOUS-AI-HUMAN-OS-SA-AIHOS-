package com.aihos.ai.autonomy
import com.aihos.ai.memory.MemoryManager
import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.reflection.ReflectionEngine
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
/**
 * DefaultActPhase - Executes side effects based on a DecisionResult.
 *
 * This is the ONLY phase that mutates engine state.
 * Each action name maps to a concrete engine operation.
 * If the decision has no selected action, this is a no-op.
 */
class DefaultActPhase(
    private val memoryManager: MemoryManager,
    private val reasoningEngine: ReasoningEngine,
    private val evolutionEngine: EvolutionEngine,
    private val reflectionEngine: ReflectionEngine,
    private val clock: () -> Long = System::currentTimeMillis
) : ActPhase {
    override suspend fun act(decision: DecisionResult): PhaseResult<ActionOutcome?> {
        val start = clock()
        return try {
            val selected = decision.selectedAction
            if (selected == null) {
                val elapsed = clock() - start
                Timber.d("[ACT   #%d] SKIP - no action selected", decision.cycleNumber)
                return PhaseResult.Success(null, elapsed)
            }
            val outcome = executeAction(selected, decision)
            val elapsed = clock() - start
            Timber.d("[ACT   #%d] OK in %dms | action='%s' success=%b",
                decision.cycleNumber, elapsed, selected.action.name, outcome.success)
            PhaseResult.Success(outcome, elapsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val elapsed = clock() - start
            Timber.e(t, "[ACT   #%d] FAILED in %dms", decision.cycleNumber, elapsed)
            PhaseResult.Failure("ACT", t, elapsed)
        }
    }
    private suspend fun executeAction(
        selected: EvaluatedAction,
        decision: DecisionResult
    ): ActionOutcome {
        val actionName = selected.action.name
        val description: String
        when (actionName) {
            "ConsolidateMemory" -> {
                memoryManager.consolidateMemories()
                description = "Triggered memory consolidation"
            }
            "BoostReasoning" -> {
                reasoningEngine.addBelief(
                    "System cycle ${decision.cycleNumber} boosting confidence",
                    0.7f
                )
                reasoningEngine.propagateConfidence()
                description = "Added reasoning evidence and propagated"
            }
            "TriggerEvolution" -> {
                evolutionEngine.evolve()
                description = "Ran one evolution generation"
            }
            "PursueGoal" -> {
                reflectionEngine.reflect(
                    decision.senseSnapshot.reasoningConfidence,
                    decision.senseSnapshot.memoryLoad,
                    decision.autonomyLevel,
                    decision.senseSnapshot.evolutionRate
                )
                description = "Reflected on goal progress"
            }
            else -> {
                description = "Idle monitoring cycle"
            }
        }
        return ActionOutcome(
            decisionId = decision.id,
            actionId = selected.action.id,
            executedAt = clock(),
            success = true,
            effectDescription = description
        )
    }
}