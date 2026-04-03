/**
 * Reasoning Engine - Decision generation and analysis
 * Generates options, scores decisions, and provides reasoning chains
 */

class ReasoningEngine {
    constructor(memoryManager) {
        this.memoryManager = memoryManager;
        this.reasoningHistory = [];
        this.lastReasoning = null;
    }

    /**
     * Analyze context and generate decision options
     */
    async analyzeOptions(context, goals) {
        const analysis = {
            timestamp: new Date(),
            context: context,
            goals: goals || [],
            options: [],
            recommendation: null,
            reasoning: {}
        };

        try {
            // Generate options
            analysis.options = await this.generateOptions(context, goals);

            // Score and rank options
            analysis.options = await this.scoreOptions(analysis.options, context);

            // Select best option
            analysis.recommendation = analysis.options[0] || null;

            // Generate reasoning chain
            analysis.reasoning = this.generateReasoningChain(analysis);

            // Store in history
            this.reasoningHistory.push(analysis);
            this.lastReasoning = analysis;

            if (this.reasoningHistory.length > 100) {
                this.reasoningHistory.shift();
            }

            return analysis;

        } catch (error) {
            console.error('Analysis error:', error);
            return analysis;
        }
    }

    /**
     * Generate decision options based on context
     */
    async generateOptions(context, goals) {
        const options = [];

        // Get relevant memories
        const memories = await this.memoryManager.getRelevantMemories(context);

        // Option 1: Execute tasks
        if (context.pendingTasks && context.pendingTasks.length > 0) {
            const urgentTasks = context.pendingTasks
                .filter(t => t.priority === 'HIGH' || t.priority === 'URGENT')
                .slice(0, 3);

            for (const task of urgentTasks) {
                options.push({
                    id: `opt_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
                    action: 'execute_task',
                    target: task.id,
                    description: `Execute task: ${task.title}`,
                    rationale: `Task prioritized as ${task.priority}`,
                    estimatedReward: 0.8,
                    riskLevel: 'LOW',
                    confidence: 0.9
                });
            }
        }

        // Option 2: Memory consolidation
        if (context.memoryStats && context.memoryStats.utilizationPercent > 70) {
            options.push({
                id: `opt_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
                action: 'consolidate_memory',
                description: 'Consolidate and compress memory',
                rationale: `Memory at ${context.memoryStats.utilizationPercent}% utilization`,
                estimatedReward: 0.6,
                riskLevel: 'LOW',
                confidence: 0.85
            });
        }

        // Option 3: Deep reflection
        options.push({
            id: `opt_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            action: 'trigger_reflection',
            description: 'Analyze recent decisions for patterns',
            rationale: 'Continuous learning and improvement',
            estimatedReward: 0.5,
            riskLevel: 'MINIMAL',
            confidence: 0.8
        });

        // Option 4: Explore new patterns
        if (Math.random() > 0.6) {
            options.push({
                id: `opt_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
                action: 'explore_patterns',
                description: 'Investigate new behavioral patterns',
                rationale: 'Discovery and exploration for future optimization',
                estimatedReward: 0.45,
                riskLevel: 'MEDIUM',
                confidence: 0.7
            });
        }

        return options;
    }

    /**
     * Score options based on multiple factors
     */
    async scoreOptions(options, context) {
        for (const option of options) {
            let score = 0;

            // Factor 1: Historical success (20%)
            if (option.action) {
                const historicalEpisodes = await this.memoryManager.queryEpisodes({
                    action: option.action,
                    limit: 10
                });

                if (historicalEpisodes.length > 0) {
                    const successRate = historicalEpisodes.filter(e => e.success).length / historicalEpisodes.length;
                    score += successRate * 0.2;
                }
            }

            // Factor 2: Expected reward (30%)
            score += (option.estimatedReward || 0.5) * 0.3;

            // Factor 3: Risk level (20%)
            const riskScores = { 'MINIMAL': 1.0, 'LOW': 0.8, 'MEDIUM': 0.5, 'HIGH': 0.2 };
            score += (riskScores[option.riskLevel] || 0.5) * 0.2;

            // Factor 4: Confidence (20%)
            score += (option.confidence || 0.5) * 0.2;

            // Factor 5: Alignment with goals (10%)
            if (context.goals && context.goals.length > 0) {
                const goalAlignment = context.goals.some(g => 
                    option.description.toLowerCase().includes(g.toLowerCase())
                ) ? 1.0 : 0.5;
                score += goalAlignment * 0.1;
            }

            option.score = Math.max(0, Math.min(1, score + (Math.random() - 0.5) * 0.05));
        }

        // Sort by score (descending)
        return options.sort((a, b) => b.score - a.score);
    }

