package com.aihos.ui.render.universe

import android.opengl.GLES30
import com.aihos.ui.gl.MeshGenerator
import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.AIMetricsSnapshot
import com.aihos.ui.render.core.QualityLevel
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * ProceduralUniverseRenderer — GPU buffer management and draw calls
 * for the procedural cosmic environment.
 *
 * Architecture:
 * ┌──────────────────────────────────────────────────────────────┐
 * │                ProceduralUniverseRenderer                    │
 * │                                                              │
 * │  ┌──────────────────┐  ┌─────────────────────────────────┐  │
 * │  │  Starfield Layer  │  │  Nebula Cloud Layer (FS quad)   │  │
 * │  │  (instanced pts)  │  │  (fractal noise + distortion)   │  │
 * │  └──────────────────┘  └─────────────────────────────────┘  │
 * │                                                              │
 * │  ┌──────────────────┐  ┌─────────────────────────────────┐  │
 * │  │  Ambient Particles│  │  Energy Field (FS quad overlay) │  │
 * │  │  (instanced pts)  │  │  (radial distortion + ripple)   │  │
 * │  └──────────────────┘  └─────────────────────────────────┘  │
 * └──────────────────────────────────────────────────────────────┘
 *
 * All buffers are pre-allocated at initialization time.
 * Zero allocation inside render loop.
 *
 * Parallax depth layers:
 *   Layer 0 (farthest): Nebula clouds
 *   Layer 1: Distant starfield
 *   Layer 2: Ambient particle field
 *   Layer 3 (nearest): Energy field around AI core
 */
