package com.aihos.ui.render.core

import android.opengl.GLES30
import timber.log.Timber

/**
 * RenderPass — A single isolated stage in the render pipeline.
 *
 * Each pass:
 *   - Has its own responsibility (geometry, lighting, post-process, composite)
 *   - Manages its own GL state transitions
 *   - Reads from [RenderState] (immutable per-frame snapshot)
 *   - Is independently testable and replaceable
 *
 * Lifecycle:
 *   initialize()  — Called once on GL surface created
 *   resize()      — Called on surface size change
 *   execute()     — Called every frame
 *   release()     — Called on surface destroyed
 */
interface RenderPass {
    /** Human-readable name for logging. */
    val name: String

    /** One-time GL resource setup. Called on onSurfaceCreated. */
    fun initialize()

    /** Handle screen size change. Called on onSurfaceChanged. */
    fun resize(width: Int, height: Int, qualityLevel: QualityLevel)

    /** Execute this pass for the current frame. */
    fun execute(state: RenderState)

    /** Release all GL resources. Called on surface destroyed. */
    fun release()
}

/**
 * FBORenderPass — A render pass that renders into its own framebuffer.
 * Provides FBO lifecycle management and texture output for downstream passes.
 */
abstract class FBORenderPass : RenderPass {

    var fbo: Int = 0
        protected set
    var colorTexture: Int = 0
        protected set
    var depthRBO: Int = 0
        protected set
    var fboWidth: Int = 1
        protected set
    var fboHeight: Int = 1
        protected set

    /**
     * Create/recreate the FBO at the given resolution.
     * @param withDepth whether to attach a depth renderbuffer
     */
    protected fun createFBO(width: Int, height: Int, withDepth: Boolean = false) {
        releaseFBO()
        fboWidth = width.coerceAtLeast(1)
        fboHeight = height.coerceAtLeast(1)

        // Generate FBO
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fbo = fbos[0]

        // Color texture (RGBA16F for HDR)
        val texs = IntArray(1)
        GLES30.glGenTextures(1, texs, 0)
        colorTexture = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            fboWidth, fboHeight, 0, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, colorTexture, 0
        )

        if (withDepth) {
            val rbos = IntArray(1)
            GLES30.glGenRenderbuffers(1, rbos, 0)
            depthRBO = rbos[0]
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthRBO)
            GLES30.glRenderbufferStorage(
                GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, fboWidth, fboHeight
            )
            GLES30.glFramebufferRenderbuffer(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_RENDERBUFFER, depthRBO
            )
        }

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Timber.e("$name: FBO incomplete! status=0x${Integer.toHexString(status)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /** Bind this pass's FBO as render target. */
    protected fun bindFBO() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, fboWidth, fboHeight)
    }

    /** Unbind FBO (restore default framebuffer). */
    protected fun unbindFBO() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    protected fun releaseFBO() {
        if (fbo != 0) { GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0); fbo = 0 }
        if (colorTexture != 0) { GLES30.glDeleteTextures(1, intArrayOf(colorTexture), 0); colorTexture = 0 }
        if (depthRBO != 0) { GLES30.glDeleteRenderbuffers(1, intArrayOf(depthRBO), 0); depthRBO = 0 }
    }

    override fun release() {
        releaseFBO()
    }
}

