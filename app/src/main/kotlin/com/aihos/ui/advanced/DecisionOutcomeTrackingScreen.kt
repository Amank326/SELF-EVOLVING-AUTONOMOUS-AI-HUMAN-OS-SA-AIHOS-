package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Decision Outcome Tracking Screen - Monitor decision results and effectiveness
 * 
 * Features:
 * - Decision history with outcomes
 * - Success rate tracking
 * - Decision confidence vs actual outcome correlation
 * - Outcome distribution visualization
 * - Quality metrics for decisions
 */

data class DecisionRecord(
    val id: Int,
    val decision: String,
    val confidence: Float,
    val outcome: String,
    val success: Boolean,
    val timeToOutcome: String,
    val timestamp: String,
    val context: String
)

@Composable
fun DecisionOutcomeTrackingScreen(
    modifier: Modifier = Modifier
) {
    var decisions by remember { 
        mutableStateOf(listOf<DecisionRecord>())
    }
    var successRate by remember { mutableStateOf(0.87f) }
    var totalDecisions by remember { mutableStateOf(147) }
    var averageConfidence by remember { mutableStateOf(0.82f) }
    var confidenceAccuracy by remember { mutableStateOf(0.91f) }
    
    // Simulate decision tracking
    LaunchedEffect(Unit) {
        var count = 147
        while (true) {
            val success = Math.random() > 0.15
            val decision = listOf(
                "Execute strategy A",
                "Maintain current course",
                "Escalate priority",
                "Defer decision",
                "Combine strategies"
            ).random()
            
            val outcome = if (success) 
                listOf("Successful", "Optimal", "Completed", "Achieved goal").random()
            else 
                listOf("Partial success", "Suboptimal", "Need adjustment", "Retry needed").random()
            
            val conf = (Math.random() * 0.4f + 0.6f).toFloat()
            val rec = DecisionRecord(
                id = count,
                decision = decision,
                confidence = conf,
                outcome = outcome,
                success = success,
                timeToOutcome = "${(Math.random() * 300 + 50).toInt()}ms",
                timestamp = "now",
                context = listOf(
                    "Memory-driven",
                    "Reasoning-inferred",
                    "Evolution-optimized",
                    "Reflection-improved"
                ).random()
            )
            
            decisions = (decisions + rec).takeLast(20)
            count++
            totalDecisions = count
            
            // Update metrics
            val successCount = decisions.count { it.success }
            successRate = if (decisions.isNotEmpty()) 
                (successCount.toFloat() / decisions.size)
            else 0f
            
            averageConfidence = if (decisions.isNotEmpty())
                decisions.map { it.confidence }.average().toFloat()
            else 0f
            
            // Confidence calibration: how well confidence predicts success
            if (decisions.size >= 5) {
                val highConfidence = decisions.filter { it.confidence > 0.85f }
                val highConfidenceSuccess = highConfidence.count { it.success }
                confidenceAccuracy = if (highConfidence.isNotEmpty())
                    (highConfidenceSuccess.toFloat() / highConfidence.size).coerceIn(0.5f, 1f)
                else 0f
            }
            
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
                text = "Decision Outcomes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Key Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutcomeMetricCard(
                    label = "Success Rate",
                    value = "%.0f%%".format(successRate * 100),
                    trend = "↑",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF4CAF50)
                )
                OutcomeMetricCard(
                    label = "Total Decisions",
                    value = totalDecisions.toString(),
                    trend = "→",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2196F3)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutcomeMetricCard(
                    label = "Avg Confidence",
                    value = "%.0f%%".format(averageConfidence * 100),
                    trend = "↑",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFF9800)
                )
                OutcomeMetricCard(
                    label = "Calibration",
                    value = "%.0f%%".format(confidenceAccuracy * 100),
                    trend = "↑",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE91E63)
                )
            }
        }
        
        // Outcome Distribution
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
                        text = "Outcome Distribution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    val successCount = decisions.count { it.success }
                    val failureCount = decisions.size - successCount
                    
                    OutcomeBar("Successful", successCount, decisions.size, Color(0xFF4CAF50))
                    OutcomeBar("Partial/Retry", failureCount, decisions.size, Color(0xFFFF9800))
                }
            }
        }
        
        // Confidence Analysis
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
                        text = "Confidence Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    val highConf = decisions.filter { it.confidence > 0.85f }
                    val medConf = decisions.filter { it.confidence in 0.7f..0.85f }
                    val lowConf = decisions.filter { it.confidence < 0.7f }
                    
                    ConfidenceAnalysisRow(
                        label = "High Confidence (>85%)",
                        count = highConf.size,
                        successRate = if (highConf.isNotEmpty()) 
                            (highConf.count { it.success }.toFloat() / highConf.size) else 0f,
                        color = Color(0xFF4CAF50)
                    )
                    ConfidenceAnalysisRow(
                        label = "Medium Confidence (70-85%)",
                        count = medConf.size,
                        successRate = if (medConf.isNotEmpty())
                            (medConf.count { it.success }.toFloat() / medConf.size) else 0f,
                        color = Color(0xFFFF9800)
                    )
                    ConfidenceAnalysisRow(
                        label = "Low Confidence (<70%)",
                        count = lowConf.size,
                        successRate = if (lowConf.isNotEmpty())
                            (lowConf.count { it.success }.toFloat() / lowConf.size) else 0f,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
        
        // Recent Decisions
        if (decisions.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Decisions (Last ${decisions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            items(decisions.reversed()) { decision ->
                DecisionOutcomeCard(decision)
            }
        }
        
        // Decision Quality Insights
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Decision Quality Insights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    listOf(
                        "Confidence calibration is strong (91% predictive)",
                        "Strategy A has highest success rate (89%)",
                        "Average decision-to-outcome time: 145ms",
                        "Memory-driven decisions outperform others",
                        "Latest 5 decisions: 100% success rate"
                    ).forEach { insight ->
                        Text(
                            text = "• $insight",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun OutcomeMetricCard(
    label: String,
    value: String,
    trend: String,
    color: Color,
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = trend,
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(color)
            )
        }
    }
}

@Composable
private fun OutcomeBar(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp)
            Text(
                text = "$count/$total (${if (total > 0) (count * 100 / total) else 0}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = if (total > 0) (count.toFloat() / total) else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ConfidenceAnalysisRow(
    label: String,
    count: Int,
    successRate: Float,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp)
            Text(
                text = "$count decisions",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.0f%% success".format(successRate * 100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                LinearProgressIndicator(
                    progress = successRate,
                    modifier = Modifier.width(60.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DecisionOutcomeCard(
    decision: DecisionRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (decision.success)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = decision.decision,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = decision.context,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = if (decision.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (decision.success) "Success" else "Retry",
                    tint = if (decision.success) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Divider(modifier = Modifier.fillMaxWidth())
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Confidence",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f%%".format(decision.confidence * 100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Outcome",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = decision.outcome,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = decision.timeToOutcome,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
