package com.aihos.system.signals

import kotlinx.coroutines.flow.StateFlow

/**
 * Base interface for device signals.
 * Represents any observable signal from the device or system.
 */
interface Signal<T> {
    /**
     * Current value of the signal.
     */
    val value: StateFlow<T>
    
    /**
     * Signal name (e.g., "battery", "temperature", "app_usage").
     */
    val name: String
    
    /**
     * Confidence in the signal value (0.0 to 1.0).
     */
    val confidence: StateFlow<Float>
}

/**
 * Battery level signal (0-100%).
 */
interface BatterySignal : Signal<Float>

/**
 * Device temperature signal (celsius).
 */
interface TemperatureSignal : Signal<Float>

/**
 * Screen state signal (true if on, false if off).
 */
interface ScreenStateSignal : Signal<Boolean>

/**
 * Current app in foreground.
 */
interface ForegroundAppSignal : Signal<String>

/**
 * Device usage intensity (0-1, calculated from various factors).
 */
interface UsageIntensitySignal : Signal<Float>

/**
 * Time of day (0-1, where 0 is midnight, 1 is next midnight).
 */
interface TimeOfDaySignal : Signal<Float>

/**
 * Network connectivity state.
 */
interface NetworkSignal : Signal<Boolean>

/**
 * Idle vs active user interaction state.
 */
interface InteractionStateSignal : Signal<String>

/**
 * Aggregated device context from multiple signals.
 * Provides a unified view of the device state.
 */
interface DeviceContext {
    /**
     * Current battery level (0-100).
     */
    val batteryLevel: Float
    
    /**
     * Current device temperature (celsius).
     */
    val temperature: Float
    
    /**
     * Is screen currently on?
     */
    val isScreenOn: Boolean
    
    /**
     * Current foreground app package.
     */
    val foregroundApp: String
    
    /**
     * Overall device usage intensity (0-1).
     */
    val usageIntensity: Float
    
    /**
     * Time of day (0-1).
     */
    val timeOfDay: Float
    
    /**
     * Is device connected to network?
     */
    val isNetworkConnected: Boolean
    
    /**
     * Snapshot timestamp.
     */
    val timestamp: Long
}

/**
 * Signal collector - aggregates all device signals.
 */
interface SignalCollector {
    /**
     * Get current device context.
     */
    val deviceContext: StateFlow<DeviceContext>
    
    /**
     * Start collecting signals.
     */
    suspend fun start()
    
    /**
     * Stop collecting signals.
     */
    suspend fun stop()
}
