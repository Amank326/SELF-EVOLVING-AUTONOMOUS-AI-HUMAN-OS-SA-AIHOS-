package com.aihos.replay.di

import android.content.Context
import androidx.room.Room
import com.aihos.replay.data.*
import com.aihos.replay.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// DI MODULE — Hilt configuration for the Replay System
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object ReplayModule {

    @Provides
    @Singleton
    fun provideReplayDatabase(@ApplicationContext context: Context): ReplayDatabase {
        return Room.databaseBuilder(
            context,
            ReplayDatabase::class.java,
            "saihos_replay_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideReplayEventDao(database: ReplayDatabase): ReplayEventDao {
        return database.replayEventDao()
    }

    @Provides
    @Singleton
    fun provideReplaySessionDao(database: ReplayDatabase): ReplaySessionDao {
        return database.replaySessionDao()
    }

    @Provides
    @Singleton
    fun provideReplayBookmarkDao(database: ReplayDatabase): ReplayBookmarkDao {
        return database.replayBookmarkDao()
    }

    @Provides
    @Singleton
    fun provideEventRecorder(
        replayEventDao: ReplayEventDao,
        replaySessionDao: ReplaySessionDao
    ): EventRecorder {
        return EventRecorder(
            replayEventDao = replayEventDao,
            replaySessionDao = replaySessionDao,
            maxEventsInMemory = 500,
            maxStorageBytes = 50 * 1024 * 1024,  // 50 MB
            batchWriteSize = 20,
            batchWriteIntervalMs = 2000
        )
    }

    @Provides
    @Singleton
    fun provideReplayController(
        replayEventDao: ReplayEventDao,
        replaySessionDao: ReplaySessionDao
    ): ReplayController {
        return ReplayController(
            replayEventDao = replayEventDao,
            replaySessionDao = replaySessionDao
        )
    }
}
