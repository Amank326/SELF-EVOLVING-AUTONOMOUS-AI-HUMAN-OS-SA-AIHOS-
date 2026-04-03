/**
 * Memory Manager - Multi-temporal storage system
 * Manages Episodic, Semantic, and Procedural memory
 */

class MemoryManager {
    constructor(database) {
        this.database = database;
        this.episodicMemory = [];
        this.semanticMemory = [];
        this.proceduralMemory = [];
        this.attentionMemory = []; // Recent high-importance items
        this.maxAttentionSize = 50;
    }

    /**
     * Initialize memory systems
     */
    async initialize() {
        console.log('🧠 Initializing Memory Manager...');

        try {
            // Load existing memories from database
            this.episodicMemory = await this.database.getAllEpisodes();
            this.semanticMemory = await this.database.getAllSemanticFacts();
            this.proceduralMemory = await this.database.getAllProceduralRules();

            console.log(`✅ Memory systems loaded:`);
            console.log(`   📝 Episodic: ${this.episodicMemory.length} episodes`);
            console.log(`   💡 Semantic: ${this.semanticMemory.length} facts`);
            console.log(`   🎯 Procedural: ${this.proceduralMemory.length} rules`);

        } catch (error) {
            console.error('Memory initialization error:', error);
        }
    }

    // ========================================================================
    // EPISODIC MEMORY - Events with timestamps and outcomes
    // ========================================================================

    /**
     * Store an episodic memory (event with timestamp)
     */
    async storeEpisode(episode) {
        const episodeRecord = {
            id: `ep_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            decision: episode.decision || 'unknown',
            action: episode.action || 'unknown',
            context: episode.context || {},
            outcome: episode.outcome || 'UNKNOWN',
            reasoning: episode.reasoning || '',
            reflection: episode.reflection || '',
            timestamp: new Date(),
            success: episode.outcome === 'SUCCESS' || episode.success === true
        };

        this.episodicMemory.push(episodeRecord);
        
        // Store in database
        await this.database.storeEpisode(episodeRecord);

        // Add to attention memory (FIFO - max size 50)
        this.attentionMemory.unshift(episodeRecord);
        if (this.attentionMemory.length > this.maxAttentionSize) {
            this.attentionMemory.pop();
        }

        return episodeRecord.id;
    }

    /**
     * Get recent episodes
     */
    async getRecentEpisodes(limit = 10) {
        return this.episodicMemory
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
            .slice(0, limit);
    }

    /**
     * Query episodes by filter
     */
    async queryEpisodes(filter) {
        let results = this.episodicMemory;

        if (filter.action) {
            results = results.filter(e => e.action === filter.action);
        }

        if (filter.outcome) {
            results = results.filter(e => e.outcome === filter.outcome);
        }

        if (filter.startDate) {
            results = results.filter(e => new Date(e.timestamp) >= filter.startDate);
        }

        if (filter.limit) {
            results = results.slice(0, filter.limit);
        }

        return results;
    }

    // ========================================================================
    // SEMANTIC MEMORY - Learned facts and knowledge
    // ========================================================================

    /**
     * Store semantic fact
     */
    async storeSemanticFact(fact, confidence = 0.8, sources = []) {
        const semanticRecord = {
            id: `sem_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            fact: fact,
            confidence: confidence,
            sources: sources || [],
            createdAt: new Date(),
            updatedAt: new Date(),
            accessCount: 0
        };

        this.semanticMemory.push(semanticRecord);
        await this.database.storeSemanticFact(semanticRecord);

        return semanticRecord.id;
    }

    /**
     * Get all semantic facts
     */
    async getSemanticFacts(confidence = 0.5) {
        return this.semanticMemory.filter(f => f.confidence >= confidence);
    }

    /**
     * Update semantic fact confidence
     */
    async updateSemanticFact(factId, newConfidence) {
        const fact = this.semanticMemory.find(f => f.id === factId);
        if (fact) {
            fact.confidence = Math.max(0, Math.min(1, newConfidence));
            fact.updatedAt = new Date();
            await this.database.updateSemanticFact(fact);
        }
    }

    // ========================================================================
    // PROCEDURAL MEMORY - Behavioral rules and heuristics
    // ========================================================================

