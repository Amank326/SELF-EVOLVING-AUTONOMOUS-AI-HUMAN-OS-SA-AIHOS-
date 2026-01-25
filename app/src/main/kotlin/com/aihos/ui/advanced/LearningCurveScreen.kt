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
 * Learning Curve Screen - Visualize AI system learning progress and improvements
 * 
 * Features:
 * - Decision accuracy trends
 * - Confidence calibration progress
 * - Strategy fitness evolution
 * - Knowledge growth visualization
 */
@Composable
fun LearningCurveScreen(
    modifier: Modifier = Modifier
) {
    var decisionAccuracy by remember { mutableStateOf(0.65f) }
    var confidenceScore by remember { mutableStateOf(0.58f) }
    var strategyFitness by remember { mutableStateOf(0.72f) }
    var knowledgeScore by remember { mutableStateOf(0.81f) }
    var accuracyHistory by remember { 
        mutableStateOf(listOf<Float>())
    }
    var fitnessTrend by remember { 
        mutableStateOf(listOf<Float>())
    }
    
    // Simulate learning progression
    LaunchedEffect(Unit) {
        var acc = 0.65f
        var fit = 0.72f
        var conf = 0.58f
        var know = 0.81f
        
        while (true) {
            // Gradual improvement
            acc = (acc + (Math.random() * 0.05f - 0.01f)).coerceIn(0f, 1f).toFloat()
            fit = (fit + (Math.random() * 0.04f - 0.01f)).coerceIn(0f, 1f).toFloat()
            conf = (conf + (Math.random() * 0.06f - 0.02f)).coerceIn(0f, 1f).toFloat()
            know = (know + (Math.random() * 0.03f - 0.005f)).coerceIn(0f, 1f).toFloat()
            
            decisionAccuracy = acc
            strategyFitness = fit
            confidenceScore = conf
            knowledgeScore = know
            
            accuracyHistory = (accuracyHistory + acc).takeLast(15)
            fitnessTrend = (fitnessTrend + fit).takeLast(15)
            
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
                text = "Learning Progress",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Main Learning Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LearningMetricCard(
                    label = "Decision Accuracy",
                    score = decisionAccuracy,
                    trend = "↑",
                    modifier = Modifier.weight(1f)
                )
                LearningMetricCard(
                    label = "Confidence",
                    score = confidenceScore,
                    trend = "↑",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LearningMetricCard(
                    label = "Strategy Fitness",
                    score = strategyFitness,
                    trend = "↑",
                    modifier = Modifier.weight(1f)
                )
                LearningMetricCard(
                    label = "Knowledge Base",
                    score = knowledgeScore,
                    trend = "↑",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Accuracy Trend
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
                        text = "Decision Accuracy Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    TrendLineChart(
                        data = accuracyHistory,
                        color = Color(0xFF2196F3),
                        maxValue = 1f
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current: %.0f%%".format(decisionAccuracy * 100),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Peak: %.0f%%".format((accuracyHistory.maxOrNull() ?: 0f) * 100),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Fitness Evolution
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
                        text = "Strategy Fitness Evolution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    TrendLineChart(
                        data = fitnessTrend,
                        color = Color(0xFF4CAF50),
                        maxValue = 1f
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current: %.2f".format(strategyFitness),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Improvement: +%.1f%%".format((strategyFitness - 0.72f) * 100),
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        // Learning Milestones
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
                        text = "🏆 Learning Milestones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    listOf(
                        Triple("70% Accuracy Reached", "✅", true),
                        Triple("80% Accuracy Reached", if (decisionAccuracy >= 0.8f) "✅" else "◯", decisionAccuracy >= 0.8f),
                        Triple("90% Accuracy Reached", if (decisionAccuracy >= 0.9f) "✅" else "◯", decisionAccuracy >= 0.9f),
                        Triple("Confidence Calibrated", if (confidenceScore >= 0.8f) "✅" else "◯", confidenceScore >= 0.8f),
                        Triple("100 Decisions Learned", "✅", true),
                        Triple("Perfect Strategy Evolved", if (strategyFitness >= 0.95f) "✅" else "◯", strategyFitness >= 0.95f)
                    ).forEach { (milestone, status, reached) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = milestone,
                                fontSize = 13.sp,
                                color = if (reached) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = status,
                                fontSize = 14.sp,
                                color = if (reached) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        // Key Insights
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
                        text = "📊 Key Insights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    listOf(
                        "Decision accuracy improving steadily (+${(decisionAccuracy - 0.65f).let { if (it > 0) "%.1f%%".format(it * 100) else "0%" }})",
                        "Genetic algorithm converging to optimal strategy",
                        "Memory consolidation has freed ${(0.15f * 100).toInt()}% of capacity",
                        "Error patterns detected: ${(Math.random() * 5).toInt()} new patterns",
                        "Knowledge base grown by ${(Math.random() * 30).toInt()}% since last epoch"
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
private fun LearningMetricCard(
    label: String,
    score: Float,
    trend: String,
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
                    text = "%.0f%%".format(score * 100),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = trend,
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            
            LinearProgressIndicator(
                progress = score,
                modifier = Modifier.fillMaxWidth(0.8f),
                color = Color(0xFF2196F3),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TrendLineChart(
    data: List<Float>,
    color: Color,
    maxValue: Float
) {
    if (data.isEmpty()) {
        Text(
            text = "Gathering data...",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { value ->
                val height = ((value / maxValue) * 80f).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height)
                        .background(color.copy(alpha = 0.7f), shape = MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}
