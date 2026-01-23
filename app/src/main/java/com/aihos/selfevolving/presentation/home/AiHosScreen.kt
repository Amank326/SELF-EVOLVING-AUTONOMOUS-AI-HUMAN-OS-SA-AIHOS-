package com.aihos.selfevolving.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aihos.selfevolving.domain.model.AiMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHosScreen(
    viewModel: AiHosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInputDialog by remember { mutableStateOf(false) }
    var inputType by remember { mutableStateOf(InputType.MEMORY) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Self-Evolving AI Human OS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AI Status Card
            AiStatusCard(
                aiState = uiState.aiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Control Buttons
            ControlButtons(
                isProcessing = uiState.isProcessing,
                onMemoryClick = {
                    inputType = InputType.MEMORY
                    showInputDialog = true
                },
                onReasoningClick = {
                    inputType = InputType.REASONING
                    showInputDialog = true
                },
                onReflectionClick = { viewModel.performReflection() },
                onEvolutionClick = { viewModel.triggerEvolution() },
                onTaskClick = {
                    inputType = InputType.TASK
                    showInputDialog = true
                },
                onExecuteTasksClick = { viewModel.executeAutonomousTasks() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            // Logs Section
            LogsSection(
                logs = uiState.logs,
                onClearLogs = { viewModel.clearLogs() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )
        }
    }
    
    // Input Dialog
    if (showInputDialog) {
        InputDialog(
            inputType = inputType,
            onDismiss = { showInputDialog = false },
            onConfirm = { input ->
                when (inputType) {
                    InputType.MEMORY -> viewModel.storeMemory(input)
                    InputType.REASONING -> viewModel.performReasoning(input)
                    InputType.TASK -> viewModel.scheduleTask("Task", input)
                }
                showInputDialog = false
            }
        )
    }
}

@Composable
fun AiStatusCard(
    aiState: com.aihos.selfevolving.domain.model.AiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI System Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem("Status", if (aiState.isActive) "Active" else "Inactive")
                StatusItem("Mode", aiState.currentMode.name)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            StatusBar("Memory", aiState.memoryUtilization)
            Spacer(modifier = Modifier.height(4.dp))
            StatusBar("Processing", aiState.processingLoad)
            Spacer(modifier = Modifier.height(4.dp))
            StatusBar("Autonomy", aiState.autonomyLevel)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem("Evolution", "Stage ${aiState.evolutionStage}")
                StatusItem("", "")
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBar(label: String, value: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                value > 0.8f -> Color.Red
                value > 0.5f -> Color.Yellow
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun ControlButtons(
    isProcessing: Boolean,
    onMemoryClick: () -> Unit,
    onReasoningClick: () -> Unit,
    onReflectionClick: () -> Unit,
    onEvolutionClick: () -> Unit,
    onTaskClick: () -> Unit,
    onExecuteTasksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "AI Controls",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                text = "Memory",
                icon = Icons.Default.Memory,
                onClick = onMemoryClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
            ControlButton(
                text = "Reason",
                icon = Icons.Default.Psychology,
                onClick = onReasoningClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                text = "Reflect",
                icon = Icons.Default.SelfImprovement,
                onClick = onReflectionClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
            ControlButton(
                text = "Evolve",
                icon = Icons.Default.AutoAwesome,
                onClick = onEvolutionClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                text = "Schedule",
                icon = Icons.Default.Schedule,
                onClick = onTaskClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
            ControlButton(
                text = "Execute",
                icon = Icons.Default.PlayArrow,
                onClick = onExecuteTasksClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text)
    }
}

@Composable
fun LogsSection(
    logs: List<String>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear logs"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputDialog(
    inputType: InputType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (inputType) {
                    InputType.MEMORY -> "Store Memory"
                    InputType.REASONING -> "Perform Reasoning"
                    InputType.TASK -> "Schedule Task"
                }
            )
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(
                        when (inputType) {
                            InputType.MEMORY -> "Memory content"
                            InputType.REASONING -> "Query"
                            InputType.TASK -> "Task description"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class InputType {
    MEMORY,
    REASONING,
    TASK
}
