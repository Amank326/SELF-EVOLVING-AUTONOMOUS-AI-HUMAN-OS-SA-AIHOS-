package com.aihos.ui.render.lighting

import com.aihos.ui.gl.ShaderProgram
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Light — Abstract light source.
 */
sealed class Light {
    abstract fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot)
}

data class DirectionalLight(
    val direction: FloatArray = floatArrayOf(-0.5f, -1.0f, -0.3f),
    val color: FloatArray = floatArrayOf(0.95f, 0.95f, 1.0f),
    var intensity: Float = 1.0f
) : Light() {
    override fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot) {
        intensity = 0.6f + aiMetrics.confidence * 0.8f
        color[0] = 0.85f + aiMetrics.cognitiveLoad * 0.15f
        color[1] = 0.9f - aiMetrics.cognitiveLoad * 0.1f
        color[2] = 1.0f - aiMetrics.cognitiveLoad * 0.3f
    }

    override fun equals(other: Any?): Boolean = other is DirectionalLight && direction.contentEquals(other.direction)
    override fun hashCode(): Int = direction.contentHashCode()
}

data class PointLight(
    val position: FloatArray = FloatArray(3),
    val color: FloatArray = floatArrayOf(0f, 0.8f, 1f),
    var intensity: Float = 1.0f,
    val orbitRadius: Float = 3.0f,
    val phaseOffset: Float = 0f,
    val speedMultiplier: Float = 1.0f
) : Light() {
    private var orbitAngle = phaseOffset

    override fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot) {
        val speed = (0.3f + aiMetrics.evolutionRate * 0.8f) * speedMultiplier
        orbitAngle += speed * dt
        val twoPi = (2.0 * PI).toFloat()
        if (orbitAngle > twoPi) orbitAngle -= twoPi

        position[0] = cos(orbitAngle) * orbitRadius
        position[1] = sin(time * 0.5f * speedMultiplier) * 0.5f
        position[2] = sin(orbitAngle) * orbitRadius
    }

    override fun equals(other: Any?): Boolean = other is PointLight && phaseOffset == other.phaseOffset
    override fun hashCode(): Int = phaseOffset.hashCode()
}

data class AmbientLight(
    val color: FloatArray = floatArrayOf(0.05f, 0.08f, 0.15f),
    var intensity: Float = 0.3f
) : Light() {
    override fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot) {
        intensity = 0.15f + aiMetrics.systemHealth * 0.25f
        color[0] = 0.03f + aiMetrics.cognitiveLoad * 0.04f
        color[1] = 0.06f + aiMetrics.confidence * 0.04f
        color[2] = 0.12f + (1f - aiMetrics.cognitiveLoad) * 0.08f
    }

    override fun equals(other: Any?): Boolean = other is AmbientLight
    override fun hashCode(): Int = color.contentHashCode()
}

data class RimLight(
    val color: FloatArray = floatArrayOf(0f, 0.7f, 1f),
    var intensity: Float = 0.5f
) : Light() {
    override fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot) {
        intensity = 0.3f + aiMetrics.autonomyLevel * 0.7f
        color[0] = aiMetrics.cognitiveLoad * 0.4f
        color[1] = 0.5f + aiMetrics.confidence * 0.3f
        color[2] = 1.0f - aiMetrics.cognitiveLoad * 0.3f
    }

    override fun equals(other: Any?): Boolean = other is RimLight
    override fun hashCode(): Int = color.contentHashCode()
}

/**
 * LightingController — Manages all lights and uploads them to shaders.
 *
 * Owns:
 *   1 DirectionalLight
 *   Up to 4 PointLights
 *   1 AmbientLight
 *   1 RimLight
 *
 * All arrays pre-allocated. Zero GC per frame.
 */
class LightingController {

    val directional = DirectionalLight()
    val pointLights = listOf(
        PointLight(color = floatArrayOf(0f, 0.85f, 1f), orbitRadius = 3f, speedMultiplier = 1f),
        PointLight(color = floatArrayOf(0.9f, 0.1f, 0.5f), orbitRadius = 2.5f, phaseOffset = PI.toFloat(), speedMultiplier = 0.7f),
        PointLight(color = floatArrayOf(1f, 0.75f, 0.15f), orbitRadius = 2f, phaseOffset = PI.toFloat() / 2f, speedMultiplier = 0.4f),
        PointLight(color = floatArrayOf(0.5f, 0.6f, 1f), orbitRadius = 1.5f, phaseOffset = PI.toFloat() * 1.5f, speedMultiplier = 0.3f)
    )
    val ambient = AmbientLight()
    val rim = RimLight()

    private var activePointLights = 2

    // Pre-packed upload arrays
    private val packedPositions = FloatArray(12)
    private val packedColors = FloatArray(12)
    private val packedIntensities = FloatArray(4)

    fun setPointLightCount(count: Int) {
        activePointLights = count.coerceIn(0, 4)
    }

    fun update(dt: Float, time: Float, aiMetrics: AIMetricsSnapshot) {
        directional.update(dt, time, aiMetrics)
        ambient.update(dt, time, aiMetrics)
        rim.update(dt, time, aiMetrics)
        for (i in 0 until activePointLights) {
            pointLights[i].update(dt, time, aiMetrics)
        }

        // Pack into flat arrays for upload
        for (i in 0 until activePointLights) {
            val pl = pointLights[i]
            packedPositions[i * 3 + 0] = pl.position[0]
            packedPositions[i * 3 + 1] = pl.position[1]
            packedPositions[i * 3 + 2] = pl.position[2]
            packedColors[i * 3 + 0] = pl.color[0]
            packedColors[i * 3 + 1] = pl.color[1]
            packedColors[i * 3 + 2] = pl.color[2]
            packedIntensities[i] = pl.intensity
        }
    }

    fun uploadToShader(program: ShaderProgram) {
        program.setVec3("uDirLightDir", directional.direction[0], directional.direction[1], directional.direction[2])
        program.setVec3("uDirLightColor", directional.color[0], directional.color[1], directional.color[2])
        program.setFloat("uDirLightIntensity", directional.intensity)

        program.setInt("uPointLightCount", activePointLights)
        program.setVec3Array("uPointLightPos", packedPositions, activePointLights)
        program.setVec3Array("uPointLightColor", packedColors, activePointLights)
        for (i in 0 until activePointLights) {
            program.setFloat("uPointLightIntensity[$i]", packedIntensities[i])
        }

        program.setVec3("uAmbientColor", ambient.color[0], ambient.color[1], ambient.color[2])
        program.setFloat("uAmbientIntensity", ambient.intensity)

        program.setVec3("uRimColor", rim.color[0], rim.color[1], rim.color[2])
        program.setFloat("uRimIntensity", rim.intensity)
    }
}

