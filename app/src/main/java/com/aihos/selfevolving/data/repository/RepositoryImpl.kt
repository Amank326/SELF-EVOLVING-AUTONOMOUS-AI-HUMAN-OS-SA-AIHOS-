package com.aihos.selfevolving.data.repository

import com.aihos.selfevolving.data.local.*
import com.aihos.selfevolving.domain.model.*
import com.aihos.selfevolving.domain.repository.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementations with offline-first approach using Room Database
 */

class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao,
    private val gson: Gson
) : MemoryRepository {
    
    override suspend fun storeMemory(memory: Memory): Result<Unit> = runCatching {
        val entity = memory.toEntity(gson)
        memoryDao.insert(entity)
    }
    
    override suspend fun retrieveMemory(id: String): Result<Memory> = runCatching {
        val entity = memoryDao.getById(id) ?: throw NoSuchElementException("Memory not found")
        memoryDao.incrementRetrievalCount(id, System.currentTimeMillis())
        entity.toDomain(gson)
    }
    
    override suspend fun retrieveMemoriesByType(type: MemoryType): Flow<List<Memory>> {
        return memoryDao.getByType(type.name).map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
    
    override suspend fun retrieveRelevantMemories(query: String, limit: Int): Flow<List<Memory>> {
        return memoryDao.searchMemories(query, limit).map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
    
    override suspend fun updateMemoryImportance(id: String, importance: Float): Result<Unit> = runCatching {
        memoryDao.updateImportance(id, importance)
    }
    
    override suspend fun consolidateMemories(): Result<Unit> = runCatching {
        // Logic to consolidate short-term to long-term memories
        // This is a simplified implementation
    }
    
    override suspend fun pruneOldMemories(): Result<Unit> = runCatching {
        val threshold = 0.1f
        val oldTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days
        memoryDao.pruneOldMemories(threshold, oldTimestamp)
    }
}

class ReasoningRepositoryImpl @Inject constructor(
    private val reasoningDao: ReasoningDao,
    private val gson: Gson
) : ReasoningRepository {
    
    override suspend fun createReasoningContext(query: String): Result<ReasoningContext> = runCatching {
        val context = ReasoningContext(
            id = java.util.UUID.randomUUID().toString(),
            query = query,
            relevantMemories = emptyList(),
            inferenceSteps = emptyList(),
            conclusion = null,
            confidence = 0f,
            timestamp = System.currentTimeMillis()
        )
        val entity = context.toEntity(gson)
        reasoningDao.insert(entity)
        context
    }
    
    override suspend fun addInferenceStep(contextId: String, step: InferenceStep): Result<Unit> = runCatching {
        val entity = reasoningDao.getById(contextId) ?: throw NoSuchElementException("Context not found")
        val context = entity.toDomain(gson)
        val updatedSteps = context.inferenceSteps + step
        val stepsJson = gson.toJson(updatedSteps)
        reasoningDao.updateInferenceSteps(contextId, stepsJson)
    }
    
    override suspend fun concludeReasoning(contextId: String, conclusion: String, confidence: Float): Result<Unit> = runCatching {
        reasoningDao.updateConclusion(contextId, conclusion, confidence)
    }
    
    override suspend fun getReasoningHistory(limit: Int): Flow<List<ReasoningContext>> {
        return reasoningDao.getRecent(limit).map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
}

class ReflectionRepositoryImpl @Inject constructor(
    private val reflectionDao: ReflectionDao,
    private val gson: Gson
) : ReflectionRepository {
    
    override suspend fun recordReflection(reflection: ReflectionEntry): Result<Unit> = runCatching {
        val entity = reflection.toEntity(gson)
        reflectionDao.insert(entity)
    }
    
    override suspend fun getRecentReflections(limit: Int): Flow<List<ReflectionEntry>> {
        return reflectionDao.getRecent(limit).map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
    
    override suspend fun analyzePerformance(timeRange: LongRange): Result<PerformanceMetrics> = runCatching {
        val reflections = reflectionDao.getInTimeRange(timeRange.first, timeRange.last)
        
        if (reflections.isEmpty()) {
            return@runCatching PerformanceMetrics(
                taskSuccessRate = 0.5f,
                responseTime = 500,
                memoryEfficiency = 0.7f,
                reasoningAccuracy = 0.6f,
                userSatisfaction = 0.5f
            )
        }
        
        // Aggregate metrics from reflections
        val metrics = reflections.map { it.toDomain(gson).performanceMetrics }
        PerformanceMetrics(
            taskSuccessRate = metrics.map { it.taskSuccessRate }.average().toFloat(),
            responseTime = metrics.map { it.responseTime }.average().toLong(),
            memoryEfficiency = metrics.map { it.memoryEfficiency }.average().toFloat(),
            reasoningAccuracy = metrics.map { it.reasoningAccuracy }.average().toFloat(),
            userSatisfaction = metrics.map { it.userSatisfaction }.average().toFloat()
        )
    }
    
    override suspend fun identifyPatterns(): Result<List<String>> = runCatching {
        // Simplified pattern identification
        listOf("Regular task execution", "Learning from experiences", "Adaptive behavior")
    }
}

class EvolutionRepositoryImpl @Inject constructor(
    private val evolutionDao: EvolutionDao,
    private val gson: Gson
) : EvolutionRepository {
    
    override suspend fun getCurrentEvolutionState(): Result<EvolutionState> = runCatching {
        val entity = evolutionDao.getCurrent()
        if (entity != null) {
            entity.toDomain(gson)
        } else {
            // Create initial state
            val initialState = EvolutionState(
                id = java.util.UUID.randomUUID().toString(),
                version = 1,
                timestamp = System.currentTimeMillis(),
                learningProgress = 0f,
                adaptations = emptyList(),
                capabilities = listOf(
                    Capability("Memory Management", 0.3f, emptyList()),
                    Capability("Reasoning", 0.2f, emptyList()),
                    Capability("Reflection", 0.1f, emptyList())
                )
            )
            evolutionDao.insert(initialState.toEntity(gson))
            initialState
        }
    }
    
    override suspend fun proposeAdaptation(adaptation: Adaptation): Result<Unit> = runCatching {
        val currentState = getCurrentEvolutionState().getOrThrow()
        val updatedState = currentState.copy(
            adaptations = currentState.adaptations + adaptation
        )
        evolutionDao.insert(updatedState.toEntity(gson))
    }
    
    override suspend fun activateAdaptation(adaptationId: String): Result<Unit> = runCatching {
        val currentState = getCurrentEvolutionState().getOrThrow()
        val updatedAdaptations = currentState.adaptations.map { adaptation ->
            if (adaptation.id == adaptationId) {
                adaptation.copy(status = AdaptationStatus.ACTIVE)
            } else {
                adaptation
            }
        }
        val updatedState = currentState.copy(adaptations = updatedAdaptations)
        evolutionDao.insert(updatedState.toEntity(gson))
    }
    
    override suspend fun updateCapability(capability: Capability): Result<Unit> = runCatching {
        val currentState = getCurrentEvolutionState().getOrThrow()
        val updatedCapabilities = currentState.capabilities.map { cap ->
            if (cap.name == capability.name) capability else cap
        }
        val finalCapabilities = if (capability.name !in currentState.capabilities.map { it.name }) {
            updatedCapabilities + capability
        } else {
            updatedCapabilities
        }
        
        val updatedState = currentState.copy(
            capabilities = finalCapabilities,
            version = currentState.version + 1,
            timestamp = System.currentTimeMillis()
        )
        evolutionDao.insert(updatedState.toEntity(gson))
    }
    
    override suspend fun getEvolutionHistory(): Flow<List<EvolutionState>> {
        return evolutionDao.getHistory().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
}

class AutonomyRepositoryImpl @Inject constructor(
    private val autonomyDao: AutonomyDao,
    private val gson: Gson
) : AutonomyRepository {
    
    override suspend fun scheduleTask(task: AutonomousTask): Result<Unit> = runCatching {
        val entity = task.toEntity(gson)
        autonomyDao.insert(entity)
    }
    
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> = runCatching {
        autonomyDao.updateStatus(taskId, status.name)
    }
    
    override suspend fun completeTask(taskId: String, outcome: TaskOutcome): Result<Unit> = runCatching {
        val outcomeJson = gson.toJson(outcome)
        autonomyDao.completeTask(
            taskId,
            TaskStatus.COMPLETED.name,
            System.currentTimeMillis(),
            outcomeJson
        )
    }
    
    override suspend fun getPendingTasks(): Flow<List<AutonomousTask>> {
        return autonomyDao.getPendingTasks().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
    
    override suspend fun getTaskHistory(): Flow<List<AutonomousTask>> {
        return autonomyDao.getAllTasks().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }
}

class AiStateRepositoryImpl @Inject constructor(
    private val aiStateDao: AiStateDao,
    private val gson: Gson
) : AiStateRepository {
    
    override suspend fun getAiState(): Flow<AiState> {
        return aiStateDao.getState().map { entity ->
            entity?.toDomain() ?: AiState(
                isActive = false,
                currentMode = AiMode.IDLE,
                memoryUtilization = 0f,
                processingLoad = 0f,
                evolutionStage = 1,
                autonomyLevel = 0.1f
            )
        }
    }
    
    override suspend fun updateAiState(state: AiState): Result<Unit> = runCatching {
        val entity = state.toEntity()
        aiStateDao.insert(entity)
    }
    
    override suspend fun toggleAiMode(mode: AiMode): Result<Unit> = runCatching {
        aiStateDao.updateMode(mode.name, System.currentTimeMillis())
    }
}

/**
 * Extension functions for entity-domain mapping
 */

private fun Memory.toEntity(gson: Gson): MemoryEntity {
    return MemoryEntity(
        id = id,
        content = content,
        timestamp = timestamp,
        type = type.name,
        importance = importance,
        associatedMemories = gson.toJson(associatedMemories),
        retrievalCount = retrievalCount,
        lastAccessed = lastAccessed
    )
}

private fun MemoryEntity.toDomain(gson: Gson): Memory {
    val associatedMemoriesList: List<String> = gson.fromJson(
        associatedMemories,
        object : TypeToken<List<String>>() {}.type
    ) ?: emptyList()
    
    return Memory(
        id = id,
        content = content,
        timestamp = timestamp,
        type = MemoryType.valueOf(type),
        importance = importance,
        associatedMemories = associatedMemoriesList,
        retrievalCount = retrievalCount,
        lastAccessed = lastAccessed
    )
}

private fun ReasoningContext.toEntity(gson: Gson): ReasoningContextEntity {
    return ReasoningContextEntity(
        id = id,
        query = query,
        inferenceSteps = gson.toJson(inferenceSteps),
        conclusion = conclusion,
        confidence = confidence,
        timestamp = timestamp
    )
}

private fun ReasoningContextEntity.toDomain(gson: Gson): ReasoningContext {
    val steps: List<InferenceStep> = gson.fromJson(
        inferenceSteps,
        object : TypeToken<List<InferenceStep>>() {}.type
    ) ?: emptyList()
    
    return ReasoningContext(
        id = id,
        query = query,
        relevantMemories = emptyList(),
        inferenceSteps = steps,
        conclusion = conclusion,
        confidence = confidence,
        timestamp = timestamp
    )
}

private fun ReflectionEntry.toEntity(gson: Gson): ReflectionEntity {
    return ReflectionEntity(
        id = id,
        timestamp = timestamp,
        performanceMetrics = gson.toJson(performanceMetrics),
        insights = gson.toJson(insights),
        improvements = gson.toJson(improvements),
        behaviorAnalysis = gson.toJson(behaviorAnalysis)
    )
}

private fun ReflectionEntity.toDomain(gson: Gson): ReflectionEntry {
    return ReflectionEntry(
        id = id,
        timestamp = timestamp,
        performanceMetrics = gson.fromJson(performanceMetrics, PerformanceMetrics::class.java),
        insights = gson.fromJson(insights, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        improvements = gson.fromJson(improvements, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        behaviorAnalysis = gson.fromJson(behaviorAnalysis, BehaviorAnalysis::class.java)
    )
}

private fun EvolutionState.toEntity(gson: Gson): EvolutionStateEntity {
    return EvolutionStateEntity(
        id = id,
        version = version,
        timestamp = timestamp,
        learningProgress = learningProgress,
        adaptations = gson.toJson(adaptations),
        capabilities = gson.toJson(capabilities)
    )
}

private fun EvolutionStateEntity.toDomain(gson: Gson): EvolutionState {
    return EvolutionState(
        id = id,
        version = version,
        timestamp = timestamp,
        learningProgress = learningProgress,
        adaptations = gson.fromJson(adaptations, object : TypeToken<List<Adaptation>>() {}.type) ?: emptyList(),
        capabilities = gson.fromJson(capabilities, object : TypeToken<List<Capability>>() {}.type) ?: emptyList()
    )
}

private fun AutonomousTask.toEntity(gson: Gson): AutonomousTaskEntity {
    return AutonomousTaskEntity(
        id = id,
        name = name,
        description = description,
        priority = priority.name,
        status = status.name,
        createdAt = createdAt,
        scheduledAt = scheduledAt,
        completedAt = completedAt,
        dependencies = gson.toJson(dependencies),
        outcome = outcome?.let { gson.toJson(it) }
    )
}

private fun AutonomousTaskEntity.toDomain(gson: Gson): AutonomousTask {
    return AutonomousTask(
        id = id,
        name = name,
        description = description,
        priority = Priority.valueOf(priority),
        status = TaskStatus.valueOf(status),
        createdAt = createdAt,
        scheduledAt = scheduledAt,
        completedAt = completedAt,
        dependencies = gson.fromJson(dependencies, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        outcome = outcome?.let { gson.fromJson(it, TaskOutcome::class.java) }
    )
}

private fun AiState.toEntity(): AiStateEntity {
    return AiStateEntity(
        id = "current_state",
        isActive = isActive,
        currentMode = currentMode.name,
        memoryUtilization = memoryUtilization,
        processingLoad = processingLoad,
        evolutionStage = evolutionStage,
        autonomyLevel = autonomyLevel,
        lastUpdated = System.currentTimeMillis()
    )
}

private fun AiStateEntity.toDomain(): AiState {
    return AiState(
        isActive = isActive,
        currentMode = AiMode.valueOf(currentMode),
        memoryUtilization = memoryUtilization,
        processingLoad = processingLoad,
        evolutionStage = evolutionStage,
        autonomyLevel = autonomyLevel
    )
}
