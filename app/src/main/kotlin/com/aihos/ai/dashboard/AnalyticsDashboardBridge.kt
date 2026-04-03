package com.aihos.ai.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * ANALYTICS DASHBOARD BRIDGE
 * Real-time metrics synchronization to UI and 3D visualization
 * - Live data streaming to Android Compose UI
 * - WebGL 3D visualization updates
 * - Real-time charts and metrics
 * - Interactive system monitoring
 */

data class DashboardMetric(
    val name: String = "",
    val value: Float = 0f,
    val min: Float = 0f,
    val max: Float = 1f,
    val trend: String = "stable",
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

data class DashboardState(
    val timestamp: Long = System.currentTimeMillis(),
    val metrics: List<DashboardMetric> = emptyList(),
    val systemHealth: Float = 0.5f,
    val activeVisualizations: List<String> = emptyList(),
    val updateFrequency: Long = 100,
    val isStreaming: Boolean = false
)

data class VisualizationData(
    val chartType: String = "line",
    val dataPoints: List<Float> = emptyList(),
    val labels: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val animationIntensity: Float = 0.5f
)

class AnalyticsDashboardBridge {
    // Dashboard state
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    // Visualization data
    private val _visualizationData = MutableStateFlow(mapOf<String, VisualizationData>())
    val visualizationData: StateFlow<Map<String, VisualizationData>> = _visualizationData

    // Metrics history
    private val metricHistory = mutableMapOf<String, MutableList<Float>>()
    private val maxHistorySize = 200

    private var isStreaming = false
    private var updateCount = 0

    /**
     * Update dashboard metrics from system
     */
    suspend fun updateDashboardMetrics(metricsMap: Map<String, Float>) {
        updateCount++

        val dashboardMetrics = mutableListOf<DashboardMetric>()

        metricsMap.forEach { (name, value) ->
            // Get or initialize history
            val history = metricHistory.getOrPut(name) { mutableListOf() }
            history.add(value)
            if (history.size > maxHistorySize) {
                history.removeAt(0)
            }

            // Calculate trend
            val trend = calculateTrend(history)

            // Determine category
            val category = categorizeMetric(name)

            // Create dashboard metric
            val metric = DashboardMetric(
                name = name,
                value = value,
                min = history.minOrNull() ?: 0f,
                max = history.maxOrNull() ?: 1f,
                trend = trend,
                category = category
            )

            dashboardMetrics.add(metric)
        }

        // Calculate overall system health
        val systemHealth = calculateSystemHealth(dashboardMetrics)

        // Update state
        val newState = _dashboardState.value.copy(
            timestamp = System.currentTimeMillis(),
            metrics = dashboardMetrics,
            systemHealth = systemHealth,
            isStreaming = isStreaming
        )

        _dashboardState.emit(newState)

        // Update visualizations
        updateVisualizations(dashboardMetrics)
    }

    /**
     * Start real-time metric streaming
     */
    suspend fun startStreaming() {
        isStreaming = true
        val updatedState = _dashboardState.value.copy(isStreaming = true)
        _dashboardState.emit(updatedState)
    }

    /**
     * Stop real-time metric streaming
     */
    suspend fun stopStreaming() {
        isStreaming = false
        val updatedState = _dashboardState.value.copy(isStreaming = false)
        _dashboardState.emit(updatedState)
    }

    /**
     * Get metric history for charting
     */
    fun getMetricHistory(metricName: String, limit: Int = 50): List<Float> {
        return (metricHistory[metricName] ?: emptyList()).takeLast(limit)
    }

    /**
     * Get visualization configuration
     */
    fun getVisualizationConfig(visualizationType: String): Map<String, Any> {
        return when (visualizationType) {
            "health_gauge" -> mapOf(
                "type" to "gauge",
                "min" to 0f,
                "max" to 1f,
                "segments" to 5,
                "colors" to listOf("#FF0000", "#FFA500", "#FFFF00", "#90EE90", "#00FF00")
            )
            "convergence_3d" -> mapOf(
                "type" to "3d_scatter",
                "axes" to listOf("pso_fitness", "aco_cost", "qga_fitness"),
                "animationSpeed" to 1000,
                "particleSize" to 2
            )
            "performance_timeline" -> mapOf(
                "type" to "line_chart",
                "xAxis" to "time",
                "yAxis" to "performance_score",
                "smoothing" to true
            )
            "skill_radar" -> mapOf(
                "type" to "radar",
                "categories" to listOf("decision", "pattern", "optimization", "adaptation"),
                "animationType" to "circular"
            )
            else -> emptyMap()
        }
    }

    /**
     * Get dashboard summary
     */
    fun getDashboardSummary(): Map<String, Any> {
        val state = _dashboardState.value
        return mapOf(
            "timestamp" to state.timestamp,
            "system_health" to state.systemHealth,
            "metric_count" to state.metrics.size,
            "update_count" to updateCount,
            "is_streaming" to isStreaming,
            "metrics_by_category" to state.metrics.groupBy { it.category }
                .mapValues { (_, metrics) -> metrics.map { it.name } }
        )
    }

    // ======================== PRIVATE HELPERS ========================

    private fun calculateTrend(history: List<Float>): String {
        if (history.size < 2) return "stable"

        val recent = history.takeLast(5)
        val avgChange = (recent.last() - recent.first()) / maxOf(recent.first(), 0.001f)

        return when {
            avgChange > 0.05f -> "improving"
            avgChange < -0.05f -> "degrading"
            else -> "stable"
        }
    }

    private fun categorizeMetric(name: String): String {
        return when {
            name.contains("fitness") || name.contains("cost") -> "optimization"
            name.contains("accuracy") || name.contains("prediction") -> "neural"
            name.contains("health") || name.contains("cpu") || name.contains("memory") -> "system"
            name.contains("agreement") || name.contains("weight") -> "ensemble"
            name.contains("skill") || name.contains("proficiency") -> "learning"
            else -> "general"
        }
    }

    private fun calculateSystemHealth(metrics: List<DashboardMetric>): Float {
        if (metrics.isEmpty()) return 0.5f

        val healthMetrics = metrics.filter { metric ->
            metric.category in listOf("system", "optimization", "neural")
        }

        if (healthMetrics.isEmpty()) return 0.5f

        // Normalize metrics to 0-1 (inverse for cost-like metrics)
        val normalizedValues = healthMetrics.map { metric ->
            when {
                metric.name.contains("cost") || metric.name.contains("error") ->
                    1f / (1f + metric.value)
                metric.name.contains("fitness") ->
                    metric.value
                metric.max > 0 ->
                    metric.value / metric.max
                else ->
                    metric.value
            }.coerceIn(0f, 1f)
        }

        return normalizedValues.average().toFloat()
    }

    private suspend fun updateVisualizations(metrics: List<DashboardMetric>) {
        val vizData = mutableMapOf<String, VisualizationData>()

        // Health gauge visualization
        val healthMetric = metrics.find { it.name == "system_health" }
        if (healthMetric != null) {
            vizData["health_gauge"] = VisualizationData(
                chartType = "gauge",
                dataPoints = listOf(healthMetric.value),
                animationIntensity = healthMetric.value
            )
        }

        // Convergence 3D visualization
        val psoFitness = metrics.find { it.name.contains("pso") }
        val acoFitness = metrics.find { it.name.contains("aco") }
        val qgaFitness = metrics.find { it.name.contains("qga") }

        if (psoFitness != null && acoFitness != null && qgaFitness != null) {
            vizData["convergence_3d"] = VisualizationData(
                chartType = "scatter_3d",
                dataPoints = listOf(psoFitness.value, acoFitness.value, qgaFitness.value),
                animationIntensity = 0.5f + 0.5f * (1f - minOf(psoFitness.value, 1f))
            )
        }

        // Performance timeline
        val perfHistory = metricHistory["performance"]?.takeLast(50) ?: emptyList()
        if (perfHistory.isNotEmpty()) {
            vizData["performance_timeline"] = VisualizationData(
                chartType = "line",
                dataPoints = perfHistory,
                animationIntensity = 0.7f
            )
        }

        _visualizationData.emit(vizData)
    }
}
