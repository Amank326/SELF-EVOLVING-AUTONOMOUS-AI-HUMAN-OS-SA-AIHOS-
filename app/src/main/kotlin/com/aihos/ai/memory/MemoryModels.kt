package com.aihos.ai.memory

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Advanced Memory System for SA-AIHOS
 * Implements multi-tier memory architecture:
 * - Semantic Memory: Knowledge, concepts, facts
 * - Episodic Memory: Events, experiences, sequences
 * - Behavioral Memory: Learned patterns, strategies, responses
 * - Working Memory: Current context, active processing
 * - Consolidation: Transfer long-term knowledge
 */

data class SemanticMemory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val concept: String,
    val definition: String,
    val relatedConcepts: List<String> = emptyList(),
    val confidence: Float = 0.5f,
    val usage: Int = 0,
    val lastAccessed: Long = Instant.now().toEpochMilli(),
    val strength: Float = 0.5f
)

data class EpisodicMemory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = Instant.now().toEpochMilli(),
    val description: String,
    val context: Map<String, Any> = emptyMap(),
    val emotionalValence: Float = 0f, // -1.0 (negative) to +1.0 (positive)
    val importance: Float = 0.5f,
    val relatedMemories: List<String> = emptyList(),
    val consolidationScore: Float = 0f
)

data class BehavioralMemory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pattern: String,
    val trigger: String,
    val response: String,
    val successRate: Float = 0.5f,
    val executionCount: Int = 0,
    val averageReward: Float = 0f,
    val lastExecuted: Long = Instant.now().toEpochMilli(),
    val adaptation: Float = 0.5f
)

data class WorkingMemory(
    val activeContext: String = "",
    val currentFocus: String = "",
    val shortTermBuffer: List<String> = emptyList(),
    val activeThoughts: List<String> = emptyList(),
    val attention: Float = 0.5f,
    val capacity: Float = 1.0f
)

data class ConsolidatedMemory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceEpisodes: List<String> = emptyList(),
    val generatedConcept: String = "",
    val strength: Float = 0.5f,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val accessFrequency: Int = 0
)

/**
 * Central Memory Manager - Orchestrates all memory types
 */
class MemoryManager {
    private val _semanticMemories = MutableStateFlow<List<SemanticMemory>>(emptyList())
    val semanticMemories: StateFlow<List<SemanticMemory>> = _semanticMemories
    
    private val _episodicMemories = MutableStateFlow<List<EpisodicMemory>>(emptyList())
    val episodicMemories: StateFlow<List<EpisodicMemory>> = _episodicMemories
    
    private val _behavioralMemories = MutableStateFlow<List<BehavioralMemory>>(emptyList())
    val behavioralMemories: StateFlow<List<BehavioralMemory>> = _behavioralMemories
    
    private val _workingMemory = MutableStateFlow(WorkingMemory())
    val workingMemory: StateFlow<WorkingMemory> = _workingMemory
    
    private val _consolidatedMemories = MutableStateFlow<List<ConsolidatedMemory>>(emptyList())
    val consolidatedMemories: StateFlow<List<ConsolidatedMemory>> = _consolidatedMemories
    
    // Memory metrics
    private val _memoryLoad = MutableStateFlow(0f)
    val memoryLoad: StateFlow<Float> = _memoryLoad
    
    private val _consolidationProgress = MutableStateFlow(0f)
    val consolidationProgress: StateFlow<Float> = _consolidationProgress

    /**
     * Store new semantic knowledge
     */
    suspend fun storeSemanticKnowledge(concept: String, definition: String, relatedConcepts: List<String> = emptyList()) {
        val memory = SemanticMemory(
            concept = concept,
            definition = definition,
            relatedConcepts = relatedConcepts
        )
        val current = _semanticMemories.value.toMutableList()
        current.add(memory)
        _semanticMemories.emit(current)
        updateMemoryLoad()
    }

    /**
     * Record episodic experience
     */
    suspend fun recordExperience(description: String, context: Map<String, Any>, importance: Float = 0.5f) {
        val memory = EpisodicMemory(
            description = description,
            context = context,
            importance = importance
        )
        val current = _episodicMemories.value.toMutableList()
        current.add(memory)
        _episodicMemories.emit(current)
        updateMemoryLoad()
    }

