package com.aihos.selfevolving

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Hilt initialization
 */
@HiltAndroidApp
class AiHosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}
