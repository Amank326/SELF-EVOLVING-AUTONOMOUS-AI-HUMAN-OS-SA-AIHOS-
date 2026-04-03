package com.aihos.ai.analytics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * ADVANCED ANALYTICS ENGINE
 * Real-time analysis and insights generation across all AI systems
 * - Multi-dimensional data aggregation
 * - Pattern detection and anomaly identification
 * - Predictive trend forecasting
 * - Performance profiling
 * - Bottleneck analysis
 * - Advanced statistical modeling
 */

data class DataPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val value: Float = 0f,
    val label: String = "",
    val metadata: Map<String, Any> = emptyMap()
)

data class TimeSeries(
    val name: String = "",
    val dataPoints: List<DataPoint> = emptyList(),
    val window: Int = 100 // sliding window size
) {
    fun addPoint(point: DataPoint): TimeSeries {
        val newPoints = (dataPoints + point).takeLast(window)
        return TimeSeries(name, newPoints, window)
    }

    fun mean(): Float = if (dataPoints.isEmpty()) 0f else dataPoints.map { it.value }.average().toFloat()
    fun variance(): Float {
        val m = mean()
        return if (dataPoints.isEmpty()) 0f else dataPoints.map { (it.value - m).pow(2) }.average().toFloat()
    }
    fun stdDev(): Float = sqrt(variance())
    fun trend(): Float {
        if (dataPoints.size < 2) return 0f
        val n = dataPoints.size.toFloat()
        val xMean = (dataPoints.size - 1) / 2f
        val yMean = mean()

        var numerator = 0f
        var denominator = 0f

        for (i in dataPoints.indices) {
            val x = i.toFloat() - xMean
            val y = dataPoints[i].value - yMean
            numerator += x * y
            denominator += x * x
        }

        return if (denominator == 0f) 0f else numerator / denominator
    }
}

data class AnalyticsSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val dataPoints: Int = 0,
    val activeSeries: Int = 0,
    val patterns: List<String> = emptyList(),
    val anomalies: List<String> = emptyList(),
    val trends: List<Pair<String, Float>> = emptyList(),
    val correlations: List<Triple<String, String, Float>> = emptyList(),
    val systemHealthScore: Float = 0.5f,
    val predictedStateChange: Float = 0f
)

