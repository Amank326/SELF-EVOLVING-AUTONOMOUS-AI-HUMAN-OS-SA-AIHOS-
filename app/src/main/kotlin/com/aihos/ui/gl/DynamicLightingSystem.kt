package com.aihos.ui.gl

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * DynamicLightingSystem — Manages all lights for the cinematic scene.
 *
 * Light sources:
 *   1 directional light   — Key light (sun-like)
 *   4 point lights (max)  — Orbiting accent lights
 *   1 ambient term        — Base illumination
 *   1 rim light           — Edge highlight (Fresnel-like, set via uniform)
 *
 * AI metric mapping:
 *   confidence     → key light brightness
 *   cognitiveLoad  → warm/cool color temperature shift
 *   systemHealth   → ambient level
 *   evolutionRate  → orbiting light speed
 *   autonomyLevel  → rim light intensity
 *
 * All arrays are pre-allocated. Zero GC pressure per frame.
 */
class DynamicLightingSystem {

    // ── Directional light ────────────────────────────────────────
    private val dirLightDir = floatArrayOf(-0.5f, -1.0f, -0.3f)
    private val dirLightColor = floatArrayOf(0.95f, 0.95f, 1.0f)
    private var dirLightIntensity = 1.0f

    // ── Point lights (max 4) ─────────────────────────────────────
    // Packed: [x, y, z] per light → flat array of 12 floats
    private val pointLightPositions = FloatArray(12)
    private val pointLightColors = FloatArray(12)
    private val pointLightIntensities = FloatArray(4)
    private var activePointLights = 2

    // ── Ambient ──────────────────────────────────────────────────
    private val ambientColor = floatArrayOf(0.05f, 0.08f, 0.15f)
    private var ambientIntensity = 0.3f

    // ── Rim light ────────────────────────────────────────────────
    private val rimColor = floatArrayOf(0.0f, 0.7f, 1.0f)
    private var rimIntensity = 0.5f

    // ── Orbit state ──────────────────────────────────────────────
    private var orbitAngle1 = 0f
    private var orbitAngle2 = 0f

    // ═══════════════════════════════════════════════════════════════
    // Update
    // ═══════════════════════════════════════════════════════════════