    /**
     * Learn behavioral pattern
     */
    suspend fun learnBehavior(pattern: String, trigger: String, response: String) {
        val memory = BehavioralMemory(
            pattern = pattern,
            trigger = trigger,
            response = response
        )
        val current = _behavioralMemories.value.toMutableList()
        current.add(memory)
        _behavioralMemories.emit(current)
    }

    /**
     * Update working memory context
     */
    suspend fun updateWorkingContext(context: String, focus: String) {
        val current = _workingMemory.value
        val updated = current.copy(
            activeContext = context,
            currentFocus = focus
        )
        _workingMemory.emit(updated)
    }

    /**
     * Consolidate episodic memories into semantic knowledge
     */
    suspend fun consolidateMemories() {
        val episodes = _episodicMemories.value
        val consolidated = mutableListOf<ConsolidatedMemory>()
        
        // Group similar episodes
        val groupedByContext = episodes.groupBy { it.context["type"] }
        
        for ((_, memories) in groupedByContext) {
            if (memories.size >= 3) { // Threshold for consolidation
                val concept = "Pattern_${memories.hashCode()}"
                val consolidated_mem = ConsolidatedMemory(
                    sourceEpisodes = memories.map { it.id },
                    generatedConcept = concept,
                    strength = memories.map { it.importance }.average().toFloat()
                )
                consolidated.add(consolidated_mem)
            }
        }
        
        val current = _consolidatedMemories.value.toMutableList()
        current.addAll(consolidated)
        _consolidatedMemories.emit(current)
        _consolidationProgress.emit((consolidated.size / maxOf(episodes.size, 1).toFloat()))
    }

    /**
     * Retrieve relevant semantic memory
     */
    suspend fun getSemanticKnowledge(query: String): List<SemanticMemory> {
        return _semanticMemories.value.filter { it.concept.contains(query, ignoreCase = true) }
    }

    /**
     * Retrieve episodic memories by context
     */
    suspend fun getExperiencesByContext(contextKey: String): List<EpisodicMemory> {
        return _episodicMemories.value.filter { it.context.containsKey(contextKey) }
    }

    /**
     * Retrieve behavioral patterns matching trigger
     */
    suspend fun getBehaviorsByTrigger(trigger: String): List<BehavioralMemory> {
        return _behavioralMemories.value.filter { it.trigger.contains(trigger, ignoreCase = true) }
    }

    /**
     * Reinforce successful behavior
     */
    suspend fun reinforceBehavior(behaviorId: String, reward: Float) {
        val current = _behavioralMemories.value.toMutableList()
        val index = current.indexOfFirst { it.id == behaviorId }
        if (index >= 0) {
            val behavior = current[index]
            val updated = behavior.copy(
                successRate = (behavior.successRate + reward) / 2,
                executionCount = behavior.executionCount + 1,
                averageReward = (behavior.averageReward * behavior.executionCount + reward) / (behavior.executionCount + 1)
            )
            current[index] = updated
            _behavioralMemories.emit(current)
        }
    }

    /**
     * Update memory load metric
     */
    private suspend fun updateMemoryLoad() {
        val total = _semanticMemories.value.size + _episodicMemories.value.size + _behavioralMemories.value.size
        val load = minOf((total / 1000f), 1.0f) // Cap at 1.0
        _memoryLoad.emit(load)
    }

    /**
     * Clear memory (careful operation)
     */
    suspend fun clearMemory() {
        _semanticMemories.emit(emptyList())
        _episodicMemories.emit(emptyList())
        _behavioralMemories.emit(emptyList())
        _consolidatedMemories.emit(emptyList())
        _memoryLoad.emit(0f)
        _consolidationProgress.emit(0f)
    }

    /**
     * Get memory statistics
     */
    fun getMemoryStats(): Map<String, Any> {
        return mapOf(
            "semanticMemories" to _semanticMemories.value.size,
            "episodicMemories" to _episodicMemories.value.size,
            "behavioralMemories" to _behavioralMemories.value.size,
            "consolidatedMemories" to _consolidatedMemories.value.size,
            "memoryLoad" to _memoryLoad.value,
            "consolidationProgress" to _consolidationProgress.value
        )
    }
}
