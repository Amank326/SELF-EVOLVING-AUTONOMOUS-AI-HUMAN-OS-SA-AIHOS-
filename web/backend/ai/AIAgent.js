/**
 * AI Agent - Self-Evolving Autonomous Reasoning System
 * Core decision-making loop: THINK → ACT → REFLECT → EVOLVE
 */

const EventEmitter = require('events');

class AIAgent extends EventEmitter {
    constructor(memoryManager, reasoningEngine, database) {
        super();
        this.memoryManager = memoryManager;
        this.reasoningEngine = reasoningEngine;
        this.database = database;

        // Configuration
        this.autonomyLevel = 75; // 0-100 (% likelihood to act autonomously)
        this.isRunning = false;
        this.thoughtMode = 'CONTINUOUS'; // CONTINUOUS, SCHEDULED, MANUAL
        this.decisionIntervalMs = 5000; // Check for decisions every 5 seconds
        this.lastThought = null;
        this.nextDecisionTime = Date.now();

        // Decision tracking
        this.decisionsToday = 0;
        this.successfulDecisions = 0;
        this.reflectionQueue = [];

        // Cognition loop control
        this.cognitionLoopRunning = false;
        this.cognitionLoopInterval = null;
    }

    /**
     * Initialize AI Agent
     */
    async initialize() {
        console.log('🤖 Initializing AI Agent...');
        
        // Load previous evolution rules
        await this.memoryManager.loadEvolutionRules();
        
        // Initialize default behavioral rules if needed
        const existingRules = await this.memoryManager.getProceduralRules();
        if (existingRules.length === 0) {
            await this.initializeDefaultRules();
        }

        console.log('✅ AI Agent initialized');
    }

    /**
     * Initialize default behavioral rules
     */
    async initializeDefaultRules() {
        const defaultRules = [
            {
                id: 'rule_1',
                condition: 'task_pending_and_urgent',
                action: 'execute_task',
                weight: 0.9,
                description: 'Execute urgent pending tasks'
            },
            {
                id: 'rule_2',
                condition: 'memory_utilization_high',
                action: 'consolidate_memory',
                weight: 0.7,
                description: 'Consolidate memory when utilization is high'
            },
            {
                id: 'rule_3',
                condition: 'decision_accuracy_low',
                action: 'increase_reflection',
                weight: 0.8,
                description: 'Reflect more when decision accuracy is low'
            },
            {
                id: 'rule_4',
                condition: 'success_pattern_detected',
                action: 'reinforce_behavior',
                weight: 0.85,
                description: 'Reinforce behaviors that lead to success'
            },
            {
                id: 'rule_5',
                condition: 'failure_pattern_detected',
                action: 'adapt_behavior',
                weight: 0.8,
                description: 'Adapt behavior when failure patterns detected'
            }
        ];

        for (const rule of defaultRules) {
            await this.memoryManager.storeProceduralRule(rule);
        }

        console.log('✅ Default behavioral rules initialized');
    }

    /**
     * Start the autonomous cognition loop
     */
    startCognitionLoop() {
        if (this.cognitionLoopRunning) return;

        this.cognitionLoopRunning = true;
        this.isRunning = true;

        console.log('🔄 Starting AI Agent cognition loop...');

        // Run cognition cycle
        this.cognitionLoopInterval = setInterval(async () => {
            try {
                await this.cognitionCycle();
            } catch (error) {
                console.error('Error in cognition cycle:', error);
            }
        }, this.decisionIntervalMs);
    }

    /**
     * Stop the cognition loop
     */
    stopCognitionLoop() {
        if (this.cognitionLoopInterval) {
            clearInterval(this.cognitionLoopInterval);
            this.cognitionLoopRunning = false;
            this.isRunning = false;
            console.log('⏸️  Cognition loop stopped');
        }
    }

