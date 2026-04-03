package com.aihos.ai.autonomy
import com.aihos.ai.memory.MemoryManager
import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.reflection.ReflectionEngine
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
/**
 * DefaultSensePhase - Reads current state from all AI engines.
 *
 * Contract: read-only. Touches no mutable state.
 * Each engine exposes StateFlow; we read .value which is a thread-safe snapshot.
 */
class DefaultSensePhase(
    private val memoryManager: MemoryManager,
    private val reasoningEngine: ReasoningEngine,
    private val evolutionEngine: EvolutionEngine,
    private val reflectionEngine: ReflectionEngine,
    private val goalProvider: () -> List<Goal>,
    private val actionCountProvider: () -> Int,
    private val clock: () -> Long = System::currentTimeMillis
) : SensePhase {
    override suspend fun sense(cycleNumber: Long): PhaseResult<SenseSnapshot> {
        val start = clock()
        return try {
            val memLoad = memoryManager.memoryLoad.value.coerceIn(0f, 1f)
            val rConf = reasoningEngine.confidence.value.coerceIn(0f, 1f)
            val rComp = reasoningEngine.complexity.value.coerceIn(0f, 1f)
            val evoRate = evolutionEngine.evolutionMetrics.value.evolutionRate.coerceIn(0f, 1f)
            val aware = reflectionEngine.selfAwareness.value.coerceIn(0f, 1f)
            val goals = goalProvider()
            val actionCount = actionCountProvider()
            val healthScores = listOf(
                1f - memLoad,
                rConf,
                evoRate,
                aware
            )
            val health = (healthScores.sum() / healthScores.size).coerceIn(0f, 1f)
            val snapshot = SenseSnapshot(
                timestamp = clock(),
                cycleNumber = cycleNumber,
                memoryLoad = memLoad,
                reasoningConfidence = rConf,
                reasoningComplexity = rComp,
                evolutionRate = evoRate,
                selfAwareness = aware,
                activeGoalCount = goals.count { it.status == GoalStatus.ACTIVE },
                executedActionCount = actionCount,
                systemHealthEstimate = health
            )
            val elapsed = clock() - start
            Timber.d("[SENSE #%d] OK in %dms | health=%.2f conf=%.2f mem=%.2f",
                cycleNumber, elapsed, health, rConf, memLoad)
            PhaseResult.Success(snapshot, elapsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val elapsed = clock() - start
            Timber.e(t, "[SENSE #%d] FAILED in %dms", cycleNumber, elapsed)
            PhaseResult.Failure("SENSE", t, elapsed)
        }
    }
}