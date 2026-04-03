package com.aihos.ui.render.universe

/**
 * ProceduralUniverseShaders — GLSL ES 3.0 shaders for the procedural universe engine.
 *
 * Contains:
 *   1. STARFIELD — GPU-instanced infinite starfield with parallax drift
 *   2. NEBULA   — Full-screen fractal nebula clouds with UV distortion
 *   3. AMBIENT  — Drifting ambient data particles with depth glow
 *   4. ENERGY   — Radial energy field distortion around AI core
 *
 * All shaders are raw string constants — zero allocation, no I/O.
 * Every variable avoids GLSL reserved words.
 */
object ProceduralUniverseShaders {

    val STARFIELD_VERTEX: String get() = VERT_STAR
    val STARFIELD_FRAGMENT: String get() = FRAG_STAR
    val NEBULA_VERTEX: String get() = VERT_NEBULA
    val NEBULA_FRAGMENT: String get() = FRAG_NEBULA
    val AMBIENT_PARTICLE_VERTEX: String get() = VERT_AMBIENT
    val AMBIENT_PARTICLE_FRAGMENT: String get() = FRAG_AMBIENT
    val ENERGY_FIELD_VERTEX: String get() = VERT_ENERGY
    val ENERGY_FIELD_FRAGMENT: String get() = FRAG_ENERGY
}

// ════════════════════════════════════════════════════════════════════
// 1. STARFIELD — Thousands of procedural stars with parallax
// ════════════════════════════════════════════════════════════════════

private val VERT_STAR = """
#version 300 es
precision highp float;

// Per-star instance attributes
layout(location=0) in vec3 aStarPos;       // xyz position in cube [-1,1]
layout(location=1) in float aBrightness;   // randomized brightness [0,1]
layout(location=2) in float aColorTemp;    // color temperature index [0,1]
layout(location=3) in float aTwinklePhase; // twinkling phase offset

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
uniform float uParallaxFactor;   // how much stars shift with camera
uniform float uCognitiveLoad;
uniform float uAnimationIntensity;

out float vBrightness;
out float vColorTemp;
out float vTwinkle;
out float vDepth;

void main() {
    // Parallax: distant stars move less than near stars
    float depthFactor = 0.3 + aStarPos.z * 0.7;
    vec3 pos = aStarPos;

    // Slow cosmic drift
    float drift = uTime * 0.002 * depthFactor;
    pos.x += sin(drift + aTwinklePhase * 3.0) * 0.01;
    pos.y += cos(drift * 0.7 + aTwinklePhase * 5.0) * 0.008;

    // Scale to far distance
    pos *= 40.0;

    vec4 viewPos = uView * vec4(pos, 1.0);
    gl_Position = uProjection * viewPos;

    // Twinkle animation
    float twinkle = 0.5 + 0.5 * sin(uTime * (1.5 + aTwinklePhase * 3.0) + aTwinklePhase * 20.0);
    twinkle = mix(0.7, twinkle, uAnimationIntensity * 0.6);

    vBrightness = aBrightness * twinkle;
    vColorTemp = aColorTemp;
    vTwinkle = twinkle;
    vDepth = -viewPos.z;

    // Size: brighter = larger, with distance attenuation
    float baseSz = 1.0 + aBrightness * 3.0;
    float distAtten = 80.0 / max(vDepth, 1.0);
    gl_PointSize = clamp(baseSz * distAtten * (0.8 + uCognitiveLoad * 0.4), 1.0, 8.0);
}
""".trimIndent()

private val FRAG_STAR = """
#version 300 es
precision highp float;

in float vBrightness;
in float vColorTemp;
in float vTwinkle;
in float vDepth;

uniform float uTime;

out vec4 fragColor;

void main() {
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float dist = length(coord);
    if (dist > 1.0) discard;

    // Soft radial falloff with bright core
    float core = 1.0 - smoothstep(0.0, 0.2, dist);
    float halo = 1.0 - smoothstep(0.2, 1.0, dist);
    float brightness = core + halo * 0.15;

    // Color temperature: 0=warm white, 0.5=blue-white, 1=cyan
    vec3 warmColor = vec3(1.0, 0.9, 0.75);
    vec3 coolColor = vec3(0.7, 0.85, 1.0);
    vec3 cyanColor = vec3(0.4, 0.9, 1.0);

    vec3 starColor = mix(warmColor, coolColor, clamp(vColorTemp * 2.0, 0.0, 1.0));
    starColor = mix(starColor, cyanColor, clamp(vColorTemp * 2.0 - 1.0, 0.0, 1.0));

    vec3 color = starColor * brightness * vBrightness;

    // Depth fog: distant stars slightly dimmer
    float fog = 1.0 - smoothstep(20.0, 60.0, vDepth) * 0.5;
    color *= fog;

    float alpha = brightness * vBrightness * fog;
    fragColor = vec4(color, alpha);
}
""".trimIndent()

// ════════════════════════════════════════════════════════════════════
// 2. NEBULA — Fractal noise-based volumetric cloud layer
// ════════════════════════════════════════════════════════════════════

