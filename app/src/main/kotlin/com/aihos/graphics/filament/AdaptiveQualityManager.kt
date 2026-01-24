package com.aihos.graphics.filament

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlin.math.roundToInt

/**
 * Adaptive Quality Manager - Dynamic Rendering Quality Scaling
 *
 * Monitors rendering performance and automatically adjusts quality settings
 * to maintain target frame rates across different devices.
 *
 * Responsibilities:
 * - Detect device GPU capabilities and memory
 * - Track frame times and identify performance issues
 * - Adjust geometry complexity, shadow resolution, and light count
 * - Provide quality levels for different device capabilities
 * - Optimize for battery life on lower-end devices
 *
 * Quality Levels:
 * - ULTRA: Full quality, demanding devices, unlimited VRAM
 * - HIGH: Standard quality, mid-range devices
 * - MEDIUM: Balanced quality/performance, lower-end devices
 * - LOW: Performance-focused, budget devices
 */
class AdaptiveQualityManager(private val context: Context) {
    
    // ==================== QUALITY LEVELS ====================
    
    enum class QualityLevel {
        ULTRA,      // 60 FPS, full features
        HIGH,       // 60 FPS, optimized
        MEDIUM,     // 50 FPS, balanced
        LOW         // 45 FPS, efficiency
    }
    
    // ==================== QUALITY SETTINGS ====================
    
    data class QualitySettings(
        val qualityLevel: QualityLevel,
        val targetFps: Int,                    // Target frame rate (45-60)
        val sphereSegments: Int,               // Sphere mesh complexity (16-64)
        val shadowResolution: Int,             // Shadow map size (512-2048)
        val enableDynamicLighting: Boolean,    // Main light updates
        val enableAmbientLight: Boolean,       // Fill light
        val maxLightCount: Int,                // Simultaneous lights
        val enableMotionBlur: Boolean,         // Post-process effect
        val useHalfFloatTextures: Boolean      // Memory optimization
    )
    
    // ==================== STATE ====================
    
    private var currentLevel = QualityLevel.HIGH
    private var frameTimeHistory = mutableListOf<Long>()
    private var frameTimeWindow = 30  // Number of frames to analyze
    private var performanceScore = 0f  // 0.0 = struggling, 1.0 = headroom
    
    // Device characteristics (cached)
    private val isLowMemoryDevice = isLowMemoryDevice()
    private val hasVulkan = hasVulkanSupport()
    private val gpuRenderer = getGPURenderer()
    
    init {
        // Auto-detect initial quality level based on device
        currentLevel = detectOptimalQualityLevel()
    }
    
    // ==================== QUALITY DETECTION ====================
    
    /**
     * Detect optimal quality level based on device characteristics
     */
    private fun detectOptimalQualityLevel(): QualityLevel {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return when {
            // Low-end devices: <2GB RAM or low-end GPU
            memoryInfo.totalMemory < 2_000_000_000 || gpuRenderer.contains("Mali-400") -> QualityLevel.LOW
            
            // Mid-range devices: 2-4GB RAM
            memoryInfo.totalMemory < 4_000_000_000 -> QualityLevel.MEDIUM
            
            // High-end devices: 4-6GB RAM
            memoryInfo.totalMemory < 6_000_000_000 -> QualityLevel.HIGH
            
            // Flagship: 6GB+ RAM with Vulkan
            hasVulkan && !isLowMemoryDevice -> QualityLevel.ULTRA
            
            else -> QualityLevel.HIGH
        }
    }
    
    /**
     * Get current quality settings
     */
    fun getQualitySettings(): QualitySettings {
        return when (currentLevel) {
            QualityLevel.ULTRA -> QualitySettings(
                qualityLevel = QualityLevel.ULTRA,
                targetFps = 60,
                sphereSegments = 64,
                shadowResolution = 2048,
                enableDynamicLighting = true,
                enableAmbientLight = true,
                maxLightCount = 3,
                enableMotionBlur = true,
                useHalfFloatTextures = false
            )
            
            QualityLevel.HIGH -> QualitySettings(
                qualityLevel = QualityLevel.HIGH,
                targetFps = 60,
                sphereSegments = 48,
                shadowResolution = 1024,
                enableDynamicLighting = true,
                enableAmbientLight = true,
                maxLightCount = 2,
                enableMotionBlur = false,
                useHalfFloatTextures = false
            )
            
            QualityLevel.MEDIUM -> QualitySettings(
                qualityLevel = QualityLevel.MEDIUM,
                targetFps = 50,
                sphereSegments = 32,
                shadowResolution = 512,
                enableDynamicLighting = true,
                enableAmbientLight = false,
                maxLightCount = 1,
                enableMotionBlur = false,
                useHalfFloatTextures = true
            )
            
            QualityLevel.LOW -> QualitySettings(
                qualityLevel = QualityLevel.LOW,
                targetFps = 45,
                sphereSegments = 16,
                shadowResolution = 256,
                enableDynamicLighting = false,
                enableAmbientLight = false,
                maxLightCount = 1,
                enableMotionBlur = false,
                useHalfFloatTextures = true
            )
        }
    }
    
