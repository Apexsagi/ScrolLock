package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SpikeTestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpikeTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpikeTestEntity): Long

    @Query("SELECT * FROM spike_test_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SpikeTestEntity>>

    @Query("DELETE FROM spike_test_records")
    suspend fun clearAll()
}
