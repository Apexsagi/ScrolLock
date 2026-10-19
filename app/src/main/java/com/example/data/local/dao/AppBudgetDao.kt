package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBudgetDao {
    @Query("SELECT * FROM app_budgets")
    fun getAllBudgets(): Flow<List<AppBudgetEntity>>

    @Query("SELECT * FROM app_budgets")
    suspend fun getAllBudgetsOnce(): List<AppBudgetEntity>

    @Query("SELECT * FROM app_budgets WHERE packageName = :packageName LIMIT 1")
    suspend fun getBudgetByPackage(packageName: String): AppBudgetEntity?

    @Query("SELECT * FROM app_budgets WHERE packageName = :packageName LIMIT 1")
    fun observeBudgetByPackage(packageName: String): Flow<AppBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: AppBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<AppBudgetEntity>)

    @Update
    suspend fun updateBudget(budget: AppBudgetEntity)

    @Query("UPDATE app_budgets SET dailyLimit = :limit WHERE packageName = :packageName")
    suspend fun updateLimit(packageName: String, limit: Int)

    @Query("UPDATE app_budgets SET screenTimeLimitMinutes = :minutes WHERE packageName = :packageName")
    suspend fun updateScreenTimeLimit(packageName: String, minutes: Int)

    @Query("UPDATE app_budgets SET launchLimit = :limit WHERE packageName = :packageName")
    suspend fun updateLaunchLimit(packageName: String, limit: Int)

    @Query("UPDATE app_budgets SET lockMode = :lockMode WHERE packageName = :packageName")
    suspend fun updateLockMode(packageName: String, lockMode: String)

    @Query("UPDATE app_budgets SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateEnabled(packageName: String, isEnabled: Boolean)

    @Query("UPDATE app_budgets SET isPaused = :isPaused, pauseUntilTimestamp = :pauseUntil WHERE packageName = :packageName")
    suspend fun updatePauseState(packageName: String, isPaused: Boolean, pauseUntil: Long)

    @Query("UPDATE app_budgets SET lockResetTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateLockResetTimestamp(packageName: String, timestamp: Long)

    @Query("DELETE FROM app_budgets WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM app_budgets WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isPackageMonitored(packageName: String): Boolean
}
