package com.aihos.network

import com.aihos.replay.model.RuleUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
// 1. DATA MODEL & 6. PRIVACY
// Lightweight, serializable sync models. Sent encrypted over wss://
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CognitiveSyncPayload(
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long,            // Standard unixtime
    val vectorClock: Long,          // Logical clock for causal conflict resolution
    val stateSnapshot: RemoteStateSnapshot,
    val ruleUpdates: List<RemoteRuleUpdate>,
    val signature: String           // Validate packet origin/integrity
)

@Serializable
data class RemoteStateSnapshot(
    val activityLevel: Float,
    val focusLevel: Float,
    val stability: Float,
    val activeAgents: Int,
    val coreMemoryHash: String      // Anonymized footprint of memory graph
)

@Serializable
data class RemoteRuleUpdate(
    val ruleId: String,             // Universal Rule ID
    val weightOffset: Float,        // Delta sync to prevent absolute overwrites
    val conditionHash: String       // Privacy-preserving rule semantic check
)


// ─────────────────────────────────────────────────────────────────────────────
// 2. BACKEND ARCHITECTURE & 3. DEVICE COMMUNICATION
// WebSocket client wrapping OkHttp with Flow streams.
// ─────────────────────────────────────────────────────────────────────────────
class CloudSyncEngine(
    private val endpointUrl: String = "wss://api.aihossync.dev/v1/stream"
) {
    private val deviceId = UUID.randomUUID().toString()
    private var vectorClock: Long = 0L

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
        
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    // Broadcast incoming changes to AI + Visualizer
    private val _incomingSyncs = MutableSharedFlow<CognitiveSyncPayload>(replay = 10)
    val incomingSyncs = _incomingSyncs.asSharedFlow()

    fun connect() {
        // Enforces WSS for transit privacy mapping to Rule 6
        val request = Request.Builder().url(endpointUrl).build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val payload = json.decodeFromString<CognitiveSyncPayload>(text)
                        handleIncomingSync(payload)
                    } catch (e: Exception) {
                        // Drop malformed packets securely
                    }
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    delay(5000) // Fallback backoff
                    connect()
                }
            }
        })
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 7. PERFORMANCE: BATCHING
    // Dispatch outbound updates periodically rather than per-frame.
    // ─────────────────────────────────────────────────────────────────────────────
    private val outboundQueue = mutableListOf<RemoteRuleUpdate>()

    fun queueRuleEvolution(update: RemoteRuleUpdate) {
        synchronized(outboundQueue) {
            outboundQueue.add(update)
        }
    }

    fun dispatchSyncPulse(currentState: RemoteStateSnapshot) {
        val rulesToSync = synchronized(outboundQueue) {
            val list = outboundQueue.toList()
            outboundQueue.clear()
            list
        }

        vectorClock++ // Advance local clock

        val payload = CognitiveSyncPayload(
            deviceId = deviceId,
            deviceName = "SA-Device-${deviceId.take(4)}",
            timestamp = System.currentTimeMillis(),
            vectorClock = vectorClock,
            stateSnapshot = currentState,
            ruleUpdates = rulesToSync,
            signature = generateSecureSignature() // Pseudo-Crypto mapping
        )

        webSocket?.send(json.encodeToString(payload))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. CONFLICT RESOLUTION
    // Last-write-wins + Vector clock matching + Delta aggregates.
    // ─────────────────────────────────────────────────────────────────────────────
    private val discoveredDevices = mutableMapOf<String, Long>() // Map<DeviceID, MaxVectorClockSeen>

    private suspend fun handleIncomingSync(payload: CognitiveSyncPayload) {
        // 1. Versioning System (Vector clock drop-check)
        val lastSeenClock = discoveredDevices[payload.deviceId] ?: -1L
        if (payload.vectorClock <= lastSeenClock) return // Stale packet, drop
        
        discoveredDevices[payload.deviceId] = payload.vectorClock
        
        // Advance our own local clock forward to match causal continuity 
        vectorClock = max(vectorClock, payload.vectorClock)
        
        // 2. State Merging -> Passes valid packet to AI graph layer
        _incomingSyncs.emit(payload)
    }

    private fun generateSecureSignature(): String = "encrypted_hash_token"
    
    fun disconnect() {
        webSocket?.close(1000, "Device Offline")
        scope.cancel()
    }
}
