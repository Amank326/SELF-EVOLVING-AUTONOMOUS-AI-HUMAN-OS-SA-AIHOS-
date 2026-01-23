package com.aihos.data.repository

import com.aihos.ai.memory.MemoryItem
import com.aihos.ai.memory.MemoryLayer
import com.aihos.ai.memory.MemoryType
import com.aihos.ai.memory.MemoryStats
import com.aihos.data.db.dao.MemoryDao
import com.aihos.data.db.entity.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Repository implementation for Memory Layer
 * Bridges the domain layer (AI) with the data layer (Room database)
 * Offline-first, all operations are local
 */
class MemoryRepository(
    private val memoryDao: MemoryDao
) : MemoryLayer {
    
    override suspend fun storeMemory(memory: MemoryItem): String = withContext(Dispatchers.IO) {
        try {
            val entity = memory.toEntity()
            memoryDao.insertMemory(entity)
            Timber.d("Memory stored: ${memory.id}")
            memory.id
        } catch (e: Exception) {
            Timber.e(e, "Error storing memory")
            throw e
        }
    }
    
    override suspend fun retrieveBySemantics(query: String, limit: Int): List<MemoryItem> = withContext(Dispatchers.IO) {
        try {
            // Get top memories by importance and access frequency
            // In production, implement proper semantic similarity search
            memoryDao.getTopMemoriesByImportance(limit)
                .map { it.toMemoryItem() }
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving memories")
            emptyList()
        }
    }
    
    override suspend fun retrieveById(id: String): MemoryItem? = withContext(Dispatchers.IO) {
        try {
            val entity = memoryDao.getMemoryById(id)
            if (entity != null) {
                memoryDao.incrementAccessCount(id)
            }
            entity?.toMemoryItem()
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving memory by ID: $id")
            null
        }
    }
    
    override suspend fun updateMemory(memory: MemoryItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = memory.copy(updatedAt = System.currentTimeMillis()).toEntity()
            memoryDao.updateMemory(entity)
            Timber.d("Memory updated: ${memory.id}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error updating memory")
            false
        }
    }
    
    override suspend fun deleteMemory(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            memoryDao.deleteMemoryById(id)
            Timber.d("Memory deleted: $id")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error deleting memory")
            false
        }
    }
    
    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            memoryDao.deleteAllMemories()
            Timber.w("All memories cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing all memories")
        }
    }
    
    override suspend fun getMemoryStats(): MemoryStats = withContext(Dispatchers.IO) {
        try {
            val count = memoryDao.getMemoryCount()
            val size = memoryDao.getTotalMemorySize() ?: 0L
            
            MemoryStats(
                totalMemories = count,
                totalSize = size,
                averageAccessFrequency = 0f,  // TODO: Calculate from access counts
                oldestMemoryTime = 0L,         // TODO: Query from database
                newestMemoryTime = System.currentTimeMillis(),
                memoryTypeDistribution = emptyMap()  // TODO: Calculate from database
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting memory stats")
            MemoryStats()
        }
    }
    
    // Helper extension functions for entity conversion
    private fun MemoryItem.toEntity() = MemoryEntity(
        id = id,
        type = type.name,
        content = content,
        semanticVector = semanticVector.toString(),
        metadata = metadata.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        importance = importance,
        accessCount = accessCount
    )
    
    private fun MemoryEntity.toMemoryItem() = MemoryItem(
        id = id,
        type = try {
            MemoryType.valueOf(type)
        } catch (e: Exception) {
            MemoryType.EPISODIC
        },
        content = content,
        semanticVector = emptyList(),  // TODO: Parse from stored vector
        metadata = emptyMap(),           // TODO: Parse from stored metadata
        createdAt = createdAt,
        updatedAt = updatedAt,
        importance = importance,
        accessCount = accessCount
    )
}
