package com.aihos.ui.render.volumetric
/**
 * VolumetricLightingShaders - GLSL ES 3.0 shaders for volumetric god-rays,
 * light scattering, and depth-based light fog.
 *
 * Pipeline:
 *   1. LIGHT_MASK: Renders bright regions (light sources) into an occlusion texture
 *   2. GOD_RAY: Radial blur from light source position to create volumetric beams
 *   3. DEPTH_FOG: Depth-based atmospheric scattering overlay
 *
 * All screen-space techniques for mobile GPU efficiency.
 */
object VolumetricLightingShaders {
    // ========================================================================
    // SHARED VERTEX - Full-screen quad vertex shader
    // ========================================================================
    const val FULLSCREEN_VERTEX = """#version 300 es
precision highp float;
layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aTexCoord;
out vec2 vUV;
void main() {
    vUV = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""
    // ========================================================================
    // PASS 1 - LIGHT MASK: Extract bright light source regions
    // ========================================================================
    const val LIGHT_MASK_FRAGMENT = """#version 300 es
precision highp float;
in vec2 vUV;
uniform sampler2D uSceneTexture;
uniform float uLightThreshold;
uniform float uCognitiveLoad;
uniform float uTime;
uniform vec2 uCoreLightUV;
out vec4 fragColor;
void main() {
    vec3 scene = texture(uSceneTexture, vUV).rgb;
    // Luminance
    float lum = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    // Extract bright regions above threshold
    float bright = smoothstep(uLightThreshold, uLightThreshold + 0.3, lum);
    // Boost region around AI core (central light source)
    vec2 toCore = vUV - uCoreLightUV;
    float coreDist = length(toCore);
    float coreGlow = exp(-coreDist * coreDist * 8.0) * (0.5 + uCognitiveLoad * 0.8);
    // Pulsating core brightness
    float pulse = 0.8 + 0.2 * sin(uTime * 2.5 + coreDist * 6.0);
    coreGlow *= pulse;
    float mask = max(bright, coreGlow);
    // Tint light mask with scene color for colored rays
    vec3 lightColor = scene * bright + vec3(0.3, 0.6, 1.0) * coreGlow;
    fragColor = vec4(lightColor * mask, mask);
}
"""
    // ========================================================================
    // PASS 2 - GOD RAYS: Radial blur from light source (screen-space)
    // ========================================================================
    const val GOD_RAY_FRAGMENT = """#version 300 es
precision highp float;
in vec2 vUV;
uniform sampler2D uLightMaskTexture;
uniform vec2 uLightScreenPos;
uniform float uDensity;
uniform float uWeight;
uniform float uDecay;
uniform float uExposure;
uniform int uSampleCount;
uniform float uTime;
uniform float uCognitiveLoad;
uniform float uEvolutionRate;
out vec4 fragColor;
// Simple hash for noise
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void main() {
    vec2 texCoord = vUV;
    vec2 deltaUV = (texCoord - uLightScreenPos) * uDensity / float(uSampleCount);
    // Dither start position to reduce banding
    float dither = hash(vUV * 500.0 + uTime * 0.1) * 0.5;
    texCoord -= deltaUV * dither;
    vec3 color = vec3(0.0);
    float illuminationDecay = 1.0;
    for (int i = 0; i < 64; i++) {
        if (i >= uSampleCount) break;
        texCoord -= deltaUV;
        // Clamp to valid UV range
        vec2 sampleUV = clamp(texCoord, vec2(0.001), vec2(0.999));
        vec3 sampleColor = texture(uLightMaskTexture, sampleUV).rgb;
        // Noise distortion for organic feel
        float noise = hash(sampleUV * 100.0 + float(i) * 0.3) * 0.15;
        sampleColor *= (1.0 + noise * uEvolutionRate);
        sampleColor *= illuminationDecay * uWeight;
        color += sampleColor;
        illuminationDecay *= uDecay;
    }
    color *= uExposure;
    // Subtle color shift based on cognitive state
    vec3 tint = mix(
        vec3(0.8, 0.9, 1.0),
        vec3(1.0, 0.7, 0.4),
        uCognitiveLoad * 0.3
    );
    color *= tint;
    fragColor = vec4(color, 1.0);
}
"""
    // ========================================================================
    // PASS 3 - DEPTH FOG: Atmospheric scattering based on depth
    // ========================================================================
    const val DEPTH_FOG_FRAGMENT = """#version 300 es
precision highp float;
in vec2 vUV;
uniform sampler2D uSceneTexture;
uniform sampler2D uGodRayTexture;
uniform sampler2D uDepthTexture;
uniform float uTime;
uniform float uFogDensity;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uNearPlane;
uniform float uFarPlane;
uniform vec2 uCoreLightUV;
uniform float uPulseIntensity;
out vec4 fragColor;
// Linearize depth from depth buffer
float linearizeDepth(float d) {
    float z = d * 2.0 - 1.0;
    return (2.0 * uNearPlane * uFarPlane) / (uFarPlane + uNearPlane - z * (uFarPlane - uNearPlane));
}
void main() {
    vec3 scene = texture(uSceneTexture, vUV).rgb;
    vec3 godRays = texture(uGodRayTexture, vUV).rgb;
    // Read and linearize depth (if depth texture available)
    float rawDepth = texture(uDepthTexture, vUV).r;
    float depth = linearizeDepth(rawDepth);
    float normalizedDepth = clamp((depth - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
    // Exponential fog factor
    float fogFactor = 1.0 - exp(-normalizedDepth * uFogDensity);
    // Fog color: cool blue with AI state influence
    vec3 fogColor = vec3(0.02, 0.04, 0.08);
    fogColor += vec3(0.01, 0.02, 0.05) * uConfidence;
    fogColor += vec3(0.03, 0.01, 0.02) * uCognitiveLoad;
    // Distance-from-core glow (adds warmth near center)
    vec2 toCore = vUV - uCoreLightUV;
    float coreDist = length(toCore);
    float coreProximity = exp(-coreDist * coreDist * 4.0);
    fogColor += vec3(0.05, 0.1, 0.2) * coreProximity;
    // Light pulse wave
    float pulseWave = sin(coreDist * 20.0 - uTime * 4.0) * 0.5 + 0.5;
    pulseWave *= uPulseIntensity * coreProximity;
    fogColor += vec3(0.1, 0.15, 0.3) * pulseWave;
    // Combine: scene + god rays + depth fog
    vec3 result = scene;
    result += godRays;
    result = mix(result, fogColor, fogFactor * 0.3);
    // Add subtle radial glow
    float radialGlow = coreProximity * (0.3 + uCognitiveLoad * 0.2);
    result += vec3(0.02, 0.05, 0.1) * radialGlow;
    fragColor = vec4(result, 1.0);
}
"""
}
