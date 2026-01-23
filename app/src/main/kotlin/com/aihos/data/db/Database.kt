package com.aihos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aihos.data.db.dao.*
import com.aihos.data.db.entity.*

/**
 * Room database for SA-AIHOS
 * Handles all local data persistence for AI memory, reasoning, evolution, and autonomy
 * Offline-first, Privacy-first database
 */
@Database(
    entities = [
        MemoryEntity::class,
        ReasoningRuleEntity::class,
        InsightEntity::class,
        EvolutionLogEntity::class,
        AutonomousDecisionEntity::class,
        PerformanceMetricEntity::class,
        FeedbackEntity::class,
        SystemConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SAIHOSDatabase : RoomDatabase() {
    
    // DAO accessors
    abstract fun memoryDao(): MemoryDao
    abstract fun reasoningRuleDao(): ReasoningRuleDao
    abstract fun insightDao(): InsightDao
    abstract fun evolutionLogDao(): EvolutionLogDao
    abstract fun autonomousDecisionDao(): AutonomousDecisionDao
    abstract fun performanceMetricDao(): PerformanceMetricDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun systemConfigDao(): SystemConfigDao
    
    companion object {
        @Volatile
        private var instance: SAIHOSDatabase? = null
        
        fun getInstance(context: Context): SAIHOSDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }
        
        private fun buildDatabase(context: Context): SAIHOSDatabase {
            return Room.databaseBuilder(
                context,
                SAIHOSDatabase::class.java,
                "sa_aihos_db"
            )
                .fallbackToDestructiveMigration()  // For development - use proper migrations in production
                .build()
        }
        
        fun closeInstance() {
            instance?.close()
            instance = null
        }
    }
}
