package com.aihos.replay.engine

import com.aihos.replay.data.*
import com.aihos.replay.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

// ─────────────────────────────────────────────────────────────────────────────
// EVENT RECORDER
// Records AI cognitive events in real-time without blocking the decision loop.
// Uses a dedicated background coroutine with batched writes.
// ─────────────────────────────────────────────────────────────────────────────

class EventRecorder(
    private val replayEventDao: ReplayEventDao,
    private val replaySessionDao: ReplaySessionDao,
    private val maxEventsInMemory: Int = 500,
    private val maxStorageBytes: Long = 50 * 1024 * 1024,  // 50 MB limit
    private val batchWriteSize: Int = 20,
    private val batchWriteIntervalMs: Long = 2000
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentSessionId: String = ""
    private var sequenceCounter: Long = 0
    private var isRecording = false

    // Lock-free write buffer
    private val writeBuffer = ConcurrentLinkedQueue<ReplayEventEntity>()

    // ── Session lifecycle ────────────────────────────────────────────────

    fun startSession(description: String? = null): String {
        currentSessionId = UUID.randomUUID().toString()
        sequenceCounter = 0
        isRecording = true

        scope.launch {
            replaySessionDao.insert(
                ReplaySessionEntity(
                    sessionId = currentSessionId,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    eventCount = 0,
                    totalSizeBytes = 0,
                    description = description ?: "Session ${currentSessionId.take(8)}"
                )
            )
        }

        // Start background batch writer
        startBatchWriter()

        Timber.i("Replay recording started: session=$currentSessionId")
        return currentSessionId
    }

    fun stopSession() {
        isRecording = false
        // Flush remaining buffer
        scope.launch {
            flushBuffer()
            replaySessionDao.getById(currentSessionId)?.let { session ->
                val count = replayEventDao.getEventCount(currentSessionId)
                val size = replayEventDao.getTotalSizeBytes(currentSessionId) ?: 0
                replaySessionDao.update(
                    session.copy(
                        endTime = System.currentTimeMillis(),
                        eventCount = count,
                        totalSizeBytes = size
                    )
                )
            }
        }
        Timber.i("Replay recording stopped: session=$currentSessionId, events=$sequenceCounter")
    }

    // ── Event recording ──────────────────────────────────────────────────

    /**
     * Record a replay event.  Non-blocking — adds to an in-memory queue
     * that is flushed to Room in batches on background IO.
     */
    fun recordEvent(event: ReplayEvent) {
        if (!isRecording) return

        val entity = ReplayEventEntity(
            id = event.id,
            sequenceNumber = sequenceCounter++,
            timestamp = event.timestamp,
            sessionId = currentSessionId,
            eventType = event.eventType.name,
            cognitiveSnapshotJson = json.encodeToString(event.cognitiveSnapshot),
            decisionSnapshotJson = event.decisionSnapshot?.let { json.encodeToString(it) },
            ruleUpdatesJson = json.encodeToString(event.ruleUpdates),
            memoryChangesJson = json.encodeToString(event.memoryChanges),
            metadataJson = json.encodeToString(event.metadata),
            cognitiveLoad = event.cognitiveSnapshot.cognitiveLoad,
            confidenceLevel = event.cognitiveSnapshot.confidenceLevel,
            actionTaken = event.decisionSnapshot?.action,
            estimatedSizeBytes = event.estimatedSizeBytes
        )

        writeBuffer.add(entity)

        // Enforce memory limit
        while (writeBuffer.size > maxEventsInMemory) {
            writeBuffer.poll()
            Timber.w("Replay write buffer overflow — dropping oldest event")
        }
    }

    /**
     * Convenience: record a decision cycle event from the autonomy controller.
     */
    fun recordDecisionCycle(
        cognitiveSnapshot: CognitiveSnapshot,
        decisionSnapshot: DecisionSnapshot,
        ruleUpdates: List<RuleUpdate> = emptyList(),
        memoryChanges: List<MemoryChange> = emptyList()
    ) {
        recordEvent(
            ReplayEvent(
                sequenceNumber = sequenceCounter,
                sessionId = currentSessionId,
                eventType = ReplayEventType.DECISION_CYCLE,
                cognitiveSnapshot = cognitiveSnapshot,
                decisionSnapshot = decisionSnapshot,
                ruleUpdates = ruleUpdates,
                memoryChanges = memoryChanges
            )
        )
    }

    // ── Background batch writer ──────────────────────────────────────────

    private fun startBatchWriter() {
        scope.launch {
            while (isRecording) {
                delay(batchWriteIntervalMs)
                flushBuffer()
                enforceStorageLimits()
            }
        }
    }

    private suspend fun flushBuffer() {
        if (writeBuffer.isEmpty()) return

        val batch = mutableListOf<ReplayEventEntity>()
        while (batch.size < batchWriteSize) {
            val event = writeBuffer.poll() ?: break
            batch.add(event)
        }

        if (batch.isNotEmpty()) {
            try {
                replayEventDao.insertBatch(batch)
                Timber.d("Flushed ${batch.size} replay events to database")
            } catch (e: Exception) {
                Timber.e(e, "Failed to flush replay events")
                // Re-queue failed events
                writeBuffer.addAll(batch)
            }
        }
    }

    private suspend fun enforceStorageLimits() {
        val totalBytes = replayEventDao.getTotalStorageBytes() ?: 0
        if (totalBytes > maxStorageBytes) {
            val excess = ((totalBytes - maxStorageBytes * 0.8) / 256).toInt()
            if (excess > 0) {
                replayEventDao.deleteOldest(excess)
                Timber.w("Pruned $excess old replay events (storage: ${totalBytes / 1024}KB)")
            }
        }
    }

    fun destroy() {
        stopSession()
        scope.cancel()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// PART 3 — REPLAY CONTROLLER
// Full-featured playback controller: play, pause, step, jump, seek.
// Operates on a separate data path to avoid affecting the live system.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Playback state machine.
 */
enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    STEPPING_FORWARD,
    STEPPING_BACKWARD,
    SEEKING
}

/**
 * Configuration for replay playback.
 */
data class PlaybackConfig(
    val playbackSpeed: Float = 1.0f,         // 0.25x to 4x
    val preloadBatchSize: Int = 50,
    val autoStopAtEnd: Boolean = true,
    val highlightChanges: Boolean = true,
    val showDiffOverlay: Boolean = true
)

/**
 * Replay Controller — the central replay engine.
 *
 * Design principles:
 * - Completely isolated from the live AI system (read-only data access)
 * - Non-blocking: all data loading happens on IO, all state updates on Main
 * - Preloads event batches for smooth playback
 * - Emits reactive state via StateFlow for UI/renderer binding
 */
class ReplayController(
    private val replayEventDao: ReplayEventDao,
    private val replaySessionDao: ReplaySessionDao
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ── Observable state ─────────────────────────────────────────────────
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentEvent = MutableStateFlow<ReplayEvent?>(null)
    val currentEvent: StateFlow<ReplayEvent?> = _currentEvent.asStateFlow()

    private val _currentDiff = MutableStateFlow<StateDiff?>(null)
    val currentDiff: StateFlow<StateDiff?> = _currentDiff.asStateFlow()

    private val _timelinePosition = MutableStateFlow(0f)    // 0.0–1.0 progress
    val timelinePosition: StateFlow<Float> = _timelinePosition.asStateFlow()

    private val _timelineMarkers = MutableStateFlow<List<TimelineMarker>>(emptyList())
    val timelineMarkers: StateFlow<List<TimelineMarker>> = _timelineMarkers.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────
    private var sessionId: String = ""
    private var config = PlaybackConfig()
    private var currentSequence: Long = 0
    private var minSequence: Long = 0
    private var maxSequence: Long = 0

    // Preloaded event cache (ring buffer)
    private val eventCache = LinkedHashMap<Long, ReplayEvent>(100, 0.75f, true)
    private var cacheWindowStart: Long = 0
    private var cacheWindowEnd: Long = 0

    private var playbackJob: Job? = null

    // ═════════════════════════════════════════════════════════════════════
    // SESSION LOADING
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Load a replay session for playback.
     */
    suspend fun loadSession(sessionId: String): Boolean {
        this.sessionId = sessionId
        minSequence = replayEventDao.getMinSequence(sessionId) ?: return false
        maxSequence = replayEventDao.getMaxSequence(sessionId) ?: return false
        currentSequence = minSequence

        // Preload first batch
        preloadBatch(minSequence)

        // Build timeline markers
        buildTimelineMarkers()

        // Load first event
        loadCurrentEvent()

        _playbackState.value = PlaybackState.PAUSED
        Timber.i("Replay session loaded: $sessionId (events: ${maxSequence - minSequence + 1})")
        return true
    }

    // ═════════════════════════════════════════════════════════════════════
    // PLAYBACK CONTROLS
    // ═════════════════════════════════════════════════════════════════════

    fun play(speed: Float = config.playbackSpeed) {
        config = config.copy(playbackSpeed = speed.coerceIn(0.1f, 4.0f))
        _playbackState.value = PlaybackState.PLAYING

        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (_playbackState.value == PlaybackState.PLAYING) {
                val delayMs = (100 / config.playbackSpeed).toLong().coerceAtLeast(16)
                delay(delayMs)

                if (currentSequence < maxSequence) {
                    stepForwardInternal()
                } else if (config.autoStopAtEnd) {
                    pause()
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _playbackState.value = PlaybackState.PAUSED
    }

    suspend fun stepForward() {
        _playbackState.value = PlaybackState.STEPPING_FORWARD
        stepForwardInternal()
        _playbackState.value = PlaybackState.PAUSED
    }

    suspend fun stepBackward() {
        _playbackState.value = PlaybackState.STEPPING_BACKWARD
        if (currentSequence > minSequence) {
            val previousEvent = _currentEvent.value
            currentSequence--
            loadCurrentEvent()
            computeDiffWithPrevious(previousEvent)
            updateTimelinePosition()
        }
        _playbackState.value = PlaybackState.PAUSED
    }

    suspend fun jumpToSequence(sequence: Long) {
        _playbackState.value = PlaybackState.SEEKING
        currentSequence = sequence.coerceIn(minSequence, maxSequence)
        preloadBatch(currentSequence)
        loadCurrentEvent()
        updateTimelinePosition()
        _playbackState.value = PlaybackState.PAUSED
    }

    suspend fun jumpToTimestamp(timestamp: Long) {
        _playbackState.value = PlaybackState.SEEKING
        val entity = replayEventDao.getNearestBefore(sessionId, timestamp)
        if (entity != null) {
            currentSequence = entity.sequenceNumber
            preloadBatch(currentSequence)
            loadCurrentEvent()
            updateTimelinePosition()
        }
        _playbackState.value = PlaybackState.PAUSED
    }

    /**
     * Seek to a normalized position [0..1] on the timeline.
     */
    suspend fun seekToPosition(position: Float) {
        val pos = position.coerceIn(0f, 1f)
        val targetSeq = (minSequence + (maxSequence - minSequence) * pos).toLong()
        jumpToSequence(targetSeq)
    }

    fun setPlaybackSpeed(speed: Float) {
        config = config.copy(playbackSpeed = speed.coerceIn(0.1f, 4.0f))
    }

    // ═════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun stepForwardInternal() {
        val previousEvent = _currentEvent.value
        currentSequence++
        if (currentSequence > cacheWindowEnd - 10) {
            preloadBatch(currentSequence)
        }
        loadCurrentEvent()
        computeDiffWithPrevious(previousEvent)
        updateTimelinePosition()
    }

    private suspend fun loadCurrentEvent() {
        // Try cache first
        var event = eventCache[currentSequence]
        if (event == null) {
            // Cache miss — fetch from DB
            val entity = replayEventDao.getBySequence(sessionId, currentSequence) ?: return
            event = deserializeEvent(entity)
            eventCache[currentSequence] = event
        }
        _currentEvent.value = event
    }

    private suspend fun preloadBatch(startSeq: Long) {
        val entities = replayEventDao.preloadBatch(sessionId, startSeq, config.preloadBatchSize)
        for (entity in entities) {
            if (!eventCache.containsKey(entity.sequenceNumber)) {
                eventCache[entity.sequenceNumber] = deserializeEvent(entity)
            }
        }
        if (entities.isNotEmpty()) {
            cacheWindowStart = entities.first().sequenceNumber
            cacheWindowEnd = entities.last().sequenceNumber
        }

        // Evict old cache entries beyond window
        val keepStart = (startSeq - config.preloadBatchSize).coerceAtLeast(minSequence)
        eventCache.keys.removeAll { it < keepStart || it > cacheWindowEnd + config.preloadBatchSize }
    }

    private fun deserializeEvent(entity: ReplayEventEntity): ReplayEvent {
        return ReplayEvent(
            id = entity.id,
            sequenceNumber = entity.sequenceNumber,
            timestamp = entity.timestamp,
            sessionId = entity.sessionId,
            eventType = ReplayEventType.valueOf(entity.eventType),
            cognitiveSnapshot = json.decodeFromString(entity.cognitiveSnapshotJson),
            decisionSnapshot = entity.decisionSnapshotJson?.let { json.decodeFromString(it) },
            ruleUpdates = json.decodeFromString(entity.ruleUpdatesJson),
            memoryChanges = json.decodeFromString(entity.memoryChangesJson),
            metadata = json.decodeFromString(entity.metadataJson)
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // PART 6 — DIFFERENCE COMPUTATION
    // ═════════════════════════════════════════════════════════════════════

    private fun computeDiffWithPrevious(previousEvent: ReplayEvent?) {
        val current = _currentEvent.value ?: return
        if (previousEvent == null) {
            _currentDiff.value = null
            return
        }

        val prevNodes = previousEvent.cognitiveSnapshot.nodeStates.associateBy { it.nodeId }
        val currNodes = current.cognitiveSnapshot.nodeStates.associateBy { it.nodeId }

        val prevEdges = previousEvent.cognitiveSnapshot.edgeStates.associateBy { it.edgeId }
        val currEdges = current.cognitiveSnapshot.edgeStates.associateBy { it.edgeId }

        _currentDiff.value = StateDiff(
            fromSequence = previousEvent.sequenceNumber,
            toSequence = current.sequenceNumber,

            addedNodes = current.cognitiveSnapshot.nodeStates.filter { it.nodeId !in prevNodes },
            removedNodeIds = previousEvent.cognitiveSnapshot.nodeStates
                .filter { it.nodeId !in currNodes }
                .map { it.nodeId },
            changedNodes = currNodes.mapNotNull { (id, curr) ->
                val prev = prevNodes[id] ?: return@mapNotNull null
                if (prev.activationLevel != curr.activationLevel || prev.position != curr.position) {
                    NodeDiff(
                        nodeId = id,
                        previousActivation = prev.activationLevel,
                        newActivation = curr.activationLevel,
                        previousPosition = prev.position,
                        newPosition = curr.position
                    )
                } else null
            },

            addedEdges = current.cognitiveSnapshot.edgeStates.filter { it.edgeId !in prevEdges },
            removedEdgeIds = previousEvent.cognitiveSnapshot.edgeStates
                .filter { it.edgeId !in currEdges }
                .map { it.edgeId },
            changedEdges = currEdges.mapNotNull { (id, curr) ->
                val prev = prevEdges[id] ?: return@mapNotNull null
                if (prev.weight != curr.weight || prev.flowRate != curr.flowRate) {
                    EdgeDiff(
                        edgeId = id,
                        previousWeight = prev.weight,
                        newWeight = curr.weight,
                        previousFlowRate = prev.flowRate,
                        newFlowRate = curr.flowRate
                    )
                } else null
            },

            ruleChanges = current.ruleUpdates,
            memoryChanges = current.memoryChanges
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // TIMELINE
    // ═════════════════════════════════════════════════════════════════════

    private fun updateTimelinePosition() {
        val range = (maxSequence - minSequence).toFloat().coerceAtLeast(1f)
        _timelinePosition.value = (currentSequence - minSequence).toFloat() / range
    }

    private suspend fun buildTimelineMarkers() {
        val markers = mutableListOf<TimelineMarker>()
        val range = (maxSequence - minSequence).toFloat().coerceAtLeast(1f)

        // Mark cognitive spikes
        val spikes = replayEventDao.getCognitiveSpikes(sessionId, 0.7f)
        for (spike in spikes) {
            markers.add(
                TimelineMarker(
                    position = (spike.sequenceNumber - minSequence) / range,
                    type = TimelineMarkerType.COGNITIVE_SPIKE,
                    label = "Spike (${(spike.cognitiveLoad * 100).toInt()}%)",
                    sequenceNumber = spike.sequenceNumber,
                    timestamp = spike.timestamp
                )
            )
        }

        // Mark evolution events
        val evolutions = replayEventDao.getByType(sessionId, ReplayEventType.EVOLUTION.name)
        for (evo in evolutions) {
            markers.add(
                TimelineMarker(
                    position = (evo.sequenceNumber - minSequence) / range,
                    type = TimelineMarkerType.EVOLUTION,
                    label = "Rule change",
                    sequenceNumber = evo.sequenceNumber,
                    timestamp = evo.timestamp
                )
            )
        }

        // Mark anomalies
        val anomalies = replayEventDao.getByType(sessionId, ReplayEventType.ANOMALY.name)
        for (anomaly in anomalies) {
            markers.add(
                TimelineMarker(
                    position = (anomaly.sequenceNumber - minSequence) / range,
                    type = TimelineMarkerType.ANOMALY,
                    label = "Anomaly",
                    sequenceNumber = anomaly.sequenceNumber,
                    timestamp = anomaly.timestamp
                )
            )
        }

        _timelineMarkers.value = markers.sortedBy { it.position }
    }

    fun getCurrentSequence(): Long = currentSequence
    fun getTotalFrames(): Long = maxSequence - minSequence + 1
    fun getSessionId(): String = sessionId

    fun destroy() {
        playbackJob?.cancel()
        scope.cancel()
    }
}

/**
 * Marker on the timeline UI for highlighting significant events.
 */
data class TimelineMarker(
    val position: Float,           // 0.0–1.0 normalized position
    val type: TimelineMarkerType,
    val label: String,
    val sequenceNumber: Long,
    val timestamp: Long
)

enum class TimelineMarkerType {
    COGNITIVE_SPIKE,
    EVOLUTION,
    ANOMALY,
    BOOKMARK,
    DECISION
}
