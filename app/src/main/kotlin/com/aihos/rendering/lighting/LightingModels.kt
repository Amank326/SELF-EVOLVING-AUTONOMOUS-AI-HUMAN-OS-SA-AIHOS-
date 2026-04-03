package com.aihos.rendering.lighting

import kotlinx.serialization.Serializable
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// PART 1 — LIGHT SOURCE SYSTEM
// Flexible light architecture supporting directional, point, pulsating core,
// and orbiting energy lights for the SA-AIHOS volumetric environment.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Enumeration of all supported light types in the volumetric engine.
 */
enum class LightType {
    DIRECTIONAL,
    POINT,
    PULSATING_CORE,
    ORBITING_ENERGY
}

/**
 * RGB + Alpha color representation for GPU-friendly data packing.
 * Each channel is in [0.0, 1.0] linear space.
 */
@Serializable
data class LightColor(
    val r: Float = 1.0f,
    val g: Float = 1.0f,
    val b: Float = 1.0f,
    val a: Float = 1.0f
) {
    fun toFloatArray() = floatArrayOf(r, g, b, a)

    companion object {
        val CORE_CYAN = LightColor(0.0f, 0.85f, 1.0f, 1.0f)
        val NEURAL_VIOLET = LightColor(0.6f, 0.2f, 1.0f, 1.0f)
        val ENERGY_GOLD = LightColor(1.0f, 0.82f, 0.2f, 1.0f)
        val COSMOS_WHITE = LightColor(0.95f, 0.97f, 1.0f, 1.0f)
        val WARNING_RED = LightColor(1.0f, 0.15f, 0.1f, 1.0f)
    }
}

/**
 * 3-component vector for positions and directions.
 */
@Serializable
data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    fun toFloatArray() = floatArrayOf(x, y, z)
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length() = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    fun normalize(): Vec3 {
        val l = length()
        return if (l > 0.0001f) Vec3(x / l, y / l, z / l) else Vec3()
    }
}

/**
 * Attenuation model for point lights.
 * Uses physically-based inverse-square falloff with configurable constants.
 */
@Serializable
data class Attenuation(
    val constant: Float = 1.0f,
    val linear: Float = 0.09f,
    val quadratic: Float = 0.032f
)

/**
 * Pulsation parameters for animated/AI-reactive lights.
 */
@Serializable
data class PulsationConfig(
    val baseFrequencyHz: Float = 1.0f,
    val amplitudeMin: Float = 0.3f,
    val amplitudeMax: Float = 1.0f,
    val phaseOffset: Float = 0.0f,
    val aiActivityMultiplier: Float = 1.5f  // Scales when cognitive activity spikes
)

/**
 * Orbit parameters for orbiting energy lights.
 */
@Serializable
data class OrbitConfig(
    val radiusXZ: Float = 5.0f,
    val radiusY: Float = 1.5f,
    val angularSpeedRadPerSec: Float = 0.8f,
    val tiltAngleRad: Float = 0.3f,
    val initialAngleRad: Float = 0.0f
)

/**
 * God-ray rendering parameters per light.
 */
@Serializable
data class GodRayConfig(
    val enabled: Boolean = true,
    val numSamples: Int = 32,            // Radial blur tap count (mobile: 24-48)
    val density: Float = 0.96f,          // Controls ray spread
    val weight: Float = 0.58f,           // Per-sample contribution
    val decay: Float = 0.97f,            // Falloff per sample step
    val exposure: Float = 0.35f,         // Final intensity multiplier
    val noiseIntensity: Float = 0.05f,   // Noise distortion amount  
    val noiseFrequency: Float = 3.0f     // Noise spatial frequency
)

/**
 * Volumetric fog contribution per light.
 */
