package com.aihos.ui.render.immersive

/**
 * ImmersiveShaders — GLSL ES 3.0 shaders for immersive depth mode.
 *
 * Contains:
 *   1. Depth fog + DoF composite shader
 *   2. Stereo anaglyph blend shader
 *   3. God-ray radial blur shader
 */
object ImmersiveShaders {
    val DEPTH_FOG_DOF_FRAGMENT: String get() = FRAG_DEPTH_FOG
    val STEREO_BLEND_FRAGMENT: String get() = FRAG_STEREO_BLEND
    val GOD_RAY_FRAGMENT: String get() = FRAG_GOD_RAY
    val FULLSCREEN_VERTEX: String get() = VERT_FULLSCREEN
}

private val VERT_FULLSCREEN = """
#version 300 es
precision highp float;
layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main(){
    vTexCoord=aTexCoord;
    gl_Position=vec4(aPosition,0.0,1.0);
}
""".trimIndent()

/**
 * Depth-based fog + depth-of-field blur.
 *
 * Reads: scene color texture + scene depth texture
 * Outputs: scene with distance fog + bokeh-style DoF blur
 *
 * DoF formula:
 *   linearDepth = (2*near*far) / (far+near - (2*depth-1)*(far-near))
 *   coc = abs(linearDepth - focusDist) / focusRange
 *   coc = clamp(coc, 0, maxBlur)
 *   blur = gaussian9tap(coc * texelSize)
 *
 * Fog formula:
 *   fog = 1 - exp(-density * max(linearDepth - fogNear, 0))
 *   color = mix(sceneColor, fogColor, fog)
 */
private val FRAG_DEPTH_FOG = """
#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uSceneTexture;
uniform sampler2D uDepthTexture;
uniform float uNearPlane;
uniform float uFarPlane;
uniform float uFocusDistance;
uniform float uFocusRange;
uniform float uMaxBlur;
uniform float uFogNear;
uniform float uFogFar;
uniform float uFogDensity;
uniform vec3 uFogColor;
uniform float uDofEnabled;
uniform float uFogEnabled;
uniform vec2 uTexelSize;
out vec4 fragColor;

float linearizeDepth(float d){
    float ndc=d*2.0-1.0;
    return(2.0*uNearPlane*uFarPlane)/(uFarPlane+uNearPlane-ndc*(uFarPlane-uNearPlane));
}

vec3 gaussianBlur(vec2 uv,float radius){
    vec3 col=vec3(0.0);
    float total=0.0;
    float weights[5];
    weights[0]=0.227027;weights[1]=0.194596;weights[2]=0.121622;weights[3]=0.054054;weights[4]=0.016216;
    for(int i=-4;i<=4;i++){
        for(int j=-4;j<=4;j++){
            float w=weights[abs(i)]*weights[abs(j)];
            vec2 off=vec2(float(i),float(j))*uTexelSize*radius;
            col+=texture(uSceneTexture,uv+off).rgb*w;
            total+=w;
        }
    }
    return col/total;
}

void main(){
    vec3 scene=texture(uSceneTexture,vTexCoord).rgb;
    float depth=texture(uDepthTexture,vTexCoord).r;
    float linDepth=linearizeDepth(depth);

    vec3 color=scene;

    // Depth of field
    if(uDofEnabled>0.5){
        float coc=abs(linDepth-uFocusDistance)/uFocusRange;
        coc=clamp(coc,0.0,uMaxBlur);
        if(coc>0.01){
            vec3 blurred=gaussianBlur(vTexCoord,coc*8.0);
            color=mix(color,blurred,coc);
        }
    }

    // Distance fog
    if(uFogEnabled>0.5){
        float fogFactor=1.0-exp(-uFogDensity*max(linDepth-uFogNear,0.0));
        fogFactor=clamp(fogFactor,0.0,0.9);
        color=mix(color,uFogColor,fogFactor);
    }

    fragColor=vec4(color,1.0);
}
""".trimIndent()

