package com.aihos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aihos.data.db.entity.*
import com.aihos.data.db.entities.*
import com.aihos.data.db.daos.*

/**
 * Room database for SA-AIHOS
 * Handles all local data persistence for AI memory, reasoning, evolution, and autonomy
 * Offline-first, Privacy-first database
 * 
 * Phase 2.2 Enhancement: Extended schema for metrics history, events, and analytics
 */
@Database(
    entities = [
        // Core entities from entity package
        MemoryEntity::class,
        ReasoningRuleEntity::class,
        InsightEntity::class,
        EvolutionLogEntity::class,
        AutonomousDecisionEntity::class,
        PerformanceMetricEntity::class,
        FeedbackEntity::class,
        SystemConfigEntity::class,
        // Smart Automation entities
        AutomationTaskEntity::class,
        AgentTaskEntity::class,
        // Extended entities from entities package
        MetricsHistory::class,
        MemoryConsolidationEvent::class,
        SystemEventLog::class,
        HealthSnapshot::class,
        UserInteractionEvent::class,
        PerformanceMetric::class
    ],
    version = 2,
    exportSchema = false  // Disable schema export for development
)
abstract class SAIHOSDatabase : RoomDatabase() {
    
    // DAO accessors - 8 core DAOs from dao package
    abstract fun memoryDao(): MemoryDao
    abstract fun reasoningRuleDao(): ReasoningRuleDao
    abstract fun insightDao(): InsightDao
    abstract fun evolutionLogDao(): EvolutionLogDao
    abstract fun autonomousDecisionDao(): AutonomousDecisionDao
    abstract fun performanceMetricDao(): PerformanceMetricDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun systemConfigDao(): SystemConfigDao
    
    // Smart Automation DAOs
    abstract fun automationTaskDao(): AutomationTaskDao
    abstract fun agentTaskDao(): AgentTaskDao

    // Extended DAO accessors from daos package
    abstract fun metricsHistoryDao(): MetricsHistoryDao
    abstract fun memoryConsolidationDao(): MemoryConsolidationDao
    abstract fun systemEventLogDao(): SystemEventLogDao
    abstract fun healthSnapshotDao(): HealthSnapshotDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun performanceMetricsHistoryDao(): PerformanceMetricsHistoryDao

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
