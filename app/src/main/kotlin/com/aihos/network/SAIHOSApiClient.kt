package com.aihos.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ==================== API MODELS ====================

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: String,
    val email: String,
    val username: String,
    val initialized: Boolean? = false
)

data class UserProfile(
    val success: Boolean,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String,
    val username: String,
    val profile: ProfileData,
    val systemState: SystemState,
    val createdAt: String
)

data class ProfileData(
    val avatar: String? = null,
    val bio: String? = null
)

data class SystemState(
    val initialized: Boolean,
    val initializedAt: String? = null,
    val lastLogin: String? = null
)

data class InitResponse(
    val success: Boolean,
    val message: String,
    val systemState: SystemState
)

data class MemoryRequest(
    val type: String,
    val content: String,
    val metadata: Map<String, Any>? = null
)

data class MemoryResponse(
    val success: Boolean,
    val memory: MemoryData
)

data class MemoryData(
    val id: String,
    val type: String,
    val content: String,
    val timestamp: String
)

data class MemoriesResponse(
    val success: Boolean,
    val memories: List<MemoryData>
)

data class ApiError(
    val error: String
)

// ==================== RETROFIT SERVICE ====================

interface SAIHOSApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("system/init")
    suspend fun initializeSystem(): InitResponse

    @GET("user/profile")
    suspend fun getUserProfile(): UserProfile

    @POST("memory/store")
    suspend fun storeMemory(@Body request: MemoryRequest): MemoryResponse

    @GET("memory/retrieve")
    suspend fun retrieveMemories(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50
    ): MemoriesResponse
}

// ==================== API CLIENT ====================

object SAIHOSApiClient {
    // Use local network IP for real device, 10.0.2.2 for emulator
    // Change this to your computer's local IP when testing on real device
    private const val BASE_URL = "http://192.168.29.106:5000/"
    private const val TAG = "SAIHOSApi"

    private var authToken: String? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            // Add auth token to requests
            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val request = requestBuilder.build()
            Log.d(TAG, "Request: ${request.url}")

            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(SAIHOSApiService::class.java)

    fun setAuthToken(token: String) {
        authToken = token
        Log.d(TAG, "Auth token set")
    }

    fun clearAuthToken() {
        authToken = null
        Log.d(TAG, "Auth token cleared")
    }

    // ==================== API METHODS ====================

    suspend fun login(email: String, password: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))
                authToken = response.token
                Log.d(TAG, "Login successful: ${response.user.email}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Login failed: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun register(email: String, password: String, username: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(RegisterRequest(email, password, username))
                authToken = response.token
                Log.d(TAG, "Registration successful: ${response.user.email}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun initializeSystem(): Result<InitResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.initializeSystem()
                Log.d(TAG, "System initialized: ${response.systemState.initialized}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun getUserProfile(): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserProfile()
                Log.d(TAG, "Profile fetched: ${response.user.username}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Profile fetch failed: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun storeMemory(type: String, content: String, metadata: Map<String, Any>? = null): Result<MemoryResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.storeMemory(MemoryRequest(type, content, metadata))
                Log.d(TAG, "Memory stored: ${response.memory.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Memory store failed: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun retrieveMemories(type: String? = null, limit: Int = 50): Result<MemoriesResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.retrieveMemories(type, limit)
                Log.d(TAG, "Memories retrieved: ${response.memories.size}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Memory retrieve failed: ${e.message}")
                Result.failure(e)
            }
        }
}
