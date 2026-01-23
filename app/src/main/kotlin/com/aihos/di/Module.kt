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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection configuration using Hilt
 * Provides all AI layers, repositories, and database instances
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
    
    // ===== Repository Providers =====
    
    @Singleton
    @Provides
    fun provideMemoryRepository(database: SAIHOSDatabase): MemoryRepository {
        return MemoryRepository(database.memoryDao())
    }
}
