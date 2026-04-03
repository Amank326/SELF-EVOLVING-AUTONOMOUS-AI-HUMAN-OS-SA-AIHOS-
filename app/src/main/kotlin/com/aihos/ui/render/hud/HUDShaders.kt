package com.aihos.ui.render.hud
/**
 * HUDShaders — GLSL ES 3.0 holographic glass shaders.
 *
 * Glass panel shader: Fresnel glow + transparency gradient + noise shimmer
 *                     + animated scanlines + depth fade + data visualization
 *
 * Data overlay shader: Arc/bar/metric rendering on panel surface
 */
object HUDShaders {
    val GLASS_VERTEX: String get() = VERT_GLASS
    val GLASS_FRAGMENT: String get() = FRAG_GLASS
    val DATA_VERTEX: String get() = VERT_DATA
    val DATA_FRAGMENT: String get() = FRAG_DATA
}
private val VERT_GLASS = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec2 aTexCoord;
// Per-instance
layout(location=2) in vec4 aInstanceCol0;
layout(location=3) in vec4 aInstanceCol1;
layout(location=4) in vec4 aInstanceCol2;
layout(location=5) in vec4 aInstanceCol3;
layout(location=6) in vec4 aInstanceColor;
layout(location=7) in vec4 aInstanceParams; // opacity, glowIntensity, dataValue, hoverAlpha
uniform mat4 uView;
uniform mat4 uProjection;
uniform vec3 uCameraPos;
uniform float uTime;
out vec2 vUV;
out vec3 vWorldPos;
out vec3 vNormal;
out vec4 vColor;
out float vOpacity;
out float vGlow;
out float vDataValue;
out float vHover;
out float vFresnel;
out float vCameraDist;
void main() {
    mat4 model = mat4(aInstanceCol0, aInstanceCol1, aInstanceCol2, aInstanceCol3);
    vec4 worldPos = model * vec4(aPosition, 1.0);
    vWorldPos = worldPos.xyz;
    // Panel normal (Z axis of model matrix)
    vNormal = normalize(vec3(model[2]));
    vUV = aTexCoord;
    vColor = aInstanceColor;
    vOpacity = aInstanceParams.x;
    vGlow = aInstanceParams.y;
    vDataValue = aInstanceParams.z;
    vHover = aInstanceParams.w;
    // Fresnel: view angle to panel surface
    vec3 viewDir = normalize(uCameraPos - vWorldPos);
    float nDotV = abs(dot(vNormal, viewDir));
    vFresnel = pow(1.0 - nDotV, 2.5);
    // Distance to camera (for depth fade)
    vCameraDist = length(uCameraPos - vWorldPos);
    gl_Position = uProjection * uView * worldPos;
}
""".trimIndent()
private val FRAG_GLASS = """
#version 300 es
precision highp float;
in vec2 vUV;
in vec3 vWorldPos;
in vec3 vNormal;
in vec4 vColor;
in float vOpacity;
in float vGlow;
in float vDataValue;
in float vHover;
in float vFresnel;
in float vCameraDist;
uniform float uTime;
uniform float uScanlineEnabled;
out vec4 fragColor;
// Lightweight hash noise
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
void main() {
    // ── Base glass color ─────────────────────────────────
    vec3 baseColor = vColor.rgb;
    // ── Edge border ──────────────────────────────────────
    float edgeX = smoothstep(0.0, 0.04, vUV.x) * smoothstep(1.0, 0.96, vUV.x);
    float edgeY = smoothstep(0.0, 0.04, vUV.y) * smoothstep(1.0, 0.96, vUV.y);
    float edgeMask = 1.0 - edgeX * edgeY;
    float borderLine = step(0.98, max(max(vUV.x, 1.0-vUV.x), max(vUV.y, 1.0-vUV.y)));
    // ── Transparency gradient (top→bottom fade) ──────────
    float gradientAlpha = mix(0.15, 0.05, vUV.y);
    // ── Noise shimmer ────────────────────────────────────
    float shimmer = noise(vUV * 30.0 + uTime * 0.5) * 0.06;
    // ── Animated scanlines ───────────────────────────────
    float scanline = 0.0;
    if (uScanlineEnabled > 0.5) {
        float scanY = fract(vUV.y * 60.0 - uTime * 2.0);
        scanline = smoothstep(0.4, 0.5, scanY) * smoothstep(0.6, 0.5, scanY) * 0.08;
        // Horizontal sweep line
        float sweep = smoothstep(0.0, 0.02, abs(vUV.y - fract(uTime * 0.3))) * 0.15;
        scanline = max(scanline, 1.0 - sweep);
    }
    // ── Fresnel glow ─────────────────────────────────────
    vec3 fresnelColor = mix(baseColor, vec3(1.0), 0.3) * vFresnel * vGlow;
    // ── Hover highlight ──────────────────────────────────
    float hoverGlow = vHover * 0.3;
    vec3 hoverColor = vec3(0.2, 0.9, 1.0) * hoverGlow;
    // ── Depth fade ───────────────────────────────────────
    float depthFade = 1.0 - smoothstep(2.0, 8.0, vCameraDist);
    // ── Combine ──────────────────────────────────────────
    vec3 color = baseColor * (gradientAlpha + shimmer + scanline);
    color += fresnelColor;
    color += hoverColor;
    color += baseColor * edgeMask * 0.5 * vGlow; // edge glow
    color += vec3(0.4, 0.8, 1.0) * borderLine * 0.3; // thin border line
    float alpha = (vOpacity + edgeMask * 0.3 + vFresnel * 0.2 + hoverGlow) * depthFade;
    alpha = clamp(alpha, 0.0, 0.85);
    fragColor = vec4(color, alpha);
}
""".trimIndent()
private val VERT_DATA = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec2 aTexCoord;
layout(location=2) in vec4 aInstanceCol0;
layout(location=3) in vec4 aInstanceCol1;
layout(location=4) in vec4 aInstanceCol2;
layout(location=5) in vec4 aInstanceCol3;
layout(location=6) in vec4 aInstanceParams; // dataValue, panelType(int), time, hover
uniform mat4 uView;
uniform mat4 uProjection;
out vec2 vUV;
out float vDataValue;
out float vPanelType;
out float vTime;
out float vHover;
void main() {
    mat4 model = mat4(aInstanceCol0, aInstanceCol1, aInstanceCol2, aInstanceCol3);
    // Slight Z offset toward camera to render on top of glass
    vec4 worldPos = model * vec4(aPosition.xy, aPosition.z + 0.001, 1.0);
    vUV = aTexCoord;
    vDataValue = aInstanceParams.x;
    vPanelType = aInstanceParams.y;
    vTime = aInstanceParams.z;
    vHover = aInstanceParams.w;
    gl_Position = uProjection * uView * worldPos;
}
""".trimIndent()
private val FRAG_DATA = """
#version 300 es
precision highp float;
in vec2 vUV;
in float vDataValue;
in float vPanelType;
in float vTime;
in float vHover;
out vec4 fragColor;
#define TYPE_METRIC 0.0
#define TYPE_ARC    1.0
#define TYPE_BAR    2.0
#define TYPE_GRAPH  3.0
#define TYPE_STATUS 4.0
#define TYPE_AMBIENT 5.0
void main() {
    vec2 uv = vUV;
    vec3 color = vec3(0.0);
    float alpha = 0.0;
    float val = vDataValue;
    int pType = int(vPanelType + 0.5);
    if (pType == 1) {
        // ── ARC: circular progress ───────────────────────
        vec2 center = vec2(0.5, 0.5);
        float dist = length(uv - center);
        float ringOuter = 0.42;
        float ringInner = 0.32;
        float ring = smoothstep(ringInner - 0.01, ringInner, dist) *
                     smoothstep(ringOuter + 0.01, ringOuter, dist);
        float angle = atan(uv.y - 0.5, uv.x - 0.5);
        float normAngle = (angle + 3.14159) / 6.28318;
        float filled = step(normAngle, val);
        color = mix(vec3(0.1, 0.3, 0.5), vec3(0.0, 0.85, 1.0), filled) * ring;
        // Glow at fill edge
        float edgeGlow = 1.0 - smoothstep(0.0, 0.03, abs(normAngle - val));
        color += vec3(0.2, 1.0, 0.8) * edgeGlow * ring * 0.8;
        alpha = ring * 0.9;
    } else if (pType == 2) {
        // ── BAR: horizontal progress bar ─────────────────
        float barY = smoothstep(0.35, 0.38, uv.y) * smoothstep(0.65, 0.62, uv.y);
        float filled = smoothstep(val, val - 0.01, uv.x * 0.8 + 0.1);
        float barBg = barY * 0.15;
        float barFill = barY * filled;
        color = vec3(0.05, 0.2, 0.3) * barBg + vec3(0.0, 0.8, 1.0) * barFill;
        // Tip glow
        float tipX = val * 0.8 + 0.1;
        float tipGlow = barY * (1.0 - smoothstep(0.0, 0.03, abs(uv.x - tipX)));
        color += vec3(0.3, 1.0, 0.9) * tipGlow * 0.6;
        alpha = max(barBg, barFill) + tipGlow * 0.5;
    } else if (pType == 0) {
        // ── METRIC: centered value circle ────────────────
        float dist = length(uv - vec2(0.5));
        float dot = 1.0 - smoothstep(0.08, 0.12, dist);
        float ring = smoothstep(0.22, 0.24, dist) * (1.0 - smoothstep(0.24, 0.26, dist));
        float pulse = sin(vTime * 3.0) * 0.5 + 0.5;
        color = vec3(0.0, 0.7 + val * 0.3, 1.0) * (dot * 0.8 + ring * 0.5);
        color += vec3(0.1, 0.9, 0.7) * dot * pulse * 0.3;
        alpha = dot * 0.9 + ring * 0.6;
    } else if (pType == 3) {
        // ── GRAPH: fake scrolling waveform ───────────────
        float x = uv.x;
        float wave = sin((x * 12.0 - vTime * 2.0)) * val * 0.3 + 0.5;
        float wave2 = sin((x * 8.0 - vTime * 1.5 + 1.0)) * val * 0.2 + 0.5;
        float line = 1.0 - smoothstep(0.0, 0.015, abs(uv.y - wave));
        float line2 = 1.0 - smoothstep(0.0, 0.02, abs(uv.y - wave2));
        color = vec3(0.0, 0.8, 1.0) * line * 0.8 + vec3(0.0, 0.5, 0.7) * line2 * 0.4;
        // Grid lines
        float gridX = 1.0 - smoothstep(0.0, 0.003, abs(fract(uv.x * 10.0) - 0.5) - 0.49);
        float gridY = 1.0 - smoothstep(0.0, 0.003, abs(fract(uv.y * 8.0) - 0.5) - 0.49);
        color += vec3(0.05, 0.15, 0.2) * (gridX + gridY) * 0.3;
        alpha = max(line * 0.8, line2 * 0.4) + (gridX + gridY) * 0.05;
    } else if (pType == 4) {
        // ── STATUS: simple indicator dot ─────────────────
        float dist = length(uv - vec2(0.3, 0.5));
        float dot = 1.0 - smoothstep(0.06, 0.08, dist);
        float pulse = sin(vTime * 2.0) * 0.3 + 0.7;
        vec3 statusColor = val > 0.5 ? vec3(0.0, 1.0, 0.5) : vec3(1.0, 0.3, 0.1);
        color = statusColor * dot * pulse;
        alpha = dot * 0.9;
    } else {
        // ── AMBIENT: subtle floating particles ───────────
        float f = fract(sin(dot(uv * 10.0 + vTime * 0.3, vec2(12.9898, 78.233))) * 43758.5453);
        float sparkle = step(0.97, f) * 0.5;
        float drift = sin(uv.x * 20.0 + vTime) * sin(uv.y * 15.0 + vTime * 0.7) * 0.15;
        color = vec3(0.1, 0.5, 0.8) * (sparkle + drift);
        alpha = sparkle + drift * 0.5;
    }
    // Hover boost
    color += vec3(0.1, 0.3, 0.4) * vHover * 0.3;
    alpha *= (1.0 + vHover * 0.3);
    alpha = clamp(alpha, 0.0, 0.95);
    fragColor = vec4(color, alpha);
}
""".trimIndent()
