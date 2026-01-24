package com.aihos.di

import android.content.Context
import com.aihos.ai.autonomy.AutonomyController
import com.aihos.ai.autonomy.impl.DefaultAutonomyController
import com.aihos.ai.evolution.EvolutionEngine
import com.aihos.ai.evolution.impl.DefaultEvolutionEngine
import com.aihos.ai.memory.MemoryLayer
import com.aihos.ai.memory.impl.DefaultMemoryLayer
import com.aihos.ai.reasoning.ReasoningLayer
import com.aihos.ai.reasoning.impl.DefaultReasoningLayer
import com.aihos.ai.reflection.ReflectionLayer
import com.aihos.ai.reflection.impl.DefaultReflectionLayer
import com.aihos.data.db.SAIHOSDatabase
import com.aihos.data.repository.MemoryRepository
import com.aihos.domain.use_case.AIBrainUseCase
import com.aihos.domain.use_case.impl.AIBrainUseCaseImpl
import com.aihos.system.energy.EnergyManager
import com.aihos.system.energy.ThermalManager
import com.aihos.system.energy.impl.EnergyManagerImpl
import com.aihos.system.energy.impl.ThermalManagerImpl
import com.aihos.system.signals.SignalCollector
import com.aihos.system.signals.impl.BatterySignalImpl
import com.aihos.system.signals.impl.SignalCollectorImpl
import com.aihos.system.signals.impl.TemperatureSignalImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Dependency Injection configuration using Hilt
 * 
 * This module provides all layer implementations:
 * - AI Core (Memory, Reasoning, Reflection, Evolution, Autonomy)
 * - Domain Layer (Use cases)
 * - System Layer (Signals, Energy, Thermal managers)
 * - Data Layer (Database, Repositories)
 * 
 * All objects are singletons with application scope.
 */
@Module
@InstallIn(SingletonComponent::class)
object Module {
    
    @Singleton
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): SAIHOSDatabase {
        return SAIHOSDatabase.getInstance(context)
    }
    
    // ===== Coroutine Scope for Use Cases =====
    
    @Singleton
    @Provides
    fun provideApplicationScope(): CoroutineScope = 
        CoroutineScope(kotlinx.coroutines.Dispatchers.Default + SupervisorJob())
    
    // ===== AI Core Layer Providers =====
    
    @Singleton
    @Provides
    fun provideMemoryLayer(): MemoryLayer = DefaultMemoryLayer()
    
    @Singleton
    @Provides
    fun provideReasoningLayer(): ReasoningLayer = DefaultReasoningLayer()
    
    @Singleton
    @Provides
    fun provideReflectionLayer(): ReflectionLayer = DefaultReflectionLayer()
    
    @Singleton
    @Provides
    fun provideEvolutionEngine(): EvolutionEngine = DefaultEvolutionEngine()
    
    @Singleton
    @Provides
    fun provideAutonomyController(): AutonomyController = DefaultAutonomyController()
    
    // ===== Domain Layer Use Cases =====
    
    @Singleton
    @Provides
    fun provideAIBrainUseCase(
        reasoningEngine: ReasoningLayer,
        reflectionEngine: ReflectionLayer,
        evolutionEngine: EvolutionEngine,
        memoryLayer: MemoryLayer,
        autonomyController: AutonomyController,
        scope: CoroutineScope
    ): AIBrainUseCase = AIBrainUseCaseImpl(
        reasoningEngine = reasoningEngine,
        reflectionEngine = reflectionEngine,
        evolutionEngine = evolutionEngine,
        memorySystem = memoryLayer,
        autonomyController = autonomyController,
        scope = scope
    )
    
    // ===== System Layer: Signals =====
    
    @Singleton
    @Provides
    fun provideBatterySignal(@ApplicationContext context: Context): BatterySignalImpl =
        BatterySignalImpl(context)
    
    @Singleton
    @Provides
    fun provideTemperatureSignal(@ApplicationContext context: Context): TemperatureSignalImpl =
        TemperatureSignalImpl(context)
    
    @Singleton
    @Provides
    fun provideSignalCollector(
        batterySignal: BatterySignalImpl,
        temperatureSignal: TemperatureSignalImpl
    ): SignalCollector = SignalCollectorImpl(batterySignal, temperatureSignal)
    
    // ===== System Layer: Energy & Thermal =====
    
    @Singleton
    @Provides
    fun provideEnergyManager(@ApplicationContext context: Context): EnergyManager =
        EnergyManagerImpl(context)
    
    @Singleton
    @Provides
    fun provideThermalManager(): ThermalManager = ThermalManagerImpl()
    
    // ===== Repository Providers =====
    
    @Singleton
    @Provides
    fun provideMemoryRepository(database: SAIHOSDatabase): MemoryRepository {
        return MemoryRepository(database.memoryDao())
    }
}
