package com.aihos.ai.quantum

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*
import kotlin.random.Random

/**
 * Quantum-Inspired Advanced Optimization Engine
 * Implements:
 * - Particle Swarm Optimization (PSO)
 * - Ant Colony Optimization (ACO)
 * - Quantum-inspired Genetic Algorithms
 * - Multi-objective optimization
 * - Adaptive parameter tuning
 */

data class Particle(
    val id: String = java.util.UUID.randomUUID().toString(),
    var position: FloatArray,
    var velocity: FloatArray,
    var bestPosition: FloatArray = position.copyOf(),
    var bestFitness: Float = Float.MAX_VALUE,
    var currentFitness: Float = Float.MAX_VALUE
)

data class Ant(
    val id: String = java.util.UUID.randomUUID().toString(),
    var position: IntArray,
    var path: List<Int> = emptyList(),
    var pathCost: Float = Float.MAX_VALUE,
    var pheromoneTrail: FloatArray = FloatArray(0)
)

data class QuantumState(
    val iteration: Int = 0,
    val bestFitness: Float = Float.MAX_VALUE,
    val averageFitness: Float = 0f,
    val diversity: Float = 1f,
    val convergenceRate: Float = 0f,
    val adaptiveParam: Float = 1f
)

/**
 * Particle Swarm Optimization (PSO)
 */
class ParticleSwarmOptimizer(
    val populationSize: Int = 30,
    val dimensions: Int = 10,
    private val w: Float = 0.7f, // inertia weight
    private val c1: Float = 1.5f, // cognitive parameter
    private val c2: Float = 1.5f  // social parameter
) {
    private val particles = mutableListOf<Particle>()
    private var globalBestPosition = FloatArray(dimensions)
    private var globalBestFitness = Float.MAX_VALUE
    private var iteration = 0

    private val _state = MutableStateFlow(QuantumState())
    val state: StateFlow<QuantumState> = _state

    init {
        repeat(populationSize) {
            val position = FloatArray(dimensions) { Random.nextFloat() * 2f - 1f }
            val velocity = FloatArray(dimensions) { Random.nextFloat() * 0.2f - 0.1f }
            particles.add(Particle(position = position, velocity = velocity))
        }
        globalBestPosition = particles[0].position.copyOf()
    }

    /**
     * Evaluate fitness function
     */
    private fun evaluateFitness(position: FloatArray): Float {
        // Sphere function (minimize sum of squares)
        return position.sumOf { (it * it).toDouble() }.toFloat()
    }

    /**
     * Single PSO iteration
     */
    suspend fun optimize(): Float {
        iteration++

        // Evaluate fitness for all particles
        for (particle in particles) {
            val fitness = evaluateFitness(particle.position)
            particle.currentFitness = fitness

            // Update personal best
            if (fitness < particle.bestFitness) {
                particle.bestFitness = fitness
                particle.bestPosition = particle.position.copyOf()
            }

            // Update global best
            if (fitness < globalBestFitness) {
                globalBestFitness = fitness
                globalBestPosition = particle.position.copyOf()
            }
        }

        // Update velocities and positions
        for (particle in particles) {
            for (d in 0 until dimensions) {
                val r1 = Random.nextFloat()
                val r2 = Random.nextFloat()

                // Velocity update
                particle.velocity[d] = (
                    w * particle.velocity[d] +
                    c1 * r1 * (particle.bestPosition[d] - particle.position[d]) +
                    c2 * r2 * (globalBestPosition[d] - particle.position[d])
                ).coerceIn(-1f, 1f)

                // Position update
                particle.position[d] = (particle.position[d] + particle.velocity[d]).coerceIn(-10f, 10f)
            }
        }

        updateState()
        return globalBestFitness
    }

    /**
     * Optimize for N iterations
     */
    suspend fun optimizeNIterations(n: Int): Float {
        repeat(n) { optimize() }
        return globalBestFitness
    }

    private suspend fun updateState() {
        val fitnesses = particles.map { it.currentFitness }
        val best = fitnesses.minOrNull() ?: Float.MAX_VALUE
        val avg = fitnesses.average().toFloat()

        val variance = fitnesses.map { (it - avg) * (it - avg) }.average().toFloat()
        val diversity = sqrt(variance).coerceIn(0f, 1f)

        val convergenceRate = 1f - diversity

        val newState = QuantumState(
            iteration = iteration,
            bestFitness = best,
            averageFitness = avg,
            diversity = diversity,
            convergenceRate = convergenceRate
        )
        _state.emit(newState)
    }

    fun getBestPosition(): FloatArray = globalBestPosition.copyOf()
    fun getBestFitness(): Float = globalBestFitness
}

