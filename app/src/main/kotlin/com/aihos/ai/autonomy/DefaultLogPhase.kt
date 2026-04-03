package com.aihos.ai.autonomy
import com.aihos.data.db.daos.AutonomousDecisionDao
import com.aihos.data.db.daos.SystemEventLogDao
import com.aihos.data.db.entity.AutonomousDecisionEntity
import com.aihos.data.db.entities.SystemEventLog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
/**
 * DefaultLogPhase - Persists cycle trace to Room database.
 *
 * Runs entirely on Dispatchers.IO.
 * If the database write fails, the cycle is NOT considered failed;
 * we log the error and move on.
 */
class DefaultLogPhase(
    private val decisionDao: AutonomousDecisionDao,
    private val eventLogDao: SystemEventLogDao,
    private val gson: Gson = Gson()
) : LogPhase {
    override suspend fun log(cycleLog: CycleLog): PhaseResult<Unit> {
        val start = System.currentTimeMillis()
        return try {
            withContext(Dispatchers.IO) {
                persistDecision(cycleLog)
                persistEvent(cycleLog)
            }
            val elapsed = System.currentTimeMillis() - start
            Timber.d("[LOG   #%d] OK in %dms | total_cycle=%dms",
                cycleLog.cycleNumber, elapsed, cycleLog.totalDurationMs)
            PhaseResult.Success(Unit, elapsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            val elapsed = System.currentTimeMillis() - start
            Timber.e(t, "[LOG   #%d] FAILED in %dms (non-fatal)", cycleLog.cycleNumber, elapsed)
            PhaseResult.Failure("LOG", t, elapsed)
        }
    }
    private suspend fun persistDecision(cycleLog: CycleLog) {
        val decision = cycleLog.thinkResult.getOrNull() ?: return
        val outcome = cycleLog.actResult.getOrNull()
        val entity = AutonomousDecisionEntity(
            decisionId = decision.id,
            actionType = decision.selectedAction?.action?.name ?: "NONE",
            description = decision.reasoning,
            confidence = decision.confidence,
            executed = outcome != null,
            outcome = outcome?.effectDescription,
            timestamp = decision.timestamp
        )
        decisionDao.insertDecision(entity)
    }
    private suspend fun persistEvent(cycleLog: CycleLog) {
        val severity = when {
            cycleLog.senseResult is PhaseResult.Failure -> "error"
            cycleLog.thinkResult is PhaseResult.Failure -> "error"
            cycleLog.actResult is PhaseResult.Failure -> "warning"
            cycleLog.persistResult is PhaseResult.Failure -> "warning"
            else -> "info"
        }
        val metadata = mapOf(
            "cycleNumber" to cycleLog.cycleNumber,
            "totalDurationMs" to cycleLog.totalDurationMs,
            "senseOk" to cycleLog.senseResult.isSuccess,
            "thinkOk" to cycleLog.thinkResult.isSuccess,
            "actOk" to cycleLog.actResult.isSuccess
        )
        val event = SystemEventLog(
            eventType = "autonomy_cycle",
            eventName = "cycle_${cycleLog.cycleNumber}",
            severity = severity,
            description = cycleLog.thinkResult.getOrNull()?.reasoning,
            metadataJson = gson.toJson(metadata),
            sourceEngine = "AutonomyController",
            relatedCycleCount = cycleLog.cycleNumber
        )
        eventLogDao.insertEvent(event)
    }
}