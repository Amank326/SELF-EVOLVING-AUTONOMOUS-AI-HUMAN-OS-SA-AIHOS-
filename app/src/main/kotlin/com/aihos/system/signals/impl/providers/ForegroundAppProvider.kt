package com.aihos.system.signals.impl.providers

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground app signal provider.
 *
 * Responsibilities:
 * - Detect which app is currently in foreground
 * - Provide app package name via StateFlow
 * - Track app focus changes
 *
 * Behavior:
 * - Uses UsageStatsManager to query recent app usage
 * - Polls every 2 seconds (configurable)
 * - Returns package name of foreground app
 * - Returns "unknown" if unable to determine
 *
 * Security & Privacy:
 * - Requires PACKAGE_USAGE_STATS permission (already declared in AndroidManifest)
 * - Only queries aggregate usage stats (no detailed event logs)
 * - Does not track user interactions, only app focus
 *
 * Fallback:
 * - On Android 5.0+: UsageStatsManager is available
 * - On older APIs: Falls back to "unknown"
 *
 * Note: This provider uses polling instead of broadcasts because there's no
 * public broadcast for app focus changes. Accessibility Service could be more
 * efficient but requires user consent and invasive permissions.
 */
class ForegroundAppProvider(
    private val context: Context,
    private val pollIntervalMs: Long = 2_000L // Poll every 2 seconds
) {

    private val _value = MutableStateFlow("unknown")
    val value: StateFlow<String> = _value.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private var isRegistered = false

    /**
     * Start foreground app polling.
     */
    fun register() {
        if (isRegistered) {
            Timber.d("ForegroundAppProvider: Already registered, skipping")
            return
        }

        if (usageStatsManager == null) {
            Timber.w("ForegroundAppProvider: UsageStatsManager unavailable, skipping")
            return
        }

        try {
            isRegistered = true
            // Emit initial foreground app immediately
            updateForegroundApp()
            // Then start polling
            scope.launch {
                pollForegroundApp()
            }
            Timber.d("ForegroundAppProvider: Polling started")

        } catch (e: Exception) {
            Timber.e(e, "ForegroundAppProvider: Failed to start polling")
            isRegistered = false
            throw e
        }
    }

    /**
     * Stop foreground app polling.
     */
    fun unregister() {
        if (!isRegistered) {
            Timber.d("ForegroundAppProvider: Not registered, skipping")
            return
        }

        isRegistered = false
        Timber.d("ForegroundAppProvider: Polling stopped")
    }

    /**
     * Polling loop that runs continuously while registered.
     */
    private suspend fun pollForegroundApp() {
        while (isRegistered) {
            try {
                updateForegroundApp()
            } catch (e: Exception) {
                Timber.w(e, "ForegroundAppProvider: Error updating foreground app")
            }

            delay(pollIntervalMs)
        }
        Timber.d("ForegroundAppProvider: Polling loop stopped")
    }

    /**
     * Update current foreground app and emit.
     */
    private fun updateForegroundApp() {
        val foregroundApp = getForegroundApp()
        _value.tryEmit(foregroundApp)
        Timber.v("ForegroundAppProvider: Foreground app: $foregroundApp")
    }

    /**
     * Get package name of currently foreground app.
     * Uses UsageStatsManager to query recent app usage.
     *
     * Returns:
     * - Package name of foreground app (e.g., "com.android.chrome")
     * - "system" if system UI is foreground
     * - "unknown" if unable to determine
     */
    private fun getForegroundApp(): String {
        return try {
            if (usageStatsManager == null) return "unknown"

            // Query usage stats for last 1 second
            // This gets the most recently used app (foreground)
            val now = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 1_000,
                now
            )

            if (stats.isEmpty()) return "unknown"

            // Find the most recently used app
            val foregroundPackage = stats
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
                ?: return "unknown"

            // Filter out system packages
            if (isSystemPackage(foregroundPackage)) {
                "system"
            } else {
                foregroundPackage
            }

        } catch (e: SecurityException) {
            // SecurityException if app lacks PACKAGE_USAGE_STATS permission
            // Permission is declared in AndroidManifest but must be granted by user
            Timber.w(e, "ForegroundAppProvider: PACKAGE_USAGE_STATS permission not granted")
            "unknown"
        } catch (e: Exception) {
            Timber.w(e, "ForegroundAppProvider: Failed to get foreground app")
            "unknown"
        }
    }

    /**
     * Check if package is a system package.
     */
    private fun isSystemPackage(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }
}
