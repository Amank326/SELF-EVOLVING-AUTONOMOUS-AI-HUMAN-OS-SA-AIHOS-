package com.aihos.ai.memory

import kotlin.math.*

/**
 * Advanced Memory Layer with Semantic Vector Search
 * Implements semantic memory with vector embeddings, consolidation, and intelligent retrieval
 */
interface AdvancedMemoryLayer : MemoryLayer {
    
    /**
     * Store memory with semantic vector embedding
     */
    suspend fun storeWithSemanticVector(
        memory: MemoryItem,
        embedding: FloatArray
    ): String
    
    /**
     * Semantic search using vector similarity
     */
    suspend fun semanticSearch(
        query: String,
        embedding: FloatArray,
        limit: Int = 10,
        similarityThreshold: Float = 0.5f
    ): List<MemorySearchResult>
    
    /**
     * Consolidate related memories into clusters
     */
    suspend fun consolidateMemories(
        consolidationFactor: Float = 0.8f
    ): List<MemoryCluster>
    
    /**
     * Implement memory decay - importance decreases over time
     */
    suspend fun applyMemoryDecay()
    
    /**
     * Get related memories through vector similarity
     */
    suspend fun getRelatedMemories(
        memoryId: String,
        similarityThreshold: Float = 0.6f
    ): List<MemoryItem>
}

/**
 * Default implementation of Advanced Memory Layer
 */
class DefaultAdvancedMemoryLayer : AdvancedMemoryLayer {
    
    private val memories = mutableListOf<MemoryItemWithVector>()
    private val semanticClusters = mutableListOf<MemoryCluster>()
    
    data class MemoryItemWithVector(
        val memory: MemoryItem,
        val embedding: FloatArray,
        val vectorMagnitude: Float = calculateMagnitude(embedding)
    ) {
        companion object {
            private fun calculateMagnitude(vector: FloatArray): Float {
                return sqrt(vector.sumOf { it * it.toDouble() }).toFloat()
            }
        }
    }
    
    override suspend fun store(memory: MemoryItem): String {
        val defaultEmbedding = FloatArray(384) { 0.1f } // Placeholder
        return storeWithSemanticVector(memory, defaultEmbedding)
    }
    
    override suspend fun storeWithSemanticVector(
        memory: MemoryItem,
        embedding: FloatArray
    ): String {
        val itemWithVector = MemoryItemWithVector(memory, embedding)
        memories.add(itemWithVector)
        return memory.id
    }
    
    override suspend fun retrieve(id: String): MemoryItem? {
        return memories.find { it.memory.id == id }?.memory?.apply {
            accessCount++
        }
    }
    
    override suspend fun search(query: String): List<MemoryItem> {
        return memories.filter { item ->
            item.memory.content.contains(query, ignoreCase = true)
        }.sortedByDescending { it.memory.importance }
         .take(10)
         .map { it.memory }
    }
    
    override suspend fun semanticSearch(
        query: String,
        embedding: FloatArray,
        limit: Int,
        similarityThreshold: Float
    ): List<MemorySearchResult> {
        return memories
            .map { item ->
                val similarity = cosineSimilarity(embedding, item.embedding)
                MemorySearchResult(
                    memory = item.memory,
                    similarity = similarity,
                    relevanceScore = similarity * item.memory.importance
                )
            }
            .filter { it.similarity >= similarityThreshold }
            .sortedByDescending { it.relevanceScore }
            .take(limit)
    }
    
    override suspend fun consolidateMemories(
        consolidationFactor: Float
    ): List<MemoryCluster> {
        if (memories.isEmpty()) return emptyList()
        
        // Simple clustering using vector similarity
        val clusters = mutableListOf<MemoryCluster>()
        val processed = mutableSetOf<String>()
        
        memories.forEach { item ->
            if (!processed.contains(item.memory.id)) {
                val cluster = mutableListOf<MemoryItem>()
                cluster.add(item.memory)
                processed.add(item.memory.id)
                
                // Find related memories
                memories.forEach { other ->
                    if (!processed.contains(other.memory.id)) {
                        val similarity = cosineSimilarity(item.embedding, other.embedding)
                        if (similarity > consolidationFactor) {
                            cluster.add(other.memory)
                            processed.add(other.memory.id)
                        }
                    }
                }
                
                // Create cluster
                val centerVector = calculateClusterCenter(
                    cluster.map { m -> 
                        memories.find { it.memory.id == m.id }?.embedding 
                            ?: FloatArray(384)
                    }
                )
                
                clusters.add(
                    MemoryCluster(
                        clusterId = "cluster_${clusters.size}",
                        memories = cluster,
                        centerVector = centerVector,
                        density = calculateClusterDensity(cluster, centerVector)
                    )
                )
            }
        }
        
        semanticClusters.clear()
        semanticClusters.addAll(clusters)
        return clusters
    }
    
