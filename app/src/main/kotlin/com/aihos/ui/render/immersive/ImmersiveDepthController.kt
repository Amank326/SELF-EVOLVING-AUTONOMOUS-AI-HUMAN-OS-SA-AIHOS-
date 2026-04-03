package com.aihos.ui.render.immersive

import android.opengl.GLES30
import com.aihos.ui.render.camera.CameraController
import com.aihos.ui.render.core.QualityLevel
import timber.log.Timber

/**
 * ImmersiveDepthController — Master orchestrator for Immersive Depth Mode.
 *
 * Manages:
 *   - StereoCamera (dual-eye projection math)
 *   - GyroscopeTracker integration (head orientation → camera offset)
 *   - Parallax layer offsets
 *   - Immersive mode toggle with smooth transition
 *   - Performance auto-scaling
 *
 * ┌────────────────────────────────────────────────────────────────┐
 * │  ImmersiveDepthController                                      │
 * │                                                                │
 * │  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐   │
 * │  │ StereoCamera │  │ GyroTracker    │  │ ImmersiveConfig  │   │
 * │  │ (L/R eyes)   │  │ (sensor→ypr)   │  │ (parameters)     │   │
 * │  └──────┬───────┘  └───────┬────────┘  └──────┬───────────┘   │
 * │         │                  │                   │               │
 * │         ▼                  ▼                   ▼               │
 * │  ┌──────────────────────────────────────────────────────────┐  │
 * │  │  update()                                                 │  │
 * │  │  1. Read gyro orientation                                 │  │
 * │  │  2. Apply head-track to camera azimuth/elevation          │  │
 * │  │  3. Compute parallax offsets per layer                    │  │
 * │  │  4. Update stereo eye positions                           │  │
 * │  │  5. Store view/projection matrices for left/right         │  │
 * │  └──────────────────────────────────────────────────────────┘  │
 * │                                                                │
 * │  Outputs (read by GeometryPass per eye):                       │
 * │    leftViewMatrix, leftProjectionMatrix, leftPosition          │
 * │    rightViewMatrix, rightProjectionMatrix, rightPosition       │
 * │    parallaxOffsets[3] for background/mid/foreground             │
 * └────────────────────────────────────────────────────────────────┘
 *
 * Smooth transition:
 *   When toggling immersive on/off, a transitionAlpha spring
 *   interpolates from 0 → 1 (or back) over ~0.5s.
 *   At alpha=0, rendering is standard mono.
 *   At alpha=1, rendering is full stereo+parallax.
 */
class ImmersiveDepthController {

    val config = ImmersiveDepthConfig()
    val stereoCamera = StereoCamera()

    // Gyro tracker is set externally (needs Context)
    var gyroTracker: GyroscopeTracker? = null

    // ── Parallax offsets (read by each draw call) ────────────────
    /** Parallax offset for background layer. XY in world units. */
    val parallaxBackground = FloatArray(2)
    /** Parallax offset for midground layer. */
    val parallaxMidground = FloatArray(2)
    /** Parallax offset for foreground layer. */
    val parallaxForeground = FloatArray(2)

    // ── Head tracking output ─────────────────────────────────────
    var headYaw = 0f; private set
    var headPitch = 0f; private set

    // ── Transition ───────────────────────────────────────────────
    /** 0 = fully normal, 1 = fully immersive. Smooth transition. */
    var transitionAlpha = 0f
        private set
    private var targetAlpha = 0f

    // ── FBO management for dual-eye rendering ────────────────────
    var leftEyeFbo = 0; private set
    var leftEyeColorTex = 0; private set
    var leftEyeDepthTex = 0; private set
    var rightEyeFbo = 0; private set
    var rightEyeColorTex = 0; private set
    var rightEyeDepthTex = 0; private set
    var eyeFboWidth = 1; private set
    var eyeFboHeight = 1; private set

    private var fbosCreated = false


    // ═══════════════════════════════════════════════════════════════

