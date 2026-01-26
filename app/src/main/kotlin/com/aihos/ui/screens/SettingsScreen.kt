package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings Screen - Configuration and preferences
 * Allows control over: Theme, Quality, Animation, Debug options
 */
@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    var settings by remember {
        mutableStateOf(
            SettingsData(
                themeMode = "Dark",
                qualityLevel = "High",
                animationEnabled = true,
                debugMode = false,
                hapticFeedback = true,
                autoOptimize = true
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a0e27))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF00FF88)
                    )
                }
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Divider(color = Color(0xFF333333), thickness = 1.dp)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Display Settings
                SettingsSectionTitle("Display")

                SettingRow(
                    label = "Theme Mode",
                    value = settings.themeMode,
                    onClick = {
                        settings = settings.copy(
                            themeMode = if (settings.themeMode == "Dark") "Light" else "Dark"
                        )
                    }
                )

                // Performance Settings
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Performance")

                SettingRow(
                    label = "Quality Level",
                    value = settings.qualityLevel,
                    onClick = {
                        val levels = listOf("Low", "Medium", "High", "Ultra")
                        val currentIndex = levels.indexOf(settings.qualityLevel)
                        val nextLevel = levels[(currentIndex + 1) % levels.size]
                        settings = settings.copy(qualityLevel = nextLevel)
                    }
                )

                // Animation Settings
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Animation")

                ToggleSetting(
                    label = "Animations Enabled",
                    enabled = settings.animationEnabled,
                    onToggle = {
                        settings = settings.copy(animationEnabled = !settings.animationEnabled)
                    }
                )

                // Interaction Settings
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Interaction")

                ToggleSetting(
                    label = "Haptic Feedback",
                    enabled = settings.hapticFeedback,
                    onToggle = {
                        settings = settings.copy(hapticFeedback = !settings.hapticFeedback)
                    }
                )

                // Optimization Settings
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Optimization")

                ToggleSetting(
                    label = "Auto-Optimize",
                    enabled = settings.autoOptimize,
                    onToggle = {
                        settings = settings.copy(autoOptimize = !settings.autoOptimize)
                    }
                )

                // Developer Settings
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("Developer")

                ToggleSetting(
                    label = "Debug Mode",
                    enabled = settings.debugMode,
                    onToggle = {
                        settings = settings.copy(debugMode = !settings.debugMode)
                    }
                )

                if (settings.debugMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1a2240)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Debug Info",
                                fontSize = 12.sp,
                                color = Color(0xFFFFAA00),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "APK Version: 1.0.0",
                                fontSize = 10.sp,
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = "Build: Phase 5 Advanced",
                                fontSize = 10.sp,
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = "Integration: Android ↔ WebView Ready",
                                fontSize = 10.sp,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply Settings Button
                Button(
                    onClick = { /* Settings are applied immediately */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF88)
                    )
                ) {
                    Text(
                        "Apply Settings",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Settings section title
 */
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF00FF88),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Clickable setting row
 */
@Composable
fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF333333)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a2240)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFFFFFFFF),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color(0xFF00FF88),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Toggle switch setting
 */
@Composable
fun ToggleSetting(label: String, enabled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF333333)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a2240)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFFFFFFFF),
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00FF88),
                    checkedTrackColor = Color(0xFF00FF88).copy(alpha = 0.3f),
                    uncheckedThumbColor = Color(0xFF666666),
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }
    }
}

/**
 * Settings data class
 */
data class SettingsData(
    val themeMode: String,
    val qualityLevel: String,
    val animationEnabled: Boolean,
    val debugMode: Boolean,
    val hapticFeedback: Boolean,
    val autoOptimize: Boolean
)
