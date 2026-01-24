package com.aihos.ai.cognition

import com.aihos.ai.reasoning.ReasoningContext
import com.aihos.ai.perception.SystemSignalsManager
import com.aihos.ai.perception.EnvironmentContext
import timber.log.Timber

/**
 * Integration layer between CognitionLoopManager and existing AI systems
 * 
 * This module adapts the perception (EnvironmentContext) from SystemSignalsManager
 * into ReasoningContext for the autonomy controller.
 * 
 * It's the bridge between:
 * - CognitionLoopManager (scheduling and continuous operation)
 * - SystemSignalsManager (Android OS signals)
 * - ReasoningEngine (AI decision making)
 */

/**
 * Default implementation of ReasoningContextProvider
 * Converts EnvironmentContext → ReasoningContext
 */
class DefaultReasoningContextProvider(
    private val systemSignalsManager: SystemSignalsManager,
    private val contextEnricher: ReasoningContextEnricher? = null
) : ReasoningContextProvider {
    
    override suspend fun getCurrentContext(): ReasoningContext {
        return try {
            // Get environment signals
            val environment = systemSignalsManager.getEnvironmentContext()
            
            // Build basic reasoning context from environment
            var context = buildContextFromEnvironment(environment)
            
            // Optional: Enrich with additional data (user preferences, etc.)
            if (contextEnricher != null) {
                context = contextEnricher.enrichContext(context, environment)
            }
            
            context
        } catch (e: Exception) {
            Timber.e(e, "Error building reasoning context")
            // Return safe default context
            buildDefaultContext()
        }
    }
    
    /**
     * Build ReasoningContext from EnvironmentContext
     */
    private fun buildContextFromEnvironment(environment: EnvironmentContext): ReasoningContext {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        
        val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "SUNDAY"
            java.util.Calendar.MONDAY -> "MONDAY"
            java.util.Calendar.TUESDAY -> "TUESDAY"
            java.util.Calendar.WEDNESDAY -> "WEDNESDAY"
            java.util.Calendar.THURSDAY -> "THURSDAY"
            java.util.Calendar.FRIDAY -> "FRIDAY"
            java.util.Calendar.SATURDAY -> "SATURDAY"
            else -> "UNKNOWN"
        }
        
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val currentTime = String.format("%02d:%02d", hourOfDay, minute)
        
        // Map activity level to focus state
        val userIsFocused = when (environment.activity.name) {
            "IDLE", "LIGHT" -> false
            "ACTIVE", "INTENSE" -> true
            else -> false
        }
        
        // Estimate app usage duration (simplified - in real scenario would track this)
        val appUsageDurationMinutes = if (userIsFocused) 15 else 5
        
        // Recent interaction count based on activity
        val recentInteractionCount = when (environment.activity.name) {
            "IDLE" -> 0
            "LIGHT" -> 2
            "ACTIVE" -> 5
            "INTENSE" -> 10
            else -> 0
        }
        
        return ReasoningContext(
            timestamp = now,
            currentTime = currentTime,
            dayOfWeek = dayOfWeek,
            appUsageDurationMinutes = appUsageDurationMinutes,
            recentInteractionCount = recentInteractionCount,
            userIsFocused = userIsFocused,
            batteryPercent = environment.battery.levelPercent,
            isCharging = environment.battery.isCharging,
            recentDecisions = emptyList(),
            userGoals = emptyList(),
            userPreferences = mapOf(
                "battery_level" to environment.battery.levelPercent.toString(),
                "time_of_day" to currentTime,
                "day_of_week" to dayOfWeek,
                "network_available" to (environment.network.value != "DISCONNECTED").toString(),
                "user_activity" to environment.activity.name
            ),
            availableActions = emptyList()
        )
    }
    
    /**
     * Safe default context when something goes wrong
     */
    private fun buildDefaultContext(): ReasoningContext {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val currentTime = String.format("%02d:%02d", 
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE)
        )
        
        return ReasoningContext(
            timestamp = now,
            currentTime = currentTime,
            dayOfWeek = "UNKNOWN",
            appUsageDurationMinutes = 0,
            recentInteractionCount = 0,
            userIsFocused = false,
            batteryPercent = 50,
            isCharging = false
        )
    }
}

/**
 * Optional enricher for adding domain-specific context
 */
interface ReasoningContextEnricher {
    suspend fun enrichContext(context: ReasoningContext, environment: EnvironmentContext): ReasoningContext
}

/**
 * Default enricher that adds constraints based on environment
 */
class EnvironmentAwareReasoningContextEnricher : ReasoningContextEnricher {
    
    override suspend fun enrichContext(
        context: ReasoningContext,
        environment: EnvironmentContext
    ): ReasoningContext {
        var enrichedContext = context
        
        // Add constraints to preferences based on calmness/constraints
        enrichedContext = enrichedContext.copy(
            userPreferences = enrichedContext.userPreferences.toMutableMap().apply {
                put("environment_calmness", environment.calmness.toString())
                put("environment_constraints", environment.constraints.toString())
                put("environment_openness", environment.evolutionaryOpenness.toString())
                
                // Add environmental recommendations
                if (environment.constraints > 0.7f) {
                    put("reasoning_mode", "conservative") // High pressure: be conservative
                } else if (environment.calmness > 0.7f) {
                    put("reasoning_mode", "exploratory") // Calm: explore more
                }
                
                // Network-aware
                if (environment.network.value == "DISCONNECTED") {
                    put("offline_mode", "true")
                }
                
                // Battery-aware
                if (environment.battery.levelPercent < 15) {
                    put("battery_mode", "critical")
                } else if (environment.battery.levelPercent < 30) {
                    put("battery_mode", "low")
                }
            }.toMap()
        )
        
        // Adjust decision interval preferences based on environment
        if (environment.constraints > 0.8f || environment.battery.levelPercent < 15) {
            // High pressure or critical battery: reasoning can be less frequent
            enrichedContext = enrichedContext.copy(
                userPreferences = enrichedContext.userPreferences.toMutableMap().apply {
                    put("preferred_decision_frequency", "low")
                }.toMap()
            )
        }
        
        return enrichedContext
    }
}

