package com.aihos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point
 * Initializes Hilt and logging
 */
@HiltAndroidApp
class SAIHOSApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.i("SA-AIHOS Application initialized")
    }
}
