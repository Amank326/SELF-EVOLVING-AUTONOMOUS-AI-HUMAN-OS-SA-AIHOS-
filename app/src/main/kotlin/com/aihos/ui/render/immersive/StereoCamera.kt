package com.aihos.ui.render.immersive

import android.opengl.Matrix

/**
 * StereoCamera — Dual-eye stereoscopic camera with off-axis projection.
 *
 * Uses asymmetric frustum (off-axis) projection, NOT toe-in, which
 * is the physically correct method for stereo rendering.
 *
 * Math:
 *   rightVec = normalize(cross(forward, up))
 *   leftEyePos  = cameraPos - rightVec * (IPD * intensity / 2)
 *   rightEyePos = cameraPos + rightVec * (IPD * intensity / 2)
 *   frustumShift = halfIpd * nearPlane / convergenceDistance
 *
 * Zero allocation per frame.
 */
class StereoCamera {

    val leftViewMatrix = FloatArray(16)
    val leftProjectionMatrix = FloatArray(16)
    val leftPosition = FloatArray(3)

    val rightViewMatrix = FloatArray(16)
    val rightProjectionMatrix = FloatArray(16)
    val rightPosition = FloatArray(3)

    private val rightVec = FloatArray(3)
    private val forwardVec = FloatArray(3)

    private var fov = 45f
    private var aspect = 1f
    private var nearPlane = 0.1f
    private var farPlane = 100f

    fun setProjectionParams(fov: Float, aspect: Float, near: Float, far: Float) {
        this.fov = fov; this.aspect = aspect
        this.nearPlane = near; this.farPlane = far
    }

    fun update(
        cameraPos: FloatArray,
        targetX: Float, targetY: Float, targetZ: Float,
        config: ImmersiveDepthConfig
    ) {
        val halfIpd = config.ipd * config.stereoIntensity * 0.5f
        val convergence = config.convergenceDistance

        forwardVec[0] = targetX - cameraPos[0]
        forwardVec[1] = targetY - cameraPos[1]
        forwardVec[2] = targetZ - cameraPos[2]
        normalize3(forwardVec)

        // right = forward x up(0,1,0)
        rightVec[0] = forwardVec[2]
        rightVec[1] = 0f
        rightVec[2] = -forwardVec[0]
        normalize3(rightVec)

        leftPosition[0] = cameraPos[0] - rightVec[0] * halfIpd
        leftPosition[1] = cameraPos[1] - rightVec[1] * halfIpd
        leftPosition[2] = cameraPos[2] - rightVec[2] * halfIpd

        rightPosition[0] = cameraPos[0] + rightVec[0] * halfIpd
        rightPosition[1] = cameraPos[1] + rightVec[1] * halfIpd
        rightPosition[2] = cameraPos[2] + rightVec[2] * halfIpd

        Matrix.setLookAtM(leftViewMatrix, 0,
            leftPosition[0], leftPosition[1], leftPosition[2],
            targetX, targetY, targetZ, 0f, 1f, 0f)
        Matrix.setLookAtM(rightViewMatrix, 0,
            rightPosition[0], rightPosition[1], rightPosition[2],
            targetX, targetY, targetZ, 0f, 1f, 0f)

        val frustumShift = halfIpd * nearPlane / convergence
        val fovRad = Math.toRadians(fov.toDouble()).toFloat()
        val top = nearPlane * kotlin.math.tan(fovRad * 0.5f)
        val bottom = -top
        val right = top * aspect
        val left = -right

        Matrix.frustumM(leftProjectionMatrix, 0,
            left + frustumShift, right + frustumShift,
            bottom, top, nearPlane, farPlane)
        Matrix.frustumM(rightProjectionMatrix, 0,
            left - frustumShift, right - frustumShift,
            bottom, top, nearPlane, farPlane)
    }

    private fun normalize3(v: FloatArray) {
        val len = kotlin.math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        if (len > 0.0001f) { val inv = 1f / len; v[0] *= inv; v[1] *= inv; v[2] *= inv }
    }
}

