package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Error Pattern Analysis Screen - Visualize and analyze detected error patterns
 * 
 * Features:
 * - Error frequency analysis
 * - Pattern detection visualization
 * - Error categorization
 * - Recovery recommendations
 * - Historical error trends
 */
@Composable
fun ErrorPatternScreen(
    modifier: Modifier = Modifier
) {
    var totalErrors by remember { mutableStateOf(0) }
    var criticalErrors by remember { mutableStateOf(0) }
    var warningCount by remember { mutableStateOf(0) }
    var errorPatterns by remember { 
        mutableStateOf(listOf<ErrorPatternData>())
    }
    var errorTrend by remember { 
        mutableStateOf(listOf<Pair<Int, Int>>())
    }
    
    // Simulate error detection
    LaunchedEffect(Unit) {
        while (true) {
            if (Math.random() > 0.92) {
                totalErrors++
                if (Math.random() > 0.7) {
                    criticalErrors++
                } else {
                    warningCount++
                }
            }
            
            errorPatterns = listOf(
                ErrorPatternData(
                    type = "Reasoning Timeout",
                    frequency = (Math.random() * 20).toInt(),
                    severity = "High",
                    lastOccurrence = "2m ago",
                    suggestion = "Increase inference timeout or simplify constraints"
                ),
                ErrorPatternData(
                    type = "Memory Allocation",
                    frequency = (Math.random() * 15).toInt(),
                    severity = "Medium",
                    lastOccurrence = "5m ago",
                    suggestion = "Consolidate or prune old memory items"
                ),
                ErrorPatternData(
                    type = "Evolution Stagnation",
                    frequency = (Math.random() * 10).toInt(),
                    severity = "Low",
                    lastOccurrence = "10m ago",
                    suggestion = "Increase mutation rate or restart population"
                ),
                ErrorPatternData(
                    type = "Reflection Divergence",
                    frequency = (Math.random() * 8).toInt(),
                    severity = "Medium",
                    lastOccurrence = "3m ago",
                    suggestion = "Recalibrate confidence thresholds"
                )
            )
            
            errorTrend = (0..10).map { i ->
                i to (Math.random() * (totalErrors + 5)).toInt()
            }
            
            kotlinx.coroutines.delay(4000)
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
                text = "Error Pattern Analysis",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Error Statistics
        item {
            ErrorStatistics(
                totalErrors = totalErrors,
                criticalErrors = criticalErrors,
                warningCount = warningCount
            )
        }
        
        // Error Trend Chart
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Error Trend (Last 10 Cycles)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    ErrorTrendVisualization(trend = errorTrend)
                }
            }
        }
        
        // Detected Patterns
        item {
            Text(
                text = "Detected Patterns",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        items(errorPatterns.size) { index ->
            ErrorPatternCard(pattern = errorPatterns[index])
        }
        
        // Recovery Strategies
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
                        text = "🔧 Recommended Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    listOf(
                        "Run memory consolidation cycle",
                        "Increase genetic algorithm mutation rate",
                        "Reset reasoning constraint cache",
                        "Recalibrate confidence calibration model",
                        "Enable verbose logging for pattern tracking"
                    ).forEach { action ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50))
                            )
                            Text(
                                text = action,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ErrorStatistics(
    totalErrors: Int,
    criticalErrors: Int,
    warningCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Total Errors",
            value = totalErrors.toString(),
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Critical",
            value = criticalErrors.toString(),
            color = Color(0xFFD32F2F),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Warnings",
            value = warningCount.toString(),
            color = Color(0xFFFFC107),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
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
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ErrorTrendVisualization(trend: List<Pair<Int, Int>>) {
    val maxValue = trend.maxOfOrNull { it.second } ?: 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        trend.forEach { (_, value) ->
            val height = if (maxValue > 0) {
                ((value.toFloat() / maxValue) * 80f).dp
            } else {
                2.dp
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .background(Color(0xFFF44336), shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun ErrorPatternCard(pattern: ErrorPatternData) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = getSeverityColor(pattern.severity),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = pattern.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = pattern.severity,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = getSeverityColor(pattern.severity)
                )
            }
            
            Divider()
            
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricRow("Frequency", "${pattern.frequency} occurrences")
                MetricRow("Last Occurred", pattern.lastOccurrence)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "💡 ${pattern.suggestion}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

data class ErrorPatternData(
    val type: String,
    val frequency: Int,
    val severity: String,
    val lastOccurrence: String,
    val suggestion: String
)

private fun getSeverityColor(severity: String): Color {
    return when (severity) {
        "High" -> Color(0xFFF44336)
        "Medium" -> Color(0xFFFFC107)
        "Low" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
}
