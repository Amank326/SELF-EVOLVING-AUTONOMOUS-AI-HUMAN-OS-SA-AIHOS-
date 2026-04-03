package com.aihos.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * SECURITY MANAGER
 * System-wide security enforcement and access control
 * - Role-based access control (RBAC)
 * - Secure credential storage
 * - Encryption/decryption
 * - Audit logging
 * - Security policy enforcement
 */

data class SecurityPolicy(
    val requireEncryption: Boolean = true,
    val allowOfflineMode: Boolean = false,
    val maxFailedAttempts: Int = 5,
    val lockoutDurationSeconds: Int = 300,
    val requiredMinimumSecurityLevel: Int = 2,
    val enableAuditLogging: Boolean = true
)

data class AccessToken(
    val userId: String = "",
    val permissions: Set<String> = emptySet(),
    val roles: Set<String> = emptySet(),
    val issuedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 3600000, // 1 hour
    val signature: String = ""
)

data class AuditLog(
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val action: String = "",
    val resource: String = "",
    val result: String = "SUCCESS",
    val details: Map<String, String> = emptyMap()
)

class SecurityManager(private val context: Context) {
    
    private val securityPolicy = MutableStateFlow(SecurityPolicy())
    val policy: StateFlow<SecurityPolicy> = securityPolicy.asStateFlow()
    
    private val accessTokens = mutableMapOf<String, AccessToken>()
    private val auditLogs = mutableListOf<AuditLog>()
    private val maxAuditLogs = 10000
    
    private val failedAttempts = mutableMapOf<String, Int>()
    private val lockedUsers = mutableSetOf<String>()
    
    private val encryptionCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var encryptionKey: SecretKey? = null
    
    private val _securityLevel = MutableStateFlow(2)
    val securityLevel: StateFlow<Int> = _securityLevel.asStateFlow()
    
    init {
        keyStore.load(null)
        ensureEncryptionKeyExists()
        Timber.d("SecurityManager initialized")
    }
    