private val VERT_NEBULA = """
#version 300 es
precision highp float;

layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aTexCoord;

out vec2 vUV;

void main() {
    vUV = aTexCoord;
    // Render behind everything at z=0.999
    gl_Position = vec4(aPosition, 0.999, 1.0);
}
""".trimIndent()

private val FRAG_NEBULA = """
#version 300 es
precision highp float;

in vec2 vUV;

uniform float uTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uEvolutionRate;
uniform float uMemoryActivity;
uniform vec2 uResolution;
uniform vec3 uCameraOffset;  // camera-driven parallax offset
uniform float uNebulaIntensity; // dynamic quality control [0,1]

out vec4 fragColor;

// ── Noise functions (optimized for mobile) ──

float hash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm4(vec2 p) {
    float val = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < 4; i++) {
        val += amp * valueNoise(p * freq);
        freq *= 2.0;
        amp *= 0.5;
    }
    return val;
}

float fbm6(vec2 p) {
    float val = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < 6; i++) {
        val += amp * valueNoise(p * freq);
        freq *= 2.1;
        amp *= 0.48;
    }
    return val;
}

void main() {
    float aspect = uResolution.x / uResolution.y;
    vec2 uv = vUV;
    uv.x *= aspect;

    // Camera-driven parallax shift
    uv += uCameraOffset.xy * 0.02;

    float t = uTime * 0.03;

    // UV distortion using noise (animated warping)
    vec2 distortion = vec2(
        fbm4(uv * 1.5 + vec2(t * 0.4, t * 0.3)) - 0.5,
        fbm4(uv * 1.5 + vec2(t * 0.2, -t * 0.5)) - 0.5
    ) * 0.15 * (1.0 + uEvolutionRate * 0.3);

    vec2 warpedUV = uv + distortion;

    // Multi-layer nebula noise
    float n1 = fbm6(warpedUV * 2.0 + vec2(t, t * 0.6));
    float n2 = fbm4(warpedUV * 3.5 - vec2(t * 0.4, t * 0.8));
    float n3 = fbm4(warpedUV * 6.0 + vec2(-t * 0.3, t * 0.2));

    // Deep space base
    vec3 deepSpace = vec3(0.005, 0.005, 0.02);

    // Nebula color palette (AI-state-driven)
    vec3 nebula1 = vec3(0.04, 0.08, 0.22) * (1.0 + uConfidence * 0.4);     // blue
    vec3 nebula2 = vec3(0.14, 0.02, 0.18) * (1.0 + uCognitiveLoad * 0.3);  // purple
    vec3 nebula3 = vec3(0.02, 0.12, 0.15) * (1.0 + uMemoryActivity * 0.3); // teal
    vec3 accent  = vec3(0.0, 0.3, 0.5) * uEvolutionRate;                     // cyan

    vec3 color = deepSpace;
    color += nebula1 * n1 * 0.5;
    color += nebula2 * n2 * 0.35;
    color += nebula3 * n3 * 0.15;
    color += accent * n1 * n2 * 0.3;

    // Dense cloud wisps
    float cloud = smoothstep(0.4, 0.7, n1 * n2 * 2.0);
    color += vec3(0.03, 0.06, 0.12) * cloud;

    // Scattered starfield (procedural, embedded in nebula)
    float starSeed = hash21(floor(vUV * 400.0));
    float isStar = step(0.997, starSeed);
    float starBrightness = isStar * (0.4 + 0.6 * sin(uTime * 2.0 + starSeed * 80.0));
    // Star color variation
    vec3 starCol = mix(vec3(1.0, 0.95, 0.85), vec3(0.7, 0.85, 1.0), hash21(floor(vUV * 400.0) + 0.5));
    color += starCol * starBrightness * 0.7;

    // Radial vignette (darker edges, brighter center)
    vec2 center = vec2(aspect * 0.5, 0.5);
    float radial = 1.0 - smoothstep(0.2, 1.2, length(uv - center) * 0.7);
    color *= mix(0.3, 1.0, radial);

    // Intensity control for performance scaling
    color *= uNebulaIntensity;

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
""".trimIndent()

// ════════════════════════════════════════════════════════════════════
// 3. AMBIENT PARTICLES — Floating data-activity particles
// ════════════════════════════════════════════════════════════════════

