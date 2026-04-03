
package com.aihos.data.repository

import com.aihos.data.db.SAIHOSDatabase
import com.aihos.data.db.entities.*
import com.aihos.data.db.daos.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for metrics and analytics data
 * Provides a unified interface for database operations
 * Phase 2.2: Database persistence layer
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val database: SAIHOSDatabase
) {
    
    // ==================== Metrics History ====================
    
    /**
     * Record current system metrics
     */
    suspend fun recordMetrics(
        memoryUsage: Float,
        reasoningConfidence: Float,
        autonomyLevel: Float,
        evolutionProgress: Float,
        systemHealth: Float,
        cycleCount: Long
    ) {
        val metrics = MetricsHistory.fromMetrics(
            memoryUsage, reasoningConfidence, autonomyLevel,
            evolutionProgress, systemHealth, cycleCount
        )
        database.metricsHistoryDao().insertMetrics(metrics)
    }
    
    /**
     * Get latest metrics record
     */
    suspend fun getLatestMetrics(): MetricsHistory? {
        return database.metricsHistoryDao().getLatestMetrics()
    }
    
    /**
     * Get metrics from past 24 hours
     */
    suspend fun getRecentMetrics(hours: Int = 24): List<MetricsHistory> {
        val startTime = System.currentTimeMillis() - (hours * 3600000L)
        return database.metricsHistoryDao().getRecentMetrics(100, startTime)
    }
    
    /**
     * Get average metrics for analysis
     */
    suspend fun getAverageMetrics(hours: Int = 1): MetricsAverage? {
        val startTime = System.currentTimeMillis() - (hours * 3600000L)
        return database.metricsHistoryDao().getAverageMetrics(startTime)
    }
    
    /**
     * Cleanup old metrics (keep last 7 days)
     */
    suspend fun cleanupOldMetrics(daysToKeep: Int = 7) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 86400000L)
        database.metricsHistoryDao().deleteOlderThan(cutoffTime)
    }
    
    // ==================== Memory Consolidation ====================
    
    /**
     * Start a memory consolidation event
     */
    suspend fun startMemoryConsolidation(
        consolidationType: String,
        initialCapacity: Float
    ): Long {
        val consolidation = MemoryConsolidationEvent(
            consolidationType = consolidationType,
            initialCapacity = initialCapacity
        )
        return database.memoryConsolidationDao().insertConsolidation(consolidation)
    }
    
    /**
     * Complete a memory consolidation event
     */
    suspend fun completeMemoryConsolidation(
        consolidationId: Long,
        finalCapacity: Float,
        success: Boolean,
        itemsConsolidated: Int,
        durationMs: Long,
        errorMessage: String? = null
    ) {
        val consolidation = MemoryConsolidationEvent(
            id = consolidationId,
            endTime = System.currentTimeMillis(),
            initialCapacity = 0f,  // This will be fetched in real implementation
            finalCapacity = finalCapacity,
            consolidationType = "",
            success = success,
            errorMessage = errorMessage,
            itemsConsolidated = itemsConsolidated,
            durationMs = durationMs
        )
        database.memoryConsolidationDao().updateConsolidation(consolidation)
    }
    
    /**
     * Get consolidation statistics for last 24 hours
     */
    suspend fun getConsolidationStats(): ConsolidationStats? {
        val startTime = System.currentTimeMillis() - 86400000L
        return database.memoryConsolidationDao().getConsolidationStatistics(startTime)
    }
    
    // ==================== System Events ====================
    
    /**
     * Log a system event
     */
    suspend fun logSystemEvent(
        eventType: String,
        eventName: String,
        severity: String,
        description: String? = null,
        sourceEngine: String? = null,
        cycleCount: Long? = null,
        metadataJson: String? = null
    ) {
        val event = SystemEventLog.fromSystemEvent(
            eventType = eventType,
            eventName = eventName,
            severity = severity,
            description = description,
            metadataJson = metadataJson,
            sourceEngine = sourceEngine,
            cycleCount = cycleCount
        )
        database.systemEventLogDao().insertEvent(event)
    }
    
    /**
     * Get all critical events from last hour
     */
    suspend fun getCriticalEvents(): Int {
        val startTime = System.currentTimeMillis() - 3600000L
        return database.systemEventLogDao().getCriticalEventCount(startTime)
    }
    
    /**
     * Get events by type
     */
    suspend fun getEventsByType(eventType: String): List<SystemEventLog> {
        return database.systemEventLogDao().getEventsByType(eventType)
    }
    
    /**
     * Get events by engine
     */
    suspend fun getEventsByEngine(engine: String): List<SystemEventLog> {
        return database.systemEventLogDao().getEventsByEngine(engine, 100)
    }
    
    /**
     * Watch events as flow
     */
    fun watchEvents(hoursAgo: Int = 1): Flow<List<SystemEventLog>> {
        val startTime = System.currentTimeMillis() - (hoursAgo * 3600000L)
        return database.systemEventLogDao().getEventsAfter(startTime)
    }
    
    // ==================== Health Snapshots ====================
    
    /**
     * Record a health snapshot (typically hourly)
     */
    suspend fun recordHealthSnapshot(
        overallHealth: Float,
        memoryHealth: Float,
        reasoningHealth: Float,
        autonomyHealth: Float,
        evolutionHealth: Float,
        uptimeMs: Long,
        cycleCount: Long,
        memoryUsageMb: Int
    ) {
        val snapshot = HealthSnapshot(
            overallHealth = overallHealth,
            memoryHealth = memoryHealth,
            reasoningHealth = reasoningHealth,
            autonomyHealth = autonomyHealth,
            evolutionHealth = evolutionHealth,
            uptimeMs = uptimeMs,
            cycleCount = cycleCount,
            memoryUsageMb = memoryUsageMb
        )
        database.healthSnapshotDao().insertSnapshot(snapshot)
    }
    
    /**
     * Get health trend data
     */
    suspend fun getHealthTrend(hours: Int = 24): HealthTrend? {
        val startTime = System.currentTimeMillis() - (hours * 3600000L)
        return database.healthSnapshotDao().getHealthTrend(startTime)
    }
    
    /**
     * Watch health snapshots as flow
     */
    fun watchHealthSnapshots(): Flow<List<HealthSnapshot>> {
        val startTime = System.currentTimeMillis() - 86400000L
        return database.healthSnapshotDao().getSnapshotsAfter(startTime)
    }
    
    // ==================== User Interactions ====================
    
    /**
     * Log user interaction
     */
    suspend fun logUserInteraction(
        interactionType: String,
        screenName: String,
        action: String,
        elementId: String? = null,
        durationMs: Long = 0L,
        metadataJson: String? = null
    ) {
        val interaction = UserInteractionEvent.fromInteraction(
            interactionType = interactionType,
            screenName = screenName,
            action = action,
            elementId = elementId,
            durationMs = durationMs,
            metadataJson = metadataJson
        )
        database.userInteractionDao().insertInteraction(interaction)
    }
    
    /**
     * Get engagement metrics
     */
    suspend fun getEngagementMetrics(): EngagementMetrics? {
        val startTime = System.currentTimeMillis() - 3600000L
        return database.userInteractionDao().getEngagementMetrics(startTime)
    }
    
    /**
     * Get screen-specific interactions
     */
    suspend fun getScreenInteractions(screenName: String): List<UserInteractionEvent> {
        return database.userInteractionDao().getScreenInteractions(screenName, 100)
    }
    
    // ==================== Performance Metrics ====================
    
    /**
     * Record performance metric
     */
    suspend fun recordPerformanceMetric(
        metricName: String,
        metricValue: Float,
        unit: String,
        screenName: String? = null,
        thresholdType: String? = null,
        additionalDataJson: String? = null
    ) {
        val metric = PerformanceMetric(
            metricName = metricName,
            metricValue = metricValue,
            unit = unit,
            thresholdType = thresholdType,
            screenName = screenName,
            additionalDataJson = additionalDataJson
        )
        database.performanceMetricsHistoryDao().insertMetric(metric)
    }
    
    /**
     * Get metric statistics
     */
    suspend fun getMetricStatistics(metricName: String): MetricStatistics? {
        val startTime = System.currentTimeMillis() - 3600000L
        return database.performanceMetricsHistoryDao().getMetricStatistics(metricName, startTime)
    }
    
    /**
     * Get performance anomalies
     */
    suspend fun getPerformanceAnomalies(): List<PerformanceMetric> {
        return database.performanceMetricsHistoryDao().getAnomalies(100)
    }
    
    /**
     * Get metric history
     */
    suspend fun getMetricHistory(metricName: String): List<PerformanceMetric> {
        return database.performanceMetricsHistoryDao().getMetricHistory(metricName, 100)
    }
    
    // ==================== Cleanup & Maintenance ====================
    
    /**
     * Comprehensive database cleanup
     * Called periodically to remove old data
     */
    suspend fun cleanupDatabase(daysToKeep: Int = 7) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 86400000L)
        
        database.metricsHistoryDao().deleteOlderThan(cutoffTime)
        database.memoryConsolidationDao().deleteOlderThan(cutoffTime)
        database.systemEventLogDao().deleteOlderThan(cutoffTime)
        database.healthSnapshotDao().deleteOlderThan(cutoffTime)
        database.userInteractionDao().deleteOlderThan(cutoffTime)
        database.performanceMetricsHistoryDao().deleteOlderThan(cutoffTime)
    }
    
    /**
     * Export analytics data as JSON
     * Used for backup or sharing
     */
    suspend fun exportAnalyticsData(hoursAgo: Int = 24): AnalyticsExport {
        val startTime = System.currentTimeMillis() - (hoursAgo * 3600000L)
        
        return AnalyticsExport(
            metrics = database.metricsHistoryDao().getRecentMetrics(1000, startTime),
            events = database.systemEventLogDao().getEventsAfter(startTime).first(),
            consolidations = database.memoryConsolidationDao().getConsolidationsAfter(startTime),
            exportTimestamp = System.currentTimeMillis(),
            exportHours = hoursAgo
        )
    }
}

/**
 * Data class for exported analytics data
 */
data class AnalyticsExport(
    val metrics: List<MetricsHistory>,
    val events: List<SystemEventLog>,
    val consolidations: List<MemoryConsolidationEvent>,
    val exportTimestamp: Long,
    val exportHours: Int
)
