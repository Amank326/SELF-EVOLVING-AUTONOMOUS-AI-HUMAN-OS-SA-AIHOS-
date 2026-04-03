package com.aihos.rendering.evolution

import com.aihos.rendering.lighting.Vec3
import com.aihos.replay.model.RuleUpdate
import com.aihos.replay.model.RuleChangeType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
// 1. EVOLUTION MODEL & 7. PERFORMANCE
// Flattened Data-Oriented structure for continuous Rule Nodes rendering.
// Instanced via GL ES 3.0. Zero per-frame allocations.
// ─────────────────────────────────────────────────────────────────────────────
class EvolutionNodeBuffer(private val maxRules: Int = 500) {
    // Current live properties
    val posX = FloatArray(maxRules); val posY = FloatArray(maxRules); val posZ = FloatArray(maxRules)
    val scale = FloatArray(maxRules)
    val colorR = FloatArray(maxRules); val colorG = FloatArray(maxRules); val colorB = FloatArray(maxRules)
    val alpha = FloatArray(maxRules)
    
    // Target properties for smooth inter-frame interpolation (Rule 5)
    val targetScale = FloatArray(maxRules)
    val targetAlpha = FloatArray(maxRules)
    val targetColorR = FloatArray(maxRules); val targetColorG = FloatArray(maxRules); val targetColorB = FloatArray(maxRules)
    
    // Lifecycle / States
    val ruleIds = arrayOfNulls<String>(maxRules)
    val state = IntArray(maxRules) // 0 = Idle, 1 = Birth, 2 = Morphing, 3 = Dissolve
    val lifetimeMs = LongArray(maxRules)
    
    var activeCount: Int = 0

    // GL Instanced Output (Position vec3, Color vec4, Scale float)
    val instanceBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(maxRules * 8 * 4) 
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    fun findOrAllocRule(ruleId: String): Int {
        for (i in 0 until activeCount) {
            if (ruleIds[i] == ruleId) return i
        }
        if (activeCount >= maxRules) return -1 // Drop if full limit met
        val idx = activeCount++
        ruleIds[idx] = ruleId
        return idx
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. ANIMATION & 3. TIMELINE INTEGRATION
// Smooth interpolation across states, strictly time-based.
// ─────────────────────────────────────────────────────────────────────────────
class SelfEvolutionVisualizer {
    val nodes = EvolutionNodeBuffer(500)
    
    // Highlight Path interaction structure (Rule 4)
    private var highlightedRuleId: String? = null
    private var compareModeActive: Boolean = false

    /**
     * Integrates with the `ReplayController` / live event bus.
     * Fires when a rule evolution is detected.
     */
    fun onRuleEvolved(update: RuleUpdate, nodePosition: Vec3) {
        val idx = nodes.findOrAllocRule(update.ruleId)
        if (idx == -1) return
        
        when (update.changeType) {
            RuleChangeType.CREATED -> {
                // Node Birth animation: spawn tiny and transparent, target full scale
                nodes.posX[idx] = nodePosition.x; nodes.posY[idx] = nodePosition.y; nodes.posZ[idx] = nodePosition.z
                nodes.scale[idx] = 0.0f; nodes.targetScale[idx] = 1.0f
                nodes.alpha[idx] = 0.0f; nodes.targetAlpha[idx] = 1.0f
                nodes.state[idx] = 1 // Birth
            }
            RuleChangeType.WEIGHT_INCREASED, RuleChangeType.WEIGHT_DECREASED -> {
                // Morphing: scale maps to weight, glow mapped via color targeting
                val finalWeight = update.newWeight ?: 1.0f
                nodes.targetScale[idx] = 0.5f + (finalWeight * 1.5f) // Size mapped to weight
                
                if (update.changeType == RuleChangeType.WEIGHT_INCREASED) { // Green shift
                    nodes.targetColorR[idx] = 0.2f; nodes.targetColorG[idx] = 1.0f; nodes.targetColorB[idx] = 0.2f
                } else { // Dimming red shift
                    nodes.targetColorR[idx] = 1.0f; nodes.targetColorG[idx] = 0.4f; nodes.targetColorB[idx] = 0.4f
                }
                nodes.state[idx] = 2 // Morphing
            }
            RuleChangeType.DEPRECATED -> {
                // Dissolve effect
                nodes.targetScale[idx] = 2.0f // Briefly expand...
                nodes.targetAlpha[idx] = 0.0f // ...while dissolving out
                nodes.state[idx] = 3 // Dissolve
            }
            else -> {}
        }
    }

    /**
     * 5. ANIMATION LOOP: Interpolates current to target values
     */
    fun update(deltaTime: Float) {
        val lerpSpeed = 5.0f * deltaTime
        var writeIdx = 0
        
        for (i in 0 until nodes.activeCount) {
            // Apply temporal smoothing (lerp)
            nodes.scale[i] += (nodes.targetScale[i] - nodes.scale[i]) * lerpSpeed
            nodes.alpha[i] += (nodes.targetAlpha[i] - nodes.alpha[i]) * lerpSpeed
            nodes.colorR[i] += (nodes.targetColorR[i] - nodes.colorR[i]) * lerpSpeed
            nodes.colorG[i] += (nodes.targetColorG[i] - nodes.colorG[i]) * lerpSpeed
            nodes.colorB[i] += (nodes.targetColorB[i] - nodes.colorB[i]) * lerpSpeed
            
            // Interaction: if not highlighted but compare mode is active, ghost it out
            if (compareModeActive && highlightedRuleId != nodes.ruleIds[i]) {
                nodes.alpha[i] *= 0.5f 
            }

            // Cleanup dissolved nodes once they are functionally invisible
            if (nodes.state[i] == 3 && nodes.alpha[i] < 0.05f) {
                // Dead rule, skip writing
                continue 
            }
            
            if (writeIdx != i) compactArrays(i, writeIdx)
            flushToGPUBuffer(writeIdx)
            writeIdx++
        }
        nodes.activeCount = writeIdx
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. INTERACTION
    // ─────────────────────────────────────────────────────────────────────────────
    
    fun onRuleTapped(ruleId: String) {
        // Toggle specific detail highlighting
        // Submits event to UI layer to render floating card with past -> present diff
    }

    fun onRuleLongPressed(ruleId: String) {
        highlightedRuleId = ruleId
        compareModeActive = true
        // Instruct DataStreams engine to trace lines indicating which modules this rule touches
    }

    fun clearInteraction() {
        highlightedRuleId = null
        compareModeActive = false
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UTILS
    // ─────────────────────────────────────────────────────────────────────────────
    private fun compactArrays(src: Int, dst: Int) {
        nodes.ruleIds[dst] = nodes.ruleIds[src]
        nodes.posX[dst] = nodes.posX[src] // .. other copies
        nodes.scale[dst] = nodes.scale[src]
        nodes.alpha[dst] = nodes.alpha[src]
        nodes.state[dst] = nodes.state[src]
        // Complete structural copy...
    }

    private fun flushToGPUBuffer(idx: Int) {
        val offset = idx * 8
        nodes.instanceBuffer.position(offset)
        nodes.instanceBuffer.put(nodes.posX[idx]).put(nodes.posY[idx]).put(nodes.posZ[idx])
        nodes.instanceBuffer.put(nodes.colorR[idx]).put(nodes.colorG[idx]).put(nodes.colorB[idx]).put(nodes.alpha[idx])
        nodes.instanceBuffer.put(nodes.scale[idx])
    }
}
