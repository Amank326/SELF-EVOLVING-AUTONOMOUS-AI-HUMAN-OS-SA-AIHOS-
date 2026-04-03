package com.aihos.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.aihos.ai.AIApplication
import com.aihos.ai.automation.SmartAutomationEngine
import com.aihos.bridge.AndroidJSInterface
import com.aihos.data.db.SAIHOSDatabase
import com.aihos.ui.render.RenderSurfaceView
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var glSurfaceView: RenderSurfaceView? = null
    private var automationEngine: SmartAutomationEngine? = null
    private var jsInterface: AndroidJSInterface? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var metricsPumpJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity - SA-AIHOS App Launched!")
        
        // Initialize AI system
        AIApplication.initialize(this)

        // ── Root layout: FrameLayout stacking GL behind WebView ─────
        val root = FrameLayout(this)

        // ── OpenGL ES 3.0 cinematic background layer ────────────
        val glView = RenderSurfaceView(this)
        if (glView.isES30Supported) {
            glSurfaceView = glView
            root.addView(glView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            Timber.i("RenderEngine: modular OpenGL ES 3.0 pipeline active")
        } else {
            Timber.w("OpenGL ES 3.0 not available — skipping GL layer")
        }

        // ── WebView foreground layer ────────────────────────────────
        webView = WebView(this)

        // Make WebView background transparent so GL shows through
        webView.setBackgroundColor(0x00000000)
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.databaseEnabled = true

        webView.webViewClient = WebViewClient()

        // Register JavaScript Interface (bridge)
        val bridge = AndroidJSInterface(webView) { method, data ->
            handleWebMessage(method, data)
        }
        jsInterface = bridge
        webView.addJavascriptInterface(bridge, "SAIHOSBridge")

        webView.loadUrl("file:///android_asset/index.html")

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)

        // ── Start subsystems ────────────────────────────────────────
        initializeAutomation()
        startMetricsPump()
    }

    // ════════════════════════════════════════════════════════════════
    // Metrics pump — feeds live AI data into the GL renderer
    // ════════════════════════════════════════════════════════════════

    private fun startMetricsPump() {
        val glView = glSurfaceView ?: return
        metricsPumpJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val state = AIApplication.getSystemState()
                if (state != null) {
                    glView.updateFromSystemState(
                        overallConfidence = state.overallConfidence,
                        memoryLoad = state.memoryLoad,
                        autonomyLevel = state.autonomyLevel,
                        systemHealth = state.systemHealth,
                        reasoningComplexity = state.reasoningComplexity,
                        bestFitness = state.bestFitness.coerceIn(0f, 1f),
                        selfAwareness = state.selfAwareness,
                        animationIntensity = state.animationIntensity
                    )
                }
                delay(50)  // ~20 Hz update rate
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Automation init (unchanged logic)
    // ════════════════════════════════════════════════════════════════

    private fun initializeAutomation() {
        scope.launch(Dispatchers.IO) {
            try {
                val db = SAIHOSDatabase.getInstance(applicationContext)
                val engine = SmartAutomationEngine(
                    automationDao = db.automationTaskDao(),
                    agentTaskDao = db.agentTaskDao()
                )
                engine.initialize()
                engine.setupDefaults()
                automationEngine = engine
                jsInterface?.automationEngine = engine
                Timber.d("Smart Automation Engine initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize automation engine")
            }
        }
    }

    private fun handleWebMessage(method: String, data: JsonObject) {
        Timber.d("Web message received: $method")
        when (method) {
            "systemInit" -> {
                scope.launch(Dispatchers.IO) {
                    AIApplication.getCoordinator()?.cycle()
                }
            }
            "requestMetrics" -> {
                val metrics = AIApplication.getAllMetrics()
                Timber.d("Sending metrics: ${metrics.size} items")
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        metricsPumpJob?.cancel()
        glSurfaceView?.release()
        scope.launch(Dispatchers.IO) {
            automationEngine?.shutdown()
        }
        AIApplication.shutdown()
        webView.destroy()
        Timber.d("MainActivity destroyed")
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
