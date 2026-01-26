package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Visualization Control Screen
 * Interactive controls for the 3D scene: colors, animations, effects
 */
@Composable
fun VisualizationScreen(onBackClick: () -> Unit = {}) {
    var controls by remember {
        mutableStateOf(
            VisualizationControls(
                colorMode = "Dynamic",
                animationSpeed = 0.5f,
                bloomIntensity = 0.7f,
                cameraRotation = 0.6f,
                particleCount = 0.8f,
                effectsEnabled = true
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
                        tint = Color(0xFF6600FF)
                    )
                }
                Text(
                    text = "3D Visualization",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6600FF),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Divider(color = Color(0xFF333333), thickness = 1.dp)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color Settings
                VisualizationSection(title = "Color & Appearance") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SegmentedControl(
                            label = "Color Mode",
                            options = listOf("Dynamic", "Static", "Gradient"),
                            selected = controls.colorMode,
                            onSelect = {
                                controls = controls.copy(colorMode = it)
                            }
                        )

                        SliderControl(
                            label = "Bloom Intensity",
                            value = controls.bloomIntensity,
                            onValueChange = {
                                controls = controls.copy(bloomIntensity = it)
                            }
                        )
                    }
                }

                // Animation Settings
                VisualizationSection(title = "Animation") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SliderControl(
                            label = "Animation Speed",
                            value = controls.animationSpeed,
                            onValueChange = {
                                controls = controls.copy(animationSpeed = it)
                            }
                        )

                        SliderControl(
                            label = "Camera Rotation",
                            value = controls.cameraRotation,
                            onValueChange = {
                                controls = controls.copy(cameraRotation = it)
                            }
                        )
                    }
                }

                // Effects Settings
                VisualizationSection(title = "Effects") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Enable Effects",
                                fontSize = 14.sp,
                                color = Color(0xFFFFFFFF)
                            )
                            Switch(
                                checked = controls.effectsEnabled,
                                onCheckedChange = {
                                    controls = controls.copy(effectsEnabled = it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF6600FF),
                                    checkedTrackColor = Color(0xFF6600FF).copy(alpha = 0.3f)
                                )
                            )
                        }

                        if (controls.effectsEnabled) {
                            SliderControl(
                                label = "Particle Count",
                                value = controls.particleCount,
                                onValueChange = {
                                    controls = controls.copy(particleCount = it)
                                }
                            )
                        }
                    }
                }

                // Quick Actions
                VisualizationSection(title = "Quick Actions") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Reset to defaults */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6600FF)
                            )
                        ) {
                            Text("Reset to Defaults", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* Rotate scene */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334455)
                                )
                            ) {
                                Text("Rotate", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { /* Zoom */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334455)
                                )
                            ) {
                                Text("Zoom", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { /* Pause */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334455)
                                )
                            ) {
                                Text("Pause", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Scene Info
                VisualizationSection(title = "Scene Information") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow("Resolution", "1920x1080")
                        InfoRow("Target FPS", "60")
                        InfoRow("API Level", "WebGL 2.0")
                        InfoRow("Status", "Connected")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Visualization section card
 */
@Composable
fun VisualizationSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF6600FF).copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a2240)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6600FF),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

/**
 * Slider control with label and value display
 */
@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color(0xFF6600FF),
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6600FF),
                activeTrackColor = Color(0xFF6600FF),
                inactiveTrackColor = Color(0xFF333333)
            )
        )
    }
}

/**
 * Segmented control for multiple options
 */
@Composable
fun SegmentedControl(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (option == selected) Color(0xFF6600FF) else Color(0xFF333333)
                    )
                ) {
                    Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Info row for displaying static information
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFF6600FF),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Visualization controls data class
 */
data class VisualizationControls(
    val colorMode: String,
    val animationSpeed: Float,
    val bloomIntensity: Float,
    val cameraRotation: Float,
    val particleCount: Float,
    val effectsEnabled: Boolean
)