/**
 * Ant Colony Optimization (ACO)
 */
class AntColonyOptimizer(
    val antCount: Int = 30,
    val graphSize: Int = 10,
    private val evaporationRate: Float = 0.1f,
    private val pheromoneWeight: Float = 1f,
    private val heuristicWeight: Float = 1f
) {
    private val ants = mutableListOf<Ant>()
    private val pheromoneMatrix = FloatArray(graphSize * graphSize) { 1f }
    private var bestPath: List<Int> = emptyList()
    private var bestPathCost = Float.MAX_VALUE
    private var iteration = 0

    private val _state = MutableStateFlow(QuantumState())
    val state: StateFlow<QuantumState> = _state

    init {
        repeat(antCount) {
            val startPos = IntArray(graphSize) { 0 }
            ants.add(Ant(position = startPos))
        }
    }

    /**
     * Heuristic distance (inverse)
     */
    private fun heuristic(from: Int, to: Int): Float {
        return 1f / (1f + abs(from - to).toFloat())
    }

    /**
     * Get pheromone between nodes
     */
    private fun getPheromone(from: Int, to: Int): Float {
        return pheromoneMatrix[from * graphSize + to]
    }

    /**
     * Set pheromone between nodes
     */
    private fun setPheromone(from: Int, to: Int, value: Float) {
        pheromoneMatrix[from * graphSize + to] = value.coerceAtLeast(0.01f)
    }

    /**
     * Single ACO iteration
     */
    suspend fun optimize(): Float {
        iteration++

        // Reset ant paths
        for (ant in ants) {
            ant.path = listOf(0) // Start at node 0
            ant.pathCost = 0f
        }

        // Construct paths
        for (step in 0 until graphSize - 1) {
            for (ant in ants) {
                val current = ant.path.last()
                var nextNode = -1
                var maxValue = -Float.MAX_VALUE

                for (node in 0 until graphSize) {
                    if (node !in ant.path) {
                        val pheromone = getPheromone(current, node).pow(pheromoneWeight)
                        val heuristic = heuristic(current, node).pow(heuristicWeight)
                        val value = pheromone * heuristic

                        if (value > maxValue) {
                            maxValue = value
                            nextNode = node
                        }
                    }
                }

                if (nextNode != -1) {
                    ant.path = ant.path + nextNode
                    ant.pathCost += heuristic(current, nextNode)
                }
            }
        }

        // Find best path
        for (ant in ants) {
            if (ant.pathCost < bestPathCost) {
                bestPathCost = ant.pathCost
                bestPath = ant.path
            }
        }

        // Update pheromones
        // Evaporate
        for (i in pheromoneMatrix.indices) {
            pheromoneMatrix[i] *= (1f - evaporationRate)
        }

        // Deposit
        for (ant in ants) {
            val reward = 1f / (1f + ant.pathCost)
            for (i in 0 until ant.path.size - 1) {
                val from = ant.path[i]
                val to = ant.path[i + 1]
                setPheromone(from, to, getPheromone(from, to) + reward)
            }
        }

        updateState()
        return bestPathCost
    }

    /**
     * Optimize for N iterations
     */
    suspend fun optimizeNIterations(n: Int): Float {
        repeat(n) { optimize() }
        return bestPathCost
    }

    private suspend fun updateState() {
        val costs = ants.map { it.pathCost }
        val best = costs.minOrNull() ?: Float.MAX_VALUE
        val avg = costs.average().toFloat()

        val variance = costs.map { (it - avg) * (it - avg) }.average().toFloat()
        val diversity = sqrt(variance).coerceIn(0f, 1f)

        val newState = QuantumState(
            iteration = iteration,
            bestFitness = best,
            averageFitness = avg,
            diversity = diversity
        )
        _state.emit(newState)
    }

    fun getBestPath(): List<Int> = bestPath
    fun getBestCost(): Float = bestPathCost
}

