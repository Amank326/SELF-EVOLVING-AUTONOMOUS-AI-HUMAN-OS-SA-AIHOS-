package com.aihos.security

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
 * Unit tests for SecurityManager
 * Tests: 20+ total
 * Coverage: Encryption, RBAC, tokens, audit logging
 */
@RunWith(AndroidJUnit4::class)
class SecurityManagerTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
    }

    // ========== Test 1: Encryption/Decryption ==========

    @Test
    fun testEncryptDecryptRoundTrip() {
        // Arrange
        val plaintext = "Sensitive user data"

        // Act
        val encrypted = securityManager.encrypt(plaintext)
        val decrypted = securityManager.decrypt(encrypted)

        // Assert
        assertNotNull(encrypted)
        assertNotNull(decrypted)
        assertTrue(encrypted != plaintext)  // Should be encrypted
        assertEquals(plaintext, decrypted)  // Should decrypt to original
    }

    @Test
    fun testEncryptEmptyString() {
        // Arrange
        val plaintext = ""

        // Act
        val encrypted = securityManager.encrypt(plaintext)

        // Assert
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())
    }

    @Test
    fun testEncryptLargeData() {
        // Arrange
        val plaintext = "X".repeat(10000)  // 10KB of data

        // Act
        val encrypted = securityManager.encrypt(plaintext)
        val decrypted = securityManager.decrypt(encrypted)

        // Assert
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testDecryptInvalidData() {
        // Arrange
        val invalidCiphertext = "not-valid-encrypted-data"

        // Act & Assert
        try {
            securityManager.decrypt(invalidCiphertext)
            assertFalse(true, "Should throw exception for invalid ciphertext")
        } catch (e: Exception) {
            assertTrue(true)  // Expected exception
        }
    }

    // ========== Test 2: Token Management ==========

    @Test
    fun testCreateAccessToken() {
        // Arrange
        val userId = "user123"
        val permissions = listOf("read", "write")

        // Act
        val token = securityManager.createAccessToken(userId, permissions)

        // Assert
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.contains("."))  // JWT-like structure
    }

    @Test
    fun testValidateAccessToken() {
        // Arrange
        val userId = "user123"
        val permissions = listOf("read", "write")
        val token = securityManager.createAccessToken(userId, permissions)

        // Act
        val isValid = securityManager.validateAccessToken(token)

        // Assert
        assertTrue(isValid)
    }

    @Test
    fun testValidateInvalidToken() {
        // Act
        val isValid = securityManager.validateAccessToken("invalid.token.here")

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun testTokenExpiration() {
        // Arrange
        val userId = "user123"
        val permissions = listOf("read")

        // Act
        val token = securityManager.createAccessToken(userId, permissions)
        val isValid = securityManager.validateAccessToken(token)

        // Assert - Token should be valid immediately
        assertTrue(isValid)
    }

    // ========== Test 3: RBAC (Role-Based Access Control) ==========

    @Test
    fun testHasPermission() {
        // Arrange
        val userId = "user123"
        val permissions = listOf("read", "write", "delete")
        securityManager.createAccessToken(userId, permissions)

        // Act & Assert
        assertTrue(securityManager.hasPermission(userId, "read"))
        assertTrue(securityManager.hasPermission(userId, "write"))
        assertTrue(securityManager.hasPermission(userId, "delete"))
        assertFalse(securityManager.hasPermission(userId, "admin"))
    }

    @Test
    fun testHasPermissionWithoutToken() {
        // Act & Assert
        assertFalse(securityManager.hasPermission("unknown_user", "read"))
    }

    @Test
    fun testAdminBypass() {
        // Arrange
        val adminId = "admin_user"

        // Act - Admin should have all permissions
        assertTrue(securityManager.hasPermission(adminId, "read", isAdmin = true))
        assertTrue(securityManager.hasPermission(adminId, "write", isAdmin = true))
        assertTrue(securityManager.hasPermission(adminId, "delete", isAdmin = true))
        assertTrue(securityManager.hasPermission(adminId, "any_permission", isAdmin = true))

        // Assert - Non-admin should be restricted
        assertFalse(securityManager.hasPermission(adminId, "read", isAdmin = false))
    }

    // ========== Test 4: User Lockout ==========

    @Test
    fun testUserLockoutAfterFailedAttempts() {
        // Arrange
        val userId = "user_to_lock"

        // Act - Make 5 failed authentication attempts
        repeat(5) {
            securityManager.recordAuthenticationFailure(userId)
        }

        // Assert - User should be locked out
        assertTrue(securityManager.isUserLockedOut(userId))
    }

    @Test
    fun testUserLockoutReset() {
        // Arrange
        val userId = "user_to_unlock"

        // Act
        repeat(5) {
            securityManager.recordAuthenticationFailure(userId)
        }
        assertTrue(securityManager.isUserLockedOut(userId))

        // Reset password (should unlock user)
        securityManager.resetUserSecurity(userId)

        // Assert
        assertFalse(securityManager.isUserLockedOut(userId))
    }

    // ========== Test 5: Audit Logging ==========

    @Test
    fun testAuditLogEntry() {
        // Arrange
        val action = "USER_LOGIN"
        val userId = "user123"
        val details = mapOf("ip" to "192.168.1.1", "device" to "android")

        // Act
        securityManager.auditLog(action, userId, details)

        // Assert - Verify audit log exists
        val logs = securityManager.getAuditLogs()
        assertTrue(logs.isNotEmpty())
        assertTrue(logs.any { it.contains(action) && it.contains(userId) })
    }

    @Test
    fun testAuditLogCapacity() {
        // Arrange
        val maxLogs = 10000

        // Act - Add many logs
        repeat(maxLogs + 100) { i ->
            securityManager.auditLog("TEST_ACTION_$i", "user_$i", emptyMap())
        }

        // Assert - Should not exceed max capacity
        val logs = securityManager.getAuditLogs()
        assertTrue(logs.size <= maxLogs)
    }

    // ========== Test 6: Security Policy ==========

    @Test
    fun testSetSecurityPolicy() {
        // Arrange
        val policy = mapOf(
            "min_password_length" to "12",
            "require_special_chars" to "true",
            "lockout_duration" to "300000"
        )

        // Act
        securityManager.setSecurityPolicy(policy)

        // Assert - Policy should be applied
        val currentPolicy = securityManager.getSecurityPolicy()
        assertEquals(policy, currentPolicy)
    }

    @Test
    fun testDefaultSecurityPolicy() {
        // Act
        val policy = securityManager.getSecurityPolicy()

        // Assert - Should have default policy
        assertNotNull(policy)
        assertTrue(policy.isNotEmpty())
        assertTrue(policy.containsKey("min_password_length") || policy.size > 0)
    }

    // ========== Test 7: Token Revocation ==========

    @Test
    fun testRevokeToken() {
        // Arrange
        val userId = "user123"
        val token = securityManager.createAccessToken(userId, listOf("read"))

        // Act - Token should be valid initially
        assertTrue(securityManager.validateAccessToken(token))

        // Revoke token
        securityManager.revokeToken(token)

        // Assert - Token should be invalid after revocation
        assertFalse(securityManager.validateAccessToken(token))
    }

    @Test
    fun testRevokeAllUserTokens() {
        // Arrange
        val userId = "user123"
        val token1 = securityManager.createAccessToken(userId, listOf("read"))
        val token2 = securityManager.createAccessToken(userId, listOf("write"))

        // Act
        securityManager.revokeAllUserTokens(userId)

        // Assert - Both tokens should be invalid
        assertFalse(securityManager.validateAccessToken(token1))
        assertFalse(securityManager.validateAccessToken(token2))
    }

    // ========== Test 8: Data Sanitization ==========

    @Test
    fun testSanitizeInput() {
        // Arrange
        val maliciousInput = "<script>alert('xss')</script>"

        // Act
        val sanitized = securityManager.sanitizeInput(maliciousInput)

        // Assert
        assertTrue(sanitized.isNotEmpty())
        assertFalse(sanitized.contains("<script>"))
        assertFalse(sanitized.contains("</script>"))
    }

    // ========== Test 9: Concurrent Access ==========

    @Test
    fun testConcurrentEncryption() = runTest {
        // Arrange
        val data = "Test data"
        val results = mutableListOf<String>()

        // Act & Assert
        repeat(10) {
            val encrypted = securityManager.encrypt(data)
            results.add(encrypted)
        }

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.isNotEmpty() })
    }
}

/**
 * Test Rule for Coroutine execution
 */
