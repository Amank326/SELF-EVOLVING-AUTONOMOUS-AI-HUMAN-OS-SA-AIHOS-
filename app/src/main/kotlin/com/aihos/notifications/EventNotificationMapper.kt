package com.aihos.notifications

import android.content.Context
import com.aihos.interactions.UserResponseTracker
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Event-to-Notification Mapper - PHASE 3 STUB
 */
class EventNotificationMapper(
    private val context: Context,
    private val notificationManager: AINotificationManager,
    private val responseTracker: UserResponseTracker,
    private val scope: CoroutineScope
) {

    data class NotificationInfo(
        val type: String = "",
        val title: String = "",
        val message: String = "",
        val isCritical: Boolean = false
    )

    fun startListening() {
        Timber.d("Phase 3 stub: startListening")
    }

    fun stopListening() {
        Timber.d("Phase 3 stub: stopListening")
    }
}

