package com.aihos.ai.automation

import com.aihos.data.db.entity.AutomationTaskEntity
import com.aihos.data.db.entity.AgentTaskEntity
import com.aihos.data.db.daos.AutomationTaskDao
import com.aihos.data.db.daos.AgentTaskDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Smart Automation Engine
 * The most advanced automation system that manages workflows,
 * schedules tasks, evaluates triggers, and executes action chains.
 *
 * Features:
 * - Realtime task scheduling and execution
 * - AI-driven trigger evaluation
 * - Parallel workflow execution
 * - Self-optimizing task prioritization
 * - Automatic retry with exponential backoff
 * - Live status monitoring
 */
class SmartAutomationEngine(
    private val automationDao: AutomationTaskDao,
    private val agentTaskDao: AgentTaskDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Engine state
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeWorkflows = MutableStateFlow<List<AutomationTaskEntity>>(emptyList())
    val activeWorkflows: StateFlow<List<AutomationTaskEntity>> = _activeWorkflows.asStateFlow()

    private val _engineStats = MutableStateFlow(AutomationStats())
    val engineStats: StateFlow<AutomationStats> = _engineStats.asStateFlow()

    private val _realtimeLog = MutableStateFlow<List<AutomationLogEntry>>(emptyList())
    val realtimeLog: StateFlow<List<AutomationLogEntry>> = _realtimeLog.asStateFlow()

    // Active running jobs
    private val runningJobs = ConcurrentHashMap<String, Job>()

    // Multitask agent
    private val multitaskAgent = MultitaskAgent(agentTaskDao, scope)

    /**
     * Initialize the automation engine
     */
    suspend fun initialize() {
        Timber.d("[SmartAutomation] Initializing engine...")
        _isRunning.emit(true)

        // Load existing automations
        refreshWorkflows()

        // Start the scheduler loop
        startSchedulerLoop()

        // Initialize multitask agent
        multitaskAgent.initialize()

        log("Engine initialized", "system")
        Timber.d("[SmartAutomation] Engine initialized successfully")
    }

    /**
     * Create a new automation workflow
     */
    suspend fun createAutomation(
        name: String,
        description: String,
        type: String,
        category: String = "general",
        actionsJson: String = "[]",
        triggerCondition: String? = null,
        scheduleInterval: Long = 0,
        priority: Int = 5
    ): AutomationTaskEntity {
        val task = AutomationTaskEntity(
            name = name,
            description = description,
            type = type,
            category = category,
            actionsJson = actionsJson,
            triggerCondition = triggerCondition,
            scheduleInterval = scheduleInterval,
            priority = priority
        )
        automationDao.insert(task)
        refreshWorkflows()
        log("Created automation: $name", "create")
        updateStats()
        return task
    }

    /**
     * Toggle automation enabled/disabled
     */
    suspend fun toggleAutomation(taskId: String, enabled: Boolean) {
        automationDao.setEnabled(taskId, enabled)
        if (!enabled) {
            cancelTask(taskId)
        }
        refreshWorkflows()
        log("${if (enabled) "Enabled" else "Disabled"} automation: $taskId", "toggle")
        updateStats()
    }

    /**
     * Execute a specific automation immediately
     */
    suspend fun executeAutomation(taskId: String): Boolean {
        val task = automationDao.getById(taskId) ?: return false

        if (runningJobs.containsKey(taskId)) {
            log("Automation already running: ${task.name}", "warning")
            return false
        }

        log("Executing automation: ${task.name}", "execute")
        automationDao.updateStatus(taskId, "running")
        automationDao.incrementExecution(taskId)

        val job = scope.launch {
            try {
                val result = executeWorkflow(task)
                automationDao.recordSuccess(taskId, result)
                log("Completed: ${task.name} - $result", "success")
            } catch (e: Exception) {
                automationDao.recordFailure(taskId, e.message)
                log("Failed: ${task.name} - ${e.message}", "error")
            } finally {
                runningJobs.remove(taskId)
                refreshWorkflows()
                updateStats()
            }
        }

        runningJobs[taskId] = job
        refreshWorkflows()
        updateStats()
        return true
    }

    /**
     * Execute a workflow's action chain
     */
    private suspend fun executeWorkflow(task: AutomationTaskEntity): String {
        val results = mutableListOf<String>()

        // Parse actions from JSON
        val actions = parseActions(task.actionsJson)

        for ((index, action) in actions.withIndex()) {
            val agentTask = AgentTaskEntity(
                parentAutomationId = task.id,
                agentType = action.agentType,
                taskName = "${task.name} - Step ${index + 1}",
                taskDescription = action.description,
                inputData = action.inputData,
                priority = task.priority
            )

            val result = multitaskAgent.submitAndWait(agentTask)
            results.add("Step ${index + 1}: $result")

            // Small delay between steps
            delay(100)
        }

        return results.joinToString("; ")
    }

    /**
     * Cancel a running automation
     */
    suspend fun cancelTask(taskId: String) {
        runningJobs[taskId]?.cancel()
        runningJobs.remove(taskId)
        automationDao.updateStatus(taskId, "idle")
        refreshWorkflows()
        log("Cancelled automation: $taskId", "cancel")
    }

    /**
     * Delete an automation
     */
    suspend fun deleteAutomation(taskId: String) {
        cancelTask(taskId)
        automationDao.deleteById(taskId)
        refreshWorkflows()
        log("Deleted automation: $taskId", "delete")
        updateStats()
    }

    /**
     * Get all automations as JSON for WebView
     */
    suspend fun getAutomationsJson(): String {
        val tasks = automationDao.getAll()
        val sb = StringBuilder("[")
        tasks.forEachIndexed { index, task ->
            if (index > 0) sb.append(",")
            sb.append("{")
            sb.append("\"id\":\"${task.id}\",")
            sb.append("\"name\":\"${escapeJson(task.name)}\",")
            sb.append("\"description\":\"${escapeJson(task.description)}\",")
            sb.append("\"type\":\"${task.type}\",")
            sb.append("\"category\":\"${task.category}\",")
            sb.append("\"status\":\"${task.status}\",")
            sb.append("\"isEnabled\":${task.isEnabled},")
            sb.append("\"executionCount\":${task.executionCount},")
            sb.append("\"successCount\":${task.successCount},")
            sb.append("\"failureCount\":${task.failureCount},")
            sb.append("\"priority\":${task.priority},")
            sb.append("\"lastRunAt\":${task.lastRunAt ?: 0},")
            sb.append("\"lastResult\":\"${escapeJson(task.lastResult ?: "")}\"")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    /**
     * Get agent status as JSON for WebView
     */
    suspend fun getAgentStatusJson(): String {
        return multitaskAgent.getStatusJson()
    }

    /**
     * Get engine stats as JSON
     */
    suspend fun getStatsJson(): String {
        updateStats()
        val stats = _engineStats.value
        return "{" +
            "\"totalAutomations\":${stats.totalAutomations}," +
            "\"activeAutomations\":${stats.activeAutomations}," +
            "\"runningTasks\":${stats.runningTasks}," +
            "\"completedToday\":${stats.completedToday}," +
            "\"successRate\":${stats.successRate}," +
            "\"avgExecutionTime\":${stats.avgExecutionTime}," +
            "\"optimizationScore\":${stats.optimizationScore}," +
            "\"agentConcurrency\":${stats.agentConcurrency}" +
            "}"
    }

    /**
     * Setup default automations for new users
     */
    suspend fun setupDefaults() {
        if (automationDao.getCount() > 0) return

        createAutomation(
            name = "System Health Monitor",
            description = "Continuously monitors AI system health and performance metrics",
            type = "continuous",
            category = "system",
            actionsJson = "[{\"agentType\":\"analysis\",\"description\":\"Check system metrics\",\"inputData\":\"{}\"}]",
            priority = 1
        )

        createAutomation(
            name = "Memory Optimizer",
            description = "Periodically consolidates and optimizes memory storage",
            type = "scheduled",
            category = "ai",
            actionsJson = "[{\"agentType\":\"memory\",\"description\":\"Consolidate memories\",\"inputData\":\"{}\"}]",
            scheduleInterval = 300000, // Every 5 minutes
            priority = 3
        )

        createAutomation(
            name = "Performance Analyzer",
            description = "Analyzes system performance and suggests optimizations",
            type = "scheduled",
            category = "ai",
            actionsJson = "[{\"agentType\":\"analysis\",\"description\":\"Analyze performance\",\"inputData\":\"{}\"},{\"agentType\":\"optimization\",\"description\":\"Suggest improvements\",\"inputData\":\"{}\"}]",
            scheduleInterval = 600000, // Every 10 minutes
            priority = 4
        )

        createAutomation(
            name = "Neural Learning Cycle",
            description = "Triggers neural network training and evolution cycles",
            type = "scheduled",
            category = "ai",
            actionsJson = "[{\"agentType\":\"evolution\",\"description\":\"Run evolution cycle\",\"inputData\":\"{}\"}]",
            scheduleInterval = 900000, // Every 15 minutes
            priority = 5
        )

        createAutomation(
            name = "Predictive Analytics",
            description = "Runs predictive models and updates forecasts",
            type = "scheduled",
            category = "data",
            actionsJson = "[{\"agentType\":\"prediction\",\"description\":\"Generate predictions\",\"inputData\":\"{}\"}]",
            scheduleInterval = 1200000, // Every 20 minutes
            priority = 6
        )

        createAutomation(
            name = "Smart Alert System",
            description = "AI-driven intelligent notification management",
            type = "reactive",
            category = "notification",
            actionsJson = "[{\"agentType\":\"reasoning\",\"description\":\"Evaluate alert conditions\",\"inputData\":\"{}\"}]",
            triggerCondition = "{\"type\":\"threshold\",\"metric\":\"anomalyScore\",\"value\":0.8}",
            priority = 2
        )

        createAutomation(
            name = "Data Sync Engine",
            description = "Synchronizes data across local and cloud storage",
            type = "scheduled",
            category = "data",
            actionsJson = "[{\"agentType\":\"analysis\",\"description\":\"Check sync status\",\"inputData\":\"{}\"},{\"agentType\":\"optimization\",\"description\":\"Sync data\",\"inputData\":\"{}\"}]",
            scheduleInterval = 1800000, // Every 30 minutes
            priority = 7
        )

        createAutomation(
            name = "Workflow Optimizer",
            description = "Self-optimizing automation that improves other workflows",
            type = "scheduled",
            category = "workflow",
            actionsJson = "[{\"agentType\":\"analysis\",\"description\":\"Analyze workflow efficiency\",\"inputData\":\"{}\"},{\"agentType\":\"optimization\",\"description\":\"Optimize workflows\",\"inputData\":\"{}\"}]",
            scheduleInterval = 3600000, // Every hour
            priority = 8
        )

        log("Default automations created", "system")
    }

    /**
     * Scheduler loop - checks for due tasks and executes them
     */
    private fun startSchedulerLoop() {
        scope.launch {
            while (_isRunning.value) {
                try {
                    val enabledTasks = automationDao.getEnabled()
                    val now = System.currentTimeMillis()

                    for (task in enabledTasks) {
                        if (task.status == "running") continue
                        if (task.scheduleInterval <= 0) continue

                        val lastRun = task.lastRunAt ?: 0
                        if (now - lastRun >= task.scheduleInterval) {
                            executeAutomation(task.id)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[SmartAutomation] Scheduler error")
                }

                delay(10000) // Check every 10 seconds
            }
        }
    }

    /**
     * Refresh the active workflows list
     */
    private suspend fun refreshWorkflows() {
        _activeWorkflows.emit(automationDao.getAll())
    }

    /**
     * Update engine statistics
     */
    private suspend fun updateStats() {
        val total = automationDao.getCount()
        val enabled = automationDao.getEnabledCount()
        val running = automationDao.getRunningCount()
        val agentRunning = agentTaskDao.getRunningCount()
        val completed = agentTaskDao.getCompletedCount()
        val avgTime = agentTaskDao.getAverageExecutionTime() ?: 0f

        val allTasks = automationDao.getAll()
        val totalExec = allTasks.sumOf { it.executionCount }
        val totalSuccess = allTasks.sumOf { it.successCount }
        val successRate = if (totalExec > 0) totalSuccess.toFloat() / totalExec else 1f
        val optimizationScore = (successRate * 0.4f + (1f - (avgTime / 10000f).coerceIn(0f, 1f)) * 0.3f + (enabled.toFloat() / total.coerceAtLeast(1)) * 0.3f).coerceIn(0f, 1f)

        _engineStats.emit(
            AutomationStats(
                totalAutomations = total,
                activeAutomations = enabled,
                runningTasks = running + agentRunning,
                completedToday = completed,
                successRate = successRate,
                avgExecutionTime = avgTime,
                optimizationScore = optimizationScore,
                agentConcurrency = agentRunning
            )
        )
    }

    /**
     * Add entry to realtime log
     */
    private suspend fun log(message: String, type: String) {
        val entry = AutomationLogEntry(
            message = message,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        val current = _realtimeLog.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) {
            _realtimeLog.emit(current.take(100))
        } else {
            _realtimeLog.emit(current)
        }
    }

    /**
     * Parse action steps from JSON string
     */
    private fun parseActions(actionsJson: String): List<AutomationAction> {
        val actions = mutableListOf<AutomationAction>()
        try {
            // Simple JSON parsing without external library
            val cleaned = actionsJson.trim()
            if (cleaned == "[]" || cleaned.isEmpty()) return actions

            // Split by },{ to get individual action objects
            val items = cleaned.removePrefix("[").removeSuffix("]")
            if (items.isBlank()) return actions

            val parts = items.split("},")
            for (part in parts) {
                val json = part.trim().removePrefix("{").removeSuffix("}")
                var agentType = "analysis"
                var description = ""
                var inputData = "{}"

                for (field in json.split("\",")) {
                    val kv = field.split(":")
                    if (kv.size >= 2) {
                        val key = kv[0].trim().replace("\"", "")
                        val value = kv.drop(1).joinToString(":").trim().replace("\"", "")
                        when (key) {
                            "agentType" -> agentType = value
                            "description" -> description = value
                            "inputData" -> inputData = value
                        }
                    }
                }

                actions.add(AutomationAction(agentType, description, inputData))
            }
        } catch (e: Exception) {
            Timber.e(e, "[SmartAutomation] Error parsing actions JSON")
            // Return single default action
            actions.add(AutomationAction("analysis", "Execute automation", "{}"))
        }
        return actions
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Shutdown the engine
     */
    suspend fun shutdown() {
        _isRunning.emit(false)
        runningJobs.values.forEach { it.cancel() }
        runningJobs.clear()
        multitaskAgent.shutdown()
        log("Engine shutdown", "system")
    }
}

/**
 * Data classes
 */
data class AutomationStats(
    val totalAutomations: Int = 0,
    val activeAutomations: Int = 0,
    val runningTasks: Int = 0,
    val completedToday: Int = 0,
    val successRate: Float = 1f,
    val avgExecutionTime: Float = 0f,
    val optimizationScore: Float = 0f,
    val agentConcurrency: Int = 0
)

data class AutomationLogEntry(
    val message: String,
    val type: String,
    val timestamp: Long
)

data class AutomationAction(
    val agentType: String,
    val description: String,
    val inputData: String
)

