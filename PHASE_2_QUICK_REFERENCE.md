# Phase 2: Advanced AI Systems - Quick Reference Guide

## 1. Using Advanced Memory Layer

```kotlin
// Inject into your component
@Inject lateinit var advancedMemory: AdvancedMemoryLayer

// Store with semantic embedding
suspend fun storeMemory(fact: String, embedding: FloatArray) {
    val result = advancedMemory.storeWithSemanticVector(
        memory = MemoryItemWithVector(UUID.randomUUID().toString(), fact, embedding),
        existingMemories = allMemories
    )
}

// Search semantically
suspend fun findRelated(query: String, embedding: FloatArray) {
    val results = advancedMemory.semanticSearch(
        embedding = embedding,
        memories = allMemories,
        similarityThreshold = 0.7f,
        limit = 10
    )
    results.forEach { result ->
        println("Found: ${result.memory.content} (similarity: ${result.similarity})")
    }
}

// Consolidate memories
suspend fun consolidate() {
    val clusters = advancedMemory.consolidateMemories(
        memories = allMemories,
        maxClusters = 5
    )
    clusters.forEach { cluster ->
        println("Cluster center: ${cluster.centerVector}")
        println("Contains ${cluster.memberIndices.size} memories")
    }
}

// Apply memory decay
suspend fun decay() {
    val decayed = advancedMemory.applyMemoryDecay(
        memories = allMemories,
        decayRate = 0.01f,
        currentTime = System.currentTimeMillis()
    )
}
```

---

## 2. Using Advanced Reasoning Layer

```kotlin
// Inject into your component
@Inject lateinit var advancedReasoning: AdvancedReasoningLayer

// Bayesian inference
suspend fun inferProbabilities(evidence: Map<String, Boolean>) {
    val posteriors = advancedReasoning.bayesianInference(
        evidence = evidence,
        hypotheses = listOf("hypothesis1", "hypothesis2", "hypothesis3"),
        priorProbabilities = mapOf(
            "hypothesis1" to 0.5f,
            "hypothesis2" to 0.3f,
            "hypothesis3" to 0.2f
        ),
        conditionalProbabilities = { fact, hypothesis, value ->
            // P(fact | hypothesis)
            0.8f
        }
    )
    posteriors.forEach { (hypothesis, probability) ->
        println("$hypothesis: $probability")
    }
}

// Constraint satisfaction
data class MyConstraint(val type: String, val value: String) : Constraint
suspend fun solveConstraints() {
    val solution = advancedReasoning.solveConstraintProblem(
        constraints = listOf(
            MyConstraint("time", "morning"),
            MyConstraint("energy", "high")
        ),
        variables = mapOf("action" to listOf("exercise", "sleep", "work"))
    )
    println("Solution: ${solution.assignmentMap}")
}

// Temporal reasoning
suspend fun reasonOverTime() {
    val temporalResults = advancedReasoning.temporalReasoning(
        constraints = listOf(temporalConstraint1, temporalConstraint2),
        currentTime = System.currentTimeMillis(),
        lookaheadDays = 30
    )
    if (temporalResults.isValid) {
        println("Constraints satisfied over time period")
    }
}
```

---

## 3. Using Advanced Evolution Engine

```kotlin
// Inject into your component
@Inject lateinit var advancedEvolution: AdvancedEvolutionEngine

// Genetic algorithm evolution
suspend fun evolveStrategies() {
    val result = advancedEvolution.geneticAlgorithmEvolution(
        populationSize = 50,
        generations = 20,
        mutationRate = 0.15f,
        fitnessEvaluator = { candidate ->
            // Return fitness score (0-1)
            evaluateStrategy(candidate)
        }
    )
    val bestStrategy = result.bestCandidates.first()
    println("Best fitness: ${result.finalFitnessAverage}")
}

// Reinforcement learning
suspend fun learnFromExperience(state: String, action: String, reward: Float, nextState: String) {
    val update = advancedEvolution.reinforcementLearningStep(
        state = state,
        action = action,
        reward = reward,
        nextState = nextState,
        alpha = 0.1f,
        gamma = 0.95f,
        epsilon = 0.1f
    )
    println("Q-value updated: ${update.newQValue}")
}

// Experience replay
suspend fun replayExperience() {
    val replayResult = advancedEvolution.experienceReplay(
        experiences = storedExperiences,
        batchSize = 32,
        alpha = 0.1f,
        gamma = 0.95f
    )
    println("Learned from ${replayResult.samplesReplayed} experiences")
}

// Hyperparameter optimization
suspend fun optimizeHyperparameters() {
    val optimization = advancedEvolution.optimizeHyperparameters(
        parameterSpace = mapOf(
            "learningRate" to listOf(0.01f, 0.05f, 0.1f),
            "discountFactor" to listOf(0.9f, 0.95f, 0.99f)
        ),
        evaluator = { params ->
            evaluateWithParams(params)
        }
    )
    println("Best params: ${optimization.bestParameters}")
    println("Best score: ${optimization.bestScore}")
}

// Ensemble selection
suspend fun selectBestEnsemble() {
    val ensemble = advancedEvolution.selectBestEnsemble(
        candidates = allCandidates,
        ensemble Size = 5,
        performanceWeights = 0.5f,
        diversityWeight = 0.3f,
        stabilityWeight = 0.2f
    )
    println("Ensemble diversity score: ${ensemble.diversityScore}")
    println("Ensemble stability: ${ensemble.stabilityScore}")
}
```

