package com.aihos.selfevolving.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihos.selfevolving.domain.model.*
import com.aihos.selfevolving.domain.repository.AiStateRepository
import com.aihos.selfevolving.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main AI OS home screen
 */
@HiltViewModel
class AiHosViewModel @Inject constructor(
    private val aiStateRepository: AiStateRepository,
    private val storeMemoryUseCase: StoreMemoryUseCase,
    private val retrieveRelevantMemoriesUseCase: RetrieveRelevantMemoriesUseCase,
    private val performReasoningUseCase: PerformReasoningUseCase,
    private val performReflectionUseCase: PerformReflectionUseCase,
    private val evolveCapabilityUseCase: EvolveCapabilityUseCase,
    private val scheduleAutonomousTaskUseCase: ScheduleAutonomousTaskUseCase,
    private val executeAutonomousTasksUseCase: ExecuteAutonomousTasksUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AiHosUiState())
    val uiState: StateFlow<AiHosUiState> = _uiState.asStateFlow()
    
    init {
        observeAiState()
        initializeAi()
    }
    
    private fun observeAiState() {
        viewModelScope.launch {
            aiStateRepository.getAiState().collect { state ->
                _uiState.update { it.copy(aiState = state) }
            }
        }
    }
    
    private fun initializeAi() {
        viewModelScope.launch {
            // Initialize the AI with default state
            val initialState = AiState(
                isActive = true,
                currentMode = AiMode.IDLE,
                memoryUtilization = 0.1f,
                processingLoad = 0.0f,
                evolutionStage = 1,
                autonomyLevel = 0.3f
            )
            aiStateRepository.updateAiState(initialState)
            
            // Create a welcome memory
            storeMemory("System initialized - Self-Evolving AI Human OS active")
        }
    }
    
    fun storeMemory(content: String) {
        viewModelScope.launch {
            val memory = Memory(
                id = java.util.UUID.randomUUID().toString(),
                content = content,
                timestamp = System.currentTimeMillis(),
                type = MemoryType.SHORT_TERM,
                importance = 0.5f
            )
            
            storeMemoryUseCase(memory)
            addLog("Memory stored: ${content.take(50)}...")
        }
    }
    
    fun performReasoning(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            addLog("Starting reasoning: $query")
            
            aiStateRepository.toggleAiMode(AiMode.REASONING)
            
            performReasoningUseCase(query)
                .onSuccess { context ->
                    addLog("Reasoning completed: ${context.conclusion}")
                    addLog("Confidence: ${(context.confidence * 100).toInt()}%")
                    
                    // Evolve reasoning capability
                    evolveCapabilityUseCase("Reasoning", "Completed reasoning task")
                }
                .onFailure { error ->
                    addLog("Reasoning failed: ${error.message}")
                }
            
            aiStateRepository.toggleAiMode(AiMode.IDLE)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
    
    fun performReflection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            addLog("Performing self-reflection...")
            
            aiStateRepository.toggleAiMode(AiMode.REFLECTING)
            
            performReflectionUseCase()
                .onSuccess { reflection ->
                    addLog("Reflection completed")
                    reflection.insights.forEach { insight ->
                        addLog("Insight: $insight")
                    }
                    reflection.improvements.forEach { improvement ->
                        addLog("Improvement: $improvement")
                    }
                    
                    // Evolve reflection capability
                    evolveCapabilityUseCase("Reflection", "Completed reflection")
                }
                .onFailure { error ->
                    addLog("Reflection failed: ${error.message}")
                }
            
            aiStateRepository.toggleAiMode(AiMode.IDLE)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
    
    fun triggerEvolution() {
        viewModelScope.launch {
            addLog("Triggering evolution cycle...")
            aiStateRepository.toggleAiMode(AiMode.EVOLVING)
            
            // Evolve multiple capabilities
            evolveCapabilityUseCase("Memory Management", "Manual evolution trigger")
            evolveCapabilityUseCase("Reasoning", "Manual evolution trigger")
            evolveCapabilityUseCase("Reflection", "Manual evolution trigger")
            
            addLog("Evolution cycle completed")
            aiStateRepository.toggleAiMode(AiMode.IDLE)
        }
    }
    
    fun scheduleTask(taskName: String, description: String) {
        viewModelScope.launch {
            scheduleAutonomousTaskUseCase(
                name = taskName,
                description = description,
                priority = Priority.MEDIUM
            )
            addLog("Task scheduled: $taskName")
        }
    }
    
    fun executeAutonomousTasks() {
        viewModelScope.launch {
            aiStateRepository.toggleAiMode(AiMode.AUTONOMOUS)
            addLog("Executing autonomous tasks...")
            
            executeAutonomousTasksUseCase().collect { tasks ->
                tasks.forEach { task ->
                    addLog("Task: ${task.name} - ${task.status}")
                }
            }
            
            aiStateRepository.toggleAiMode(AiMode.IDLE)
        }
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        
        _uiState.update { currentState ->
            currentState.copy(
                logs = (currentState.logs + logEntry).takeLast(50)
            )
        }
    }
    
    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}

data class AiHosUiState(
    val aiState: AiState = AiState(
        isActive = false,
        currentMode = AiMode.IDLE,
        memoryUtilization = 0f,
        processingLoad = 0f,
        evolutionStage = 1,
        autonomyLevel = 0f
    ),
    val logs: List<String> = emptyList(),
    val isProcessing: Boolean = false
)
