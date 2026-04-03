package com.aihos.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Automation Task Entity
 * Represents a smart automation workflow that can run autonomously
 */
@Entity(tableName = "automation_tasks")
data class AutomationTaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "type")
    val type: String, // "scheduled", "triggered", "continuous", "reactive"

    @ColumnInfo(name = "category")
    val category: String = "general", // "system", "data", "ai", "notification", "workflow"

    @ColumnInfo(name = "triggerCondition")
    val triggerCondition: String? = null, // JSON: trigger rules

    @ColumnInfo(name = "actionsJson")
    val actionsJson: String = "[]", // JSON array of action steps

    @ColumnInfo(name = "scheduleInterval")
    val scheduleInterval: Long = 0, // Interval in milliseconds, 0 = one-time

    @ColumnInfo(name = "priority")
    val priority: Int = 5, // 1 (highest) to 10 (lowest)

    @ColumnInfo(name = "status")
    val status: String = "idle", // "idle", "running", "completed", "failed", "paused"

    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "executionCount")
    val executionCount: Int = 0,

    @ColumnInfo(name = "successCount")
    val successCount: Int = 0,

    @ColumnInfo(name = "failureCount")
    val failureCount: Int = 0,

    @ColumnInfo(name = "lastRunAt")
    val lastRunAt: Long? = null,

    @ColumnInfo(name = "nextRunAt")
    val nextRunAt: Long? = null,

    @ColumnInfo(name = "lastResult")
    val lastResult: String? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Agent Task Entity
 * Represents a single task executed by the multitasking agent system
 */
@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "parentAutomationId")
    val parentAutomationId: String? = null,

    @ColumnInfo(name = "agentType")
    val agentType: String, // "reasoning", "memory", "evolution", "analysis", "prediction", "optimization"

    @ColumnInfo(name = "taskName")
    val taskName: String,

    @ColumnInfo(name = "taskDescription")
    val taskDescription: String = "",

    @ColumnInfo(name = "inputData")
    val inputData: String? = null, // JSON input

    @ColumnInfo(name = "status")
    val status: String = "queued", // "queued", "running", "completed", "failed", "cancelled"

    @ColumnInfo(name = "progress")
    val progress: Float = 0f, // 0.0 to 1.0

    @ColumnInfo(name = "result")
    val result: String? = null, // JSON result

    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = null,

    @ColumnInfo(name = "priority")
    val priority: Int = 5,

    @ColumnInfo(name = "startedAt")
    val startedAt: Long? = null,

    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,

    @ColumnInfo(name = "executionTimeMs")
    val executionTimeMs: Long = 0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

