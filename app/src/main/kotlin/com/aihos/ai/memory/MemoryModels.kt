package com.aihos.ai.memory

import kotlinx.serialization.Serializable
import java.util.*

/**
 * Core memory model representing a complete decision episode
 * This is the fundamental unit of learning in SA-AIHOS
 */
@Serializable
data class Episode(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val decision: String,
    val action: String,
    val context: Map<String, String>,
    val outcome: Outcome = Outcome.PENDING,
    val reasoning: String = "",
    val reflection: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SemanticFact(
    val id: String = UUID.randomUUID().toString(),
    val fact: String,
    val confidence: Float,
    val sources: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Behavioral rule: decision heuristic that AI uses
 * Rules evolve over time based on outcomes
 */
@Serializable
data class BehavioralRule(
    val id: String = UUID.randomUUID().toString(),
    val condition: String, // "time > 22:00 AND usageTime > 120min"
    val action: String, // "send_focus_reminder"
    val weight: Float = 0.5f, // Strength: 0.0 (never use) to 1.0 (always use)
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val evolvedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val successRate: Float
        get() = if (successCount + failureCount == 0) 0.5f else successCount.toFloat() / (successCount + failureCount)
}

@Serializable
enum class Outcome {
    PENDING, SUCCESS, PARTIAL, FAILURE
}

/**
 * MemoryRepository interface
 * Implementations should handle persistence, querying, and updates
 */
interface MemoryRepository {
    // Episode operations
    suspend fun storeEpisode(episode: Episode): String
    suspend fun getEpisode(id: String): Episode?
    suspend fun queryEpisodes(filter: EpisodeFilter): List<Episode>
    suspend fun getRecentEpisodes(count: Int): List<Episode>
    
    // Behavioral rule operations
    suspend fun storeRule(rule: BehavioralRule): String
    suspend fun getRule(id: String): BehavioralRule?
    suspend fun getAllRules(): List<BehavioralRule>
    suspend fun getActiveRules(): List<BehavioralRule>
    suspend fun updateRuleWeight(ruleId: String, newWeight: Float)
    suspend fun updateRuleOutcome(ruleId: String, isSuccess: Boolean)
    
    // Semantic fact operations
    suspend fun storeFact(fact: SemanticFact): String
    suspend fun queryFacts(pattern: String): List<SemanticFact>
    
    // Utility
    suspend fun getMemoryStats(): MemoryStats
    suspend fun clearOldData(beforeTimestamp: Long)
}

@Serializable
data class EpisodeFilter(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val outcome: Outcome? = null,
    val actionType: String? = null,
    val limit: Int = 100
)

@Serializable
data class MemoryStats(
    val totalEpisodes: Int,
    val totalRules: Int,
    val totalFacts: Int,
    val memoryUsageBytes: Long,
    val oldestEpisodeTime: Long?,
    val newestEpisodeTime: Long?
)