class ProceduralUniverseRenderer(
    private val camera: CameraController
) {

    // ═══════════════════════════════════════════════════════════════
    // Configuration
    // ═══════════════════════════════════════════════════════════════

    companion object {
        /** Maximum stars for HIGH quality. Scaled down for lower tiers. */
        const val MAX_STARS_HIGH = 3000
        const val MAX_STARS_MED = 1500
        const val MAX_STARS_LOW = 800

        /** Maximum ambient particles. */
        const val MAX_AMBIENT_HIGH = 600
        const val MAX_AMBIENT_MED = 300
        const val MAX_AMBIENT_LOW = 150

        /** Floats per star: x,y,z, brightness, colorTemp, twinklePhase */
        const val STAR_FLOATS = 6

        /** Floats per ambient particle: x,y,z, phase, speed, size, depth */
        const val AMBIENT_FLOATS = 7
    }

    // ═══════════════════════════════════════════════════════════════
    // Shader programs
    // ═══════════════════════════════════════════════════════════════

    private var starfieldProgram: ShaderProgram? = null
    private var nebulaProgram: ShaderProgram? = null
    private var ambientProgram: ShaderProgram? = null
    private var energyProgram: ShaderProgram? = null

    // ═══════════════════════════════════════════════════════════════
    // GPU Buffers
    // ═══════════════════════════════════════════════════════════════

    // Starfield
    private var starVAO = 0
    private var starVBO = 0
    private var starCount = MAX_STARS_HIGH

    // Ambient particles
    private var ambientVAO = 0
    private var ambientVBO = 0
    private var ambientCount = MAX_AMBIENT_HIGH

    // Full-screen quad (shared for nebula + energy)
    private var fsQuad: MeshGenerator.MeshData? = null

    // ═══════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════

    private var isInitialized = false
    private var screenWidth = 1
    private var screenHeight = 1
    private var currentQuality = QualityLevel.HIGH
    private var nebulaEnabled = true

    /** Nebula intensity [0,1] — reduced under performance pressure. */
    var nebulaIntensity = 1.0f
        private set

    // Pre-allocated camera offset (avoids allocation per frame)
    private val cameraOffset = FloatArray(3)

    // ═══════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════

    fun initialize() {
        // Compile shaders
        starfieldProgram = ShaderProgram(
            ProceduralUniverseShaders.STARFIELD_VERTEX,
            ProceduralUniverseShaders.STARFIELD_FRAGMENT
        )
        nebulaProgram = ShaderProgram(
            ProceduralUniverseShaders.NEBULA_VERTEX,
            ProceduralUniverseShaders.NEBULA_FRAGMENT
        )
        ambientProgram = ShaderProgram(
            ProceduralUniverseShaders.AMBIENT_PARTICLE_VERTEX,
            ProceduralUniverseShaders.AMBIENT_PARTICLE_FRAGMENT
        )
        energyProgram = ShaderProgram(
            ProceduralUniverseShaders.ENERGY_FIELD_VERTEX,
            ProceduralUniverseShaders.ENERGY_FIELD_FRAGMENT
        )

        // Generate geometry
        generateStarfield(MAX_STARS_HIGH)
        generateAmbientParticles(MAX_AMBIENT_HIGH)
        fsQuad = MeshGenerator.generateFullScreenQuad()

        isInitialized = true
        Timber.d("ProceduralUniverseRenderer: initialized ($starCount stars, $ambientCount ambient particles)")
    }

    // ═══════════════════════════════════════════════════════════════
    // Starfield Generation
    // ═══════════════════════════════════════════════════════════════

    private fun generateStarfield(count: Int) {
        starCount = count
        releaseBuffer(starVAO, starVBO)

        val rng = Random(42) // deterministic for reproducibility
        val data = FloatArray(count * STAR_FLOATS)

        for (i in 0 until count) {
            val off = i * STAR_FLOATS

            // Distribute stars in a large sphere using Fibonacci sphere
            val golden = (1.0 + sqrt(5.0)) / 2.0
            val theta = 2.0 * PI * i / golden
            val phi = kotlin.math.acos(1.0 - 2.0 * (i + 0.5) / count)

            val r = 0.5f + rng.nextFloat() * 0.5f // radius variation
            data[off + 0] = (sin(phi) * cos(theta) * r).toFloat()  // x
            data[off + 1] = (sin(phi) * sin(theta) * r).toFloat()  // y
            data[off + 2] = (cos(phi) * r).toFloat()               // z

            // Brightness: power distribution (more dim stars, fewer bright ones)
            val brightness = rng.nextFloat()
            data[off + 3] = brightness * brightness // quadratic: more dim than bright

            // Color temperature [0=warm, 1=cool]
            data[off + 4] = rng.nextFloat()

            // Twinkle phase
            data[off + 5] = rng.nextFloat()
        }

        val result = uploadPointBuffer(data, STAR_FLOATS, listOf(3, 1, 1, 1))
        starVAO = result.first
        starVBO = result.second
    }

    // ═══════════════════════════════════════════════════════════════
    // Ambient Particle Generation
    // ═══════════════════════════════════════════════════════════════

    private fun generateAmbientParticles(count: Int) {
        ambientCount = count
        releaseBuffer(ambientVAO, ambientVBO)

        val rng = Random(123)
        val data = FloatArray(count * AMBIENT_FLOATS)

        for (i in 0 until count) {
            val off = i * AMBIENT_FLOATS

            // Distribute in cylindrical shells
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val radius = 0.1f + rng.nextFloat() * 0.8f
            val height = (rng.nextFloat() - 0.5f) * 0.6f

            data[off + 0] = cos(angle) * radius  // x
            data[off + 1] = sin(angle) * radius  // y
            data[off + 2] = height                // z
            data[off + 3] = rng.nextFloat()       // phase
            data[off + 4] = 0.3f + rng.nextFloat() * 1.2f  // speed
            data[off + 5] = 0.3f + rng.nextFloat() * 1.0f  // size
            data[off + 6] = rng.nextFloat()       // depth [0=near, 1=far]
        }

        val result = uploadPointBuffer(data, AMBIENT_FLOATS, listOf(3, 1, 1, 1, 1))
        ambientVAO = result.first
        ambientVBO = result.second
    }

    // ═══════════════════════════════════════════════════════════════
    // Buffer Upload Utility
    // ═══════════════════════════════════════════════════════════════

    /**
     * Upload a point-based vertex buffer with variable attribute layout.
     * @param data raw float data
     * @param floatsPerVertex total floats per vertex
     * @param attribSizes list of component counts per attribute (e.g., [3, 1, 1, 1])
     * @return Pair(vao, vbo)
     */
    private fun uploadPointBuffer(
        data: FloatArray,
        floatsPerVertex: Int,
        attribSizes: List<Int>
    ): Pair<Int, Int> {
        val buf: FloatBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        buf.position(0)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        val vao = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        val vbo = vbos[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buf, GLES30.GL_STATIC_DRAW)

        val stride = floatsPerVertex * 4
        var offset = 0
        for ((loc, size) in attribSizes.withIndex()) {
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribPointer(loc, size, GLES30.GL_FLOAT, false, stride, offset)
            offset += size * 4
        }

        GLES30.glBindVertexArray(0)
        return Pair(vao, vbo)
    }

    private fun releaseBuffer(vao: Int, vbo: Int) {
        if (vao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        if (vbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
    }

    // ═══════════════════════════════════════════════════════════════
    // Resize / Quality Scaling
    // ═══════════════════════════════════════════════════════════════

    fun resize(width: Int, height: Int, quality: QualityLevel) {
        screenWidth = width
        screenHeight = height

        if (quality != currentQuality) {
            currentQuality = quality
            // Scale particle counts based on quality
            val targetStars = when (quality) {
                QualityLevel.HIGH -> MAX_STARS_HIGH
                QualityLevel.MEDIUM -> MAX_STARS_MED
                QualityLevel.LOW -> MAX_STARS_LOW
            }
            val targetAmbient = when (quality) {
                QualityLevel.HIGH -> MAX_AMBIENT_HIGH
                QualityLevel.MEDIUM -> MAX_AMBIENT_MED
                QualityLevel.LOW -> MAX_AMBIENT_LOW
            }

            if (targetStars != starCount) generateStarfield(targetStars)
            if (targetAmbient != ambientCount) generateAmbientParticles(targetAmbient)

            nebulaEnabled = quality != QualityLevel.LOW
            nebulaIntensity = when (quality) {
                QualityLevel.HIGH -> 1.0f
                QualityLevel.MEDIUM -> 0.7f
                QualityLevel.LOW -> 0.0f
            }

            Timber.d("ProceduralUniverseRenderer: quality=$quality stars=$starCount ambient=$ambientCount nebula=$nebulaEnabled")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Render Methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Render the nebula background layer (FIRST — behind everything).
     * Should be called before any other geometry.
     */
    fun renderNebulaBackground(
        elapsedTime: Float,
        aiMetrics: AIMetricsSnapshot
    ) {
        if (!isInitialized || !nebulaEnabled) return
        val prog = nebulaProgram ?: return
        if (!prog.isValid) return

        // Disable depth write for background
        GLES30.glDepthMask(false)

        prog.use()
        prog.setFloat("uTime", elapsedTime)
        prog.setFloat("uCognitiveLoad", aiMetrics.cognitiveLoad)
        prog.setFloat("uConfidence", aiMetrics.confidence)
        prog.setFloat("uEvolutionRate", aiMetrics.evolutionRate)
        prog.setFloat("uMemoryActivity", aiMetrics.memoryLoad)
        prog.setVec2("uResolution", screenWidth.toFloat(), screenHeight.toFloat())
        prog.setFloat("uNebulaIntensity", nebulaIntensity)

        // Camera offset for parallax
        cameraOffset[0] = camera.position[0]
        cameraOffset[1] = camera.position[1]
        cameraOffset[2] = camera.position[2]
        prog.setVec3("uCameraOffset", cameraOffset[0], cameraOffset[1], cameraOffset[2])

        drawFullScreenQuad()
        prog.unuse()

        GLES30.glDepthMask(true)
    }

    /**
     * Render the starfield layer (behind scene, after nebula).
     */
    fun renderStarfield(
        elapsedTime: Float,
        aiMetrics: AIMetricsSnapshot
    ) {
        if (!isInitialized) return
        val prog = starfieldProgram ?: return
        if (!prog.isValid) return

        // Additive blending for stars
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", elapsedTime)
        prog.setFloat("uParallaxFactor", 0.5f)
        prog.setFloat("uCognitiveLoad", aiMetrics.cognitiveLoad)
        prog.setFloat("uAnimationIntensity", aiMetrics.animationIntensity)

        GLES30.glBindVertexArray(starVAO)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, starCount)
        GLES30.glBindVertexArray(0)

        prog.unuse()

        // Restore standard blending
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)
    }

    /**
     * Render ambient particle field (mid-depth, around AI core area).
     */
    fun renderAmbientParticles(
        elapsedTime: Float,
        aiMetrics: AIMetricsSnapshot
    ) {
        if (!isInitialized) return
        val prog = ambientProgram ?: return
        if (!prog.isValid) return

        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)

        prog.use()
        prog.setMat4("uView", camera.viewMatrix)
        prog.setMat4("uProjection", camera.projectionMatrix)
        prog.setFloat("uTime", elapsedTime)
        prog.setFloat("uCognitiveLoad", aiMetrics.cognitiveLoad)
        prog.setFloat("uMemoryActivity", aiMetrics.memoryLoad)
        prog.setFloat("uAnimationIntensity", aiMetrics.animationIntensity)
        prog.setFloat("uParallaxFactor", 0.7f)

        GLES30.glBindVertexArray(ambientVAO)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, ambientCount)
        GLES30.glBindVertexArray(0)

        prog.unuse()

        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)
    }

    /**
     * Render energy field around AI core (overlay, after main scene).
     */
    fun renderEnergyField(
        elapsedTime: Float,
        aiMetrics: AIMetricsSnapshot
    ) {
        if (!isInitialized) return
        val prog = energyProgram ?: return
        if (!prog.isValid) return

        // Energy field is a subtle overlay
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        prog.use()
        prog.setFloat("uTime", elapsedTime)
        prog.setFloat("uCognitiveLoad", aiMetrics.cognitiveLoad)
        prog.setFloat("uEvolutionRate", aiMetrics.evolutionRate)
        prog.setFloat("uConfidence", aiMetrics.confidence)
        prog.setVec2("uResolution", screenWidth.toFloat(), screenHeight.toFloat())

        // AI core is at screen center
        prog.setVec2("uCoreScreenPos", 0.5f, 0.5f)

        drawFullScreenQuad()
        prog.unuse()

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)
    }

    // ═══════════════════════════════════════════════════════════════
    // Full-Screen Quad Draw
    // ═══════════════════════════════════════════════════════════════

    private fun drawFullScreenQuad() {
        val q = fsQuad ?: return
        GLES30.glBindVertexArray(q.vao)
        if (q.indexCount > 0) {
            GLES30.glDrawElements(q.drawMode, q.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        } else {
            GLES30.glDrawArrays(q.drawMode, 0, q.vertexCount)
        }
        GLES30.glBindVertexArray(0)
    }

    // ═══════════════════════════════════════════════════════════════
    // Performance Control
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dynamic intensity scaling. Called by performance monitor when FPS drops.
     * @param fps current frames per second
     */
    fun adjustForPerformance(fps: Float) {
        nebulaIntensity = when {
            fps < 30f -> 0.0f   // disable nebula entirely
            fps < 45f -> 0.5f   // reduce nebula
            fps < 55f -> 0.8f
            else -> when (currentQuality) {
                QualityLevel.HIGH -> 1.0f
                QualityLevel.MEDIUM -> 0.7f
                QualityLevel.LOW -> 0.0f
            }
        }
        nebulaEnabled = nebulaIntensity > 0.01f
    }

    // ═══════════════════════════════════════════════════════════════
    // Release
    // ═══════════════════════════════════════════════════════════════

    fun release() {
        releaseBuffer(starVAO, starVBO)
        starVAO = 0; starVBO = 0

        releaseBuffer(ambientVAO, ambientVBO)
        ambientVAO = 0; ambientVBO = 0

        fsQuad?.release()
        fsQuad = null

        // Shader programs are garbage collected (handles freed by GL context destruction)
        starfieldProgram = null
        nebulaProgram = null
        ambientProgram = null
        energyProgram = null

        isInitialized = false
        Timber.d("ProceduralUniverseRenderer: released")
    }
}

