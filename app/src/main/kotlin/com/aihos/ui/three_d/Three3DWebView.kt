package com.aihos.ui.three_d

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.util.AttributeSet
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File

/**
 * 3D Scene data models for Android ↔ WebView communication
 */
@Serializable
data class SceneMessage(
    val method: String,
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ThemeData(val themeId: String)

@Serializable
data class AnimationIntensityData(val intensity: Double)

@Serializable
data class MetricsData(
    val frameCount: Int = 0,
    val deltaTime: Double = 0.0,
    val isAnimating: Boolean = true,
    val sceneObjectCount: Int = 0
)

/**
 * Three3DWebView - Custom WebView wrapper for SA-AIHOS 3D scene
 * 
 * Features:
 * - Loads local 3D scene HTML
 * - Handles JavaScript ↔ Kotlin communication
 * - Performance monitoring
 * - Theme and animation control
 * - Lifecycle management
 */
class Three3DWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val json = Json { ignoreUnknownKeys = true }
    private var messageCallback: ((SceneMessage) -> Unit)? = null
    private var isLoaded = false

    init {
        setupWebView()
    }

    /**
     * Configure WebView settings for 3D performance
     */
    private fun setupWebView() {
        val settings = this.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebViewClient.MIXED_CONTENT_ALWAYS_ALLOW
            hardwareAccelerationEnabled = true
            
            // Improve performance
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Allow WebGL
            allowContentAccess = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
        }

        // Add JavaScript interface for Android communication
        addJavascriptInterface(AndroidBridgeInterface(this), "SAIHOSBridge")

        // Set custom WebViewClient
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                isLoaded = true
                
                // Request initial theme from settings
                evaluateJavascript(
                    """
                    if (window.SAIHOSSceneInstance) {
                        window.SAIHOSSceneInstance.bridge.requestTheme();
                    }
                    """.trimIndent()
                ) { }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started loading: $url")
                isLoaded = false
            }
        }
    }

    /**
     * Load local 3D scene HTML
     */
    fun load3DScene(assetsPath: String = "file:///android_asset/") {
        val url = "${assetsPath}3d-scene/index.html"
        Log.d(TAG, "Loading 3D scene from: $url")
        loadUrl(url)
    }

    /**
     * Send message to JavaScript/3D scene
     */
    fun sendMessage(method: String, data: Map<String, String> = emptyMap()) {
        if (!isLoaded) {
            Log.w(TAG, "WebView not loaded yet, deferring message: $method")
            return
        }

        val json = """
            {
                "method": "$method",
                "data": ${jsonMapToString(data)},
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()

        val js = """
            if (window.SAIHOSBridge && window.SAIHOSBridge.handleMessage) {
                window.SAIHOSBridge.handleMessage('$json');
            }
        """.trimIndent()

        evaluateJavascript(js) { result ->
            Log.d(TAG, "JavaScript executed: $method -> $result")
        }
    }

    /**
     * Set message callback for JavaScript → Android communication
     */
    fun onSceneMessage(callback: (SceneMessage) -> Unit) {
        messageCallback = callback
    }

    /**
     * Set 3D scene theme
     */
    fun setTheme(themeId: String) {
        sendMessage("setTheme", mapOf("themeId" to themeId))
    }

    /**
     * Set animation intensity (0.0 - 1.0)
     */
    fun setAnimationIntensity(intensity: Double) {
        sendMessage("setAnimationIntensity", mapOf("intensity" to intensity.toString()))
    }

    /**
     * Pause animations
     */
    fun pauseScene() {
        sendMessage("pause", emptyMap())
    }

    /**
     * Resume animations
     */
    fun resumeScene() {
        sendMessage("resume", emptyMap())
    }

    /**
     * Request current metrics from 3D scene
     */
    fun requestMetrics() {
        sendMessage("getMetrics", emptyMap())
    }

    /**
     * Take screenshot of 3D scene
     */
    fun takeScreenshot() {
        sendMessage("screenshot", emptyMap())
    }

    /**
     * JavaScript interface for Android communication
     */
    private inner class AndroidBridgeInterface(private val webView: Three3DWebView) {
        @JavascriptInterface
        fun handleMessage(message: String) {
            Log.d(TAG, "Message from JS: $message")
            try {
                val sceneMessage = json.decodeFromString(SceneMessage.serializer(), message)
                messageCallback?.invoke(sceneMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: $message", e)
            }
        }

        @JavascriptInterface
        fun log(message: String) {
            Log.i(TAG, "JS Log: $message")
        }
    }

    /**
     * Helper to convert map to JSON string
     */
    private fun jsonMapToString(map: Map<String, String>): String {
        return map.entries.joinToString(",", "{", "}") { (k, v) ->
            "\"$k\":\"$v\""
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAnimations()
    }

    override fun onResume() {
        super.onResume()
        resumeAnimations()
    }

    /**
     * Pause 3D animations when Activity is paused
     */
    private fun pauseAnimations() {
        sendMessage("pause", emptyMap())
    }

    /**
     * Resume 3D animations when Activity is resumed
     */
    private fun resumeAnimations() {
        sendMessage("resume", emptyMap())
    }

    companion object {
        private const val TAG = "Three3DWebView"
    }
}
