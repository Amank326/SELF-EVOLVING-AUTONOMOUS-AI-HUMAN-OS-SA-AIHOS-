package com.aihos.ai.evolution

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random
import java.time.Instant

/**
 * Evolution Engine - Self-improving AI system
 * Implements:
 * - Genetic algorithms for optimization
 * - Mutation and adaptation
 * - Performance tracking and evolution
 * - Learning from history
 * - Skill development
 */

data class Gene(
    val id: String = java.util.UUID.randomUUID().toString(),
    val parameter: String,
    val value: Float = Random.nextFloat(),
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val mutationRate: Float = 0.1f
)

data class Phenotype(
    val id: String = java.util.UUID.randomUUID().toString(),
    val genes: List<Gene> = emptyList(),
    val fitness: Float = 0.5f,
    val generation: Int = 0,
    val age: Long = Instant.now().toEpochMilli()
)

data class Skill(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val proficiency: Float = 0.0f, // 0.0 to 1.0
    val experience: Int = 0,
    val lastPracticed: Long = Instant.now().toEpochMilli(),
    val learningRate: Float = 0.1f
)

data class EvolutionMetrics(
    val generation: Int = 0,
    val bestFitness: Float = 0f,
    val averageFitness: Float = 0f,
    val diversity: Float = 0f,
    val evolutionRate: Float = 0f
)

/**
 * Evolution Engine - Drives continuous self-improvement
 */
class EvolutionEngine {
    private val _currentPhenotype = MutableStateFlow<Phenotype?>(null)
    val currentPhenotype: StateFlow<Phenotype?> = _currentPhenotype
    
    private val _population = MutableStateFlow<List<Phenotype>>(emptyList())
    val population: StateFlow<List<Phenotype>> = _population
    
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills
    
    private val _evolutionMetrics = MutableStateFlow(EvolutionMetrics())
    val evolutionMetrics: StateFlow<EvolutionMetrics> = _evolutionMetrics
    
    private val _improvementRate = MutableStateFlow(0f)
    val improvementRate: StateFlow<Float> = _improvementRate
    
    private var generation = 0
    private val populationSize = 20
    private val eliteSize = 4

    /**
     * Initialize population with random phenotypes
     */
    suspend fun initializePopulation() {
        val newPopulation = mutableListOf<Phenotype>()
        repeat(populationSize) {
            val genes = listOf(
                Gene(parameter = "creativity", value = Random.nextFloat(), minValue = 0f, maxValue = 1f, mutationRate = 0.05f),
                Gene(parameter = "logic", value = Random.nextFloat(), minValue = 0f, maxValue = 1f, mutationRate = 0.05f),
                Gene(parameter = "intuition", value = Random.nextFloat(), minValue = 0f, maxValue = 1f, mutationRate = 0.05f),
                Gene(parameter = "adaptability", value = Random.nextFloat(), minValue = 0f, maxValue = 1f, mutationRate = 0.05f),
                Gene(parameter = "curiosity", value = Random.nextFloat(), minValue = 0f, maxValue = 1f, mutationRate = 0.05f)
            )
            val phenotype = Phenotype(genes = genes, generation = generation)
            newPopulation.add(phenotype)
        }
        _population.emit(newPopulation)
        _currentPhenotype.emit(newPopulation.first())
    }

    /**
     * Evaluate fitness of phenotypes
     */
    suspend fun evaluateFitness(phenotypes: List<Phenotype>): List<Phenotype> {
        val evaluated = phenotypes.map { phenotype ->
            // Fitness = balance of all traits
            val geneSum = phenotype.genes.sumOf { it.value.toDouble() }
            val variance = phenotype.genes.map { it.value }.let { genes ->
                val mean = genes.average()
                genes.map { (it - mean) * (it - mean) }.average()
            }
            
            // Fitness rewards balanced traits (low variance = specialization, high variance = generalization)
            val balanceFitness = (1f - variance.toFloat()) * 0.5f // 50% for balance
            val strengthFitness = (geneSum / phenotype.genes.size).toFloat() * 0.5f // 50% for strength
            val fitness = (balanceFitness + strengthFitness).coerceIn(0f, 1f)
            
            phenotype.copy(fitness = fitness)
        }
        
        return evaluated.sortedByDescending { it.fitness }
    }

    /**
     * Execute evolutionary cycle
     */
    suspend fun evolve(): Phenotype? {
        val current = _population.value
        if (current.isEmpty()) {
            initializePopulation()
            return _currentPhenotype.value
        }
        
        // Evaluate fitness
        val evaluated = evaluateFitness(current)
        
        // Selection: Keep elite
        val elite = evaluated.take(eliteSize)
        
        // Create offspring through crossover and mutation
        val offspring = mutableListOf<Phenotype>()
        offspring.addAll(elite)
        
        while (offspring.size < populationSize) {
            // Select parents (tournament selection)
            val parent1 = selectParent(evaluated)
            val parent2 = selectParent(evaluated)
            
            // Crossover
            val child = crossover(parent1, parent2)
            
            // Mutation
            val mutated = mutate(child)
            
            offspring.add(mutated.copy(generation = generation))
        }
        
        val newPopulation = offspring.take(populationSize)
        _population.emit(newPopulation)
        
        // Select best as current phenotype
        val best = newPopulation.maxByOrNull { it.fitness }
        if (best != null) {
            _currentPhenotype.emit(best)
        }
        
        // Update metrics
        updateMetrics(evaluated)
        
        generation++
        
        return best
    }

