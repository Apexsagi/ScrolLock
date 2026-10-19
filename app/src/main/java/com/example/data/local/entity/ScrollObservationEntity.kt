package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scroll_observations")
data class ScrollObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetPackage: String,
    val timestamp: Long,
    val eventClassification: String,
    val verificationStatus: String,
    val rawDeltaY: Int = 0,
    val sourceNodeClass: String = ""
)
