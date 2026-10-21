package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AppLaunchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLaunchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunch(launch: AppLaunchEntity): Long

    @Query("SELECT COUNT(*) FROM app_launches WHERE packageName = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    fun observeLaunchesBetween(packageName: String, startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_launches WHERE packageName = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getLaunchesBetween(packageName: String, startTime: Long, endTime: Long): Int

    @Query("DELETE FROM app_launches")
    suspend fun clearAll()

    @Query("DELETE FROM app_launches WHERE packageName = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun clearLaunchesForPackageBetween(packageName: String, startTime: Long, endTime: Long)
}
