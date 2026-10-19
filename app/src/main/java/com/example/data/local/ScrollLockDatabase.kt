package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppBudgetDao
import com.example.data.local.dao.AppLaunchDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.DailyUsageSummaryDao
import com.example.data.local.dao.ScrollObservationDao
import com.example.data.local.dao.SpikeTestDao
import com.example.data.local.entity.AppBudgetEntity
import com.example.data.local.entity.AppLaunchEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DailyUsageSummaryEntity
import com.example.data.local.entity.ScrollObservationEntity
import com.example.data.local.entity.SpikeTestEntity

@Database(
    entities = [
        AppBudgetEntity::class,
        AppLaunchEntity::class,
        ScrollObservationEntity::class,
        DailyUsageSummaryEntity::class,
        AuditLogEntity::class,
        SpikeTestEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ScrollLockDatabase : RoomDatabase() {
    abstract fun appBudgetDao(): AppBudgetDao
    abstract fun appLaunchDao(): AppLaunchDao
    abstract fun scrollObservationDao(): ScrollObservationDao
    abstract fun dailyUsageSummaryDao(): DailyUsageSummaryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun spikeTestDao(): SpikeTestDao

    companion object {
        @Volatile
        private var INSTANCE: ScrollLockDatabase? = null

        fun getInstance(context: Context): ScrollLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollLockDatabase::class.java,
                    "scrolllock.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