@Serializable
data class VolumetricFogConfig(
    val enabled: Boolean = true,
    val fogDensity: Float = 0.02f,
    val scatteringCoefficient: Float = 0.15f,
    val absorptionCoefficient: Float = 0.01f,
    val maxDistance: Float = 100.0f,
    val colorShiftWithAiState: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// UNIFIED LIGHT DESCRIPTOR
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Universal light descriptor.
 *
 * Any light in the scene is represented by this single structure.
 * Type-specific fields are nullable; only fields relevant to a given [type]
 * are populated.
 *
 * This keeps the GPU uniform buffer simple (pass all lights in one SSBO or UBO)
 * while still supporting heterogeneous light types.
 */
@Serializable
data class LightDescriptor(
    val id: String = UUID.randomUUID().toString(),
    val type: LightType,
    val enabled: Boolean = true,

    // Spatial
    val position: Vec3 = Vec3(),              // world-space position (point / core / orbit center)
    val direction: Vec3 = Vec3(0f, -1f, 0f),  // direction for DIRECTIONAL lights

    // Photometric
    val color: LightColor = LightColor(),
    val intensity: Float = 1.0f,

    // Type-specific
    val attenuation: Attenuation = Attenuation(),
    val pulsation: PulsationConfig? = null,
    val orbit: OrbitConfig? = null,

    // Volumetric / God-ray
    val godRay: GodRayConfig = GodRayConfig(),
    val volumetricFog: VolumetricFogConfig = VolumetricFogConfig(),

    // Runtime (mutated each frame)
    var currentIntensity: Float = 1.0f,
    var screenSpacePosition: FloatArray = floatArrayOf(0.5f, 0.5f)  // NDC [0..1]
) {
    /**
     * Pack into a flat float array suitable for uploading to a GPU UBO.
     * Layout (per light, 32 floats):
     *   [0..2]   = position
     *   [3]      = type (as float)
     *   [4..6]   = direction
     *   [7]      = intensity * currentIntensity
     *   [8..11]  = color (RGBA)
     *   [12..14] = attenuation (c, l, q)
     *   [15]     = godRay.numSamples
     *   [16..19] = godRay (density, weight, decay, exposure)
     *   [20..21] = screenSpacePosition
     *   [22]     = pulsation phase
     *   [23]     = fog density
     *   [24..31] = reserved
     */
    fun packToUBO(): FloatArray {
        val buf = FloatArray(32)
        buf[0] = position.x; buf[1] = position.y; buf[2] = position.z
        buf[3] = type.ordinal.toFloat()
        buf[4] = direction.x; buf[5] = direction.y; buf[6] = direction.z
        buf[7] = intensity * currentIntensity
        buf[8] = color.r; buf[9] = color.g; buf[10] = color.b; buf[11] = color.a
        buf[12] = attenuation.constant; buf[13] = attenuation.linear; buf[14] = attenuation.quadratic
        buf[15] = godRay.numSamples.toFloat()
        buf[16] = godRay.density; buf[17] = godRay.weight; buf[18] = godRay.decay; buf[19] = godRay.exposure
        buf[20] = screenSpacePosition.getOrElse(0) { 0.5f }
        buf[21] = screenSpacePosition.getOrElse(1) { 0.5f }
        buf[22] = pulsation?.phaseOffset ?: 0f
        buf[23] = volumetricFog.fogDensity
        return buf
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FACTORY HELPERS
// ─────────────────────────────────────────────────────────────────────────────

object LightFactory {

    /** Primary AI core light at the center of the neural lattice. */
    fun createAICoreLight(position: Vec3 = Vec3(0f, 0f, 0f)): LightDescriptor =
        LightDescriptor(
            type = LightType.PULSATING_CORE,
            position = position,
            color = LightColor.CORE_CYAN,
            intensity = 2.5f,
            pulsation = PulsationConfig(
                baseFrequencyHz = 0.8f,
                amplitudeMin = 0.6f,
                amplitudeMax = 1.0f,
                aiActivityMultiplier = 2.0f
            ),
            godRay = GodRayConfig(
                numSamples = 40,
                density = 0.96f,
                weight = 0.65f,
                decay = 0.97f,
                exposure = 0.4f,
                noiseIntensity = 0.04f
            ),
            volumetricFog = VolumetricFogConfig(
                fogDensity = 0.035f,
                scatteringCoefficient = 0.2f,
                colorShiftWithAiState = true
            )
        )

    /** Cosmic directional light simulating a distant star. */
    fun createCosmicDirectional(direction: Vec3 = Vec3(-0.3f, -1f, -0.5f).normalize()): LightDescriptor =
        LightDescriptor(
            type = LightType.DIRECTIONAL,
            direction = direction,
            color = LightColor.COSMOS_WHITE,
            intensity = 0.6f,
            godRay = GodRayConfig(
                numSamples = 32,
                density = 0.94f,
                weight = 0.4f,
                decay = 0.96f,
                exposure = 0.25f
            ),
            volumetricFog = VolumetricFogConfig(fogDensity = 0.01f)
        )

    /** Orbiting energy light that circles the AI core. */
    fun createOrbitingEnergy(
        center: Vec3 = Vec3(),
        colorPreset: LightColor = LightColor.NEURAL_VIOLET,
        orbitRadius: Float = 6.0f,
        initialAngle: Float = 0f
    ): LightDescriptor =
        LightDescriptor(
            type = LightType.ORBITING_ENERGY,
            position = center,
            color = colorPreset,
            intensity = 1.3f,
            orbit = OrbitConfig(
                radiusXZ = orbitRadius,
                angularSpeedRadPerSec = 0.6f,
                initialAngleRad = initialAngle
            ),
            pulsation = PulsationConfig(
                baseFrequencyHz = 1.2f,
                amplitudeMin = 0.5f,
                amplitudeMax = 1.0f
            ),
            godRay = GodRayConfig(numSamples = 24, exposure = 0.2f),
            volumetricFog = VolumetricFogConfig(fogDensity = 0.015f)
        )

    /** Neural lattice point light at an arbitrary node. */
    fun createLatticeNodeLight(position: Vec3, color: LightColor = LightColor.ENERGY_GOLD): LightDescriptor =
        LightDescriptor(
            type = LightType.POINT,
            position = position,
            color = color,
            intensity = 0.8f,
            attenuation = Attenuation(1.0f, 0.35f, 0.44f),
            godRay = GodRayConfig(enabled = false),
            volumetricFog = VolumetricFogConfig(fogDensity = 0.008f)
        )
}
