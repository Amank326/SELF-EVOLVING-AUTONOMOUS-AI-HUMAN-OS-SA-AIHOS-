package com.aihos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * SA-AIHOS ViewModel
 *
 * Main ViewModel for the SA-AIHOS Advanced AI system.
 * Manages UI state for the Phase 3 screens.
 */
@HiltViewModel
class SAIHOSViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // UI State for system status
    private val _uiState = MutableStateFlow("IDLE")
    val uiState: StateFlow<String> = _uiState

    // System health metrics
    private val _systemHealth = MutableStateFlow(0.85f)
    val systemHealth: StateFlow<Float> = _systemHealth

    // Cycle count
    private val _cycleCount = MutableStateFlow(0)
    val cycleCount: StateFlow<Int> = _cycleCount

    init {
        Timber.d("SAIHOSViewModel: Initialized")
    }

    /**
     * Start the AI system
     */
    fun startAI() {
        Timber.d("ViewModel: Starting AI system")
        _uiState.value = "ACTIVE"
    }

    /**
     * Pause the AI system
     */
    fun pauseAI() {
        Timber.d("ViewModel: Pausing AI system")
        _uiState.value = "PAUSED"
    }

    /**
     * Resume the AI system
     */
    fun resumeAI() {
        Timber.d("ViewModel: Resuming AI system")
        _uiState.value = "ACTIVE"
    }

    /**
     * Get human-readable state description
     */
    fun getStateDescription(): String = when (_uiState.value) {
        "IDLE" -> "Idle"
        "ACTIVE" -> "Active"
        "PAUSED" -> "Paused"
        "ERROR" -> "Error"
        else -> "Unknown"
    }

    /**
     * Called when ViewModel is destroyed
     */
    override fun onCleared() {
        Timber.d("ViewModel: Cleared")
        _uiState.value = "IDLE"
        super.onCleared()
    }
}