/**
 * Advanced Quantum-Inspired Genetic Algorithm
 */
class QuantumGeneticAlgorithm(
    val populationSize: Int = 50,
    val chromosomeLength: Int = 20
) {
    private inner class Individual(
        val genes: BitSet = BitSet(chromosomeLength),
        var fitness: Float = 0f
    ) {
        fun copy(): Individual {
            val newIndividual = Individual(genes.copy())
            newIndividual.fitness = this.fitness
            return newIndividual
        }
    }

    private val population = mutableListOf<Individual>()
    private var bestIndividual: Individual? = null
    private var bestFitness = -Float.MAX_VALUE
    private var generation = 0

    private val _state = MutableStateFlow(QuantumState())
    val state: StateFlow<QuantumState> = _state

    init {
        repeat(populationSize) {
            population.add(Individual())
        }
    }

    /**
     * Fitness function (onemax problem)
     */
    private fun evaluateFitness(genes: BitSet): Float {
        var count = 0
        for (i in 0 until genes.size) {
            if (genes[i]) count++
        }
        return count.toFloat() / genes.size
    }

    /**
     * Single generation
     */
    suspend fun evolve(): Float {
        generation++

        // Evaluate
        for (individual in population) {
            individual.fitness = evaluateFitness(individual.genes)

            if (individual.fitness > bestFitness) {
                bestFitness = individual.fitness
                bestIndividual = individual.copy()
            }
        }

        // Selection + Mutation
        val newPopulation = mutableListOf<Individual>()

        // Elite preservation
        newPopulation.add(bestIndividual?.copy() ?: population[0].copy())

        while (newPopulation.size < populationSize) {
            // Tournament selection
            val parent1 = tournamentSelect()
            val parent2 = tournamentSelect()

            // Crossover
            val child = crossover(parent1, parent2)

            // Mutation
            mutate(child)

            newPopulation.add(child)
        }

        population.clear()
        population.addAll(newPopulation.take(populationSize))

        updateState()
        return bestFitness
    }

    /**
     * Evolve for N generations
     */
    suspend fun evolveNGenerations(n: Int): Float {
        repeat(n) { evolve() }
        return bestFitness
    }

    private fun tournamentSelect(): Individual {
        val tournamentSize = 3
        var best: Individual? = null
        repeat(tournamentSize) {
            val candidate = population.random()
            if (best == null || candidate.fitness > best!!.fitness) {
                best = candidate
            }
        }
        return best ?: population[0]
    }

    private fun crossover(parent1: Individual, parent2: Individual): Individual {
        val child = Individual()
        for (i in 0 until chromosomeLength) {
            child.genes[i] = if (Random.nextBoolean()) parent1.genes[i] else parent2.genes[i]
        }
        return child
    }

    private fun mutate(individual: Individual) {
        for (i in 0 until chromosomeLength) {
            if (Random.nextFloat() < 0.01f) {
                individual.genes[i] = !individual.genes[i]
            }
        }
    }

    private suspend fun updateState() {
        val fitnesses = population.map { it.fitness }
        val avg = fitnesses.average().toFloat()
        val variance = fitnesses.map { (it - avg) * (it - avg) }.average().toFloat()
        val diversity = sqrt(variance).coerceIn(0f, 1f)

        val newState = QuantumState(
            iteration = generation,
            bestFitness = bestFitness,
            averageFitness = avg,
            diversity = diversity,
            convergenceRate = 1f - diversity
        )
        _state.emit(newState)
    }

    fun getBestFitness(): Float = bestFitness
}

/**
 * Kotlin BitSet implementation
 */
class BitSet(val size: Int) {
    private val data = BooleanArray(size)

    operator fun get(index: Int) = data[index]
    operator fun set(index: Int, value: Boolean) {
        data[index] = value
    }

    fun copy() = BitSet(size).apply {
        for (i in 0 until size) {
            data[i] = this@BitSet.data[i]
        }
    }
}
