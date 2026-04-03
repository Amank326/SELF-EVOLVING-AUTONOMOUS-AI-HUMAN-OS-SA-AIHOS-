package com.aihos.ui.gl

import android.opengl.GLES30
import timber.log.Timber

/**
 * OpenGL ES 3.0 shader compile & link utilities.
 * All methods are static, stateless, deterministic.
 */
object GLShaderUtil {

    /**
     * Compile a single shader stage.
     * @return shader handle, or 0 on failure.
     */
    fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            Timber.e("GLShaderUtil: glCreateShader failed for type=$type")
            return 0
        }
        // Ensure #version directive is on the first line (trim leading whitespace/newlines)
        val cleanedSource = source.trimIndent().trim()
        GLES30.glShaderSource(shader, cleanedSource)
        GLES30.glCompileShader(shader)

        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            Timber.e("GLShaderUtil: compile error type=$type\n$log")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    /**
     * Link a vertex + fragment shader into a program.
     * @return program handle, or 0 on failure.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (vs == 0) return 0

        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (fs == 0) {
            GLES30.glDeleteShader(vs)
            return 0
        }

        val program = GLES30.glCreateProgram()
        if (program == 0) {
            Timber.e("GLShaderUtil: glCreateProgram failed")
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            return 0
        }

        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            Timber.e("GLShaderUtil: link error\n$log")
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            return 0
        }

        // Shaders linked into program — detach & delete source handles
        GLES30.glDetachShader(program, vs)
        GLES30.glDetachShader(program, fs)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)

        Timber.d("GLShaderUtil: program $program linked OK")
        return program
    }

    /** Check for GL errors and log them. Returns true if an error was found. */
    fun checkGLError(tag: String): Boolean {
        var err = GLES30.glGetError()
        var found = false
        while (err != GLES30.GL_NO_ERROR) {
            Timber.e("GL error [$tag]: 0x${Integer.toHexString(err)}")
            found = true
            err = GLES30.glGetError()
        }
        return found
    }
}

