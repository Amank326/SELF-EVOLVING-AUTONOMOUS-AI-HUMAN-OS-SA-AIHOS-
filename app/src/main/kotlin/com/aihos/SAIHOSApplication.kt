package com.aihos

import android.app.Application
import com.aihos.notifications.AINotificationManager
import com.aihos.notifications.NotificationChannels
import timber.log.Timber

/**
 * Application entry point
 */
class SAIHOSApplication : Application() {
    
    // Notification system components
    lateinit var notificationManager: AINotificationManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        Timber.plant(Timber.DebugTree())
        Timber.d("SA-AIHOS Application initialized")

        // Initialize notification channels
        NotificationChannels.createChannels(this)
        Timber.d("Notification channels created")
        
        // Initialize notification manager
        notificationManager = AINotificationManager(this)
        Timber.d("Notification manager initialized")
        
        Timber.i("SA-AIHOS Application initialized with all services")
    }
}

