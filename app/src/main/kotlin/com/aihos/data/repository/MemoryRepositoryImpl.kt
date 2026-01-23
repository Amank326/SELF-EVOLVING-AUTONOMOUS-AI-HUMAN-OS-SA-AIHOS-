package com.aihos.data.repository

import com.aihos.ai.memory.*
import com.aihos.data.db.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Room-based implementation of MemoryRepository
 * Persistent storage using SQLite
 */
class RoomMemoryRepository(
    private val database: SAIHOSDatabase
) : MemoryRepository {
    
    private val json = Json
    private val episodeDao = database.episodeDao()
    private val ruleDao = database.ruleDao()
    private val factDao = database.factDao()
    
    override suspend fun storeEpisode(episode: Episode): String {
        try {
            val entity = EpisodeEntity(
                id = episode.id,
                timestamp = episode.timestamp,
                decision = episode.decision,
                action = episode.action,
                contextJson = json.encodeToString(episode.context),
                outcome = episode.outcome.name,
                reasoning = episode.reasoning,
                reflection = episode.reflection,
                createdAt = episode.createdAt
            )
            episodeDao.insert(entity)
            Timber.d("Episode stored: ${episode.id}")
            return episode.id
        } catch (e: Exception) {
            Timber.e(e, "Failed to store episode")
            throw e
        }
    }
    
    override suspend fun getEpisode(id: String): Episode? {
        return try {
            episodeDao.getById(id)?.toEpisode()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get episode")
            null
        }
    }
    
    override suspend fun queryEpisodes(filter: EpisodeFilter): List<Episode> {
        return try {
            val episodes = when {
                filter.startTime != null && filter.endTime != null -> {
                    episodeDao.queryByTimeRange(filter.startTime, filter.endTime)
                }
                else -> {
                    episodeDao.getRecent(filter.limit)
                }
            }
            
            episodes
                .map { it.toEpisode() }
                .filter { episode ->
                    (filter.outcome == null || episode.outcome == filter.outcome) &&
                    (filter.actionType == null || episode.action == filter.actionType)
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query episodes")
            emptyList()
        }
    }
    
    override suspend fun getRecentEpisodes(count: Int): List<Episode> {
        return try {
            episodeDao.getRecent(count).map { it.toEpisode() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get recent episodes")
            emptyList()
        }
    }
    
    override suspend fun storeRule(rule: BehavioralRule): String {
        try {
            val entity = BehavioralRuleEntity(
                id = rule.id,
                condition = rule.condition,
                action = rule.action,
                weight = rule.weight,
                successCount = rule.successCount,
                failureCount = rule.failureCount,
                createdAt = rule.createdAt,
                evolvedAt = rule.evolvedAt,
                isActive = rule.isActive
            )
            ruleDao.insert(entity)
            Timber.d("Rule stored: ${rule.id}")
            return rule.id
        } catch (e: Exception) {
            Timber.e(e, "Failed to store rule")
            throw e
        }
    }
    
    override suspend fun getRule(id: String): BehavioralRule? {
        return try {
            ruleDao.getById(id)?.toBehavioralRule()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get rule")
            null
        }
    }
    
    override suspend fun getAllRules(): List<BehavioralRule> {
        return try {
            ruleDao.getAll().map { it.toBehavioralRule() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all rules")
            emptyList()
        }
    }
    
    override suspend fun getActiveRules(): List<BehavioralRule> {
        return try {
            ruleDao.getActive().map { it.toBehavioralRule() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get active rules")
            emptyList()
        }
    }
    
    override suspend fun updateRuleWeight(ruleId: String, newWeight: Float) {
        try {
            val rule = ruleDao.getById(ruleId) ?: return
            ruleDao.update(rule.copy(weight = newWeight, evolvedAt = System.currentTimeMillis()))
            Timber.d("Rule weight updated: $ruleId -> $newWeight")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update rule weight")
        }
    }
    
    override suspend fun updateRuleOutcome(ruleId: String, isSuccess: Boolean) {
        try {
            val rule = ruleDao.getById(ruleId) ?: return
            val updated = if (isSuccess) {
                rule.copy(successCount = rule.successCount + 1)
            } else {
                rule.copy(failureCount = rule.failureCount + 1)
            }
            ruleDao.update(updated)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update rule outcome")
        }
    }
    
    override suspend fun storeFact(fact: SemanticFact): String {
        try {
            val entity = SemanticFactEntity(
                id = fact.id,
                fact = fact.fact,
                confidence = fact.confidence,
                sourcesJson = json.encodeToString(fact.sources),
                lastUpdated = fact.lastUpdated
            )
            factDao.insert(entity)
            Timber.d("Fact stored: ${fact.id}")
            return fact.id
        } catch (e: Exception) {
            Timber.e(e, "Failed to store fact")
            throw e
        }
    }
    
    override suspend fun queryFacts(pattern: String): List<SemanticFact> {
        return try {
            factDao.queryByPattern(pattern).map { it.toSemanticFact() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query facts")
            emptyList()
        }
    }
    
    override suspend fun getMemoryStats(): MemoryStats {
        return try {
            val episodes = episodeDao.getRecent(Int.MAX_VALUE)
            val rules = ruleDao.getAll()
            
            MemoryStats(
                totalEpisodes = episodeDao.count(),
                totalRules = rules.size,
                totalFacts = 0, // TODO
                memoryUsageBytes = calculateMemoryUsage(),
                oldestEpisodeTime = episodes.lastOrNull()?.timestamp,
                newestEpisodeTime = episodes.firstOrNull()?.timestamp
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get memory stats")
            MemoryStats(0, 0, 0, 0, null, null)
        }
    }
    
    override suspend fun clearOldData(beforeTimestamp: Long) {
        try {
            val deleted = episodeDao.deleteOlderThan(beforeTimestamp)
            Timber.i("Cleared $deleted old episodes")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear old data")
        }
    }
    
    // HELPERS
    
    private fun EpisodeEntity.toEpisode(): Episode {
        return Episode(
            id = id,
            timestamp = timestamp,
            decision = decision,
            action = action,
            context = json.decodeFromString(contextJson),
            outcome = Outcome.valueOf(outcome),
            reasoning = reasoning,
            reflection = reflection,
            createdAt = createdAt
        )
    }
    
    private fun BehavioralRuleEntity.toBehavioralRule(): BehavioralRule {
        return BehavioralRule(
            id = id,
            condition = condition,
            action = action,
            weight = weight,
            successCount = successCount,
            failureCount = failureCount,
            createdAt = createdAt,
            evolvedAt = evolvedAt,
            isActive = isActive
        )
    }
    
    private fun SemanticFactEntity.toSemanticFact(): SemanticFact {
        return SemanticFact(
            id = id,
            fact = fact,
            confidence = confidence,
            sources = json.decodeFromString(sourcesJson),
            lastUpdated = lastUpdated
        )
    }
    
    private fun calculateMemoryUsage(): Long {
        // Rough estimate: count * average size
        // Real implementation would measure actual DB file size
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
}
