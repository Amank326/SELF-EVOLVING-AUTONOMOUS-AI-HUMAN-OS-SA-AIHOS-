/**
 * Evolution Engine - Behavioral rule evolution and learning
 * Tracks learning progress and updates rule weights based on outcomes
 */

class EvolutionEngine {
    constructor(memoryManager, reasoningEngine) {
        this.memoryManager = memoryManager;
        this.reasoningEngine = reasoningEngine;
        this.evolutionHistory = [];
        this.learningStats = {
            totalDecisions: 0,
            successfulDecisions: 0,
            failedDecisions: 0,
            rulesEvolved: 0,
            averageImprovement: 0,
            learningRate: 0.1,
            adaptiveRate: true
        };
    }

    /**
     * Analyze decision outcome and evolve rules
     */
    analyzeOutcome(decision, outcome) {
        const isSuccess = outcome.success || outcome.satisfaction > 0.5;
        
        this.learningStats.totalDecisions++;
        if (isSuccess) {
            this.learningStats.successfulDecisions++;
        } else {
            this.learningStats.failedDecisions++;
        }

        // Find related rules and adjust weights
        const relatedRules = this.memoryManager.findRelatedRules(decision.context);
        for (const rule of relatedRules) {
            const improvement = this.calculateRuleImprovement(rule, decision, outcome);
            this.evolveRule(rule, improvement, decision, outcome);
        }

        // Adaptive learning rate
        if (this.learningStats.adaptiveRate) {
            this.adjustLearningRate();
        }

        return {
            isSuccess,
            rulesUpdated: relatedRules.length,
            improvementScore: this.learningStats.averageImprovement
        };
    }

    /**
     * Calculate how much to improve a rule
     */
    calculateRuleImprovement(rule, decision, outcome) {
        const baseImprovement = outcome.satisfaction || 0;
        const contextRelevance = this.getContextRelevance(rule, decision);
        const consistencyBonus = this.getConsistencyBonus(rule);
        
        return (baseImprovement * 0.7 + contextRelevance * 0.2 + consistencyBonus * 0.1);
    }

    /**
     * Get context relevance score
     */
    getContextRelevance(rule, decision) {
        if (!rule.context || !decision.context) return 0;
        
        let matchCount = 0;
        let totalKeys = Object.keys(rule.context).length;
        
        for (const key of Object.keys(rule.context)) {
            if (decision.context[key] === rule.context[key]) {
                matchCount++;
            }
        }
        
        return totalKeys > 0 ? matchCount / totalKeys : 0;
    }

    /**
     * Get consistency bonus
     */
    getConsistencyBonus(rule) {
        if (!rule.successCount || rule.successCount === 0) return 0;
        const consistency = rule.successCount / (rule.successCount + rule.failureCount);
        return Math.min(consistency * 0.5, 0.5);
    }

    /**
     * Update rule weights based on outcome
     */
    evolveRule(rule, improvement, decision, outcome) {
        const delta = this.learningStats.learningRate * improvement;
        
        rule.weight = Math.min(1.0, Math.max(0.0, rule.weight + delta));
        rule.lastUpdated = new Date();
        
        if (outcome.success) {
            rule.successCount = (rule.successCount || 0) + 1;
        } else {
            rule.failureCount = (rule.failureCount || 0) + 1;
        }

        // Track evolution event
        const event = {
            timestamp: new Date(),
            ruleId: rule.id,
            previousWeight: rule.weight - delta,
            newWeight: rule.weight,
            delta,
            improvement,
            decision,
            outcome
        };

        this.evolutionHistory.push(event);
        this.learningStats.rulesEvolved++;

        // Update average improvement
        this.learningStats.averageImprovement =
            (this.learningStats.averageImprovement * (this.learningStats.rulesEvolved - 1) + improvement) /
            this.learningStats.rulesEvolved;

        return event;
    }

    /**
     * Adjust learning rate based on performance
     */
    adjustLearningRate() {
        if (this.learningStats.totalDecisions < 10) return;

        const successRate = this.learningStats.successfulDecisions / this.learningStats.totalDecisions;
        
        if (successRate > 0.7) {
            this.learningStats.learningRate = Math.min(0.2, this.learningStats.learningRate * 1.1);
        } else if (successRate < 0.3) {
            this.learningStats.learningRate = Math.max(0.01, this.learningStats.learningRate * 0.9);
        }
    }

    /**
     * Get evolution statistics
     */
    getStatistics() {
        const successRate = this.learningStats.totalDecisions > 0
            ? (this.learningStats.successfulDecisions / this.learningStats.totalDecisions) * 100
            : 0;

        return {
            ...this.learningStats,
            successRate: successRate.toFixed(2),
            recentEvents: this.evolutionHistory.slice(-10)
        };
    }

    /**
     * Get evolution timeline
     */
    getTimeline(limit = 50) {
        return this.evolutionHistory.slice(-limit).map(event => ({
            timestamp: event.timestamp,
            ruleId: event.ruleId,
            delta: event.delta.toFixed(3),
            newWeight: event.newWeight.toFixed(3),
            improvement: event.improvement.toFixed(3)
        }));
    }

    /**
     * Get top evolved rules
     */
    getTopEvolvedRules(limit = 5) {
        const rules = this.memoryManager.procedural || [];
        return rules
            .sort((a, b) => (b.weight || 0) - (a.weight || 0))
            .slice(0, limit)
            .map(rule => ({
                id: rule.id,
                weight: rule.weight,
                successCount: rule.successCount || 0,
                failureCount: rule.failureCount || 0,
                successRate: rule.successCount
                    ? ((rule.successCount / (rule.successCount + (rule.failureCount || 0))) * 100).toFixed(2)
                    : 0
            }));
    }

    /**
     * Reset learning if needed
     */
    resetLearning() {
        this.evolutionHistory = [];
        this.learningStats = {
            totalDecisions: 0,
            successfulDecisions: 0,
            failedDecisions: 0,
            rulesEvolved: 0,
            averageImprovement: 0,
            learningRate: 0.1,
            adaptiveRate: true
        };
    }
}

module.exports = EvolutionEngine;
