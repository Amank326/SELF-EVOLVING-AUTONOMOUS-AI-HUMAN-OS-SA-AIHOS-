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
fun SettingsScreen(viewModel: SAIHOSViewModel) {
    val autonomyLevel by viewModel.autonomyLevel.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(8.dp)
        )
        
        // Autonomy Level Settings
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
                    "Autonomy Control",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Text(
                    "Choose how much the AI can act independently:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                
                com.aihos.ai.autonomy.AutonomyLevel.values().forEach { level ->
                    SettingOption(
                        level.name,
                        isSelected = autonomyLevel == level,
                        onSelect = { viewModel.updateAutonomyLevel(level) }
                    )
                }
            }
        }
        
        // About
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
                    "About SA-AIHOS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Text(
                    "Self-Evolving Autonomous AI Human OS",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                
                Text(
                    "Version: 1.0.0 Alpha",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                
                Text(
                    "An advanced AI system that thinks, acts, reflects, and evolves " +
                    "based on outcomes and learning.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingOption(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Button(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) AccentColor else Color(0xFF3A3A3A),
            contentColor = if (isSelected) Color.Black else TextPrimary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
