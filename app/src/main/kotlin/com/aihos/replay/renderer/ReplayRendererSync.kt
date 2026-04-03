package com.aihos.replay.renderer

import com.aihos.rendering.lighting.LightColor
import com.aihos.rendering.lighting.Vec3
import com.aihos.rendering.lighting.VolumetricLightingEngine
import com.aihos.replay.engine.PlaybackState
import com.aihos.replay.engine.ReplayController
import com.aihos.replay.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

// ─────────────────────────────────────────────────────────────────────────────
// PART 4 — VISUAL REPLAY MODE
// Synchronizes the 3D renderer with replay data so the neural lattice,
// cognitive graph, particles, HUD, and volumetric lighting all reflect
// the historical state being replayed.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rendering mode: live (normal operation) or replay (historical data).
 */
enum class RenderMode {
    LIVE,
    REPLAY
}

/**
 * Data class the renderer reads each frame to know what to display.
 * When in REPLAY mode, these values come from the replay controller
 * instead of the live AI system.
 */
data class RenderState(
    val mode: RenderMode = RenderMode.LIVE,

    // ── Neural Lattice ──────────────────────────────────────────────────
    val nodePositions: List<FloatArray> = emptyList(),       // [x,y,z] per node
    val nodeActivations: List<Float> = emptyList(),          // 0.0–1.0 per node
    val nodeColors: List<FloatArray> = emptyList(),          // [r,g,b,a] per node

    // ── Cognitive Graph Edges ────────────────────────────────────────────
    val edgeEndpoints: List<Pair<Int, Int>> = emptyList(),   // (srcIndex, dstIndex)
    val edgeWeights: List<Float> = emptyList(),              // line thickness
    val edgeFlowRates: List<Float> = emptyList(),            // particle speed

    // ── Particles ────────────────────────────────────────────────────────
    val particleCount: Int = 0,
    val particleFlowDirection: FloatArray = floatArrayOf(0f, 0f, 0f),

    // ── HUD Overlay ─────────────────────────────────────────────────────
    val hudCognitiveLoad: Float = 0f,
    val hudConfidence: Float = 0f,
    val hudAction: String = "",
    val hudAutonomyLevel: String = "",
    val hudEventType: String = "",
    val hudTimestamp: Long = 0,
    val hudSequenceNumber: Long = 0,

    // ── Volumetric Lighting ──────────────────────────────────────────────
    val lightPulseIntensity: Float = 0f,
    val lightColorOverride: FloatArray? = null,

    // ── Diff Highlights (Part 6) ─────────────────────────────────────────
    val highlightAddedNodeIds: Set<String> = emptySet(),
    val highlightRemovedNodeIds: Set<String> = emptySet(),
    val highlightChangedNodeIds: Set<String> = emptySet(),
    val highlightAddedEdgeIds: Set<String> = emptySet(),
    val highlightRemovedEdgeIds: Set<String> = emptySet()
)

/**
 * Bridge between the ReplayController and the 3D renderer.
 *
 * Collects replay events from the controller, transforms them into
 * RenderState objects, and exposes them as a reactive flow that the
 * renderer subscribes to.
 *
 * Usage in the renderer:
 * ```kotlin
 * class SAIHOSRenderer : GLSurfaceView.Renderer {
 *     private val replaySync = ReplayRendererSync(replayController)
 *
 *     override fun onDrawFrame(gl: GL10?) {
 *         val state = replaySync.currentRenderState.value
 *         when (state.mode) {
 *             RenderMode.LIVE -> renderLiveScene()
 *             RenderMode.REPLAY -> renderReplayScene(state)
 *         }
 *     }
 * }
 * ```
 */
