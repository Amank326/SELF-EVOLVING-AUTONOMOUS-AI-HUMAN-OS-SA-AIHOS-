package com.aihos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihos.system.demo.DemoModeConfig
import com.aihos.system.demo.DemoModeManager
import com.aihos.ui.components.DemoModeStatusPanel
import com.aihos.ui.components.DemoPresetsPanel

/**
 * Demo Mode Settings Screen
 *
 * Provides comprehensive controls for demo mode configuration:
 * - Enable/disable demo mode
 * - Select presets (Quick, Screen Recording, Live Presentation, Dev)
 * - View current status and statistics
 * - Fine-tune advanced settings
 */
@Composable
fun DemoModeSettingsScreen(
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val config by demoModeManager.demoConfig.collectAsState()
    val isRunning by demoModeManager.isRunning.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "🎬 DEMO MODE SETTINGS",
            color = Color(0xFFFFEB3B),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Current Status Panel
        DemoModeStatusPanel(
            demoModeManager = demoModeManager,
            modifier = Modifier.fillMaxWidth()
        )

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Quick Presets
        DemoPresetsPanel(
            demoModeManager = demoModeManager,
            modifier = Modifier.fillMaxWidth()
        )

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Advanced Settings (collapsed by default)
        var showAdvanced by remember { mutableStateOf(false) }

        Button(
            onClick = { showAdvanced = !showAdvanced },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242)
            )
        ) {
            Text(
                if (showAdvanced) "▼ Advanced Settings" else "► Advanced Settings"
            )
        }

        if (showAdvanced && config.isEnabled) {
            AdvancedSettingsPanel(
                config = config,
                onConfigChange = { newConfig ->
                    demoModeManager.updateConfig { newConfig }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // FAQ
        FAQSection()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AdvancedSettingsPanel(
    config: DemoModeConfig,
    onConfigChange: (DemoModeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Advanced Configuration",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Cognition Interval Slider
        SettingSlider(
            label = "Cognition Interval",
            value = config.cognitionIntervalMs.toFloat(),
            onValueChange = { newValue ->
                onConfigChange(config.copy(cognitionIntervalMs = newValue.toLong()))
            },
            valueRange = 1000f..10000f,
            unit = "ms"
        )

        // Transition Duration Slider
        if (config.useSlowTransitions) {
            SettingSlider(
                label = "State Transition Duration",
                value = config.transitionDurationMs.toFloat(),
                onValueChange = { newValue ->
                    onConfigChange(config.copy(transitionDurationMs = newValue.toLong()))
                },
                valueRange = 100f..1500f,
                unit = "ms"
            )
        }

        // Pause Between Cycles
        SettingSlider(
            label = "Pause Between Cycles",
            value = config.pauseBetweenCyclesMs.toFloat(),
            onValueChange = { newValue ->
                onConfigChange(config.copy(pauseBetweenCyclesMs = newValue.toLong()))
            },
            valueRange = 0f..2000f,
            unit = "ms"
        )

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Toggle Options
        SettingToggle(
            label = "Freeze Evolution",
            value = config.freezeEvolution,
            onValueChange = { newValue ->
                onConfigChange(config.copy(freezeEvolution = newValue))
            }
        )

        SettingToggle(
            label = "Freeze Reflection",
            value = config.freezeReflection,
            onValueChange = { newValue ->
                onConfigChange(config.copy(freezeReflection = newValue))
            }
        )

        SettingToggle(
            label = "Use Slow Transitions",
            value = config.useSlowTransitions,
            onValueChange = { newValue ->
                onConfigChange(config.copy(useSlowTransitions = newValue))
            }
        )

        SettingToggle(
            label = "Show State Indicator",
            value = config.showCognitiveStateIndicator,
            onValueChange = { newValue ->
                onConfigChange(config.copy(showCognitiveStateIndicator = newValue))
            }
        )

        SettingToggle(
            label = "Verbose Logging",
            value = config.verboseLogging,
            onValueChange = { newValue ->
                onConfigChange(config.copy(verboseLogging = newValue))
            }
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = "${value.toInt()}$unit",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2196F3),
                activeTrackColor = Color(0xFF2196F3),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF2196F3),
                checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun FAQSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Frequently Asked Questions",
            color = Color(0xFFFFEB3B),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        FAQItem(
            question = "What is Demo Mode?",
            answer = "Demo Mode makes AI behavior predictable and observable for presentations. Evolution is frozen to prevent unexpected changes, and cognition cycles are slowed for visibility."
        )

        FAQItem(
            question = "Will Demo Mode break anything?",
            answer = "No! Demo Mode is completely safe and reversible. All core AI functionality remains intact—it just operates slower and won't evolve."
        )

        FAQItem(
            question = "Can I switch presets mid-demo?",
            answer = "Yes! Open Settings → Demo Mode and select a different preset anytime. Changes take effect immediately."
        )

        FAQItem(
            question = "What preset should I use?",
            answer = "Quick Demo for 3-min demos, Screen Recording for videos, Live Presentation for talks, Dev Demo for showing real behavior."
        )

        FAQItem(
            question = "Does disabling Demo Mode reset state?",
            answer = "No, all AI state is preserved. You can enable and disable Demo Mode as many times as needed."
        )
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Column(
        modifier = Modifier
            .background(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = question,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = answer,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}
