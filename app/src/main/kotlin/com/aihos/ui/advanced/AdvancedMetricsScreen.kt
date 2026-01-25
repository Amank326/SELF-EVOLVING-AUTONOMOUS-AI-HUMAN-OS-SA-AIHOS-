package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Metrics Screen - Displays real-time AI system performance metrics
 * 
 * Features:
 * - Live system health indicators
 * - Performance trend graphs
 * - Algorithm execution metrics
 * - Memory and resource usage
 * - Decision quality scores
 */
@Composable
fun AdvancedMetricsScreen(
    modifier: Modifier = Modifier
) {
    var systemHealth by remember { mutableStateOf(0.85f) }
    var cycleFrequency by remember { mutableStateOf(100L) }
    var totalCycles by remember { mutableStateOf(0L) }
    var avgResponseTime by remember { mutableStateOf(0L) }
    var errorCount by remember { mutableStateOf(0) }
    var lastErrorMessage by remember { mutableStateOf("No errors") }
    
    // Simulate metrics updates
    LaunchedEffect(Unit) {
        while (true) {
            systemHealth = (0.75f + (Math.random() * 0.25f)).toFloat()
            cycleFrequency = (50 + Math.random() * 150).toLong()
            totalCycles += (Math.random() * 10).toLong()
            avgResponseTime = (100 + Math.random() * 400).toLong()
            if (Math.random() > 0.95) {
                errorCount++
                lastErrorMessage = "Error at ${getCurrentTime()}"
            }
            kotlinx.coroutines.delay(2000)
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
                text = "Advanced AI Metrics",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // System Health Card
        item {
            SystemHealthCard(
                health = systemHealth,
                lastUpdated = getCurrentTime()
            )
        }
        
        // Performance Metrics Row
        item {
            PerformanceMetricsRow(
                cycleFrequency = cycleFrequency,
                avgResponseTime = avgResponseTime,
                totalCycles = totalCycles
            )
        }
        
        // Advanced Memory Metrics
        item {
            AdvancedMemoryMetricsCard(
                semanticSearchLatency = (50 + Math.random() * 150).toInt(),
                memoryClusters = (3 + (Math.random() * 7).toInt()),
                memoryDecayRate = 0.01f + (Math.random() * 0.02f).toFloat()
            )
        }
        
        // Advanced Reasoning Metrics
        item {
            AdvancedReasoningMetricsCard(
                bayesianInferenceTime = (100 + Math.random() * 300).toInt(),
                constraintsSolved = (Math.random() * 50).toInt(),
                inferenceConfidence = (0.6f + Math.random() * 0.4f).toFloat()
            )
        }
        
        // Advanced Evolution Metrics
        item {
            AdvancedEvolutionMetricsCard(
                geneticAlgorithmGenerations = (Math.random() * 100).toInt(),
                qLearningUpdates = (Math.random() * 10000).toInt(),
                bestFitness = (Math.random()).toFloat()
            )
        }
        
        // Advanced Reflection Metrics
        item {
            AdvancedReflectionMetricsCard(
                decisionsReviewed = (Math.random() * 1000).toInt(),
                errorPatternsDetected = (Math.random() * 10).toInt(),
                confidenceCalibration = (0.7f + Math.random() * 0.3f).toFloat()
            )
        }
        
        // Error Log
        item {
            ErrorLogCard(
                errorCount = errorCount,
                lastError = lastErrorMessage
            )
        }
    }
}

@Composable
private fun SystemHealthCard(
    health: Float,
    lastUpdated: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Health",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Health",
                    tint = getHealthColor(health),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Health Bar
            LinearProgressIndicator(
                progress = health,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = getHealthColor(health),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(health * 100).toInt()}% Overall",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastUpdated,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsRow(
    cycleFrequency: Long,
    avgResponseTime: Long,
    totalCycles: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            label = "Cycle Frequency",
            value = "${cycleFrequency}ms",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Avg Response",
            value = "${avgResponseTime}ms",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Total Cycles",
            value = "$totalCycles",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AdvancedMemoryMetricsCard(
    semanticSearchLatency: Int,
    memoryClusters: Int,
    memoryDecayRate: Float
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🧠 Advanced Memory",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MetricRow("Semantic Search Latency", "$semanticSearchLatency ms")
            MetricRow("Memory Clusters", "$memoryClusters active")
            MetricRow("Memory Decay Rate", "%.4f/day".format(memoryDecayRate))
        }
    }
}

@Composable
private fun AdvancedReasoningMetricsCard(
    bayesianInferenceTime: Int,
    constraintsSolved: Int,
    inferenceConfidence: Float
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🤔 Advanced Reasoning",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MetricRow("Bayesian Inference Time", "$bayesianInferenceTime ms")
            MetricRow("Constraints Solved", "$constraintsSolved")
            MetricRow("Inference Confidence", "%.1f%%".format(inferenceConfidence * 100))
        }
    }
}

@Composable
private fun AdvancedEvolutionMetricsCard(
    geneticAlgorithmGenerations: Int,
    qLearningUpdates: Int,
    bestFitness: Float
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🧬 Advanced Evolution",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MetricRow("GA Generations", "$geneticAlgorithmGenerations")
            MetricRow("Q-Learning Updates", "$qLearningUpdates")
            MetricRow("Best Fitness", "%.2f".format(bestFitness))
        }
    }
}

@Composable
private fun AdvancedReflectionMetricsCard(
    decisionsReviewed: Int,
    errorPatternsDetected: Int,
    confidenceCalibration: Float
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🪞 Advanced Reflection",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MetricRow("Decisions Reviewed", "$decisionsReviewed")
            MetricRow("Error Patterns Detected", "$errorPatternsDetected")
            MetricRow("Confidence Calibration", "%.1f%%".format(confidenceCalibration * 100))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ErrorLogCard(
    errorCount: Int,
    lastError: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (errorCount == 0) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ System Log",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Total Errors: $errorCount",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last Event: $lastError",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

private fun getHealthColor(health: Float): Color {
    return when {
        health >= 0.8f -> Color(0xFF4CAF50) // Green - Excellent
        health >= 0.6f -> Color(0xFF8BC34A) // Light Green - Good
        health >= 0.4f -> Color(0xFFFFC107) // Yellow - Fair
        health >= 0.2f -> Color(0xFFFF9800) // Orange - Poor
        else -> Color(0xFFF44336) // Red - Critical
    }
}

private fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date())
}
