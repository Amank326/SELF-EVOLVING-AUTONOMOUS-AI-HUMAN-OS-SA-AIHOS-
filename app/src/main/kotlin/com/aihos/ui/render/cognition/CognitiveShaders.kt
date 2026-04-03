package com.aihos.ui.render.cognition

/**
 * CognitiveShaders — Enhanced GLSL ES 3.0 shaders with cognitive distortion,
 * heat map, radial pulse waves, and physics-based visual mapping.
 */
object CognitiveShaders {
    val COGNITIVE_SCENE_VERTEX: String get() = VERT_SCENE
    val COGNITIVE_SCENE_FRAGMENT: String get() = FRAG_SCENE
    val COGNITIVE_PARTICLE_VERTEX: String get() = VERT_PARTICLE
    val COGNITIVE_PARTICLE_FRAGMENT: String get() = FRAG_PARTICLE
    val COGNITIVE_COMPOSITE_FRAGMENT: String get() = FRAG_COMPOSITE
}

private val VERT_SCENE = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aTexCoord;
uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;
uniform mat3 uNormalMatrix;
uniform float uCognitiveTime;
uniform float uWallTime;
uniform float uPulsationFreq;
uniform float uPulsationAmplitude;
uniform float uMorphAmplitude;
uniform float uNoiseDistortion;
uniform float uNoiseFrequency;
uniform float uCognitiveLoad;
uniform float uEvolutionRate;
uniform float uDecisionComplexity;
out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;
out float vDistFromCenter;
out float vCognitiveTime;
out float vNoiseValue;
out float vMorphFactor;
vec3 hash3(vec3 p){
    p=vec3(dot(p,vec3(127.1,311.7,74.7)),dot(p,vec3(269.5,183.3,246.1)),dot(p,vec3(113.5,271.9,124.6)));
    return fract(sin(p)*43758.5453123)*2.0-1.0;
}
float snoise3(vec3 p){
    vec3 i=floor(p+dot(p,vec3(1.0/3.0)));
    vec3 x0=p-i+dot(i,vec3(1.0/6.0));
    vec3 e=step(vec3(0.0),x0-x0.yzx);
    vec3 i1=e*(1.0-e.zxy);
    vec3 i2=1.0-e.zxy*(1.0-e);
    vec3 x1=x0-i1+1.0/6.0;
    vec3 x2=x0-i2+1.0/3.0;
    vec3 x3=x0-0.5;
    vec4 w=max(0.6-vec4(dot(x0,x0),dot(x1,x1),dot(x2,x2),dot(x3,x3)),0.0);
    w=w*w; w=w*w;
    return dot(w,vec4(dot(hash3(i),x0),dot(hash3(i+i1),x1),dot(hash3(i+i2),x2),dot(hash3(i+1.0),x3)))*52.0;
}
void main(){
    vec3 pos=aPosition;
    float t=uCognitiveTime;
    float phase=length(pos)*4.0+pos.y*2.0;
    float breathe=uPulsationAmplitude*sin(6.2831853*uPulsationFreq*t+phase);
    pos+=aNormal*breathe;
    if(uMorphAmplitude>0.001){
        float mn=snoise3(pos*uNoiseFrequency+vec3(t*0.3));
        pos+=aNormal*mn*uMorphAmplitude;
        vMorphFactor=mn*0.5+0.5;
    } else { vMorphFactor=0.5; }
    float nv=0.0;
    if(uNoiseDistortion>0.001){
        nv=snoise3(pos*uNoiseFrequency*0.7+vec3(t*0.2,0.0,t*0.15));
        pos+=aNormal*nv*uNoiseDistortion;
    }
    vNoiseValue=nv;
    if(uDecisionComplexity>0.1){
        float cw=sin(t*5.0+pos.x*8.0)*cos(t*3.7+pos.z*6.0);
        pos+=aNormal*cw*uDecisionComplexity*0.01;
    }
    vec4 wp=uModel*vec4(pos,1.0);
    vWorldPos=wp.xyz;
    vNormal=normalize(uNormalMatrix*aNormal);
    vTexCoord=aTexCoord;
    vDistFromCenter=length(aPosition);
    vCognitiveTime=t;
    gl_Position=uProjection*uView*wp;
}
""".trimIndent()

private val FRAG_SCENE = """
#version 300 es
precision highp float;
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vTexCoord;
in float vDistFromCenter;
in float vCognitiveTime;
in float vNoiseValue;
in float vMorphFactor;
uniform float uCognitiveTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uEvolutionRate;
uniform float uReflectionDepth;
uniform float uMemoryActivity;
uniform float uDecisionComplexity;
uniform float uGlowIntensity;
uniform float uFresnelAmplification;
uniform float uColorWarmth;
uniform float uAccentStrength;
uniform float uHeatMapIntensity;
uniform float uRadialPulseIntensity;
uniform float uFogDensity;
uniform float uCameraDistance;
uniform vec3 uCameraPos;
uniform vec3 uDirLightDir;
uniform vec3 uDirLightColor;
uniform float uDirLightIntensity;
uniform vec3 uPointLightPos[4];
uniform vec3 uPointLightColor[4];
uniform float uPointLightIntensity[4];
uniform int uPointLightCount;
uniform vec3 uAmbientColor;
uniform float uAmbientIntensity;
uniform float uRimIntensity;
uniform vec3 uRimColor;
out vec4 fragColor;
vec3 mod289v3(vec3 x){return x-floor(x*(1.0/289.0))*289.0;}
vec4 mod289v4(vec4 x){return x-floor(x*(1.0/289.0))*289.0;}
vec4 permute(vec4 x){return mod289v4(((x*34.0)+1.0)*x);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159-0.85373472095314*r;}
float snoise(vec3 v){
    const vec2 C=vec2(1.0/6.0,1.0/3.0);
    const vec4 D=vec4(0.0,0.5,1.0,2.0);
    vec3 i=floor(v+dot(v,C.yyy));
    vec3 x0=v-i+dot(i,C.xxx);
    vec3 g=step(x0.yzx,x0.xyz);
    vec3 l=1.0-g;
    vec3 i1=min(g.xyz,l.zxy);
    vec3 i2=max(g.xyz,l.zxy);
    vec3 x1=x0-i1+C.xxx;
    vec3 x2=x0-i2+C.yyy;
    vec3 x3=x0-D.yyy;
    i=mod289v3(i);
    vec4 p=permute(permute(permute(i.z+vec4(0.0,i1.z,i2.z,1.0))+i.y+vec4(0.0,i1.y,i2.y,1.0))+i.x+vec4(0.0,i1.x,i2.x,1.0));
    float n_=0.142857142857;
    vec3 ns=n_*D.wyz-D.xzx;
    vec4 j=p-49.0*floor(p*ns.z*ns.z);
    vec4 x_=floor(j*ns.z);
    vec4 y_=floor(j-7.0*x_);
    vec4 hx=x_*ns.x+ns.yyyy;
    vec4 hy=y_*ns.x+ns.yyyy;
    vec4 h=1.0-abs(hx)-abs(hy);
    vec4 b0=vec4(hx.xy,hy.xy);
    vec4 b1=vec4(hx.zw,hy.zw);
    vec4 s0=floor(b0)*2.0+1.0;
    vec4 s1=floor(b1)*2.0+1.0;
    vec4 sh=-step(h,vec4(0.0));
    vec4 a0=b0.xzyw+s0.xzyw*sh.xxyy;
    vec4 a1=b1.xzyw+s1.xzyw*sh.zzww;
    vec3 p0=vec3(a0.xy,h.x);vec3 p1=vec3(a0.zw,h.y);vec3 p2=vec3(a1.xy,h.z);vec3 p3=vec3(a1.zw,h.w);
    vec4 norm=taylorInvSqrt(vec4(dot(p0,p0),dot(p1,p1),dot(p2,p2),dot(p3,p3)));
    p0*=norm.x;p1*=norm.y;p2*=norm.z;p3*=norm.w;
    vec4 m=max(0.6-vec4(dot(x0,x0),dot(x1,x1),dot(x2,x2),dot(x3,x3)),0.0);
    m=m*m;
    return 42.0*dot(m*m,vec4(dot(p0,x0),dot(p1,x1),dot(p2,x2),dot(p3,x3)));
}
vec3 calcDir(vec3 n,vec3 vd){vec3 ld=normalize(-uDirLightDir);float df=max(dot(n,ld),0.0);vec3 hd=normalize(ld+vd);float sp=pow(max(dot(n,hd),0.0),64.0);return(df*0.8+sp*0.4)*uDirLightColor*uDirLightIntensity;}
vec3 calcPt(int idx,vec3 wp,vec3 n,vec3 vd){vec3 lv=uPointLightPos[idx]-wp;float d=length(lv);vec3 ld=lv/d;float att=1.0/(1.0+0.09*d+0.032*d*d);float df=max(dot(n,ld),0.0);vec3 hd=normalize(ld+vd);float sp=pow(max(dot(n,hd),0.0),32.0);return(df*0.7+sp*0.5)*uPointLightColor[idx]*uPointLightIntensity[idx]*att;}
void main(){
    vec3 n=normalize(vNormal);vec3 vd=normalize(uCameraPos-vWorldPos);float t=vCognitiveTime;
    float fresnel=pow(1.0-max(dot(n,vd),0.0),3.0)*uFresnelAmplification;
    vec3 bc=mix(vec3(0.0,0.85,1.0),vec3(0.9,0.0,0.5),uColorWarmth);
    bc=mix(bc,vec3(1.0,0.75,0.1),uAccentStrength);
    bc=mix(bc,vec3(0.95),uConfidence*0.2);
    if(uHeatMapIntensity>0.01){float heat=snoise(vWorldPos*3.0+vec3(t*0.5))*0.5+0.5;heat=smoothstep(0.3,0.8,heat)*uHeatMapIntensity;bc=mix(bc,mix(vec3(0.0,0.0,0.5),vec3(1.0,0.2,0.0),heat),heat*0.5);}
    bc+=snoise(vWorldPos*3.0+vec3(t*0.2))*uEvolutionRate*0.1;
    bc+=vec3(0.2,0.4,0.8)*(vMorphFactor-0.5)*uEvolutionRate*0.3;
    float pulse=0.5+0.5*sin(t*uGlowIntensity*2.0+vDistFromCenter*4.0);
    float em=pulse*(0.3+uCognitiveLoad*0.7);
    if(uRadialPulseIntensity>0.01){float rd=length(vWorldPos);float w=smoothstep(0.4,0.6,sin(rd*10.0-t*4.0)*0.5+0.5);em+=w*uRadialPulseIntensity;bc+=vec3(0.1,0.3,0.6)*w*uRadialPulseIntensity*0.3;}
    vec3 lt=uAmbientColor*uAmbientIntensity+calcDir(n,vd);
    for(int i=0;i<4;i++){if(i>=uPointLightCount)break;lt+=calcPt(i,vWorldPos,n,vd);}
    vec3 col=bc*lt+bc*em*0.5+fresnel*vec3(0.0,0.85,1.0)*0.6+uRimColor*pow(1.0-max(dot(n,vd),0.0),4.0)*uRimIntensity;
    float fog=clamp(1.0-exp(-length(uCameraPos-vWorldPos)*uFogDensity),0.0,0.85);
    col=mix(col,vec3(0.01+uCognitiveLoad*0.02,0.02,0.05+uReflectionDepth*0.03),fog);
    fragColor=vec4(col,1.0);
}
""".trimIndent()

private val VERT_PARTICLE = """
#version 300 es
precision highp float;
layout(location=0) in vec3 aPosition;
layout(location=1) in float aPhase;
layout(location=2) in float aSpeed;
layout(location=3) in float aSize;
uniform mat4 uView;
uniform mat4 uProjection;
uniform float uCognitiveTime;
uniform float uCognitiveLoad;
uniform float uConfidence;
uniform float uEvolutionRate;
uniform float uMemoryActivity;
uniform float uDecisionComplexity;
uniform float uParticleEmissionRate;
uniform float uParticleVelocity;
uniform float uParticleBrightness;
uniform float uTimeScale;
uniform vec2 uResolution;
out float vAlpha;
out float vPhase;
out vec3 vColor;
void main(){
    float t=uCognitiveTime*aSpeed*uParticleVelocity;
    float emTh=uParticleEmissionRate/3.0;
    float isAlive=step(fract(aPhase*7.919),emTh);
    float angle=aPhase+t*(0.2+uCognitiveLoad*0.5);
    float bR=length(aPosition.xy);
    float radius=bR*(0.7+0.3*sin(t*0.3+aPhase));
    float x=cos(angle)*radius;
    float y=sin(angle)*radius;
    float z=aPosition.z+sin(t*0.5+aPhase*3.0)*0.2;
    float breathe=1.0+sin(uCognitiveTime*0.6)*uCognitiveLoad*0.15;
    x*=breathe; y*=breathe;
    if(uDecisionComplexity>0.2){float tw=uDecisionComplexity*sin(t*2.0+aPhase*12.0)*0.1;x+=tw;z+=tw*0.7;}
    float pulse=0.5+0.5*sin(t*2.0+aPhase*6.28318);
    vAlpha=mix(0.1,1.0,uConfidence)*pulse*isAlive*uParticleBrightness;
    vColor=mix(vec3(0.0,0.85,1.0),vec3(0.9,0.1,0.5),uCognitiveLoad);
    vColor=mix(vColor,vec3(1.0,0.78,0.15),step(0.7,uConfidence)*sin(aPhase*4.0+uCognitiveTime)*0.5);
    vColor=mix(vColor,vec3(0.3,0.5,1.0),uEvolutionRate*0.3);
    vColor=mix(vColor,vec3(0.4,0.8,0.3),uMemoryActivity*0.2);
    vPhase=aPhase;
    gl_PointSize=mix(2.0,8.0,uConfidence)*aSize*(1.0+pulse*uConfidence*0.5)*(0.8+uTimeScale*0.4);
    gl_Position=uProjection*uView*vec4(x,y,z,1.0);
}
""".trimIndent()

private val FRAG_PARTICLE = """
#version 300 es
precision highp float;
in float vAlpha;
in float vPhase;
in vec3 vColor;
uniform float uCognitiveTime;
uniform float uCognitiveLoad;
out vec4 fragColor;
void main(){
    if(vAlpha<0.01)discard;
    vec2 pc=gl_PointCoord-vec2(0.5);
    float d=length(pc);
    if(d>0.5)discard;
    float core=1.0-smoothstep(0.0,0.15,d);
    float glow=1.0-smoothstep(0.15,0.5,d);
    float soft=(core+glow*0.4)*(0.8+0.2*sin(uCognitiveTime*3.0+vPhase*12.0));
    float alpha=vAlpha*soft;
    fragColor=vec4(vColor*(1.0+core*2.0*uCognitiveLoad)*alpha,alpha);
}
""".trimIndent()

private val FRAG_COMPOSITE = """
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
uniform float uChromaticAberration;
uniform float uRadialPulseIntensity;
uniform float uCognitiveLoad;
out vec4 fragColor;
vec3 ACESFilm(vec3 x){return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0);}
void main(){
    vec3 col=texture(uSceneTexture,vTexCoord).rgb+texture(uBloomTexture,vTexCoord).rgb*uBloomStrength;
    col=ACESFilm(col*uExposure);
    vec2 uv=vTexCoord*2.0-1.0;
    col*=clamp(smoothstep(0.0,1.0,1.0-dot(uv,uv)*uVignetteIntensity),0.0,1.0);
    float gray=dot(col,vec3(0.2126,0.7152,0.0722));
    col=mix(vec3(gray),col,uSaturation);
    float ca=uChromaticAberration;
    col.r=mix(col.r,texture(uSceneTexture,vTexCoord+vec2(ca,0.0)).r,0.35);
    col.b=mix(col.b,texture(uSceneTexture,vTexCoord-vec2(ca,0.0)).b,0.35);
    if(uRadialPulseIntensity>0.01){float d=length(uv);float w=smoothstep(0.3,0.7,sin(d*15.0-uTime*3.0)*0.5+0.5)*(1.0-smoothstep(0.0,1.2,d));col+=vec3(0.05,0.15,0.3)*w*uRadialPulseIntensity;}
    col+=(fract(sin(dot(vTexCoord*uTime,vec2(12.9898,78.233)))*43758.5453)-0.5)*0.015;
    fragColor=vec4(pow(col,vec3(1.0/uGamma)),1.0);
}
""".trimIndent()

