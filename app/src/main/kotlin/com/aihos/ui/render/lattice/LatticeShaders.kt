package com.aihos.ui.render.lattice
object LatticeShaders {
    val NODE_VERTEX: String get() = VERT_NODE
    val NODE_FRAGMENT: String get() = FRAG_NODE
    val BEAM_VERTEX: String get() = VERT_BEAM
    val BEAM_FRAGMENT: String get() = FRAG_BEAM
}
private val VERT_NODE = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec3 aInstancePos;
layout(location=3) in vec4 aInstanceColor;
layout(location=4) in float aInstanceRadius;
layout(location=5) in float aInstanceEnergy;
layout(location=6) in float aInstancePhase;
uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
out vec3 vWorldPos;
out vec3 vNormal;
out vec4 vColor;
out float vEnergy;
out float vFresnel;
void main(){
    float t=uTime+aInstancePhase*3.0;
    float pulse=1.0+sin(t*2.5)*0.08*aInstanceEnergy;
    float morph=sin(aPosition.x*5.0+t*1.5)*sin(aPosition.y*4.0+t*1.2)*0.03*aInstanceEnergy;
    vec3 displaced=aPosition*(aInstanceRadius*pulse)+aNormal*morph;
    vec3 worldPos=displaced+aInstancePos;
    vWorldPos=worldPos;
    vNormal=aNormal;
    vColor=aInstanceColor;
    vEnergy=aInstanceEnergy;
    vec3 viewDir=normalize(-worldPos);
    vFresnel=pow(1.0-max(dot(vNormal,viewDir),0.0),3.0);
    gl_Position=uProjection*uView*vec4(worldPos,1.0);
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
uniform float uTime;
uniform vec3 uCameraPos;
out vec4 fragColor;
void main(){
    vec3 lightDir=normalize(vec3(0.5,1.0,0.3));
    float diff=max(dot(vNormal,lightDir),0.0);
    vec3 ambient=vColor.rgb*0.15;
    vec3 diffuse=vColor.rgb*diff*0.6;
    vec3 viewDir=normalize(uCameraPos-vWorldPos);
    vec3 halfDir=normalize(lightDir+viewDir);
    float spec=pow(max(dot(vNormal,halfDir),0.0),32.0);
    vec3 glowColor=mix(vec3(0.0,0.8,1.0),vec3(1.0,0.5,0.1),vEnergy*0.5);
    vec3 fresnel=glowColor*vFresnel*(0.5+vEnergy*1.5);
    float pulse=sin(uTime*3.0+vWorldPos.x*2.0)*0.5+0.5;
    vec3 emissive=glowColor*vEnergy*0.3*pulse;
    vec3 color=ambient+diffuse+vec3(spec*0.3)+fresnel+emissive;
    fragColor=vec4(color,vColor.a);
}
""".trimIndent()
private val VERT_BEAM = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec3 aStartPos;
layout(location=2) in vec3 aEndPos;
layout(location=3) in float aStrength;
layout(location=4) in float aPulsePhase;
layout(location=5) in float aBeamWidth;
uniform mat4 uView;
uniform mat4 uProjection;
uniform float uTime;
uniform float uCognitiveLoad;
out float vAlpha;
out float vPulse;
out vec3 vColor;
void main(){
    vec3 dir=aEndPos-aStartPos;
    float len=length(dir);
    vec3 fwd=dir/max(len,0.001);
    vec3 up=abs(fwd.y)<0.99?vec3(0,1,0):vec3(1,0,0);
    vec3 right=normalize(cross(fwd,up));
    vec3 realUp=cross(right,fwd);
    float t=aPosition.x;
    float r=aPosition.y;
    vec3 center=aStartPos+dir*t;
    float width=aBeamWidth*(0.003+aStrength*0.004);
    float taper=1.0-abs(t*2.0-1.0)*0.3;
    vec3 offset=(right*cos(r*6.283185)+realUp*sin(r*6.283185))*width*taper;
    vec3 worldPos=center+offset;
    float pulseSpeed=1.5+uCognitiveLoad*3.0;
    vPulse=sin((t-uTime*pulseSpeed+aPulsePhase)*6.283185*2.0)*0.5+0.5;
    vAlpha=aStrength*0.6*(0.5+vPulse*0.5)*taper;
    vColor=mix(vec3(0.0,0.6,1.0),vec3(0.2,1.0,0.8),aStrength);
    gl_Position=uProjection*uView*vec4(worldPos,1.0);
}
""".trimIndent()
private val FRAG_BEAM = """
#version 300 es
precision highp float;
in float vAlpha;
in float vPulse;
in vec3 vColor;
out vec4 fragColor;
void main(){
    vec3 glow=vColor*(0.8+vPulse*0.6);
    fragColor=vec4(glow,vAlpha);
}
""".trimIndent()