private val VERT_AMBIENT = """
#version 300 es
precision highp float;

layout(location=0) in vec3 aParticlePos;   // base position
layout(location=1) in float aParticlePhase; // random phase [0,1]
layout(location=2) in float aParticleSpeed; // drift speed
layout(location=3) in float aParticleSize;  // base size
layout(location=4) in float aParticleDepth; // depth layer [0=near, 1=far]

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
uniform float uCognitiveLoad;
uniform float uMemoryActivity;
uniform float uAnimationIntensity;
uniform float uParallaxFactor;

out float vAlpha;
out float vGlow;
out vec3 vColor;

void main() {
    float t = uTime * aParticleSpeed * 0.3;

    // Slow orbital drift at varying depths
    float depthScale = 5.0 + aParticleDepth * 25.0;
    float angle = aParticlePhase * 6.28318 + t * 0.1;
    float radius = length(aParticlePos.xy) * depthScale;

    vec3 pos;
    pos.x = cos(angle) * radius;
    pos.y = sin(angle) * radius;
    pos.z = (aParticlePos.z + sin(t * 0.2 + aParticlePhase * 4.0) * 0.3) * depthScale;

    // Parallax: deeper particles move less
    float parallax = mix(1.0, 0.3, aParticleDepth);
    pos *= parallax;

    // Breathing expansion
    float breathe = 1.0 + sin(uTime * 0.4 + aParticlePhase * 3.0) * 0.05 * uCognitiveLoad;
    pos *= breathe;

    vec4 viewPos = uView * vec4(pos, 1.0);
    gl_Position = uProjection * viewPos;

    // Pulse glow driven by memory activity
    float pulse = 0.5 + 0.5 * sin(t * 1.5 + aParticlePhase * 10.0);
    vGlow = mix(0.3, 1.0, pulse * uMemoryActivity);
    vAlpha = mix(0.05, 0.4, vGlow) * uAnimationIntensity;

    // Color: cool blue → warm based on depth
    vec3 nearColor = vec3(0.3, 0.7, 1.0);
    vec3 farColor = vec3(0.15, 0.3, 0.6);
    vColor = mix(nearColor, farColor, aParticleDepth);
    vColor = mix(vColor, vec3(0.5, 0.2, 0.8), uCognitiveLoad * 0.2);

    // Size with depth attenuation
    float dist = -viewPos.z;
    float sz = aParticleSize * (1.0 + vGlow * 0.5);
    gl_PointSize = clamp(sz * 50.0 / max(dist, 1.0), 1.0, 6.0);
}
""".trimIndent()

private val FRAG_AMBIENT = """
#version 300 es
precision highp float;

in float vAlpha;
in float vGlow;
in vec3 vColor;

uniform float uTime;

out vec4 fragColor;

void main() {
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float dist = length(coord);
    if (dist > 1.0) discard;

    // Soft glow with bright core
    float core = 1.0 - smoothstep(0.0, 0.25, dist);
    float glow = 1.0 - smoothstep(0.1, 1.0, dist);
    float shape = core * 0.6 + glow * 0.4;

    vec3 color = vColor * shape * (1.0 + core * vGlow * 1.5);
    float alpha = vAlpha * shape;

    fragColor = vec4(color * alpha, alpha);
}
""".trimIndent()

// ════════════════════════════════════════════════════════════════════
// 4. ENERGY FIELD — Radial distortion around AI core
// ════════════════════════════════════════════════════════════════════

private val VERT_ENERGY = """
#version 300 es
precision highp float;

layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aTexCoord;

out vec2 vUV;

void main() {
    vUV = aTexCoord;
    gl_Position = vec4(aPosition, 0.5, 1.0);
}
""".trimIndent()

private val FRAG_ENERGY = """
#version 300 es
precision highp float;

in vec2 vUV;

uniform float uTime;
uniform float uCognitiveLoad;
uniform float uEvolutionRate;
uniform float uConfidence;
uniform vec2 uResolution;
uniform vec2 uCoreScreenPos; // AI core position in screen UV [0,1]

out vec4 fragColor;

float hash2d(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noiseField(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash2d(i);
    float b = hash2d(i + vec2(1.0, 0.0));
    float c = hash2d(i + vec2(0.0, 1.0));
    float d = hash2d(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    float aspect = uResolution.x / uResolution.y;
    vec2 uv = vUV;
    uv.x *= aspect;

    vec2 corePos = uCoreScreenPos;
    corePos.x *= aspect;

    vec2 toCore = uv - corePos;
    float distToCore = length(toCore);

    // Energy field radius
    float fieldRadius = 0.3 + uCognitiveLoad * 0.15;

    // Beyond the field = transparent
    if (distToCore > fieldRadius * 1.5) {
        fragColor = vec4(0.0);
        return;
    }

    // Ripple waves emanating from core
    float ripple = sin(distToCore * 30.0 - uTime * 3.0 + noiseField(uv * 5.0 + uTime * 0.5) * 2.0);
    ripple = ripple * 0.5 + 0.5;

    // Noise distortion
    float noiseDist = noiseField(uv * 8.0 + uTime * 0.3) * 0.6;
    noiseDist += noiseField(uv * 16.0 - uTime * 0.2) * 0.3;

    // Field falloff (fades with distance from core)
    float fieldFalloff = 1.0 - smoothstep(fieldRadius * 0.3, fieldRadius * 1.5, distToCore);

    // Energy intensity
    float energyVal = ripple * noiseDist * fieldFalloff;
    energyVal *= (0.5 + uEvolutionRate * 0.5);

    // Color
    vec3 energyColor = mix(
        vec3(0.0, 0.4, 0.8),
        vec3(0.3, 0.1, 0.8),
        uCognitiveLoad
    );
    energyColor += vec3(0.0, 0.3, 0.2) * uConfidence;

    vec3 color = energyColor * energyVal * 0.3;
    float alpha = energyVal * 0.15;

    fragColor = vec4(color, alpha);
}
""".trimIndent()

