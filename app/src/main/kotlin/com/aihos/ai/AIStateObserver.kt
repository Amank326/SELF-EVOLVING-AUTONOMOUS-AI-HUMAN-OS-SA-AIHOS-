package com.aihos.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * AI State Observer - Real-time state synchronization for UI
 * Bridges between AICoordinator and Android UI components
 */

data class AIMetricUpdate(
    val timestamp: Long = Instant.now().toEpochMilli(),
    val memoryLoad: Float = 0.5f,
    val reasoningConfidence: Float = 0.5f,
    val autonomyLevel: Float = 0.5f,
    val systemHealth: Float = 0.5f,
    val evolutionGeneration: Int = 0,
    val skillCount: Int = 0,
    val activeGoals: Int = 0,
    val animationIntensity: Float = 0.5f,
    val animationRotation: Float = 0f,
    val predictedBehavior: Float = 0.5f,
    val performanceScore: Float = 0.5f,
    val insights: List<String> = emptyList()
)

data class UIState(
    val isAIRunning: Boolean = false,
    val cycleCount: Int = 0,
    val lastUpdateTime: Long = 0L,
    val metricHistory: List<AIMetricUpdate> = emptyList(),
    val currentMetrics: AIMetricUpdate? = null,
    val systemStatus: String = "Initializing..."
)

/**
 * Observer for AI state changes - pushes updates to UI
 */
class AIStateObserver {
    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState

    private val _metricStream = MutableStateFlow<AIMetricUpdate?>(null)
    val metricStream: StateFlow<AIMetricUpdate?> = _metricStream

    private val _systemStatus = MutableStateFlow("Initializing...")
    val systemStatus: StateFlow<String> = _systemStatus

    private val _dashboardUpdate = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dashboardUpdate: StateFlow<Map<String, Any>> = _dashboardUpdate

    private val _visualizationUpdate = MutableStateFlow<Map<String, Float>>(emptyMap())
    val visualizationUpdate: StateFlow<Map<String, Float>> = _visualizationUpdate

    private var cycleCount = 0
    private val metricHistory = mutableListOf<AIMetricUpdate>()

    /**
     * Update state from AICoordinator broadcast
     */
    suspend fun updateFromAIState(
        aiState: AISystemState?,
        broadcast: AIBroadcast?,
        predictions: Map<String, Float> = emptyMap()
    ) {
        if (aiState == null) return

        cycleCount++

        // Create metric update
        val metricUpdate = AIMetricUpdate(
            timestamp = aiState.timestamp,
            memoryLoad = aiState.memoryLoad,
            reasoningConfidence = aiState.reasoningConfidence,
            autonomyLevel = aiState.autonomyLevel,
            systemHealth = aiState.systemHealth,
            evolutionGeneration = aiState.evolutionGeneration,
            skillCount = aiState.skillCount,
            activeGoals = aiState.activeGoals,
            animationIntensity = aiState.animationIntensity,
            animationRotation = aiState.animationRotation,
            predictedBehavior = predictions["behavior"] ?: 0.5f,
            performanceScore = predictions["performance"] ?: 0.5f,
            insights = broadcast?.insights ?: emptyList()
        )

        // Add to history
        metricHistory.add(metricUpdate)
        if (metricHistory.size > 100) {
            metricHistory.drop(metricHistory.size - 100)
        }

        // Update metric stream
        _metricStream.emit(metricUpdate)

        // Update status
        updateSystemStatus(aiState)

        // Update dashboard metrics
        updateDashboardMetrics(metricUpdate, broadcast)

        // Update visualization controls
        updateVisualizationMetrics(aiState)

        // Update overall UI state
        val newUIState = UIState(
            isAIRunning = aiState.isActive,
            cycleCount = cycleCount,
            lastUpdateTime = aiState.timestamp,
            metricHistory = metricHistory.toList(),
            currentMetrics = metricUpdate,
            systemStatus = _systemStatus.value
        )
        _uiState.emit(newUIState)
    }

    /**
     * Update system status text
     */
    private suspend fun updateSystemStatus(aiState: AISystemState) {
        val status = when {
            aiState.systemHealth > 0.9f -> "🌟 Excellent - System performing optimally"
            aiState.systemHealth > 0.7f -> "✓ Good - System healthy and responsive"
            aiState.systemHealth > 0.5f -> "⚠ Moderate - Some optimization needed"
            aiState.systemHealth > 0.3f -> "⚡ Low - Focus on improvements"
            else -> "🔴 Critical - Immediate optimization required"
        }
        _systemStatus.emit(status)
    }

