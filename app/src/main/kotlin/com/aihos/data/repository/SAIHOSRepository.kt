package com.aihos.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.aihos.network.SAIHOSApiClient
import com.aihos.network.LoginRequest
import com.aihos.network.RegisterRequest
import com.aihos.network.LoginResponse
import com.aihos.network.InitResponse
import com.aihos.network.UserProfile
import com.aihos.network.MemoryResponse
import com.aihos.network.MemoriesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication and user data
 * Handles all API calls and local storage
 */
class SAIHOSRepository(private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "sa_aihos_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ==================== AUTHENTICATION ====================

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val result = SAIHOSApiClient.login(email, password)

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                // Save token and user securely
                saveToken(response.token)
                saveUser(response.user)

                Result.success(response)
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<LoginResponse> {
        return try {
            val result = SAIHOSApiClient.register(email, password, username)

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                saveToken(response.token)
                saveUser(response.user)

                Result.success(response)
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SYSTEM INITIALIZATION ====================

    suspend fun initializeSystem(): Result<InitResponse> {
        return try {
            val result = SAIHOSApiClient.initializeSystem()

            if (result.isSuccess) {
                // Mark as initialized
                encryptedPrefs.edit().putBoolean("system_initialized", true).apply()
                Result.success(result.getOrNull()!!)
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== USER PROFILE ====================

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            SAIHOSApiClient.getUserProfile()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MEMORY OPERATIONS ====================

    suspend fun storeMemory(type: String, content: String, metadata: Map<String, Any>? = null): Result<MemoryResponse> {
        return try {
            SAIHOSApiClient.storeMemory(type, content, metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun retrieveMemories(type: String? = null, limit: Int = 50): Result<MemoriesResponse> {
        return try {
            SAIHOSApiClient.retrieveMemories(type, limit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== TOKEN MANAGEMENT ====================

    fun saveToken(token: String) {
        encryptedPrefs.edit().putString("auth_token", token).apply()
        SAIHOSApiClient.setAuthToken(token)
    }

    fun getToken(): String? {
        return encryptedPrefs.getString("auth_token", null)
    }

    fun clearToken() {
        encryptedPrefs.edit().remove("auth_token").apply()
        SAIHOSApiClient.clearAuthToken()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    // ==================== USER STORAGE ====================

    fun saveUser(user: Any) {
        // Save user data (convert to JSON string)
        val gson = com.google.gson.Gson()
        val userJson = gson.toJson(user)
        encryptedPrefs.edit().putString("user_data", userJson).apply()
    }

    fun getUser(): String? {
        return encryptedPrefs.getString("user_data", null)
    }

    fun clearUser() {
        encryptedPrefs.edit().remove("user_data").apply()
    }

    // ==================== SESSION MANAGEMENT ====================

    fun logout() {
        clearToken()
        clearUser()
        encryptedPrefs.edit().putBoolean("system_initialized", false).apply()
    }

    fun isSystemInitialized(): Boolean {
        return encryptedPrefs.getBoolean("system_initialized", false)
    }

    // ==================== PREFERENCES ====================

    fun savePreference(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getPreference(key: String, default: String = ""): String {
        return encryptedPrefs.getString(key, default) ?: default
    }

    fun saveIntPreference(key: String, value: Int) {
        encryptedPrefs.edit().putInt(key, value).apply()
    }

    fun getIntPreference(key: String, default: Int = 0): Int {
        return encryptedPrefs.getInt(key, default)
    }

    fun saveBooleanPreference(key: String, value: Boolean) {
        encryptedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getBooleanPreference(key: String, default: Boolean = false): Boolean {
        return encryptedPrefs.getBoolean(key, default)
    }
}