    /**
     * Main cognition cycle: THINK → ACT → REFLECT → EVOLVE
     */
    async cognitionCycle() {
        const cycleStartTime = Date.now();

        try {
            // Phase 1: SENSE (Gather context)
            const context = await this.sense();
            if (!context) return; // Nothing to do

            // Phase 2: THINK (Reasoning)
            const decision = await this.think(context);
            if (!decision) return; // No viable decision

            // Phase 3: ACT (Execution)
            const result = await this.act(decision, context);

            // Phase 4: REFLECT (Analysis)
            await this.reflect(decision, result, context);

            // Phase 5: EVOLVE (Self-modification)
            await this.evolve(decision, result, context);

            // Update metrics
            this.lastThought = {
                decision: decision.action,
                timestamp: new Date(),
                duration: Date.now() - cycleStartTime,
                success: result.success
            };

            this.nextDecisionTime = Date.now() + this.decisionIntervalMs;

            // Broadcast update
            if (global.broadcastUpdate) {
                global.broadcastUpdate({
                    type: 'agent_decision',
                    decision: decision.action,
                    success: result.success,
                    timestamp: new Date()
                });
            }

        } catch (error) {
            console.error('Cognition cycle error:', error);
        }
    }

    /**
     * Phase 1: SENSE - Gather context and detect triggers
     */
    async sense() {
        const context = {
            timestamp: new Date(),
            systemTime: new Date().getHours(),
            memoryStats: await this.memoryManager.getMemoryStats(),
            recentDecisions: await this.database.getRecentDecisions(3),
            pendingTasks: await this.database.getTasks('pending'),
            autonomyLevel: this.autonomyLevel
        };

        // Check if there's anything to decide about
        if (context.pendingTasks.length === 0 && Math.random() > 0.3) {
            return null; // Nothing interesting to do
        }

        return context;
    }

    /**
     * Phase 2: THINK - Generate and score decision options
     */
    async think(context) {
        // Get applicable behavioral rules
        const rules = await this.memoryManager.getProceduralRules();

        // Generate options based on context and rules
        const options = [];

        // Option 1: Execute a pending task
        if (context.pendingTasks.length > 0) {
            const task = context.pendingTasks[0];
            options.push({
                action: 'execute_task',
                target: task.id,
                reasoning: `Execute task "${task.title}" with priority ${task.priority}`,
                expectedReward: this.scoreOption('task_execution', context, rules),
                confidence: 0.85
            });
        }

        // Option 2: Consolidate memory if needed
        if (context.memoryStats.utilizationPercent > 70) {
            options.push({
                action: 'consolidate_memory',
                reasoning: 'Memory utilization at ' + context.memoryStats.utilizationPercent + '%',
                expectedReward: this.scoreOption('memory_consolidation', context, rules),
                confidence: 0.9
            });
        }

        // Option 3: Trigger reflection
        options.push({
            action: 'trigger_reflection',
            reasoning: 'Analyze recent decisions for learning',
            expectedReward: this.scoreOption('reflection', context, rules),
            confidence: 0.7
        });

        // Option 4: Update behavioral rules based on recent outcomes
        if (context.recentDecisions.length >= 2) {
            const successRate = context.recentDecisions.filter(d => d.success).length / context.recentDecisions.length;
            if (successRate < 0.6) {
                options.push({
                    action: 'adapt_behavior',
                    reasoning: `Low success rate (${(successRate * 100).toFixed(1)}%) - adapt rules`,
                    expectedReward: this.scoreOption('behavior_adaptation', context, rules),
                    confidence: 0.75
                });
            }
        }

        // If no options generated, create a default exploration action
        if (options.length === 0) {
            options.push({
                action: 'explore_patterns',
                reasoning: 'Routine system exploration',
                expectedReward: 0.5,
                confidence: 0.6
            });
        }

        // Score and select best option
        const selectedOption = options.reduce((best, current) => 
            current.expectedReward > best.expectedReward ? current : best
        );

        return selectedOption;
    }

