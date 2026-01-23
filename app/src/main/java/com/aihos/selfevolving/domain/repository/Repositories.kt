package com.aihos.selfevolving.domain.repository

import com.aihos.selfevolving.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interfaces for Clean Architecture
 */

interface MemoryRepository {
    suspend fun storeMemory(memory: Memory): Result<Unit>
    suspend fun retrieveMemory(id: String): Result<Memory>
    suspend fun retrieveMemoriesByType(type: MemoryType): Flow<List<Memory>>
    suspend fun retrieveRelevantMemories(query: String, limit: Int): Flow<List<Memory>>
    suspend fun updateMemoryImportance(id: String, importance: Float): Result<Unit>
    suspend fun consolidateMemories(): Result<Unit>
    suspend fun pruneOldMemories(): Result<Unit>
}

interface ReasoningRepository {
    suspend fun createReasoningContext(query: String): Result<ReasoningContext>
    suspend fun addInferenceStep(contextId: String, step: InferenceStep): Result<Unit>
    suspend fun concludeReasoning(contextId: String, conclusion: String, confidence: Float): Result<Unit>
    suspend fun getReasoningHistory(limit: Int): Flow<List<ReasoningContext>>
}

interface ReflectionRepository {
    suspend fun recordReflection(reflection: ReflectionEntry): Result<Unit>
    suspend fun getRecentReflections(limit: Int): Flow<List<ReflectionEntry>>
    suspend fun analyzePerformance(timeRange: LongRange): Result<PerformanceMetrics>
    suspend fun identifyPatterns(): Result<List<String>>
}

interface EvolutionRepository {
    suspend fun getCurrentEvolutionState(): Result<EvolutionState>
    suspend fun proposeAdaptation(adaptation: Adaptation): Result<Unit>
    suspend fun activateAdaptation(adaptationId: String): Result<Unit>
    suspend fun updateCapability(capability: Capability): Result<Unit>
    suspend fun getEvolutionHistory(): Flow<List<EvolutionState>>
}

interface AutonomyRepository {
    suspend fun scheduleTask(task: AutonomousTask): Result<Unit>
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit>
    suspend fun completeTask(taskId: String, outcome: TaskOutcome): Result<Unit>
    suspend fun getPendingTasks(): Flow<List<AutonomousTask>>
    suspend fun getTaskHistory(): Flow<List<AutonomousTask>>
}

interface AiStateRepository {
    suspend fun getAiState(): Flow<AiState>
    suspend fun updateAiState(state: AiState): Result<Unit>
    suspend fun toggleAiMode(mode: AiMode): Result<Unit>
}
