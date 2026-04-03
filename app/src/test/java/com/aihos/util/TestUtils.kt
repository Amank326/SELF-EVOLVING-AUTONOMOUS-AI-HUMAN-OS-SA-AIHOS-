package com.aihos.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.Mockito
import org.mockito.kotlin.mock

/**
 * JUnit Rule for setting up coroutine test dispatcher
 * Usage: @get:Rule val coroutineRule = CoroutineTestRule()
 */
class CoroutineTestRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestRule {

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            Dispatchers.setMain(testDispatcher)
            try {
                base.evaluate()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }
}

/**
 * Mock factory for creating mock objects used in testing
 */
object MockFactory {

    /**
     * Create a mock DataStore for testing
     */
    fun <T> createMockDataStore(): DataStore<T> {
        return mock()
    }

    /**
     * Create a mock Context for testing
     */
    fun createMockContext(): Context {
        return ApplicationProvider.getApplicationContext()
    }

    /**
     * Create a mock Preferences DataStore
     */
    fun createMockPreferencesDataStore(): DataStore<Preferences> {
        return mock()
    }

    /**
     * Create a mock exception for testing error handling
     */
    fun createIOException(message: String = "Connection timeout"): Exception {
        return java.io.IOException(message)
    }

    fun createSQLException(message: String = "Database locked"): Exception {
        return java.sql.SQLException(message)
    }

    fun createRuntimeException(message: String = "Runtime error"): Exception {
        return RuntimeException(message)
    }

    fun createOutOfMemoryError(message: String = "Out of memory"): Error {
        return OutOfMemoryError(message)
    }
}

/**
 * Test utilities for common testing patterns
 */
object TestUtils {

    /**
     * Run a suspendable block and wait for completion
     */
    suspend fun <T> runTest(block: suspend () -> T): T {
        return block()
    }

    /**
     * Create a fake stack trace string for testing
     */
    fun createStackTrace(
        className: String = "TestClass",
        methodName: String = "testMethod",
        lineNumber: Int = 42
    ): String {
        return """at com.aihos.$className.$methodName($className.kt:$lineNumber)
            |at com.aihos.TestRunner.runTest(TestRunner.kt:10)
            |at java.lang.Thread.run(Thread.java:745)""".trimMargin()
    }

    /**
     * Create a fake crash log content
     */
    fun createCrashLogContent(
        exception: String = "NullPointerException",
        message: String = "Attempt to invoke virtual method",
        stackTrace: String = createStackTrace()
    ): String {
        return """
            Exception: $exception
            Message: $message
            Stack Trace:
            $stackTrace
        """.trimIndent()
    }

    /**
     * Create mock backup file content (SQLite header)
     */
    fun createMockSQLiteBackup(): ByteArray {
        // SQLite database header: "SQLite format 3\000"
        val header = "SQLite format 3\u0000".toByteArray()
        val padding = ByteArray(1000)  // Additional padding to simulate database file
        return header + padding
    }

    /**
     * Verify that two lists are equal regardless of order
     */
    fun <T> listsAreEqual(list1: List<T>, list2: List<T>): Boolean {
        return list1.size == list2.size && list1.toSet() == list2.toSet()
    }

    /**
     * Create a delay function for testing timeout scenarios
     */
    fun createDelayedOperation(delayMs: Long, shouldThrow: Boolean = false): suspend () -> String {
        return suspend {
            kotlinx.coroutines.delay(delayMs)
            if (shouldThrow) {
                throw Exception("Operation timeout")
            }
            "Operation completed"
        }
    }
}

/**
 * Assertion helpers for Phase 2.4 services
 */
object TestAssertions {

    /**
     * Assert that a recovery record matches expected values
     */
    fun assertErrorRecordValid(
        record: com.aihos.ai.resilience.ErrorRecord,
        expectedType: String,
        expectedSeverity: String? = null
    ) {
        assert(record.id.isNotEmpty()) { "Error record ID should not be empty" }
        assert(record.errorType == expectedType) { "Error type mismatch" }
        assert(record.message.isNotEmpty()) { "Error message should not be empty" }
        assert(record.stackTrace.isNotEmpty()) { "Stack trace should not be empty" }
        if (expectedSeverity != null) {
            assert(record.severity == expectedSeverity) { "Severity mismatch" }
        }
    }

    /**
     * Assert that system health is in expected state
     */
    fun assertSystemHealthValid(
        health: com.aihos.ai.resilience.SystemHealth,
        expectHealthy: Boolean? = null
    ) {
        if (expectHealthy != null) {
            assert(health.isHealthy == expectHealthy) { "Health state mismatch" }
        }
        assert(health.recoveryRate in 0f..100f) { "Recovery rate out of bounds" }
        assert(health.lastErrorTime >= 0) { "Error time should be non-negative" }
    }

    /**
     * Assert that backup info is valid
     */
    fun assertBackupInfoValid(
        backup: com.aihos.data.backup.BackupInfo,
        shouldBeVerified: Boolean = true
    ) {
        assert(backup.backupId.isNotEmpty()) { "Backup ID should not be empty" }
        assert(backup.size > 0) { "Backup size should be positive" }
        if (shouldBeVerified) {
            assert(backup.isVerified) { "Backup should be verified" }
        }
    }

    /**
     * Assert that network status is valid
     */
    fun assertNetworkStatusValid(status: com.aihos.network.resilience.NetworkStatus) {
        // Basic validity checks
        assert(status.failureCount >= 0) { "Failure count should be non-negative" }
        assert(status.lastFailureTime >= 0) { "Failure time should be non-negative" }
    }

    /**
     * Assert that a list of errors was properly categorized
     */
    fun assertErrorsProperlyTyped(
        errors: List<com.aihos.ai.resilience.ErrorRecord>,
        expectedTypes: Set<String>
    ) {
        val actualTypes = errors.map { it.errorType }.toSet()
        assert(actualTypes == expectedTypes) { 
            "Error types mismatch. Expected: $expectedTypes, Got: $actualTypes" 
        }
    }
}
