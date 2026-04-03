package com.aihos.ai.multiagent.model

import com.aihos.ai.memory.Episode
import com.aihos.ai.reasoning.ReasoningEngine
import com.aihos.rendering.lighting.Vec3
import com.aihos.replay.model.CognitiveSnapshot
import kotlinx.serialization.Serializable
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// PART 1 — AGENT CORE MODEL
// Core data structures representing a single AI agent instance in the 3D space.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents an independent cognitive agent in the multi-agent universe.
 */
data class Agent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AgentRole,
    
    // Spatial in the 3D Universe
    var position: Vec3 = Vec3(),
    var rotation: Vec3 = Vec3(),
    
    // Cognitive execution state
    var state: AgentState = AgentState.IDLE,
    val cognitiveSnapshot: CognitiveSnapshot? = null,
    
    // Visual parameters
    var auraColor: FloatArray = floatArrayOf(0.2f, 0.7f, 1.0f, 1.0f),
    var energyLevel: Float = 1.0f,
    var scale: Float = 1.0f,
    
    // Abstract references to its independent brain layers
    // In actual implementation these would be instantiated per-agent or 
    // managed by a specific DI scope.
    val memoryNamespace: String = "namespace_$id"
)

enum class AgentRole {
    PRIMARY_CORE,       // The main AI-HOS agent 
    EXPLORER,           // Generates new hypotheses
    CRITIC,             // Validates assumptions and reflections
    SYNTHESIZER         // Merges memories and rules
}

enum class AgentState {
    IDLE,
    THINKING,
    COMMUNICATING,
    EVOLVING,
    CONFLICT
}

// ─────────────────────────────────────────────────────────────────────────────
// PART 6 — MULTI-AGENT COGNITIVE GRAPH
// Extensions to combine graphs and show shared/conflicting nodes.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representation of the unified multi-agent graph.
 */
@Serializable
data class GlobalCognitiveGraph(
    val agents: List<AgentSnapshot>,
    val sharedNodes: List<SharedNode>,
    val interAgentEdges: List<InterAgentEdge>
)

@Serializable
data class AgentSnapshot(
    val agentId: String,
    val name: String,
    val position: List<Float>,
    val state: String,
    val nodesCount: Int
)

/**
 * A cognitive node that has been discovered/shared by multiple agents.
 */
@Serializable
data class SharedNode(
    val id: String,
    val globalPosition: List<Float>,
    val concept: String,
    val agreementScore: Float, // 1.0 = total agreement, 0.0 = total conflict
    val contributingAgentIds: List<String>
)

@Serializable
data class InterAgentEdge(
    val id: String,
    val sourceAgentId: String,
    val targetAgentId: String,
    val edgeType: InterAgentInteractionType,
    val strength: Float,
    val activeTimeRemainingMs: Long
)

@Serializable
enum class InterAgentInteractionType {
    KNOWLEDGE_TRANSFER, // Sharing semantic facts/rules
    COGNITIVE_CONFLICT, // Opposing rule weights or confidence
    SYNCHRONIZATION,    // Merging states
    OBSERVATION         // Background monitoring
}