/**
 * Stereo anaglyph/blend composite shader.
 *
 * Takes left and right eye textures, combines them using
 * one of several modes:
 *   mode 0: Side-by-side (VR headset)
 *   mode 1: Red-cyan anaglyph
 *   mode 2: Subtle depth blend (default — pseudo-3D without glasses)
 *
 * Subtle depth blend formula:
 *   parallax = (rightColor - leftColor) * 0.5 * intensity
 *   luminanceDiff creates subtle depth perception
 *   color = leftColor + parallax offset → creates micro-disparity
 */
private val FRAG_STEREO_BLEND = """
#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uLeftEyeTexture;
uniform sampler2D uRightEyeTexture;
uniform float uStereoIntensity;
uniform int uStereoMode;
uniform vec2 uTexelSize;
out vec4 fragColor;

void main(){
    vec3 leftCol=texture(uLeftEyeTexture,vTexCoord).rgb;
    vec3 rightCol=texture(uRightEyeTexture,vTexCoord).rgb;

    vec3 result;

    if(uStereoMode==0){
        // Side-by-side
        if(vTexCoord.x<0.5){
            result=texture(uLeftEyeTexture,vec2(vTexCoord.x*2.0,vTexCoord.y)).rgb;
        } else {
            result=texture(uRightEyeTexture,vec2((vTexCoord.x-0.5)*2.0,vTexCoord.y)).rgb;
        }
    } else if(uStereoMode==1){
        // Red-cyan anaglyph
        float lGray=dot(leftCol,vec3(0.299,0.587,0.114));
        float rGray=dot(rightCol,vec3(0.299,0.587,0.114));
        result=vec3(lGray,rGray,rGray);
        result=mix(leftCol,result,uStereoIntensity);
    } else {
        // Subtle depth blend (pseudo-3D)
        // Creates micro-disparity by blending shifted samples
        float shift=uStereoIntensity*0.003;
        vec3 leftShifted=texture(uLeftEyeTexture,vTexCoord+vec2(-shift,0.0)).rgb;
        vec3 rightShifted=texture(uRightEyeTexture,vTexCoord+vec2(shift,0.0)).rgb;
        // Luminance-weighted blend creates depth perception
        float lLum=dot(leftCol,vec3(0.299,0.587,0.114));
        float rLum=dot(rightCol,vec3(0.299,0.587,0.114));
        float depthCue=abs(lLum-rLum);
        // Base: average with subtle chromatic disparity
        result=mix(leftCol,rightCol,0.5);
        result.r=mix(result.r,leftShifted.r,uStereoIntensity*0.3);
        result.b=mix(result.b,rightShifted.b,uStereoIntensity*0.3);
        // Depth edge glow
        result+=vec3(0.05,0.1,0.15)*depthCue*uStereoIntensity*2.0;
    }

    fragColor=vec4(result,1.0);
}
""".trimIndent()

/**
 * God-ray / radial light shaft shader.
 *
 * Performs radial blur from a light source position toward each pixel.
 * lightweight: controlled sample count, decay per step.
 *
 * Formula:
 *   for each sample i:
 *     uv = mix(pixelUV, lightPosUV, i/numSamples * density)
 *     color += texture(scene, uv) * decay^i
 *   color /= numSamples
 *   result = sceneColor + color * intensity
 */
private val FRAG_GOD_RAY = """
#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uSceneTexture;
uniform vec2 uLightScreenPos;
uniform float uIntensity;
uniform float uDecay;
uniform float uDensity;
uniform int uNumSamples;
out vec4 fragColor;

void main(){
    vec3 scene=texture(uSceneTexture,vTexCoord).rgb;
    vec2 deltaUV=(vTexCoord-uLightScreenPos)*uDensity/float(uNumSamples);
    vec2 uv=vTexCoord;
    vec3 rays=vec3(0.0);
    float illuminationDecay=1.0;
    for(int i=0;i<32;i++){
        if(i>=uNumSamples) break;
        uv-=deltaUV;
        vec3 samp=texture(uSceneTexture,uv).rgb;
        // Extract bright parts only
        float lum=dot(samp,vec3(0.299,0.587,0.114));
        samp*=smoothstep(0.5,1.0,lum);
        rays+=samp*illuminationDecay;
        illuminationDecay*=uDecay;
    }
    rays/=float(uNumSamples);
    fragColor=vec4(scene+rays*uIntensity,1.0);
}
""".trimIndent()

