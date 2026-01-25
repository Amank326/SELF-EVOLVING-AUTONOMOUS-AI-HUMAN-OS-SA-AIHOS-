package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Performance Analytics Screen - Display system performance trends and analysis
 * 
 * Features:
 * - Performance trends over time
 * - Comparative metrics
 * - Efficiency indicators
 * - Optimization recommendations
 */
@Composable
fun PerformanceAnalyticsScreen(
    modifier: Modifier = Modifier
) {
    var cpuUsage by remember { mutableStateOf(35f) }
    var memoryUsage by remember { mutableStateOf(52f) }
    var batteryDrain by remember { mutableStateOf(8f) }
    var latencyAvg by remember { mutableStateOf(145L) }
    var throughputScore by remember { mutableStateOf(0.82f) }
    var efficiencyScore by remember { mutableStateOf(0.78f) }
    
    // Simulate performance updates
    LaunchedEffect(Unit) {
        while (true) {
            cpuUsage = (20f + (Math.random() * 60f)).toFloat()
            memoryUsage = (30f + (Math.random() * 50f)).toFloat()
            batteryDrain = (3f + (Math.random() * 12f)).toFloat()
            latencyAvg = (100L + (Math.random() * 200).toLong())
            throughputScore = (0.6f + (Math.random() * 0.4f)).toFloat()
            efficiencyScore = (0.5f + (Math.random() * 0.5f)).toFloat()
            kotlinx.coroutines.delay(3000)
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Performance Analytics",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Resource Usage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Resource Usage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    PerformanceGauge(
                        label = "CPU Usage",
                        percentage = cpuUsage,
                        color = Color(0xFF2196F3)
                    )
                    
                    PerformanceGauge(
                        label = "Memory Usage",
                        percentage = memoryUsage,
                        color = Color(0xFF4CAF50)
                    )
                    
                    PerformanceGauge(
                        label = "Battery Drain",
                        percentage = batteryDrain,
                        color = Color(0xFFFFC107)
                    )
                }
            }
        }
        
        // Performance Scores
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreCard(
                    label = "Throughput",
                    score = throughputScore,
                    modifier = Modifier.weight(1f)
                )
                ScoreCard(
                    label = "Efficiency",
                    score = efficiencyScore,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Latency Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Latency Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    MetricRow("Average Latency", "$latencyAvg ms")
                    MetricRow("Min Latency", "${latencyAvg - 20} ms")
                    MetricRow("Max Latency", "${latencyAvg + 50} ms")
                    MetricRow("P95 Latency", "${latencyAvg + 30} ms")
                }
            }
        }
        
        // Recommendations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Optimization Recommendations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    listOf(
                        "Increase cycle frequency for higher responsiveness",
                        "Memory consolidation recommended",
                        "Consider ensemble pruning to reduce overhead",
                        "Battery drain is within acceptable range"
                    ).forEach { recommendation ->
                        Text(
                            text = "• $recommendation",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceGauge(
    label: String,
    percentage: Float,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.1f%%".format(percentage),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ScoreCard(
    label: String,
    score: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.0f%%".format(score * 100),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = score,
                modifier = Modifier.fillMaxWidth(0.8f),
                color = getScoreColor(score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 0.8f -> Color(0xFF4CAF50)
        score >= 0.6f -> Color(0xFF8BC34A)
        score >= 0.4f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}
