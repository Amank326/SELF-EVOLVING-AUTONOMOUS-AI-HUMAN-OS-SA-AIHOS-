package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aihos.ui.three_d.SceneMessage
import com.aihos.ui.three_d.Three3DWebView

/**
 * Three3DScreen - Compose screen for SA-AIHOS 3D scene
 * 
 * Displays the WebGL 3D visualization with Android integration
 */
@Composable
fun Three3DScreen(
    modifier: Modifier = Modifier,
    onSceneMessage: (SceneMessage) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<Three3DWebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Three3DWebView(context).apply {
                    // Set message callback
                    onSceneMessage { message ->
                        handleSceneMessage(message, onSceneMessage)
                    }

                    // Load 3D scene
                    load3DScene()

                    // Store reference
                    webViewRef = this

                    // Mark loaded after a delay
                    Thread.sleep(1000)
                    isLoading = false
                }
            },
            update = { webView ->
                // Update WebView if needed
            }
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Initializing 3D Scene...",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Error message
        errorMessage?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    // Handle scene messages
    LaunchedEffect(webViewRef) {
        webViewRef?.apply {
            // Set initial theme
            setTheme("cyan")
            
            // Request initial metrics
            requestMetrics()
        }
    }
}

/**
 * Handle messages from 3D scene
 */
private fun handleSceneMessage(
    message: SceneMessage,
    onSceneMessage: (SceneMessage) -> Unit
) {
    when (message.method) {
        "sceneInitialized" -> {
            println("[Three3D] Scene initialized: ${message.timestamp}")
        }
        "metricsUpdate" -> {
            println("[Three3D] Metrics: ${message.data}")
        }
        "error" -> {
            println("[Three3D] Error: ${message.data}")
        }
        "mouseMoved" -> {
            // Handle mouse movement (for future interaction)
        }
        "clicked" -> {
            // Handle click
        }
        else -> {
            // Custom events
            onSceneMessage(message)
        }
    }
}

/**
 * Preview composable for design-time testing
 */
@Composable
fun Three3DScreenPreview() {
    Three3DScreen()
}