    /**
     * Update immersive state. Called once per frame on GL thread.
     */
    @Suppress("UNUSED_PARAMETER")
    fun update(
        dt: Float,
        camera: CameraController,
        quality: QualityLevel
    ) {
        // Smooth transition
        targetAlpha = if (config.enabled) 1f else 0f
        val transitionSpeed = 3f // ~0.33 seconds
        transitionAlpha += (targetAlpha - transitionAlpha) * dt * transitionSpeed
        transitionAlpha = transitionAlpha.coerceIn(0f, 1f)
        if (transitionAlpha < 0.01f && !config.enabled) {
            transitionAlpha = 0f
            return // Skip all immersive logic when fully in normal mode
        }

        // Read gyroscope
        if (config.headTrackingEnabled) {
            val orientation = gyroTracker?.consumeOrientation()
            if (orientation != null) {
                headYaw = orientation.yaw * config.headTrackSensitivity
                headPitch = orientation.pitch * config.headTrackSensitivity
            }
        } else {
            headYaw = 0f; headPitch = 0f
        }

        // Compute parallax offsets from camera velocity (approximate via head track)
        if (config.parallaxEnabled) {
            val px = headYaw * transitionAlpha
            val py = headPitch * transitionAlpha
            parallaxBackground[0] = px * config.parallaxBackground
            parallaxBackground[1] = py * config.parallaxBackground
            parallaxMidground[0] = px * config.parallaxMidground
            parallaxMidground[1] = py * config.parallaxMidground
            parallaxForeground[0] = px * config.parallaxForeground
            parallaxForeground[1] = py * config.parallaxForeground
        } else {
            parallaxBackground[0] = 0f; parallaxBackground[1] = 0f
            parallaxMidground[0] = 0f; parallaxMidground[1] = 0f
            parallaxForeground[0] = 0f; parallaxForeground[1] = 0f
        }

        // Update stereo camera
        if (config.stereoEnabled) {
            stereoCamera.setProjectionParams(45f,
                if (eyeFboHeight > 0) eyeFboWidth.toFloat() / eyeFboHeight else 1f,
                0.1f, 100f)
            stereoCamera.update(camera.position, 0f, 0f, 0f, config)
        }
    }

    /**
     * Create or resize dual-eye FBOs.
     * Call from onSurfaceChanged.
     */
    fun resizeEyeFBOs(screenWidth: Int, screenHeight: Int, quality: QualityLevel) {
        val scale = config.stereoResolutionScale * quality.resolutionScale
        eyeFboWidth = (screenWidth * scale).toInt().coerceAtLeast(1)
        eyeFboHeight = (screenHeight * scale).toInt().coerceAtLeast(1)

        releaseEyeFBOs()
        createEyeFBO(true)  // left
        createEyeFBO(false) // right
        fbosCreated = true

        Timber.d("ImmersiveDepth: eye FBOs ${eyeFboWidth}x${eyeFboHeight}")
    }

    private fun createEyeFBO(isLeft: Boolean) {
        val fbos = IntArray(1); GLES30.glGenFramebuffers(1, fbos, 0)
        val texs = IntArray(1); GLES30.glGenTextures(1, texs, 0)

        // Color texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texs[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            eyeFboWidth, eyeFboHeight, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Depth renderbuffer
        val rbos = IntArray(1); GLES30.glGenRenderbuffers(1, rbos, 0)
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbos[0])
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24,
            eyeFboWidth, eyeFboHeight)

        // Assemble FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbos[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, texs[0], 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_RENDERBUFFER, rbos[0])

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.e("ImmersiveDepth: ${if (isLeft) "left" else "right"} eye FBO incomplete: 0x${Integer.toHexString(status)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        if (isLeft) {
            leftEyeFbo = fbos[0]; leftEyeColorTex = texs[0]; leftEyeDepthTex = rbos[0]
        } else {
            rightEyeFbo = fbos[0]; rightEyeColorTex = texs[0]; rightEyeDepthTex = rbos[0]
        }
    }

    /**
     * Bind the left eye FBO as render target.
     */
    fun bindLeftEye() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, leftEyeFbo)
        GLES30.glViewport(0, 0, eyeFboWidth, eyeFboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    }

    /**
     * Bind the right eye FBO as render target.
     */
    fun bindRightEye() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, rightEyeFbo)
        GLES30.glViewport(0, 0, eyeFboWidth, eyeFboHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    }

    fun releaseEyeFBOs() {
        if (leftEyeFbo != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(leftEyeFbo), 0); leftEyeFbo = 0 }
        if (leftEyeColorTex != 0) { GLES30.glDeleteTextures(1, intArrayOf(leftEyeColorTex), 0); leftEyeColorTex = 0 }
        if (leftEyeDepthTex != 0) { GLES30.glDeleteRenderbuffers(1, intArrayOf(leftEyeDepthTex), 0); leftEyeDepthTex = 0 }
        if (rightEyeFbo != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(rightEyeFbo), 0); rightEyeFbo = 0 }
        if (rightEyeColorTex != 0) { GLES30.glDeleteTextures(1, intArrayOf(rightEyeColorTex), 0); rightEyeColorTex = 0 }
        if (rightEyeDepthTex != 0) { GLES30.glDeleteRenderbuffers(1, intArrayOf(rightEyeDepthTex), 0); rightEyeDepthTex = 0 }
        fbosCreated = false
    }

    fun release() {
        releaseEyeFBOs()
    }

    /** True when immersive stereo should render both eyes. */
    val shouldRenderStereo: Boolean get() = transitionAlpha > 0.01f && config.stereoEnabled && fbosCreated
}