    /**
     * Score a decision option based on rules and history
     */
    scoreOption(optionType, context, rules) {
        let score = 0.5; // Base score

        // Apply rule weights
        const matchingRules = rules.filter(r => r.action === optionType);
        if (matchingRules.length > 0) {
            score += matchingRules[0].weight * 0.3;
        }

        // Boost based on success history
        if (context.recentDecisions.length > 0) {
            const similarDecisions = context.recentDecisions.filter(d => d.action === optionType);
            if (similarDecisions.length > 0) {
                const successRate = similarDecisions.filter(d => d.success).length / similarDecisions.length;
                score += successRate * 0.2;
            }
        }

        // Add random variance
        score += (Math.random() - 0.5) * 0.1;

        return Math.max(0, Math.min(1, score)); // Clamp to [0, 1]
    }

    /**
     * Phase 3: ACT - Execute the decision
     */
    async act(decision, context) {
        // Check autonomy gate
        if (Math.random() * 100 > this.autonomyLevel) {
            return { success: false, reason: 'Autonomy gate blocked action' };
        }

        try {
            const actionResult = {
                action: decision.action,
                timestamp: new Date(),
                success: false,
                details: {}
            };

            switch (decision.action) {
                case 'execute_task':
                    await this.database.updateTaskStatus(decision.target, 'IN_PROGRESS');
                    // Simulate task execution
                    await new Promise(resolve => setTimeout(resolve, Math.random() * 1000));
                    await this.database.updateTaskStatus(decision.target, 'COMPLETED');
                    actionResult.success = true;
                    actionResult.details = { taskId: decision.target, status: 'COMPLETED' };
                    this.successfulDecisions++;
                    break;

                case 'consolidate_memory':
                    await this.memoryManager.consolidateMemory();
                    actionResult.success = true;
                    actionResult.details = { memoryConsolidated: true };
                    break;

                case 'trigger_reflection':
                    const reflection = await this.performReflection(context);
                    actionResult.success = true;
                    actionResult.details = reflection;
                    break;

                case 'adapt_behavior':
                    await this.adaptBehavior(context);
                    actionResult.success = true;
                    actionResult.details = { behaviorAdapted: true };
                    break;

                case 'explore_patterns':
                    const patterns = await this.analyzePatterns(context);
                    actionResult.success = true;
                    actionResult.details = patterns;
                    break;

                default:
                    actionResult.success = true;
            }

            this.decisionsToday++;
            return actionResult;

        } catch (error) {
            console.error('Action execution error:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Phase 4: REFLECT - Analyze outcomes and generate learning signals
     */
    async reflect(decision, result, context) {
        try {
            const reflection = {
                decisionId: `decision_${Date.now()}`,
                decision: decision.action,
                outcome: result.success ? 'SUCCESS' : 'FAILURE',
                timestamp: new Date(),
                analysis: {
                    wasAutonomous: true,
                    autonomyLevel: this.autonomyLevel,
                    contextFactors: Object.keys(context),
                    resultDetails: result.details
                }
            };

            // Store reflection
            await this.database.storeReflection(reflection);

            // Identify improvement opportunities
            if (!result.success) {
                reflection.analysis.improvementOpportunities = [
                    'Lower autonomy level for risky actions',
                    'Increase reflection frequency',
                    'Review decision rules'
                ];
            } else {
                reflection.analysis.successFactors = [
                    'Decision aligned with context',
                    'Autonomy level appropriate',
                    'Action executed efficiently'
                ];
            }

            return reflection;

        } catch (error) {
            console.error('Reflection error:', error);
        }
    }

    /**
     * Phase 5: EVOLVE - Self-modify behavior based on outcomes
     */
    async evolve(decision, result, context) {
        try {
            // Update rule weights based on success/failure
            const rules = await this.memoryManager.getProceduralRules();

            for (const rule of rules) {
                if (rule.action === decision.action) {
                    // Adjust weight based on result
                    if (result.success) {
                        rule.weight = Math.min(1.0, rule.weight + 0.05);
                    } else {
                        rule.weight = Math.max(0.0, rule.weight - 0.05);
                    }

                    await this.memoryManager.updateProceduralRule(rule);
                }
            }

            // Store evolution event
            await this.database.storeEvolutionEvent({
                timestamp: new Date(),
                type: 'rule_weight_adjustment',
                decision: decision.action,
                success: result.success,
                rulesAffected: rules.length
            });

        } catch (error) {
            console.error('Evolution error:', error);
        }
    }

    /**
     * Perform deep reflection on recent decisions
     */
    async performReflection(context) {
        const recentEpisodes = await this.memoryManager.getRecentEpisodes(5);
        
        const analysis = {
            totalEpisodes: recentEpisodes.length,
            successRate: recentEpisodes.filter(e => e.success).length / recentEpisodes.length,
            commonPatterns: [],
            recommendations: []
        };

        // Identify common patterns
        const actionCounts = {};
        recentEpisodes.forEach(e => {
            actionCounts[e.action] = (actionCounts[e.action] || 0) + 1;
        });

        analysis.commonPatterns = Object.entries(actionCounts)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 3);

        // Generate recommendations
        if (analysis.successRate > 0.8) {
            analysis.recommendations.push('Current strategy is working well, continue');
        } else if (analysis.successRate < 0.5) {
            analysis.recommendations.push('Success rate low, consider strategy change');
        }

        return analysis;
    }

    /**
     * Adapt behavior based on recent outcomes
     */
    async adaptBehavior(context) {
        const recentDecisions = await this.database.getRecentDecisions(10);
        const successRate = recentDecisions.filter(d => d.success).length / recentDecisions.length;

        if (successRate < 0.5) {
            // Reduce autonomy level
            this.autonomyLevel = Math.max(30, this.autonomyLevel - 10);
            console.log(`📉 Reducing autonomy level to ${this.autonomyLevel}%`);
        } else if (successRate > 0.8) {
            // Increase autonomy level
            this.autonomyLevel = Math.min(95, this.autonomyLevel + 5);
            console.log(`📈 Increasing autonomy level to ${this.autonomyLevel}%`);
        }

        await this.database.updateSetting('autonomy_level', this.autonomyLevel);
    }

    /**
     * Analyze patterns in system behavior
     */
    async analyzePatterns(context) {
        const allDecisions = await this.database.getRecentDecisions(50);
        
        const patterns = {
            totalDecisions: allDecisions.length,
            successRate: (allDecisions.filter(d => d.success).length / allDecisions.length * 100).toFixed(1) + '%',
            mostCommonAction: {},
            timeOfDayTrend: {}
        };

        // Most common action
        const actionCounts = {};
        allDecisions.forEach(d => {
            actionCounts[d.action] = (actionCounts[d.action] || 0) + 1;
        });
        patterns.mostCommonAction = Object.entries(actionCounts).sort((a, b) => b[1] - a[1])[0];

        return patterns;
    }

    /**
     * Trigger reflection externally
     */
    async triggerReflection() {
        const context = await this.sense();
        if (!context) return null;

        const reflection = await this.performReflection(context);
        return reflection;
    }

    /**
     * Toggle autonomy on/off
     */
    toggleAutonomy(enabled) {
        this.isRunning = enabled;
        if (enabled) {
            this.startCognitionLoop();
        } else {
            this.stopCognitionLoop();
        }
        console.log(`🔄 Autonomy toggled: ${enabled ? 'ON' : 'OFF'}`);
    }

    /**
     * Set autonomy level (0-100)
     */
    setAutonomyLevel(level) {
        this.autonomyLevel = Math.max(0, Math.min(100, level));
        console.log(`⚙️  Autonomy level set to ${this.autonomyLevel}%`);
    }

    /**
     * Get next decision time
     */
    getNextDecisionTime() {
        const msUntilNext = Math.max(0, this.nextDecisionTime - Date.now());
        const secondsUntilNext = (msUntilNext / 1000).toFixed(1);
        return `${secondsUntilNext}s`;
    }
}

module.exports = AIAgent;
