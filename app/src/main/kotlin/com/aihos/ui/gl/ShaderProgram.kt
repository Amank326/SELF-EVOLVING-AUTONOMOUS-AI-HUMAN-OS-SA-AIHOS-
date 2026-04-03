package com.aihos.ui.gl

import android.opengl.GLES30
import timber.log.Timber

/**
 * ShaderProgram — Type-safe wrapper around an OpenGL ES 3.0 shader program.
 *
 * Caches uniform locations to avoid per-frame glGetUniformLocation calls.
 * All uniform setters are zero-allocation (no varargs, no boxing).
 *
 * Usage:
 *   val prog = ShaderProgram(vertSrc, fragSrc)
 *   prog.use()
 *   prog.setFloat("uTime", elapsed)
 *   prog.setMat4("uMVP", mvpMatrix)
 *   // draw calls...
 *   prog.unuse()
 */
class ShaderProgram(vertexSource: String, fragmentSource: String) {

    val handle: Int = GLShaderUtil.createProgram(vertexSource, fragmentSource)

    /** Lazily populated uniform location cache. */
    private val uniformCache = HashMap<String, Int>(16)

    val isValid: Boolean get() = handle != 0

    // ── Uniform location lookup (cached) ─────────────────────────

    fun getUniformLocation(name: String): Int {
        return uniformCache.getOrPut(name) {
            val loc = GLES30.glGetUniformLocation(handle, name)
            if (loc == -1) {
                Timber.w("ShaderProgram: uniform '$name' not found in program $handle")
            }
            loc
        }
    }

    // ── Program binding ──────────────────────────────────────────

    fun use() {
        GLES30.glUseProgram(handle)
    }

    fun unuse() {
        GLES30.glUseProgram(0)
    }

    // ── Uniform setters (zero-allocation) ────────────────────────

    fun setFloat(name: String, value: Float) {
        GLES30.glUniform1f(getUniformLocation(name), value)
    }

    fun setInt(name: String, value: Int) {
        GLES30.glUniform1i(getUniformLocation(name), value)
    }

    fun setVec2(name: String, x: Float, y: Float) {
        GLES30.glUniform2f(getUniformLocation(name), x, y)
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(getUniformLocation(name), x, y, z)
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        GLES30.glUniform4f(getUniformLocation(name), x, y, z, w)
    }

    /**
     * Upload a 4×4 matrix. The [matrix] array must have exactly 16 floats.
     * [transpose] is false for column-major (OpenGL default).
     */
    fun setMat4(name: String, matrix: FloatArray, transpose: Boolean = false) {
        GLES30.glUniformMatrix4fv(
            getUniformLocation(name), 1, transpose, matrix, 0
        )
    }

    /**
     * Upload a 3×3 matrix (9 floats).
     */
    fun setMat3(name: String, matrix: FloatArray, transpose: Boolean = false) {
        GLES30.glUniformMatrix3fv(
            getUniformLocation(name), 1, transpose, matrix, 0
        )
    }

    /**
     * Upload a float array as vec3 array.
     */
    fun setVec3Array(name: String, data: FloatArray, count: Int) {
        GLES30.glUniform3fv(getUniformLocation(name), count, data, 0)
    }

    fun setVec4Array(name: String, data: FloatArray, count: Int) {
        GLES30.glUniform4fv(getUniformLocation(name), count, data, 0)
    }

    // ── Cleanup ──────────────────────────────────────────────────

    fun release() {
        if (handle != 0) {
            GLES30.glDeleteProgram(handle)
            uniformCache.clear()
            Timber.d("ShaderProgram: released program $handle")
        }
    }
}

