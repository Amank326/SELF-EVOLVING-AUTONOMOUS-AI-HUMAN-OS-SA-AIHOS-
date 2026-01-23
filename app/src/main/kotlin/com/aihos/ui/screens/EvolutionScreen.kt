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
fun EvolutionScreen(viewModel: SAIHOSViewModel) {
    val evolutionReport by viewModel.evolutionReport.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Evolution Timeline",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(8.dp)
        )
        
        // Evolution Stats
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
                    "Rule Evolution Statistics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                StatRow("Total Rules", evolutionReport.totalRules.toString())
                StatRow("Active Rules", evolutionReport.activeRules.toString())
                StatRow("Deprecated Rules", evolutionReport.deprecatedRules.toString())
                StatRow("New This Session", evolutionReport.newRulesThisSession.toString())
            }
        }
        
        // How Evolution Works
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "How SA-AIHOS Evolves",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                EvolutionStep(
                    number = "1",
                    title = "THINK",
                    description = "AI reasons about what to do based on context and learned rules"
                )
                
                EvolutionStep(
                    number = "2",
                    title = "ACT",
                    description = "AI executes autonomous action within safety constraints"
                )
                
                EvolutionStep(
                    number = "3",
                    title = "REFLECT",
                    description = "AI analyzes the outcome to understand what happened and why"
                )
                
                EvolutionStep(
                    number = "4",
                    title = "EVOLVE",
                    description = "AI updates its rules based on reflection insights"
                )
            }
        }
    }
}

@Composable
fun EvolutionStep(number: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A3A3A), shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = AccentColor
        ) {
            Text(
                number,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 14.sp
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                description,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
