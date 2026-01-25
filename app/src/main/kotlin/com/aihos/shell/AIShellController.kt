package com.aihos.shell

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.cognition.CognitionLoopManager
import com.aihos.ai.energy.EnergyAwarenessManager
import com.aihos.ai.perception.SystemSignalsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.max

/**
 * AIShellController: OS-Level AI Presence Manager
 *
 * Transforms SA-AIHOS from a traditional app into a persistent system layer
 * that acts as ambient intelligence across the entire device.
 *
 * Key Responsibilities:
 * - Manage long-lived AI state that persists across app sessions
 * - Coordinate cognition, energy, and system signals at OS level
 * - Provide entry points for other apps to interact with the AI
 * - Maintain always-visible presence (notification, overlay, launcher)
 * - React to system events and context changes
 * - Support AR/visualization overlay when requested
 *
 * Architecture Principle:
 * The AI is not "inside an app" - it's a system layer that apps can
 * interact with, similar to how Android OS provides system services.
 *
 * Implementation Strategy:
 * - Runs as long-lived foreground Service (AIShellService)
 * - Maintains persistent notification (always visible to user)
 * - Provides Intent-based interface for inter-app communication
 * - Manages overlay window (optional) for ambient visualization
 * - Monitors system context even when app is backgrounded
 *
 * User Experience:
 * - AI feels ambient/always-present, not app-specific
 * - Can interact via overlay, notification, quick settings, intents
 * - AI learns and adapts to device usage patterns
 * - Responds to system context (battery, thermal, app switches)
 * - Provides proactive insights or assistance
 */
interface AIShellController {
    /**
     * Initialize the AI shell system
     * Called once at device boot (via service)
     */
    suspend fun initialize()

    /**
     * Shutdown the AI shell system
     */
    suspend fun shutdown()

    /**
     * Get current shell state (running, paused, etc)
     */
    fun getShellState(): AIShellState

    /**
     * Get complete shell status for UI/monitoring
     */
    suspend fun getShellStatus(): AIShellStatus

    /**
     * Handle intent from another app or system
     */
    suspend fun handleIntent(intent: Intent): AIShellIntentResponse

    /**
     * React to foreground app change
     */
    suspend fun onForegroundAppChanged(packageName: String, activityName: String)

    /**
     * React to system event (battery critical, thermal throttling, etc)
     */
    suspend fun onSystemEventOccurred(event: SystemEvent)

    /**
     * Request AI to perform an action or provide insight
     */
    suspend fun requestAIAction(action: AIAction): AIActionResult

    /**
     * Get historical context about device usage
     */
    suspend fun getDeviceContextHistory(): DeviceContextHistory

    /**
     * Enable/disable overlay visualization
     */
    fun setOverlayEnabled(enabled: Boolean)

    /**
     * Get metrics about AI shell performance
     */
    fun getShellMetrics(): AIShellMetrics
}

/**
 * AI Shell state
 */
@Serializable
enum class AIShellState {
    INITIALIZING,    // Starting up
    READY,          // Running and available
    PAUSED,         // Temporarily paused by user
    SLEEPING,       // Device sleeping (screen off)
    ENERGY_SAVING,  // Reduced operation due to low power
    SHUTDOWN        // Shutting down
}

/**
 * System events that AI reacts to
 */
