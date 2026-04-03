package com.aihos.ui.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MeshGenerator — Procedural mesh factory for the cinematic pipeline.
 *
 * Generates:
 *   1. Icosphere   — Subdivided icosahedron (AI brain core)
 *   2. Grid plane  — For ground/reflection effects
 *   3. Particle cloud — Enhanced particles with size attribute
 *   4. Full-screen quad — For post-processing passes
 *
 * All geometry is uploaded to VAO/VBO/EBO on creation.
 * No per-frame allocation.
 */
object MeshGenerator {

    /**
     * Holds GPU handles for a mesh. Release via [release].
     */
    data class MeshData(
        val vao: Int,
        val vbo: Int,
        val ebo: Int,           // 0 if no index buffer
        val vertexCount: Int,
        val indexCount: Int,     // 0 if drawing with glDrawArrays
        val drawMode: Int = GLES30.GL_TRIANGLES
    ) {
        fun release() {
            if (vao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
            if (vbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
            if (ebo != 0) GLES30.glDeleteBuffers(1, intArrayOf(ebo), 0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. ICOSPHERE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate an icosphere with [subdivisions] levels.
     * Vertex format: [x, y, z, nx, ny, nz, u, v] — 8 floats/vertex
     *
     * @param subdivisions 0 = icosahedron (12 verts), 1 = 42 verts, 2 = 162 verts, 3 = 642 verts
     * @param radius sphere radius
     */
    fun generateIcosphere(subdivisions: Int = 2, radius: Float = 1.0f): MeshData {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()

        // Base icosahedron vertices (12 vertices)
        val baseVerts = mutableListOf(
            floatArrayOf(-1f, t, 0f), floatArrayOf(1f, t, 0f),
            floatArrayOf(-1f, -t, 0f), floatArrayOf(1f, -t, 0f),
            floatArrayOf(0f, -1f, t), floatArrayOf(0f, 1f, t),
            floatArrayOf(0f, -1f, -t), floatArrayOf(0f, 1f, -t),
            floatArrayOf(t, 0f, -1f), floatArrayOf(t, 0f, 1f),
            floatArrayOf(-t, 0f, -1f), floatArrayOf(-t, 0f, 1f)
        )

        // Normalize to radius
        for (v in baseVerts) {
            val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            v[0] = v[0] / len * radius
            v[1] = v[1] / len * radius
            v[2] = v[2] / len * radius
        }

        // Base icosahedron faces (20 triangles)
        var faces = mutableListOf(
            intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
            intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
            intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
            intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
        )

        val vertices = baseVerts.toMutableList()

        // Subdivision
        val midpointCache = HashMap<Long, Int>()

        fun midpoint(a: Int, b: Int): Int {
            val key = if (a < b) a.toLong() * 65536 + b else b.toLong() * 65536 + a
            midpointCache[key]?.let { return it }

            val va = vertices[a]
            val vb = vertices[b]
            val mid = floatArrayOf(
                (va[0] + vb[0]) / 2f,
                (va[1] + vb[1]) / 2f,
                (va[2] + vb[2]) / 2f
            )
            // Project onto sphere
            val len = sqrt(mid[0] * mid[0] + mid[1] * mid[1] + mid[2] * mid[2])
            mid[0] = mid[0] / len * radius
            mid[1] = mid[1] / len * radius
            mid[2] = mid[2] / len * radius

            val idx = vertices.size
            vertices.add(mid)
            midpointCache[key] = idx
            return idx
        }

        for (_sub in 0 until subdivisions) {
            val newFaces = mutableListOf<IntArray>()
            midpointCache.clear()
            for (face in faces) {
                val a = midpoint(face[0], face[1])
                val b = midpoint(face[1], face[2])
                val c = midpoint(face[2], face[0])
                newFaces.add(intArrayOf(face[0], a, c))
                newFaces.add(intArrayOf(face[1], b, a))
                newFaces.add(intArrayOf(face[2], c, b))
                newFaces.add(intArrayOf(a, b, c))
            }
            faces = newFaces
        }

        // Build interleaved vertex buffer: pos(3) + normal(3) + uv(2) = 8 floats
        val floatsPerVert = 8
        val vertData = FloatArray(vertices.size * floatsPerVert)
        for (i in vertices.indices) {
            val v = vertices[i]
            val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            val nx = v[0] / len
            val ny = v[1] / len
            val nz = v[2] / len

            // Spherical UV mapping
            val u = (0.5 + kotlin.math.atan2(nz.toDouble(), nx.toDouble()) / (2.0 * PI)).toFloat()
            val vCoord = (0.5 - kotlin.math.asin(ny.toDouble()) / PI).toFloat()

            val off = i * floatsPerVert
            vertData[off + 0] = v[0]
            vertData[off + 1] = v[1]
            vertData[off + 2] = v[2]
            vertData[off + 3] = nx
            vertData[off + 4] = ny
            vertData[off + 5] = nz
            vertData[off + 6] = u
            vertData[off + 7] = vCoord
        }

        // Index buffer
        val indexData = ShortArray(faces.size * 3)
        for (i in faces.indices) {
            indexData[i * 3 + 0] = faces[i][0].toShort()
            indexData[i * 3 + 1] = faces[i][1].toShort()
            indexData[i * 3 + 2] = faces[i][2].toShort()
        }

        return uploadMesh(vertData, indexData, floatsPerVert, hasNormals = true, hasUVs = true)
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. FULL-SCREEN QUAD (for post-processing)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate a full-screen quad for post-processing.
     * Vertex format: [x, y, u, v] — 4 floats/vertex
     * Two triangles covering NDC [-1,1].
     */
    fun generateFullScreenQuad(): MeshData {
        val data = floatArrayOf(
            // position(2) + texcoord(2)
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f
        )

        val vertBuf = createFloatBuffer(data)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        val vao = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        val vbo = vbos[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)

        val stride = 4 * 4  // 4 floats * 4 bytes

        // location 0 — position (vec2)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)

        // location 1 — texcoord (vec2)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 2 * 4)

        GLES30.glBindVertexArray(0)

        return MeshData(
            vao = vao, vbo = vbo, ebo = 0,
            vertexCount = 4, indexCount = 0,
            drawMode = GLES30.GL_TRIANGLE_STRIP
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. PARTICLE CLOUD (Enhanced)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate a particle cloud with per-particle attributes.
     * Vertex format: [x, y, z, phase, speed, size] — 6 floats/particle
     */
    fun generateParticleCloud(count: Int = 1200): MeshData {
        val floatsPerParticle = 6
        val data = FloatArray(count * floatsPerParticle)

        for (i in 0 until count) {
            val off = i * floatsPerParticle
            // Golden ratio distribution for even coverage
            val golden = (i * 0.6180339887498949f) % 1f
            val angle = golden * 2f * PI.toFloat()
            val r = 0.1f + (i.toFloat() / count) * 0.9f

            // Layered shells for depth
            val shell = (i % 3).toFloat()
            val shellRadius = r * (0.6f + shell * 0.2f)

            data[off + 0] = cos(angle) * shellRadius          // x
            data[off + 1] = sin(angle) * shellRadius           // y
            data[off + 2] = (golden - 0.5f) * 0.5f + sin(shell * 2.1f) * 0.2f  // z
            data[off + 3] = golden * 2f * PI.toFloat()         // phase
            data[off + 4] = 0.2f + golden * 1.5f              // speed
            data[off + 5] = 0.5f + (1f - golden) * 1.0f       // size
        }

        val vertBuf = createFloatBuffer(data)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        val vao = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        val vbo = vbos[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)

        val stride = floatsPerParticle * 4

        // location 0 — position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // location 1 — phase (float)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // location 2 — speed (float)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 4 * 4)

        // location 3 — size (float)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, 5 * 4)

        GLES30.glBindVertexArray(0)

        return MeshData(
            vao = vao, vbo = vbo, ebo = 0,
            vertexCount = count, indexCount = 0,
            drawMode = GLES30.GL_POINTS
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Internal: Upload interleaved mesh with index buffer
    // ═══════════════════════════════════════════════════════════════

    private fun uploadMesh(
        vertData: FloatArray,
        indexData: ShortArray,
        floatsPerVert: Int,
        hasNormals: Boolean,
        hasUVs: Boolean
    ): MeshData {
        val vertBuf = createFloatBuffer(vertData)
        val idxBuf = createShortBuffer(indexData)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        val vao = vaos[0]

        val buffers = IntArray(2)
        GLES30.glGenBuffers(2, buffers, 0)
        val vbo = buffers[0]
        val ebo = buffers[1]

        GLES30.glBindVertexArray(vao)

        // VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertData.size * 4, vertBuf, GLES30.GL_STATIC_DRAW)

        // EBO
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexData.size * 2, idxBuf, GLES30.GL_STATIC_DRAW)

        val stride = floatsPerVert * 4
        var offset = 0

        // location 0 — position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, offset)
        offset += 3 * 4

        // location 1 — normal (vec3)
        if (hasNormals) {
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, offset)
            offset += 3 * 4
        }

        // location 2 — texcoord (vec2)
        if (hasUVs) {
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, offset)
        }

        GLES30.glBindVertexArray(0)

        return MeshData(
            vao = vao, vbo = vbo, ebo = ebo,
            vertexCount = vertData.size / floatsPerVert,
            indexCount = indexData.size,
            drawMode = GLES30.GL_TRIANGLES
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Buffer helpers (called once at init, not per frame)
    // ═══════════════════════════════════════════════════════════════

    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
            .also { it.position(0) }
    }

    private fun createShortBuffer(data: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(data.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(data)
            .also { it.position(0) }
    }
}

