package com.aihos.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aihos.ai.autonomy.AISystemController
import com.aihos.graphics.bridge.AI3DBridge
import com.aihos.graphics.filament.AICoreMaterial
import com.aihos.graphics.filament.AIState
import com.aihos.graphics.filament.Native3DEngine
import com.aihos.ui.viewmodel.SAIHOSViewModel
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

/**
 * Filament3DView - Jetpack Compose wrapper for native 3D rendering
 *
 * Integrates the Filament-based Native3DEngine into Compose UI.
 * Handles lifecycle events and AI state binding.
 *
 * Usage:
 * Filament3DView(
 *     viewModel = viewModel,
 *     modifier = Modifier.fillMaxSize()
 * )
 */
@Composable
fun Filament3DView(
    viewModel: SAIHOSViewModel,
    modifier: Modifier = Modifier
) {
    // Collect AI state for binding to 3D
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val cycleMetrics by viewModel.cycleMetrics.collectAsStateWithLifecycle()
    val lastDecision by viewModel.lastDecision.collectAsStateWithLifecycle()
    val lastInsight by viewModel.lastInsight.collectAsStateWithLifecycle()
    
    // Reference to the 3D engine (kept alive across recompositions)
    val engine = remember { mutableStateOf<Native3DEngine?>(null) }
    val bridge = remember { mutableStateOf<AI3DBridge?>(null) }
    
    // Initialize engine and bridge
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Effect to bind AI state to 3D visuals
    LaunchedEffect(aiState, cycleMetrics, lastDecision, lastInsight) {
        val aiEngine = engine.value ?: return@LaunchedEffect
        val aiBridge = bridge.value ?: return@LaunchedEffect
        
        aiBridge.updateFromAIState(
            aiState = aiState,
            cycleMetrics = cycleMetrics,
            lastDecision = lastDecision,
            lastInsight = lastInsight
        )
    }
    
    // Android View containing SurfaceView
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            SurfaceView(ctx).apply {
                // Initialize 3D engine on main thread
                val nativeEngine = Native3DEngine(
                    context = ctx,
                    surfaceView = this,
                    scope = viewModel.viewModelScope
                )
                
                try {
                    nativeEngine.initialize()
                    engine.value = nativeEngine
                    
                    // Create bridge
                    val aiBridge = AI3DBridge(nativeEngine, viewModel.viewModelScope)
                    aiBridge.start()
                    bridge.value = aiBridge
                    
                    Timber.i("Filament3DView: Engine and bridge initialized successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Filament3DView: Engine initialization failed")
                }
            }
        },
        onRelease = {
            // Cleanup on recomposition
            bridge.value?.stop()
        }
    )
    
    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Filament3DView: Disposing")
            bridge.value?.destroy()
            engine.value?.destroy()
            engine.value = null
            bridge.value = null
        }
    }
}

/**
 * Screen wrapper for 3D visualization
 * Used in navigation to display the native 3D AI Core
 */
@Composable
fun AICore3DScreen(
    viewModel: SAIHOSViewModel,
    modifier: Modifier = Modifier
) {
    Filament3DView(
        viewModel = viewModel,
        modifier = modifier
    )
}
