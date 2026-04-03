package com.aihos.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aihos.ui.MainActivity
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * NotificationManager for SA-AIHOS
 * Manages creation and delivery of AI system event notifications
 */
class AINotificationManager(private val context: Context) {

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // Auto-increment notification ID
    private val notificationIdCounter = AtomicInteger(1000)
    
    // Track notification history
    private val notificationHistory = mutableListOf<NotificationRecord>()
    private val maxHistorySize = 50

    data class NotificationRecord(
        val id: Int,
        val title: String,
        val message: String,
        val type: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Show notification for autonomy event
     */
    fun notifyAutonomyEvent(title: String, message: String) {
        val channelId = NotificationChannels.AUTONOMY_CHANNEL_ID
        val notification = buildNotification(
            channelId = channelId,
            title = title,
            message = message,
            eventType = "autonomy"
        )
        showNotification(notification, "autonomy")
        Timber.d("Autonomy notification: $title")
    }

    /**
     * Show notification for learning event
     */
    fun notifyLearningEvent(title: String, message: String) {
        val channelId = NotificationChannels.LEARNING_CHANNEL_ID
        val notification = buildNotification(
            channelId = channelId,
            title = title,
            message = message,
            eventType = "learning"
        )
        showNotification(notification, "learning")
        Timber.d("Learning notification: $title")
    }

    /**
     * Show notification for reasoning event
     */
    fun notifyReasoningEvent(title: String, message: String) {
        val channelId = NotificationChannels.REASONING_CHANNEL_ID
        val notification = buildNotification(
            channelId = channelId,
            title = title,
            message = message,
            eventType = "reasoning"
        )
        showNotification(notification, "reasoning")
        Timber.d("Reasoning notification: $title")
    }

    /**
     * Show alert notification for system issues
     */
    fun notifyAlert(title: String, message: String, critical: Boolean = false) {
        val channelId = NotificationChannels.ALERT_CHANNEL_ID
        val notification = buildNotification(
            channelId = channelId,
            title = title,
            message = message,
            eventType = if (critical) "critical_alert" else "alert",
            isAlert = true
        )
        showNotification(notification, "alert")
        Timber.w("Alert notification: $title - $message (Critical: $critical)")
    }

    /**
     * Build a notification with standard formatting
     */
    private fun buildNotification(
        channelId: String,
        title: String,
        message: String,
        eventType: String,
        isAlert: Boolean = false
    ): NotificationCompat.Builder {
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", eventType)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventType.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationChannels.getPriorityForChannel(channelId))
            .apply {
                if (isAlert) {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                }
            }
    }

    /**
     * Display notification and track it
     */
    private fun showNotification(builder: NotificationCompat.Builder, eventType: String) {
        try {
            val notificationId = notificationIdCounter.getAndIncrement()
            val notification = builder.build()
            val title = "SA-AIHOS"
            val message = "Event occurred"
            
            // Show notification
            notificationManager.notify(notificationId, notification)
            
            // Track in history
            addToHistory(
                NotificationRecord(
                    id = notificationId,
                    title = title,
                    message = message,
                    type = eventType
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error showing notification")
        }
    }

    /**
     * Add notification to history with size limit
     */
    private fun addToHistory(record: NotificationRecord) {
        notificationHistory.add(0, record) // Add to front
        if (notificationHistory.size > maxHistorySize) {
            notificationHistory.removeAt(notificationHistory.size - 1)
        }
    }

    /**
     * Get notification history
     */
    fun getNotificationHistory(limit: Int = 10): List<NotificationRecord> {
        return notificationHistory.take(limit)
    }

    /**
     * Cancel notification by ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Timber.d("Notification cancelled: $notificationId")
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Timber.d("All notifications cancelled")
    }

    /**
     * Get notification history as formatted strings
     */
    fun getHistoryAsStrings(limit: Int = 5): List<String> {
        return notificationHistory.take(limit).map { record ->
            "[${record.type.uppercase()}] ${record.title}: ${record.message}"
        }
    }
}
