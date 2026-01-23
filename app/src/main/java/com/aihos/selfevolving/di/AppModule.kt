package com.aihos.selfevolving.di

import android.content.Context
import androidx.room.Room
import com.aihos.selfevolving.data.local.AiHosDatabase
import com.aihos.selfevolving.data.repository.*
import com.aihos.selfevolving.domain.repository.*
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection modules using Hilt
 */

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiHosDatabase {
        return Room.databaseBuilder(
            context,
            AiHosDatabase::class.java,
            "aihos_database"
        ).fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideMemoryDao(database: AiHosDatabase) = database.memoryDao()
    
    @Provides
    fun provideReasoningDao(database: AiHosDatabase) = database.reasoningDao()
    
    @Provides
    fun provideReflectionDao(database: AiHosDatabase) = database.reflectionDao()
    
    @Provides
    fun provideEvolutionDao(database: AiHosDatabase) = database.evolutionDao()
    
    @Provides
    fun provideAutonomyDao(database: AiHosDatabase) = database.autonomyDao()
    
    @Provides
    fun provideAiStateDao(database: AiHosDatabase) = database.aiStateDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
    
    @Provides
    @Singleton
    fun provideMemoryRepository(
        memoryDao: com.aihos.selfevolving.data.local.MemoryDao,
        gson: Gson
    ): MemoryRepository {
        return MemoryRepositoryImpl(memoryDao, gson)
    }
    
    @Provides
    @Singleton
    fun provideReasoningRepository(
        reasoningDao: com.aihos.selfevolving.data.local.ReasoningDao,
        gson: Gson
    ): ReasoningRepository {
        return ReasoningRepositoryImpl(reasoningDao, gson)
    }
    
    @Provides
    @Singleton
    fun provideReflectionRepository(
        reflectionDao: com.aihos.selfevolving.data.local.ReflectionDao,
        gson: Gson
    ): ReflectionRepository {
        return ReflectionRepositoryImpl(reflectionDao, gson)
    }
    
    @Provides
    @Singleton
    fun provideEvolutionRepository(
        evolutionDao: com.aihos.selfevolving.data.local.EvolutionDao,
        gson: Gson
    ): EvolutionRepository {
        return EvolutionRepositoryImpl(evolutionDao, gson)
    }
    
    @Provides
    @Singleton
    fun provideAutonomyRepository(
        autonomyDao: com.aihos.selfevolving.data.local.AutonomyDao,
        gson: Gson
    ): AutonomyRepository {
        return AutonomyRepositoryImpl(autonomyDao, gson)
    }
    
    @Provides
    @Singleton
    fun provideAiStateRepository(
        aiStateDao: com.aihos.selfevolving.data.local.AiStateDao,
        gson: Gson
    ): AiStateRepository {
        return AiStateRepositoryImpl(aiStateDao, gson)
    }
}