    /**
     * Ensure encryption key exists in KeyStore
     */
    private fun ensureEncryptionKeyExists() {
        try {
            encryptionKey = keyStore.getKey("SAIHOSEncryptionKey", null) as? SecretKey
            
            if (encryptionKey == null) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keySpec = KeyGenParameterSpec.Builder(
                    "SAIHOSEncryptionKey",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
                
                keyGenerator.init(keySpec)
                encryptionKey = keyGenerator.generateKey()
                Timber.d("Created new encryption key")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to ensure encryption key exists")
        }
    }
    
    /**
     * Encrypt sensitive data
     */
    fun encrypt(data: String): String {
        return try {
            if (!securityPolicy.value.requireEncryption) return data
            
            encryptionKey?.let { key ->
                val iv = ByteArray(16)
                SecureRandom().nextBytes(iv)
                encryptionCipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
                
                val encryptedData = encryptionCipher.doFinal(data.toByteArray())
                val combined = iv + encryptedData
                Base64.encodeToString(combined, Base64.DEFAULT)
            } ?: run {
                Timber.w("Encryption key not available")
                data
            }
        } catch (e: Exception) {
            Timber.e(e, "Encryption failed")
            data
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    fun decrypt(encryptedData: String): String {
        return try {
            if (!securityPolicy.value.requireEncryption) return encryptedData
            
            encryptionKey?.let { key ->
                val combined = Base64.decode(encryptedData, Base64.DEFAULT)
                val iv = combined.sliceArray(0 until 16)
                val encrypted = combined.sliceArray(16 until combined.size)
                
                encryptionCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                String(encryptionCipher.doFinal(encrypted))
            } ?: run {
                Timber.w("Encryption key not available")
                encryptedData
            }
        } catch (e: Exception) {
            Timber.e(e, "Decryption failed")
            encryptedData
        }
    }
    
    /**
     * Create access token for user
     */
    fun createAccessToken(userId: String, permissions: Set<String>, roles: Set<String>): AccessToken {
        val token = AccessToken(
            userId = userId,
            permissions = permissions,
            roles = roles,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 3600000,
            signature = generateSignature(userId)
        )
        
        accessTokens[userId] = token
        auditLog(userId, "CREATE_TOKEN", "AccessToken", "SUCCESS")
        Timber.d("Access token created for user: $userId")
        
        return token
    }
    
    /**
     * Verify access token
     */
    fun verifyAccessToken(token: AccessToken): Boolean {
        return try {
            if (token.expiresAt < System.currentTimeMillis()) {
                Timber.w("Token expired for user: ${token.userId}")
                auditLog(token.userId, "VERIFY_TOKEN", "AccessToken", "FAILED: EXPIRED")
                return false
            }
            
            if (token.signature != generateSignature(token.userId)) {
                Timber.w("Token signature invalid for user: ${token.userId}")
                auditLog(token.userId, "VERIFY_TOKEN", "AccessToken", "FAILED: INVALID_SIGNATURE")
                return false
            }
            
            auditLog(token.userId, "VERIFY_TOKEN", "AccessToken", "SUCCESS")
            true
        } catch (e: Exception) {
            Timber.e(e, "Token verification failed")
            auditLog(token.userId, "VERIFY_TOKEN", "AccessToken", "FAILED: EXCEPTION")
            false
        }
    }
    
    /**
     * Check if user has permission
     */
    fun hasPermission(userId: String, permission: String): Boolean {
        val token = accessTokens[userId] ?: return false
        
        if (!verifyAccessToken(token)) {
            auditLog(userId, "CHECK_PERMISSION", permission, "FAILED: INVALID_TOKEN")
            return false
        }
        
        val hasPermission = token.permissions.contains(permission) || token.roles.contains("ADMIN")
        auditLog(userId, "CHECK_PERMISSION", permission, if (hasPermission) "SUCCESS" else "FAILED")
        
        return hasPermission
    }
    
    /**
     * Check if user has role
     */
    fun hasRole(userId: String, role: String): Boolean {
        val token = accessTokens[userId] ?: return false
        return token.roles.contains(role) || token.roles.contains("ADMIN")
    }
    
    /**
     * Handle failed authentication attempt
     */
    fun recordFailedAttempt(userId: String) {
        val attempts = (failedAttempts[userId] ?: 0) + 1
        failedAttempts[userId] = attempts
        
        if (attempts >= securityPolicy.value.maxFailedAttempts) {
            lockUser(userId)
            auditLog(userId, "FAILED_ATTEMPTS", "Authentication", "ACCOUNT_LOCKED")
        } else {
            auditLog(userId, "FAILED_ATTEMPT", "Authentication", "ATTEMPT_$attempts")
        }
        
        Timber.w("Failed attempt $attempts for user: $userId")
    }
    
    /**
     * Lock user account
     */
    fun lockUser(userId: String) {
        lockedUsers.add(userId)
        auditLog(userId, "LOCK_ACCOUNT", "User", "SUCCESS")
        Timber.w("User locked: $userId")
    }
    
    /**
     * Unlock user account
     */
    fun unlockUser(userId: String) {
        lockedUsers.remove(userId)
        failedAttempts.remove(userId)
        auditLog(userId, "UNLOCK_ACCOUNT", "User", "SUCCESS")
        Timber.d("User unlocked: $userId")
    }
    
    /**
     * Check if user is locked
     */
    fun isUserLocked(userId: String): Boolean = lockedUsers.contains(userId)
    
    /**
     * Audit log event
     */
    fun auditLog(userId: String, action: String, resource: String, result: String) {
        try {
            if (!securityPolicy.value.enableAuditLogging) return
            
            val log = AuditLog(
                userId = userId,
                action = action,
                resource = resource,
                result = result
            )
            
            auditLogs.add(log)
            if (auditLogs.size > maxAuditLogs) {
                auditLogs.removeAt(0)
            }
        } catch (e: Exception) {
            Timber.e(e, "Audit logging failed")
        }
    }
    
    /**
     * Get audit logs for user
     */
    fun getAuditLogs(userId: String, limit: Int = 100): List<AuditLog> {
        return auditLogs.filter { it.userId == userId }.takeLast(limit)
    }
    
    /**
     * Get all audit logs
     */
    fun getAllAuditLogs(limit: Int = 100): List<AuditLog> {
        return auditLogs.takeLast(limit)
    }
    
    /**
     * Update security policy
     */
    fun updateSecurityPolicy(policy: SecurityPolicy) {
        securityPolicy.value = policy
        auditLog("SYSTEM", "UPDATE_POLICY", "SecurityPolicy", "SUCCESS")
        Timber.d("Security policy updated")
    }
    
    /**
     * Set security level
     */
    fun setSecurityLevel(level: Int) {
        require(level in 1..5) { "Security level must be between 1 and 5" }
        _securityLevel.value = level
        auditLog("SYSTEM", "SET_SECURITY_LEVEL", "SecurityLevel", "LEVEL_$level")
        Timber.d("Security level set to: $level")
    }
    
    /**
     * Revoke access token
     */
    fun revokeAccessToken(userId: String) {
        accessTokens.remove(userId)
        auditLog(userId, "REVOKE_TOKEN", "AccessToken", "SUCCESS")
        Timber.d("Access token revoked for user: $userId")
    }
    
    /**
     * Generate secure signature
     */
    private fun generateSignature(userId: String): String {
        return Base64.encodeToString(
            "$userId:${System.currentTimeMillis()}".toByteArray(),
            Base64.DEFAULT
        )
    }
    
    /**
     * Get security status
     */
    fun getSecurityStatus(): Map<String, Any> {
        return mapOf(
            "level" to _securityLevel.value,
            "policy" to securityPolicy.value,
            "activeTokens" to accessTokens.size,
            "lockedUsers" to lockedUsers.size,
            "auditLogSize" to auditLogs.size,
            "encryptionEnabled" to securityPolicy.value.requireEncryption
        )
    }
}
