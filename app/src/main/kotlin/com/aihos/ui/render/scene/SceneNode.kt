package com.aihos.ui.render.scene

import android.opengl.Matrix
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * SceneNode — A single object in the scene graph.
 *
 * Each node has:
 *   - Local transform (model matrix)
 *   - World transform (parent * local, computed on update)
 *   - Normal matrix (for lighting)
 *   - Visibility flag
 *   - Optional children
 *
 * All matrices are pre-allocated. Zero allocation per frame.
 */
open class SceneNode(val id: String) {

    val localMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    val worldMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    val normalMatrix = FloatArray(9)

    var visible: Boolean = true
    var parent: SceneNode? = null
        private set
    private val _children = mutableListOf<SceneNode>()
    val children: List<SceneNode> get() = _children

    // ── Pre-allocated scratch space ──────────────────────────────
    private val tempMatrix = FloatArray(16)

    // ── Transform helpers (modify localMatrix) ───────────────────

    fun setPosition(x: Float, y: Float, z: Float) {
        Matrix.setIdentityM(localMatrix, 0)
        Matrix.translateM(localMatrix, 0, x, y, z)
    }

    fun setRotation(angleDeg: Float, axisX: Float, axisY: Float, axisZ: Float) {
        Matrix.setIdentityM(localMatrix, 0)
        Matrix.rotateM(localMatrix, 0, angleDeg, axisX, axisY, axisZ)
    }

    fun rotate(angleDeg: Float, axisX: Float, axisY: Float, axisZ: Float) {
        Matrix.rotateM(localMatrix, 0, angleDeg, axisX, axisY, axisZ)
    }

    fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(localMatrix, 0, sx, sy, sz)
    }

    // ── Hierarchy ────────────────────────────────────────────────

    fun addChild(child: SceneNode) {
        child.parent?.removeChild(child)
        child.parent = this
        _children.add(child)
    }

    fun removeChild(child: SceneNode) {
        if (_children.remove(child)) {
            child.parent = null
        }
    }

    // ── Update (propagate world transform) ───────────────────────

    /**
     * Recompute worldMatrix from parent chain, then update children.
     * Call on root node each frame.
     */
    open fun updateTransforms(parentWorld: FloatArray? = null) {
        if (parentWorld != null) {
            Matrix.multiplyMM(worldMatrix, 0, parentWorld, 0, localMatrix, 0)
        } else {
            System.arraycopy(localMatrix, 0, worldMatrix, 0, 16)
        }

        // Extract normal matrix (upper-left 3x3 of worldMatrix)
        normalMatrix[0] = worldMatrix[0]; normalMatrix[1] = worldMatrix[1]; normalMatrix[2] = worldMatrix[2]
        normalMatrix[3] = worldMatrix[4]; normalMatrix[4] = worldMatrix[5]; normalMatrix[5] = worldMatrix[6]
        normalMatrix[6] = worldMatrix[8]; normalMatrix[7] = worldMatrix[9]; normalMatrix[8] = worldMatrix[10]

        for (child in _children) {
            child.updateTransforms(worldMatrix)
        }
    }

    /**
     * Override in subclasses to apply AI-driven animation before transform propagation.
     */
    open fun animate(time: Float, dt: Float, aiMetrics: AIMetricsSnapshot) {}
}

/**
 * AIBrainNode — The central icosphere that pulses with AI state.
 */
class AIBrainNode : SceneNode("ai_brain_core") {

    private var rotation = 0f

    override fun animate(time: Float, dt: Float, aiMetrics: AIMetricsSnapshot) {
        // AI-driven rotation speed
        val speed = 0.2f + aiMetrics.autonomyLevel * 0.4f +
                sin(time * 0.1f * 2f * PI.toFloat()) * 0.05f
        rotation += Math.toDegrees((speed * dt).toDouble()).toFloat()

        // Subtle tilt
        val tilt = sin(time * 0.3f) * 5f

        Matrix.setIdentityM(localMatrix, 0)
        Matrix.rotateM(localMatrix, 0, rotation, 0f, 1f, 0f)
        Matrix.rotateM(localMatrix, 0, tilt, 1f, 0f, 0f)
    }
}

/**
 * ParticleFieldNode — Placeholder for particle system transform.
 */
class ParticleFieldNode : SceneNode("neural_particle_field")

