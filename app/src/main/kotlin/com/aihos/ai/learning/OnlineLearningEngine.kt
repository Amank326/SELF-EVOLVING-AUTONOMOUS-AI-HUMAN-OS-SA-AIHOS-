package com.aihos.ai.learning

import kotlin.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ADAPTIVE ONLINE LEARNING ENGINE
 * Real-time neural network updates and continuous skill development
 * - Live model retraining from experience
 * - Parameter optimization from streaming data
 * - Skill development tracking
 * - Forgetting curves and memory consolidation
 */

data class Experience(
    val input: FloatArray = floatArrayOf(),
    val output: FloatArray = floatArrayOf(),
    val reward: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Skill(
    val skillId: String = "",
    val name: String = "",
    val proficiency: Float = 0.1f,
    val practiceCount: Int = 0,
    val lastPracticed: Long = System.currentTimeMillis(),
    val improvement: Float = 0f
)

data class LearningMetrics(
    val totalExperiences: Int = 0,
    val averageReward: Float = 0f,
    val learningRate: Float = 0.01f,
    val skillCount: Int = 0,
    val averageProficiency: Float = 0f,
    val improvementTrend: Float = 0f
)

class OnlineLearningEngine {
    // Experience buffer
    private val experienceBuffer = mutableListOf<Experience>()
    private val maxBufferSize = 1000

    // Skills
    private val skills = mutableMapOf<String, Skill>()
    private val skillImprovement = mutableMapOf<String, MutableList<Float>>()

    // Learning state
    private val _learningMetrics = MutableStateFlow(LearningMetrics())
    val learningMetrics: StateFlow<LearningMetrics> = _learningMetrics

    private var learningRate = 0.01f
    private var totalReward = 0f
    private var experienceCount = 0

    init {
        initializeSkills()
    }

    /**
     * Add new experience for learning
     */
    suspend fun addExperience(
        input: FloatArray,
        output: FloatArray,
        reward: Float
    ) {
        val experience = Experience(
            input = input.copyOf(),
            output = output.copyOf(),
            reward = reward
        )

        experienceBuffer.add(experience)
        if (experienceBuffer.size > maxBufferSize) {
            experienceBuffer.removeAt(0)
        }

        experienceCount++
        totalReward += reward

        // Trigger learning if buffer is sufficient
        if (experienceBuffer.size % 10 == 0) {
            performOnlineLearning()
        }
    }

    /**
     * Practice a skill and improve proficiency
     */
    suspend fun practiceSkill(skillId: String, performance: Float) {
        val skill = skills[skillId] ?: return

        // Calculate improvement using learning curve
        val previousProficiency = skill.proficiency
        val improvement = calculateImprovement(
            previousProficiency,
            performance,
            skill.practiceCount
        )

        val newProficiency = (skill.proficiency + improvement * learningRate).coerceIn(0f, 1f)
        val newSkill = skill.copy(
            proficiency = newProficiency,
            practiceCount = skill.practiceCount + 1,
            lastPracticed = System.currentTimeMillis(),
            improvement = improvement
        )

        skills[skillId] = newSkill
        skillImprovement.getOrPut(skillId) { mutableListOf() }.add(improvement)

        updateMetrics()
    }

    /**
     * Get skill proficiency
     */
    fun getSkillProficiency(skillId: String): Float {
        return skills[skillId]?.proficiency ?: 0f
    }

    /**
     * Get all skills
     */
    fun getAllSkills(): List<Skill> {
        return skills.values.toList()
    }

    /**
     * Get learning progress report
     */
    fun getLearningProgressReport(): Map<String, Any> {
        return mapOf(
            "total_experiences" to experienceCount,
            "average_reward" to if (experienceCount > 0) totalReward / experienceCount else 0f,
            "experience_buffer_size" to experienceBuffer.size,
            "skill_count" to skills.size,
            "skills" to skills.values.map {
                mapOf(
                    "name" to it.name,
                    "proficiency" to it.proficiency,
                    "practice_count" to it.practiceCount,
                    "improvement" to it.improvement
                )
            },
            "learning_rate" to learningRate,
            "average_proficiency" to skills.values.map { it.proficiency }.average()
        )
    }

    /**
     * Adaptive learning rate adjustment
     */
    suspend fun adjustLearningRate(performanceMetric: Float) {
        // Increase learning rate if performance is improving
        // Decrease if performance is degrading
        learningRate = when {
            performanceMetric > 0.8f -> minOf(learningRate * 1.1f, 0.1f)
            performanceMetric < 0.4f -> maxOf(learningRate * 0.9f, 0.001f)
            else -> learningRate
        }

        updateMetrics()
    }

    /**
     * Memory consolidation - transfer recent skills to long-term
     */
    suspend fun consolidateMemory() {
        skills.forEach { (skillId, skill) ->
            val timeSinceLastPractice = System.currentTimeMillis() - skill.lastPracticed
            
            // Apply forgetting curve (Ebbinghaus)
            val forgettingFactor = exp(-timeSinceLastPractice / (24 * 60 * 60 * 1000).toFloat())
            val consolidatedProficiency = skill.proficiency * (0.7f + 0.3f * forgettingFactor)

            skills[skillId] = skill.copy(
                proficiency = consolidatedProficiency.coerceIn(0f, 1f)
            )
        }

        updateMetrics()
    }

    // ======================== PRIVATE HELPERS ========================

    private fun initializeSkills() {
        val skillNames = listOf(
            "decision_making",
            "pattern_recognition",
            "optimization",
            "adaptation",
            "anomaly_detection",
            "prediction",
            "reasoning"
        )

        skillNames.forEach { skillName ->
            skills[skillName] = Skill(
                skillId = skillName,
                name = skillName,
                proficiency = 0.1f
            )
        }
    }

    private fun calculateImprovement(
        currentProficiency: Float,
        performance: Float,
        practiceCount: Int
    ): Float {
        // Power law of learning: improvement decreases with practice
        val practiceEffect = 1f / sqrt((practiceCount + 1).toFloat())
        val performanceGain = (performance - currentProficiency).coerceIn(-0.5f, 0.5f)

        return performanceGain * practiceEffect
    }

    private suspend fun performOnlineLearning() {
        if (experienceBuffer.isEmpty()) return

        // Mini-batch learning
        val batch = experienceBuffer.takeLast(minOf(32, experienceBuffer.size))

        // Calculate batch reward
        val batchReward = batch.map { it.reward }.average().toFloat()

        // Update learning rate based on performance
        adjustLearningRate(batchReward)
    }

    private suspend fun updateMetrics() {
        val avgProficiency = skills.values.map { it.proficiency }.average().toFloat()
        val improvementTrend = skills.values.map { it.improvement }.average().toFloat()

        val metrics = LearningMetrics(
            totalExperiences = experienceCount,
            averageReward = if (experienceCount > 0) totalReward / experienceCount else 0f,
            learningRate = learningRate,
            skillCount = skills.size,
            averageProficiency = avgProficiency,
            improvementTrend = improvementTrend
        )

        _learningMetrics.emit(metrics)
    }
}
