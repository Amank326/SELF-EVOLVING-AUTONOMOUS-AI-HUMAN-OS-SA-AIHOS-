package com.aihos.data.db.daos

import androidx.room.*
import com.aihos.data.db.entity.AutomationTaskEntity
import com.aihos.data.db.entity.AgentTaskEntity

/**
 * DAO for Smart Automation tasks
 */
@Dao
interface AutomationTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: AutomationTaskEntity): Long

    @Update
    suspend fun update(task: AutomationTaskEntity)

    @Delete
    suspend fun delete(task: AutomationTaskEntity)

    @Query("SELECT * FROM automation_tasks ORDER BY priority ASC, createdAt DESC")
    suspend fun getAll(): List<AutomationTaskEntity>

    @Query("SELECT * FROM automation_tasks WHERE id = :id")
    suspend fun getById(id: String): AutomationTaskEntity?

    @Query("SELECT * FROM automation_tasks WHERE isEnabled = 1 ORDER BY priority ASC")
    suspend fun getEnabled(): List<AutomationTaskEntity>

    @Query("SELECT * FROM automation_tasks WHERE status = :status ORDER BY priority ASC")
    suspend fun getByStatus(status: String): List<AutomationTaskEntity>

    @Query("SELECT * FROM automation_tasks WHERE type = :type ORDER BY priority ASC")
    suspend fun getByType(type: String): List<AutomationTaskEntity>

    @Query("SELECT * FROM automation_tasks WHERE category = :category ORDER BY priority ASC")
    suspend fun getByCategory(category: String): List<AutomationTaskEntity>

    @Query("UPDATE automation_tasks SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE automation_tasks SET isEnabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE automation_tasks SET executionCount = executionCount + 1, lastRunAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun incrementExecution(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE automation_tasks SET successCount = successCount + 1, lastResult = :result, status = 'completed', updatedAt = :now WHERE id = :id")
    suspend fun recordSuccess(id: String, result: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE automation_tasks SET failureCount = failureCount + 1, lastResult = :error, status = 'failed', updatedAt = :now WHERE id = :id")
    suspend fun recordFailure(id: String, error: String?, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM automation_tasks")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM automation_tasks WHERE status = 'running'")
    suspend fun getRunningCount(): Int

    @Query("SELECT COUNT(*) FROM automation_tasks WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int

    @Query("DELETE FROM automation_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM automation_tasks")
    suspend fun deleteAll()
}

/**
 * DAO for Agent Tasks (multitasking agent subtasks)
 */
@Dao
interface AgentTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: AgentTaskEntity): Long

    @Update
    suspend fun update(task: AgentTaskEntity)

    @Delete
    suspend fun delete(task: AgentTaskEntity)

    @Query("SELECT * FROM agent_tasks ORDER BY priority ASC, createdAt DESC")
    suspend fun getAll(): List<AgentTaskEntity>

    @Query("SELECT * FROM agent_tasks WHERE id = :id")
    suspend fun getById(id: String): AgentTaskEntity?

    @Query("SELECT * FROM agent_tasks WHERE parentAutomationId = :automationId ORDER BY createdAt ASC")
    suspend fun getByAutomation(automationId: String): List<AgentTaskEntity>

    @Query("SELECT * FROM agent_tasks WHERE status = :status ORDER BY priority ASC, createdAt ASC")
    suspend fun getByStatus(status: String): List<AgentTaskEntity>

    @Query("SELECT * FROM agent_tasks WHERE agentType = :type ORDER BY createdAt DESC")
    suspend fun getByAgentType(type: String): List<AgentTaskEntity>

    @Query("SELECT * FROM agent_tasks WHERE status IN ('queued', 'running') ORDER BY priority ASC, createdAt ASC")
    suspend fun getActive(): List<AgentTaskEntity>

    @Query("UPDATE agent_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE agent_tasks SET status = 'running', startedAt = :now WHERE id = :id")
    suspend fun markRunning(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE agent_tasks SET status = 'completed', result = :result, completedAt = :now, executionTimeMs = :durationMs WHERE id = :id")
    suspend fun markCompleted(id: String, result: String?, now: Long = System.currentTimeMillis(), durationMs: Long = 0)

    @Query("UPDATE agent_tasks SET status = 'failed', errorMessage = :error, completedAt = :now WHERE id = :id")
    suspend fun markFailed(id: String, error: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE agent_tasks SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)

    @Query("SELECT COUNT(*) FROM agent_tasks WHERE status = 'running'")
    suspend fun getRunningCount(): Int

    @Query("SELECT COUNT(*) FROM agent_tasks WHERE status = 'completed'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM agent_tasks WHERE status = 'queued'")
    suspend fun getQueuedCount(): Int

    @Query("SELECT AVG(executionTimeMs) FROM agent_tasks WHERE status = 'completed' AND executionTimeMs > 0")
    suspend fun getAverageExecutionTime(): Float?

    @Query("DELETE FROM agent_tasks WHERE status IN ('completed', 'failed', 'cancelled') AND completedAt < :before")
    suspend fun cleanupOld(before: Long)

    @Query("DELETE FROM agent_tasks")
    suspend fun deleteAll()
}

