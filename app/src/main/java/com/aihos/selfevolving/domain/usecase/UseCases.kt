package com.aihos.selfevolving.domain.usecase

import com.aihos.selfevolving.domain.model.*
import com.aihos.selfevolving.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use cases for Memory Layer operations
 */

class StoreMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(memory: Memory): Result<Unit> {
        return memoryRepository.storeMemory(memory)
    }
}

class RetrieveRelevantMemoriesUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 10): Flow<List<Memory>> {
        return memoryRepository.retrieveRelevantMemories(query, limit)
    }
}

class ConsolidateMemoriesUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        // Consolidate short-term memories into long-term
        return memoryRepository.consolidateMemories()
    }
}

/**
 * Use cases for Reasoning Layer operations
 */

class PerformReasoningUseCase @Inject constructor(
    private val reasoningRepository: ReasoningRepository,
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(query: String): Result<ReasoningContext> {
        // Create reasoning context
        val contextResult = reasoningRepository.createReasoningContext(query)
        
        if (contextResult.isFailure) {
            return Result.failure(contextResult.exceptionOrNull()!!)
        }
        
        val context = contextResult.getOrNull()!!
        
        // Retrieve relevant memories
        val memories = mutableListOf<Memory>()
        memoryRepository.retrieveRelevantMemories(query, 5).collect { memList ->
            memories.addAll(memList)
        }
        
        // Perform inference steps
        val steps = performInferenceSteps(query, memories)
        steps.forEachIndexed { index, step ->
            reasoningRepository.addInferenceStep(context.id, step.copy(step = index))
        }
        
        // Generate conclusion
        val conclusion = generateConclusion(steps)
        val confidence = calculateConfidence(steps, memories)
        reasoningRepository.concludeReasoning(context.id, conclusion, confidence)
        
        return Result.success(
            context.copy(
                relevantMemories = memories,
                inferenceSteps = steps,
                conclusion = conclusion,
                confidence = confidence
            )
        )
    }
    
    private fun performInferenceSteps(query: String, memories: List<Memory>): List<InferenceStep> {
        val steps = mutableListOf<InferenceStep>()
        
        // Step 1: Context Analysis
        steps.add(
            InferenceStep(
                step = 0,
                description = "Analyzing query context",
                inputData = mapOf("query" to query),
                outputData = mapOf("context" to "extracted"),
                reasoning = "Breaking down the query into components"
            )
        )
        
        // Step 2: Memory Integration
        steps.add(
            InferenceStep(
                step = 1,
                description = "Integrating relevant memories",
                inputData = mapOf("memories" to memories.size),
                outputData = mapOf("integrated" to true),
                reasoning = "Combining past experiences with current query"
            )
        )
        
        // Step 3: Pattern Recognition
        steps.add(
            InferenceStep(
                step = 2,
                description = "Recognizing patterns",
                inputData = mapOf("patterns" to "analyzing"),
                outputData = mapOf("patterns_found" to memories.isNotEmpty()),
                reasoning = "Identifying patterns from past experiences"
            )
        )
        
        return steps
    }
    
    private fun generateConclusion(steps: List<InferenceStep>): String {
        return "Based on ${steps.size} inference steps and available context, reasoning completed."
    }
    
    private fun calculateConfidence(steps: List<InferenceStep>, memories: List<Memory>): Float {
        val stepConfidence = if (steps.isNotEmpty()) 0.5f else 0.1f
        val memoryConfidence = if (memories.isNotEmpty()) 0.4f else 0.1f
        return (stepConfidence + memoryConfidence).coerceIn(0f, 1f)
    }
}

/**
 * Use cases for Reflection Layer operations
 */