    /**
     * Generate reasoning chain explaining the decision
     */
    generateReasoningChain(analysis) {
        const chain = {
            contextFactors: [],
            decisionPath: [],
            conclusion: ''
        };

        // Identify context factors
        if (analysis.context.pendingTasks && analysis.context.pendingTasks.length > 0) {
            chain.contextFactors.push(`${analysis.context.pendingTasks.length} pending tasks detected`);
        }

        if (analysis.context.memoryStats) {
            const utilization = analysis.context.memoryStats.utilizationPercent;
            if (utilization > 70) {
                chain.contextFactors.push(`Memory utilization high (${utilization}%)`);
            }
        }

        // Decision path
        if (analysis.options.length > 0) {
            const topOption = analysis.options[0];
            chain.decisionPath = [
                `Evaluated ${analysis.options.length} possible actions`,
                `Highest-scoring option: ${topOption.action}`,
                `Score: ${(topOption.score * 100).toFixed(1)}%`,
                `Confidence: ${(topOption.confidence * 100).toFixed(1)}%`
            ];

            chain.conclusion = `Recommended action: ${topOption.description}`;
        }

        return chain;
    }

    /**
     * Get latest reasoning
     */
    async getLatestReasoning() {
        return this.lastReasoning || { message: 'No recent reasoning available' };
    }

    /**
     * Get reasoning history
     */
    getReasoningHistory(limit = 10) {
        return this.reasoningHistory
            .slice(-limit)
            .reverse()
            .map(r => ({
                timestamp: r.timestamp,
                optionsCount: r.options.length,
                recommendation: r.recommendation?.action,
                score: r.recommendation?.score
            }));
    }

    /**
     * Explain a specific decision
     */
    async explainDecision(decisionId) {
        const explanation = {
            decisionId,
            timestamp: new Date(),
            factors: [],
            alternatives: [],
            confidence: 0.7
        };

        // Build explanation from last reasoning
        if (this.lastReasoning) {
            explanation.factors = this.lastReasoning.reasoning.contextFactors;
            explanation.alternatives = this.lastReasoning.options
                .slice(1, 4)
                .map(o => ({
                    action: o.action,
                    score: (o.score * 100).toFixed(1) + '%'
                }));
        }

        return explanation;
    }

    /**
     * Predict consequences of an action
     */
    async predictConsequences(action, context) {
        const prediction = {
            action,
            timestamp: new Date(),
            likelyOutcomes: [],
            riskAssessment: 'MEDIUM'
        };

        // Query similar past episodes
        const similar = await this.memoryManager.queryEpisodes({
            action: action,
            limit: 10
        });

        if (similar.length > 0) {
            const successCount = similar.filter(e => e.success).length;
            const successRate = successCount / similar.length;

            prediction.likelyOutcomes = [
                {
                    type: 'SUCCESS',
                    probability: (successRate * 100).toFixed(1) + '%'
                },
                {
                    type: 'PARTIAL',
                    probability: ((1 - successRate) * 50).toFixed(1) + '%'
                },
                {
                    type: 'FAILURE',
                    probability: ((1 - successRate) * 50).toFixed(1) + '%'
                }
            ];

            // Assess risk
            if (successRate > 0.8) {
                prediction.riskAssessment = 'LOW';
            } else if (successRate < 0.5) {
                prediction.riskAssessment = 'HIGH';
            }
        }

        return prediction;
    }

    /**
     * Compare two options
     */
    async compareOptions(option1Id, option2Id) {
        if (!this.lastReasoning) {
            return { error: 'No reasoning available' };
        }

        const opt1 = this.lastReasoning.options.find(o => o.id === option1Id);
        const opt2 = this.lastReasoning.options.find(o => o.id === option2Id);

        if (!opt1 || !opt2) {
            return { error: 'Option not found' };
        }

        return {
            option1: {
                action: opt1.action,
                score: (opt1.score * 100).toFixed(1) + '%',
                confidence: (opt1.confidence * 100).toFixed(1) + '%',
                risk: opt1.riskLevel
            },
            option2: {
                action: opt2.action,
                score: (opt2.score * 100).toFixed(1) + '%',
                confidence: (opt2.confidence * 100).toFixed(1) + '%',
                risk: opt2.riskLevel
            },
            winner: opt1.score > opt2.score ? 'option1' : 'option2',
            scoreDifference: ((Math.abs(opt1.score - opt2.score)) * 100).toFixed(1) + '%'
        };
    }

    /**
     * Generate recommendations based on goals
     */
    async getRecommendations(goals = []) {
        const recommendations = {
            timestamp: new Date(),
            goals,
            suggestions: [],
            priorities: []
        };

        // Get current memory stats
        const memStats = await this.memoryManager.getMemoryStats();

        // Recommendation 1: Memory management
        if (memStats.utilizationPercent > 70) {
            recommendations.suggestions.push({
                priority: 'HIGH',
                action: 'consolidate_memory',
                reason: `Memory utilization at ${memStats.utilizationPercent}%`
            });
        }

        // Recommendation 2: Task processing
        recommendations.suggestions.push({
            priority: 'MEDIUM',
            action: 'process_pending_tasks',
            reason: 'Maintain system responsiveness'
        });

        // Recommendation 3: Learning
        recommendations.suggestions.push({
            priority: 'MEDIUM',
            action: 'analyze_patterns',
            reason: 'Continuous improvement and adaptation'
        });

        return recommendations;
    }
}

module.exports = ReasoningEngine;
