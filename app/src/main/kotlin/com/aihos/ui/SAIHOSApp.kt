package com.aihos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aihos.ui.screens.DashboardScreen
import com.aihos.ui.screens.EvolutionScreen
import com.aihos.ui.screens.MemoryScreen
import com.aihos.ui.screens.SettingsScreen
import com.aihos.ui.viewmodel.SAIHOSViewModel

/**
 * Main Activity UI - Jetpack Compose entry point
 */
@Composable
fun SAIHOSApp() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = { SAIHOSTopBar() },
        bottomBar = { SAIHOSNavigationBar(navController) },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = hiltViewModel<SAIHOSViewModel>()
                )
            }
            
            composable("memory") {
                MemoryScreen(
                    viewModel = hiltViewModel<SAIHOSViewModel>()
                )
            }
            
            composable("evolution") {
                EvolutionScreen(
                    viewModel = hiltViewModel<SAIHOSViewModel>()
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    viewModel = hiltViewModel<SAIHOSViewModel>()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAIHOSTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "SA-AIHOS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1F1F1F)
        ),
        modifier = Modifier.height(64.dp)
    )
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
