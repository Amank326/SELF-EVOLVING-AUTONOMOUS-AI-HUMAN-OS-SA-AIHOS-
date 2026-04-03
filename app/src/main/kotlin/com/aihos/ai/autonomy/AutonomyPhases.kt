package com.aihos.ai.autonomy
/**
 * Phase Contracts - Each phase of SENSE->THINK->ACT->LOG is an interface.
 *
 * Why interfaces?
 *   - Unit tests inject fakes
 *   - Each phase is replaceable independently
 *   - No phase can access another phase's internals
 *   - Exception isolation: each impl catches its own errors
 *
 * EDGE CASE STRATEGY:
 * ------------------------------------------------------------------
 * | Scenario                  | Handling                            |
 * |---------------------------|-------------------------------------|
 * | SENSE fails (engine dead) | PhaseResult.Failure, skip cycle     |
 * | THINK gets empty actions  | DecisionResult with null selected   |
 * | ACT fails (side-effect)   | Log failure, do NOT retry (no redo) |
 * | LOG fails (DB down)       | Timber.e, cycle still counts        |
 * | Cancellation mid-phase    | CancellationException propagates   |
 * | Loop restart after cancel | Fresh Job, cycle counter preserved  |
 * | OOM during THINK          | Caught as Throwable in phase guard  |
 * ------------------------------------------------------------------
 */
/**
 * SENSE - Read the world. Pure observation, zero side effects.
 * Reads StateFlow.value from all engines and freezes them into a SenseSnapshot.
 */
interface SensePhase {
    suspend fun sense(cycleNumber: Long): PhaseResult<SenseSnapshot>
}
/**
 * THINK - Pure decision function.
 * Takes a snapshot + goals + previous autonomy level.
 * Returns a DecisionResult. MUST be deterministic:
 *   same (snapshot, goals, autonomyLevel) -> same DecisionResult
 *
 * This is the core contract that makes the system testable.
 * No database access. No network. No side effects.
 */
interface ThinkPhase {
    fun think(
        snapshot: SenseSnapshot,
        activeGoals: List<Goal>,
        currentAutonomyLevel: Float
    ): PhaseResult<DecisionResult>
}
/**
 * ACT - Execute the chosen action. This is the ONLY phase with side effects.
 * Adjusts engine parameters (autonomy level, goal progress, etc.)
 * Returns an ActionOutcome describing what happened.
 */
interface ActPhase {
    suspend fun act(decision: DecisionResult): PhaseResult<ActionOutcome?>
}
/**
 * LOG - Persist the full cycle trace to Room.
 * Runs on Dispatchers.IO. Failure here does NOT fail the cycle.
 */
interface LogPhase {
    suspend fun log(cycleLog: CycleLog): PhaseResult<Unit>
}