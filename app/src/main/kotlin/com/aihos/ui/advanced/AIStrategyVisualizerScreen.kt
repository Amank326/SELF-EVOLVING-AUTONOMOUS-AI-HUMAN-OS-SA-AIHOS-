package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 * AI Strategy Visualizer Screen - Visualize current strategy execution and evolution
 * 
 * Features:
 * - Current active strategy display
 * - Strategy parameter visualization
 * - Strategy success metrics
 * - Competing strategies comparison
 * - Strategy evolution history
 */

data class AIStrategy(
    val name: String,
    val type: String,
    val fitness: Float,
    val wins: Int,
    val losses: Int,
    val parameters: Map<String, String>,
    val status: String
)

@Composable
fun AIStrategyVisualizerScreen(
    modifier: Modifier = Modifier
) {
    var currentStrategy by remember {
        mutableStateOf(AIStrategy(
            name = "Adaptive Ensemble",
            type = "Multi-Strategy",
            fitness = 0.91f,
            wins = 187,
            losses = 19,
            parameters = mapOf(
                "memory_weight" to "0.35",
                "reasoning_weight" to "0.30",
                "evolution_weight" to "0.20",
                "reflection_weight" to "0.15"
            ),
            status = "ACTIVE"
        ))
    }
    
    var strategies by remember {
        mutableStateOf(listOf(
            AIStrategy("Aggressive GA", "Genetic Algorithm", 0.81f, 142, 28, emptyMap(), "IDLE"),
            AIStrategy("Conservative QL", "Q-Learning", 0.78f, 98, 52, emptyMap(), "IDLE"),
            AIStrategy("Memory-First", "Memory-Driven", 0.85f, 165, 35, emptyMap(), "WARM"),
            AIStrategy("Reasoning-Deep", "Reasoning-Heavy", 0.83f, 156, 44, emptyMap(), "IDLE"),
            AIStrategy("Balanced Blend", "Ensemble", 0.87f, 173, 27, emptyMap(), "STANDBY")
        ))
    }
    
    var generationCount by remember { mutableStateOf(187) }
    var strategiesEvaluated by remember { mutableStateOf(1247) }
    
    // Simulate strategy evolution
    LaunchedEffect(Unit) {
        while (true) {
            // Update current strategy
            currentStrategy = currentStrategy.copy(
                fitness = (currentStrategy.fitness + (Math.random() * 0.05f - 0.01f)).coerceIn(0.8f, 0.99f).toFloat(),
                wins = currentStrategy.wins + (Math.random() * 5 - 1).toInt(),
                losses = currentStrategy.losses + (Math.random() * 2 - 0.5).toInt()
            )
            
            // Update competing strategies
            strategies = strategies.map { strategy ->
                strategy.copy(
                    fitness = (strategy.fitness + (Math.random() * 0.03f - 0.01f)).coerceIn(0.6f, 0.95f).toFloat(),
                    wins = strategy.wins + (Math.random() * 3 - 0.5).toInt(),
                    losses = strategy.losses + (Math.random() * 2 - 0.3).toInt()
                )
            }
            
            generationCount++
            strategiesEvaluated += (Math.random() * 5).toInt()
            
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
                text = "Strategy Visualizer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Current Active Strategy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.border(
                    width = 2.dp,
                    color = Color(0xFF4CAF50)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Strategy",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentStrategy.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        
                        Surface(
                            modifier = Modifier
                                .background(
                                    Color(0xFF4CAF50),
                                    shape = MaterialTheme.shapes.small
                                ),
                            color = Color(0xFF4CAF50)
                        ) {
                            Text(
                                text = " ACTIVE ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(6.dp, 3.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fitness Score",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.2f".format(currentStrategy.fitness),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Success Rate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val total = currentStrategy.wins + currentStrategy.losses
                            val rate = if (total > 0) (currentStrategy.wins.toFloat() / total * 100) else 0f
                            Text(
                                text = "%.0f%%".format(rate),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Trials",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (currentStrategy.wins + currentStrategy.losses).toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Strategy Parameters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    currentStrategy.parameters.forEach { (param, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = param.replace("_", " ").lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }
        }
        
        // Evolution Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StrategyMetricCard(
                    label = "Generation",
                    value = generationCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StrategyMetricCard(
                    label = "Strategies Tested",
                    value = strategiesEvaluated.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Competing Strategies
        item {
            Text(
                text = "Competing Strategies",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        items(strategies.size) { index ->
            val strategy = strategies[index]
            StrategyComparisonCard(strategy)
        }
        
        // Strategy Selection Logic
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
                        text = "Strategy Selection Logic",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    StrategySelectionRule("Top Performer", currentStrategy.name, "✓ Selected")
                    StrategySelectionRule("Fitness Threshold", "> 0.80", "✓ Passed")
                    StrategySelectionRule("Recent Wins", "${currentStrategy.wins} consecutive", "✓ Strong")
                    StrategySelectionRule("Diversity Check", "5/8 unique approaches", "✓ Balanced")
                    StrategySelectionRule("Exploit-Explore", "Exploitation phase", "✓ Active")
                }
            }
        }
        
        // Evolution Insights
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
                        text = "📈 Evolution Insights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    listOf(
                        "Current strategy outperforms baseline by 23%",
                        "Fitness plateau at generation ${generationCount}: strategy converging",
                        "Top 3 strategies show similar performance - ensemble may be optimal",
                        "Memory-First strategy improving rapidly (+0.12 in last 10 gen)",
                        "No strategy dominance yet - continue evolution for diversity"
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
private fun StrategyMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
private fun StrategyComparisonCard(
    strategy: AIStrategy
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                        text = strategy.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = strategy.type,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    modifier = Modifier.background(
                        when (strategy.status) {
                            "ACTIVE" -> Color(0xFF4CAF50)
                            "WARM" -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ),
                    color = when (strategy.status) {
                        "ACTIVE" -> Color(0xFF4CAF50)
                        "WARM" -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = " ${strategy.status} ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(6.dp, 3.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fitness",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f".format(strategy.fitness),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wins",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strategy.wins.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Losses",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strategy.losses.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rate",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val total = strategy.wins + strategy.losses
                    val rate = if (total > 0) (strategy.wins.toFloat() / total * 100) else 0f
                    Text(
                        text = "%.0f%%".format(rate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = strategy.fitness,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2196F3),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StrategySelectionRule(
    rule: String,
    value: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}
