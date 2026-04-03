package com.aihos.bridge

import android.webkit.WebView
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles real-time 3D scene metric updates
 * Bridges ViewModel StateFlows to Three.js animations
 *
 * Manages:
 * - Crystal core animation based on autonomy level
 * - Particle effects based on memory load
 * - Color shifts based on reasoning confidence
 * - Evolution progress visualization
 * - Real-time FPS monitoring
 */
class SceneMetricsHandler(
    private val webView: WebView
) {
    private val isInitialized = AtomicBoolean(false)
    private var lastUpdateTime = System.currentTimeMillis()
    private var updateFrameCount = 0
    
    /**
     * Initialize scene with WebView reference
     * Call after WebView is loaded and Three.js is ready
     */
    fun initialize() {
        if (isInitialized.getAndSet(true)) {
            return
        }
        
        executeJavaScript("""
            console.log('SceneMetricsHandler.initialize() called');
            if (typeof sceneState !== 'undefined') {
                sceneState.metricsReady = true;
                console.log('Scene metrics ready');
            }
        """.trimIndent())
        
        Timber.d("SceneMetricsHandler initialized")
    }
    
    /**
     * Update crystal core animation based on autonomy level
     * Higher autonomy = faster rotation, more vibrant colors
     */
    fun updateAutonomyAnimation(autonomyLevel: Float) {
        if (!isInitialized.get()) return
        
        // Normalize to 0.0-1.0 range
        val normalizedLevel = autonomyLevel.coerceIn(0f, 1f)
        
        // Map to animation properties
        val rotationSpeed = 0.5f + (normalizedLevel * 2f)  // 0.5-2.5 rad/s
        val coreIntensity = 0.3f + (normalizedLevel * 0.7f)  // 0.3-1.0
        val pulseFrequency = 1f + (normalizedLevel * 3f)  // 1-4 Hz
        
        val json = JsonObject().apply {
            addProperty("type", "updateAutonomy")
            addProperty("level", normalizedLevel)
            addProperty("rotationSpeed", rotationSpeed)
            addProperty("coreIntensity", coreIntensity)
            addProperty("pulseFrequency", pulseFrequency)
        }
        
        sendToScene(json)
    }
    
    /**
     * Update particle effects based on memory usage
     * Higher memory = denser particle clouds
     */
    fun updateMemoryParticles(memoryUsage: Float) {
        if (!isInitialized.get()) return
        
        val normalizedUsage = memoryUsage.coerceIn(0f, 1f)
        
        // Calculate particle properties
        val particleCount = (1000f + (normalizedUsage * 4000f)).toInt()  // 1000-5000
        val particleSpeed = 0.5f + (normalizedUsage * 1.5f)  // 0.5-2.0
        val cloudDensity = 0.2f + (normalizedUsage * 0.8f)  // 0.2-1.0
        val particleSize = 0.5f + (normalizedUsage * 1.5f)  // 0.5-2.0
        
        val json = JsonObject().apply {
            addProperty("type", "updateMemory")
            addProperty("usage", normalizedUsage)
            addProperty("particleCount", particleCount)
            addProperty("particleSpeed", particleSpeed)
            addProperty("cloudDensity", cloudDensity)
            addProperty("particleSize", particleSize)
        }
        
        sendToScene(json)
    }
    
    /**
     * Update color shifts based on reasoning confidence
     * Red = low confidence, Green = high confidence
     */
    fun updateReasoningColors(reasoningConfidence: Float) {
        if (!isInitialized.get()) return
        
        val normalizedConfidence = reasoningConfidence.coerceIn(0f, 1f)
        
        // Generate color gradient: Red (0) → Yellow (0.5) → Green (1.0)
        val hue = normalizedConfidence * 120f  // 0° (red) to 120° (green)
        val saturation = 0.6f + (normalizedConfidence * 0.4f)  // 0.6-1.0
        val lightness = 0.4f + (normalizedConfidence * 0.3f)  // 0.4-0.7
        
        // RGB conversion from HSL
        val rgb = hslToRgb(hue, saturation, lightness)
        
        val json = JsonObject().apply {
            addProperty("type", "updateReasoning")
            addProperty("confidence", normalizedConfidence)
            addProperty("hue", hue)
            addProperty("saturation", saturation)
            addProperty("lightness", lightness)
            addProperty("colorR", rgb.first)
            addProperty("colorG", rgb.second)
            addProperty("colorB", rgb.third)
            addProperty("glowIntensity", 0.3f + (normalizedConfidence * 0.7f))
        }
        
        sendToScene(json)
    }
    
    /**
     * Update evolution progress visualization
     * Growth of secondary structures based on population success
     */
    fun updateEvolutionProgress(evolutionProgress: Float) {
        if (!isInitialized.get()) return
        
        val normalizedProgress = evolutionProgress.coerceIn(0f, 1f)
        
        // Evolution effects scale
        val structureSize = 0.5f + (normalizedProgress * 1.5f)  // 0.5-2.0
        val branchingFactor = 2 + (normalizedProgress * 6f).toInt()  // 2-8 branches
        val growthRate = 0.2f + (normalizedProgress * 0.8f)  // 0.2-1.0
        val bloomIntensity = normalizedProgress * 0.6f  // 0-0.6
        
        val json = JsonObject().apply {
            addProperty("type", "updateEvolution")
            addProperty("progress", normalizedProgress)
            addProperty("structureSize", structureSize)
            addProperty("branchingFactor", branchingFactor)
            addProperty("growthRate", growthRate)
            addProperty("bloomIntensity", bloomIntensity)
        }
        
        sendToScene(json)
    }
    
    /**
     * Update overall system health visualization
     * Ambient light intensity and color based on health
     */
    fun updateSystemHealth(systemHealth: Float) {
        if (!isInitialized.get()) return
        
        val normalizedHealth = systemHealth.coerceIn(0f, 1f)
        
        // Health visualization
        val ambientIntensity = 0.3f + (normalizedHealth * 0.5f)  // 0.3-0.8
        val lightColor = when {
            normalizedHealth > 0.7f -> "0x00FF00"  // Green
            normalizedHealth > 0.5f -> "0xFFFF00"  // Yellow
            else -> "0xFF0000"  // Red
        }
        val vibrationIntensity = (1f - normalizedHealth) * 0.5f  // Low health = more vibration
        
        val json = JsonObject().apply {
            addProperty("type", "updateHealth")
            addProperty("health", normalizedHealth)
            addProperty("ambientIntensity", ambientIntensity)
            addProperty("lightColor", lightColor)
            addProperty("vibrationIntensity", vibrationIntensity)
        }
        
        sendToScene(json)
    }
    
    /**
     * Request metrics update from all systems
     * Call periodically (e.g., 30-60 times per second)
     */
    fun requestMetricsFrame() {
        if (!isInitialized.get()) return
        
        updateFrameCount++
        val now = System.currentTimeMillis()
        
        // Calculate FPS every second
        if (now - lastUpdateTime >= 1000) {
            val fps = updateFrameCount
            updateFrameCount = 0
            lastUpdateTime = now
            
            Timber.d("Scene update FPS: $fps")
        }
        
        val json = JsonObject().apply {
            addProperty("type", "requestFrame")
            addProperty("timestamp", now)
        }
        
        sendToScene(json)
    }
    
    /**
     * Update cycle indicator with pulse effect
     * More cycles = faster pulsing
     */
    fun updateCycleIndicator(cycleCount: Long) {
        if (!isInitialized.get()) return
        
        // Pulse frequency increases with cycle count (capped at 10Hz)
        val pulseFrequency = 1f + ((cycleCount % 100) / 100f * 9f)  // 1-10 Hz
        val cycleDisplayValue = (cycleCount % 1000000).toInt()  // Show last 6 digits
        
        val json = JsonObject().apply {
            addProperty("type", "updateCycles")
            addProperty("count", cycleDisplayValue)
            addProperty("pulseFrequency", pulseFrequency)
            addProperty("fullCount", cycleCount)
        }
        
        sendToScene(json)
    }
    
    /**
     * Trigger special animation for significant events
     * e.g., Goal completion, memory consolidation
     */
    fun triggerEventAnimation(eventType: String, intensity: Float = 1.0f) {
        if (!isInitialized.get()) return
        
        val normalizedIntensity = intensity.coerceIn(0f, 1f)
        
        val json = JsonObject().apply {
            addProperty("type", "triggerEvent")
            addProperty("eventType", eventType)
            addProperty("intensity", normalizedIntensity)
        }
        
        sendToScene(json)
    }
    
    /**
     * Send batch metrics update for efficiency
     * Combine multiple metrics into single message
     */
    fun sendBatchUpdate(
        autonomyLevel: Float,
        memoryUsage: Float,
        reasoningConfidence: Float,
        evolutionProgress: Float,
        systemHealth: Float
    ) {
        if (!isInitialized.get()) return
        
        val json = JsonObject().apply {
            addProperty("type", "batchUpdate")
            addProperty("autonomy", autonomyLevel.coerceIn(0f, 1f))
            addProperty("memory", memoryUsage.coerceIn(0f, 1f))
            addProperty("reasoning", reasoningConfidence.coerceIn(0f, 1f))
            addProperty("evolution", evolutionProgress.coerceIn(0f, 1f))
            addProperty("health", systemHealth.coerceIn(0f, 1f))
            addProperty("timestamp", System.currentTimeMillis())
        }
        
        sendToScene(json)
    }
    
    /**
     * Send JSON message to Three.js scene via WebView
     */
    private fun sendToScene(json: JsonObject) {
        val jsonString = json.toString().replace("\"", "\\\"")
        executeJavaScript("""
            if (typeof handleMetricsUpdate !== 'undefined') {
                handleMetricsUpdate($jsonString);
            }
        """.trimIndent())
    }
    
    /**
     * Execute JavaScript in WebView
     */
    private fun executeJavaScript(script: String) {
        try {
            webView.evaluateJavascript(script) { result ->
                if (result != null && result != "null") {
                    Timber.d("JS Result: $result")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute JavaScript")
        }
    }
    
    /**
     * Convert HSL color to RGB
     * Returns triple of (R, G, B) values 0.0-1.0
     */
    private fun hslToRgb(hue: Float, saturation: Float, lightness: Float): Triple<Float, Float, Float> {
        val h = hue / 360f
        val s = saturation
        val l = lightness
        
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h * 6f) % 2f - 1f))
        val m = l - c / 2f
        
        val (r, g, b) = when {
            h < 1f / 6f -> Triple(c, x, 0f)
            h < 2f / 6f -> Triple(x, c, 0f)
            h < 3f / 6f -> Triple(0f, c, x)
            h < 4f / 6f -> Triple(0f, x, c)
            h < 5f / 6f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        
        return Triple(r + m, g + m, b + m)
    }
    
    /**
     * Shutdown and cleanup resources
     */
    fun shutdown() {
        isInitialized.set(false)
        Timber.d("SceneMetricsHandler shutdown")
    }
}
