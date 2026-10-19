package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_usage_summaries",
    indices = [Index(value = ["dateKey", "targetPackage"], unique = true)]
)
data class DailyUsageSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // YYYY-MM-DD
    val targetPackage: String,
    val verifiedScrollCount: Int,
    val screenTimeMillis: Long = 0L,
    val launchesCount: Int = 0,
    val budgetReached: Boolean = false,
    val budgetLimitAtExhaustion: Int = 0,
    val monitoringCompleteness: String = "COMPLETE"
)
