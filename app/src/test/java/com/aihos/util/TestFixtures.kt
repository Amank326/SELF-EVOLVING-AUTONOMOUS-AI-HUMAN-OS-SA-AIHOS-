package com.aihos.util

import com.aihos.ai.resilience.ErrorRecord
import com.aihos.ai.resilience.SystemHealth
import com.aihos.ai.resilience.RecoveryStrategy
import com.aihos.ai.resilience.CrashLog
import com.aihos.data.backup.BackupInfo
import com.aihos.network.resilience.NetworkRequest
import com.aihos.network.resilience.NetworkStatus
import java.util.UUID

/**
 * Test fixtures providing reusable test data for Phase 2.4 services
 */
object TestFixtures {

    // ========== ErrorRecoveryManager Fixtures ==========

    fun createErrorRecord(
        errorType: String = "TEST",
        message: String = "Test error",
        severity: String = "MEDIUM",
        isRecovered: Boolean = false
    ) = ErrorRecord(
        id = UUID.randomUUID().toString(),
        errorType = errorType,
        message = message,
        stackTrace = "at com.aihos.test.TestClass.testMethod(TestClass.kt:42)",
        severity = severity,
        timestamp = System.currentTimeMillis(),
        isRecovered = isRecovered,
        recoveryAttempts = 0
    )

    fun createNetworkErrorRecord() = createErrorRecord(
        errorType = "NETWORK",
        message = "Connection timeout",
        severity = "HIGH"
    )

    fun createDatabaseErrorRecord() = createErrorRecord(
        errorType = "DATABASE",
        message = "Database locked",
        severity = "HIGH"
    )

    fun createMemoryErrorRecord() = createErrorRecord(
        errorType = "MEMORY",
        message = "Out of memory",
        severity = "CRITICAL"
    )

    fun createSystemHealth(
        isHealthy: Boolean = true,
        consecutiveErrors: Int = 0,
        recoveryRate: Float = 100f
    ) = SystemHealth(
        isHealthy = isHealthy,
        consecutiveErrors = consecutiveErrors,
        lastErrorTime = System.currentTimeMillis(),
        totalErrors = consecutiveErrors,
        recoveryRate = recoveryRate,
        databaseHealth = "HEALTHY",
        performanceState = "NORMAL",
        memoryState = "NORMAL"
    )

    fun createRecoveryStrategy(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L
    ) = RecoveryStrategy(
        maxRetries = maxRetries,
        initialDelayMs = initialDelayMs,
        maxDelayMs = 30000L,
        backoffMultiplier = 2f,
        timeoutMs = 60000L
    )

    // ========== DatabaseBackupManager Fixtures ==========

    fun createBackupInfo(
        backupId: String = UUID.randomUUID().toString(),
        isValid: Boolean = true
    ) = BackupInfo(
        backupId = backupId,
        timestamp = System.currentTimeMillis(),
        backupSize = 102400L,  // 100 KB
        isValid = isValid,
        databaseName = "aihos_database",
        comment = "Test backup"
    )

    fun createMultipleBackups(count: Int = 3): List<BackupInfo> {
        return (0 until count).map { i ->
            createBackupInfo(
                backupId = "backup_${System.currentTimeMillis() + i}",
                isValid = true
            )
        }
    }
    // ========== CrashHandler Fixtures ==========

    fun createCrashLog(
        exceptionType: String = "NullPointerException",
        message: String = "Test exception",
        threadName: String = "main"
    ) = CrashLog(
        crashId = UUID.randomUUID().toString(),
        timestamp = System.currentTimeMillis(),
        exceptionType = exceptionType,
        message = message,
        stackTrace = "at com.aihos.test.TestClass.testMethod(TestClass.kt:42)",
        threadName = threadName,
        processId = android.os.Process.myPid(),
        isHandled = false
    )
    // ========== NetworkResilience Fixtures ==========

    fun createNetworkRequest(
        url: String = "https://api.example.com/data",
        method: String = "GET"
    ) = NetworkRequest(
        id = UUID.randomUUID().toString(),
        url = url,
        method = method,
        payload = null,
        retryCount = 0,
        timestamp = System.currentTimeMillis(),
        priority = 1
    )

    fun createNetworkStatus(
        isConnected: Boolean = true,
        isMetered: Boolean = false
    ) = NetworkStatus(
        isConnected = isConnected,
        isWifi = !isMetered,
        isMobile = isMetered,
        isMetered = isMetered,
        pendingRequests = 0,
        failureCount = 0
    )

    fun createMultipleNetworkRequests(count: Int = 5): List<NetworkRequest> {
        return (0 until count).map { i ->
            createNetworkRequest(
                url = "https://api.example.com/data/$i",
                method = if (i % 2 == 0) "GET" else "POST"
            )
        }
    }

    // ========== State Fixtures ==========

    fun createApplicationState(
        lastScreenName: String = "DashboardScreen",
        navigationState: String? = null,
        userPreferences: String? = null,
        sessionId: String = UUID.randomUUID().toString()
    ) = com.aihos.ai.resilience.ApplicationState(
        lastScreenName = lastScreenName,
        navigationState = navigationState,
        userPreferences = userPreferences,
        cachedData = mapOf(
            "dashboard_items" to "5",
            "settings_theme" to "dark"
        ),
        sessionId = sessionId,
        lastUpdateTime = System.currentTimeMillis(),
        isCorrupted = false
    )

    // ========== Test Constants ==========

    const val MOCK_DATABASE_SIZE = 1048576L  // 1 MB
    const val MOCK_API_URL = "https://api.example.com"
    const val MOCK_SESSION_ID = "test_session_12345"
    const val MOCK_ERROR_MESSAGE = "Test error message"
    const val MOCK_STACK_TRACE = "at com.aihos.test.TestClass.method(TestClass.kt:10)\n" +
            "at com.aihos.test.AnotherClass.caller(AnotherClass.kt:20)"
}
