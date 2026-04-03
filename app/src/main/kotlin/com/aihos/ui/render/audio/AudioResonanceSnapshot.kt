package com.aihos.ui.render.audio
data class AudioResonanceSnapshot(
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val highEnergy: Float = 0f,
    val amplitude: Float = 0f,
    val onsetDetected: Boolean = false,
    val spectralFlux: Float = 0f,
    val isActive: Boolean = false
) {
    fun sanitized(): AudioResonanceSnapshot = AudioResonanceSnapshot(
        bassEnergy = bassEnergy.coerceIn(0f, 1f),
        midEnergy = midEnergy.coerceIn(0f, 1f),
        highEnergy = highEnergy.coerceIn(0f, 1f),
        amplitude = amplitude.coerceIn(0f, 1f),
        onsetDetected = onsetDetected,
        spectralFlux = spectralFlux.coerceIn(0f, 1f),
        isActive = isActive
    )
    companion object { val SILENT = AudioResonanceSnapshot() }
}
