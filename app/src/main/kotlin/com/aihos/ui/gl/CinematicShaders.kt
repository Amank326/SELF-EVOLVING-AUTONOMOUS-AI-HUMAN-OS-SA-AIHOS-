package com.aihos.ui.gl

/**
 * CinematicShaders — All GLSL ES 3.00 shader source code for the cinematic pipeline.
 *
 * Shaders:
 *   1. SCENE  — Full 3D scene with MVP, Phong lighting, Fresnel glow, noise, fog
 *   2. BLOOM_EXTRACT — Bright-pass filter (extract pixels above threshold)
 *   3. BLUR  — Separable Gaussian blur (horizontal + vertical passes)
 *   4. COMPOSITE — Final pass: scene + bloom + vignette + color grading
 *   5. PARTICLE — Enhanced particle shader with cinematic effects
 *   6. BACKGROUND — Full-screen procedural nebula background
 *
 * Every shader is a raw string constant — zero allocation, no I/O.
 */
object CinematicShaders {

    // ═══════════════════════════════════════════════════════════════
    // 1. SCENE VERTEX SHADER
    // ═══════════════════════════════════════════════════════════════
    const val SCENE_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;
uniform mat3 uNormalMatrix;
uniform float uTime;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;
out float vDistFromCenter;
out float vTime;

void main() {
    // Subtle vertex displacement for organic breathing
    vec3 pos = aPosition;
    float breathe = sin(uTime * 0.8 + pos.y * 2.0) * 0.02;
    pos += aNormal * breathe;

    vec4 worldPos = uModel * vec4(pos, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = normalize(uNormalMatrix * aNormal);
    vTexCoord = aTexCoord;
    vDistFromCenter = length(pos);
    vTime = uTime;

    gl_Position = uProjection * uView * worldPos;
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 2. SCENE FRAGMENT SHADER
    // ═══════════════════════════════════════════════════════════════
    const val SCENE_FRAGMENT = """
#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vTexCoord;
in float vDistFromCenter;
in float vTime;

// AI metric uniforms
uniform float uTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uEvolutionRate;
uniform float uCameraDistance;

// Camera
uniform vec3 uCameraPos;

// Directional light
uniform vec3 uDirLightDir;
uniform vec3 uDirLightColor;
uniform float uDirLightIntensity;

// Point lights (max 4)
uniform vec3 uPointLightPos[4];
uniform vec3 uPointLightColor[4];
uniform float uPointLightIntensity[4];
uniform int uPointLightCount;

// Ambient
uniform vec3 uAmbientColor;
uniform float uAmbientIntensity;

// Rim light
uniform float uRimIntensity;
uniform vec3 uRimColor;

out vec4 fragColor;

// ── Noise functions ──────────────────────────────────────────
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise(vec3 v) {
    const vec2 C = vec2(1.0/6.0, 1.0/3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;
    i = mod289(i);
    vec4 p = permute(permute(permute(
        i.z + vec4(0.0, i1.z, i2.z, 1.0))
      + i.y + vec4(0.0, i1.y, i2.y, 1.0))
      + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2,p2), dot(p3,p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
}

// ── Lighting calculations ────────────────────────────────────

vec3 calcDirectionalLight(vec3 normal, vec3 viewDir) {
    vec3 lightDir = normalize(-uDirLightDir);
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfDir), 0.0), 64.0);
    return (diff * 0.8 + spec * 0.4) * uDirLightColor * uDirLightIntensity;
}

vec3 calcPointLight(int idx, vec3 worldPos, vec3 normal, vec3 viewDir) {
    vec3 lightVec = uPointLightPos[idx] - worldPos;
    float dist = length(lightVec);
    vec3 lightDir = lightVec / dist;
    float attenuation = 1.0 / (1.0 + 0.09 * dist + 0.032 * dist * dist);
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfDir), 0.0), 32.0);
    return (diff * 0.7 + spec * 0.5) * uPointLightColor[idx] * uPointLightIntensity[idx] * attenuation;
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 viewDir = normalize(uCameraPos - vWorldPos);

    // ── Fresnel edge glow ────────────────────────────────
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0);
    float fresnelGlow = fresnel * (0.5 + uCognitiveLoad * 1.5);

    // ── Base material color (AI state driven) ────────────
    vec3 cCyan    = vec3(0.0, 0.85, 1.0);
    vec3 cMagenta = vec3(0.9, 0.0, 0.5);
    vec3 cGold    = vec3(1.0, 0.75, 0.1);
    vec3 cWhite   = vec3(0.95, 0.95, 1.0);

    // Color morphs based on cognitive state
    vec3 baseColor = mix(cCyan, cMagenta, uCognitiveLoad);
    baseColor = mix(baseColor, cGold, uEvolutionRate * 0.4);
    baseColor = mix(baseColor, cWhite, uConfidence * 0.2);

    // ── Noise distortion on surface ──────────────────────
    float noiseVal = snoise(vWorldPos * 3.0 + vec3(uTime * 0.2));
    float distortion = noiseVal * uEvolutionRate * 0.15;
    baseColor += distortion;

    // ── Emissive pulsing ─────────────────────────────────
    float pulse = 0.5 + 0.5 * sin(uTime * 1.5 + vDistFromCenter * 4.0);
    float emissive = pulse * (0.3 + uCognitiveLoad * 0.7);

    // ── Lighting accumulation ────────────────────────────
    vec3 lighting = uAmbientColor * uAmbientIntensity;
    lighting += calcDirectionalLight(normal, viewDir);

    for (int i = 0; i < 4; i++) {
        if (i >= uPointLightCount) break;
        lighting += calcPointLight(i, vWorldPos, normal, viewDir);
    }

    // ── Rim light ────────────────────────────────────────
    float rim = pow(1.0 - max(dot(normal, viewDir), 0.0), 4.0);
    vec3 rimContrib = uRimColor * rim * uRimIntensity;

    // ── Final color composition ──────────────────────────
    vec3 color = baseColor * lighting;
    color += baseColor * emissive * 0.5;        // emissive contribution
    color += fresnelGlow * cCyan * 0.6;         // Fresnel edge glow
    color += rimContrib;                         // rim light

    // ── Distance fog ─────────────────────────────────────
    float fogDist = length(uCameraPos - vWorldPos);
    float fog = 1.0 - exp(-fogDist * 0.15);
    fog = clamp(fog, 0.0, 0.85);
    vec3 fogColor = vec3(0.01, 0.02, 0.05);
    color = mix(color, fogColor, fog);

    // ── HDR output (bloom will pick up bright parts) ─────
    fragColor = vec4(color, 1.0);
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 3. BLOOM BRIGHT-PASS EXTRACT SHADER
    // ═══════════════════════════════════════════════════════════════
    const val BLOOM_EXTRACT_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

    const val BLOOM_EXTRACT_FRAGMENT = """
#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uSceneTexture;
uniform float uThreshold;

out vec4 fragColor;

void main() {
    vec4 color = texture(uSceneTexture, vTexCoord);
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > uThreshold) {
        fragColor = vec4(color.rgb * (brightness - uThreshold), 1.0);
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 4. GAUSSIAN BLUR SHADER (Separable — H and V)
    // ═══════════════════════════════════════════════════════════════
    const val BLUR_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

    const val BLUR_FRAGMENT = """
#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform vec2 uDirection;    // (1/width, 0) for horizontal, (0, 1/height) for vertical
uniform float uBlurScale;   // quality multiplier (reduce for perf)

out vec4 fragColor;

// 9-tap Gaussian kernel (sigma ~2.5)
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(uTexture, vTexCoord).rgb * weights[0];

    for (int i = 1; i < 5; i++) {
        vec2 offset = uDirection * float(i) * uBlurScale;
        result += texture(uTexture, vTexCoord + offset).rgb * weights[i];
        result += texture(uTexture, vTexCoord - offset).rgb * weights[i];
    }

    fragColor = vec4(result, 1.0);
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 5. COMPOSITE SHADER (Scene + Bloom + Vignette + Color Grading)
    // ═══════════════════════════════════════════════════════════════
    const val COMPOSITE_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

    const val COMPOSITE_FRAGMENT = """
#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uSceneTexture;
uniform sampler2D uBloomTexture;
uniform float uBloomStrength;
uniform float uVignetteIntensity;
uniform float uExposure;
uniform float uGamma;
uniform float uSaturation;
uniform float uTime;

out vec4 fragColor;

// ACESFilm tone mapping
vec3 ACESFilm(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 scene = texture(uSceneTexture, vTexCoord).rgb;
    vec3 bloom = texture(uBloomTexture, vTexCoord).rgb;

    // Additive bloom
    vec3 color = scene + bloom * uBloomStrength;

    // Exposure
    color *= uExposure;

    // ACES tone mapping
    color = ACESFilm(color);

    // Vignette
    vec2 uv = vTexCoord * 2.0 - 1.0;
    float vignette = 1.0 - dot(uv, uv) * uVignetteIntensity;
    vignette = clamp(vignette, 0.0, 1.0);
    vignette = smoothstep(0.0, 1.0, vignette);
    color *= vignette;

    // Saturation adjustment
    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(gray), color, uSaturation);

    // Subtle chromatic aberration for cinematic feel
    float caStrength = 0.002;
    float r = texture(uSceneTexture, vTexCoord + vec2(caStrength, 0.0)).r;
    float b = texture(uSceneTexture, vTexCoord - vec2(caStrength, 0.0)).b;
    color.r = mix(color.r, r, 0.3);
    color.b = mix(color.b, b, 0.3);

    // Film grain
    float grain = fract(sin(dot(vTexCoord * uTime, vec2(12.9898, 78.233))) * 43758.5453);
    color += (grain - 0.5) * 0.015;

    // Gamma correction
    color = pow(color, vec3(1.0 / uGamma));

    fragColor = vec4(color, 1.0);
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 6. ENHANCED PARTICLE SHADER (Cinematic version)
    // ═══════════════════════════════════════════════════════════════
    const val PARTICLE_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec3 aPosition;
layout(location = 1) in float aPhase;
layout(location = 2) in float aSpeed;
layout(location = 3) in float aSize;

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uEvolutionRate;
uniform float uAutonomyLevel;
uniform float uSystemHealth;
uniform vec2 uResolution;

out float vAlpha;
out float vPhase;
out vec3 vColor;

void main() {
    float t = uTime * aSpeed;

    // 3D orbital motion
    float angle = aPhase + t * (0.2 + uAutonomyLevel * 0.5);
    float baseRadius = length(aPosition.xy);
    float radius = baseRadius * (0.7 + 0.3 * sin(t * 0.3 + aPhase));

    float x = cos(angle) * radius;
    float y = sin(angle) * radius;
    float z = aPosition.z + sin(t * 0.5 + aPhase * 3.0) * 0.2;

    // Breathing expansion based on cognitive load
    float breathe = 1.0 + sin(uTime * 0.6) * uCognitiveLoad * 0.15;
    x *= breathe;
    y *= breathe;

    // Pulse
    float pulse = 0.5 + 0.5 * sin(t * 2.0 + aPhase * 6.28318);
    vAlpha = mix(0.1, 1.0, uConfidence) * pulse * uSystemHealth;

    // Dynamic color based on AI state
    vec3 cCyan = vec3(0.0, 0.85, 1.0);
    vec3 cMagenta = vec3(0.9, 0.1, 0.5);
    vec3 cGold = vec3(1.0, 0.78, 0.15);
    vec3 cElectric = vec3(0.3, 0.5, 1.0);

    vColor = mix(cCyan, cMagenta, uCognitiveLoad);
    vColor = mix(vColor, cGold, step(0.7, uConfidence) * sin(aPhase * 4.0 + uTime) * 0.5);
    vColor = mix(vColor, cElectric, uEvolutionRate * 0.3);
    vPhase = aPhase;

    // Point size
    float basePt = mix(2.0, 8.0, uSystemHealth);
    float sizeMult = aSize * (1.0 + pulse * uConfidence * 0.5);
    gl_PointSize = basePt * sizeMult;

    vec4 viewPos = uView * vec4(x, y, z, 1.0);
    gl_Position = uProjection * viewPos;
}
"""

    const val PARTICLE_FRAGMENT = """
#version 300 es
precision highp float;

in float vAlpha;
in float vPhase;
in vec3 vColor;

uniform float uTime;
uniform float uCognitiveLoad;

out vec4 fragColor;

void main() {
    vec2 pc = gl_PointCoord - vec2(0.5);
    float dist = length(pc);
    if (dist > 0.5) discard;

    // Soft circle with glow falloff
    float core = 1.0 - smoothstep(0.0, 0.15, dist);
    float glow = 1.0 - smoothstep(0.15, 0.5, dist);
    float soft = core + glow * 0.4;

    // Inner glow pulse
    float innerPulse = 0.8 + 0.2 * sin(uTime * 3.0 + vPhase * 12.0);
    soft *= innerPulse;

    float alpha = vAlpha * soft;

    // HDR bloom contribution (bright core)
    vec3 color = vColor * (1.0 + core * 2.0 * uCognitiveLoad);

    fragColor = vec4(color * alpha, alpha);
}
"""

    // ═══════════════════════════════════════════════════════════════
    // 7. BACKGROUND NEBULA SHADER
    // ═══════════════════════════════════════════════════════════════
    const val BACKGROUND_VERTEX = """
#version 300 es
precision highp float;

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.999, 1.0);
}
"""

    const val BACKGROUND_FRAGMENT = """
#version 300 es
precision highp float;

in vec2 vTexCoord;

uniform float uTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform vec2 uResolution;

out vec4 fragColor;

// Hash for pseudo-random
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

// Value noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// FBM (fractal Brownian motion)
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = vTexCoord;
    float aspect = uResolution.x / uResolution.y;
    uv.x *= aspect;

    float t = uTime * 0.05;

    // Nebula layers
    float n1 = fbm(uv * 2.0 + vec2(t, t * 0.7));
    float n2 = fbm(uv * 4.0 - vec2(t * 0.5, t * 1.2));
    float n3 = fbm(uv * 8.0 + vec2(t * 0.3));

    // Deep space colors
    vec3 cDeep = vec3(0.01, 0.01, 0.03);
    vec3 cNebula1 = vec3(0.05, 0.1, 0.25) * (1.0 + uConfidence * 0.5);
    vec3 cNebula2 = vec3(0.15, 0.02, 0.2) * (1.0 + uCognitiveLoad * 0.3);
    vec3 cAccent = vec3(0.0, 0.4, 0.6);

    vec3 color = cDeep;
    color += cNebula1 * n1 * 0.4;
    color += cNebula2 * n2 * 0.3;
    color += cAccent * n3 * 0.1;

    // Stars
    float starField = hash(floor(vTexCoord * 300.0));
    float star = step(0.998, starField) * (0.5 + 0.5 * sin(uTime * 2.0 + starField * 100.0));
    color += vec3(star * 0.8);

    // Subtle radial gradient (darker at edges)
    vec2 center = vec2(aspect * 0.5, 0.5);
    float radial = 1.0 - length(uv - center) * 0.5;
    color *= max(radial, 0.3);

    fragColor = vec4(color, 1.0);
}
"""
}

