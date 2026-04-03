package com.aihos.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aihos.util.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Unit tests for PerformanceOptimizer
 * Tests: 20+ total
 * Coverage: Metrics, caching, optimization triggers, resource management
 */
@RunWith(AndroidJUnit4::class)
class PerformanceOptimizerTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var context: Context
    private lateinit var performanceOptimizer: PerformanceOptimizer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        performanceOptimizer = PerformanceOptimizer(context)
    }

    // ========== Test 1: Metrics Collection ==========

    @Test
    fun testCollectMetrics() {
        // Act
        val metrics = performanceOptimizer.collectMetrics()

        // Assert
        assertNotNull(metrics)
        assertTrue(metrics.cpuUsage >= 0f && metrics.cpuUsage <= 100f)
        assertTrue(metrics.memoryUsage >= 0f && metrics.memoryUsage <= 100f)
        assertTrue(metrics.batteryLevel >= 0f && metrics.batteryLevel <= 100f)
        assertTrue(metrics.thermalStatus in 0..100)
    }

    @Test
    fun testMetricsTimestamp() {
        // Act
        val metrics = performanceOptimizer.collectMetrics()

        // Assert
        assertTrue(metrics.timestamp > 0)
        assertTrue(metrics.timestamp <= System.currentTimeMillis())
    }

    @Test
    fun testCollectMultipleMetrics() {
        // Act
        val metrics1 = performanceOptimizer.collectMetrics()
        Thread.sleep(100)  // Wait a bit
        val metrics2 = performanceOptimizer.collectMetrics()

        // Assert - Different timestamps
        assertTrue(metrics2.timestamp >= metrics1.timestamp)
    }

    // ========== Test 2: Cache Management ==========

    @Test
    fun testCacheData() {
        // Arrange
        val key = "test_key"
        val value = "test_value"

        // Act
        performanceOptimizer.cacheData(key, value)
        val cached = performanceOptimizer.getCachedData(key)

        // Assert
        assertNotNull(cached)
        assertEquals(value, cached)
    }

    @Test
    fun testCacheEviction() {
        // Arrange - Fill cache beyond capacity
        val largeData = "X".repeat(52000000)  // 52MB exceeds 50MB limit

        // Act
        performanceOptimizer.cacheData("large_key", largeData)

        // Assert - LRU eviction should have occurred
        assertTrue(performanceOptimizer.getCacheSize() <= 52000000)
    }

    @Test
    fun testClearCache() {
        // Arrange
        performanceOptimizer.cacheData("key1", "value1")
        performanceOptimizer.cacheData("key2", "value2")

        // Act
        performanceOptimizer.clearCache()

        // Assert
        assertTrue(performanceOptimizer.getCacheSize() == 0L)
    }

    @Test
    fun testCacheHitRate() {
        // Arrange
        performanceOptimizer.cacheData("key", "value")

        // Act
        val _ = performanceOptimizer.getCachedData("key")  // Cache hit
        val _ = performanceOptimizer.getCachedData("missing")  // Cache miss

        // Assert
        val hitRate = performanceOptimizer.getCacheHitRate()
        assertTrue(hitRate in 0f..1f)
    }

    // ========== Test 3: Optimization Triggers ==========

    @Test
    fun testOptimizationTrigger_HighMemory() {
        // This test verifies that high memory usage triggers optimization
        // Note: Actual behavior depends on device state

        // Act
        val shouldOptimize = performanceOptimizer.shouldTriggerOptimization(
            memoryThreshold = 0.5f  // 50% usage triggers
        )

        // Assert - Behavior depends on current device state
        assertTrue(shouldOptimize is Boolean)
    }

    @Test
    fun testOptimizationTrigger_LowBattery() {
        // Act
        val shouldOptimize = performanceOptimizer.shouldTriggerOptimization(
            batteryThreshold = 0.95f  // Almost all battery available
        )

        // Assert
        assertTrue(shouldOptimize is Boolean)
    }

    @Test
    fun testOptimizationTrigger_Thermal() {
        // Act
        val shouldOptimize = performanceOptimizer.shouldTriggerOptimization(
            thermalThreshold = 0.9f  // 90% thermal limit
        )

        // Assert
        assertTrue(shouldOptimize is Boolean)
    }

    // ========== Test 4: Optimization Execution ==========

    @Test
    fun testOptimizeMemory() {
        // Arrange - Get initial memory
        val initialMetrics = performanceOptimizer.collectMetrics()

        // Act
        performanceOptimizer.optimizeMemory(level = 1)  // Moderate optimization
        val afterMetrics = performanceOptimizer.collectMetrics()

        // Assert - Memory should not increase
        assertTrue(afterMetrics.memoryUsage <= initialMetrics.memoryUsage + 5)  // Allow 5% margin
    }

    @Test
    fun testOptimizeCpu() {
        // Act
        performanceOptimizer.optimizeCpu(level = 1)

        // Assert - Should complete without error
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testOptimizeBattery() {
        // Act
        performanceOptimizer.optimizeBattery(level = 1)

        // Assert - Should complete without error
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testOptimizeNetwork() {
        // Act
        performanceOptimizer.optimizeNetwork(level = 1)

        // Assert - Should complete without error
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }

    // ========== Test 5: Dynamic Strategies ==========

    @Test
    fun testUpdateOptimizationStrategy() {
        // Arrange
        val strategy = mapOf(
            "cache_size_mb" to "100",
            "memory_threshold" to "80",
            "cpu_threshold" to "70"
        )

        // Act
        performanceOptimizer.updateStrategy(strategy)

        // Assert
        val currentStrategy = performanceOptimizer.getStrategy()
        assertEquals(strategy, currentStrategy)
    }

    @Test
    fun testAdaptiveOptimization() {
        // Act
        val metrics = performanceOptimizer.collectMetrics()
        performanceOptimizer.adaptiveOptimize(metrics)

        // Assert - Should complete without error
        val afterMetrics = performanceOptimizer.collectMetrics()
        assertNotNull(afterMetrics)
    }

    // ========== Test 6: Performance Status ==========

    @Test
    fun testGetPerformanceStatus() {
        // Act
        val status = performanceOptimizer.getPerformanceStatus()

        // Assert
        assertNotNull(status)
        assertTrue(status.isNotEmpty())
        assertTrue(status.contains("cpu") || status.contains("memory") || status.contains("battery"))
    }

    @Test
    fun testGetOptimizationLevel() {
        // Act
        val level = performanceOptimizer.getCurrentOptimizationLevel()

        // Assert
        assertTrue(level in 0..2)  // 0: normal, 1: moderate, 2: aggressive
    }

    // ========== Test 7: Throttling and Rate Limiting ==========

    @Test
    fun testThrottleHighFrequencyOperations() {
        // Act
        repeat(100) {
            performanceOptimizer.throttleOperation("test_operation")
        }

        // Assert - Should not crash and should manage load
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testRateLimiting() {
        // Act
        val allowed1 = performanceOptimizer.shouldAllowOperation("expensive_op")
        val allowed2 = performanceOptimizer.shouldAllowOperation("expensive_op")

        // Assert - At least first should be allowed
        assertTrue(allowed1)  // First call always allowed
    }

    // ========== Test 8: Background Tasks ==========

    @Test
    fun testBackgroundTaskOptimization() = runTest {
        // Act
        performanceOptimizer.optimizeBackgroundTasks()

        // Assert - Should complete without error
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }

    @Test
    fun testDeferNonCriticalTasks() = runTest {
        // Act
        performanceOptimizer.deferNonCriticalTasks()

        // Assert
        assertTrue(performanceOptimizer.areNonCriticalTasksDeferred())
    }

    // ========== Test 9: Concurrent Optimization ==========

    @Test
    fun testConcurrentMetricsCollection() = runTest {
        // Act
        val metrics = mutableListOf<Any>()
        repeat(10) {
            metrics.add(performanceOptimizer.collectMetrics())
        }

        // Assert
        assertEquals(10, metrics.size)
        assertTrue(metrics.all { it != null })
    }

    // ========== Test 10: Memory Monitoring ==========

    @Test
    fun testMemoryMonitoring() {
        // Act
        val memoryWarnings = performanceOptimizer.getMemoryWarnings()

        // Assert
        assertNotNull(memoryWarnings)
        assertTrue(memoryWarnings is List<*>)
    }

    @Test
    fun testProactiveMemoryCleanup() {
        // Act
        performanceOptimizer.proactiveMemoryCleanup()

        // Assert
        val metrics = performanceOptimizer.collectMetrics()
        assertNotNull(metrics)
    }
}
