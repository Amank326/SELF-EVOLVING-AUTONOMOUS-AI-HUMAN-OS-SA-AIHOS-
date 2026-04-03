package com.aihos.ui.render.audio
import java.util.concurrent.atomic.AtomicReference
/**
 * AudioResonanceBridge - Thread-safe lock-free bridge between audio analysis
 * thread and GL render thread. Same pattern as AIStateBridge.
 */
class AudioResonanceBridge {
    private val stateRef = AtomicReference(AudioResonanceSnapshot.SILENT)
    fun pushState(snapshot: AudioResonanceSnapshot) {
        stateRef.set(snapshot.sanitized())
    }
    fun consumeState(): AudioResonanceSnapshot = stateRef.get()
    fun clear() { stateRef.set(AudioResonanceSnapshot.SILENT) }
}