    // ==================== ADAPTIVE SCALING ====================
    
    /**
     * Update frame time and check if quality adjustment needed
     * @param frameTimeNanos Frame time in nanoseconds
     */
    fun recordFrameTime(frameTimeNanos: Long) {
        frameTimeHistory.add(frameTimeNanos)
        
        // Keep window size manageable
        if (frameTimeHistory.size > frameTimeWindow) {
            frameTimeHistory.removeAt(0)
        }
        
        // Analyze performance every 30 frames
        if (frameTimeHistory.size >= frameTimeWindow) {
            analyzePerformance()
        }
    }
    
    /**
     * Analyze frame time history and adjust quality if needed
     */
    private fun analyzePerformance() {
        val settings = getQualitySettings()
        val targetFrameTimeNanos = (1_000_000_000 / settings.targetFps).toLong()
        
        // Calculate P95 frame time
        val sortedTimes = frameTimeHistory.sorted()
        val p95Index = (frameTimeHistory.size * 0.95).toInt()
        val p95FrameTime = sortedTimes.getOrElse(p95Index) { 0L }
        
        // Calculate average frame time
        val avgFrameTime = frameTimeHistory.average()
        
        // Performance score (1.0 = on target, <0.8 = struggling, >1.2 = idle)
        performanceScore = (targetFrameTimeNanos.toFloat() / avgFrameTime).coerceIn(0.5f, 1.5f)
        
        // Auto-adjust quality
        when {
            // Severely struggling: drop quality
            performanceScore < 0.7f && currentLevel != QualityLevel.LOW -> {
                decreaseQuality()
            }
            
            // Slightly struggling: try to maintain
            performanceScore < 0.85f -> {
                // No change, monitor closely
            }
            
            // Lots of headroom: can increase quality
            performanceScore > 1.3f && currentLevel != QualityLevel.ULTRA -> {
                increaseQuality()
            }
        }
    }
    
    /**
     * Decrease quality level for better performance
     */
    private fun decreaseQuality() {
        val newLevel = when (currentLevel) {
            QualityLevel.ULTRA -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.LOW
            QualityLevel.LOW -> return  // Already at minimum
        }
        currentLevel = newLevel
        frameTimeHistory.clear()  // Reset history after change
    }
    
    /**
     * Increase quality level for better visuals
     */
    private fun increaseQuality() {
        val newLevel = when (currentLevel) {
            QualityLevel.LOW -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.ULTRA
            QualityLevel.ULTRA -> return  // Already at maximum
        }
        currentLevel = newLevel
        frameTimeHistory.clear()  // Reset history after change
    }
    
    /**
     * Manually set quality level (e.g., from user settings)
     */
    fun setQualityLevel(level: QualityLevel) {
        currentLevel = level
        frameTimeHistory.clear()
    }
    
    /**
     * Get current performance score (0.0 = struggling, 1.0 = on target, 1.5+ = idle)
     */
    fun getPerformanceScore(): Float = performanceScore
    
    /**
     * Get current quality level
     */
    fun getCurrentQualityLevel(): QualityLevel = currentLevel
    
    // ==================== DEVICE DETECTION ====================
    
    /**
     * Check if device is low-memory
     */
    private fun isLowMemoryDevice(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityManager.isLowRamDevice
        } else {
            false
        }
    }
    
    /**
     * Check if device supports Vulkan (vs OpenGL ES)
     */
    private fun hasVulkanSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    /**
     * Get GPU renderer string for device-specific optimization
     */
    private fun getGPURenderer(): String {
        return try {
            // Try to read from BuildProperties
            val buildProp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Build.SOC_MANUFACTURER
            } else {
                "Unknown"
            }
            buildProp.lowercase()
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get detailed device capabilities report
     */
    fun getDeviceCapabilitiesReport(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return """
            === Device Capabilities ===
            GPU Renderer: $gpuRenderer
            Vulkan Support: $hasVulkan
            Low Memory Device: $isLowMemoryDevice
            Total RAM: ${(memoryInfo.totalMemory / 1_000_000_000).roundToInt()} GB
            Available RAM: ${(memoryInfo.availMem / 1_000_000_000).roundToInt()} GB
            Current Quality Level: $currentLevel
            Performance Score: ${"%.2f".format(performanceScore)}
        """.trimIndent()
    }
}
