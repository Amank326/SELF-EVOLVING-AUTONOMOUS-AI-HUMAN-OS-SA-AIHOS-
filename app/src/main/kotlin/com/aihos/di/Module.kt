package com.aihos.di

import android.content.Context
import androidx.room.Room
import com.aihos.ai.autonomy.DefaultAutonomyController
import com.aihos.ai.evolution.DefaultEvolutionEngine
import com.aihos.ai.memory.MemoryRepository
import com.aihos.ai.reasoning.HeuristicReasoningEngine
import com.aihos.ai.reflection.DefaultReflectionEngine
import com.aihos.data.db.SAIHOSDatabase
import com.aihos.data.repository.RoomMemoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection configuration using Hilt
 * Sets up all core components as singletons
 */

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSAIHOSDatabase(@ApplicationContext context: Context): SAIHOSDatabase {
        return Room.databaseBuilder(
            context,
            SAIHOSDatabase::class.java,
            "saihos_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideMemoryRepository(database: SAIHOSDatabase): MemoryRepository {
        return RoomMemoryRepository(database)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AIEngineModule {
    
    @Provides
    @Singleton
    fun provideReasoningEngine(): com.aihos.ai.reasoning.ReasoningEngine {
        return HeuristicReasoningEngine()
    }
    
    @Provides
    @Singleton
    fun provideReflectionEngine(): com.aihos.ai.reflection.ReflectionEngine {
        return DefaultReflectionEngine()
    }
    
    @Provides
    @Singleton
    fun provideEvolutionEngine(
        memoryRepository: MemoryRepository
    ): com.aihos.ai.evolution.EvolutionEngine {
        @Suppress("UNCHECKED_CAST")
        return DefaultEvolutionEngine(memoryRepository as com.aihos.ai.evolution.MemoryRepository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AutonomyModule {
    
    @Provides
    @Singleton
    fun provideContextProvider(@ApplicationContext context: Context): com.aihos.ai.autonomy.ContextProvider {
        return AndroidContextProvider(context)
    }
    
    @Provides
    @Singleton
    fun provideActionExecutor(): com.aihos.ai.autonomy.ActionExecutor {
        return DefaultActionExecutor()
    }
    
    @Provides
    @Singleton
    fun provideAutonomyController(
        memoryRepository: MemoryRepository,
        reasoningEngine: com.aihos.ai.reasoning.ReasoningEngine,
        reflectionEngine: com.aihos.ai.reflection.ReflectionEngine,
        evolutionEngine: com.aihos.ai.evolution.EvolutionEngine,
        contextProvider: com.aihos.ai.autonomy.ContextProvider,
        actionExecutor: com.aihos.ai.autonomy.ActionExecutor
    ): com.aihos.ai.autonomy.AutonomyController {
        return DefaultAutonomyController(
            memoryRepository = memoryRepository,
            reasoningEngine = reasoningEngine,
            reflectionEngine = reflectionEngine,
            evolutionEngine = evolutionEngine,
            contextProvider = contextProvider,
            actionExecutor = actionExecutor
        )
    }
}
