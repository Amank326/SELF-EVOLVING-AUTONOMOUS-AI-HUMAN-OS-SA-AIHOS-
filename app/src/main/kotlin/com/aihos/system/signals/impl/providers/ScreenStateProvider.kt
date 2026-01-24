package com.aihos.system.signals.impl.providers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Screen on/off state signal provider.
 *
 * Responsibilities:
 * - Listen to screen on/off broadcasts
 * - Update screen state
 * - Ensure broadcast receiver is properly unregistered
 * - Provide current screen state via StateFlow
 *
 * Safety:
 * - Broadcast receiver is unregistered in unregister() to prevent memory leaks
 * - Thread-safe StateFlow
 *
 * Behavior:
 * - Emits immediately on first registration (queries screen state if possible)
 * - Emits on ACTION_SCREEN_ON and ACTION_SCREEN_OFF broadcasts
 * - Critical for AI cognition: Reduced reasoning when screen is off
 *
 * Note: Initial screen state is set to true (conservative assumption: screen likely on when app starts)
 */
class ScreenStateProvider(private val context: Context) {

    private val _value = MutableStateFlow(true) // Default: assume screen is on
    val value: StateFlow<Boolean> = _value.asStateFlow()

    private var receiver: ScreenStateBroadcastReceiver? = null
    private var isRegistered = false

    /**
     * Register screen state broadcast receiver.
     * Safe: Checks if already registered.
     */
    fun register() {
        if (isRegistered) {
            Timber.d("ScreenStateProvider: Already registered, skipping")
            return
        }

        try {
            // Create and register receiver
            receiver = ScreenStateBroadcastReceiver { isScreenOn ->
                _value.tryEmit(isScreenOn)
            }

            // Listen to both screen on and screen off events
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }

            context.registerReceiver(receiver, filter)
            isRegistered = true

            Timber.d("ScreenStateProvider: Registered successfully, initial state: ON (assumed)")

        } catch (e: Exception) {
            Timber.e(e, "ScreenStateProvider: Failed to register screen state receiver")
            receiver = null
            isRegistered = false
            throw e
        }
    }

    /**
     * Unregister screen state broadcast receiver.
     * Safe: Checks if registered before unregistering.
     * Critical: Prevents memory leak from unmanaged broadcast receiver.
     */
    fun unregister() {
        if (!isRegistered || receiver == null) {
            Timber.d("ScreenStateProvider: Not registered or already unregistered")
            return
        }

        try {
            context.unregisterReceiver(receiver)
            isRegistered = false
            receiver = null
            Timber.d("ScreenStateProvider: Unregistered successfully")

        } catch (e: Exception) {
            Timber.w(e, "ScreenStateProvider: Failed to unregister")
            isRegistered = false
            receiver = null
        }
    }

    /**
     * Broadcast receiver for screen on/off events.
     */
    private class ScreenStateBroadcastReceiver(
        private val onScreenStateChange: (Boolean) -> Unit
    ) : android.content.BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            try {
                val isScreenOn = when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        Timber.d("ScreenStateProvider: Screen turned ON")
                        true
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Timber.d("ScreenStateProvider: Screen turned OFF")
                        false
                    }
                    else -> return
                }

                onScreenStateChange(isScreenOn)

            } catch (e: Exception) {
                Timber.e(e, "ScreenStateProvider: Error processing screen state broadcast")
            }
        }
    }
}
