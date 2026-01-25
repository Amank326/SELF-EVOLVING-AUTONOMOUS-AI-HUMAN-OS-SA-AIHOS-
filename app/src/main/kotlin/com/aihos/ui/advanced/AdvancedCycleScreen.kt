package com.aihos.ui.advanced

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Advanced Cycle Execution Screen - Real-time visualization of THINK→ACT→REFLECT→EVOLVE cycle
 * 
 * Features:
 * - Animated cycle execution flow
 * - Phase-by-phase visualization
 * - Performance timeline
 * - Decision output display
 * - Error recovery visualization
 */
@Composable
fun AdvancedCycleScreen(
    modifier: Modifier = Modifier
) {
    var currentPhase by remember { mutableStateOf<CyclePhase>(CyclePhase.IDLE) }
    var cycleCount by remember { mutableStateOf(0L) }
    var currentDecision by remember { mutableStateOf("Waiting...") }
    var cycleStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var phaseTimes by remember { mutableStateOf(mapOf<String, Long>()) }
    
    // Simulate cycle execution
    LaunchedEffect(Unit) {
        while (true) {
            // Think Phase
            currentPhase = CyclePhase.THINK
            val thinkStart = System.currentTimeMillis()
            delay((50 + Math.random() * 150).toLong())
            phaseTimes = phaseTimes + ("THINK" to (System.currentTimeMillis() - thinkStart))
            
            // Act Phase
            currentPhase = CyclePhase.ACT
            val actStart = System.currentTimeMillis()
            delay((30 + Math.random() * 100).toLong())
            phaseTimes = phaseTimes + ("ACT" to (System.currentTimeMillis() - actStart))
            currentDecision = listOf(
                "Execute planning routine",
                "Initiate focus mode",
                "Optimize resource usage",
                "Update learning parameters",
                "Trigger adaptation protocol"
            ).random()
            
            // Reflect Phase
            currentPhase = CyclePhase.REFLECT
            val reflectStart = System.currentTimeMillis()
            delay((40 + Math.random() * 120).toLong())
            phaseTimes = phaseTimes + ("REFLECT" to (System.currentTimeMillis() - reflectStart))
            
            // Evolve Phase
            currentPhase = CyclePhase.EVOLVE
            val evolveStart = System.currentTimeMillis()
            delay((50 + Math.random() * 150).toLong())
            phaseTimes = phaseTimes + ("EVOLVE" to (System.currentTimeMillis() - evolveStart))
            
            // Cycle complete
            currentPhase = CyclePhase.COMPLETE
            cycleCount++
            delay(1000)
            
            currentPhase = CyclePhase.IDLE
            delay(500)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "AI Cycle Execution",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Cycle Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cycle #$cycleCount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Status: ${currentPhase.displayName}",
                    fontSize = 16.sp,
                    color = getPhaseColor(currentPhase),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Cycle Visualization
            CycleVisualization(
                currentPhase = currentPhase,
                phaseTimes = phaseTimes
            )
            
            // Current Decision
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
                    Text(
                        text = "Current Decision",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = currentDecision,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Phase Details
            PhaseDetailsCard(phaseTimes = phaseTimes)
        }
    }
}

@Composable
private fun CycleVisualization(
    currentPhase: CyclePhase,
    phaseTimes: Map<String, Long>
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // THINK Phase
            PhaseStep(
                label = "THINK",
                description = "Reasoning & Analysis",
                isActive = currentPhase == CyclePhase.THINK,
                isCompleted = currentPhase.ordinal > CyclePhase.THINK.ordinal,
                duration = phaseTimes["THINK"] ?: 0L,
                order = 1
            )
            
            // Connection
            PhaseConnection(isActive = currentPhase.ordinal >= CyclePhase.ACT.ordinal)
            
            // ACT Phase
            PhaseStep(
                label = "ACT",
                description = "Decision Execution",
                isActive = currentPhase == CyclePhase.ACT,
                isCompleted = currentPhase.ordinal > CyclePhase.ACT.ordinal,
                duration = phaseTimes["ACT"] ?: 0L,
                order = 2
            )
            
            // Connection
            PhaseConnection(isActive = currentPhase.ordinal >= CyclePhase.REFLECT.ordinal)
            
            // REFLECT Phase
            PhaseStep(
                label = "REFLECT",
                description = "Outcome Analysis",
                isActive = currentPhase == CyclePhase.REFLECT,
                isCompleted = currentPhase.ordinal > CyclePhase.REFLECT.ordinal,
                duration = phaseTimes["REFLECT"] ?: 0L,
                order = 3
            )
            
            // Connection
            PhaseConnection(isActive = currentPhase.ordinal >= CyclePhase.EVOLVE.ordinal)
            
            // EVOLVE Phase
            PhaseStep(
                label = "EVOLVE",
                description = "Self-Improvement",
                isActive = currentPhase == CyclePhase.EVOLVE,
                isCompleted = currentPhase.ordinal > CyclePhase.EVOLVE.ordinal,
                duration = phaseTimes["EVOLVE"] ?: 0L,
                order = 4
            )
        }
    }
}

@Composable
private fun PhaseStep(
    label: String,
    description: String,
    isActive: Boolean,
    isCompleted: Boolean,
    duration: Long,
    order: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Phase Indicator
        val backgroundColor = when {
            isActive -> Color(0xFF2196F3)
            isCompleted -> Color(0xFF4CAF50)
            else -> Color.Gray
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                // Pulsing animation
                val pulseAnimation = rememberInfiniteTransition(label = "pulse")
                val scale by pulseAnimation.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulsing"
                )
                Box(
                    modifier = Modifier
                        .size(40.dp * scale)
                        .clip(CircleShape)
                        .background(backgroundColor.copy(alpha = 0.3f))
                )
            }
            
            Text(
                text = order.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Phase Details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (duration > 0) {
                Text(
                    text = "$duration ms",
                    fontSize = 12.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PhaseConnection(isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(
                    if (isActive) Color(0xFF4CAF50) else Color.Gray,
                    RoundedCornerShape(1.dp)
                )
                .padding(start = 19.dp)
        )
    }
}

@Composable
private fun PhaseDetailsCard(phaseTimes: Map<String, Long>) {
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
            Text(
                text = "Phase Timing",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            listOf("THINK", "ACT", "REFLECT", "EVOLVE").forEach { phase ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = phase,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${phaseTimes[phase] ?: 0} ms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Progress bar
                LinearProgressIndicator(
                    progress = (phaseTimes[phase] ?: 0L).toFloat() / 200f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF2196F3),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Total time
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Cycle Time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${phaseTimes.values.sum()} ms",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class CyclePhase(val displayName: String) {
    IDLE("Ready"),
    THINK("Thinking 🧠"),
    ACT("Acting 🎯"),
    REFLECT("Reflecting 🪞"),
    EVOLVE("Evolving 🧬"),
    COMPLETE("Complete ✅")
}

private fun getPhaseColor(phase: CyclePhase): Color {
    return when (phase) {
        CyclePhase.IDLE -> Color.Gray
        CyclePhase.THINK -> Color(0xFF2196F3) // Blue
        CyclePhase.ACT -> Color(0xFFFF9800) // Orange
        CyclePhase.REFLECT -> Color(0xFF9C27B0) // Purple
        CyclePhase.EVOLVE -> Color(0xFF4CAF50) // Green
        CyclePhase.COMPLETE -> Color(0xFF4CAF50) // Green
    }
}
