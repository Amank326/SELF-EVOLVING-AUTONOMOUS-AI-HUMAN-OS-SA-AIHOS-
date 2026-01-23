package com.aihos.ui.three_d

import com.aihos.ai.motion.AIMotionState
import com.aihos.ai.motion.AIStateBroadcaster
import com.aihos.ai.motion.AIMotionStateListener
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Three3D AI Bridge: Connects AI state broadcaster to 3D WebView
 * Sends real-time AI cognitive state to drive procedural animations
 */
class Three3DAIBridge(
    private val webView: Three3DWebView,
    private val broadcaster: AIStateBroadcaster
) : AIMotionStateListener {
    
    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    init {
        // Register this bridge as a listener for AI state changes
        broadcaster.addListener(this)
        Timber.i("Three3D AI Bridge initialized")
    }
    
    /**
     * Receive AI motion state changes and send to WebView
     * This is the primary connection between AI and 3D visualization
     */
    override suspend fun onAIMotionStateChanged(state: AIMotionState) {
        // Serialize AI state to JSON
        val stateJson = json.encodeToString(state)
        
        // Send to WebView on main thread
        scope.launch {
            try {
                webView.sendAIMotionState(stateJson)
                Timber.d("Sent AI state to 3D: ${state.primaryState}")
            } catch (e: Exception) {
                Timber.e(e, "Error sending AI state to 3D")
            }
        }
    }
    
    /**
     * Receive errors from AI system
     */
    override suspend fun onAIError(error: String) {
        Timber.e("AI Error: $error")
        scope.launch {
            webView.notifyError("ai_error", error)
        }
    }
    
    fun dispose() {
        scope.cancel()
        broadcaster.removeListener(this)
    }
}

/**
 * Extension function for Three3DWebView to send AI state
 */
fun Three3DWebView.sendAIMotionState(aiMotionStateJson: String) {
    // Execute JavaScript to update AI state
    val js = """
        if (window.SAIHOSSceneInstance && window.SAIHOSSceneInstance.setAIMotionState) {
            window.SAIHOSSceneInstance.setAIMotionState('$aiMotionStateJson');
        }
    """.trimIndent()
    
    evaluateJavascript(js) { result ->
        // Callback if needed
    }
}

/**
 * Extension function to notify errors
 */
fun Three3DWebView.notifyError(errorType: String, message: String) {
    val js = """
        if (window.SAIHOSSceneInstance && window.SAIHOSSceneInstance.bridge) {
            window.SAIHOSSceneInstance.bridge.notifyError('$errorType', '$message');
        }
    """.trimIndent()
    
    evaluateJavascript(js, null)
}
