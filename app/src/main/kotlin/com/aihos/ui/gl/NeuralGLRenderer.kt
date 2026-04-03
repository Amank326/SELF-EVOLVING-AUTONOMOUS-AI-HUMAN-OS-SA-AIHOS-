package com.aihos.ui.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * NeuralGLRenderer — OpenGL ES 3.0 neural-network particle visualization.
 *
 * Renders a field of animated point-sprite particles that simulate
 * neural synaptic activity.  AI metrics (confidence, memory load, etc.)
 * are fed in at runtime and alter the visual output in real-time:
 *
 *   • confidence   → particle brightness & pulse speed
 *   • memoryLoad   → color shift (cyan → magenta when overloaded)
 *   • autonomy     → rotation speed of the entire field
 *   • systemHealth → particle count visible (fades out when unhealthy)
 *
 * All shaders are GLSL ES 3.00.  No external textures required.
 */
class NeuralGLRenderer : GLSurfaceView.Renderer {

    // ── AI metrics fed from the outside (thread-safe volatile) ──────────
    @Volatile var confidence: Float = 0.5f
    @Volatile var memoryLoad: Float = 0.3f
    @Volatile var autonomyLevel: Float = 0.5f
    @Volatile var systemHealth: Float = 0.8f

    // ── GL handles ──────────────────────────────────────────────────────
    private var program = 0
    private var vaoHandle = 0
    private var vboHandle = 0

    // uniform locations
    private var uTimeLoc = -1
    private var uResolutionLoc = -1
    private var uConfidenceLoc = -1
    private var uMemoryLoadLoc = -1
    private var uAutonomyLoc = -1
    private var uHealthLoc = -1

    // ── Particle data ───────────────────────────────────────────────────
    companion object {
        private const val PARTICLE_COUNT = 800
        /** Floats per particle: x, y, z, phase, speed */
        private const val FLOATS_PER_PARTICLE = 5
    }

    private lateinit var particleBuffer: FloatBuffer
    private var startTimeNs = System.nanoTime()
    private var screenWidth = 1f
    private var screenHeight = 1f

    // ═══════════════════════════════════════════════════════════════════
    // GLSL ES 3.00 shaders
    // ═══════════════════════════════════════════════════════════════════

    private val vertexShaderSrc = """
        #version 300 es
        precision highp float;

        // per-particle attributes
        layout(location = 0) in vec3 aPosition;   // x, y, z  (NDC -1..1)
        layout(location = 1) in float aPhase;     // unique phase offset
        layout(location = 2) in float aSpeed;     // animation speed multiplier

        uniform float uTime;
        uniform vec2  uResolution;
        uniform float uConfidence;
        uniform float uAutonomy;
        uniform float uHealth;

        out float vAlpha;
        out float vPhase;

        void main() {
            float t = uTime * aSpeed;

            // orbital motion modulated by autonomy
            float angle = aPhase + t * (0.3 + uAutonomy * 0.7);
            float radius = length(aPosition.xy) * (0.6 + 0.4 * sin(t * 0.5 + aPhase));

            float x = cos(angle) * radius;
            float y = sin(angle) * radius;
            float z = aPosition.z + sin(t + aPhase * 3.0) * 0.15;

            // pulse alpha based on confidence
            float pulse = 0.5 + 0.5 * sin(t * 2.0 + aPhase * 6.28);
            vAlpha = mix(0.15, 1.0, uConfidence) * pulse * uHealth;
            vPhase = aPhase;

            // point size: bigger when healthy
            float basePt = mix(2.0, 6.0, uHealth);
            gl_PointSize = basePt + 2.0 * pulse * uConfidence;

            gl_Position = vec4(x, y * (uResolution.x / uResolution.y), z, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderSrc = """
        #version 300 es
        precision highp float;

        in float vAlpha;
        in float vPhase;

        uniform float uTime;
        uniform float uMemoryLoad;
        uniform float uConfidence;

        out vec4 fragColor;

        void main() {
            // soft circle mask from gl_PointCoord
            vec2 pc = gl_PointCoord - vec2(0.5);
            float dist = length(pc);
            if (dist > 0.5) discard;

            float soft = 1.0 - smoothstep(0.25, 0.5, dist);

            // color: cyan (#00F5FF) → magenta (#FF006E) as memoryLoad rises
            vec3 cCyan    = vec3(0.0, 0.96, 1.0);
            vec3 cMagenta = vec3(1.0, 0.0, 0.43);
            vec3 cGold    = vec3(1.0, 0.75, 0.0);

            // base color blend
            vec3 color = mix(cCyan, cMagenta, uMemoryLoad);

            // sprinkle some gold on high-confidence particles
            float goldMix = step(0.7, uConfidence) * (0.5 + 0.5 * sin(vPhase * 12.56 + uTime));
            color = mix(color, cGold, goldMix * 0.4);

            // glow
            float glow = soft * soft;
            float alpha = vAlpha * glow;

            fragColor = vec4(color * alpha, alpha);
        }
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════════
    // GLSurfaceView.Renderer callbacks
    // ═══════════════════════════════════════════════════════════════════

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Timber.d("NeuralGLRenderer: onSurfaceCreated  (ES 3.0)")

