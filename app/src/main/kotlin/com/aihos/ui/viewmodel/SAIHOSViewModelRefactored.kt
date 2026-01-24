package com.aihos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihos.domain.model.CognitiveState
import com.aihos.domain.model.ExecutionPhase
import com.aihos.domain.use_case.AIBrainUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * SA-AIHOS ViewModel - REFACTORED for Clean Architecture
 *
 * RESPONSIBILITY: UI state management only.
 * NO AI logic, NO system monitoring, NO cross-layer dependencies.
 *
 * This ViewModel:
 * - Exposes AI state from use cases as UI-friendly flows
 * - Handles UI lifecycle (not AI lifecycle)
 * - Routes user actions to use cases
 * - Manages UI-specific state (loading, errors, animations)
 *
 * All AI logic is in the domain layer (use cases).
 * All system logic is in the system layer.
 */
@HiltViewModel
class SAIHOSViewModel @Inject constructor(
    private val aiBrainUseCase: AIBrainUseCase
) : ViewModel() {

    // ==================== UI STATE ====================
    // These are view-only projections of domain state, UI can observe these

    /**
     * Observable cognitive state for UI.
     * UI reads this to show status, decisions, learning, etc.
     */
    val cognitiveState: StateFlow<CognitiveState> = aiBrainUseCase.cognitiveState

    // ==================== UI ACTIONS ====================
    // These methods handle user interactions and route them to use cases

    /**
     * User tapped "Start AI".
     */
    fun startAI() {
        Timber.d("UI: User started AI")
        viewModelScope.launch {
            aiBrainUseCase.start()
        }
    }

    /**
     * User tapped "Pause AI".
     */
    fun pauseAI() {
        Timber.d("UI: User paused AI")
        viewModelScope.launch {
            aiBrainUseCase.pause()
        }
    }

    /**
     * User tapped "Resume AI".
     */
    fun resumeAI() {
        Timber.d("UI: User resumed AI")
        viewModelScope.launch {
            aiBrainUseCase.resume()
        }
    }

    /**
     * User tapped "Stop AI".
     */
    fun stopAI() {
        Timber.d("UI: User stopped AI")
        viewModelScope.launch {
            aiBrainUseCase.stop()
        }
    }

    // ==================== LIFECYCLE ====================

    /**
     * Clean up when ViewModel is destroyed.
     * Note: AI logic cleanup is handled by use cases, not here.
     */
    override fun onCleared() {
        super.onCleared()
        Timber.d("ViewModel cleared")
    }
}
