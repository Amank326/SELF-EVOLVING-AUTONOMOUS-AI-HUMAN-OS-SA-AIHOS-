package com.aihos.interactions

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * User Response Tracker - PHASE 3 STUB
 */
class UserResponseTracker(private val context: Context) {

    data class UserResponse(
        val eventId: String = "",
        val notificationType: String = "",
        val actionType: String = "",
        val metadata: Map<String, String> = emptyMap()
    )

    data class EngagementMetrics(
        val notificationsViewed: Int = 0,
        val notificationsDismissed: Int = 0,
        val notificationsActioned: Int = 0,
        val averageResponseTime: Float = 0f
    )

    private val _userResponses = MutableStateFlow<List<UserResponse>>(emptyList())
    val userResponses: StateFlow<List<UserResponse>> = _userResponses.asStateFlow()

    private val _engagementMetrics = MutableStateFlow(EngagementMetrics())
    val engagementMetrics: StateFlow<EngagementMetrics> = _engagementMetrics.asStateFlow()

    fun trackNotificationViewed(eventId: String, notificationType: String, metadata: Map<String, String> = emptyMap()) {
        Timber.d("Phase 3 stub: trackNotificationViewed")
    }

    fun trackNotificationDismissed(eventId: String, notificationType: String, timeToActionMs: Long = 0L, metadata: Map<String, String> = emptyMap()) {
        Timber.d("Phase 3 stub: trackNotificationDismissed")
    }

    fun trackNotificationActioned(eventId: String, notificationType: String, actionId: String, metadata: Map<String, String> = emptyMap()) {
        Timber.d("Phase 3 stub: trackNotificationActioned")
    }

    fun getMetrics(): EngagementMetrics = engagementMetrics.value

    fun clearHistory() {
        Timber.d("Phase 3 stub: clearHistory")
    }
}
