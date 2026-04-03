package com.aihos.ui.render.immersive

/**
 * ImmersiveDepthConfig — Runtime configuration for Immersive Depth Mode.
 *
 * Controls:
 *   - Mode toggle (normal vs immersive)
 *   - Stereo intensity (IPD scale)
 *   - Parallax layer offsets
 *   - Head tracking sensitivity
 *   - Depth-of-field parameters
 *   - Performance auto-scaling
 *
 * All fields are mutable and can be changed at runtime.
 * The render engine reads these once per frame.
 */
class ImmersiveDepthConfig {

    // ── Mode ─────────────────────────────────────────────────────
    /** Master switch: false = standard cinematic, true = immersive depth. */
    var enabled: Boolean = false

    /** Sub-features. Each can be toggled independently. */
    var stereoEnabled: Boolean = true
    var parallaxEnabled: Boolean = true
    var headTrackingEnabled: Boolean = true
    var depthFogEnabled: Boolean = true
    var depthOfFieldEnabled: Boolean = true
    var godRaysEnabled: Boolean = true

    // ── Stereo Camera ────────────────────────────────────────────
    /**
     * Inter-pupillary distance in world units.
     * Human average ≈ 0.063m. Scale for scene units.
     * Range: [0.01, 0.15]
     */
    var ipd: Float = 0.06f

    /**
     * Stereo intensity multiplier. 0 = mono, 1 = full stereo.
     * Useful for smooth transition in/out of immersive mode.
     * Range: [0, 1]
     */
    var stereoIntensity: Float = 0.7f

    /**
     * Convergence distance — where left/right eyes converge.
     * Objects at this distance have zero parallax.
     * Should roughly equal the AI core distance from camera.
     */
    var convergenceDistance: Float = 4.0f

    // ── Parallax Layers ──────────────────────────────────────────
    /**
     * Parallax strength per depth zone.
     * Higher = more motion offset when camera moves.
     *
     * Background (far): small offset (distant objects barely move)
     * Midground (particles): moderate offset
     * Foreground (AI core): largest offset (close objects shift most)
     */
    var parallaxBackground: Float = 0.02f
    var parallaxMidground: Float = 0.06f
    var parallaxForeground: Float = 0.12f

    // ── Head Tracking ────────────────────────────────────────────
    /** Gyroscope → camera rotation sensitivity. Range: [0.1, 3.0] */
    var headTrackSensitivity: Float = 1.0f

    /** Low-pass filter alpha for sensor smoothing. Lower = smoother. Range: [0.01, 0.3] */
    var sensorSmoothingAlpha: Float = 0.08f

    /** Maximum head-track rotation in radians. Prevents excessive tilt. */
    var maxHeadRotation: Float = 0.25f

    // ── Depth of Field ───────────────────────────────────────────
    /** Focus distance (world units from camera). */
    var focusDistance: Float = 4.0f

    /** Focus transition range (world units). Smaller = sharper focus boundary. */
    var focusRange: Float = 2.0f

    /** Maximum blur intensity for out-of-focus regions. Range: [0, 1] */
    var maxBlurIntensity: Float = 0.4f

    // ── Depth Fog ────────────────────────────────────────────────
    /** Fog start distance from camera. */
    var fogNear: Float = 3.0f

    /** Fog full opacity distance. */
    var fogFar: Float = 15.0f

    /** Fog density exponent. Higher = denser fog. */
    var fogDensity: Float = 0.08f

    // ── God Rays ─────────────────────────────────────────────────
    /** Light shaft intensity. Range: [0, 1] */
    var godRayIntensity: Float = 0.3f

    /** Number of radial blur samples. Lower = cheaper. Range: [8, 32] */
    var godRaySamples: Int = 16

    /** Decay per sample step. Range: [0.9, 1.0] */
    var godRayDecay: Float = 0.96f

    // ── Performance ──────────────────────────────────────────────
    /** Minimum FPS before auto-disabling stereo. */
    var stereoDisableFpsThreshold: Float = 40f

    /** Minimum FPS before auto-disabling DoF. */
    var dofDisableFpsThreshold: Float = 45f

    /** Resolution scale for stereo eye FBOs. Range: [0.5, 1.0] */
    var stereoResolutionScale: Float = 0.75f

    /**
     * Snap all settings to safe defaults.
     */
    fun resetToDefaults() {
        enabled = false
        stereoEnabled = true; parallaxEnabled = true
        headTrackingEnabled = true; depthFogEnabled = true
        depthOfFieldEnabled = true; godRaysEnabled = true
        ipd = 0.06f; stereoIntensity = 0.7f; convergenceDistance = 4f
        parallaxBackground = 0.02f; parallaxMidground = 0.06f; parallaxForeground = 0.12f
        headTrackSensitivity = 1f; sensorSmoothingAlpha = 0.08f; maxHeadRotation = 0.25f
        focusDistance = 4f; focusRange = 2f; maxBlurIntensity = 0.4f
        fogNear = 3f; fogFar = 15f; fogDensity = 0.08f
        godRayIntensity = 0.3f; godRaySamples = 16; godRayDecay = 0.96f
        stereoDisableFpsThreshold = 40f; dofDisableFpsThreshold = 45f
        stereoResolutionScale = 0.75f
    }
}

