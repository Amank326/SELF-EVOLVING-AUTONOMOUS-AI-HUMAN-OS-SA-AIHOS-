package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihos.ui.viewmodel.SAIHOSViewModel
import com.aihos.ui.viewmodel.SystemStatus
import com.aihos.ui.*

/**
 * Dashboard Screen: Main overview of system status
 */
@Composable
fun DashboardScreen(viewModel: SAIHOSViewModel) {
    val systemStatus by viewModel.systemStatus.collectAsState()
    val autonomyLevel by viewModel.autonomyLevel.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Status Card
        StatusCard(systemStatus = systemStatus, viewModel = viewModel)
        
        // Autonomy Level Card
        AutonomyCard(autonomyLevel = autonomyLevel, viewModel = viewModel)
        
        // Recent Decisions
        RecentDecisionsCard(viewModel = viewModel)
    }
}

@Composable
fun StatusCard(systemStatus: SystemStatus, viewModel: SAIHOSViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "System Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            val (statusText, statusColor) = when (systemStatus) {
                SystemStatus.Idle -> "Idle" to Color(0xFFB0B0B0)
                SystemStatus.Running -> "Running" to Color(0xFF4CAF50)
                SystemStatus.Paused -> "Paused" to Color(0xFFFF9800)
                is SystemStatus.Error -> "Error: ${systemStatus.message}" to Color(0xFFf44336)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(statusText, color = statusColor, fontSize = 16.sp)
                
                Button(
                    onClick = {
                        if (systemStatus == SystemStatus.Idle) {
                            viewModel.startAutonomousLoop()
                        } else {
                            viewModel.stopAutonomousLoop()
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(if (systemStatus == SystemStatus.Idle) "Start" else "Stop")
                }
            }
        }
    }
}

@Composable
fun AutonomyCard(
    autonomyLevel: com.aihos.ai.autonomy.AutonomyLevel,
    viewModel: SAIHOSViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Autonomy Level",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Text(
                autonomyLevel.name,
                color = AccentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                "Current setting controls how much the AI can act independently",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RecentDecisionsCard(viewModel: SAIHOSViewModel) {
    val recentDecisions by viewModel.recentDecisions.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Recent Decisions (Last 10)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            if (recentDecisions.isEmpty()) {
                Text(
                    "No decisions yet",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            } else {
                recentDecisions.take(5).forEach { decision ->
                    DecisionItem(decision)
                }
            }
            
            Button(
                onClick = { viewModel.loadRecentDecisions() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Text("Refresh Decisions")
            }
        }
    }
}

@Composable
fun DecisionItem(decision: com.aihos.ui.viewmodel.DecisionDisplay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A3A3A), shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                decision.action,
                color = AccentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                decision.timestamp,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Text(
            "Outcome: ${decision.outcome}",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