    /**
     * Learn new skill or improve existing
     */
    suspend fun learnSkill(skillName: String): Skill {
        val existing = _skills.value.find { it.name == skillName }
        
        val skill = if (existing != null) {
            // Practice improves proficiency
            val newProficiency = (existing.proficiency + existing.learningRate * 0.1f).coerceIn(0f, 1f)
            existing.copy(
                proficiency = newProficiency,
                experience = existing.experience + 1,
                lastPracticed = Instant.now().toEpochMilli()
            )
        } else {
            // New skill starts at 0
            Skill(name = skillName, proficiency = 0.1f, experience = 1)
        }
        
        val current = _skills.value.toMutableList()
        if (existing != null) {
            current[current.indexOf(existing)] = skill
        } else {
            current.add(skill)
        }
        
        _skills.emit(current)
        return skill
    }

    /**
     * Increase skill proficiency
     */
    suspend fun practiceSkill(skillName: String, iterations: Int = 1): Float {
        repeat(iterations) {
            learnSkill(skillName)
        }
        
        return _skills.value.find { it.name == skillName }?.proficiency ?: 0f
    }

    /**
     * Get proficiency in skill
     */
    fun getSkillProficiency(skillName: String): Float {
        return _skills.value.find { it.name == skillName }?.proficiency ?: 0f
    }

    /**
     * Parent selection via tournament
     */
    private fun selectParent(population: List<Phenotype>): Phenotype {
        val tournamentSize = 5
        val tournament = population.shuffled().take(tournamentSize)
        return tournament.maxByOrNull { it.fitness } ?: population.first()
    }

    /**
     * Crossover: Create child from two parents
     */
    private fun crossover(parent1: Phenotype, parent2: Phenotype): Phenotype {
        val childGenes = mutableListOf<Gene>()
        for (i in parent1.genes.indices) {
            val gene1 = parent1.genes[i]
            val gene2 = parent2.genes[i]
            
            // Random blend of parent genes
            val blendValue = (gene1.value + gene2.value) / 2f + Random.nextFloat() * 0.1f - 0.05f
            val childGene = gene1.copy(value = blendValue.coerceIn(gene1.minValue, gene1.maxValue))
            childGenes.add(childGene)
        }
        
        return Phenotype(genes = childGenes)
    }

    /**
     * Mutation: Random variation
     */
    private fun mutate(phenotype: Phenotype): Phenotype {
        val mutatedGenes = phenotype.genes.map { gene ->
            if (Random.nextFloat() < gene.mutationRate) {
                val mutationMagnitude = Random.nextFloat() * 0.2f - 0.1f
                val newValue = (gene.value + mutationMagnitude).coerceIn(gene.minValue, gene.maxValue)
                gene.copy(value = newValue)
            } else {
                gene
            }
        }
        
        return phenotype.copy(genes = mutatedGenes)
    }

    /**
     * Update evolution metrics
     */
    private suspend fun updateMetrics(population: List<Phenotype>) {
        val fitnesses = population.map { it.fitness }
        val bestFitness = fitnesses.maxOrNull() ?: 0f
        val avgFitness = fitnesses.average().toFloat()
        
        // Diversity = variance in fitness
        val mean = fitnesses.average()
        val variance = fitnesses.map { (it - mean) * (it - mean) }.average()
        val diversity = kotlin.math.sqrt(variance).toFloat()
        
        // Evolution rate = improvement per generation
        val prevBest = _evolutionMetrics.value.bestFitness
        val evolutionRate = (bestFitness - prevBest).coerceAtLeast(0f)
        
        val metrics = EvolutionMetrics(
            generation = generation,
            bestFitness = bestFitness,
            averageFitness = avgFitness,
            diversity = diversity,
            evolutionRate = evolutionRate
        )
        
        _evolutionMetrics.emit(metrics)
        _improvementRate.emit(evolutionRate)
    }

    /**
     * Get evolution summary
     */
    fun getEvolutionSummary(): Map<String, Any> {
        val metrics = _evolutionMetrics.value
        return mapOf<String, Any>(
            "generation" to metrics.generation,
            "bestFitness" to metrics.bestFitness,
            "averageFitness" to metrics.averageFitness,
            "diversity" to metrics.diversity,
            "evolutionRate" to metrics.evolutionRate,
            "populationSize" to _population.value.size,
            "skillCount" to _skills.value.size,
            "topSkills" to (_skills.value.sortedByDescending { it.proficiency }.take(5).map { it.name } as Any)
        )
    }

    /**
     * Get all learned skills
     */
    fun getLearnedSkills(): List<Skill> {
        return _skills.value.sortedByDescending { it.proficiency }
    }

    /**
     * Clear evolution state
     */
    suspend fun reset() {
        _population.emit(emptyList())
        _skills.emit(emptyList())
        _currentPhenotype.emit(null)
        generation = 0
    }
}
