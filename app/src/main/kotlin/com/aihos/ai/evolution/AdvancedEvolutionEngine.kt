package com.aihos.ai.evolution

import kotlin.random.Random
import kotlin.math.*

/**
 * Advanced Evolution Engine with Genetic Algorithms
 * Implements evolutionary strategy optimization and self-improvement
 */
interface AdvancedEvolutionEngine : EvolutionEngine {
    
    /**
     * Genetic algorithm for strategy evolution
     */
    suspend fun geneticAlgorithmEvolution(
        populationSize: Int = 50,
        generations: Int = 10,
        mutationRate: Float = 0.15f
    ): EvolutionResult
    
    /**
     * Reinforcement learning integration
     */
    suspend fun reinforcementLearningStep(
        state: String,
        action: String,
        reward: Float,
        nextState: String
    ): QValueUpdate
    
    /**
     * Experience replay for off-policy learning
     */
    suspend fun experienceReplay(
        batchSize: Int = 32
    ): ExperienceReplayResult
    
    /**
     * Hyperparameter optimization
     */
    suspend fun optimizeHyperparameters(
        currentParams: Map<String, Float>,
        performanceHistory: List<Float>
    ): OptimizationResult
    
    /**
     * Model ensemble selection
     */
    suspend fun selectBestEnsemble(
        candidates: List<StrategyCandidate>
    ): StrategyCandidate
}

/**
 * Default implementation of Advanced Evolution Engine
 */
class DefaultAdvancedEvolutionEngine : AdvancedEvolutionEngine {
    
    private val random = Random(System.currentTimeMillis())
    private val experienceReplayBuffer = mutableListOf<Experience>()
    private val qValueTable = mutableMapOf<String, MutableMap<String, Float>>()
    private val strategy = mutableMapOf<String, Float>()
    private var generation = 0
    
