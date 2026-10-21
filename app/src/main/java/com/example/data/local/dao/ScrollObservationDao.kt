package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ScrollObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollObservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ScrollObservationEntity): Long

    @Query("SELECT * FROM scroll_observations WHERE targetPackage = :packageName AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getObservationsBetween(packageName: String, startTime: Long, endTime: Long): Flow<List<ScrollObservationEntity>>

    @Query("SELECT COUNT(*) FROM scroll_observations WHERE targetPackage = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    fun observeCountBetween(packageName: String, startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM scroll_observations WHERE targetPackage = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getCountBetween(packageName: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM scroll_observations WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getTotalCountBetween(startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM scroll_observations ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestObservation(): Flow<ScrollObservationEntity?>

    @Query("SELECT * FROM scroll_observations ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentObservations(limit: Int): Flow<List<ScrollObservationEntity>>

    @Query("DELETE FROM scroll_observations")
    suspend fun clearAll()

    @Query("DELETE FROM scroll_observations WHERE targetPackage = :packageName AND timestamp >= :startTime AND timestamp < :endTime")
    suspend fun clearObservationsForPackageBetween(packageName: String, startTime: Long, endTime: Long)

    @Query("DELETE FROM scroll_observations WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
