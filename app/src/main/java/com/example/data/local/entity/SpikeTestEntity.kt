package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spike_test_records")
data class SpikeTestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val scenario: String,
    val expectedSwipes: Int,
    val detectedEvents: Int,
    val falsePositives: Int,
    val deduplicatedEvents: Int,
    val precisionPercent: Int,
    val recallPercent: Int,
    val notes: String
)