    /**
     * Store procedural rule
     */
    async storeProceduralRule(rule) {
        const ruleRecord = {
            id: rule.id || `rule_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            condition: rule.condition || 'always',
            action: rule.action || 'default_action',
            weight: rule.weight || 0.5,
            successRate: rule.successRate || 0.5,
            description: rule.description || '',
            createdAt: new Date(),
            evolvedAt: new Date(),
            executionCount: 0,
            successCount: 0
        };

        const existingIndex = this.proceduralMemory.findIndex(r => r.id === ruleRecord.id);
        if (existingIndex >= 0) {
            this.proceduralMemory[existingIndex] = ruleRecord;
        } else {
            this.proceduralMemory.push(ruleRecord);
        }

        await this.database.storeProceduralRule(ruleRecord);
        return ruleRecord.id;
    }

    /**
     * Get all procedural rules
     */
    async getProceduralRules() {
        return this.proceduralMemory.sort((a, b) => b.weight - a.weight);
    }

    /**
     * Update procedural rule
     */
    async updateProceduralRule(rule) {
        const index = this.proceduralMemory.findIndex(r => r.id === rule.id);
        if (index >= 0) {
            this.proceduralMemory[index] = {
                ...this.proceduralMemory[index],
                ...rule,
                evolvedAt: new Date()
            };
            await this.database.updateProceduralRule(this.proceduralMemory[index]);
        }
    }

    // ========================================================================
    // ATTENTION MEMORY - Recent high-importance items
    // ========================================================================

    /**
     * Get attention memory (recent events)
     */
    getAttentionMemory() {
        return this.attentionMemory.slice(0, 20);
    }

    // ========================================================================
    // EVOLUTION TRACKING
    // ========================================================================

    /**
     * Get evolution rules (rules that have been modified)
     */
    async getEvolutionRules() {
        return this.proceduralMemory.filter(rule => {
            const created = new Date(rule.createdAt);
            const evolved = new Date(rule.evolvedAt);
            return evolved > created;
        }).map(rule => ({
            ...rule,
            modifications: rule.successRate > 0.7 ? 'improved' : 'adjusted'
        }));
    }

    /**
     * Update evolution rule
     */
    async updateEvolutionRule(ruleId, adjustment, reason) {
        const rule = this.proceduralMemory.find(r => r.id === ruleId);
        if (rule) {
            rule.weight = Math.max(0, Math.min(1, rule.weight + adjustment));
            rule.evolvedAt = new Date();
            await this.updateProceduralRule(rule);

            // Log evolution event
            await this.database.storeEvolutionEvent({
                ruleId,
                adjustment,
                reason,
                timestamp: new Date(),
                newWeight: rule.weight
            });
        }
    }

    /**
     * Load evolution rules from database
     */
    async loadEvolutionRules() {
        const rules = await this.database.getEvolutionHistory();
        // Rules are already in proceduralMemory
        return rules;
    }

    // ========================================================================
    // MEMORY STATISTICS & CONSOLIDATION
    // ========================================================================

    /**
     * Get memory statistics
     */
    async getMemoryStats() {
        const totalEpisodes = this.episodicMemory.length;
        const totalFacts = this.semanticMemory.length;
        const totalRules = this.proceduralMemory.length;

        // Calculate memory utilization (simplified)
        const estimatedSize = 
            (totalEpisodes * 0.5) + // ~500 bytes per episode
            (totalFacts * 0.2) + // ~200 bytes per fact
            (totalRules * 0.3); // ~300 bytes per rule

        const maxMemory = 100; // MB (simulated)
        const utilizationPercent = Math.min(100, (estimatedSize / maxMemory) * 100);

        return {
            episodicMemory: totalEpisodes,
            semanticMemory: totalFacts,
            proceduralMemory: totalRules,
            totalItems: totalEpisodes + totalFacts + totalRules,
            estimatedSizeMB: (estimatedSize / 1000).toFixed(2),
            maxMemoryMB: maxMemory,
            utilizationPercent: utilizationPercent.toFixed(1)
        };
    }

    /**
     * Get memory overview
     */
    async getMemoryOverview() {
        const stats = await this.getMemoryStats();
        const recentEpisodes = await this.getRecentEpisodes(5);
        const topFacts = this.semanticMemory
            .sort((a, b) => b.confidence - a.confidence)
            .slice(0, 5);
        const topRules = this.proceduralMemory
            .sort((a, b) => b.weight - a.weight)
            .slice(0, 5);

        return {
            statistics: stats,
            recentEpisodes,
            topSemanticFacts: topFacts.map(f => ({ fact: f.fact, confidence: f.confidence })),
            topBehavioralRules: topRules.map(r => ({ action: r.action, weight: r.weight }))
        };
    }

    /**
     * Consolidate memory (compress and clean)
     */
    async consolidateMemory() {
        console.log('🧠 Consolidating memory...');

        // Remove low-confidence facts
        const beforeFacts = this.semanticMemory.length;
        this.semanticMemory = this.semanticMemory.filter(f => f.confidence >= 0.3);
        console.log(`   Removed ${beforeFacts - this.semanticMemory.length} low-confidence facts`);

        // Archive old episodes (keep only recent 500)
        const beforeEpisodes = this.episodicMemory.length;
        if (this.episodicMemory.length > 500) {
            this.episodicMemory = this.episodicMemory
                .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
                .slice(0, 500);
        }
        console.log(`   Archived ${beforeEpisodes - this.episodicMemory.length} old episodes`);

        // Update database
        await this.database.consolidateMemory(this.episodicMemory, this.semanticMemory);

        console.log('✅ Memory consolidation complete');
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get relevant memories for a given context
     */
    async getRelevantMemories(context) {
        const relevant = {
            episodes: [],
            facts: [],
            rules: []
        };

        // Find relevant episodes
        if (context.action) {
            relevant.episodes = this.episodicMemory.filter(e => e.action === context.action);
        }

        // Find relevant facts
        if (context.topic) {
            relevant.facts = this.semanticMemory.filter(f => 
                f.fact.toLowerCase().includes(context.topic.toLowerCase())
            );
        }

        // Find relevant rules
        if (context.condition) {
            relevant.rules = this.proceduralMemory.filter(r => 
                r.condition === context.condition
            );
        }

        return relevant;
    }

    /**
     * Search memory
     */
    async searchMemory(query) {
        const results = {
            episodes: [],
            facts: [],
            rules: []
        };

        const queryLower = query.toLowerCase();

        results.episodes = this.episodicMemory.filter(e =>
            e.decision.toLowerCase().includes(queryLower) ||
            e.action.toLowerCase().includes(queryLower) ||
            e.reasoning.toLowerCase().includes(queryLower)
        ).slice(0, 10);

        results.facts = this.semanticMemory.filter(f =>
            f.fact.toLowerCase().includes(queryLower)
        ).slice(0, 10);

        results.rules = this.proceduralMemory.filter(r =>
            r.action.toLowerCase().includes(queryLower) ||
            r.description.toLowerCase().includes(queryLower)
        ).slice(0, 10);

        return results;
    }
}

module.exports = MemoryManager;
