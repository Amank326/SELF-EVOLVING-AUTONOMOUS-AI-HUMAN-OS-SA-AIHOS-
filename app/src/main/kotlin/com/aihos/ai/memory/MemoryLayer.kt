package com.aihos.ai.memory

import kotlinx.serialization.Serializable

/**
 * Core abstraction for Memory Layer
 * Handles storing, retrieving, and managing AI memory
 * Supports semantic memory, episodic memory, and procedural memory
 */
interface MemoryLayer {
    
    /**
     * Store a memory item in the system
     * @param memory The memory to store
     */
    suspend fun storeMemory(memory: MemoryItem): String
    
    /**
     * Retrieve memory by semantic similarity
     * @param query The query string
     * @param limit Maximum number of results
     * @return List of relevant memories
     */
    suspend fun retrieveBySemantics(query: String, limit: Int = 10): List<MemoryItem>
    
    /**
     * Retrieve memory by ID
     * @param id The memory ID
     * @return The memory item if found
     */
    suspend fun retrieveById(id: String): MemoryItem?
    
    /**
     * Update existing memory
     * @param memory The updated memory item
     */
    suspend fun updateMemory(memory: MemoryItem): Boolean
    
    /**
     * Delete memory by ID
     * @param id The memory ID
     */
    suspend fun deleteMemory(id: String): Boolean
    
    /**
     * Clear all memories (dangerous operation)
     */
    suspend fun clearAll()
    
    /**
     * Get memory statistics
     */
    suspend fun getMemoryStats(): MemoryStats
}

/**
 * Represents a single memory item in the system
 */
@Serializable
data class MemoryItem(
    val id: String = "",
    val type: MemoryType = MemoryType.EPISODIC,
    val content: String = "",
    val semanticVector: List<Float> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val importance: Float = 0.5f,
    val accessCount: Int = 0
)

/**
 * Types of memory supported by the system
 */
enum class MemoryType {
    EPISODIC,      // Events and experiences
    SEMANTIC,      // Facts and knowledge
    PROCEDURAL,    // Skills and methods
    EMOTIONAL,     // Emotions and sentiments
    CONTEXTUAL     // Context and situation awareness
}

/**
 * Statistics about the memory system
 */
@Serializable
data class MemoryStats(
    val totalMemories: Int = 0,
    val totalSize: Long = 0,
    val averageAccessFrequency: Float = 0f,
    val oldestMemoryTime: Long = 0,
    val newestMemoryTime: Long = 0,
    val memoryTypeDistribution: Map<String, Int> = emptyMap()
)
