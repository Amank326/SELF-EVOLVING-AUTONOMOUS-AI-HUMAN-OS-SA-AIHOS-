package com.aihos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.motion.AIStateBroadcaster
import com.aihos.ai.reflection.ReflectionEngine
import com.aihos.ui.three_d.Three3DAIBridge
import com.aihos.ui.three_d.Three3DWebView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Example ViewModel showing how to integrate AI Motion Intelligence
 * with your UI and screen lifecycle
 * 
 * This demonstrates the proper setup for displaying AI-driven 3D animations
 */
@HiltViewModel
class Three3DVisualizationViewModel @Inject constructor(
    private val autonomyController: AutonomyController,
    private val broadcaster: AIStateBroadcaster,
    private val reflectionEngine: ReflectionEngine
) : ViewModel() {
    
    private var bridge: Three3DAIBridge? = null
    private var isInitialized = false
    
    /**
     * Initialize the 3D visualization system
     * Call this when your 3D WebView is ready
     */
    fun initializeThree3D(webView: Three3DWebView) {
        if (isInitialized) return
        
        viewModelScope.launch {
            try {
                Timber.i("Initializing 3D AI Motion Intelligence system...")
                
                // Create bridge between broadcaster and 3D WebView
                bridge = Three3DAIBridge(
                    webView = webView,
                    broadcaster = broadcaster
                )
                
                // Start broadcasting AI state to 3D system
                broadcaster.startBroadcasting()
                isInitialized = true
                
                Timber.i("✓ AI Motion Intelligence system ready")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize 3D system")
            }
        }
    }
    
    /**
     * Start the AI autonomy loop
     * This will begin making decisions and the 3D system will visualize them
     */
    fun startAILoop() {
        viewModelScope.launch {
            try {
                Timber.i("Starting AI autonomy loop...")
                autonomyController.startDecisionLoop()
                
                // The broadcaster will automatically detect decisions
                // and send state changes to 3D system
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to start AI loop")
            }
        }
    }
    
    /**
     * Stop the AI autonomy loop
     */
    fun stopAILoop() {
        viewModelScope.launch {
            autonomyController.stopDecisionLoop()
            Timber.i("AI loop stopped")
        }
    }
    
    /**
     * Pause/resume 3D visualization (but keep AI running)
     */
    fun pauseThree3D() {
        bridge?.let {
            viewModelScope.launch {
                // Send pause message to WebView
                Timber.i("Pausing 3D visualization...")
            }
        }
    }
    
    fun resumeThree3D() {
        bridge?.let {
            viewModelScope.launch {
                Timber.i("Resuming 3D visualization...")
            }
        }
    }
    
    /**
     * Get current status for UI display
     */
    fun getCurrentStatus(): String {
        return buildString {
            appendLine("3D Visualization Status")
            appendLine("─────────────────────")
            appendLine("Initialized: $isInitialized")
            appendLine("Bridge Active: ${bridge != null}")
            appendLine("Broadcasting: (check logcat)")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Clean up resources
        stopAILoop()
        broadcaster.stopBroadcasting()
        bridge?.dispose()
        
        Timber.i("ViewModel cleared, 3D system cleaned up")
    }
}

/**
 * Example Composable showing how to use the ViewModel
 */
/*
@Composable
fun Three3DVisualizationExample(
    viewModel: Three3DVisualizationViewModel = hiltViewModel()
) {
    var webView by remember { mutableStateOf<Three3DWebView?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    
    LaunchedEffect(webView) {
        if (webView != null) {
            // Initialize 3D system when WebView is ready
            viewModel.initializeThree3D(webView!!)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 3D WebView filling top portion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    Three3DWebView(context).apply {
                        webView = this
                        loadUrl("file:///android_asset/3d-scene/index.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Control buttons at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        viewModel.startAILoop()
                        isRunning = true
                    } else {
                        viewModel.stopAILoop()
                        isRunning = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red else Color.Green
                )
            ) {
                Text(if (isRunning) "Stop AI" else "Start AI")
            }
            
            Button(
                onClick = { viewModel.pauseThree3D() },
                enabled = isRunning
            ) {
                Text("Pause 3D")
            }
        }
        
        // Status text
        Text(
            viewModel.getCurrentStatus(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
*/
