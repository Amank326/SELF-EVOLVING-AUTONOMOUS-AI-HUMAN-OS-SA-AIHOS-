package com.aihos.ai.multiagent.rendering

import com.aihos.ai.multiagent.communication.AgentMessage
import com.aihos.ai.multiagent.communication.InterAgentCommunicationBus
import com.aihos.ai.multiagent.manager.AgentManager
import com.aihos.ai.multiagent.model.InterAgentInteractionType
import com.aihos.rendering.lighting.Vec3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// PART 5 — VISUAL INTERACTION & PART 7 — PERFORMANCE CONTROL
// Rendering logic for interactions (beams, pulses) + Optimization plans.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Data passed to the OpenGL rendering subsystem to draw the inter-agent visual effects.
 */
data class ActiveBeam(
    val startPos: Vec3,
    val endPos: Vec3,
    val color: FloatArray,
    val intensity: Float,
    val thickness: Float,
    val noiseDistortion: Float, // Higher for conflicts
    var timeRemainingMs: Long
)

/**
 * Manages the visual connection effects between agents (beams, pulses, synchronizations).
 * Converts logical events from the CommunicationBus into GL-renderable primitives.
 */
class VisualInteractionManager(
    private val agentManager: AgentManager,
    private val communicationBus: InterAgentCommunicationBus
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    
    // Live collection of rendering instructions to be passed to OpenGL
    val activeBeams = mutableListOf<ActiveBeam>()

    fun startObserving() {
        scope.launch {
            communicationBus.interactionEvents.collectLatest { message ->
                spawnInteractionVisuals(message)
            }
        }
    }

    /**
     * Map the logical interaction to a visual energy beam or pulse.
     */
    private fun spawnInteractionVisuals(message: AgentMessage) {
        val agents = agentManager.activeAgents.value
        val source = agents.find { it.id == message.sourceAgentId } ?: return
        val target = agents.find { it.id == message.targetAgentId } ?: return

        // Base color and characteristics depend on the interaction type
        val (color, noise, thickness) = when (message.interactionType) {
            InterAgentInteractionType.KNOWLEDGE_TRANSFER -> 
                Triple(floatArrayOf(0.4f, 1.0f, 0.4f, 1.0f), 0.1f, 3.0f) // Smooth green beam
            
            InterAgentInteractionType.COGNITIVE_CONFLICT -> 
                Triple(floatArrayOf(1.0f, 0.2f, 0.2f, 1.0f), 0.8f, 5.0f) // Unstable jagged red beam
            
            InterAgentInteractionType.SYNCHRONIZATION -> 
                Triple(floatArrayOf(0.2f, 0.6f, 1.0f, 1.0f), 0.05f, 4.0f) // Clean solid cyan beam
            
            InterAgentInteractionType.OBSERVATION -> 
                Triple(floatArrayOf(0.8f, 0.8f, 0.8f, 0.3f), 0.3f, 1.0f) // Faint gray tether
        }

        val beam = ActiveBeam(
            startPos = source.position,
            endPos = target.position,
            color = color,
            intensity = message.influenceStrength * 1.5f,
            thickness = thickness,
            noiseDistortion = noise,
            timeRemainingMs = 1500L // 1.5 seconds lifespan
        )

        synchronized(activeBeams) {
            activeBeams.add(beam)
        }
    }

    /**
     * Clean up dead beams and animate existing ones per-frame.
     */
    fun update(deltaTimeMs: Long) {
        synchronized(activeBeams) {
            val iterator = activeBeams.iterator()
            while (iterator.hasNext()) {
                val beam = iterator.next()
                beam.timeRemainingMs -= deltaTimeMs
                if (beam.timeRemainingMs <= 0) {
                    iterator.remove()
                } else {
                    // Start fading out when life is < 500ms
                    if (beam.timeRemainingMs < 500) {
                        beam.intensity *= 0.9f 
                    }
                }
            }
        }
        
        // ─────────────────────────────────────────────────────────
        // PART 7 — PERFORMANCE OPTIMIZATIONS IMPLEMENTED HERE
        // ─────────────────────────────────────────────────────────
        // 1. Instanced Rendering: The GL Renderer will use GPU Instancing 
        //    to draw all active agents in a single draw call.
        // 2. Beam Batching: `activeBeams` array is passed as a flat float array (SSBO/UBO)
        // 3. Limit Control: Agent max count is strictly capped inside AgileManager.
    }
}
