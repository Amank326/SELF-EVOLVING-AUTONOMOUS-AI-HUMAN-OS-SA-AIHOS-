package com.aihos.di

import android.content.Context
import com.aihos.data.db.SAIHOSDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Dependency Injection configuration using Hilt
 * 
 * Minimal DI setup for Phase 3 UI only.
 * Provides database access for Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object Module {
    
    @Singleton
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        Timber.d("Providing Application Context")
        return context
    }
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): SAIHOSDatabase {
        Timber.d("Providing SAIHOSDatabase singleton")
        return SAIHOSDatabase.getInstance(context)
    }
}
