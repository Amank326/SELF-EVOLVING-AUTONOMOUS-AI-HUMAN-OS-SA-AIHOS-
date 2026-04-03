package com.aihos.ui.render.datagraph

import android.opengl.GLES30
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.RenderState
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * CognitiveGraphRenderer — GPU instanced renderer for cognitive data visualization.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Rendering Architecture                                          │
 * │                                                                  │
 * │  Nodes:   Icosphere mesh (shared) + per-instance attributes      │
 * │           pos(3)+color(4)+radius(1)+energy(1)+phase(1)           │
 * │           +type(1)+weight(1)+highlight(1) = 13 floats            │
 * │           → glDrawElementsInstanced (1 draw call)                │
 * │                                                                  │
 * │  Edges:   Procedural cylinder quad-strip + per-instance attrs    │
 * │           start(3)+end(3)+strength(1)+pulse(1)+type(1) = 9       │
 * │           → glDrawArraysInstanced (1 draw call)                  │
 * │                                                                  │
 * │  Particles: Point sprites with per-instance attributes           │
 * │           pos(3)+color(4)+size(1) = 8 floats                     │
 * │           → glDrawArraysInstanced (1 draw call)                  │
 * │                                                                  │
 * │  Total: 3 draw calls for entire cognitive graph visualization    │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Buffer strategy:
 *   - Static VBOs for base geometry (icosphere, cylinder template)
 *   - Dynamic VBOs for instance data (glBufferSubData, no reallocation)
 *   - Pre-allocated FloatBuffers for CPU staging
 *   - Zero per-frame heap allocation
 */
class CognitiveGraphRenderer {

    companion object {
        // Per-node instance: pos(3)+color(4)+radius(1)+energy(1)+phase(1)+type(1)+weight(1)+highlight(1) = 13
        private const val NODE_INSTANCE_FLOATS = 13
        // Per-edge instance: start(3)+end(3)+strength(1)+pulsePhase(1)+edgeType(1) = 9
        private const val EDGE_INSTANCE_FLOATS = 9
        // Per-particle: pos(3)+color(4)+size(1) = 8
        private const val PARTICLE_INSTANCE_FLOATS = 8

        // Beam geometry segments
        private const val BEAM_LENGTH_SEGMENTS = 12
        private const val BEAM_RADIAL_SEGMENTS = 6

        // Max particles = MAX_EDGES * MAX_PARTICLES_PER_EDGE
        private const val MAX_PARTICLES = CognitiveGraph.MAX_EDGES * CognitiveEdge.MAX_PARTICLES_PER_EDGE
    }

    // Shader programs
    private var nodeProgram: ShaderProgram? = null
    private var edgeProgram: ShaderProgram? = null
    private var particleProgram: ShaderProgram? = null

    // Base geometry
    private var nodeMesh: MeshGenerator.MeshData? = null
    private var edgeVao = 0
    private var edgeBaseVbo = 0
    private var edgeVertexCount = 0

    // Instance VBOs
    private var nodeInstanceVbo = 0
    private var edgeInstanceVbo = 0
    private var particleInstanceVbo = 0

    // VAOs
    private var nodeVao = 0
    private var particleVao = 0

    // CPU staging buffers (pre-allocated, reused every frame)
    private val nodeInstanceData = FloatBuffer.allocate(CognitiveGraph.MAX_NODES * NODE_INSTANCE_FLOATS)
    private val edgeInstanceData = FloatBuffer.allocate(CognitiveGraph.MAX_EDGES * EDGE_INSTANCE_FLOATS)
    private val particleInstanceData = FloatBuffer.allocate(MAX_PARTICLES * PARTICLE_INSTANCE_FLOATS)

    // Counters for draw calls
    var uploadedNodeCount = 0; private set
    var uploadedEdgeCount = 0; private set
    var uploadedParticleCount = 0; private set

    private var isInitialized = false

    // ════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════

    fun initialize() {
        nodeProgram = ShaderProgram(CognitiveGraphShaders.NODE_VERTEX, CognitiveGraphShaders.NODE_FRAGMENT)
        edgeProgram = ShaderProgram(CognitiveGraphShaders.EDGE_VERTEX, CognitiveGraphShaders.EDGE_FRAGMENT)
        particleProgram = ShaderProgram(CognitiveGraphShaders.PARTICLE_VERTEX, CognitiveGraphShaders.PARTICLE_FRAGMENT)

        // Icosphere for nodes (low-poly for perf with 300 instances)
        nodeMesh = MeshGenerator.generateIcosphere(subdivisions = 1, radius = 1f)

        setupNodeInstancing()
        setupEdgeGeometry()
        setupParticleInstancing()

        isInitialized = true
        Timber.d("CognitiveGraphRenderer: initialized (node/edge/particle shaders)")
    }

