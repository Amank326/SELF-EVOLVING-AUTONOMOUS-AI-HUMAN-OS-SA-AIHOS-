package com.aihos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * Notification Channel definitions for SA-AIHOS
 * Defines 4 distinct channels for different AI events
 */
object NotificationChannels {
    
    // Channel IDs
    const val AUTONOMY_CHANNEL_ID = "aihos_autonomy_channel"
    const val LEARNING_CHANNEL_ID = "aihos_learning_channel"
    const val REASONING_CHANNEL_ID = "aihos_reasoning_channel"
    const val ALERT_CHANNEL_ID = "aihos_alert_channel"

    /**
     * Create all notification channels
     * Must be called on app startup (in Application.onCreate)
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createAutonomyChannel(context)
            createLearningChannel(context)
            createReasoningChannel(context)
            createAlertChannel(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAutonomyChannel(context: Context) {
        val channel = NotificationChannel(
            AUTONOMY_CHANNEL_ID,
            "Autonomy Actions",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about autonomous AI decisions and goal execution"
            enableVibration(true)
            setShowBadge(true)
            lightColor = 0xFFFF6600.toInt() // Orange
        }
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLearningChannel(context: Context) {
        val channel = NotificationChannel(
            LEARNING_CHANNEL_ID,
            "System Learning",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about memory consolidation and learning events"
            enableVibration(false)
            setShowBadge(true)
            lightColor = 0xFF00FF88.toInt() // Cyan
        }
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createReasoningChannel(context: Context) {
        val channel = NotificationChannel(
            REASONING_CHANNEL_ID,
            "Reasoning Events",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications about reasoning cycles and inference completion"
            enableVibration(false)
            setShowBadge(false)
            lightColor = 0xFF6600FF.toInt() // Purple
        }
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createAlertChannel(context: Context) {
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "System Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical alerts about system health and errors"
            enableVibration(true)
            setShowBadge(true)
            lightColor = 0xFFFF0000.toInt() // Red
        }
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Map event type to channel
     */
    fun getChannelForEventType(eventType: String): String {
        return when {
            eventType.contains("autonomy", ignoreCase = true) -> AUTONOMY_CHANNEL_ID
            eventType.contains("learning", ignoreCase = true) || 
            eventType.contains("memory", ignoreCase = true) -> LEARNING_CHANNEL_ID
            eventType.contains("reasoning", ignoreCase = true) -> REASONING_CHANNEL_ID
            eventType.contains("error", ignoreCase = true) || 
            eventType.contains("alert", ignoreCase = true) || 
            eventType.contains("critical", ignoreCase = true) -> ALERT_CHANNEL_ID
            else -> LEARNING_CHANNEL_ID // Default to learning
        }
    }

    /**
     * Get priority for channel
     */
    fun getPriorityForChannel(channelId: String): Int {
        return when (channelId) {
            ALERT_CHANNEL_ID -> NotificationCompat.PRIORITY_HIGH
            AUTONOMY_CHANNEL_ID -> NotificationCompat.PRIORITY_DEFAULT
            REASONING_CHANNEL_ID -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
}
