package com.aihos.ui.render.lattice
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
 * NeuralLatticeRenderer — GPU instanced renderer for neural lattice.
 *
 * Architecture:
 *   Nodes:  Icosphere mesh (shared) + per-instance attributes (pos, color, radius, energy, phase)
 *   Beams:  Procedural cylinder quad-strip + per-instance attributes (start, end, strength, pulse, width)
 *
 * Buffer strategy:
 *   - Static VBOs for base geometry (icosphere, cylinder template)
 *   - Dynamic VBOs for instance data (double-buffered, glBufferSubData only)
 *   - Instance data updated from NeuralLattice CPU state each frame
 *   - glBufferSubData instead of glBufferData (no reallocation)
 *
 * Draw calls:
 *   1. glDrawElementsInstanced for nodes (1 draw call, all nodes)
 *   2. glDrawArraysInstanced for beams (1 draw call, all connections)
 */
class NeuralLatticeRenderer {
    companion object {
        // Per-node instance data: pos(3) + color(4) + radius(1) + energy(1) + phase(1) = 10 floats
        private const val NODE_INSTANCE_FLOATS = 10
        // Per-beam instance data: start(3) + end(3) + strength(1) + pulsePhase(1) + width(1) = 9 floats
        private const val BEAM_INSTANCE_FLOATS = 9
        // Beam geometry: 16 segments along length x 8 radial segments
        private const val BEAM_LENGTH_SEGMENTS = 16
        private const val BEAM_RADIAL_SEGMENTS = 8
    }
    // Shaders
    private var nodeProgram: ShaderProgram? = null
    private var beamProgram: ShaderProgram? = null
    // Base geometry
    private var nodeMesh: MeshGenerator.MeshData? = null
    private var beamVao = 0
    private var beamBaseVbo = 0
    private var beamVertexCount = 0
    // Instance buffers
    private var nodeInstanceVbo = 0
    private var beamInstanceVbo = 0
    // CPU-side staging buffers (pre-allocated, reused every frame)
    private val nodeInstanceData = FloatBuffer.allocate(NeuralLattice.MAX_NODES * NODE_INSTANCE_FLOATS)
    private val beamInstanceData = FloatBuffer.allocate(NeuralLattice.MAX_CONNECTIONS * BEAM_INSTANCE_FLOATS)
    // VAO for node instancing
    private var nodeVao = 0
    private var isInitialized = false
    fun initialize() {
        // Node shader
        nodeProgram = ShaderProgram(LatticeShaders.NODE_VERTEX, LatticeShaders.NODE_FRAGMENT)
        beamProgram = ShaderProgram(LatticeShaders.BEAM_VERTEX, LatticeShaders.BEAM_FRAGMENT)
        // Generate node mesh (low-poly icosphere for perf)
        nodeMesh = MeshGenerator.generateIcosphere(subdivisions = 1, radius = 1f)
        // Setup node instancing VAO
        setupNodeInstancing()
        // Generate beam template geometry
        setupBeamGeometry()
        isInitialized = true
        Timber.d("NeuralLatticeRenderer: initialized")
    }
    private fun setupNodeInstancing() {
        val mesh = nodeMesh ?: return
        // Create instance VBO
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        nodeInstanceVbo = vbos[0]
        // Allocate instance buffer (MAX_NODES * 10 floats * 4 bytes)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
            NeuralLattice.MAX_NODES * NODE_INSTANCE_FLOATS * 4,
            null, GLES30.GL_DYNAMIC_DRAW)
        // Create new VAO that combines mesh + instance attributes
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        nodeVao = vaos[0]
        GLES30.glBindVertexArray(nodeVao)
        // Bind mesh VBO for position (loc 0) and normal (loc 1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.vbo)
        // Position: 3 floats, stride 32 (8 floats * 4 bytes), offset 0
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 32, 0)
        GLES30.glEnableVertexAttribArray(0)
        // Normal: 3 floats, stride 32, offset 12
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 32, 12)
        GLES30.glEnableVertexAttribArray(1)
        // Bind index buffer
        if (mesh.ebo != 0) {
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.ebo)
        }
        // Bind instance VBO for per-instance attributes
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
        val stride = NODE_INSTANCE_FLOATS * 4 // 40 bytes
        // loc 2: instancePos (3 floats)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribDivisor(2, 1)
        // loc 3: instanceColor (4 floats)
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribDivisor(3, 1)
        // loc 4: instanceRadius (1 float)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, stride, 28)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribDivisor(4, 1)
        // loc 5: instanceEnergy (1 float)
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, stride, 32)
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribDivisor(5, 1)
        // loc 6: instancePhase (1 float)
        GLES30.glVertexAttribPointer(6, 1, GLES30.GL_FLOAT, false, stride, 36)
        GLES30.glEnableVertexAttribArray(6)
        GLES30.glVertexAttribDivisor(6, 1)
        GLES30.glBindVertexArray(0)
    }
    private fun setupBeamGeometry() {
        // Generate cylinder quad strip template
        // Each vertex: (t, radialAngle, 0) where t=[0,1] along beam, radialAngle=[0,1]
        val verts = mutableListOf<Float>()
        for (i in 0..BEAM_LENGTH_SEGMENTS) {
            val t = i.toFloat() / BEAM_LENGTH_SEGMENTS
            for (j in 0..BEAM_RADIAL_SEGMENTS) {
                val r = j.toFloat() / BEAM_RADIAL_SEGMENTS
                verts.add(t); verts.add(r); verts.add(0f)
            }
        }
        val vertArray = verts.toFloatArray()
        beamVertexCount = vertArray.size / 3
        // Create beam base VBO
        val vbos = IntArray(2)
        GLES30.glGenBuffers(2, vbos, 0)
        beamBaseVbo = vbos[0]
        beamInstanceVbo = vbos[1]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, beamBaseVbo)
        val buf = ByteBuffer.allocateDirect(vertArray.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(vertArray).flip()
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertArray.size * 4, buf, GLES30.GL_STATIC_DRAW)
        // Instance buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, beamInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
            NeuralLattice.MAX_CONNECTIONS * BEAM_INSTANCE_FLOATS * 4,
            null, GLES30.GL_DYNAMIC_DRAW)
        // Beam VAO
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        beamVao = vaos[0]
        GLES30.glBindVertexArray(beamVao)
        // loc 0: base vertex (t, r, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, beamBaseVbo)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 12, 0)
        GLES30.glEnableVertexAttribArray(0)
        // Instance attrs
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, beamInstanceVbo)
        val bs = BEAM_INSTANCE_FLOATS * 4
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
        // loc 5: beamWidth (1)
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, bs, 32)
        GLES30.glEnableVertexAttribArray(5); GLES30.glVertexAttribDivisor(5, 1)
        GLES30.glBindVertexArray(0)
    }
    /**
     * Upload lattice state to GPU instance buffers.
     * Called once per frame. Uses glBufferSubData (no reallocation).
     */
    fun uploadInstanceData(lattice: NeuralLattice) {
        if (!isInitialized) return
        // Nodes
        nodeInstanceData.clear()
        var nodeCount = 0
        for (n in lattice.nodes) {
            if (!n.active) continue
            nodeInstanceData.put(n.position[0]); nodeInstanceData.put(n.position[1]); nodeInstanceData.put(n.position[2])
            nodeInstanceData.put(n.color[0]); nodeInstanceData.put(n.color[1]); nodeInstanceData.put(n.color[2]); nodeInstanceData.put(n.color[3])
            nodeInstanceData.put(n.radius); nodeInstanceData.put(n.energy); nodeInstanceData.put(n.phase)
            nodeCount++
        }
        nodeInstanceData.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeInstanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, nodeCount * NODE_INSTANCE_FLOATS * 4, nodeInstanceData)
        // Beams
        beamInstanceData.clear()
        var beamCount = 0
        for (c in lattice.connections) {
            if (!c.active) continue
            val from = lattice.nodes[c.fromIndex]; val to = lattice.nodes[c.toIndex]
            if (!from.active || !to.active) continue
            beamInstanceData.put(from.position[0]); beamInstanceData.put(from.position[1]); beamInstanceData.put(from.position[2])
            beamInstanceData.put(to.position[0]); beamInstanceData.put(to.position[1]); beamInstanceData.put(to.position[2])
            beamInstanceData.put(c.strength); beamInstanceData.put(c.pulsePhase); beamInstanceData.put(1f)
            beamCount++
        }
        beamInstanceData.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, beamInstanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, beamCount * BEAM_INSTANCE_FLOATS * 4, beamInstanceData)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
    /**
     * Draw all nodes (single instanced draw call).
     */
    fun drawNodes(camera: CameraController, state: RenderState, activeCount: Int) {
        val prog = nodeProgram ?: return
        if (activeCount <= 0) return
        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", state.elapsedTime)
        prog.setVec3("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
        val mesh = nodeMesh ?: return
        GLES30.glBindVertexArray(nodeVao)
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0, activeCount
        )
        GLES30.glBindVertexArray(0)
    }
    /**
     * Draw all connection beams (single instanced draw call).
     */
    fun drawBeams(camera: CameraController, state: RenderState, metrics: AIMetricsSnapshot, activeCount: Int) {
        val prog = beamProgram ?: return
        if (activeCount <= 0) return
        GLES30.glDepthMask(false)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", state.elapsedTime)
        prog.setFloat("uCognitiveLoad", metrics.cognitiveLoad)
        GLES30.glBindVertexArray(beamVao)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, beamVertexCount, activeCount)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }
    fun release() {
        nodeProgram?.release(); nodeProgram = null
        beamProgram?.release(); beamProgram = null
        nodeMesh?.release(); nodeMesh = null
        if (nodeVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(nodeVao), 0); nodeVao = 0 }
        if (nodeInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(nodeInstanceVbo), 0); nodeInstanceVbo = 0 }
        if (beamVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(beamVao), 0); beamVao = 0 }
        if (beamBaseVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(beamBaseVbo), 0); beamBaseVbo = 0 }
        if (beamInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(beamInstanceVbo), 0); beamInstanceVbo = 0 }
        isInitialized = false
        Timber.d("NeuralLatticeRenderer: released")
    }
}