/**
 * Monitor for continuous cognition loop health and performance
 * 
 * Tracks:
 * - Loop execution frequency
 * - Decision rate
 * - Error rate
 * - Battery impact
 * - Performance degradation
 */
class CognitionLoopMonitor(
    private val loopManager: CognitionLoopManager
) {
    
    private var lastMetricsLogTime = 0L
    private val metricsLogIntervalMs = 60_000L // Log every minute
    
    /**
     * Check loop health and log if issues detected
     */
    suspend fun checkHealthAndLog() {
        val now = System.currentTimeMillis()
        if (now - lastMetricsLogTime < metricsLogIntervalMs) return
        
        lastMetricsLogTime = now
        
        val status = loopManager.getLoopStatus()
        val metrics = loopManager.getSchedulingMetrics()
        
        logMetrics(status, metrics)
        checkForIssues(status, metrics)
    }
    
    /**
     * Log current metrics
     */
    private fun logMetrics(status: CognitionLoopStatus, metrics: SchedulingMetrics) {
        Timber.d("""
            |CognitionLoop Status:
            |  Running: ${status.isRunning}
            |  Paused: ${status.isPaused}
            |  BackgroundMode: ${status.isBackgroundMode}
            |  CurrentInterval: ${status.currentIntervalMs}ms
            |  NextCognitionIn: ${status.nextCognitionInMs}ms
            |  CyclesThisSession: ${status.cyclesCompletedThisSession}
            |  AvgCycleTime: ${status.averageCycleTimeMs}ms
            |Metrics:
            |  TotalCycles: ${metrics.totalCyclesCompleted}
            |  AvgCycleTime: ${metrics.averageCycleTimeMs}ms
            |  MaxCycleTime: ${metrics.maxCycleTimeMs}ms
            |  MinCycleTime: ${metrics.minCycleTimeMs}ms
            |  PausedCount: ${metrics.pausedCount}
            |  ResumedCount: ${metrics.resumedCount}
            |  EstBatteryDrain: ${metrics.batteryDrainEstimate}%/hour
        """.trimMargin())
    }
    
    /**
     * Check for issues and alert
     */
    private fun checkForIssues(status: CognitionLoopStatus, metrics: SchedulingMetrics) {
        val issues = mutableListOf<String>()
        
        // Check if cycle time is growing (memory leak?)
        if (metrics.maxCycleTimeMs > 5000) {
            issues.add("Cycle time exceeds 5 seconds (${metrics.maxCycleTimeMs}ms)")
        }
        
        // Check if battery drain is high
        if (metrics.batteryDrainEstimate > 0.5f) {
            issues.add("Estimated battery drain is high (${metrics.batteryDrainEstimate}%/hour)")
        }
        
        // Check for repeated errors
        if (status.lastError != null) {
            issues.add("Recent error: ${status.lastError}")
        }
        
        if (issues.isNotEmpty()) {
            Timber.w("CognitionLoop Issues: ${issues.joinToString(", ")}")
        }
    }
}

/**
 * Debugging utilities for continuous cognition
 */
object CognitionLoopDebugUtils {
    
    /**
     * Format metrics for human-readable display
     */
    fun formatMetrics(metrics: SchedulingMetrics): String {
        return """
            Cognition Loop Metrics:
            ├─ Total Cycles: ${metrics.totalCyclesCompleted}
            ├─ Average Cycle Time: ${metrics.averageCycleTimeMs}ms
            ├─ Max Cycle Time: ${metrics.maxCycleTimeMs}ms
            ├─ Min Cycle Time: ${metrics.minCycleTimeMs}ms
            ├─ Paused Count: ${metrics.pausedCount}
            ├─ Resumed Count: ${metrics.resumedCount}
            ├─ Background Transitions: ${metrics.backgroundTransitions}
            ├─ Foreground Transitions: ${metrics.foregroundTransitions}
            ├─ Error Count: ${metrics.errorCount}
            └─ Est. Battery Drain: ${metrics.batteryDrainEstimate}%/hour
        """.trimIndent()
    }
    
    /**
     * Format status for human-readable display
     */
    fun formatStatus(status: CognitionLoopStatus): String {
        return """
            Cognition Loop Status:
            ├─ Running: ${status.isRunning}
            ├─ Paused: ${status.isPaused}
            ├─ Background Mode: ${status.isBackgroundMode}
            ├─ Current Interval: ${status.currentIntervalMs}ms
            ├─ Next Cognition In: ${status.nextCognitionInMs}ms
            ├─ Cycles This Session: ${status.cyclesCompletedThisSession}
            ├─ Average Cycle Time: ${status.averageCycleTimeMs}ms
            ├─ Last Cognition: ${status.lastCognitionTimestamp}
            └─ Last Error: ${status.lastError ?: "None"}
        """.trimIndent()
    }
}