data class SystemHealth(
    val score: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

class AdvancedAnalyticsEngine {
    // Time series storage
    private val timeSeries = mutableMapOf<String, TimeSeries>()
    
    // History
    private val history = mutableListOf<AnalyticsSnapshot>()
    private val maxHistory = 500
    
    // Event log
    private val events = mutableListOf<Map<String, Any>>()
    private val maxEvents = 1000

    // State
    private val _snapshot = MutableStateFlow(AnalyticsSnapshot())
    val snapshot: StateFlow<AnalyticsSnapshot> = _snapshot

    // Event Flow for observers (e.g., notification system)
    private val _eventFlow = MutableSharedFlow<Map<String, Any>>(replay = 10)
    val eventFlow: SharedFlow<Map<String, Any>> = _eventFlow

    // Pattern detection
    private val patternDetectors = mutableMapOf<String, PatternDetector>()
    
    private var isRunning = false

    /**
     * Add data point to time series
     */
    fun recordMetric(seriesName: String, value: Float, label: String = "") {
        val point = DataPoint(
            timestamp = System.currentTimeMillis(),
            value = value,
            label = label
        )

        timeSeries[seriesName] = (timeSeries[seriesName] ?: TimeSeries(seriesName)).addPoint(point)
    }

    /**
     * Multi-metric recording
     */
    fun recordMetrics(metrics: Map<String, Float>) {
        metrics.forEach { (name, value) ->
            recordMetric(name, value)
        }
    }

    /**
     * Real-time analysis
     */
    suspend fun analyzeSnapshot(): AnalyticsSnapshot {
        val patterns = detectPatterns()
        val anomalies = detectAnomalies()
        val trends = analyzeTrends()
        val correlations = computeCorrelations()
        val healthScore = computeSystemHealth()
        val stateChange = predictStateChange()

        val newSnapshot = AnalyticsSnapshot(
            timestamp = System.currentTimeMillis(),
            dataPoints = timeSeries.values.sumOf { it.dataPoints.size },
            activeSeries = timeSeries.size,
            patterns = patterns,
            anomalies = anomalies,
            trends = trends,
            correlations = correlations,
            systemHealthScore = healthScore,
            predictedStateChange = stateChange
        )

        _snapshot.emit(newSnapshot)
        
        // Keep history
        history.add(newSnapshot)
        if (history.size > maxHistory) {
            history.removeAt(0)
        }

        return newSnapshot
    }

    /**
     * Detect patterns in time series
     */
    private fun detectPatterns(): List<String> {
        val patterns = mutableListOf<String>()

        timeSeries.forEach { (name, series) ->
            val trend = series.trend()
            val stdDev = series.stdDev()

            when {
                trend > 0.1f && stdDev < 0.2f -> patterns.add("$name: Steady Increase")
                trend < -0.1f && stdDev < 0.2f -> patterns.add("$name: Steady Decrease")
                stdDev > 0.5f && trend.absoluteValue < 0.05f -> patterns.add("$name: High Volatility")
                stdDev < 0.1f -> patterns.add("$name: Stable State")
                series.dataPoints.size > 10 -> {
                    // Detect periodicity
                    val period = detectPeriodicity(series)
                    if (period > 0) patterns.add("$name: Periodic (period=$period)")
                }
            }
        }

        return patterns
    }

    /**
     * Detect anomalies using statistical methods
     */
    private fun detectAnomalies(): List<String> {
        val anomalies = mutableListOf<String>()

        timeSeries.forEach { (name, series) ->
            if (series.dataPoints.isEmpty()) return@forEach

            val mean = series.mean()
            val stdDev = series.stdDev()
            val threshold = 2.5f // 2.5 sigma

            series.dataPoints.takeLast(5).forEach { point ->
                val zScore = abs((point.value - mean) / (stdDev + 0.001f))
                if (zScore > threshold) {
                    anomalies.add("$name: Outlier detected (z-score=${"%.2f".format(zScore)})")
                }
            }
        }

        return anomalies
    }

    /**
     * Analyze trends across all series
     */
    private fun analyzeTrends(): List<Pair<String, Float>> {
        return timeSeries.mapNotNull { (name, series) ->
            val trend = series.trend()
            if (trend.absoluteValue > 0.05f) {
                Pair(name, trend)
            } else null
        }
    }

    /**
     * Compute correlations between series
     */
    private fun computeCorrelations(): List<Triple<String, String, Float>> {
        val correlations = mutableListOf<Triple<String, String, Float>>()
        val seriesNames = timeSeries.keys.toList()

        for (i in seriesNames.indices) {
            for (j in i + 1 until seriesNames.size) {
                val series1 = timeSeries[seriesNames[i]] ?: continue
                val series2 = timeSeries[seriesNames[j]] ?: continue

                val correlation = computePearsonCorrelation(series1, series2)
                if (abs(correlation) > 0.6f) {
                    correlations.add(Triple(seriesNames[i], seriesNames[j], correlation))
                }
            }
        }

        return correlations.sortedByDescending { it.third.absoluteValue }
    }

    /**
     * Pearson correlation coefficient
     */
    private fun computePearsonCorrelation(series1: TimeSeries, series2: TimeSeries): Float {
        val n = minOf(series1.dataPoints.size, series2.dataPoints.size)
        if (n < 2) return 0f

        val points1 = series1.dataPoints.takeLast(n)
        val points2 = series2.dataPoints.takeLast(n)

        val mean1 = points1.map { it.value.toDouble() }.average()
        val mean2 = points2.map { it.value.toDouble() }.average()

        var covariance = 0.0
        var variance1 = 0.0
        var variance2 = 0.0

        for (i in 0 until n) {
            val diff1 = points1[i].value.toDouble() - mean1
            val diff2 = points2[i].value.toDouble() - mean2
            covariance += diff1 * diff2
            variance1 += diff1 * diff1
            variance2 += diff2 * diff2
        }

        return if (variance1 == 0.0 || variance2 == 0.0) {
            0f
        } else {
            (covariance / sqrt(variance1 * variance2)).toFloat().coerceIn(-1f, 1f)
        }
    }

    /**
     * Compute system health score
     */
    private fun computeSystemHealth(): Float {
        var healthScore = 0.5f

        timeSeries.forEach { (name, series) ->
            when {
                name.contains("error", ignoreCase = true) -> {
                    val errorRate = series.mean()
                    healthScore -= errorRate * 0.1f
                }
                name.contains("performance", ignoreCase = true) -> {
                    val performance = series.mean()
                    healthScore += performance * 0.05f
                }
                name.contains("latency", ignoreCase = true) -> {
                    val latency = series.mean()
                    healthScore -= (latency / 1000f) * 0.1f
                }
            }
        }

        return healthScore.coerceIn(0f, 1f)
    }

    /**
     * Predict state change using ARIMA-like approach
     */
    private fun predictStateChange(): Float {
        if (timeSeries.isEmpty()) return 0f

        var totalChange = 0f
        timeSeries.forEach { (_, series) ->
            if (series.dataPoints.size >= 3) {
                val recent = series.dataPoints.takeLast(3).map { it.value }
                val change = (recent[2] - recent[0]) / (recent[0] + 0.001f)
                totalChange += change
            }
        }

        return (totalChange / timeSeries.size.coerceAtLeast(1)).coerceIn(-1f, 1f)
    }

    /**
     * Detect periodicity in time series
     */
    private fun detectPeriodicity(series: TimeSeries): Int {
        if (series.dataPoints.size < 10) return -1

        val maxPeriod = series.dataPoints.size / 2

        for (period in 2..maxPeriod) {
            var correlation = 0f
            var count = 0

            for (i in 0 until series.dataPoints.size - period) {
                if (i + period < series.dataPoints.size) {
                    correlation += (series.dataPoints[i].value - series.dataPoints[i + period].value).absoluteValue
                    count++
                }
            }

            if (count > 0 && correlation / count < 0.1f) {
                return period
            }
        }

        return -1
    }

    /**
     * Get historical trend
     */
    fun getHistoricalTrend(seriesName: String, lookback: Int = 50): List<Float> {
        return (timeSeries[seriesName]?.dataPoints?.takeLast(lookback) ?: emptyList())
            .map { it.value }
    }

    /**
     * Get statistical summary
     */
    fun getStatisticalSummary(seriesName: String): Map<String, Float> {
        val series = timeSeries[seriesName] ?: return emptyMap()

        return mapOf(
            "mean" to series.mean(),
            "variance" to series.variance(),
            "stdDev" to series.stdDev(),
            "trend" to series.trend(),
            "min" to (series.dataPoints.minOfOrNull { it.value } ?: 0f),
            "max" to (series.dataPoints.maxOfOrNull { it.value } ?: 0f)
        )
    }

    /**
     * Get all time series names
     */
    fun getTimeSeriesNames(): List<String> = timeSeries.keys.toList()

    /**
     * Clear data older than specified duration
     */
    fun clearOldData(olderThanMs: Long) {
        val cutoffTime = System.currentTimeMillis() - olderThanMs
        timeSeries.forEach { (name, series) ->
            val newPoints = series.dataPoints.filter { it.timestamp > cutoffTime }
            timeSeries[name] = TimeSeries(name, newPoints, series.window)
        }
    }

    /**
     * Record system health metrics
     */
    fun recordSystemHealth(health: SystemHealth) {
        _snapshot.value = _snapshot.value.copy(
            systemHealthScore = health.score,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Start analytics engine
     */
    fun start() {
        isRunning = true
    }

    /**
     * Stop analytics engine
     */
    fun stop() {
        isRunning = false
    }

    /**
     * Get metric history
     */
    fun getMetricHistory(metric: String): List<Double> {
        return timeSeries[metric]?.dataPoints?.map { it.value.toDouble() } ?: emptyList()
    }

    /**
     * Log event
     */
    fun logEvent(event: Map<String, Any>) {
        events.add(event)
        if (events.size > maxEvents) {
            events.removeAt(0)
        }
        
        // Emit event to SharedFlow for observers (e.g., notification system)
        try {
            _eventFlow.tryEmit(event)
        } catch (e: Exception) {
            // Ignore emit failures
        }
    }

    /**
     * Get all events
     */
    fun getEvents(): List<Map<String, Any>> = events.toList()

    /**
     * Get event flow for reactive subscribers
     */
    suspend fun getEventFlow(): SharedFlow<Map<String, Any>> = eventFlow

    /**
     * Log event with name and properties (convenience overload)
     */
    fun logEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val event = mutableMapOf<String, Any>(
            "event_name" to eventName,
            "timestamp" to System.currentTimeMillis()
        )
        event.putAll(properties)
        logEvent(event)
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        history.clear()
        timeSeries.clear()
        events.clear()
    }
}

/**
 * Pattern detector interface for extensibility
 */
interface PatternDetector {
    fun detect(series: TimeSeries): List<String>
}

/**
 * Cycle pattern detector
 */
class CyclePatternDetector : PatternDetector {
    override fun detect(series: TimeSeries): List<String> {
        val patterns = mutableListOf<String>()
        
        if (series.dataPoints.size < 20) return patterns

        val values = series.dataPoints.map { it.value }
        var cycleCount = 0
        var inCycle = false

        for (i in 1 until values.size) {
            val diff = values[i] - values[i - 1]
            if (diff > 0 && !inCycle) {
                inCycle = true
                cycleCount++
            } else if (diff < 0 && inCycle) {
                inCycle = false
            }
        }

        if (cycleCount > 2) {
            patterns.add("Multiple Cycles Detected: $cycleCount cycles")
        }

        return patterns
    }
}