    /**
     * Update all lights based on time and AI metrics.
     * Call once per frame before uploading to shader.
     */
    fun update(
        dt: Float,
        time: Float,
        confidence: Float,
        cognitiveLoad: Float,
        systemHealth: Float,
        evolutionRate: Float,
        autonomyLevel: Float
    ) {
        // ── Directional light: brightness from confidence ────
        dirLightIntensity = 0.6f + confidence * 0.8f

        // Color temperature shift: cool (blue) at low load, warm (gold) at high
        dirLightColor[0] = 0.85f + cognitiveLoad * 0.15f    // R gets warmer
        dirLightColor[1] = 0.9f - cognitiveLoad * 0.1f      // G slightly reduced
        dirLightColor[2] = 1.0f - cognitiveLoad * 0.3f      // B gets cooler

        // ── Ambient: level from system health ────────────────
        ambientIntensity = 0.15f + systemHealth * 0.25f
        ambientColor[0] = 0.03f + cognitiveLoad * 0.04f
        ambientColor[1] = 0.06f + confidence * 0.04f
        ambientColor[2] = 0.12f + (1f - cognitiveLoad) * 0.08f

        // ── Rim light: intensity from autonomy ───────────────
        rimIntensity = 0.3f + autonomyLevel * 0.7f
        // Rim color shifts with cognitive state
        rimColor[0] = cognitiveLoad * 0.4f
        rimColor[1] = 0.5f + confidence * 0.3f
        rimColor[2] = 1.0f - cognitiveLoad * 0.3f

        // ── Orbiting point lights ────────────────────────────
        val orbitSpeed = 0.3f + evolutionRate * 0.8f  // rad/sec
        orbitAngle1 += orbitSpeed * dt
        orbitAngle2 += orbitSpeed * 0.7f * dt

        val twoPi = (2.0 * PI).toFloat()
        if (orbitAngle1 > twoPi) orbitAngle1 -= twoPi
        if (orbitAngle2 > twoPi) orbitAngle2 -= twoPi

        // Point light 1: cyan orbit
        val orbitRadius1 = 3.0f
        pointLightPositions[0] = cos(orbitAngle1) * orbitRadius1
        pointLightPositions[1] = sin(time * 0.5f) * 0.5f + 0.5f
        pointLightPositions[2] = sin(orbitAngle1) * orbitRadius1
        pointLightColors[0] = 0.0f
        pointLightColors[1] = 0.8f + confidence * 0.2f
        pointLightColors[2] = 1.0f
        pointLightIntensities[0] = 0.8f + confidence * 0.5f

        // Point light 2: magenta orbit (opposite side)
        val orbitRadius2 = 2.5f
        pointLightPositions[3] = cos(orbitAngle2 + PI.toFloat()) * orbitRadius2
        pointLightPositions[4] = sin(time * 0.3f + 1.0f) * 0.4f
        pointLightPositions[5] = sin(orbitAngle2 + PI.toFloat()) * orbitRadius2
        pointLightColors[3] = 0.9f
        pointLightColors[4] = 0.1f + cognitiveLoad * 0.2f
        pointLightColors[5] = 0.5f + evolutionRate * 0.3f
        pointLightIntensities[1] = 0.6f + cognitiveLoad * 0.4f

        // Point light 3 (optional high-quality only): gold accent below
        pointLightPositions[6] = sin(time * 0.2f) * 2.0f
        pointLightPositions[7] = -1.5f
        pointLightPositions[8] = cos(time * 0.2f) * 2.0f
        pointLightColors[6] = 1.0f
        pointLightColors[7] = 0.75f
        pointLightColors[8] = 0.15f
        pointLightIntensities[2] = evolutionRate * 0.5f

        // Point light 4 (optional): top-down subtle fill
        pointLightPositions[9] = 0f
        pointLightPositions[10] = 3.0f
        pointLightPositions[11] = 0f
        pointLightColors[9] = 0.5f
        pointLightColors[10] = 0.6f
        pointLightColors[11] = 1.0f
        pointLightIntensities[3] = systemHealth * 0.3f
    }

    /**
     * Set how many point lights to use (2 for LOW, 3 for MEDIUM, 4 for HIGH).
     */
    fun setPointLightCount(count: Int) {
        activePointLights = count.coerceIn(0, 4)
    }

    // ═══════════════════════════════════════════════════════════════
    // Upload to Shader
    // ═══════════════════════════════════════════════════════════════

    /**
     * Upload all lighting uniforms to the given shader program.
     * The shader must have matching uniform names.
     */
    fun uploadToShader(program: ShaderProgram) {
        // Directional light
        program.setVec3("uDirLightDir", dirLightDir[0], dirLightDir[1], dirLightDir[2])
        program.setVec3("uDirLightColor", dirLightColor[0], dirLightColor[1], dirLightColor[2])
        program.setFloat("uDirLightIntensity", dirLightIntensity)

        // Point lights
        program.setInt("uPointLightCount", activePointLights)
        program.setVec3Array("uPointLightPos", pointLightPositions, activePointLights)
        program.setVec3Array("uPointLightColor", pointLightColors, activePointLights)
        for (i in 0 until activePointLights) {
            program.setFloat("uPointLightIntensity[$i]", pointLightIntensities[i])
        }

        // Ambient
        program.setVec3("uAmbientColor", ambientColor[0], ambientColor[1], ambientColor[2])
        program.setFloat("uAmbientIntensity", ambientIntensity)

        // Rim
        program.setVec3("uRimColor", rimColor[0], rimColor[1], rimColor[2])
        program.setFloat("uRimIntensity", rimIntensity)
    }
}

