package com.aihos.replay.model

import androidx.room.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// PART 1 — EVENT RECORDING DATA MODELS
// Core data structures for recording AI decision cycles and enabling
// full cognitive replay with time-travel debugging.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Top-level replay event — a single point-in-time snapshot of the AI system.
 * 
 * This is the fundamental unit of the replay timeline. Each event captures
 * the full cognitive state at the moment a decision was made or a rule changed.
 */
@Serializable
data class ReplayEvent(
    val id: String = UUID.randomUUID().toString(),
    val sequenceNumber: Long,                    // Monotonic ordering within session
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,                       // Groups events per session
    val eventType: ReplayEventType,
    val cognitiveSnapshot: CognitiveSnapshot,
    val decisionSnapshot: DecisionSnapshot?,     // null for non-decision events
    val ruleUpdates: List<RuleUpdate> = emptyList(),
    val memoryChanges: List<MemoryChange> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    /** Approximate byte size for memory budget tracking. */
    val estimatedSizeBytes: Int
        get() = 256 + // fixed overhead
                (cognitiveSnapshot.nodeStates.size * 64) +
                (cognitiveSnapshot.edgeStates.size * 48) +
                (decisionSnapshot?.reasoningScores?.size ?: 0) * 32 +
                ruleUpdates.size * 128 +
                memoryChanges.size * 96
}

/**
 * Classification of replay events for filtering and timeline visualization.
 */
@Serializable
enum class ReplayEventType {
    DECISION_CYCLE,      // Full THINK → ACT cycle
    REFLECTION,          // Post-decision analysis
    EVOLUTION,           // Rule weight/structure change
    MEMORY_STORE,        // New memory created
    MEMORY_RECALL,       // Memory accessed/used
    COGNITIVE_SPIKE,     // Burst of activity (multiple decisions)
    SYSTEM_STATE_CHANGE, // Autonomy level or config change
    ANOMALY              // Unexpected behavior detected
}

/**
 * Full snapshot of the cognitive graph at a point in time.
 * 
 * Captures every node and edge in the neural lattice so the renderer
 * can reconstruct the exact visual state during replay.
 */
@Serializable
data class CognitiveSnapshot(
    val timestamp: Long = System.currentTimeMillis(),

    // ── Neural Lattice State ─────────────────────────────────────────
    val nodeStates: List<NodeState>,
    val edgeStates: List<EdgeState>,

    // ── AI Layer Metrics ─────────────────────────────────────────────
    val autonomyLevel: String,
    val activeRuleCount: Int,
    val totalEpisodeCount: Int,
    val memoryUsageBytes: Long,

    // ── Cognitive Metrics ────────────────────────────────────────────
    val cognitiveLoad: Float,           // 0.0–1.0; drives neural lattice intensity
    val processingLatencyMs: Long,
    val confidenceLevel: Float,
    val decisionRatePerMinute: Float,

    // ── Visualization Hints ──────────────────────────────────────────
    val activeParticleCount: Int,
    val energyFlowDirection: List<Float> = listOf(0f, 0f, 0f),  // vec3
    val lightPulseIntensity: Float = 0f
)

/**
 * State of a single node in the cognitive graph.
 */
