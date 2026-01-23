package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihos.ui.viewmodel.SAIHOSViewModel
import com.aihos.ui.*

@Composable
fun MemoryScreen(viewModel: SAIHOSViewModel) {
    val memoryStats by viewModel.memoryStats.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Memory Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(8.dp)
        )
        
        // Stats Cards
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
                StatRow("Total Episodes", memoryStats.totalEpisodes.toString())
                StatRow("Active Rules", memoryStats.totalRules.toString())
                StatRow("Learned Facts", memoryStats.totalFacts.toString())
                
                Divider(color = Color(0xFF4A4A4A), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    "Memory Usage",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                LinearProgressIndicator(
                    progress = { memoryStats.memoryUsagePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AccentColor,
                    trackColor = Color(0xFF4A4A4A)
                )
                Text(
                    "${memoryStats.memoryUsagePercent}% of 500MB",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        
        // Information
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "About Memory",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Episodes: Each autonomous decision is recorded as an episode with context, " +
                    "reasoning, and outcome. Episodes are used for reflection and learning.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "Rules: Behavioral rules guide AI decisions. They evolve based on success rates.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "Facts: Semantic facts represent learned knowledge about user preferences " +
                    "and behavior patterns.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
