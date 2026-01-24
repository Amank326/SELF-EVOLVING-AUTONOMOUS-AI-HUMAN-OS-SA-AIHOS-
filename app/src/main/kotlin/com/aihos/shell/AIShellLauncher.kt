package com.aihos.shell

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * AIShellLauncher: Quick-Access Interface
 *
 * Provides quick access to AI capabilities without opening the full app.
 * Can be integrated with:
 * - App launcher (as a separate launcher)
 * - Quick settings tiles
 * - Notification actions
 * - Voice assistant integration
 * - System search integration
 *
 * Quick Actions:
 * - Ask question (voice or text)
 * - Get current insight
 * - Request device action
 * - View AI status
 * - Access settings
 *
 * Design:
 * - Minimal, unobtrusive interface
 * - Fast launch (1-2 seconds max)
 * - Context-aware suggestions
 * - Integration with system services
 */
interface AIShellLauncher {
    /**
     * Initialize launcher
     */
    suspend fun initialize()

    /**
     * Get available quick actions
     */
    fun getQuickActions(): List<QuickAction>

    /**
     * Get suggested actions based on context
     */
    suspend fun getSuggestedActions(): List<QuickAction>

    /**
     * Execute a quick action
     */
    suspend fun executeAction(action: QuickAction): ActionResult

    /**
     * Get launcher metadata
     */
    fun getLauncherInfo(): LauncherInfo

    /**
     * Show voice query interface
     */
    suspend fun showVoiceQuery()

    /**
     * Show text query interface
     */
    suspend fun showTextQuery()

    /**
     * Register launcher callback
     */
    fun setLauncherListener(listener: LauncherListener)
}

/**
 * Quick action that can be executed
 */
@Serializable
data class QuickAction(
    val id: String,
    val label: String,
    val description: String,
    val icon: String = "ic_action",
    val category: ActionCategory = ActionCategory.OTHER,
    val priority: Int = 0,  // Higher priority shown first
    val isContextual: Boolean = false,  // Based on current context
    val actionType: String = ""  // Internal action type
)

@Serializable
enum class ActionCategory {
    QUERY,        // Ask questions
    CONTROL,      // Device control
    INSIGHT,      // Get insights
    SUGGESTION,   // Suggestions
    SETTINGS,     // Settings
    OTHER
}

/**
 * Result of action execution
 */
@Serializable
data class ActionResult(
    val success: Boolean,
    val message: String = "",
    val shouldNotify: Boolean = false,
    val followUpActions: List<QuickAction> = emptyList(),
    val processingTimeMs: Long = 0L
)

/**
 * Launcher information
 */
