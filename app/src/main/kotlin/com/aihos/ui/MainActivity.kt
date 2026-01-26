package com.aihos.ui

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import com.aihos.bridge.AndroidJSInterface
import com.google.gson.JsonObject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var jsInterface: AndroidJSInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created - Initializing 3D WebView integration")
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1F1F1F)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            configureWebView()
                            jsInterface = AndroidJSInterface(this, ::onWebMessageReceived)
                            addJavascriptInterface(jsInterface, "SAIHOSBridge")
                            
                            // Load local 3D visualization
                            loadUrl("http://localhost:8000")
                            
                            Timber.d("WebView initialized and loading 3D scene")
                        }
                    }
                )
            }
        }
    }

    /**
     * Configure WebView settings for optimal 3D rendering
     */
    private fun WebView.configureWebView() {
        settings.apply {
            // Enable JavaScript (required for Three.js)
            javaScriptEnabled = true
            
            // WebGL support - Allow mixed HTTP/HTTPS content
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = 0 // MIXED_CONTENT_ALLOW_ALL
            }
            
            // Performance optimization
            cacheMode = WebSettings.LOAD_DEFAULT
            domStorageEnabled = true
            databaseEnabled = true
        }

        Timber.d("WebView settings configured")
    }

    /**
     * Handle messages from JavaScript (3D scene)
     */
    private fun onWebMessageReceived(method: String, data: JsonObject) {
        when (method) {
            "sceneInitialized" -> {
                Timber.i("3D Scene initialized successfully")
                sendAIStateToWeb()
            }
            "metricsUpdate" -> {
                val fps = data.get("fps")?.asFloat ?: 0f
                val renderTime = data.get("renderTime")?.asFloat ?: 0f
                Timber.d("Scene metrics - FPS: $fps, Render: ${renderTime}ms")
            }
            "userGesture" -> {
                val gestureType = data.get("type")?.asString ?: "unknown"
                Timber.d("User gesture detected: $gestureType")
            }
            else -> {
                Timber.d("Received from web: $method")
            }
        }
    }

    /**
     * Send AI state to 3D visualization
     * This would typically come from your AI/ViewModel layer
     */
    private fun sendAIStateToWeb() {
        val aiState = mapOf(
            "status" to "initialized",
            "memory" to mapOf(
                "semantic" to 0.45f,
                "behavioral" to 0.60f,
                "episodic" to 0.30f
            ),
            "reasoning" to mapOf(
                "active" to true,
                "confidence" to 0.85f,
                "complexity" to "medium"
            ),
            "timestamp" to System.currentTimeMillis()
        )
        jsInterface.sendToWeb("setAIMotionState", aiState)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        jsInterface.pauseScene()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        jsInterface.resumeScene()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