    /**
     * Update dashboard UI metrics
     */
    private suspend fun updateDashboardMetrics(update: AIMetricUpdate, broadcast: AIBroadcast?) {
        val dashboardData = mapOf(
            "memory" to update.memoryLoad,
            "reasoning" to update.reasoningConfidence,
            "autonomy" to update.autonomyLevel,
            "health" to update.systemHealth,
            "generation" to update.evolutionGeneration,
            "skills" to update.skillCount,
            "goals" to update.activeGoals,
            "predicted" to update.predictedBehavior,
            "performance" to update.performanceScore,
            "timestamp" to update.timestamp,
            "insights" to update.insights,
            "actionRequired" to (broadcast?.actionRequired ?: false)
        ) as Map<String, Any>
        _dashboardUpdate.emit(dashboardData)
    }

    /**
     * Update visualization sliders and controls
     */
    private suspend fun updateVisualizationMetrics(aiState: AISystemState) {
        val vizData = mapOf(
            "animationIntensity" to aiState.animationIntensity,
            "animationRotation" to aiState.animationRotation,
            "animationOscillation" to aiState.animationOscillation,
            "bloomIntensity" to (aiState.systemHealth * 0.5f + aiState.reasoningConfidence * 0.5f),
            "particleCount" to ((aiState.autonomyLevel + aiState.memoryLoad) * 50f).toInt() / 100f,
            "colorIntensity" to aiState.systemHealth,
            "cameraRotation" to ((aiState.evolutionGeneration % 36) * 10f),
            "effectsIntensity" to aiState.reasoningComplexity
        )
        _visualizationUpdate.emit(vizData)
    }

    /**
     * Get metric trend (improvement or decline)
     */
    fun getMetricTrend(metric: String, window: Int = 10): Float {
        if (metricHistory.size < 2) return 0f

        val recent = metricHistory.takeLast(window)
        if (recent.size < 2) return 0f

        val first = getMetricValue(recent.first(), metric)
        val last = getMetricValue(recent.last(), metric)

        return last - first
    }

    /**
     * Get metric statistics
     */
    fun getMetricStats(metric: String): Map<String, Float> {
        if (metricHistory.isEmpty()) return emptyMap()

        val values = metricHistory.map { getMetricValue(it, metric) }
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        val avg = values.average().toFloat()
        val latest = values.lastOrNull() ?: 0f

        return mapOf(
            "min" to min,
            "max" to max,
            "average" to avg,
            "latest" to latest,
            "trend" to ((values.getOrNull(values.size - 2) ?: 0f).let { latest - it })
        )
    }

    /**
     * Extract metric value from update
     */
    private fun getMetricValue(update: AIMetricUpdate, metric: String): Float {
        return when (metric) {
            "memory" -> update.memoryLoad
            "reasoning" -> update.reasoningConfidence
            "autonomy" -> update.autonomyLevel
            "health" -> update.systemHealth
            "animation" -> update.animationIntensity
            "predicted" -> update.predictedBehavior
            "performance" -> update.performanceScore
            else -> 0.5f
        }
    }

    /**
     * Get historical data for charting
     */
    fun getHistoricalData(metric: String, points: Int = 20): List<Float> {
        return metricHistory
            .takeLast(points)
            .map { getMetricValue(it, metric) }
    }

    /**
     * Get current metric snapshot
     */
    fun getCurrentSnapshot(): Map<String, Any> {
        val current = _metricStream.value
        return if (current != null) {
            mapOf(
                "timestamp" to current.timestamp,
                "memory" to current.memoryLoad,
                "reasoning" to current.reasoningConfidence,
                "autonomy" to current.autonomyLevel,
                "health" to current.systemHealth,
                "evolution" to current.evolutionGeneration,
                "skills" to current.skillCount,
                "goals" to current.activeGoals,
                "animation" to current.animationIntensity,
                "predicted" to current.predictedBehavior,
                "performance" to current.performanceScore,
                "insights" to current.insights
            )
        } else {
            emptyMap()
        }
    }

    /**
     * Check if metrics are improving
     */
    fun areMetricsImproving(): Boolean {
        val trends = listOf(
            getMetricTrend("health"),
            getMetricTrend("autonomy"),
            getMetricTrend("performance")
        )
        return trends.count { it > 0 } >= 2
    }

    /**
     * Get alert status if issues detected
     */
    fun getAlertStatus(): String? {
        val current = _metricStream.value ?: return null

        return when {
            current.memoryLoad > 0.9f -> "⚠ Memory approaching capacity"
            current.systemHealth < 0.3f -> "🔴 System health critical"
            current.reasoningConfidence < 0.2f -> "⚡ Low reasoning confidence"
            current.autonomyLevel > 0.95f -> "? Max autonomy reached"
            else -> null
        }
    }

    /**
     * Reset observer
     */
    suspend fun reset() {
        metricHistory.clear()
        _metricStream.emit(null)
        _systemStatus.emit("Reset")
        _dashboardUpdate.emit(emptyMap())
        _visualizationUpdate.emit(emptyMap())
        cycleCount = 0
    }
}