@Serializable
data class LauncherInfo(
    val name: String = "AI Shell Launcher",
    val version: String = "1.0",
    val isAvailable: Boolean = true,
    val totalActions: Int = 0,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

/**
 * Callback for launcher events
 */
interface LauncherListener {
    fun onActionExecuted(action: QuickAction, result: ActionResult)
    fun onLauncherOpen()
    fun onLauncherClosed()
    fun onActionSelected(action: QuickAction)
}

/**
 * Implementation of AIShellLauncher
 */
class DefaultAIShellLauncher(
    private val context: Context,
    private val shellController: AIShellController? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : AIShellLauncher, DefaultLifecycleObserver {

    private var launcherListener: LauncherListener? = null
    private val quickActions = mutableListOf<QuickAction>()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🐚 AIShellLauncher created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.i("🐚 AIShellLauncher destroyed")
    }

    override suspend fun initialize() {
        Timber.i("🐚 Initializing AI Shell Launcher")

        // Initialize quick actions
        initializeQuickActions()

        Timber.i("🐚 AI Shell Launcher initialized")
    }

    override fun getQuickActions(): List<QuickAction> = quickActions.toList()

    override suspend fun getSuggestedActions(): List<QuickAction> {
        // Get context from shell controller
        val context = shellController?.getDeviceContextHistory()

        // Suggest actions based on context
        return quickActions.filter { action ->
            when {
                context?.currentForegroundApp?.contains("mail") == true &&
                        action.label.contains("Email") -> true
                context?.currentForegroundApp?.contains("calendar") == true &&
                        action.label.contains("Calendar") -> true
                else -> action.priority > 0
            }
        }.sortedByDescending { it.priority }
    }

    override suspend fun executeAction(action: QuickAction): ActionResult {
        val startTime = System.currentTimeMillis()

        Timber.i("🐚 Executing action: ${action.label}")

        return try {
            when (action.actionType) {
                "ask_question" -> executeAskQuestion(action)
                "get_insight" -> executeGetInsight(action)
                "control_device" -> executeControlDevice(action)
                "show_settings" -> executeShowSettings(action)
                else -> ActionResult(
                    success = false,
                    message = "Unknown action type: ${action.actionType}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error executing action: ${action.label}")
            ActionResult(
                success = false,
                message = "Error: ${e.message}",
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }.also { result ->
            // Notify listener
            launcherListener?.onActionExecuted(action, result)
        }
    }

    override fun getLauncherInfo(): LauncherInfo {
        return LauncherInfo(
            isAvailable = shellController != null,
            totalActions = quickActions.size
        )
    }

    override suspend fun showVoiceQuery() {
        Timber.i("🐚 Showing voice query interface")
        // Would launch voice input interface
    }

    override suspend fun showTextQuery() {
        Timber.i("🐚 Showing text query interface")
        // Would launch text input interface
    }

    override fun setLauncherListener(listener: LauncherListener) {
        this.launcherListener = listener
    }

    /**
     * Initialize default quick actions
     */
    private fun initializeQuickActions() {
        quickActions.clear()

        quickActions.add(
            QuickAction(
                id = "action_ask_question",
                label = "Ask AI",
                description = "Ask the AI a question",
                category = ActionCategory.QUERY,
                priority = 10,
                actionType = "ask_question"
            )
        )

        quickActions.add(
            QuickAction(
                id = "action_get_insight",
                label = "Get Insight",
                description = "Get AI insight about current situation",
                category = ActionCategory.INSIGHT,
                priority = 8,
                actionType = "get_insight"
            )
        )

        quickActions.add(
            QuickAction(
                id = "action_battery_status",
                label = "Battery Status",
                description = "Check battery and device status",
                category = ActionCategory.QUERY,
                priority = 6,
                actionType = "ask_question"
            )
        )

        quickActions.add(
            QuickAction(
                id = "action_device_control",
                label = "Device Control",
                description = "Control device settings",
                category = ActionCategory.CONTROL,
                priority = 5,
                actionType = "control_device"
            )
        )

        quickActions.add(
            QuickAction(
                id = "action_ai_settings",
                label = "AI Settings",
                description = "Configure AI Shell settings",
                category = ActionCategory.SETTINGS,
                priority = 3,
                actionType = "show_settings"
            )
        )

        quickActions.add(
            QuickAction(
                id = "action_ai_status",
                label = "AI Status",
                description = "View AI system status",
                category = ActionCategory.QUERY,
                priority = 7,
                actionType = "ask_question"
            )
        )

        Timber.i("🐚 Initialized ${quickActions.size} quick actions")
    }

    /**
     * Execute ask question action
     */
    private suspend fun executeAskQuestion(action: QuickAction): ActionResult {
        Timber.i("🐚 Executing ask question action")

        shellController?.let { controller ->
            val result = controller.requestAIAction(
                AIAction.AnswerQuestion("Tell me about current device status")
            )

            return ActionResult(
                success = result.success,
                message = result.result,
                shouldNotify = result.shouldNotify
            )
        }

        return ActionResult(success = false, message = "Shell controller not available")
    }

    /**
     * Execute get insight action
     */
    private suspend fun executeGetInsight(action: QuickAction): ActionResult {
        Timber.i("🐚 Executing get insight action")

        shellController?.let { controller ->
            val result = controller.requestAIAction(
                AIAction.GetInsight("Current device and user context")
            )

            return ActionResult(
                success = result.success,
                message = result.result,
                shouldNotify = result.shouldNotify,
                followUpActions = listOf(
                    QuickAction(
                        id = "follow_up_1",
                        label = "Tell me more",
                        description = "Get more details",
                        actionType = "ask_question"
                    )
                )
            )
        }

        return ActionResult(success = false, message = "Shell controller not available")
    }

    /**
     * Execute control device action
     */
    private suspend fun executeControlDevice(action: QuickAction): ActionResult {
        Timber.i("🐚 Executing control device action")
        // Would open device control interface
        return ActionResult(success = true, message = "Device control interface opened")
    }

    /**
     * Execute show settings action
     */
    private suspend fun executeShowSettings(action: QuickAction): ActionResult {
        Timber.i("🐚 Executing show settings action")
        // Would open AI settings
        return ActionResult(success = true, message = "Settings opened")
    }
}

/**
 * Quick settings tile for AI Shell
 */
class AIShellQuickSettingsTile(private val context: Context) {
    /**
     * Build quick settings tile intent
     */
    fun buildTileIntent(): Intent {
        return Intent().apply {
            action = "android.service.quicksettings.action.QS_TILE"
            putExtra("ai_shell_enabled", true)
        }
    }

    /**
     * Create tile label
     */
    fun getTileLabel(): String = "AI Shell"

    /**
     * Create tile description
     */
    fun getTileDescription(): String = "AI Shell - Ambient Intelligence"
}

/**
 * Singleton accessor for AIShellLauncher
 */
object AIShellLauncherSystem {
    private var launcher: AIShellLauncher? = null

    suspend fun initialize(
        context: Context,
        shellController: AIShellController? = null
    ): AIShellLauncher {
        if (launcher == null) {
            launcher = DefaultAIShellLauncher(context, shellController)
            launcher?.initialize()
        }
        return launcher!!
    }

    fun get(): AIShellLauncher? = launcher

    fun shutdown() {
        launcher = null
    }
}