        GLES30.glClearColor(0f, 0f, 0f, 0f)          // transparent bg
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)  // additive
        // gl_PointSize is always enabled in ES 3.0 vertex shaders

        // ── compile program ─────────────────────────────────────────
        program = GLShaderUtil.createProgram(vertexShaderSrc, fragmentShaderSrc)
        if (program == 0) {
            Timber.e("NeuralGLRenderer: shader program creation FAILED")
            return
        }

        // ── resolve uniforms ────────────────────────────────────────
        uTimeLoc       = GLES30.glGetUniformLocation(program, "uTime")
        uResolutionLoc = GLES30.glGetUniformLocation(program, "uResolution")
        uConfidenceLoc = GLES30.glGetUniformLocation(program, "uConfidence")
        uMemoryLoadLoc = GLES30.glGetUniformLocation(program, "uMemoryLoad")
        uAutonomyLoc   = GLES30.glGetUniformLocation(program, "uAutonomy")
        uHealthLoc     = GLES30.glGetUniformLocation(program, "uHealth")

        // ── generate particle geometry ──────────────────────────────
        generateParticles()

        // ── upload to VAO / VBO ─────────────────────────────────────
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vaoHandle = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vboHandle = vbos[0]

        GLES30.glBindVertexArray(vaoHandle)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboHandle)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            particleBuffer.capacity() * 4,
            particleBuffer,
            GLES30.GL_STATIC_DRAW
        )

        val stride = FLOATS_PER_PARTICLE * 4  // bytes

        // location 0 — position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // location 1 — phase (float)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // location 2 — speed (float)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 4 * 4)

        GLES30.glBindVertexArray(0)

        startTimeNs = System.nanoTime()
        GLShaderUtil.checkGLError("onSurfaceCreated")
        Timber.d("NeuralGLRenderer: init complete — $PARTICLE_COUNT particles")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        GLES30.glViewport(0, 0, width, height)
        Timber.d("NeuralGLRenderer: surface ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        if (program == 0) return

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val elapsed = (System.nanoTime() - startTimeNs) / 1_000_000_000f

        GLES30.glUseProgram(program)

        // push uniforms
        GLES30.glUniform1f(uTimeLoc, elapsed)
        GLES30.glUniform2f(uResolutionLoc, screenWidth, screenHeight)
        GLES30.glUniform1f(uConfidenceLoc, confidence)
        GLES30.glUniform1f(uMemoryLoadLoc, memoryLoad)
        GLES30.glUniform1f(uAutonomyLoc, autonomyLevel)
        GLES30.glUniform1f(uHealthLoc, systemHealth)

        // draw
        GLES30.glBindVertexArray(vaoHandle)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, PARTICLE_COUNT)
        GLES30.glBindVertexArray(0)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Particle generation (deterministic: seeded by index)
    // ═══════════════════════════════════════════════════════════════════

    private fun generateParticles() {
        val data = FloatArray(PARTICLE_COUNT * FLOATS_PER_PARTICLE)
        for (i in 0 until PARTICLE_COUNT) {
            val offset = i * FLOATS_PER_PARTICLE
            // pseudo-random from golden ratio, no java.util.Random needed
            val golden = (i * 0.6180339887498949f) % 1f
            val angle = golden * 2f * Math.PI.toFloat()
            val radius = 0.1f + (i.toFloat() / PARTICLE_COUNT) * 0.85f

            data[offset + 0] = cos(angle) * radius          // x
            data[offset + 1] = sin(angle) * radius           // y
            data[offset + 2] = (golden - 0.5f) * 0.3f        // z
            data[offset + 3] = golden * 6.2831855f            // phase
            data[offset + 4] = 0.3f + golden * 1.2f           // speed
        }
        particleBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        particleBuffer.position(0)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════════

    fun release() {
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        if (vboHandle != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboHandle), 0)
            vboHandle = 0
        }
        if (vaoHandle != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoHandle), 0)
            vaoHandle = 0
        }
        Timber.d("NeuralGLRenderer: released")
    }
}