class ReplayRendererSync(
    private val replayController: ReplayController,
    private val volumetricEngine: VolumetricLightingEngine? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentRenderState = MutableStateFlow(RenderState())
    val currentRenderState: StateFlow<RenderState> = _currentRenderState.asStateFlow()

    private val _renderMode = MutableStateFlow(RenderMode.LIVE)
    val renderMode: StateFlow<RenderMode> = _renderMode.asStateFlow()

    // Node ID → index mapping for GPU buffer ordering
    private val nodeIndexMap = mutableMapOf<String, Int>()

    /**
     * Activate replay mode. The renderer switches to reading from historical data.
     */
    fun enterReplayMode() {
        _renderMode.value = RenderMode.REPLAY
        startStateSync()
        Timber.i("Entered visual replay mode")
    }

    /**
     * Return to live rendering.
     */
    fun exitReplayMode() {
        _renderMode.value = RenderMode.LIVE
        stopStateSync()
        _currentRenderState.value = RenderState(mode = RenderMode.LIVE)
        Timber.i("Exited visual replay mode — returning to live")
    }

    // ═════════════════════════════════════════════════════════════════════
    // STATE SYNCHRONIZATION
    // ═════════════════════════════════════════════════════════════════════

    private var syncJob: Job? = null

    private fun startStateSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            // Observe replay controller's current event
            replayController.currentEvent
                .filterNotNull()
                .combine(replayController.currentDiff) { event, diff -> event to diff }
                .collect { (event, diff) ->
                    val newState = buildRenderState(event, diff)
                    _currentRenderState.value = newState

                    // Sync volumetric lighting
                    volumetricEngine?.let { engine ->
                        engine.setAIActivityLevel(event.cognitiveSnapshot.cognitiveLoad)
                        if (event.cognitiveSnapshot.lightPulseIntensity > 0.1f) {
                            engine.triggerEnergyPulse(event.cognitiveSnapshot.lightPulseIntensity)
                        }
                    }
                }
        }
    }

    private fun stopStateSync() {
        syncJob?.cancel()
    }

    /**
     * Transform a ReplayEvent + optional StateDiff into a RenderState
     * that the GPU-side renderer can consume directly.
     */
    private fun buildRenderState(event: ReplayEvent, diff: StateDiff?): RenderState {
        val snapshot = event.cognitiveSnapshot

        // ── Build node index map ─────────────────────────────────────────
        nodeIndexMap.clear()
        snapshot.nodeStates.forEachIndexed { index, node ->
            nodeIndexMap[node.nodeId] = index
        }

        // ── Node data ────────────────────────────────────────────────────
        val positions = snapshot.nodeStates.map { it.position.toFloatArray() }
        val activations = snapshot.nodeStates.map { it.activationLevel }
        val colors = snapshot.nodeStates.map { node ->
            colorForNodeType(node.nodeType, node.activationLevel)
        }

        // ── Edge data ────────────────────────────────────────────────────
        val edges = snapshot.edgeStates.mapNotNull { edge ->
            val srcIdx = nodeIndexMap[edge.sourceNodeId]
            val dstIdx = nodeIndexMap[edge.targetNodeId]
            if (srcIdx != null && dstIdx != null) Triple(srcIdx to dstIdx, edge.weight, edge.flowRate)
            else null
        }

        // ── Diff highlights ──────────────────────────────────────────────
        val addedNodeIds = diff?.addedNodes?.map { it.nodeId }?.toSet() ?: emptySet()
        val removedNodeIds = diff?.removedNodeIds?.toSet() ?: emptySet()
        val changedNodeIds = diff?.changedNodes?.map { it.nodeId }?.toSet() ?: emptySet()
        val addedEdgeIds = diff?.addedEdges?.map { it.edgeId }?.toSet() ?: emptySet()
        val removedEdgeIds = diff?.removedEdgeIds?.toSet() ?: emptySet()

        return RenderState(
            mode = RenderMode.REPLAY,

            nodePositions = positions,
            nodeActivations = activations,
            nodeColors = colors,

            edgeEndpoints = edges.map { it.first },
            edgeWeights = edges.map { it.second },
            edgeFlowRates = edges.map { it.third },

            particleCount = snapshot.activeParticleCount,
            particleFlowDirection = snapshot.energyFlowDirection.toFloatArray(),

            hudCognitiveLoad = snapshot.cognitiveLoad,
            hudConfidence = snapshot.confidenceLevel,
            hudAction = event.decisionSnapshot?.action ?: "",
            hudAutonomyLevel = snapshot.autonomyLevel,
            hudEventType = event.eventType.name,
            hudTimestamp = event.timestamp,
            hudSequenceNumber = event.sequenceNumber,

            lightPulseIntensity = snapshot.lightPulseIntensity,

            highlightAddedNodeIds = addedNodeIds,
            highlightRemovedNodeIds = removedNodeIds,
            highlightChangedNodeIds = changedNodeIds,
            highlightAddedEdgeIds = addedEdgeIds,
            highlightRemovedEdgeIds = removedEdgeIds
        )
    }

    /**
     * Map cognitive node type to a color for the visualization.
     */
    private fun colorForNodeType(type: CognitiveNodeType, activation: Float): FloatArray {
        val base = when (type) {
            CognitiveNodeType.MEMORY_EPISODIC    -> floatArrayOf(0.2f, 0.7f, 1.0f, 1f)  // Cyan
            CognitiveNodeType.MEMORY_SEMANTIC    -> floatArrayOf(0.3f, 0.9f, 0.5f, 1f)  // Green
            CognitiveNodeType.MEMORY_PROCEDURAL  -> floatArrayOf(1.0f, 0.8f, 0.2f, 1f)  // Gold
            CognitiveNodeType.REASONING_OPTION   -> floatArrayOf(0.9f, 0.4f, 0.1f, 1f)  // Orange
            CognitiveNodeType.REASONING_DECISION -> floatArrayOf(1.0f, 0.2f, 0.3f, 1f)  // Red
            CognitiveNodeType.REFLECTION_INSIGHT -> floatArrayOf(0.7f, 0.3f, 1.0f, 1f)  // Violet
            CognitiveNodeType.EVOLUTION_RULE     -> floatArrayOf(0.1f, 0.5f, 1.0f, 1f)  // Blue
            CognitiveNodeType.AUTONOMY_GATE      -> floatArrayOf(1.0f, 1.0f, 1.0f, 1f)  // White
        }
        // Modulate alpha by activation
        base[3] = (activation * 0.7f + 0.3f).coerceIn(0.3f, 1.0f)
        return base
    }

    fun destroy() {
        stopStateSync()
        scope.cancel()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// GLSL SHADER UTILITIES FOR DIFF VISUALIZATION (Part 6)
// These snippets integrate into the existing node/edge shaders to
// highlight changes during replay.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GLSL code snippets that should be injected into the neural lattice
 * node rendering shader when replay mode is active.
 */
object ReplayShaderSnippets {

    /**
     * Uniform block to declare in the node shader:
     */
    const val REPLAY_UNIFORMS = """
uniform int  uReplayMode;           // 0 = live, 1 = replay
uniform float uReplayHighlightTime; // animated pulse for highlights
"""

    /**
     * Per-node highlight logic (call in fragment shader).
     * Expects `uNodeHighlightType` as a per-instance attribute:
     *   0 = normal, 1 = added (green glow), 2 = removed (red fade),
     *   3 = changed (yellow pulse)
     */
    const val NODE_HIGHLIGHT_FRAG = """
// ── Replay diff highlight ────────────────────────────────────────────
vec4 applyReplayHighlight(vec4 baseColor, int highlightType, float time) {
    if (highlightType == 0) return baseColor;
    
    float pulse = sin(time * 4.0) * 0.5 + 0.5;
    
    if (highlightType == 1) {
        // Added node: green glow with expanding ring
        vec3 addGlow = vec3(0.1, 1.0, 0.3) * pulse * 0.6;
        return vec4(baseColor.rgb + addGlow, baseColor.a);
    }
    else if (highlightType == 2) {
        // Removed node: red, fading out
        float fade = 1.0 - pulse * 0.5;
        return vec4(mix(baseColor.rgb, vec3(1.0, 0.1, 0.1), 0.5), baseColor.a * fade);
    }
    else if (highlightType == 3) {
        // Changed node: yellow pulsing outline
        vec3 changeGlow = vec3(1.0, 0.9, 0.1) * pulse * 0.4;
        return vec4(baseColor.rgb + changeGlow, baseColor.a);
    }
    
    return baseColor;
}
"""

    /**
     * Edge highlight logic for changed/added/removed connections.
     */
    const val EDGE_HIGHLIGHT_FRAG = """
vec4 applyEdgeHighlight(vec4 baseColor, int highlightType, float time) {
    if (highlightType == 0) return baseColor;
    
    float dash = step(0.5, fract(time * 3.0));
    
    if (highlightType == 1) {
        // New connection: bright cyan, dashed animation
        return vec4(0.0, 0.9, 1.0, baseColor.a * (0.5 + dash * 0.5));
    }
    else if (highlightType == 2) {
        // Removed connection: red, dissolving
        float dissolve = 1.0 - fract(time * 0.5);
        return vec4(1.0, 0.2, 0.1, baseColor.a * dissolve);
    }
    
    return baseColor;
}
"""
}
