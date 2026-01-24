package com.aihos.shell

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AIShellOverlayManager: Persistent Floating Window Interface
 *
 * Creates and manages an always-visible floating bubble/widget that
 * allows users to interact with the AI shell without opening the app.
 *
 * Key Features:
 * - Floating bubble that stays on top of other apps
 * - Drag to reposition, tap to interact
 * - Minimal when collapsed, expandable for commands
 * - Shows AI status and quick actions
 * - Respects user space (dismissible)
 * - Integrates with system notifications
 *
 * Design Philosophy:
 * The overlay should feel ambient and non-intrusive. It should:
 * - Take minimal screen space when not needed
 * - Respond quickly to user touch
 * - Show AI status at a glance
 * - Allow quick access to common actions
 * - Never force user attention (no annoying animations)
 *
 * Technical Approach:
 * Uses WindowManager.LayoutParams to draw on top of all apps.
 * Implements gesture detection (tap, drag, long-press).
 * Updates reactively based on AI state changes.
 *
 * Android 11+ Considerations:
 * - Requires SYSTEM_ALERT_WINDOW permission
 * - May need user approval for overlay permission
 * - Respects Android 11+ overlay restrictions
 */
interface AIShellOverlayManager {
    /**
     * Show the overlay bubble
     */
    fun showOverlay()

    /**
     * Hide the overlay bubble
     */
    fun hideOverlay()

    /**
     * Check if overlay is currently visible
     */
    fun isOverlayVisible(): Boolean

    /**
     * Update overlay appearance based on AI state
     */
    suspend fun updateOverlayState(state: AIShellOverlayState)

    /**
     * Move overlay to new position
     */
    fun moveOverlay(x: Int, y: Int)

    /**
     * Set callback for user interactions
     */
    fun setInteractionListener(listener: OverlayInteractionListener)
}

/**
 * Overlay state and appearance
 */
@Serializable
enum class AIShellOverlayState {
    IDLE,           // Minimal, collapsed bubble
    ACTIVE,         // Shows activity/thinking
    READY,          // Ready for interaction, slightly expanded
    LISTENING,      // Waiting for voice input
    PROCESSING,     // Processing user request
    RESPONDING,     // Showing response/suggestion
    ERROR           // Error state, needs attention
}

/**
 * Callback for user interactions with overlay
 */
interface OverlayInteractionListener {
    fun onTapped()
    fun onLongPressed()
    fun onDragged(x: Int, y: Int)
    fun onDismissed()
    fun onActionSelected(action: String)
}

/**
 * Implementation of AIShellOverlayManager
 */
class DefaultAIShellOverlayManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : AIShellOverlayManager, DefaultLifecycleObserver {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isVisible = false
    private var currentX = 0
    private var currentY = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val _overlayStateFlow = MutableStateFlow(AIShellOverlayState.IDLE)
    val overlayStateFlow: StateFlow<AIShellOverlayState> = _overlayStateFlow.asStateFlow()

    private var interactionListener: OverlayInteractionListener? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.i("🐚 AIShellOverlayManager created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        hideOverlay()
        Timber.i("🐚 AIShellOverlayManager destroyed")
    }

    override fun showOverlay() {
        if (isVisible) return

        try {
            Timber.i("🐚 Showing AI Shell overlay")

            if (overlayView == null) {
                overlayView = createOverlayView()
            }

            val params = createLayoutParams()
            windowManager.addView(overlayView, params)
            isVisible = true

            Timber.i("🐚 Overlay displayed")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Failed to show overlay")
        }
    }

    override fun hideOverlay() {
        if (!isVisible) return

        try {
            Timber.i("🐚 Hiding AI Shell overlay")
            overlayView?.let {
                windowManager.removeView(it)
            }
            isVisible = false
            Timber.i("🐚 Overlay hidden")
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error hiding overlay")
        }
    }