@Serializable
sealed class SystemEvent {
    data class AppForegroundChanged(
        val packageName: String,
        val activityName: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()

    data class ScreenStateChanged(
        val isOn: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()

    data class BatteryLow(
        val levelPercent: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()

    data class ThermalThrottling(
        val temperatureCelsius: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()

    data class UserInteraction(
        val type: String,  // "tap", "swipe", "voice", etc
        val context: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()

    data class NetworkStateChanged(
        val isConnected: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : SystemEvent()
}

/**
 * Actions that apps or system can request from AI
 */
@Serializable
sealed class AIAction {
    data class AnswerQuestion(val question: String) : AIAction()
    data class PerformTask(val taskDescription: String) : AIAction()
    data class GetInsight(val context: String) : AIAction()
    data class SuggestAction(val situation: String) : AIAction()
    data class LearnFromFeedback(val feedback: String) : AIAction()
}

/**
 * Response from AI action
 */
@Serializable
data class AIActionResult(
    val success: Boolean,
    val result: String,
    val confidence: Float = 0.8f,
    val shouldNotify: Boolean = false,
    val followUpSuggestions: List<String> = emptyList(),
    val processingTimeMs: Long = 0L
)

/**
 * Response to intent handler
 */
@Serializable
data class AIShellIntentResponse(
    val handled: Boolean,
    val action: String = "",
    val data: String = "",
    val resultCode: Int = -1,
    val extras: Map<String, String> = emptyMap()
)

/**
 * Complete AI shell status
 */
@Serializable
data class AIShellStatus(
    val shellState: AIShellState = AIShellState.INITIALIZING,
    val isServiceRunning: Boolean = false,
    val isOverlayVisible: Boolean = false,
    val currentForegroundApp: String = "unknown",
    val lastEventType: String = "initialization",
    val uptimeMinutes: Int = 0,
    val cognitionCyclesRun: Long = 0L,
    val deviceContextScore: Float = 0.5f,
    val estimatedAIWisdomScore: Float = 0.5f,
    val averageResponseTimeMs: Long = 0L,
    val recommendedNextAction: String = "",
    val visibilityMode: VisibilityMode = VisibilityMode.NOTIFICATION,
    val lastUpdateMs: Long = System.currentTimeMillis()
)

/**
 * How AI presents itself to user
 */
@Serializable
enum class VisibilityMode {
    NOTIFICATION,   // Always visible in status bar
    OVERLAY,        // Floating bubble/widget
    LAUNCHER,       // Dedicated launcher integration
    QUIET,          // Minimal visibility (only logs)
    ALWAYS_ON       // Multiple surfaces (notification + overlay + launcher)
}

/**
 * System events the AI reacts to
 */
@Serializable
data class DeviceContextHistory(
    val currentForegroundApp: String = "unknown",
    val appUsagePatterns: Map<String, AppUsageStats> = emptyMap(),
    val timeOfDayPattern: String = "unknown",
    val typicalAppSequences: List<List<String>> = emptyList(),
    val predictedNextApp: String = "",
    val deviceBehaviorTrends: String = "",
    val userPreferences: Map<String, String> = emptyMap()
)

@Serializable
data class AppUsageStats(
    val packageName: String,
    val timeSpentMinutes: Long = 0L,
    val openCount: Int = 0,
    val averageSessionMinutes: Float = 0f,
    val preferredTimeOfDay: String = "unknown"
)

/**
 * Metrics for AI shell performance
 */
@Serializable
data class AIShellMetrics(
    val totalCognitionCycles: Long = 0L,
    val totalIntentHandled: Int = 0,
    val averageResponseTimeMs: Long = 0L,
    val totalSystemEventsProcessed: Int = 0,
    val uptimeMinutes: Long = 0L,
    val overlayInteractions: Int = 0,
    val notificationInteractions: Int = 0,
    val intentInteractions: Int = 0,
    val averageAIWisdomScore: Float = 0.5f,
    val errorCount: Int = 0,
    val lastErrorMessage: String = ""
)

/**
 * Implementation of AIShellController
 *
 * Acts as the OS-level AI presence manager, coordinating:
 * - CognitionLoopManager (AI thinking)
 * - SystemSignalsManager (device context)
 * - EnergyAwarenessManager (power constraints)
 * - AutonomyController (decision making)
 * - AIShellService (persistent service)
 * - AIShellOverlayManager (visual presence)
 * - AIShellNotificationManager (notification presence)
 * - AIShellLauncher (quick access)
 */
class DefaultAIShellController(
    private val context: Context,
    private val cognitionLoopManager: CognitionLoopManager,
    private val systemSignalsManager: SystemSignalsManager,
    private val energyManager: EnergyAwarenessManager,
    private val autonomyController: AutonomyController?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : AIShellController, DefaultLifecycleObserver {

    private val _shellStateFlow = MutableStateFlow(AIShellState.INITIALIZING)
    val shellStateFlow: StateFlow<AIShellState> = _shellStateFlow.asStateFlow()

    private var isInitialized = false
    private var currentForegroundApp = "unknown"
    private var shellStartTimeMs = System.currentTimeMillis()
    private var cognitionCyclesRun = 0L
    private var intentHandledCount = 0
    private var totalResponseTimeMs = 0L
    private var totalSystemEvents = 0
    private var isOverlayEnabled = false

    private val appContextHistory = mutableMapOf<String, AppUsageStats>()
    private val systemEventHistory = ArrayDeque<SystemEvent>(maxSize = 100)
    private var lastAIWisdomScore = 0.5f

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🐚 AIShellController created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            shutdown()
        }
        Timber.i("🐚 AIShellController destroyed")
    }

    override suspend fun initialize() {
        if (isInitialized) return

        Timber.i("🐚 Initializing AI Shell system")
        _shellStateFlow.value = AIShellState.INITIALIZING

        try {
            // Start cognition loop if not already running
            cognitionLoopManager.startContinuousCognition()

            // Start monitoring system signals
            systemSignalsManager.startObserving()

            // Mark as initialized
            isInitialized = true
            _shellStateFlow.value = AIShellState.READY
            shellStartTimeMs = System.currentTimeMillis()

            Timber.i("🐚 AI Shell system initialized and ready")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Failed to initialize AI Shell")
            _shellStateFlow.value = AIShellState.SHUTDOWN
            throw e
        }
    }

    override suspend fun shutdown() {
        if (!isInitialized) return

        Timber.i("🐚 Shutting down AI Shell system")
        _shellStateFlow.value = AIShellState.SHUTDOWN

        try {
            cognitionLoopManager.stopContinuousCognition()
            systemSignalsManager.stopObserving()
            isInitialized = false
            Timber.i("🐚 AI Shell shutdown complete")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error during AI Shell shutdown")
        }
    }

    override fun getShellState(): AIShellState = _shellStateFlow.value

    override suspend fun getShellStatus(): AIShellStatus {
        val uptime = ((System.currentTimeMillis() - shellStartTimeMs) / 60000).toInt()
        val avgResponseTime = if (intentHandledCount > 0) {
            totalResponseTimeMs / intentHandledCount
        } else {
            0L
        }

        return AIShellStatus(
            shellState = _shellStateFlow.value,
            isServiceRunning = isInitialized,
            isOverlayVisible = isOverlayEnabled,
            currentForegroundApp = currentForegroundApp,
            uptimeMinutes = uptime,
            cognitionCyclesRun = cognitionCyclesRun,
            deviceContextScore = calculateDeviceContextScore(),
            estimatedAIWisdomScore = lastAIWisdomScore,
            averageResponseTimeMs = avgResponseTime,
            recommendedNextAction = getRecommendedAction(),
            visibilityMode = if (isOverlayEnabled) VisibilityMode.ALWAYS_ON else VisibilityMode.NOTIFICATION
        )
    }

    override suspend fun handleIntent(intent: Intent): AIShellIntentResponse {
        val startTime = System.currentTimeMillis()
        intentHandledCount++

        return try {
            when (intent.action) {
                "com.aihos.shell.ACTION_ASK_AI" -> {
                    val question = intent.getStringExtra("question") ?: ""
                    handleAskAIIntent(question, startTime)
                }
                "com.aihos.shell.ACTION_GET_STATUS" -> {
                    handleGetStatusIntent(startTime)
                }
                "com.aihos.shell.ACTION_REQUEST_ACTION" -> {
                    val actionType = intent.getStringExtra("actionType") ?: ""
                    handleRequestActionIntent(actionType, startTime)
                }
                "com.aihos.shell.ACTION_LEARN_FEEDBACK" -> {
                    val feedback = intent.getStringExtra("feedback") ?: ""
                    handleLearnFeedbackIntent(feedback, startTime)
                }
                else -> {
                    handleUnknownIntent(intent, startTime)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error handling intent: ${intent.action}")
            AIShellIntentResponse(
                handled = false,
                resultCode = -1
            )
        }
    }

    override suspend fun onForegroundAppChanged(packageName: String, activityName: String) {
        currentForegroundApp = packageName

        // Update app usage stats
        appContextHistory.getOrPut(packageName) {
            AppUsageStats(packageName)
        }.let { stats ->
            appContextHistory[packageName] = stats.copy(
                openCount = stats.openCount + 1
            )
        }

        // Record event
        systemEventHistory.addLast(
            SystemEvent.AppForegroundChanged(packageName, activityName)
        )

        Timber.i("🐚 Foreground app changed: $packageName / $activityName")

        // AI can react to app change
        onSystemEventOccurred(SystemEvent.AppForegroundChanged(packageName, activityName))
    }

    override suspend fun onSystemEventOccurred(event: SystemEvent) {
        totalSystemEvents++
        systemEventHistory.addLast(event)

        when (event) {
            is SystemEvent.BatteryLow -> {
                Timber.w("🐚 Battery low event: ${event.levelPercent}%")
                _shellStateFlow.value = AIShellState.ENERGY_SAVING
            }
            is SystemEvent.ThermalThrottling -> {
                Timber.w("🐚 Thermal throttling: ${event.temperatureCelsius}°C")
                // AI will reduce cognition via energy manager
            }
            is SystemEvent.ScreenStateChanged -> {
                if (!event.isOn) {
                    Timber.i("🐚 Screen turned off")
                    _shellStateFlow.value = AIShellState.SLEEPING
                } else {
                    Timber.i("🐚 Screen turned on")
                    if (_shellStateFlow.value == AIShellState.SLEEPING) {
                        _shellStateFlow.value = AIShellState.READY
                    }
                }
            }
            is SystemEvent.UserInteraction -> {
                Timber.d("🐚 User interaction: ${event.type}")
            }
            else -> {}
        }
    }

    override suspend fun requestAIAction(action: AIAction): AIActionResult {
        return try {
            when (action) {
                is AIAction.AnswerQuestion -> handleQuestionAction(action.question)
                is AIAction.PerformTask -> handleTaskAction(action.taskDescription)
                is AIAction.GetInsight -> handleInsightAction(action.context)
                is AIAction.SuggestAction -> handleSuggestionAction(action.situation)
                is AIAction.LearnFromFeedback -> handleLearningAction(action.feedback)
            }
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error processing AI action")
            AIActionResult(
                success = false,
                result = "Error: ${e.message}",
                confidence = 0f
            )
        }
    }

    override suspend fun getDeviceContextHistory(): DeviceContextHistory {
        return DeviceContextHistory(
            currentForegroundApp = currentForegroundApp,
            appUsagePatterns = appContextHistory.toMap(),
            predictedNextApp = predictNextApp(),
            typicalAppSequences = extractAppSequences()
        )
    }

    override fun setOverlayEnabled(enabled: Boolean) {
        isOverlayEnabled = enabled
        if (enabled) {
            Timber.i("🐚 Overlay enabled")
        } else {
            Timber.i("🐚 Overlay disabled")
        }
    }

    override fun getShellMetrics(): AIShellMetrics {
        val uptime = (System.currentTimeMillis() - shellStartTimeMs) / 60000
        val avgWisdom = lastAIWisdomScore

        return AIShellMetrics(
            totalCognitionCycles = cognitionCyclesRun,
            totalIntentHandled = intentHandledCount,
            averageResponseTimeMs = if (intentHandledCount > 0) totalResponseTimeMs / intentHandledCount else 0L,
            totalSystemEventsProcessed = totalSystemEvents,
            uptimeMinutes = uptime,
            averageAIWisdomScore = avgWisdom
        )
    }

    // Private helper methods

    private suspend fun handleAskAIIntent(question: String, startTime: Long): AIShellIntentResponse {
        val result = autonomyController?.triggerDecisionCycle(null) ?: "No autonomy controller"
        val elapsed = System.currentTimeMillis() - startTime
        totalResponseTimeMs += elapsed

        return AIShellIntentResponse(
            handled = true,
            action = "com.aihos.shell.ACTION_ASK_AI",
            data = result.toString(),
            resultCode = 0,
            extras = mapOf("responseTimeMs" to elapsed.toString())
        )
    }

    private suspend fun handleGetStatusIntent(startTime: Long): AIShellIntentResponse {
        val status = getShellStatus()
        val elapsed = System.currentTimeMillis() - startTime
        totalResponseTimeMs += elapsed

        return AIShellIntentResponse(
            handled = true,
            action = "com.aihos.shell.ACTION_GET_STATUS",
            data = "Ready: ${status.shellState}",
            resultCode = 0
        )
    }

    private suspend fun handleRequestActionIntent(actionType: String, startTime: Long): AIShellIntentResponse {
        val elapsed = System.currentTimeMillis() - startTime
        totalResponseTimeMs += elapsed

        return AIShellIntentResponse(
            handled = true,
            action = "com.aihos.shell.ACTION_REQUEST_ACTION",
            data = "Processing: $actionType",
            resultCode = 0
        )
    }

    private suspend fun handleLearnFeedbackIntent(feedback: String, startTime: Long): AIShellIntentResponse {
        val elapsed = System.currentTimeMillis() - startTime
        totalResponseTimeMs += elapsed
        lastAIWisdomScore = max(0.5f, lastAIWisdomScore + 0.05f)

        return AIShellIntentResponse(
            handled = true,
            action = "com.aihos.shell.ACTION_LEARN_FEEDBACK",
            data = "Learning from feedback",
            resultCode = 0
        )
    }

    private suspend fun handleUnknownIntent(intent: Intent, startTime: Long): AIShellIntentResponse {
        val elapsed = System.currentTimeMillis() - startTime
        totalResponseTimeMs += elapsed

        return AIShellIntentResponse(
            handled = false,
            action = intent.action ?: "unknown",
            resultCode = -1
        )
    }

    private suspend fun handleQuestionAction(question: String): AIActionResult {
        Timber.i("🐚 Handling question: $question")
        return AIActionResult(
            success = true,
            result = "Answer would go here",
            confidence = 0.7f,
            shouldNotify = true
        )
    }

    private suspend fun handleTaskAction(description: String): AIActionResult {
        Timber.i("🐚 Handling task: $description")
        return AIActionResult(
            success = true,
            result = "Task initiated",
            confidence = 0.6f
        )
    }

    private suspend fun handleInsightAction(context: String): AIActionResult {
        Timber.i("🐚 Generating insight for: $context")
        return AIActionResult(
            success = true,
            result = "Insight would go here",
            confidence = 0.8f,
            shouldNotify = true
        )
    }

    private suspend fun handleSuggestionAction(situation: String): AIActionResult {
        Timber.i("🐚 Suggesting action for: $situation")
        return AIActionResult(
            success = true,
            result = "Suggestion would go here",
            confidence = 0.7f
        )
    }

    private suspend fun handleLearningAction(feedback: String): AIActionResult {
        Timber.i("🐚 Learning from feedback: $feedback")
        lastAIWisdomScore = max(0.5f, lastAIWisdomScore + 0.05f)
        return AIActionResult(
            success = true,
            result = "Feedback incorporated",
            confidence = 0.9f
        )
    }

    private fun calculateDeviceContextScore(): Float {
        // Score based on how well we understand device context
        var score = 0.5f

        if (appContextHistory.size > 5) score += 0.1f
        if (systemEventHistory.size > 20) score += 0.1f
        if (currentForegroundApp != "unknown") score += 0.1f

        return score.coerceIn(0f, 1f)
    }

    private fun getRecommendedAction(): String {
        return when {
            currentForegroundApp.contains("email") -> "Check email summary"
            currentForegroundApp.contains("calendar") -> "Remind about upcoming events"
            currentForegroundApp.contains("phone") -> "Log call activity"
            else -> "Ready to assist"
        }
    }

    private fun predictNextApp(): String {
        // Simple prediction: most used app after current
        if (appContextHistory.isEmpty()) return "unknown"
        return appContextHistory.maxByOrNull { it.value.openCount }?.key ?: "unknown"
    }

    private fun extractAppSequences(): List<List<String>> {
        // Extract typical app usage sequences from history
        val sequences = mutableListOf<List<String>>()
        var currentSequence = mutableListOf<String>()

        for (event in systemEventHistory) {
            if (event is SystemEvent.AppForegroundChanged) {
                if (!currentSequence.contains(event.packageName)) {
                    currentSequence.add(event.packageName)
                } else {
                    if (currentSequence.size > 2) {
                        sequences.add(currentSequence.toList())
                    }
                    currentSequence = mutableListOf(event.packageName)
                }
            }
        }

        return sequences.takeLast(5)
    }
}

/**
 * Singleton accessor for AIShellController
 */
object AIShellSystem {
    private var controller: AIShellController? = null

    suspend fun initialize(
        context: Context,
        cognitionLoopManager: CognitionLoopManager,
        systemSignalsManager: SystemSignalsManager,
        energyManager: EnergyAwarenessManager,
        autonomyController: AutonomyController?
    ) {
        controller = DefaultAIShellController(
            context,
            cognitionLoopManager,
            systemSignalsManager,
            energyManager,
            autonomyController
        )
        controller?.initialize()
    }

    fun get(): AIShellController? = controller

    suspend fun shutdown() {
        controller?.shutdown()
        controller = null
    }
}