---

## 4. Using Advanced Reflection Layer

```kotlin
// Inject into your component
@Inject lateinit var advancedReflection: AdvancedReflectionLayer

// Review decision outcomes
suspend fun reviewDecision(decisionId: String, actualOutcome: Float, expectedOutcome: Float) {
    val review = advancedReflection.reviewDecision(
        decisionId = decisionId,
        decision = Decision(decisionId, "action", 0.8f),
        actualOutcome = ActualOutcome(actualOutcome),
        expectedOutcome = ExpectedOutcome(expectedOutcome)
    )
    println("Accuracy: ${review.accuracy}")
    println("Learnings: ${review.learningPoints}")
}

// Identify error patterns
suspend fun findErrorPatterns() {
    val patterns = advancedReflection.identifyErrorPatterns(
        decisions = recentDecisions,
        errorThreshold = 0.6f
    )
    patterns.forEach { pattern ->
        println("Error: ${pattern.errorType}")
        println("Frequency: ${pattern.frequency}")
        println("Suggestion: ${pattern.suggestedFix}")
    }
}

// Counterfactual analysis
suspend fun analyzeAlternatives(decision: Decision) {
    val counterfactual = advancedReflection.counterfactualAnalysis(
        actualDecision = decision,
        alternativeDecisions = listOf(altDecision1, altDecision2),
        actualOutcome = actualOutcome
    )
    println("Regret: ${counterfactual.regret}")
    println("Better alternative: ${counterfactual.betterAlternative}")
}

// Confidence calibration
suspend fun calibrateConfidence() {
    val calibration = advancedReflection.calibrateConfidence(
        predictions = predictions,
        actualOutcomes = outcomes
    )
    println("Calibration score: ${calibration.calibrationScore}")
    println("Overconfident: ${calibration.isOverconfident}")
}

// Meta-cognitive modeling
suspend fun buildKnowledgeModel() {
    val metaCognition = advancedReflection.buildMetaCognition(
        decisionHistory = allDecisions,
        taskTypes = listOf("planning", "execution", "recovery")
    )
    println("Task confidences: ${metaCognition.taskConfidences}")
    println("Learning rate: ${metaCognition.averageLearningRate}")
}
```

---

## 5. Using Advanced Orchestration

```kotlin
// Inject into your component
@Inject lateinit var advancedController: AdvancedAutonomyController

// Execute full advanced cycle
suspend fun runAdvancedCycle(context: String) {
    val cycleResult = advancedController.executeAdvancedCycle(
        cycleId = UUID.randomUUID().toString(),
        context = context,
        memoryQuery = "relevant facts",
        memoryEmbedding = vectorEmbedding,
        evidenceMap = mapOf("condition" to "value"),
        actionOptions = listOf("option1", "option2", "option3")
    )
    
    println("Think Phase Duration: ${cycleResult.thinkPhase.durationMs}ms")
    println("Best Decision: ${cycleResult.actPhase.selectedAction}")
    println("Error Patterns Found: ${cycleResult.reflectPhase.errorPatterns.size}")
    println("Evolution Improved: ${cycleResult.evolvePhase.generationsFailed < 5}")
}

// Analyze system performance
suspend fun analyzePerformance() {
    val analysis = advancedController.analyzeSystemPerformance(
        cycleResults = recentCycleResults
    )
    println("Overall Health: ${analysis.overallHealthStatus}")
    println("Average Cycle Time: ${analysis.averageCycleDurationMs}ms")
    println("Performance Trend: ${analysis.performanceTrend}")
}

// Optimize cycle frequency
suspend fun adaptFrequency() {
    val optimization = advancedController.optimizeCycleFrequency(
        performanceMetrics = currentMetrics,
        resourceConstraints = systemResources
    )
    println("Optimal Frequency: ${optimization.frequencyMs}ms")
    println("Frequency Trend: ${optimization.frequencyTrend}")
}

// Make ensemble decision
suspend fun ensembleDecide(strategies: List<Strategy>) {
    val decision = advancedController.ensembleDecisionMaking(
        strategies = strategies,
        context = decisionContext
    )
    println("Consensus Action: ${decision.consensusAction}")
    println("Confidence: ${decision.ensembleConfidence}")
    println("Reasoning: ${decision.combinedReasoning}")
}
```