    override fun isOverlayVisible(): Boolean = isVisible

    override suspend fun updateOverlayState(state: AIShellOverlayState) {
        _overlayStateFlow.value = state

        overlayView?.let { view ->
            updateOverlayAppearance(view, state)
        }

        Timber.d("🐚 Overlay state updated: $state")
    }

    override fun moveOverlay(x: Int, y: Int) {
        if (!isVisible) return

        try {
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                params.x = x
                params.y = y
                windowManager.updateViewLayout(view, params)

                currentX = x
                currentY = y
            }
        } catch (e: Exception) {
            Timber.e(e, "🐚 Error moving overlay")
        }
    }

    override fun setInteractionListener(listener: OverlayInteractionListener) {
        this.interactionListener = listener
    }

    /**
     * Create the overlay view
     */
    private fun createOverlayView(): View {
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                200,  // width in pixels
                200,  // height in pixels
            )
        }

        // Create bubble view
        val bubble = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(200, 200)
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(android.R.drawable.btn_default)
            gravity = Gravity.CENTER
        }

        // Status indicator
        val statusView = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16)
            setBackgroundColor(android.graphics.Color.GREEN)
        }

        // Status text
        val statusText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "AI"
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
        }

        bubble.addView(statusView)
        bubble.addView(statusText)
        container.addView(bubble)

        // Set up touch listener for interactions
        container.setOnTouchListener { v, event ->
            handleTouchEvent(event)
            true
        }

        Timber.i("🐚 Overlay view created")
        return container
    }

    /**
     * Handle touch events on overlay
     */
    private fun handleTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > 10) {  // Drag threshold
                    isDragging = true
                    moveOverlay(
                        (currentX + dx).toInt(),
                        (currentY + dy).toInt()
                    )
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Single tap
                    interactionListener?.onTapped()
                    Timber.d("🐚 Overlay tapped")
                } else {
                    // End of drag
                    interactionListener?.onDragged(currentX, currentY)
                    Timber.d("🐚 Overlay dragged to: ($currentX, $currentY)")
                }
            }
        }
    }

    /**
     * Update overlay appearance based on state
     */
    private fun updateOverlayAppearance(view: View, state: AIShellOverlayState) {
        val statusColor = when (state) {
            AIShellOverlayState.IDLE -> android.graphics.Color.GRAY
            AIShellOverlayState.ACTIVE -> android.graphics.Color.BLUE
            AIShellOverlayState.READY -> android.graphics.Color.GREEN
            AIShellOverlayState.LISTENING -> android.graphics.Color.CYAN
            AIShellOverlayState.PROCESSING -> android.graphics.Color.YELLOW
            AIShellOverlayState.RESPONDING -> android.graphics.Color.GREEN
            AIShellOverlayState.ERROR -> android.graphics.Color.RED
        }

        val bubble = view.findViewById<View>(0)
        bubble?.setBackgroundColor(statusColor)

        Timber.d("🐚 Overlay appearance updated for state: $state (color: $statusColor)")
    }

    /**
     * Create layout parameters for window placement
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val screenWidth = windowManager.defaultDisplay.width

        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            width = 200
            height = 200

            // Start in bottom-right corner
            gravity = Gravity.BOTTOM or Gravity.END
            x = 0
            y = 0

            // Store initial position
            currentX = x
            currentY = y

            Timber.d("🐚 Overlay layout params created")
        }
    }
}

/**
 * Singleton accessor for AIShellOverlayManager
 */
object AIShellOverlaySystem {
    private var manager: AIShellOverlayManager? = null

    fun initialize(context: Context): AIShellOverlayManager {
        if (manager == null) {
            manager = DefaultAIShellOverlayManager(context)
        }
        return manager!!
    }

    fun get(): AIShellOverlayManager? = manager

    fun shutdown() {
        (manager as? DefaultAIShellOverlayManager)?.hideOverlay()
        manager = null
    }
}
