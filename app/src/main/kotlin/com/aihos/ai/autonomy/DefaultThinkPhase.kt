package com.aihos.ai.autonomy
import timber.log.Timber
import java.util.UUID
/**
 * DefaultThinkPhase - Pure, deterministic decision function.
 *
 * DETERMINISM GUARANTEE:
 *   Given the same (snapshot, goals, autonomyLevel) triple, this function
 *   will ALWAYS return the same DecisionResult.  No randomness, no clock
 *   reads, no external state.  The only "randomness" source (UUID) is
 *   injected via idGenerator so tests can pin it.
 *
 * Scoring formula per action:
 *   contextMatch = 1.0 if goal keywords match action name, else 0.5
 *   utility = (contextMatch * baseUtility) - cost - (risk * 0.5)
 *   clamped to [0, 1]
 *
 * Autonomy adjustment:
 *   newLevel = (confidence * (1 - complexity)) * 0.8 + oldLevel * 0.2
 */
class DefaultThinkPhase(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> Long = System::currentTimeMillis
) : ThinkPhase {
    override fun think(
        snapshot: SenseSnapshot,
        activeGoals: List<Goal>,
        currentAutonomyLevel: Float
    ): PhaseResult<DecisionResult> {
        val start = System.nanoTime()
        return try {
            val candidates = generateCandidates(snapshot, activeGoals)
            val context = activeGoals.joinToString(",") { it.description }
            val evaluated = candidates.map { action ->
                val contextMatch = if (activeGoals.any { goal ->
                        goal.description.contains(action.name, ignoreCase = true)
                    }) 1f else 0.5f
                val utility = ((contextMatch * action.baseUtility) - action.cost - (action.riskLevel * 0.5f))
                    .coerceIn(0f, 1f)
                EvaluatedAction(action, utility, contextMatch)
            }.sortedByDescending { it.computedUtility }
            val selected = evaluated.firstOrNull()?.takeIf { it.computedUtility > 0.05f }
            val newAutonomy = (
                (snapshot.reasoningConfidence * (1f - snapshot.reasoningComplexity)) * 0.8f +
                currentAutonomyLevel * 0.2f
            ).coerceIn(0f, 1f)
            val reasoning = buildString {
                append("Cycle ${snapshot.cycleNumber}: ")
                append("${candidates.size} candidates, ")
                if (selected != null) {
                    append("selected '${selected.action.name}' ")
                    append("utility=${String.format("%.3f", selected.computedUtility)} ")
                } else {
                    append("no action above threshold. ")
                }
                append("autonomy ${String.format("%.2f", currentAutonomyLevel)}")
                append("->${String.format("%.2f", newAutonomy)}")
            }
            val result = DecisionResult(
                id = idGenerator(),
                cycleNumber = snapshot.cycleNumber,
                timestamp = timestampProvider(),
                senseSnapshot = snapshot,
                evaluatedActions = evaluated,
                selectedAction = selected,
                reasoning = reasoning,
                confidence = selected?.computedUtility ?: 0f,
                autonomyLevel = newAutonomy
            )
            val elapsed = (System.nanoTime() - start) / 1_000_000
            Timber.d("[THINK #%d] OK in %dms | %s",
                snapshot.cycleNumber, elapsed,
                if (selected != null) "action='${selected.action.name}'" else "NO_ACTION")
            PhaseResult.Success(result, elapsed)
        } catch (t: Throwable) {
            val elapsed = (System.nanoTime() - start) / 1_000_000
            Timber.e(t, "[THINK #%d] FAILED in %dms", snapshot.cycleNumber, elapsed)
            PhaseResult.Failure("THINK", t, elapsed)
        }
    }
    /**
     * Generate candidate actions from current system state and goals.
     * Deterministic: same inputs -> same candidates.
     */
    private fun generateCandidates(
        snapshot: SenseSnapshot,
        goals: List<Goal>
    ): List<CandidateAction> {
        val candidates = mutableListOf<CandidateAction>()
        if (snapshot.memoryLoad > 0.7f) {
            candidates.add(CandidateAction(
                id = "act-consolidate-memory",
                name = "ConsolidateMemory",
                description = "Memory load high (${String.format("%.0f", snapshot.memoryLoad * 100)}%), trigger consolidation",
                baseUtility = 0.8f,
                cost = 0.1f,
                riskLevel = 0.05f
            ))
        }
        if (snapshot.reasoningConfidence < 0.4f) {
            candidates.add(CandidateAction(
                id = "act-boost-reasoning",
                name = "BoostReasoning",
                description = "Low confidence (${String.format("%.0f", snapshot.reasoningConfidence * 100)}%), add evidence",
                baseUtility = 0.7f,
                cost = 0.15f,
                riskLevel = 0.1f
            ))
        }
        if (snapshot.evolutionRate < 0.3f) {
            candidates.add(CandidateAction(
                id = "act-evolve",
                name = "TriggerEvolution",
                description = "Evolution stagnant, run generation",
                baseUtility = 0.6f,
                cost = 0.2f,
                riskLevel = 0.15f
            ))
        }
        for (goal in goals.filter { it.status == GoalStatus.ACTIVE }) {
            candidates.add(CandidateAction(
                id = "act-goal-${goal.id.take(8)}",
                name = "PursueGoal",
                description = "Work toward: ${goal.description}",
                baseUtility = goal.priority * 0.9f,
                cost = 0.1f,
                riskLevel = 0.1f
            ))
        }
        if (candidates.isEmpty()) {
            candidates.add(CandidateAction(
                id = "act-idle",
                name = "IdleMonitor",
                description = "System nominal, continue monitoring",
                baseUtility = 0.3f,
                cost = 0.02f,
                riskLevel = 0.01f
            ))
        }
        return candidates
    }
}