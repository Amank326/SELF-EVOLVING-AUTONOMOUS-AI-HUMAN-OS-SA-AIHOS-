package com.aihos.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import timber.log.Timber
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.aihos.ai.automation.SmartAutomationEngine

/**
 * JavaScript Interface for Secure Android ↔ WebView Communication
 * Handles bidirectional message passing between 3D web scene and Android app
 * Includes Smart Automation bridge for realtime task management
 */
class AndroidJSInterface(
    private val webView: WebView,
    private val onMessageReceived: (method: String, data: JsonObject) -> Unit
) {
    private val scope = MainScope()
    private val gson = Gson()

    // Smart Automation Engine reference (set from MainActivity)
    var automationEngine: SmartAutomationEngine? = null

    /**
     * Called from JavaScript: window.SAIHOSBridge.handleMessage()
     * Receives messages from the 3D web scene
     */
    @JavascriptInterface
    fun handleMessage(messageJson: String) {
        try {
            scope.launch {
                try {
                    val message = gson.fromJson(messageJson, JsonObject::class.java)
                    val method = message.get("method")?.asString ?: return@launch
                    val data = message.get("data")?.asJsonObject ?: JsonObject()

                    Timber.d("[AndroidJSInterface] Received from web: $method")
                    onMessageReceived(method, data)
                } catch (e: Exception) {
                    Timber.e(e, "[AndroidJSInterface] Error parsing message")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error in handleMessage")
        }
    }

    /**
     * Send message from Android to JavaScript
     * Called from: ViewModel, Activities, or other Android components
     */
    fun sendToWeb(method: String, data: Map<String, Any>? = null) {
        try {
            val jsonData = gson.toJsonTree(data ?: emptyMap<String, Any>()).asJsonObject
            val message = JsonObject().apply {
                addProperty("method", method)
                add("data", jsonData)
                addProperty("timestamp", System.currentTimeMillis())
            }

            val json = gson.toJson(message)
            // Escape JSON string for JavaScript
            val escapedJson = json.replace("'", "\\'")
            val jsCode = "if(window.androidInterface) { window.androidInterface.onAndroidMessage('$escapedJson'); }"

            webView.post {
                webView.evaluateJavascript(jsCode) { result ->
                    if (result != null && result != "null") {
                        Timber.d("[AndroidJSInterface] Sent to web: $method")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error sending to web")
        }
    }

    /**
     * Send AI State Update to 3D visualization
     * Example: Motion state, reasoning progress, memory usage
     */
    @JavascriptInterface
    fun updateAIState(stateJson: String) {
        handleMessage("""{
            "method": "setAIMotionState",
            "data": $stateJson
        }""")
    }

    /**
     * Send Performance Metrics from Android to Web
     * Includes: CPU usage, memory, battery, thermal state
     */
    @JavascriptInterface
    fun sendMetrics(metricsJson: String) {
        handleMessage("""{
            "method": "metricsUpdate",
            "data": $metricsJson
        }""")
    }

    /**
     * Send User Gesture/Interaction to 3D scene
     * Maps touch events → animation triggers
     */
    @JavascriptInterface
    fun sendGesture(gestureType: String, intensity: Float) {
        val data = mapOf(
            "type" to gestureType,
            "intensity" to intensity.coerceIn(0f, 1f)
        )
        scope.launch {
            val message = gson.fromJson(
                gson.toJson(
                    mapOf(
                        "method" to "gesture",
                        "data" to data,
                        "timestamp" to System.currentTimeMillis()
                    )
                ),
                JsonObject::class.java
            )
            onMessageReceived("gesture", message.getAsJsonObject("data"))
        }
    }

    /**
     * Query 3D Scene for current metrics
     * Returns: FPS, rendering time, object count, etc.
     */
    @JavascriptInterface
    fun requestSceneMetrics(): String {
        val metrics = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "method" to "getMetrics",
            "requestId" to System.nanoTime()
        )
        return gson.toJson(metrics)
    }

    /**
     * Control 3D scene lifecycle
     */
    @JavascriptInterface
    fun pauseScene() {
        handleMessage("""{
            "method": "pause",
            "data": {}
        }""")
    }

    @JavascriptInterface
    fun resumeScene() {
        handleMessage("""{
            "method": "resume",
            "data": {}
        }""")
    }

    /**
     * Log from JavaScript for debugging
     */
    @JavascriptInterface
    fun log(message: String) {
        Timber.i("[WebView] $message")
    }

    /**
     * Error logging from JavaScript
     */
    @JavascriptInterface
    fun logError(error: String) {
        Timber.e("[WebView] ERROR: $error")
    }

    // ==========================================
    // SMART AUTOMATION BRIDGE
    // ==========================================

    /**
     * Get all automation tasks as JSON
     */
    @JavascriptInterface
    fun getAutomationTasks(): String {
        return try {
            runBlocking {
                automationEngine?.getAutomationsJson() ?: "[]"
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error getting automation tasks")
            "[]"
        }
    }

    /**
     * Create a new automation task
     */
    @JavascriptInterface
    fun createAutomationTask(name: String, description: String, type: String, category: String): String {
        return try {
            runBlocking {
                val task = automationEngine?.createAutomation(
                    name = name,
                    description = description,
                    type = type,
                    category = category
                )
                task?.id ?: ""
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error creating automation")
            ""
        }
    }

    /**
     * Execute a specific automation task
     */
    @JavascriptInterface
    fun executeAutomation(taskId: String): Boolean {
        return try {
            runBlocking {
                automationEngine?.executeAutomation(taskId) ?: false
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error executing automation")
            false
        }
    }

    /**
     * Toggle automation enabled/disabled
     */
    @JavascriptInterface
    fun toggleAutomation(taskId: String, enabled: Boolean) {
        scope.launch {
            try {
                automationEngine?.toggleAutomation(taskId, enabled)
            } catch (e: Exception) {
                Timber.e(e, "[AndroidJSInterface] Error toggling automation")
            }
        }
    }

    /**
     * Delete an automation task
     */
    @JavascriptInterface
    fun deleteAutomation(taskId: String) {
        scope.launch {
            try {
                automationEngine?.deleteAutomation(taskId)
            } catch (e: Exception) {
                Timber.e(e, "[AndroidJSInterface] Error deleting automation")
            }
        }
    }

    /**
     * Get agent status (multitasking agent state)
     */
    @JavascriptInterface
    fun getAgentStatus(): String {
        return try {
            runBlocking {
                automationEngine?.getAgentStatusJson() ?: "{}"
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error getting agent status")
            "{}"
        }
    }

    /**
     * Get automation engine stats
     */
    @JavascriptInterface
    fun getAutomationStats(): String {
        return try {
            runBlocking {
                automationEngine?.getStatsJson() ?: "{}"
            }
        } catch (e: Exception) {
            Timber.e(e, "[AndroidJSInterface] Error getting automation stats")
            "{}"
        }
    }
}