    data class Experience(
        val state: String,
        val action: String,
        val reward: Float,
        val nextState: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    override suspend fun evolveRules(rules: List<String>): List<String> {
        val result = geneticAlgorithmEvolution(populationSize = 50, generations = 5)
        return result.bestCandidates.map { it.genes.toString() }
    }
    
    override suspend fun optimizeStrategies(strategies: List<String>): List<String> {
        return strategies.map { strategy ->
            val fitness = evaluateStrategyFitness(strategy)
            if (fitness > 0.7f) strategy else mutateStrategy(strategy)
        }
    }
    
    override suspend fun adaptToEnvironment(feedback: String): String {
        val adaptation = "adapted_${System.currentTimeMillis()}"
        strategy[adaptation] = evaluateStrategySuitability(feedback)
        return adaptation
    }
    
    override suspend fun geneticAlgorithmEvolution(
        populationSize: Int,
        generations: Int,
        mutationRate: Float
    ): EvolutionResult {
        
        // Initialize population
        val population = List(populationSize) {
            createRandomCandidate()
        }
        
        var currentPopulation = population
        val fitnessHistory = mutableListOf<Float>()
        
        repeat(generations) { gen ->
            // Evaluate fitness
            val withFitness = currentPopulation.map { candidate ->
                candidate.copy(fitness = evaluateCandidateFitness(candidate))
            }
            
            fitnessHistory.add(withFitness.maxOf { it.fitness })
            
            // Selection
            val selected = tournamentSelection(withFitness, populationSize / 2)
            
            // Crossover and mutation
            val offspring = mutableListOf<StrategyCandidate>()
            repeat(populationSize / 2) {
                val parent1 = selected[random.nextInt(selected.size)]
                val parent2 = selected[random.nextInt(selected.size)]
                
                var child = crossover(parent1, parent2)
                if (random.nextFloat() < mutationRate) {
                    child = mutateCandidate(child)
                }
                offspring.add(child)
            }
            
            // New population
            currentPopulation = (withFitness.take(populationSize / 2) + offspring)
                .take(populationSize)
                .map { it.copy(generation = gen) }
            
            generation = gen
        }
        
        val finalPopulation = currentPopulation.map { candidate ->
            candidate.copy(fitness = evaluateCandidateFitness(candidate))
        }
        
        return EvolutionResult(
            bestCandidates = finalPopulation.sortedByDescending { it.fitness }.take(5),
            fitnessHistory = fitnessHistory,
            generation = generation,
            averageFitness = finalPopulation.map { it.fitness }.average().toFloat()
        )
    }
    
    override suspend fun reinforcementLearningStep(
        state: String,
        action: String,
        reward: Float,
        nextState: String
    ): QValueUpdate {
        
        // Store experience
        experienceReplayBuffer.add(
            Experience(state, action, reward, nextState)
        )
        
        // Q-learning update
        val stateQValues = qValueTable.getOrPut(state) { mutableMapOf() }
        val nextStateQValues = qValueTable.getOrDefault(nextState, mutableMapOf())
        
        val currentQValue = stateQValues.getOrDefault(action, 0f)
        val maxNextQValue = nextStateQValues.values.maxOrNull() ?: 0f
        
        val alpha = 0.1f // Learning rate
        val gamma = 0.95f // Discount factor
        val newQValue = currentQValue + alpha * (reward + gamma * maxNextQValue - currentQValue)
        
        stateQValues[action] = newQValue
        qValueTable[state] = stateQValues
        
        return QValueUpdate(
            state = state,
            action = action,
            oldValue = currentQValue,
            newValue = newQValue,
            tdError = abs(newQValue - currentQValue)
        )
    }
    
    override suspend fun experienceReplay(batchSize: Int): ExperienceReplayResult {
        if (experienceReplayBuffer.size < batchSize) {
            return ExperienceReplayResult(
                batchSize = 0,
                updateCount = 0,
                averageTDError = 0f
            )
        }
        
        val batch = experienceReplayBuffer
            .shuffled(random)
            .take(batchSize)
        
        var totalTDError = 0f
        batch.forEach { experience ->
            val update = reinforcementLearningStep(
                experience.state,
                experience.action,
                experience.reward,
                experience.nextState
            )
            totalTDError += update.tdError
        }
        
        return ExperienceReplayResult(
            batchSize = batch.size,
            updateCount = batch.size,
            averageTDError = totalTDError / batch.size
        )
    }
    
    override suspend fun optimizeHyperparameters(
        currentParams: Map<String, Float>,
        performanceHistory: List<Float>
    ): OptimizationResult {
        
        if (performanceHistory.size < 2) {
            return OptimizationResult(
                optimizedParams = currentParams,
                improvement = 0f,
                iterationCount = 0
            )
        }
        
        val lastPerformance = performanceHistory.last()
        val previousPerformance = performanceHistory.dropLast(1).average().toFloat()
        val improvement = lastPerformance - previousPerformance
        
        val optimizedParams = currentParams.mapValues { (key, value) ->
            when {
                improvement > 0 && random.nextFloat() < 0.3f -> 
                    value * 1.05f // Slight increase if improving
                improvement < 0 && random.nextFloat() < 0.5f -> 
                    value * 0.95f // Decrease if not improving
                else -> value
            }
        }
        
        return OptimizationResult(
            optimizedParams = optimizedParams,
            improvement = improvement,
            iterationCount = performanceHistory.size
        )
    }
    
    override suspend fun selectBestEnsemble(
        candidates: List<StrategyCandidate>
    ): StrategyCandidate {
        
        val withScores = candidates.map { candidate ->
            val diversityScore = calculateDiversityScore(candidate, candidates)
            val performanceScore = candidate.fitness
            val stabilityScore = calculateStabilityScore(candidate)
            
            val ensembleScore = 
                (performanceScore * 0.5f) + 
                (diversityScore * 0.3f) + 
                (stabilityScore * 0.2f)
            
            Pair(candidate, ensembleScore)
        }
        
        return withScores.maxByOrNull { it.second }?.first ?: candidates.first()
    }
    
    // Helper functions
    
    private fun createRandomCandidate(): StrategyCandidate {
        return StrategyCandidate(
            id = "strategy_${System.currentTimeMillis()}_${random.nextInt(1000)}",
            genes = mutableMapOf(
                "mutation_rate" to random.nextFloat(),
                "crossover_rate" to random.nextFloat(),
                "selection_pressure" to random.nextFloat(),
                "learning_rate" to random.nextFloat(),
                "discount_factor" to random.nextFloat()
            ),
            fitness = 0f,
            generation = 0,
            parentIds = emptyList()
        )
    }
    
    private fun evaluateCandidateFitness(candidate: StrategyCandidate): Float {
        var fitness = 0.5f
        fitness += (candidate.genes["learning_rate"] ?: 0.5f) * 0.2f
        fitness += (candidate.genes["mutation_rate"] ?: 0.5f) * 0.15f
        fitness += (candidate.genes["selection_pressure"] ?: 0.5f) * 0.15f
        return fitness.coerceIn(0f, 1f)
    }
    
    private fun tournamentSelection(
        population: List<StrategyCandidate>,
        selectCount: Int,
        tournamentSize: Int = 3
    ): List<StrategyCandidate> {
        val selected = mutableListOf<StrategyCandidate>()
        repeat(selectCount) {
            val tournament = population.shuffled(random).take(tournamentSize)
            selected.add(tournament.maxByOrNull { it.fitness } ?: population.first())
        }
        return selected
    }
    
    private fun crossover(parent1: StrategyCandidate, parent2: StrategyCandidate): StrategyCandidate {
        val childGenes = mutableMapOf<String, Float>()
        parent1.genes.forEach { (key, value1) ->
            val value2 = parent2.genes[key] ?: value1
            childGenes[key] = if (random.nextBoolean()) value1 else value2
        }
        
        return StrategyCandidate(
            id = "strategy_${System.currentTimeMillis()}",
            genes = childGenes,
            fitness = 0f,
            generation = generation,
            parentIds = listOf(parent1.id, parent2.id)
        )
    }
    
    private fun mutateCandidate(candidate: StrategyCandidate): StrategyCandidate {
        val mutatedGenes = candidate.genes.mapValues { (_, value) ->
            val mutation = random.nextGaussian().toFloat() * 0.1f
            (value + mutation).coerceIn(0f, 1f)
        }
        
        return candidate.copy(genes = mutatedGenes)
    }
    
    private fun evaluateStrategyFitness(strategy: String): Float {
        return (strategy.length.toFloat() / 100).coerceIn(0.1f, 0.9f)
    }
    
    private fun mutateStrategy(strategy: String): String {
        return strategy + "_mutated_${random.nextInt(1000)}"
    }
    
    private fun evaluateStrategySuitability(feedback: String): Float {
        return (feedback.length.toFloat() / 100).coerceIn(0.1f, 0.9f)
    }
    
    private fun calculateDiversityScore(
        candidate: StrategyCandidate,
        all: List<StrategyCandidate>
    ): Float {
        val otherDistances = all.filter { it.id != candidate.id }
            .map { other -> 
                calculateGenomeDistance(candidate.genes, other.genes)
            }
        
        return if (otherDistances.isEmpty()) 0.5f 
        else otherDistances.average().toFloat()
    }
    
    private fun calculateGenomeDistance(
        genes1: Map<String, Float>,
        genes2: Map<String, Float>
    ): Float {
        var distance = 0f
        genes1.forEach { (key, value1) ->
            val value2 = genes2[key] ?: 0.5f
            distance += abs(value1 - value2)
        }
        return distance / genes1.size.coerceAtLeast(1)
    }
    
    private fun calculateStabilityScore(candidate: StrategyCandidate): Float {
        val variance = candidate.genes.values.map { value ->
            (value - candidate.genes.values.average()).pow(2)
        }.average()
        
        return (1f / (1f + variance)).toFloat()
    }
}

/**
 * Data models for advanced evolution
 */

data class StrategyCandidate(
    val id: String,
    val genes: Map<String, Float>,
    val fitness: Float,
    val generation: Int,
    val parentIds: List<String>
)

data class EvolutionResult(
    val bestCandidates: List<StrategyCandidate>,
    val fitnessHistory: List<Float>,
    val generation: Int,
    val averageFitness: Float
)

data class QValueUpdate(
    val state: String,
    val action: String,
    val oldValue: Float,
    val newValue: Float,
    val tdError: Float
)

data class ExperienceReplayResult(
    val batchSize: Int,
    val updateCount: Int,
    val averageTDError: Float
)

data class OptimizationResult(
    val optimizedParams: Map<String, Float>,
    val improvement: Float,
    val iterationCount: Int
)
