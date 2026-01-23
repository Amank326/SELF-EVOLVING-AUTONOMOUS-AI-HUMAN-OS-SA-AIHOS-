package com.aihos.interaction

import android.webkit.WebView
import com.aihos.ui.SAIHOSApp
import timber.log.Timber

/**
 * Extension of Android Bridge for Interaction System
 * Routes InteractionState from Kotlin to JavaScript
 * 
 * Called by: InteractionController
 * Sends to: InteractionResponsiveController (JavaScript)
 */
object InteractionAndroidBridge {

    /**
     * Send interaction state to 3D scene
     */
    fun sendInteractionState(webView: WebView, state: InteractionState) {
        try {
            val json = state.toJson()
            val js = """
                if (window.scene && window.scene.setInteractionState) {
                    window.scene.setInteractionState($json);
                } else if (window.handleInteractionState) {
                    window.handleInteractionState($json);
                }
            """.trimIndent()

            webView.evaluateJavascript(js) { result ->
                if (result == "null") {
                    Timber.d("[InteractionBridge] State sent: ${state.gestureType}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[InteractionBridge] Error sending interaction state")
        }
    }

    /**
     * Register handler in JavaScript to receive interaction events
     * Call this after WebView loads the 3D scene
     */
    fun registerJavaScriptHandler(webView: WebView) {
        try {
            val setupJs = """
                window.handleInteractionState = function(state) {
                    console.log('[JSBridge] Received interaction state:', state.gestureType);
                    if (window.scene && window.scene.setInteractionState) {
                        window.scene.setInteractionState(state);
                    }
                };
                console.log('[JSBridge] Interaction handler registered');
            """.trimIndent()

            webView.evaluateJavascript(setupJs, null)
        } catch (e: Exception) {
            Timber.e(e, "[InteractionBridge] Error registering JS handler")
        }
    }

    /**
     * Notify JavaScript of specific gestures
     */
    fun notifyGesture(webView: WebView, gestureType: String, intensity: Float) {
        try {
            val js = """
                if (window.scene && window.scene.onGesture) {
                    window.scene.onGesture('$gestureType', $intensity);
                }
            """.trimIndent()

            webView.evaluateJavascript(js, null)
        } catch (e: Exception) {
            Timber.e(e, "[InteractionBridge] Error notifying gesture")
        }
    }

    /**
     * Request metrics from 3D scene
     */
    fun requestMetrics(webView: WebView, callback: (String) -> Unit) {
        try {
            webView.evaluateJavascript("JSON.stringify(window.scene?.getMetrics?.() || {})") { result ->
                callback(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "[InteractionBridge] Error requesting metrics")
        }
    }
}
