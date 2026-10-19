package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DailyUsageSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageSummaryDao {
    @Query("SELECT * FROM daily_usage_summaries WHERE dateKey = :dateKey")
    fun getSummariesForDate(dateKey: String): Flow<List<DailyUsageSummaryEntity>>

    @Query("SELECT * FROM daily_usage_summaries WHERE dateKey = :dateKey AND targetPackage = :packageName LIMIT 1")
    suspend fun getSummary(dateKey: String, packageName: String): DailyUsageSummaryEntity?

    @Query("SELECT * FROM daily_usage_summaries WHERE targetPackage = :packageName ORDER BY dateKey DESC LIMIT :days")
    fun getRecentHistory(packageName: String, days: Int): Flow<List<DailyUsageSummaryEntity>>

    @Query("SELECT * FROM daily_usage_summaries ORDER BY dateKey DESC")
    fun getAllSummaries(): Flow<List<DailyUsageSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailyUsageSummaryEntity)

    @Update
    suspend fun updateSummary(summary: DailyUsageSummaryEntity)

    @Query("SELECT COUNT(*) FROM daily_usage_summaries WHERE budgetReached = 1")
    suspend fun countBudgetsReached(): Int

    @Query("DELETE FROM daily_usage_summaries")
    suspend fun clearAll()
}
