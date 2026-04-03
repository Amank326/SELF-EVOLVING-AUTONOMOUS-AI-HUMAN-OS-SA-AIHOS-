package com.aihos.rendering.streams

import com.aihos.rendering.lighting.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// ─────────────────────────────────────────────────────────────────────────────
// 1. DATA STREAM MODEL & 5. PERFORMANCE
// Flattened Data Oriented Design (DOD). Zero object allocation during runtime.
// Pre-allocated primitive arrays managed by a continuous ring buffer.
// ─────────────────────────────────────────────────────────────────────────────

fun getStreamColor(type: StreamType): FloatArray = when(type) {
    StreamType.MEMORY_RECALL -> floatArrayOf(0.2f, 0.8f, 1.0f, 1.0f) // Cyan
    StreamType.REASONING_FLOW -> floatArrayOf(1.0f, 0.4f, 0.1f, 1.0f) // Orange
    StreamType.AGENT_MESSAGE -> floatArrayOf(0.4f, 1.0f, 0.4f, 1.0f) // Green
}

enum class StreamType { MEMORY_RECALL, REASONING_FLOW, AGENT_MESSAGE }

class DataStreamBuffer(private val maxStreams: Int = 1024) {
    // Flattened arrays for SoA (Structure of Arrays) performance
    val startX = FloatArray(maxStreams)
    val startY = FloatArray(maxStreams)
    val startZ = FloatArray(maxStreams)
    
    val endX = FloatArray(maxStreams)
    val endY = FloatArray(maxStreams)
    val endZ = FloatArray(maxStreams)
    
    val colorR = FloatArray(maxStreams)
    val colorG = FloatArray(maxStreams)
    val colorB = FloatArray(maxStreams)
    
    val phase = FloatArray(maxStreams)      // 0.0 to 1.0 (UV animation)
    val speed = FloatArray(maxStreams)      // Animation speed
    val intensity = FloatArray(maxStreams)  // Pulse brightness / lifetime
    
    var activeCount: Int = 0

    // Stride = 12 floats per stream instance (vec3 start, vec3 end, vec3 color, phase, speed, intensity)
    val instanceBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(maxStreams * 12 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    fun spawnStream(start: Vec3, end: Vec3, type: StreamType, flowSpeed: Float, baseIntensity: Float) {
        if (activeCount >= maxStreams) return // Drop if full (Performance cap)
        
        val idx = activeCount
        startX[idx] = start.x; startY[idx] = start.y; startZ[idx] = start.z
        endX[idx] = end.x; endY[idx] = end.y; endZ[idx] = end.z
        
        val color = getStreamColor(type)
        colorR[idx] = color[0]; colorG[idx] = color[1]; colorB[idx] = color[2]
        
        phase[idx] = 0f
        speed[idx] = flowSpeed
        intensity[idx] = baseIntensity
        
        activeCount++
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. ANIMATION & UPDATE LOOP LOGIC
    // ─────────────────────────────────────────────────────────────────────────────
    fun update(deltaTime: Float) {
        var writeIdx = 0
        
        for (i in 0 until activeCount) {
            // Animate phase 
            phase[i] += speed[i] * deltaTime
            // Decay intensity (fading tail)
            intensity[i] -= deltaTime * 0.5f 
            
            if (intensity[i] > 0f) {
                // Keep alive, copy to write pointer if compacted
                if (writeIdx != i) copyIndex(i, writeIdx)
                writeToBuffer(writeIdx)
                writeIdx++
            }
        }
        activeCount = writeIdx
    }

    private fun copyIndex(src: Int, dst: Int) {
        startX[dst] = startX[src]; startY[dst] = startY[src]; startZ[dst] = startZ[src]
        endX[dst] = endX[src]; endY[dst] = endY[src]; endZ[dst] = endZ[src]
        colorR[dst] = colorR[src]; colorG[dst] = colorG[src]; colorB[dst] = colorB[src]
        phase[dst] = phase[src]; speed[dst] = speed[src]; intensity[dst] = intensity[src]
    }

    private fun writeToBuffer(idx: Int) {
        val offset = idx * 12
        instanceBuffer.position(offset)
        instanceBuffer.put(startX[idx]).put(startY[idx]).put(startZ[idx])
        instanceBuffer.put(endX[idx]).put(endY[idx]).put(endZ[idx])
        instanceBuffer.put(colorR[idx]).put(colorG[idx]).put(colorB[idx])
        instanceBuffer.put(phase[idx]).put(speed[idx]).put(intensity[idx])
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. RENDERING: SHADER APPROACH (Instanced Lines / Quads)
// ─────────────────────────────────────────────────────────────────────────────
object StreamShader {
    // VERTEX SHADER: Extrudes a base unit quad along the vector start -> end
    val vertexShader = """
        #version 300 es
        in vec3 aPosition; // Unit quad base geometry
        in vec2 aUV;
        
        // Instanced data
        in vec3 iStart;
        in vec3 iEnd;
        in vec3 iColor;
        in float iPhase;
        in float iSpeed;
        in float iIntensity;
        
        uniform mat4 uVP;
        
        out vec2 vUV;
        out vec3 vColor;
        out float vIntensity;
        out float vPhase;

        void main() {
            vec3 dir = iEnd - iStart;
            float len = length(dir);
            vec3 right = normalize(cross(dir, vec3(0.0, 1.0, 0.0))); // Approx billboard
            
            // Build geometry stretching from start to end
            vec3 pos = mix(iStart, iEnd, aPosition.y); 
            pos += right * aPosition.x * 0.1; // Thickness
            
            gl_Position = uVP * vec4(pos, 1.0);
            
            // UV.y tracks length. Add phase to create moving dashed line effect.
            vUV = vec2(aUV.x, aUV.y * len * 2.0); 
            vColor = iColor;
            vIntensity = iIntensity;
            vPhase = iPhase;
        }
    """.trimIndent()

    // FRAGMENT SHADER: Generates flowing pulse pattern via modified sine wave on UV
    val fragmentShader = """
        #version 300 es
        precision highp float;
        
        in vec2 vUV;
        in vec3 vColor;
        in float vIntensity;
        in float vPhase;
        
        out vec4 FragColor;

        void main() {
            // Flowing packets using fract on UV + Phase
            float flow = fract(vUV.y - vPhase * 10.0);
            
            // Pulse shaping: bright leading edge, fading tail
            float pulse = smoothstep(0.0, 0.2, flow) * smoothstep(1.0, 0.6, flow);
            
            // Soft edges for the quad width
            float edges = 1.0 - abs(vUV.x * 2.0 - 1.0);
            
            // Final composite
            float alpha = pulse * edges * vIntensity;
            if(alpha < 0.01) discard;
            
            FragColor = vec4(vColor * alpha * 2.0, alpha); // Additive blending prep
        }
    """.trimIndent()
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. INTEGRATION
// Connecting with existing Cognitive Graph & Multi-agent Systems
// ─────────────────────────────────────────────────────────────────────────────
class DataStreamEngine {
    val streams = DataStreamBuffer(maxStreams = 500)

    // Triggered automatically by existing event logic in AI layers
    fun onCognitiveEventFired(startNodePos: Vec3, targetNodePos: Vec3, type: StreamType, weight: Float) {
        val flowSpeed = 1.0f + weight * 2.0f
        val intensity = 1.0f + weight
        streams.spawnStream(startNodePos, targetNodePos, type, flowSpeed, intensity)
    }

    // Called once per frame right before glDrawArraysInstanced
    fun renderUpdate(deltaTime: Float) {
        streams.update(deltaTime)
    }
}
