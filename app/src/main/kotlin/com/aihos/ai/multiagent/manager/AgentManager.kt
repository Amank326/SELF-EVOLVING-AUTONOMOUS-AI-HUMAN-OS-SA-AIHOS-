package com.aihos.ai.multiagent.manager

import com.aihos.ai.multiagent.model.Agent
import com.aihos.ai.multiagent.model.AgentRole
import com.aihos.ai.multiagent.model.AgentState
import com.aihos.rendering.lighting.Vec3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// PART 2 — AGENT MANAGER & PART 3 — SPATIAL DISTRIBUTION
// Lifecycle management, spawning, updates, and spatial math to avoid overlaps.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Orchestrates the lifecycle and positioning of all AI agents in the environment.
 */
class AgentManager(
    private val maxAgents: Int = 8 // Fixed upper limit for performance (Part 7)
) {
    private val _activeAgents = MutableStateFlow<List<Agent>>(emptyList())
    val activeAgents: StateFlow<List<Agent>> = _activeAgents.asStateFlow()

    private var timeAccumulator: Float = 0f

    // Sphere parameters for spatial distribution
    private val orbitRadiusBase = 12.0f
    private val goldenRatio = (1.0f + kotlin.math.sqrt(5.0f)) / 2.0f

    /**
     * Initializes the core system with the primary agent and standard roles.
     */
    fun initializeCoreSystem() {
        spawnAgent("SA-Core", AgentRole.PRIMARY_CORE)
        spawnAgent("Hypothesis-Gen", AgentRole.EXPLORER)
        spawnAgent("Verifier", AgentRole.CRITIC)
        updateSpatialDistribution()
    }

    /**
     * Spawns a new agent if within performance limits.
     */
    fun spawnAgent(name: String, role: AgentRole): Agent? {
        val currentList = _activeAgents.value
        if (currentList.size >= maxAgents) {
            return null
        }

        val baseColor = when (role) {
            AgentRole.PRIMARY_CORE -> floatArrayOf(0.0f, 0.85f, 1.0f, 1.0f) // Cyan
            AgentRole.EXPLORER -> floatArrayOf(1.0f, 0.82f, 0.2f, 1.0f)     // Gold
            AgentRole.CRITIC -> floatArrayOf(1.0f, 0.2f, 0.3f, 1.0f)        // Red
            AgentRole.SYNTHESIZER -> floatArrayOf(0.6f, 0.2f, 1.0f, 1.0f)   // Violet
        }

        val newAgent = Agent(
            name = name,
            type = role,
            auraColor = baseColor,
            scale = if (role == AgentRole.PRIMARY_CORE) 1.5f else 0.8f
        )

        _activeAgents.value = currentList + newAgent
        updateSpatialDistribution()
        
        return newAgent
    }

    /**
     * Terminate and remove an agent.
     */
    fun removeAgent(agentId: String) {
        val agent = _activeAgents.value.find { it.id == agentId }
        if (agent?.type == AgentRole.PRIMARY_CORE) return // Cannot remove core

        _activeAgents.value = _activeAgents.value.filter { it.id != agentId }
        updateSpatialDistribution()
    }

    /**
     * Update called per-frame to animate orbit and handle state changes.
     */
    fun update(deltaTime: Float) {
        timeAccumulator += deltaTime
        
        val agents = _activeAgents.value.toMutableList()
        val numSubAgents = agents.size - 1
        
        var subAgentIndex = 0
        agents.forEach { agent ->
            if (agent.type == AgentRole.PRIMARY_CORE) {
                // Core slightly bobs in the center
                agent.position = Vec3(0f, sin(timeAccumulator * 0.5f) * 0.5f, 0f)
            } else {
                // Background orbit for sub-agents
                val orbitSpeed = 0.2f
                val currentAngle = timeAccumulator * orbitSpeed
                
                // Distribute sub-agents in a ring or sphere around the core using Fibonacci distribution
                val theta = 2.0f * Math.PI.toFloat() * subAgentIndex / goldenRatio
                val phi = kotlin.math.acos(1.0f - 2.0f * (subAgentIndex + 0.5f) / numSubAgents)
                
                // Add animated current angle for orbiting
                val animatedTheta = theta + currentAngle

                val x = orbitRadiusBase * sin(phi) * cos(animatedTheta)
                val y = orbitRadiusBase * cos(phi) * sin(timeAccumulator * 0.3f)
                val z = orbitRadiusBase * sin(phi) * sin(animatedTheta)

                agent.position = Vec3(x, y, z)
                subAgentIndex++
            }
            
            // Randomly update energy levels based on state
            agent.energyLevel = when (agent.state) {
                AgentState.IDLE -> 0.4f + sin(timeAccumulator * 2f) * 0.1f
                AgentState.THINKING -> 0.8f + sin(timeAccumulator * 8f) * 0.2f
                AgentState.COMMUNICATING -> 1.0f
                AgentState.EVOLVING -> 1.2f
                AgentState.CONFLICT -> 0.9f + sin(timeAccumulator * 20f) * 0.4f
            }
        }
        
        _activeAgents.value = agents
    }

    /**
     * Update fixed positions, usually called after spawn/despawn.
     */
    private fun updateSpatialDistribution() {
        // Spatial math implemented smoothly in the update() method via Fibonacci sphere 
        // to naturally avoid overlaps no matter how many agents exist.
    }

    fun updateAgentState(agentId: String, newState: AgentState) {
        val agents = _activeAgents.value.toMutableList()
        val index = agents.indexOfFirst { it.id == agentId }
        if (index != -1) {
            agents[index].state = newState
            _activeAgents.value = agents
        }
    }
}