    override suspend fun applyMemoryDecay() {
        val now = System.currentTimeMillis()
        memories.forEach { item ->
            val ageInDays = (now - item.memory.createdAt) / (1000 * 60 * 60 * 24).toDouble()
            val decayFactor = exp(-0.01 * ageInDays).toFloat()
            item.memory.importance *= decayFactor
        }
    }
    
    override suspend fun getRelatedMemories(
        memoryId: String,
        similarityThreshold: Float
    ): List<MemoryItem> {
        val targetMemory = memories.find { it.memory.id == memoryId } ?: return emptyList()
        
        return memories
            .filter { it.memory.id != memoryId }
            .filter { other ->
                cosineSimilarity(targetMemory.embedding, other.embedding) > similarityThreshold
            }
            .sortedByDescending { cosineSimilarity(targetMemory.embedding, it.embedding) }
            .map { it.memory }
    }
    
    override suspend fun consolidate() {
        consolidateMemories()
    }
    
    override suspend fun clear() {
        memories.clear()
        semanticClusters.clear()
    }
    
    // Helper functions
    
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size || vec1.isEmpty()) return 0f
        
        val dotProduct = vec1.indices.sumOf { i -> 
            (vec1[i].toDouble() * vec2[i].toDouble())
        }.toFloat()
        
        val magnitude1 = sqrt(vec1.sumOf { it * it.toDouble() }).toFloat()
        val magnitude2 = sqrt(vec2.sumOf { it * it.toDouble() }).toFloat()
        
        if (magnitude1 == 0f || magnitude2 == 0f) return 0f
        
        return dotProduct / (magnitude1 * magnitude2)
    }
    
    private fun calculateClusterCenter(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return FloatArray(384)
        
        val center = FloatArray(vectors[0].size)
        vectors.forEach { vector ->
            vector.indices.forEach { i ->
                center[i] += vector[i]
            }
        }
        
        center.indices.forEach { i ->
            center[i] /= vectors.size
        }
        
        return center
    }
    
    private fun calculateClusterDensity(
        memories: List<MemoryItem>,
        center: FloatArray
    ): Float {
        if (memories.size <= 1) return 1f
        
        var totalSimilarity = 0f
        memories.forEach { memory ->
            val memoryVector = this.memories.find { it.memory.id == memory.id }?.embedding 
                ?: return@forEach
            totalSimilarity += cosineSimilarity(center, memoryVector)
        }
        
        return totalSimilarity / memories.size
    }
}

/**
 * Data models for advanced memory operations
 */

data class MemorySearchResult(
    val memory: MemoryItem,
    val similarity: Float,
    val relevanceScore: Float
)

data class MemoryCluster(
    val clusterId: String,
    val memories: List<MemoryItem>,
    val centerVector: FloatArray,
    val density: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryCluster) return false
        
        if (clusterId != other.clusterId) return false
        if (memories != other.memories) return false
        if (!centerVector.contentEquals(other.centerVector)) return false
        if (density != other.density) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = clusterId.hashCode()
        result = 31 * result + memories.hashCode()
        result = 31 * result + centerVector.contentHashCode()
        result = 31 * result + density.hashCode()
        return result
    }
}

data class SemanticVector(
    val id: String,
    val embeddings: FloatArray,
    val memoryId: String,
    val similarity: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticVector) return false
        
        if (id != other.id) return false
        if (!embeddings.contentEquals(other.embeddings)) return false
        if (memoryId != other.memoryId) return false
        if (similarity != other.similarity) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + embeddings.contentHashCode()
        result = 31 * result + memoryId.hashCode()
        result = 31 * result + similarity.hashCode()
        return result
    }
}
