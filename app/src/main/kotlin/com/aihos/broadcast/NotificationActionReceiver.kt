package com.aihos.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Broadcast receiver for handling notification actions
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISSED = "com.aihos.notification.DISMISSED"
        const val ACTION_CLICKED = "com.aihos.notification.CLICKED"
        const val ACTION_ACTION = "com.aihos.notification.ACTION"

        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_ACTION_TYPE = "action_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: "unknown"

        when (intent.action) {
            ACTION_DISMISSED -> {
                Timber.d("Notification $notificationId dismissed")
                handleDismissed(notificationId)
            }
            ACTION_CLICKED -> {
                Timber.d("Notification $notificationId clicked")
                handleClicked(context, notificationId)
            }
            ACTION_ACTION -> {
                Timber.d("Notification $notificationId action: $actionType")
                handleAction(context, notificationId, actionType)
            }
        }
    }

    private fun handleDismissed(notificationId: Int) {
        // Log notification dismissal for analytics
        Timber.d("User dismissed notification: $notificationId")
    }

    private fun handleClicked(context: Context, notificationId: Int) {
        // Open main activity when notification is clicked
        val intent = Intent(context, Class.forName("com.aihos.ui.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        context.startActivity(intent)
    }

    private fun handleAction(context: Context, notificationId: Int, actionType: String) {
        when (actionType) {
            "view_details" -> {
                Timber.d("View details action for notification $notificationId")
                handleClicked(context, notificationId)
            }
            "dismiss" -> {
                Timber.d("Dismiss action for notification $notificationId")
            }
            "approve" -> {
                Timber.d("Approve action for notification $notificationId")
                // Handle approval action for autonomous decisions
            }
            "reject" -> {
                Timber.d("Reject action for notification $notificationId")
                // Handle rejection action for autonomous decisions
            }
            else -> {
                Timber.w("Unknown action type: $actionType")
            }
        }
    }
}

