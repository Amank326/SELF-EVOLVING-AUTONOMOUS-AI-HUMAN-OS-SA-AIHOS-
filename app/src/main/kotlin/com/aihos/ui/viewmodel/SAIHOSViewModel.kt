package com.aihos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.autonomy.AutonomyLevel
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.memory.MemoryRepository
import com.aihos.ai.reflection.ReflectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main ViewModel for the Android UI
 * Bridges between Compose UI and AI systems
 */
@HiltViewModel
class SAIHOSViewModel @Inject constructor(
    private val autonomyController: AutonomyController,
    private val memoryRepository: MemoryRepository,
    private val reflectionEngine: ReflectionEngine,
    private val evolutionEngine: EvolutionEngine
) : ViewModel() {
    
    // UI State
    private val _autonomyLevel = MutableStateFlow(AutonomyLevel.CONSTRAINED)
    val autonomyLevel: StateFlow<AutonomyLevel> = _autonomyLevel.asStateFlow()
    
    private val _systemStatus = MutableStateFlow<SystemStatus>(SystemStatus.Idle)
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    
    private val _recentDecisions = MutableStateFlow<List<DecisionDisplay>>(emptyList())
    val recentDecisions: StateFlow<List<DecisionDisplay>> = _recentDecisions.asStateFlow()
    
    private val _memoryStats = MutableStateFlow<MemoryStatsDisplay>(MemoryStatsDisplay())
    val memoryStats: StateFlow<MemoryStatsDisplay> = _memoryStats.asStateFlow()
    
    private val _evolutionReport = MutableStateFlow<EvolutionReportDisplay>(EvolutionReportDisplay())
    val evolutionReport: StateFlow<EvolutionReportDisplay> = _evolutionReport.asStateFlow()
    
    init {
        Timber.d("SAIHOSViewModel initialized")
        loadInitialData()
    }
    
    fun startAutonomousLoop() {
        viewModelScope.launch {
            _systemStatus.value = SystemStatus.Running
            try {
                autonomyController.startDecisionLoop()
            } catch (e: Exception) {
                Timber.e(e, "Error in autonomous loop")
                _systemStatus.value = SystemStatus.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun stopAutonomousLoop() {
        viewModelScope.launch {
            autonomyController.stopDecisionLoop()
            _systemStatus.value = SystemStatus.Idle
        }
    }
    
    fun updateAutonomyLevel(level: AutonomyLevel) {
        _autonomyLevel.value = level
        val settings = autonomyController.getSettings().copy()
        // TODO: Actually update settings with new level
        autonomyController.updateSettings(settings)
        Timber.i("Autonomy level changed to: $level")
    }
    
    fun refreshMemoryStats() {
        viewModelScope.launch {
            try {
                val stats = memoryRepository.getMemoryStats()
                _memoryStats.value = MemoryStatsDisplay(
                    totalEpisodes = stats.totalEpisodes,
                    totalRules = stats.totalRules,
                    totalFacts = stats.totalFacts,
                    memoryUsagePercent = (stats.memoryUsageBytes / 500_000_000.0 * 100).toInt()
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh memory stats")
            }
        }
    }
    
    fun refreshEvolutionReport() {
        viewModelScope.launch {
            try {
                val report = evolutionEngine.getEvolutionReport()
                _evolutionReport.value = EvolutionReportDisplay(
                    totalRules = report.totalRulesCount,
                    activeRules = report.activeRulesCount,
                    deprecatedRules = report.deprecatedRulesCount,
                    newRulesThisSession = report.newRulesCreatedThisSession,
                    topPerformingCount = report.topPerformingRules.size
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh evolution report")
            }
        }
    }
    
    fun loadRecentDecisions() {
        viewModelScope.launch {
            try {
                val episodes = memoryRepository.getRecentEpisodes(10)
                _recentDecisions.value = episodes.map { episode ->
                    DecisionDisplay(
                        id = episode.id,
                        action = episode.action,
                        outcome = episode.outcome.name,
                        timestamp = formatTime(episode.timestamp),
                        reasoning = episode.reasoning.take(100) + "..."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load recent decisions")
            }
        }
    }
    
    private fun loadInitialData() {
        refreshMemoryStats()
        refreshEvolutionReport()
        loadRecentDecisions()
    }
    
    private fun formatTime(timeMs: Long): String {
        val date = java.util.Date(timeMs)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}

// UI State Models
sealed class SystemStatus {
    object Idle : SystemStatus()
    object Running : SystemStatus()
    object Paused : SystemStatus()
    data class Error(val message: String) : SystemStatus()
}

data class DecisionDisplay(
    val id: String,
    val action: String,
    val outcome: String,
    val timestamp: String,
    val reasoning: String
)

data class MemoryStatsDisplay(
    val totalEpisodes: Int = 0,
    val totalRules: Int = 0,
    val totalFacts: Int = 0,
    val memoryUsagePercent: Int = 0
)

data class EvolutionReportDisplay(
    val totalRules: Int = 0,
    val activeRules: Int = 0,
    val deprecatedRules: Int = 0,
    val newRulesThisSession: Int = 0,
    val topPerformingCount: Int = 0
)
