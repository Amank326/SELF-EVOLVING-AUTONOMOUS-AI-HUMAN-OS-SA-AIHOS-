package com.aihos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihos.system.demo.AICognitiveState
import com.aihos.system.demo.DemoModeConfig
import com.aihos.system.demo.DemoModeManager
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * AI Cognitive State Indicator
 *
 * Displays the current AI cognitive state with visual feedback.
 * Works in both demo mode and normal mode, but is most useful in demo mode
 * for showing observers what the AI is doing at any moment.
 */
@Composable
fun AICognitiveStateIndicator(
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val config by demoModeManager.demoConfig.collectAsState()
    val state by demoModeManager.aiCognitiveState.collectAsState()
    
    if (!config.showCognitiveStateIndicator) {
        return  // Hidden if disabled in config
    }

    val stateColor = when (state) {
        AICognitiveState.IDLE -> Color(0xFF4CAF50)        // Green
        AICognitiveState.THINKING -> Color(0xFF2196F3)    // Blue
        AICognitiveState.REFLECTING -> Color(0xFFFF9800)  // Orange
        AICognitiveState.EVOLVING -> Color(0xFFE91E63)    // Pink/Red
        AICognitiveState.PAUSED -> Color(0xFF9E9E9E)      // Gray
    }

    val animatedColor by animateColorAsState(
        targetValue = stateColor,
        animationSpec = tween(300, easing = LinearEasing)
    )

    if (compact) {
        CompactStateIndicator(
            state = state,
            color = animatedColor,
            demoEnabled = config.isEnabled,
            modifier = modifier
        )
    } else {
        FullStateIndicator(
            state = state,
            color = animatedColor,
            demoEnabled = config.isEnabled,
            description = demoModeManager.getStateDescription(),
            modifier = modifier
        )
    }
}

/**
 * Compact indicator (for top corner or toolbar)
 */
@Composable
private fun CompactStateIndicator(
    state: AICognitiveState,
    color: Color,
    demoEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color = color, shape = CircleShape)
        )

        // Demo mode indicator badge
        if (demoEnabled) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = Color(0xFFFFEB3B),  // Yellow
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Full state indicator with label and animation
 */
@Composable
private fun FullStateIndicator(
    state: AICognitiveState,
    color: Color,
    demoEnabled: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // State label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = color, shape = CircleShape)
            )
            Text(
                text = state.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Description
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )

        // Demo mode badge
        if (demoEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                color = Color(0xFFFFEB3B),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "🎬 DEMO MODE",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentCenter(),
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Demo Mode Status Panel
 *
 * Shows detailed demo mode information:
 * - Demo mode on/off toggle
 * - Current configuration
 * - Session timer
 * - AI statistics
 */
@Composable
fun DemoModeStatusPanel(
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier
) {
    val config by demoModeManager.demoConfig.collectAsState()
    val state by demoModeManager.aiCognitiveState.collectAsState()
    val isRunning by demoModeManager.isRunning.collectAsState()
    val duration by demoModeManager.demoDurationMs.collectAsState()

    Column(
        modifier = modifier
            .background(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DEMO MODE",
                color = Color(0xFFFFEB3B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (config.isEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (config.isEnabled) "ACTIVE" else "INACTIVE",
                    color = if (config.isEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.1f))

        // Configuration display
        if (config.isEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow("Cognition Interval", "${config.cognitionIntervalMs}ms")
                StatusRow("Freeze Evolution", if (config.freezeEvolution) "YES" else "NO")
                StatusRow("Freeze Reflection", if (config.freezeReflection) "YES" else "NO")
                StatusRow("Demo Duration", demoModeManager.getFormattedDuration())
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                StatusRow("Current AI State", state.name)
            }
        } else {
            Text(
                text = "Demo mode is disabled. Enable for presentation-safe AI behavior.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Quick Demo Preset Buttons
 *
 * Provides one-tap access to common demo configurations
 */
@Composable
fun DemoPresetsPanel(
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "DEMO PRESETS",
            color = Color(0xFFFFEB3B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        DemoPresetButton(
            label = "🎬 Quick Demo (3min)",
            description = "Fast setup for quick demos"
        ) {
            demoModeManager.enableDemoMode(DemoModeManager.quickDemoConfig())
        }

        DemoPresetButton(
            label = "📱 Screen Recording",
            description = "Ultra-predictable for recording"
        ) {
            demoModeManager.enableDemoMode(DemoModeManager.screenRecordingConfig())
        }

        DemoPresetButton(
            label = "🎤 Live Presentation",
            description = "Balanced for live demo"
        ) {
            demoModeManager.enableDemoMode(DemoModeManager.livePresentationConfig())
        }

        DemoPresetButton(
            label = "🔧 Dev Demo",
            description = "Realistic but observable"
        ) {
            demoModeManager.enableDemoMode(DemoModeManager.devDemoConfig())
        }

        Button(
            onClick = { demoModeManager.disableDemoMode() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9E9E9E)
            )
        ) {
            Text("Disable Demo Mode")
        }
    }
}

@Composable
private fun DemoPresetButton(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3).copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