class PerformReflectionUseCase @Inject constructor(
    private val reflectionRepository: ReflectionRepository
) {
    suspend operator fun invoke(): Result<ReflectionEntry> {
        // Analyze current performance
        val timeRange = (System.currentTimeMillis() - 3600000)..System.currentTimeMillis()
        val metricsResult = reflectionRepository.analyzePerformance(timeRange)
        
        if (metricsResult.isFailure) {
            return Result.failure(metricsResult.exceptionOrNull()!!)
        }
        
        val metrics = metricsResult.getOrNull()!!
        val patternsResult = reflectionRepository.identifyPatterns()
        val patterns = patternsResult.getOrNull() ?: emptyList()
        
        // Generate insights
        val insights = generateInsights(metrics, patterns)
        val improvements = suggestImprovements(metrics)
        
        val reflection = ReflectionEntry(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            performanceMetrics = metrics,
            insights = insights,
            improvements = improvements,
            behaviorAnalysis = BehaviorAnalysis(
                patterns = patterns,
                anomalies = emptyList(),
                trends = emptyList()
            )
        )
        
        reflectionRepository.recordReflection(reflection)
        return Result.success(reflection)
    }
    
    private fun generateInsights(metrics: PerformanceMetrics, patterns: List<String>): List<String> {
        val insights = mutableListOf<String>()
        
        if (metrics.taskSuccessRate > 0.8f) {
            insights.add("High task success rate indicates effective learning")
        }
        if (metrics.memoryEfficiency < 0.5f) {
            insights.add("Memory efficiency could be improved")
        }
        if (patterns.isNotEmpty()) {
            insights.add("Detected ${patterns.size} behavioral patterns")
        }
        
        return insights
    }
    
    private fun suggestImprovements(metrics: PerformanceMetrics): List<String> {
        val improvements = mutableListOf<String>()
        
        if (metrics.responseTime > 1000) {
            improvements.add("Optimize response time through better indexing")
        }
        if (metrics.reasoningAccuracy < 0.7f) {
            improvements.add("Enhance reasoning algorithms")
        }
        
        return improvements
    }
}

/**
 * Use cases for Evolution Layer operations
 */

class EvolveCapabilityUseCase @Inject constructor(
    private val evolutionRepository: EvolutionRepository
) {
    suspend operator fun invoke(capabilityName: String, trigger: String): Result<Unit> {
        val stateResult = evolutionRepository.getCurrentEvolutionState()
        
        if (stateResult.isFailure) {
            return Result.failure(stateResult.exceptionOrNull()!!)
        }
        
        val state = stateResult.getOrNull()!!
        val capability = state.capabilities.find { it.name == capabilityName }
        
        val updatedCapability = if (capability != null) {
            // Evolve existing capability
            val newLevel = (capability.level + 0.1f).coerceAtMost(1.0f)
            capability.copy(
                level = newLevel,
                evolution = capability.evolution + EvolutionPoint(
                    timestamp = System.currentTimeMillis(),
                    level = newLevel,
                    trigger = trigger
                )
            )
        } else {
            // Create new capability
            Capability(
                name = capabilityName,
                level = 0.1f,
                evolution = listOf(
                    EvolutionPoint(
                        timestamp = System.currentTimeMillis(),
                        level = 0.1f,
                        trigger = trigger
                    )
                )
            )
        }
        
        return evolutionRepository.updateCapability(updatedCapability)
    }
}

class ProposeAdaptationUseCase @Inject constructor(
    private val evolutionRepository: EvolutionRepository
) {
    suspend operator fun invoke(description: String, trigger: String, impact: Float): Result<Unit> {
        val adaptation = Adaptation(
            id = java.util.UUID.randomUUID().toString(),
            description = description,
            timestamp = System.currentTimeMillis(),
            trigger = trigger,
            impact = impact,
            status = AdaptationStatus.PROPOSED
        )
        
        return evolutionRepository.proposeAdaptation(adaptation)
    }
}

/**
 * Use cases for Autonomy Layer operations
 */

class ScheduleAutonomousTaskUseCase @Inject constructor(
    private val autonomyRepository: AutonomyRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        priority: Priority,
        scheduledAt: Long? = null
    ): Result<Unit> {
        val task = AutonomousTask(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            priority = priority,
            status = TaskStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            scheduledAt = scheduledAt
        )
        
        return autonomyRepository.scheduleTask(task)
    }
}

class ExecuteAutonomousTasksUseCase @Inject constructor(
    private val autonomyRepository: AutonomyRepository
) {
    suspend operator fun invoke(): Flow<List<AutonomousTask>> {
        return autonomyRepository.getPendingTasks()
    }
}

class CompleteAutonomousTaskUseCase @Inject constructor(
    private val autonomyRepository: AutonomyRepository
) {
    suspend operator fun invoke(
        taskId: String,
        success: Boolean,
        result: String,
        learnings: List<String>
    ): Result<Unit> {
        val outcome = TaskOutcome(
            success = success,
            result = result,
            learnings = learnings,
            timestamp = System.currentTimeMillis()
        )
        
        return autonomyRepository.completeTask(taskId, outcome)
    }
}