@Serializable
data class NodeState(
    val nodeId: String,
    val nodeType: CognitiveNodeType,
    val position: List<Float>,          // [x, y, z]
    val activationLevel: Float,         // 0.0–1.0; brightness/size in visualization
    val label: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class CognitiveNodeType {
    MEMORY_EPISODIC,
    MEMORY_SEMANTIC,
    MEMORY_PROCEDURAL,
    REASONING_OPTION,
    REASONING_DECISION,
    REFLECTION_INSIGHT,
    EVOLUTION_RULE,
    AUTONOMY_GATE
}

/**
 * State of a single edge (connection) in the cognitive graph.
 */
@Serializable
data class EdgeState(
    val edgeId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val weight: Float,                  // Connection strength
    val flowRate: Float,                // Data flow intensity (drives particle speed)
    val edgeType: CognitiveEdgeType
)

@Serializable
enum class CognitiveEdgeType {
    MEMORY_RECALL,       // Memory → Reasoning
    REASONING_FLOW,      // Between reasoning nodes
    REFLECTION_LINK,     // Reasoning → Reflection
    EVOLUTION_UPDATE,    // Reflection → Evolution
    AUTONOMY_GATE_FLOW,  // Evolution → Autonomy
    CAUSAL_LINK          // Cross-layer causal relationship
}

/**
 * Snapshot of a decision for replay inspection.
 */
@Serializable
data class DecisionSnapshot(
    val decisionId: String,
    val action: String,
    val chosenOptionId: String,

    // All candidate options with scores
    val reasoningScores: List<OptionScore>,

    // The explanation generated by the reasoning engine
    val explanation: String,

    // Execution outcome (if known at recording time)
    val executionStatus: String,
    val outcome: String? = null,

    // Assumptions the AI made
    val assumptions: List<String> = emptyList(),

    // Context that influenced the decision
    val contextMap: Map<String, String> = emptyMap()
)

@Serializable
data class OptionScore(
    val optionId: String,
    val action: String,
    val score: Float,
    val riskLevel: String,
    val wasChosen: Boolean
)

/**
 * A rule change event (creation, weight update, deprecation).
 */
@Serializable
data class RuleUpdate(
    val ruleId: String,
    val changeType: RuleChangeType,
    val previousWeight: Float?,
    val newWeight: Float?,
    val previousCondition: String?,
    val newCondition: String?,
    val trigger: String               // What caused this change
)

@Serializable
enum class RuleChangeType {
    CREATED,
    WEIGHT_INCREASED,
    WEIGHT_DECREASED,
    CONDITION_MODIFIED,
    DEPRECATED,
    REACTIVATED
}

/**
 * A memory change event (new episode, fact update, etc.).
 */
@Serializable
data class MemoryChange(
    val memoryType: MemoryChangeType,
    val entityId: String,
    val changeDescription: String,
    val previousValue: String? = null,
    val newValue: String? = null
)

@Serializable
enum class MemoryChangeType {
    EPISODE_CREATED,
    EPISODE_OUTCOME_UPDATED,
    FACT_CREATED,
    FACT_CONFIDENCE_CHANGED,
    RULE_STORED,
    MEMORY_PRUNED
}


// ─────────────────────────────────────────────────────────────────────────────
// PART 6 — DIFFERENCE VISUALIZATION MODELS
// Structures used to highlight what changed between two replay frames.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Difference between two consecutive CognitiveSnapshots.
 * The renderer uses this to highlight changed/added/removed elements.
 */
@Serializable
data class StateDiff(
    val fromSequence: Long,
    val toSequence: Long,

    val addedNodes: List<NodeState>,
    val removedNodeIds: List<String>,
    val changedNodes: List<NodeDiff>,

    val addedEdges: List<EdgeState>,
    val removedEdgeIds: List<String>,
    val changedEdges: List<EdgeDiff>,

    val ruleChanges: List<RuleUpdate>,
    val memoryChanges: List<MemoryChange>,

    // Summary metrics
    val totalChanges: Int
        get() = addedNodes.size + removedNodeIds.size + changedNodes.size +
                addedEdges.size + removedEdgeIds.size + changedEdges.size +
                ruleChanges.size + memoryChanges.size
)

@Serializable
data class NodeDiff(
    val nodeId: String,
    val previousActivation: Float,
    val newActivation: Float,
    val previousPosition: List<Float>?,
    val newPosition: List<Float>?
)

@Serializable
data class EdgeDiff(
    val edgeId: String,
    val previousWeight: Float,
    val newWeight: Float,
    val previousFlowRate: Float,
    val newFlowRate: Float
)
