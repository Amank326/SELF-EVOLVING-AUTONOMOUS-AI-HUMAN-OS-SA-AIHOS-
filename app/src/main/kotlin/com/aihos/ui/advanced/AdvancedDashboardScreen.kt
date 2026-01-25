package com.aihos.ui.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Advanced Dashboard Screen - Master hub for all AI system monitoring
 * 
 * Features:
 * - Navigation to all advanced screens
 * - System status overview
 * - Quick access to critical metrics
 * - Real-time system health summary
 */
@Composable
fun AdvancedDashboardScreen(
    onNavigateToMetrics: () -> Unit = {},
    onNavigateToCycle: () -> Unit = {},
    onNavigateToPerformance: () -> Unit = {},
    onNavigateToErrors: () -> Unit = {},
    onNavigateToLearning: () -> Unit = {},
    onNavigateToDecisions: () -> Unit = {},
    onNavigateToState: () -> Unit = {},
    onNavigateToStrategy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var systemStatus by remember { mutableStateOf("OPERATIONAL") }
    var overallHealth by remember { mutableStateOf(0.85f) }
    var activeDecisions by remember { mutableStateOf(147) }
    var latestAccuracy by remember { mutableStateOf(0.87f) }
    
    // Simulate system updates
    LaunchedEffect(Unit) {
        while (true) {
            overallHealth = (overallHealth + (Math.random() * 0.1f - 0.05f)).coerceIn(0.6f, 1f).toFloat()
            activeDecisions = (activeDecisions + ((Math.random() * 10 - 3).toInt())).coerceAtLeast(100)
            latestAccuracy = (latestAccuracy + (Math.random() * 0.05f - 0.01f)).coerceIn(0.7f, 0.99f).toFloat()
            kotlinx.coroutines.delay(4000)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SA-AIHOS Dashboard",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Advanced AI System Orchestration",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Status Overview
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "System Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        Color(0xFF4CAF50),
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                            )
                        }
                        
                        Text(
                            text = systemStatus,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardQuickStat("Overall Health", "%.0f%%".format(overallHealth * 100))
                            DashboardQuickStat("Active Decisions", activeDecisions.toString())
                            DashboardQuickStat("Latest Accuracy", "%.1f%%".format(latestAccuracy * 100))
                        }
                    }
                }
            }
            
            // Quick Action Grid
            item {
                Text(
                    text = "System Controls",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        DashboardNavigationCard(
                            title = "Metrics",
                            subtitle = "System Health",
                            icon = Icons.Default.Info,
                            color = Color(0xFF2196F3),
                            onClick = onNavigateToMetrics
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Cycle",
                            subtitle = "Execution Flow",
                            icon = Icons.Default.Refresh,
                            color = Color(0xFFFF9800),
                            onClick = onNavigateToCycle
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Performance",
                            subtitle = "Analytics",
                            icon = Icons.Default.Settings,
                            color = Color(0xFF9C27B0),
                            onClick = onNavigateToPerformance
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Errors",
                            subtitle = "Patterns",
                            icon = Icons.Default.Warning,
                            color = Color(0xFFF44336),
                            onClick = onNavigateToErrors
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Learning",
                            subtitle = "Progress",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF4CAF50),
                            onClick = onNavigateToLearning
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Decisions",
                            subtitle = "Outcomes",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF00BCD4),
                            onClick = onNavigateToDecisions
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "State",
                            subtitle = "Inspector",
                            icon = Icons.Default.Build,
                            color = Color(0xFFE91E63),
                            onClick = onNavigateToState
                        )
                    }
                    item {
                        DashboardNavigationCard(
                            title = "Strategy",
                            subtitle = "Visualizer",
                            icon = Icons.Default.Psychology,
                            color = Color(0xFF673AB7),
                            onClick = onNavigateToStrategy
                        )
                    }
                }
            }
            
            // Active Systems Overview
            item {
                Text(
                    text = "Active AI Systems",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemStatusRow("Memory Layer", "Active", 0.92f, Color(0xFF2196F3))
                    SystemStatusRow("Reasoning Engine", "Active", 0.88f, Color(0xFF4CAF50))
                    SystemStatusRow("Evolution Engine", "Active", 0.81f, Color(0xFFFF9800))
                    SystemStatusRow("Reflection Layer", "Active", 0.85f, Color(0xFFE91E63))
                    SystemStatusRow("Orchestration", "Active", 0.96f, Color(0xFF00BCD4))
                }
            }
            
            // Recent Activity
            item {
                Text(
                    text = "Recent Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
                
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
                        listOf(
                            "✓ Completed decision cycle #147 with 87% confidence",
                            "✓ Memory consolidation freed 2.3 MB capacity",
                            "⚠ Warning: Reasoning latency increased 12%",
                            "✓ Evolutionary fitness improved: 0.81→0.85",
                            "✓ Reflection identified 3 new error patterns"
                        ).forEach { activity ->
                            Text(
                                text = activity,
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
}

@Composable
private fun DashboardQuickStat(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DashboardNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.border(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SystemStatusRow(
    systemName: String,
    status: String,
    health: Float,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = systemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = status,
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = health,
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "%.0f%%".format(health * 100),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
