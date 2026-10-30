package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

class UsageStatsRepository(private val context: Context) {

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return try {
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun getScreenTimeTodayMillis(packageName: String): Long {
        if (!hasUsagePermission()) return 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()

        return try {
            val aggregated = usageStatsManager.queryAndAggregateUsageStats(start, end)
            if (aggregated.containsKey(packageName)) {
                aggregated[packageName]?.totalTimeInForeground ?: 0L
            } else {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    start,
                    end
                ) ?: return 0L
                stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
            }
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        fun formatScreenTime(millis: Long): String {
            if (millis <= 0L) return "0m"
            val totalSeconds = millis / 1000
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
    }
}
