package com.aihos.ui.render.datagraph

/**
 * CognitiveGraphShaders — GLSL ES 3.0 shaders for cognitive data visualization.
 *
 * Three shader pairs:
 *   1. Node shaders — Instanced icosphere rendering with type-based Fresnel glow
 *   2. Edge shaders — Instanced beam rendering with data flow gradient
 *   3. Particle shaders — Point sprite rendering for reasoning flow particles
 *
 * All shaders use instanced attributes for single-draw-call batching.
 * No per-frame uniform arrays — all per-instance data in vertex attributes.
 */
object CognitiveGraphShaders {

    val NODE_VERTEX: String get() = VERT_NODE
    val NODE_FRAGMENT: String get() = FRAG_NODE
    val EDGE_VERTEX: String get() = VERT_EDGE
    val EDGE_FRAGMENT: String get() = FRAG_EDGE
    val PARTICLE_VERTEX: String get() = VERT_PARTICLE
    val PARTICLE_FRAGMENT: String get() = FRAG_PARTICLE
}

// ════════════════════════════════════════════════════════════════════
// NODE SHADERS — Instanced glowing spheres for cognitive nodes
// ════════════════════════════════════════════════════════════════════

private val VERT_NODE = """
#version 300 es
precision highp float;

// Base mesh attributes
layout(location=0) in vec3 aPosition;
layout(location=1) in vec3 aNormal;

// Per-instance attributes
layout(location=2) in vec3 aInstancePos;
layout(location=3) in vec4 aInstanceColor;
layout(location=4) in float aInstanceRadius;
layout(location=5) in float aInstanceEnergy;
layout(location=6) in float aInstancePhase;
layout(location=7) in float aInstanceType;
layout(location=8) in float aInstanceWeight;
layout(location=9) in float aInstanceHighlight;

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;

out vec3 vWorldPos;
out vec3 vNormal;
out vec4 vColor;
out float vEnergy;
out float vFresnel;
out float vHighlight;
out float vNodeType;

void main() {
    float t = uTime + aInstancePhase * 3.0;
    
    // Pulsation driven by energy
    float pulse = 1.0 + sin(t * 2.5) * 0.1 * aInstanceEnergy;
    
    // Type-specific vertex displacement
    float morphFreq = 3.0 + aInstanceType * 0.5;
    float morph = sin(aPosition.x * morphFreq + t * 1.5) 
                * sin(aPosition.y * (morphFreq - 1.0) + t * 1.2) 
                * 0.04 * aInstanceEnergy;
    
    // Highlight expansion
    float highlightScale = 1.0 + aInstanceHighlight * 0.15;
    
    vec3 displaced = aPosition * (aInstanceRadius * pulse * highlightScale) + aNormal * morph;
    vec3 worldPos = displaced + aInstancePos;
    
    vWorldPos = worldPos;
    vNormal = aNormal;
    vColor = aInstanceColor;
    vEnergy = aInstanceEnergy;
    vHighlight = aInstanceHighlight;
    vNodeType = aInstanceType;
    
    // Fresnel calculation
    vec3 viewDir = normalize(-worldPos);
    vFresnel = pow(1.0 - max(dot(vNormal, viewDir), 0.0), 3.0);
    
    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
}
""".trimIndent()

private val FRAG_NODE = """
#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec4 vColor;
in float vEnergy;
in float vFresnel;
in float vHighlight;
in float vNodeType;

uniform float uTime;
uniform vec3 uCameraPos;
uniform float uCognitiveLoad;
uniform float uConfidence;

out vec4 fragColor;

// Simple 3D noise
float hash(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

void main() {
    // Phong lighting
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
    float diff = max(dot(vNormal, lightDir), 0.0);
    vec3 viewDir = normalize(uCameraPos - vWorldPos);
    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(vNormal, halfDir), 0.0), 48.0);
    
    // Ambient + diffuse
    vec3 ambient = vColor.rgb * 0.12;
    vec3 diffuse = vColor.rgb * diff * 0.55;
    
    // Glow color based on node type
    vec3 glowColor;
    int typeI = int(vNodeType + 0.5);
    if (typeI == 0) glowColor = vec3(1.0, 0.5, 0.15);       // DECISION — orange
    else if (typeI == 1) glowColor = vec3(0.2, 0.6, 1.0);    // MEMORY — blue
    else if (typeI == 2) glowColor = vec3(0.0, 0.9, 0.7);    // BELIEF — cyan
    else if (typeI == 3) glowColor = vec3(0.8, 0.3, 1.0);    // INFERENCE — purple
    else if (typeI == 4) glowColor = vec3(1.0, 0.9, 0.2);    // INSIGHT — gold
    else glowColor = vec3(0.3, 1.0, 0.4);                     // RULE — green
    
    // Fresnel edge glow
    vec3 fresnel = glowColor * vFresnel * (0.6 + vEnergy * 1.8);
    
    // Emissive pulsation
    float pulse = sin(uTime * 3.0 + vWorldPos.x * 2.0 + vNodeType) * 0.5 + 0.5;
    vec3 emissive = glowColor * vEnergy * 0.35 * pulse;
    
    // Highlight glow ring
    vec3 highlightGlow = glowColor * vHighlight * 0.8 * (0.5 + sin(uTime * 5.0) * 0.5);
    
    // Subtle noise shimmer
    float noise = hash(vWorldPos * 10.0 + uTime * 0.5) * 0.05 * vEnergy;
    
    // Cognitive load modulates overall intensity
    float loadBoost = 1.0 + uCognitiveLoad * 0.3;
    
    vec3 color = (ambient + diffuse + vec3(spec * 0.35) + fresnel + emissive + highlightGlow) * loadBoost;
    color += noise;
    
    // Confidence affects alpha stability
    float alpha = vColor.a * (0.85 + uConfidence * 0.15);
    
    fragColor = vec4(color, alpha);
}
""".trimIndent()