---

## 6. Key Data Structures

### Memory
```kotlin
data class MemorySearchResult(
    val memory: MemoryItemWithVector,
    val similarity: Float,
    val relevanceScore: Float
)

data class MemoryCluster(
    val centroid: SemanticVector,
    val memberIndices: List<Int>,
    val density: Float
)
```

### Reasoning
```kotlin
data class ConstraintSolution(
    val satisfies: Boolean,
    val assignmentMap: Map<String, String>
)

data class ReasoningQuality(
    val confidence: Float,
    val uniqueness: Float,
    val consistency: Float,
    val score: Float
)
```

### Evolution
```kotlin
data class StrategyCandidate(
    val genes: List<Float>,
    val fitness: Float = 0f
)

data class EvolutionResult(
    val bestCandidates: List<StrategyCandidate>,
    val fitnessHistory: List<Float>,
    val generation: Int,
    val finalFitnessAverage: Float
)
```

### Reflection
```kotlin
data class DecisionReview(
    val decisionId: String,
    val outcome: ActualOutcome,
    val accuracy: Float,
    val learningPoints: List<String>,
    val errorPattern: ErrorPattern?
)

data class ErrorPattern(
    val errorType: String,
    val frequency: Int,
    val suggestedFix: String
)
```

### Orchestration
```kotlin
data class AdvancedCycleResult(
    val cycleId: String,
    val context: String,
    val thinkPhase: ThinkPhaseResult,
    val actPhase: ActPhaseResult,
    val reflectPhase: ReflectPhaseResult,
    val evolvePhase: EvolvePhaseResult,
    val totalDurationMs: Long
)
```

---

## 7. Common Patterns

### Pattern 1: Full Learning Cycle
```kotlin
// Execute cycle → Review → Learn → Evolve
val cycleResult = advancedController.executeAdvancedCycle(...)

advancedReflection.reviewDecision(
    decisionId = cycleResult.actPhase.decisionId,
    actualOutcome = systemOutcome,
    expectedOutcome = null
)

advancedEvolution.reinforcementLearningStep(
    state = cycleResult.context,
    action = cycleResult.actPhase.selectedAction,
    reward = calculateReward(cycleResult),
    nextState = newContext
)
```

### Pattern 2: Error Recovery
```kotlin
val patterns = advancedReflection.identifyErrorPatterns(decisions)
if (patterns.isNotEmpty()) {
    advancedEvolution.geneticAlgorithmEvolution(
        fitnessEvaluator = { candidate ->
            // Evolve away from error patterns
            evaluateAgainstPatterns(candidate, patterns)
        }
    )
}
```

### Pattern 3: Adaptive Learning
```kotlin
val performance = advancedController.analyzeSystemPerformance(...)
val frequency = advancedController.optimizeCycleFrequency(
    performanceMetrics = performance,
    resourceConstraints = systemResources
)
// Adjust cycle timing based on performance
cycleIntervalMs = frequency.frequencyMs
```

---

## 8. Performance Tips

1. **Memory Search**: Use `similarityThreshold` of 0.7-0.8 for balance
2. **Reasoning**: Limit constraints to <10 for fast solving
3. **Evolution**: Start with 50 population, 10 generations
4. **Reflection**: Review decisions in batches for efficiency
5. **Orchestration**: Run full cycles every 1-5 seconds

---

## 9. Testing

All advanced systems support suspend functions for easy testing:

```kotlin
@Test
fun testMemorySearch() = runTest {
    val results = advancedMemory.semanticSearch(...)
    assert(results.isNotEmpty())
    assert(results[0].similarity > 0.7f)
}
```

---

*Last Updated: 2024*
*Part of SA-AIHOS Phase 2 Advanced AI Systems*