    private fun setupNodeInstancing() {
        val mesh = nodeMesh ?: return

        // Create instance VBO
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        nodeInstanceVbo = vbos[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            CognitiveGraph.MAX_NODES * NODE_INSTANCE_FLOATS * 4,
            null, GLES30.GL_DYNAMIC_DRAW
        )

        // VAO combining mesh + instance attributes
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        nodeVao = vaos[0]
        GLES30.glBindVertexArray(nodeVao)

        // Mesh VBO: position (loc 0) + normal (loc 1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.vbo)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 32, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 32, 12)
        GLES30.glEnableVertexAttribArray(1)

        // Index buffer
        if (mesh.ebo != 0) {
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.ebo)
        }

        // Instance VBO attributes
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
        val stride = NODE_INSTANCE_FLOATS * 4 // 52 bytes

        // loc 2: instancePos (3 floats) offset 0
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribDivisor(2, 1)

        // loc 3: instanceColor (4 floats) offset 12
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribDivisor(3, 1)

        // loc 4: instanceRadius (1 float) offset 28
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, stride, 28)
        GLES30.glEnableVertexAttribArray(4); GLES30.glVertexAttribDivisor(4, 1)

        // loc 5: instanceEnergy (1 float) offset 32
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, stride, 32)
        GLES30.glEnableVertexAttribArray(5); GLES30.glVertexAttribDivisor(5, 1)

        // loc 6: instancePhase (1 float) offset 36
        GLES30.glVertexAttribPointer(6, 1, GLES30.GL_FLOAT, false, stride, 36)
        GLES30.glEnableVertexAttribArray(6); GLES30.glVertexAttribDivisor(6, 1)

        // loc 7: instanceType (1 float) offset 40
        GLES30.glVertexAttribPointer(7, 1, GLES30.GL_FLOAT, false, stride, 40)
        GLES30.glEnableVertexAttribArray(7); GLES30.glVertexAttribDivisor(7, 1)

        // loc 8: instanceWeight (1 float) offset 44
        GLES30.glVertexAttribPointer(8, 1, GLES30.GL_FLOAT, false, stride, 44)
        GLES30.glEnableVertexAttribArray(8); GLES30.glVertexAttribDivisor(8, 1)

        // loc 9: instanceHighlight (1 float) offset 48
        GLES30.glVertexAttribPointer(9, 1, GLES30.GL_FLOAT, false, stride, 48)
        GLES30.glEnableVertexAttribArray(9); GLES30.glVertexAttribDivisor(9, 1)

        GLES30.glBindVertexArray(0)
    }

    private fun setupEdgeGeometry() {
        // Generate cylinder quad strip template (same pattern as NeuralLatticeRenderer)
        val verts = mutableListOf<Float>()
        for (i in 0..BEAM_LENGTH_SEGMENTS) {
            val t = i.toFloat() / BEAM_LENGTH_SEGMENTS
            for (j in 0..BEAM_RADIAL_SEGMENTS) {
                val r = j.toFloat() / BEAM_RADIAL_SEGMENTS
                verts.add(t); verts.add(r); verts.add(0f)
            }
        }
        val vertArray = verts.toFloatArray()
        edgeVertexCount = vertArray.size / 3

        // VBOs
        val vbos = IntArray(2)
        GLES30.glGenBuffers(2, vbos, 0)
        edgeBaseVbo = vbos[0]
        edgeInstanceVbo = vbos[1]

        // Base geometry VBO (static)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, edgeBaseVbo)
        val buf = ByteBuffer.allocateDirect(vertArray.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(vertArray).flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertArray.size * 4, buf, GLES30.GL_STATIC_DRAW)

        // Instance VBO (dynamic)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, edgeInstanceVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            CognitiveGraph.MAX_EDGES * EDGE_INSTANCE_FLOATS * 4,
            null, GLES30.GL_DYNAMIC_DRAW
        )

        // VAO
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        edgeVao = vaos[0]
        GLES30.glBindVertexArray(edgeVao)

        // Base vertex (loc 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, edgeBaseVbo)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 12, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Instance attrs
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, edgeInstanceVbo)
        val bs = EDGE_INSTANCE_FLOATS * 4

        // loc 1: startPos (3)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, bs, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribDivisor(1, 1)
        // loc 2: endPos (3)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, bs, 12)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribDivisor(2, 1)
        // loc 3: strength (1)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, bs, 24)
        GLES30.glEnableVertexAttribArray(3); GLES30.glVertexAttribDivisor(3, 1)
        // loc 4: pulsePhase (1)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, bs, 28)
        GLES30.glEnableVertexAttribArray(4); GLES30.glVertexAttribDivisor(4, 1)
        // loc 5: edgeType (1)
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, bs, 32)
        GLES30.glEnableVertexAttribArray(5); GLES30.glVertexAttribDivisor(5, 1)

        GLES30.glBindVertexArray(0)
    }

    private fun setupParticleInstancing() {
        // VBO for particle instance data
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        particleInstanceVbo = vbos[0]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, particleInstanceVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            MAX_PARTICLES * PARTICLE_INSTANCE_FLOATS * 4,
            null, GLES30.GL_DYNAMIC_DRAW
        )

        // VAO
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        particleVao = vaos[0]
        GLES30.glBindVertexArray(particleVao)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, particleInstanceVbo)
        val ps = PARTICLE_INSTANCE_FLOATS * 4

        // loc 0: particlePos (3)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, ps, 0)
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribDivisor(0, 1)
        // loc 1: particleColor (4)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, ps, 12)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribDivisor(1, 1)
        // loc 2: particleSize (1)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, ps, 28)
        GLES30.glEnableVertexAttribArray(2); GLES30.glVertexAttribDivisor(2, 1)

        GLES30.glBindVertexArray(0)
    }

    // ════════════════════════════════════════════════════════════════
    // Instance data upload (called once per frame)
    // ════════════════════════════════════════════════════════════════

    fun uploadInstanceData(graph: CognitiveGraph) {
        if (!isInitialized) return

        uploadNodeData(graph)
        uploadEdgeData(graph)
        uploadParticleData(graph)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadNodeData(graph: CognitiveGraph) {
        nodeInstanceData.clear()
        var count = 0
        for (n in graph.nodes) {
            if (!n.active) continue
            nodeInstanceData.put(n.position[0]); nodeInstanceData.put(n.position[1]); nodeInstanceData.put(n.position[2])
            nodeInstanceData.put(n.color[0]); nodeInstanceData.put(n.color[1]); nodeInstanceData.put(n.color[2]); nodeInstanceData.put(n.color[3])
            nodeInstanceData.put(n.radius)
            nodeInstanceData.put(n.energy)
            nodeInstanceData.put(n.phase)
            nodeInstanceData.put(n.nodeType.typeIndex.toFloat())
            nodeInstanceData.put(n.weight)
            nodeInstanceData.put(n.highlightAlpha)
            count++
        }
        nodeInstanceData.flip()
        uploadedNodeCount = count

        if (count > 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * NODE_INSTANCE_FLOATS * 4, nodeInstanceData)
        }
    }

    private fun uploadEdgeData(graph: CognitiveGraph) {
        edgeInstanceData.clear()
        var count = 0
        for (e in graph.edges) {
            if (!e.active) continue
            val from = graph.nodes[e.fromIndex]; val to = graph.nodes[e.toIndex]
            if (!from.active || !to.active) continue
            edgeInstanceData.put(from.position[0]); edgeInstanceData.put(from.position[1]); edgeInstanceData.put(from.position[2])
            edgeInstanceData.put(to.position[0]); edgeInstanceData.put(to.position[1]); edgeInstanceData.put(to.position[2])
            edgeInstanceData.put(e.strength)
            edgeInstanceData.put(e.phase)
            edgeInstanceData.put(e.edgeType.typeIndex.toFloat())
            count++
        }
        edgeInstanceData.flip()
        uploadedEdgeCount = count

        if (count > 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, edgeInstanceVbo)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * EDGE_INSTANCE_FLOATS * 4, edgeInstanceData)
        }
    }

    private fun uploadParticleData(graph: CognitiveGraph) {
        particleInstanceData.clear()
        var count = 0
        val maxParticles = MAX_PARTICLES

        for (e in graph.edges) {
            if (!e.active) continue
            if (count >= maxParticles) break

            val from = graph.nodes[e.fromIndex]; val to = graph.nodes[e.toIndex]
            if (!from.active || !to.active) continue

            for (p in 0 until CognitiveEdge.MAX_PARTICLES_PER_EDGE) {
                if (!e.particleActive[p]) continue
                if (count >= maxParticles) break

                val t = e.particleProgress[p]
                val alpha = e.particleAlpha[p]

                // Interpolate position along edge
                val px = from.position[0] + (to.position[0] - from.position[0]) * t
                val py = from.position[1] + (to.position[1] - from.position[1]) * t
                val pz = from.position[2] + (to.position[2] - from.position[2]) * t

                particleInstanceData.put(px); particleInstanceData.put(py); particleInstanceData.put(pz)

                // Color from edge type
                when (e.edgeType) {
                    EdgeType.CAUSAL ->         { particleInstanceData.put(0.8f); particleInstanceData.put(0.95f); particleInstanceData.put(1f) }
                    EdgeType.EVIDENTIAL ->     { particleInstanceData.put(0.4f); particleInstanceData.put(0.7f); particleInstanceData.put(1f) }
                    EdgeType.TEMPORAL ->        { particleInstanceData.put(0.3f); particleInstanceData.put(1f); particleInstanceData.put(0.5f) }
                    EdgeType.REINFORCEMENT ->  { particleInstanceData.put(1f); particleInstanceData.put(0.7f); particleInstanceData.put(0.3f) }
                    EdgeType.CONFLICT ->        { particleInstanceData.put(1f); particleInstanceData.put(0.3f); particleInstanceData.put(0.35f) }
                }
                particleInstanceData.put(alpha)

                // Size: larger particles for stronger edges
                particleInstanceData.put(0.02f + e.strength * 0.03f)

                count++
            }
        }
        particleInstanceData.flip()
        uploadedParticleCount = count

        if (count > 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, particleInstanceVbo)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * PARTICLE_INSTANCE_FLOATS * 4, particleInstanceData)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Draw calls
    // ════════════════════════════════════════════════════════════════

    fun drawEdges(camera: CameraController, state: RenderState, metrics: AIMetricsSnapshot) {
        val prog = edgeProgram ?: return
        if (uploadedEdgeCount <= 0) return

        // Additive blend for energy beams
        GLES30.glDepthMask(false)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", state.elapsedTime)
        prog.setFloat("uCognitiveLoad", metrics.cognitiveLoad)
        prog.setFloat("uMemoryActivity", metrics.memoryLoad)

        GLES30.glBindVertexArray(edgeVao)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, edgeVertexCount, uploadedEdgeCount)
        GLES30.glBindVertexArray(0)

        GLES30.glDepthMask(true)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun drawNodes(camera: CameraController, state: RenderState, metrics: AIMetricsSnapshot) {
        val prog = nodeProgram ?: return
        if (uploadedNodeCount <= 0) return

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", state.elapsedTime)
        prog.setVec3("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
        prog.setFloat("uCognitiveLoad", metrics.cognitiveLoad)
        prog.setFloat("uConfidence", metrics.confidence)

        val mesh = nodeMesh ?: return
        GLES30.glBindVertexArray(nodeVao)
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0, uploadedNodeCount
        )
        GLES30.glBindVertexArray(0)
    }

    fun drawParticles(camera: CameraController, state: RenderState) {
        val prog = particleProgram ?: return
        if (uploadedParticleCount <= 0) return

        // Additive blend, no depth write
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", state.elapsedTime)

        GLES30.glBindVertexArray(particleVao)
        GLES30.glDrawArraysInstanced(GLES30.GL_POINTS, 0, 1, uploadedParticleCount)
        GLES30.glBindVertexArray(0)

        // Restore
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    // ════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════

    fun release() {
        nodeProgram?.release(); nodeProgram = null
        edgeProgram?.release(); edgeProgram = null
        particleProgram?.release(); particleProgram = null
        nodeMesh?.release(); nodeMesh = null

        if (nodeVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(nodeVao), 0); nodeVao = 0 }
        if (nodeInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(nodeInstanceVbo), 0); nodeInstanceVbo = 0 }
        if (edgeVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(edgeVao), 0); edgeVao = 0 }
        if (edgeBaseVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(edgeBaseVbo), 0); edgeBaseVbo = 0 }
        if (edgeInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(edgeInstanceVbo), 0); edgeInstanceVbo = 0 }
        if (particleVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(particleVao), 0); particleVao = 0 }
        if (particleInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(particleInstanceVbo), 0); particleInstanceVbo = 0 }

        isInitialized = false
        Timber.d("CognitiveGraphRenderer: released")
    }
}

