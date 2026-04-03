package com.aihos.ai.recovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * SELF-HEALING SYSTEM
 * Fault detection, auto-recovery, and system resilience
 * - Health monitoring across all components
 * - Anomaly detection and alerting
 * - Automatic recovery triggers
 * - State rollback and restoration
 */

data class ComponentHealth(
    val componentId: String = "",
    val healthScore: Float = 0.8f,
    val status: String = "healthy",
    val lastCheckTime: Long = System.currentTimeMillis(),
    val failureCount: Int = 0,
    val recoveryAttempts: Int = 0
)

data class HealthReport(
    val timestamp: Long = System.currentTimeMillis(),
    val overallHealth: Float = 0.8f,
    val componentHealths: List<ComponentHealth> = emptyList(),
    val criticalIssues: List<String> = emptyList(),
    val warningIssues: List<String> = emptyList(),
    val recoveryMode: Boolean = false
)

class SelfHealingSystem {
    private val componentHealths = mutableMapOf<String, ComponentHealth>()
    private val healthHistory = mutableListOf<HealthReport>()
    private val stateSnapshots = mutableListOf<Map<String, Any>>()

    private val _healthReport = MutableStateFlow(HealthReport())
    val healthReport: StateFlow<HealthReport> = _healthReport

    private var lastRecoveryTime = 0L
    private var recoveryInProgress = false

    init {
        initializeComponents()
    }

    /**
     * Update component health status
     */
    suspend fun updateComponentHealth(
        componentId: String,
        healthScore: Float,
        status: String = "healthy"
    ) {
        val current = componentHealths[componentId] ?: ComponentHealth(componentId = componentId)
        
        val newHealth = current.copy(
            healthScore = healthScore.coerceIn(0f, 1f),
            status = status,
            lastCheckTime = System.currentTimeMillis(),
            failureCount = if (healthScore < 0.5f) current.failureCount + 1 else current.failureCount
        )

        componentHealths[componentId] = newHealth

        // Check if recovery needed
        if (healthScore < 0.3f) {
            triggerRecovery(componentId, newHealth)
        }

        generateHealthReport()
    }

    /**
     * Store state snapshot for rollback
     */
    fun storeStateSnapshot(state: Map<String, Any>) {
        stateSnapshots.add(state)
        if (stateSnapshots.size > 50) {
            stateSnapshots.removeAt(0)
        }
    }

    /**
     * Restore from previous state
     */
    fun restoreFromSnapshot(stepsBack: Int = 1): Map<String, Any>? {
        val targetIdx = stateSnapshots.size - stepsBack
        return if (targetIdx >= 0) stateSnapshots[targetIdx] else null
    }

    /**
     * Perform system diagnostics
     */
    suspend fun runDiagnostics(): Map<String, Any> {
        val results = mutableMapOf<String, Any>()

        componentHealths.forEach { (componentId, health) ->
            results["$componentId.health"] = health.healthScore
            results["$componentId.status"] = health.status
            results["$componentId.failures"] = health.failureCount
        }

        val overallHealth = componentHealths.values.map { it.healthScore }.average().toFloat()
        results["overall_health"] = overallHealth
        results["critical_issues"] = identifyCriticalIssues()
        results["components_degraded"] = componentHealths.values.count { it.healthScore < 0.6f }

        return results
    }

    /**
     * Get health timeline
     */
    fun getHealthTimeline(limit: Int = 20): List<HealthReport> {
        return healthHistory.takeLast(limit)
    }

    /**
     * Check if system is in critical state
     */
    fun isCritical(): Boolean {
        return _healthReport.value.criticalIssues.isNotEmpty()
    }

    /**
     * Reset component health
     */
    suspend fun resetComponent(componentId: String) {
        val current = componentHealths[componentId] ?: return
        val reset = current.copy(
            healthScore = 0.8f,
            status = "recovered",
            failureCount = 0,
            recoveryAttempts = current.recoveryAttempts + 1
        )
        componentHealths[componentId] = reset
        generateHealthReport()
    }

    // ======================== PRIVATE HELPERS ========================

    private fun initializeComponents() {
        val components = listOf(
            "orchestrator",
            "neural_network",
            "pso_optimizer",
            "aco_optimizer",
            "qga_optimizer",
            "memory_system",
            "reasoning_engine",
            "autonomy_controller",
            "analytics_engine"
        )

        components.forEach { componentId ->
            componentHealths[componentId] = ComponentHealth(
                componentId = componentId,
                healthScore = 0.9f,
                status = "healthy"
            )
        }
    }

    private suspend fun triggerRecovery(componentId: String, health: ComponentHealth) {
        if (recoveryInProgress) return
        if (System.currentTimeMillis() - lastRecoveryTime < 5000) return // Rate limit

        recoveryInProgress = true
        lastRecoveryTime = System.currentTimeMillis()

        val updated = health.copy(
            healthScore = 0.5f,
            status = "recovering",
            recoveryAttempts = health.recoveryAttempts + 1
        )
        componentHealths[componentId] = updated

        // Simulate recovery
        kotlinx.coroutines.delay(1000)

        val recovered = updated.copy(
            healthScore = 0.7f,
            status = "recovered"
        )
        componentHealths[componentId] = recovered

        recoveryInProgress = false
        generateHealthReport()
    }

    private fun identifyCriticalIssues(): List<String> {
        val issues = mutableListOf<String>()

        componentHealths.forEach { (componentId, health) ->
            when {
                health.healthScore < 0.2f -> issues.add("$componentId: Critical failure")
                health.healthScore < 0.5f -> issues.add("$componentId: Degraded performance")
                health.failureCount > 5 -> issues.add("$componentId: Repeated failures")
            }
        }

        return issues
    }

    private fun identifyWarningIssues(): List<String> {
        val warnings = mutableListOf<String>()

        componentHealths.forEach { (componentId, health) ->
            when {
                health.healthScore < 0.7f -> warnings.add("$componentId: Below optimal health")
                health.failureCount > 2 -> warnings.add("$componentId: Multiple failures detected")
            }
        }

        return warnings
    }

    private suspend fun generateHealthReport() {
        val overallHealth = componentHealths.values.map { it.healthScore }.average().toFloat()
        val criticalIssues = identifyCriticalIssues()
        val warningIssues = identifyWarningIssues()

        val report = HealthReport(
            timestamp = System.currentTimeMillis(),
            overallHealth = overallHealth,
            componentHealths = componentHealths.values.toList(),
            criticalIssues = criticalIssues,
            warningIssues = warningIssues,
            recoveryMode = recoveryInProgress
        )

        healthHistory.add(report)
        if (healthHistory.size > 100) {
            healthHistory.removeAt(0)
        }

        _healthReport.emit(report)
    }
}
