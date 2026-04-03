package com.aihos.ui.render.hud
import android.opengl.GLES30
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.RenderState
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
/**
 * HUDRenderer — GPU instanced renderer for holographic HUD panels.
 *
 * Draws all panels in 2 passes (2 draw calls total):
 *   Pass 1: Glass panels (back-face then front-face for double-sided glass)
 *   Pass 2: Data overlays (front-face only)
 *
 * Per-instance data layout for glass:
 *   modelMatrix col0-3 (16 floats) + color (4 floats) + params (4 floats) = 24 floats
 *
 * Per-instance data layout for data overlay:
 *   modelMatrix col0-3 (16 floats) + params (4 floats) = 20 floats
 */
class HUDRenderer {
    companion object {
        private const val GLASS_INSTANCE_FLOATS = 24
        private const val DATA_INSTANCE_FLOATS = 20
    }
    private var glassProgram: ShaderProgram? = null
    private var dataProgram: ShaderProgram? = null
    // Quad geometry (shared by all panels)
    private var quadVao = 0
    private var quadVbo = 0
    private var quadEbo = 0
    // Instance buffers
    private var glassInstanceVbo = 0
    private var dataInstanceVbo = 0
    // CPU staging (pre-allocated)
    private val glassInstanceData = FloatBuffer.allocate(HUDManager.MAX_PANELS * GLASS_INSTANCE_FLOATS)
    private val dataInstanceData = FloatBuffer.allocate(HUDManager.MAX_PANELS * DATA_INSTANCE_FLOATS)
    private var isInitialized = false
    fun initialize() {
        glassProgram = ShaderProgram(HUDShaders.GLASS_VERTEX, HUDShaders.GLASS_FRAGMENT)
        dataProgram = ShaderProgram(HUDShaders.DATA_VERTEX, HUDShaders.DATA_FRAGMENT)
        setupQuadGeometry()
        setupInstanceBuffers()
        isInitialized = true
        Timber.d("HUDRenderer: initialized")
    }
    private fun setupQuadGeometry() {
        // Unit quad: position(3) + texcoord(2) = 5 floats per vertex
        val vertices = floatArrayOf(
            -0.5f,  0.5f, 0f, 0f, 1f,  // TL
             0.5f,  0.5f, 0f, 1f, 1f,  // TR
             0.5f, -0.5f, 0f, 1f, 0f,  // BR
            -0.5f, -0.5f, 0f, 0f, 0f   // BL
        )
        val indices = shortArrayOf(0, 2, 1, 0, 3, 2)
        val vaos = IntArray(1); GLES30.glGenVertexArrays(1, vaos, 0); quadVao = vaos[0]
        val vbos = IntArray(1); GLES30.glGenBuffers(1, vbos, 0); quadVbo = vbos[0]
        val ebos = IntArray(1); GLES30.glGenBuffers(1, ebos, 0); quadEbo = ebos[0]
        GLES30.glBindVertexArray(quadVao)
        // Vertex buffer
        val vBuf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(vertices).flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vBuf, GLES30.GL_STATIC_DRAW)
        // Position (loc 0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 20, 0)
        GLES30.glEnableVertexAttribArray(0)
        // TexCoord (loc 1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 20, 12)
        GLES30.glEnableVertexAttribArray(1)
        // Index buffer
        val iBuf = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        iBuf.put(indices).flip()
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, quadEbo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, iBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindVertexArray(0)
    }
    private fun setupInstanceBuffers() {
        val vbos = IntArray(2); GLES30.glGenBuffers(2, vbos, 0)
        glassInstanceVbo = vbos[0]; dataInstanceVbo = vbos[1]
        // Glass instance buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glassInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
            HUDManager.MAX_PANELS * GLASS_INSTANCE_FLOATS * 4, null, GLES30.GL_DYNAMIC_DRAW)
        // Data instance buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dataInstanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
            HUDManager.MAX_PANELS * DATA_INSTANCE_FLOATS * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
    fun uploadInstanceData(hud: HUDManager, time: Float) {
        if (!isInitialized) return
        glassInstanceData.clear()
        dataInstanceData.clear()
        var count = 0
        for (p in hud.panels) {
            if (!p.active) continue
            // Glass: model(16) + color(4) + params(4)
            glassInstanceData.put(p.modelMatrix)
            glassInstanceData.put(p.color)
            glassInstanceData.put(p.opacity)
            glassInstanceData.put(p.glowIntensity)
            glassInstanceData.put(p.dataValue)
            glassInstanceData.put(p.hoverAlpha)
            // Data: model(16) + params(4)
            dataInstanceData.put(p.modelMatrix)
            dataInstanceData.put(p.dataValue)
            dataInstanceData.put(p.panelType.ordinal.toFloat())
            dataInstanceData.put(time)
            dataInstanceData.put(p.hoverAlpha)
            count++
        }
        glassInstanceData.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glassInstanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * GLASS_INSTANCE_FLOATS * 4, glassInstanceData)
        dataInstanceData.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dataInstanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, count * DATA_INSTANCE_FLOATS * 4, dataInstanceData)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
    fun drawGlassPanels(camera: CameraController, state: RenderState, activeCount: Int) {
        val prog = glassProgram ?: return
        if (activeCount <= 0) return
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setVec3("uCameraPos", camera.position[0], camera.position[1], camera.position[2])
        prog.setFloat("uTime", state.elapsedTime)
        prog.setFloat("uScanlineEnabled", if (state.qualityLevel != com.aihos.ui.render.core.QualityLevel.LOW) 1f else 0f)
        bindGlassVAO()
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, 0, activeCount)
        GLES30.glBindVertexArray(0)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glDepthMask(true)
    }
    fun drawDataOverlays(camera: CameraController, state: RenderState, activeCount: Int) {
        val prog = dataProgram ?: return
        if (activeCount <= 0) return
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        bindDataVAO()
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, 0, activeCount)
        GLES30.glBindVertexArray(0)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)
    }
    private fun bindGlassVAO() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, glassInstanceVbo)
        val stride = GLASS_INSTANCE_FLOATS * 4
        // loc 2-5: model matrix columns
        for (col in 0 until 4) {
            val loc = 2 + col
            GLES30.glVertexAttribPointer(loc, 4, GLES30.GL_FLOAT, false, stride, col * 16)
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribDivisor(loc, 1)
        }
        // loc 6: color
        GLES30.glVertexAttribPointer(6, 4, GLES30.GL_FLOAT, false, stride, 64)
        GLES30.glEnableVertexAttribArray(6)
        GLES30.glVertexAttribDivisor(6, 1)
        // loc 7: params
        GLES30.glVertexAttribPointer(7, 4, GLES30.GL_FLOAT, false, stride, 80)
        GLES30.glEnableVertexAttribArray(7)
        GLES30.glVertexAttribDivisor(7, 1)
    }
    private fun bindDataVAO() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dataInstanceVbo)
        val stride = DATA_INSTANCE_FLOATS * 4
        for (col in 0 until 4) {
            val loc = 2 + col
            GLES30.glVertexAttribPointer(loc, 4, GLES30.GL_FLOAT, false, stride, col * 16)
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribDivisor(loc, 1)
        }
        // loc 6: params
        GLES30.glVertexAttribPointer(6, 4, GLES30.GL_FLOAT, false, stride, 64)
        GLES30.glEnableVertexAttribArray(6)
        GLES30.glVertexAttribDivisor(6, 1)
    }
    fun release() {
        glassProgram?.release(); glassProgram = null
        dataProgram?.release(); dataProgram = null
        if (quadVao != 0) { GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0); quadVao = 0 }
        if (quadVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0); quadVbo = 0 }
        if (quadEbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(quadEbo), 0); quadEbo = 0 }
        if (glassInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(glassInstanceVbo), 0); glassInstanceVbo = 0 }
        if (dataInstanceVbo != 0) { GLES30.glDeleteBuffers(1, intArrayOf(dataInstanceVbo), 0); dataInstanceVbo = 0 }
        isInitialized = false
        Timber.d("HUDRenderer: released")
    }
}