// ════════════════════════════════════════════════════════════════════
// EDGE SHADERS — Instanced beams with data flow gradient
// ════════════════════════════════════════════════════════════════════

private val VERT_EDGE = """
#version 300 es
precision highp float;

// Base template vertex (t, radialAngle, 0)
layout(location=0) in vec3 aPosition;

// Per-instance attributes
layout(location=1) in vec3 aStartPos;
layout(location=2) in vec3 aEndPos;
layout(location=3) in float aStrength;
layout(location=4) in float aPulsePhase;
layout(location=5) in float aEdgeType;

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
uniform float uCognitiveLoad;
uniform float uMemoryActivity;

out float vAlpha;
out float vPulse;
out vec3 vColor;
out float vT;       // position along edge [0,1]
out float vEdgeType;

void main() {
    vec3 dir = aEndPos - aStartPos;
    float len = length(dir);
    vec3 fwd = dir / max(len, 0.001);
    
    // Construct orthonormal basis
    vec3 up = abs(fwd.y) < 0.99 ? vec3(0, 1, 0) : vec3(1, 0, 0);
    vec3 right = normalize(cross(fwd, up));
    vec3 realUp = cross(right, fwd);
    
    float t = aPosition.x;  // [0,1] along beam
    float r = aPosition.y;  // [0,1] radial angle
    
    vec3 center = aStartPos + dir * t;
    
    // Beam width: type-dependent thinness
    float baseWidth = 0.002 + aStrength * 0.005;
    float taper = 1.0 - abs(t * 2.0 - 1.0) * 0.4;  // Narrower at endpoints
    vec3 offset = (right * cos(r * 6.283185) + realUp * sin(r * 6.283185)) * baseWidth * taper;
    
    vec3 worldPos = center + offset;
    
    // Animated energy pulse
    float pulseSpeed = 1.5 + uCognitiveLoad * 3.0;
    float dataPulse = sin((t - uTime * pulseSpeed + aPulsePhase) * 6.283185 * 2.5) * 0.5 + 0.5;
    
    // Memory activity creates secondary wave
    float memWave = sin((t + uTime * 0.8 + aPulsePhase * 2.0) * 6.283185 * 1.5) * uMemoryActivity * 0.3;
    
    vPulse = dataPulse + memWave;
    vT = t;
    vEdgeType = aEdgeType;
    
    // Alpha: strength + pulse + taper
    vAlpha = aStrength * 0.5 * (0.4 + dataPulse * 0.6) * taper;
    
    // Color based on edge type
    int typeI = int(aEdgeType + 0.5);
    if (typeI == 0) vColor = vec3(0.7, 0.9, 1.0);        // CAUSAL — white-cyan
    else if (typeI == 1) vColor = vec3(0.3, 0.6, 1.0);    // EVIDENTIAL — blue
    else if (typeI == 2) vColor = vec3(0.2, 0.9, 0.4);    // TEMPORAL — green
    else if (typeI == 3) vColor = vec3(1.0, 0.6, 0.2);    // REINFORCEMENT — orange
    else vColor = vec3(1.0, 0.2, 0.3);                     // CONFLICT — red
    
    gl_Position = uProjection * uView * vec4(worldPos, 1.0);
}
""".trimIndent()

private val FRAG_EDGE = """
#version 300 es
precision highp float;

in float vAlpha;
in float vPulse;
in vec3 vColor;
in float vT;
in float vEdgeType;

uniform float uTime;

out vec4 fragColor;

void main() {
    // Glow with energy pulse
    vec3 glow = vColor * (0.7 + vPulse * 0.8);
    
    // Center-line brightness boost
    float centerBrightness = 1.0 + vPulse * 0.3;
    
    // Subtle sparkle along edge
    float sparkle = fract(sin(vT * 47.3 + uTime * 2.7) * 43758.5453) * 0.15 * vPulse;
    
    fragColor = vec4(glow * centerBrightness + sparkle, vAlpha);
}
""".trimIndent()

// ════════════════════════════════════════════════════════════════════
// PARTICLE SHADERS — Point sprites for reasoning flow animation
// ════════════════════════════════════════════════════════════════════

private val VERT_PARTICLE = """
#version 300 es
precision highp float;

// Per-instance particle attributes
layout(location=0) in vec3 aParticlePos;    // Interpolated position on edge
layout(location=1) in vec4 aParticleColor;  // Color + alpha
layout(location=2) in float aParticleSize;  // Point size

uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;

out vec4 vColor;

void main() {
    vColor = aParticleColor;
    
    vec4 viewPos = uView * vec4(aParticlePos, 1.0);
    gl_Position = uProjection * viewPos;
    
    // Size attenuation: larger when closer
    float dist = -viewPos.z;
    gl_PointSize = aParticleSize * 80.0 / max(dist, 0.5);
}
""".trimIndent()

private val FRAG_PARTICLE = """
#version 300 es
precision highp float;

in vec4 vColor;
uniform float uTime;

out vec4 fragColor;

void main() {
    // Soft circular point sprite
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float dist = length(coord);
    if (dist > 1.0) discard;
    
    // Smooth falloff
    float alpha = 1.0 - smoothstep(0.0, 1.0, dist);
    alpha *= alpha;  // Quadratic falloff for softer glow
    
    // Core brightness boost
    float core = 1.0 - smoothstep(0.0, 0.3, dist);
    
    vec3 color = vColor.rgb * (1.0 + core * 0.8);
    
    fragColor = vec4(color, alpha * vColor.a);
}
""".trimIndent()

