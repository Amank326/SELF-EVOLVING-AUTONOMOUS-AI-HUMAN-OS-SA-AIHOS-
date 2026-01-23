package com.aihos.ai.memory.impl

import com.aihos.ai.memory.*
import timber.log.Timber

/**
 * Default in-memory implementation of MemoryLayer for initial development
 * Can be replaced with database-backed implementation
 */
class DefaultMemoryLayer : MemoryLayer {
    
    private val memories = mutableMapOf<String, MemoryItem>()
    private val accessLog = mutableMapOf<String, Int>()
    
    override suspend fun storeMemory(memory: MemoryItem): String {
        val id = memory.id.ifEmpty { generateId() }
        val item = memory.copy(id = id, createdAt = System.currentTimeMillis())
        memories[id] = item
        Timber.d("Memory stored: $id (${item.type})")
        return id
    }
    
    override suspend fun retrieveBySemantics(query: String, limit: Int): List<MemoryItem> {
        // Simple retrieval - in production, implement semantic similarity
        return memories.values
            .filter { it.content.contains(query, ignoreCase = true) }
            .sortedByDescending { it.importance }
            .take(limit)
    }
    
    override suspend fun retrieveById(id: String): MemoryItem? {
        val item = memories[id]
        if (item != null) {
            accessLog[id] = (accessLog[id] ?: 0) + 1
        }
        return item
    }
    
    override suspend fun updateMemory(memory: MemoryItem): Boolean {
        return if (memories.containsKey(memory.id)) {
            memories[memory.id] = memory.copy(updatedAt = System.currentTimeMillis())
            true
        } else {
            false
        }
    }
    
    override suspend fun deleteMemory(id: String): Boolean {
        memories.remove(id)
        accessLog.remove(id)
        return true
    }
    
    override suspend fun clearAll() {
        memories.clear()
        accessLog.clear()
        Timber.w("All memories cleared")
    }
    
    override suspend fun getMemoryStats(): MemoryStats {
        val typeDistribution = memories.values
            .groupingBy { it.type.name }
            .eachCount()
        
        return MemoryStats(
            totalMemories = memories.size,
            totalSize = memories.values.sumOf { it.content.length.toLong() },
            averageAccessFrequency = if (accessLog.isEmpty()) 0f 
                                    else accessLog.values.average().toFloat(),
            oldestMemoryTime = memories.values.minOfOrNull { it.createdAt } ?: 0L,
            newestMemoryTime = memories.values.maxOfOrNull { it.createdAt } ?: 0L,
            memoryTypeDistribution = typeDistribution
        )
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}
