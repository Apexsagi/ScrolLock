package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_budgets")
data class AppBudgetEntity(
    @PrimaryKey
    val packageName: String,
    val displayName: String,
    val dailyLimit: Int = 50,
    val screenTimeLimitMinutes: Int = 45,
    val launchLimit: Int = 0,
    val scrollCountingSupported: Boolean = true,
    val weekendScreenTimeLimitMinutes: Int = 0,
    val scheduleStartHour: Int = 0,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 0,
    val scheduleEndMinute: Int = 0,
    val isScheduleEnabled: Boolean = false,
    val warnAtEightyPercent: Boolean = true,
    val isPaused: Boolean = false,
    val pauseUntilTimestamp: Long = 0L,
    val category: String = "Social",
    val isEnabled: Boolean = true,
    val lockMode: String = "STANDARD",
    val resetHour: Int = 0,
    val resetMinute: Int = 0,
    val lockResetTimestamp: Long = 0L
)
