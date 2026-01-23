package com.aihos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aihos.ai.autonomy.AISystemController
import com.aihos.ui.screens.DashboardScreen
import com.aihos.ui.screens.EvolutionScreen
import com.aihos.ui.screens.MemoryScreen
import com.aihos.ui.screens.SettingsScreen
import com.aihos.ui.viewmodel.SAIHOSViewModel
import timber.log.Timber

/**
 * Main Activity UI - Jetpack Compose entry point with real-time AI state visualization
 */
@Composable
fun SAIHOSApp() {
    val navController = rememberNavController()
    val viewModel: SAIHOSViewModel = hiltViewModel()
    
    // Collect AI state in real-time
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val cycleMetrics by viewModel.cycleMetrics.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = { SAIHOSTopBar(aiState = aiState, cycleMetrics = cycleMetrics) },
        bottomBar = { SAIHOSNavigationBar(navController) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AI State Status Bar
            AIStateStatusBar(aiState = aiState, cycleMetrics = cycleMetrics)
            
            // Navigation content
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                composable("dashboard") {
                    DashboardScreen(viewModel = viewModel)
                }
                
                composable("memory") {
                    MemoryScreen(viewModel = viewModel)
                }
                
                composable("evolution") {
                    EvolutionScreen(viewModel = viewModel)
                }
                
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAIHOSTopBar(
    aiState: AISystemController.AIState,
    cycleMetrics: AISystemController.CycleMetrics
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SA-AIHOS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    getStateDescription(aiState),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1F1F1F)
        ),
        modifier = Modifier.height(80.dp)
    )
}

/**
 * Real-time AI state status bar showing current state and metrics
 */
@Composable
fun AIStateStatusBar(
    aiState: AISystemController.AIState,
    cycleMetrics: AISystemController.CycleMetrics
) {
    val stateColor by animateColorAsState(
        targetValue = getStateColor(aiState),
        animationSpec = tween(500),
        label = "stateColorAnimation"
    )
    
    val healthPercent = if (cycleMetrics.targetCycleTimeMs == 0L) {
        100
    } else {
        ((cycleMetrics.targetCycleTimeMs.toFloat() / cycleMetrics.lastCycleTimeMs.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 200)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(stateColor),
        color = stateColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // State indicator
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "State: ${getStateDescription(aiState)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Cycle: ${cycleMetrics.lastCycleTimeMs}ms",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Health bar
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "Health: $healthPercent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                LinearProgressIndicator(
                    progress = { healthPercent / 200f },
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp),
                    color = when {
                        healthPercent >= 100 -> Color.Green
                        healthPercent >= 75 -> Color.Yellow
                        else -> Color.Red
                    }
                )
            }
        }
    }
}

@Composable
fun SAIHOSNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = Color(0xFF1F1F1F),
        modifier = Modifier.height(64.dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp) },
            selected = false,
            onClick = { navController.navigate("dashboard") }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Memory") },
            label = { Text("Memory", fontSize = 10.sp) },
            selected = false,
            onClick = { navController.navigate("memory") }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Evolution") },
            label = { Text("Evolution", fontSize = 10.sp) },
            selected = false,
            onClick = { navController.navigate("evolution") }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp) },
            selected = false,
            onClick = { navController.navigate("settings") }
        )
    }
}

// Colors for consistency
val DarkBackground = Color(0xFF1F1F1F)
val CardBackground = Color(0xFF2A2A2A)
val AccentColor = Color(0xFF4CAF50)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
/**
 * Get human-readable description of AI state
 */
fun getStateDescription(state: AISystemController.AIState): String = when (state) {
    AISystemController.AIState.Idle -> "Idle"
    AISystemController.AIState.Initializing -> "Initializing..."
    AISystemController.AIState.Thinking -> "Thinking"
    AISystemController.AIState.Acting -> "Acting"
    AISystemController.AIState.Reflecting -> "Reflecting"
    AISystemController.AIState.Evolving -> "Evolving"
    AISystemController.AIState.Paused -> "Paused"
    AISystemController.AIState.Stopped -> "Stopped"
    is AISystemController.AIState.Error -> "Error"
}

/**
 * Get color for AI state (for visual feedback)
 */
fun getStateColor(state: AISystemController.AIState): Color = when (state) {
    AISystemController.AIState.Idle -> Color(0xFF808080) // Gray
    AISystemController.AIState.Initializing -> Color(0xFFFFFF00) // Yellow
    AISystemController.AIState.Thinking -> Color(0xFF0088FF) // Blue
    AISystemController.AIState.Acting -> Color(0xFF00FF00) // Green
    AISystemController.AIState.Reflecting -> Color(0xFFFF8800) // Orange
    AISystemController.AIState.Evolving -> Color(0xFFFF00FF) // Magenta
    AISystemController.AIState.Paused -> Color(0xFFFF0000) // Red
    AISystemController.AIState.Stopped -> Color(0xFF000000) // Black
    is AISystemController.AIState.Error -> Color(0xFFFF0000) // Red
}