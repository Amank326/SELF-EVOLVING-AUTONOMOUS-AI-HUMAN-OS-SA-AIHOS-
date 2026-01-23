package com.aihos.ai.perception

import com.aihos.ai.reasoning.ReasoningContext
import kotlinx.serialization.Serializable

/**
 * Extended reasoning context that includes environmental awareness
 * 
 * This enriches the standard ReasoningContext with environmental signals
 * from the Android OS, allowing the AI to understand device and app state
 * as part of its reasoning process.
 */
@Serializable
data class EnvironmentAwareReasoningContext(
    // Original reasoning context fields
    val timestamp: Long,
    val currentTime: String,
    val dayOfWeek: String,
    val appUsageDurationMinutes: Int,
    val recentInteractionCount: Int,
    val userIsFocused: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val recentDecisions: List<String> = emptyList(),
    val userGoals: List<String> = emptyList(),
    val userPreferences: Map<String, String> = emptyMap(),
    val availableActions: List<String> = emptyList(),
    
    // Environmental awareness
    val environment: EnvironmentContext,
    
    // Derived flags for reasoning
    val isHighPressureEnvironment: Boolean = false,      // Low battery, no network, intense activity
    val isOptimalLearningTime: Boolean = false,          // Good battery, calm, engaged
    val isReflectionTime: Boolean = false,               // Low activity, calm, night time
    val shouldConserveEnergy: Boolean = false,           // Critical battery, low power mode
    val hasNetworkAvailability: Boolean = true           // Network for evolution/updates
) {
    companion object {
        /**
         * Convert standard ReasoningContext to EnvironmentAwareReasoningContext
         */
        fun from(
            reasoningContext: ReasoningContext,
            environment: EnvironmentContext
        ): EnvironmentAwareReasoningContext {
            return EnvironmentAwareReasoningContext(
                timestamp = reasoningContext.timestamp,
                currentTime = reasoningContext.currentTime,
                dayOfWeek = reasoningContext.dayOfWeek,
                appUsageDurationMinutes = reasoningContext.appUsageDurationMinutes,
                recentInteractionCount = reasoningContext.recentInteractionCount,
                userIsFocused = reasoningContext.userIsFocused,
                batteryPercent = reasoningContext.batteryPercent,
                isCharging = reasoningContext.isCharging,
                recentDecisions = reasoningContext.recentDecisions,
                userGoals = reasoningContext.userGoals,
                userPreferences = reasoningContext.userPreferences,
                availableActions = reasoningContext.availableActions,
                environment = environment,
                isHighPressureEnvironment = environment.environmentalConstraints > 0.6f,
                isOptimalLearningTime = environment.evolutionaryOpenness > 0.7f &&
                                       environment.userActivityLevel != UserActivityLevel.IDLE,
                isReflectionTime = environment.environmentalCalmness > 0.6f &&
                                  environment.userActivityLevel == UserActivityLevel.IDLE,
                shouldConserveEnergy = environment.battery.isCritical ||
                                      environment.battery.isInLowPowerMode,
                hasNetworkAvailability = environment.networkState == NetworkState.CONNECTED
            )
        }
    }
}
