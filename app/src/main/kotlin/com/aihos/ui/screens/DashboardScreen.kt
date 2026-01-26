package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * Dashboard Screen - Main display for AI system state
 * Shows: Memory usage, reasoning state, animation intensity, performance metrics
 */
@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit = {},
    onVisualizationClick: () -> Unit = {}
) {
    var aiState by remember {
        mutableStateOf(
            AIStateData(
                status = "Initialized",
                memoryUsage = 0.45f,
                reasoningConfidence = 0.85f,
                animationIntensity = 0.60f,
                fps = 60,
                renderTime = 16.67f
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SA-AIHOS Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF88)
                    )
                    Text(
                        text = "Status: ${aiState.status}",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
                
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF00FF88)
                    )
                }
            }

            // Memory Section
            MetricsCard(
                title = "Memory Status",
                metrics = listOf(
                    "Semantic" to aiState.memoryUsage,
                    "Behavioral" to aiState.memoryUsage * 1.2f,
                    "Episodic" to aiState.memoryUsage * 0.8f
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reasoning Section
            MetricsCard(
                title = "Reasoning Engine",
                metrics = listOf(
                    "Confidence" to aiState.reasoningConfidence,
                    "Complexity" to 0.65f,
                    "Processing" to 0.45f
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animation Section
            MetricsCard(
                title = "Animation State",
                metrics = listOf(
                    "Intensity" to aiState.animationIntensity,
                    "Rotation Speed" to 0.75f,
                    "Oscillation" to 0.55f
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Section
            PerformanceCard(
                fps = aiState.fps,
                renderTime = aiState.renderTime
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onVisualizationClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF88)
                    )
                ) {
                    Text(
                        "3D Visualization",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        // Simulate AI state update
                        aiState = aiState.copy(
                            reasoningConfidence = (aiState.reasoningConfidence + 0.1f).coerceIn(0f, 1f),
                            animationIntensity = (aiState.animationIntensity + 0.05f).coerceIn(0f, 1f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6600FF)
                    )
                ) {
                    Text(
                        "Trigger AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activity Log
            ActivityLogSection()
        }
    }
}

/**
 * Reusable metrics card component
 */
@Composable
fun MetricsCard(
    title: String,
    metrics: List<Pair<String, Float>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF00FF88).copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a2240)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF88)
            )

            metrics.forEach { (label, value) ->
                MetricRow(label, value)
            }
        }
    }
}

/**
 * Individual metric with progress bar
 */
@Composable
fun MetricRow(label: String, value: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFFAAAAAA)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 11.sp,
                color = Color(0xFF00FF88),
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = value.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFF00FF88),
            trackColor = Color(0xFF333333)
        )
    }
}

/**
 * Performance metrics card
 */
@Composable
fun PerformanceCard(fps: Int, renderTime: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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
                text = "Performance",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6600FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        text = "FPS",
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Text(
                        text = "$fps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6600FF)
                    )
                }

                Column {
                    Text(
                        text = "Render Time",
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Text(
                        text = "%.2f ms".format(renderTime),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6600FF)
                    )
                }
            }
        }
    }
}

/**
 * Activity log section
 */
@Composable
fun ActivityLogSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFFF6600).copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a2240)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Activity Log",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6600)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val activities = listOf(
                "[00:12] Scene initialized successfully",
                "[00:45] Memory consolidation triggered",
                "[01:23] Gesture detected: Swipe right",
                "[02:10] Reasoning confidence: 85%"
            )

            activities.forEach { activity ->
                Text(
                    text = activity,
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Data class for AI state
 */
data class AIStateData(
    val status: String,
    val memoryUsage: Float,
    val reasoningConfidence: Float,
    val animationIntensity: Float,
    val fps: Int,
    val renderTime: Float
)
