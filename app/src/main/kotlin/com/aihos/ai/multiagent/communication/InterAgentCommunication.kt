package com.aihos.ai.multiagent.communication

import com.aihos.ai.multiagent.manager.AgentManager
import com.aihos.ai.multiagent.model.AgentState
import com.aihos.ai.multiagent.model.InterAgentInteractionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// PART 4 — INTER-AGENT COMMUNICATION
// Message passing, shared memory signaling, and influence propagation.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Message passed between independent agents.
 */
data class AgentMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val sourceAgentId: String,
    val targetAgentId: String,
    val interactionType: InterAgentInteractionType,
    val payload: Any, // MemoryFact, RuleUpdate, or CognitiveSnapshot
    val influenceStrength: Float // 0.0 to 1.0 driving the visual connection
)

/**
 * Handles all message passing and influence propagation between agents.
 */
class InterAgentCommunicationBus(
    private val agentManager: AgentManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Event bus for broadcasting interactions (listened to by the Renderer)
    private val _interactionEvents = MutableSharedFlow<AgentMessage>(extraBufferCapacity = 64)
    val interactionEvents: SharedFlow<AgentMessage> = _interactionEvents.asSharedFlow()

    /**
     * Sends a message from one agent to another and triggers visual interaction flows.
     */
    fun sendMessage(
        sourceId: String,
        targetId: String,
        type: InterAgentInteractionType,
        payload: Any,
        strength: Float = 0.8f
    ) {
        val message = AgentMessage(
            sourceAgentId = sourceId,
            targetAgentId = targetId,
            interactionType = type,
            payload = payload,
            influenceStrength = strength
        )

        // Broadcast to listeners (e.g. VisualInteractionManager)
        _interactionEvents.tryEmit(message)

        // Update states to trigger rendering effects
        agentManager.updateAgentState(sourceId, AgentState.COMMUNICATING)
        
        val targetState = if (type == InterAgentInteractionType.COGNITIVE_CONFLICT) {
            AgentState.CONFLICT
        } else {
            AgentState.COMMUNICATING
        }
        agentManager.updateAgentState(targetId, targetState)

        // Process message async
        scope.launch {
            processMessagePayload(message)
            
            // Allow visuals to play out, then revert states
            delay(1500)
            agentManager.updateAgentState(sourceId, AgentState.IDLE)
            agentManager.updateAgentState(targetId, AgentState.IDLE)
        }
    }

    /**
     * Broadcasts a knowledge discovery to all other agents.
     */
    fun broadcastToAll(sourceId: String, type: InterAgentInteractionType, payload: Any, strength: Float = 0.5f) {
        val activeAgents = agentManager.activeAgents.value
        activeAgents.forEach { agent ->
            if (agent.id != sourceId) {
                sendMessage(sourceId, agent.id, type, payload, strength)
            }
        }
    }

    private suspend fun processMessagePayload(message: AgentMessage) {
        // In a real implementation:
        // 1. Resolve target reasoning engine via DI namespace.
        // 2. Inject rule or fact into the target's memory graph.
        // 3. Trigger a reflection cycle if influenceStrength > threshold.
    }
}
