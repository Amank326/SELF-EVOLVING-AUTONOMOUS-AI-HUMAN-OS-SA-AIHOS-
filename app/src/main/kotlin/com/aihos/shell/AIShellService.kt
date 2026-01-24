package com.aihos.shell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aihos.MainActivity
import com.aihos.R
import com.aihos.ai.cognition.CognitionLoopManager
import com.aihos.ai.energy.EnergyAwarenessManager
import com.aihos.ai.perception.SystemSignalsManager
import com.aihos.ai.autonomy.AutonomyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * AIShellService: Persistent System-Level AI Service
 *
 * Ensures the AI shell remains active even when the app is backgrounded.
 * Acts as a system service that other apps can interact with.
 *
 * Key Features:
 * - Foreground service (always visible in notification)
 * - System intent handler (com.aihos.shell actions)
 * - Persistent across app lifecycle
 * - Graceful shutdown and restart
 * - Integration with Android lifecycle and power management
 *
 * Why Foreground Service?
 * - Reliable persistence (not killed by Android)
 * - Honest to user (notification shows it's running)
 * - Better battery optimization signals
 * - Can use wakelocks if needed for critical operations
 *
 * Notification Importance:
 * The persistent notification is not a "feature" - it's a requirement of
 * foreground services. It shows users that the AI is active, maintains
 * trust, and allows quick access to AI controls.
 *
 * Intent Protocol:
 * Other apps interact via Intent:
 *   - com.aihos.shell.ACTION_ASK_AI (question: String)
 *   - com.aihos.shell.ACTION_GET_STATUS
 *   - com.aihos.shell.ACTION_REQUEST_ACTION (actionType: String)
 *   - com.aihos.shell.ACTION_LEARN_FEEDBACK (feedback: String)
 */
class AIShellService : Service() {

    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var shellController: AIShellController? = null
    private var isInitialized = false

    companion object {
        private const val NOTIFICATION_ID = 9001
        private const val NOTIFICATION_CHANNEL_ID = "ai_shell_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "AI Shell Service"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("🐚 AIShellService created")

        // Create notification channel (required for Android 8+)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("🐚 AIShellService started with intent: ${intent?.action}")

        // Initialize on first start
        if (!isInitialized) {
            serviceScope.launch {
                try {
                    initializeShellController()
                    isInitialized = true

                    // Start as foreground service
                    startForegroundService()

                    Timber.i("🐚 AIShellService fully initialized")
                } catch (e: Exception) {
                    Timber.e(e, "🐚 Failed to initialize AIShellService")
                    stopSelf()
                }
            }
        }

        // Handle specific intents
        intent?.let { handleIntent(it) }

        // Service should be restarted if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // For now, we don't support binding
        // Could extend to support bound service in future
        return null
    }

    override fun onDestroy() {
        Timber.i("🐚 AIShellService destroyed")
        serviceScope.launch {
            shutdownShellController()
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    /**
     * Initialize the AI shell controller
     */
    private suspend fun initializeShellController() {
        try {
            // Get dependencies from application or create them
            val cognitionLoopManager = getCognitionLoopManager()
            val systemSignalsManager = getSystemSignalsManager()
            val energyManager = getEnergyAwarenessManager()
            val autonomyController = getAutonomyController()

            if (cognitionLoopManager == null || systemSignalsManager == null) {
                Timber.w("🐚 Missing required dependencies for AIShellController")
                return
            }

            // Initialize shell system
            AIShellSystem.initialize(
                context = this,
                cognitionLoopManager = cognitionLoopManager,
                systemSignalsManager = systemSignalsManager,
                energyManager = energyManager ?: return,
                autonomyController = autonomyController
            )

            shellController = AIShellSystem.get()

            Timber.i("🐚 AIShellController initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error initializing AIShellController")
            throw e
        }
    }

    /**
     * Shutdown the AI shell controller
     */
    private suspend fun shutdownShellController() {
        try {
            AIShellSystem.shutdown()
            shellController = null
            Timber.i("🐚 AIShellController shutdown complete")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error shutting down AIShellController")
        }
    }

    /**
     * Start as foreground service with persistent notification
     */
    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        Timber.i("🐚 Foreground service started")
    }

    /**
     * Build the persistent notification
     */
    private fun buildNotification(): Notification {
        // Create intent to open app
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent to pause/stop AI
        val pauseIntent = Intent(this, AIShellService::class.java).apply {
            action = "com.aihos.shell.ACTION_PAUSE"
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI Assistant Active")
            .setContentText("Ambient intelligence is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.purple_500))
            .setContentIntent(appPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Open",
                appPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Pause",
                pausePendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Create notification channel (required for Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI Shell service notifications"
                enableVibration(false)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Timber.i("🐚 Notification channel created")
        }
    }

    /**
     * Handle incoming intents
     */
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "com.aihos.shell.ACTION_ASK_AI" -> {
                serviceScope.launch {
                    handleAskAI(intent)
                }
            }
            "com.aihos.shell.ACTION_GET_STATUS" -> {
                serviceScope.launch {
                    handleGetStatus(intent)
                }
            }
            "com.aihos.shell.ACTION_PAUSE" -> {
                handlePause()
            }
            "com.aihos.shell.ACTION_RESUME" -> {
                handleResume()
            }
            else -> {
                serviceScope.launch {
                    shellController?.handleIntent(intent)
                }
            }
        }
    }

    /**
     * Handle ASK_AI intent from another app
     */
    private suspend fun handleAskAI(intent: Intent) {
        try {
            val question = intent.getStringExtra("question") ?: return
            Timber.i("🐚 Handling AI question: $question")

            val response = shellController?.handleIntent(intent)
            Timber.i("🐚 AI response: ${response?.data}")

            // Could broadcast result to requesting app
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error handling AI question")
        }
    }

    /**
     * Handle GET_STATUS intent
     */
    private suspend fun handleGetStatus(intent: Intent) {
        try {
            val status = shellController?.getShellStatus()
            Timber.i("🐚 Shell status: ${status?.shellState}")

            // Could broadcast status to requesting app
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error getting shell status")
        }
    }

    /**
     * Pause AI operations
     */
    private fun handlePause() {
        Timber.i("🐚 Pausing AI operations")
        // Could notify shell controller to pause
        updateNotificationStatus("Paused")
    }

    /**
     * Resume AI operations
     */
    private fun handleResume() {
        Timber.i("🐚 Resuming AI operations")
        // Could notify shell controller to resume
        updateNotificationStatus("Active")
    }

    /**
     * Update notification status text
     */
    private fun updateNotificationStatus(status: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI Assistant")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    // Get dependencies (would be injected in real implementation)
    private fun getCognitionLoopManager(): CognitionLoopManager? {
        return try {
            // In real implementation, get from application/DI container
            (application as? com.aihos.SAIHOSApplication)?.getCognitionLoopManager()
        } catch (e: Exception) {
            null
        }
    }

    private fun getSystemSignalsManager(): SystemSignalsManager? {
        return try {
            (application as? com.aihos.SAIHOSApplication)?.getSystemSignalsManager()
        } catch (e: Exception) {
            null
        }
    }

    private fun getEnergyAwarenessManager(): EnergyAwarenessManager? {
        return try {
            (application as? com.aihos.SAIHOSApplication)?.getEnergyAwarenessManager()
        } catch (e: Exception) {
            null
        }
    }

    private fun getAutonomyController(): AutonomyController? {
        return try {
            (application as? com.aihos.SAIHOSApplication)?.getAutonomyController()
        } catch (e: Exception) {
            null
        }
    }
}
