package com.example.enforcement

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.repository.BudgetRepository
import com.example.data.repository.UsageStatsRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.LimitExhaustionReason
import com.example.domain.model.LockMode
import com.example.security.PinSecurityHelper
import com.example.ui.LimitReachedActivity
import kotlinx.coroutines.flow.first
import java.util.Calendar

class EnforcementManager(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScrollLock Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your daily scroll, screen-time, or launch limits are reached"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun enforceIfBudgetExhausted(
        targetPackage: String,
        onPerformHomeAction: () -> Unit
    ): Boolean {
        val budget = budgetRepository.getBudget(targetPackage) ?: return false
        if (!budget.isEnabled) return false

        // Check if currently paused
        val now = System.currentTimeMillis()
        if (budget.isPaused && now < budget.pauseUntilTimestamp) {
            return false
        }

        // Feature 2: Check if app is currently locked under an active Random Reset timer
        if (budget.lockResetTimestamp > 0) {
            if (now < budget.lockResetTimestamp) {
                // App is still locked until the random reset expires
                triggerEnforcement(
                    budget = budget,
                    reason = LimitExhaustionReason.SCROLL_LIMIT_REACHED,
                    currentMetric = "Random Lock Active",
                    limitMetric = "Unlocks soon",
                    lockResetTimestamp = budget.lockResetTimestamp,
                    onPerformHomeAction = onPerformHomeAction
                )
                return true
            } else {
                // Timer has expired! Clear the lock
                budgetRepository.clearLockResetTimestamp(budget.packageName)
            }
        }

        // 1. Check Scheduled Downtime
        if (budget.isScheduleEnabled && isCurrentlyInScheduleWindow(budget)) {
            val lockTimestamp = computeOrGetLockResetTimestamp(budget, now)
            triggerEnforcement(
                budget = budget,
                reason = LimitExhaustionReason.SCHEDULED_DOWNTIME_ACTIVE,
                currentMetric = "Active Downtime",
                limitMetric = formatScheduleWindow(budget),
                lockResetTimestamp = lockTimestamp,
                onPerformHomeAction = onPerformHomeAction
            )
            return true
        }

        // 2. Check Screen Time Limit
        val isWeekend = BudgetRepository.isWeekendToday()
        val screenLimitMinutes = if (isWeekend && budget.weekendScreenTimeLimitMinutes > 0) {
            budget.weekendScreenTimeLimitMinutes
        } else {
            budget.screenTimeLimitMinutes
        }

        val screenTimeMillis = usageStatsRepository.getScreenTimeTodayMillis(targetPackage)
        val screenTimeMinutes = (screenTimeMillis / (60 * 1000L)).toInt()

        if (screenLimitMinutes > 0 && screenTimeMinutes >= screenLimitMinutes) {
            val lockTimestamp = computeOrGetLockResetTimestamp(budget, now)
            triggerEnforcement(
                budget = budget,
                reason = LimitExhaustionReason.SCREEN_TIME_LIMIT_REACHED,
                currentMetric = "${screenTimeMinutes}m",
                limitMetric = "${screenLimitMinutes}m",
                lockResetTimestamp = lockTimestamp,
                onPerformHomeAction = onPerformHomeAction
            )
            return true
        } else if (budget.warnAtEightyPercent && screenLimitMinutes > 0 && screenTimeMinutes >= (screenLimitMinutes * 0.8).toInt()) {
            maybeNotifyEightyPercent(budget, "80% Screen Time Warning", "${budget.displayName} reached ${screenTimeMinutes}m of ${screenLimitMinutes}m daily limit.")
        }

        // 3. Check Scroll Quota Limit (if supported)
        if (budget.scrollCountingSupported && budget.dailyLimit > 0) {
            val countToday = budgetRepository.getScrollCountToday(targetPackage)
            if (countToday >= budget.dailyLimit) {
                val lockTimestamp = computeOrGetLockResetTimestamp(budget, now)
                triggerEnforcement(
                    budget = budget,
                    reason = LimitExhaustionReason.SCROLL_LIMIT_REACHED,
                    currentMetric = "$countToday scrolls",
                    limitMetric = "${budget.dailyLimit} scrolls",
                    lockResetTimestamp = lockTimestamp,
                    onPerformHomeAction = onPerformHomeAction
                )
                return true
            } else if (budget.warnAtEightyPercent && countToday >= (budget.dailyLimit * 0.8).toInt()) {
                maybeNotifyEightyPercent(budget, "80% Scroll Quota Warning", "$countToday scrolls observed. You have ${budget.dailyLimit - countToday} scrolls left today.")
            }
        }

        // 4. Check App Launches Limit
        if (budget.launchLimit > 0) {
            val launchesToday = budgetRepository.getLaunchesToday(targetPackage)
            if (launchesToday >= budget.launchLimit) {
                val lockTimestamp = computeOrGetLockResetTimestamp(budget, now)
                triggerEnforcement(
                    budget = budget,
                    reason = LimitExhaustionReason.LAUNCH_LIMIT_REACHED,
                    currentMetric = "$launchesToday opens",
                    limitMetric = "${budget.launchLimit} opens",
                    lockResetTimestamp = lockTimestamp,
                    onPerformHomeAction = onPerformHomeAction
                )
                return true
            } else if (budget.warnAtEightyPercent && launchesToday >= (budget.launchLimit * 0.8).toInt()) {
                maybeNotifyEightyPercent(budget, "80% Launch Limit Warning", "${budget.displayName} opened $launchesToday of ${budget.launchLimit} times today.")
            }
        }

        return false
    }

    private suspend fun computeOrGetLockResetTimestamp(budget: AppBudget, now: Long): Long {
        if (budget.lockResetTimestamp > now) {
            return budget.lockResetTimestamp
        }
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        val lockTimestamp = if (prefs.randomResetEnabled) {
            val randomMinutes = PinSecurityHelper.generateRandomDelayMinutes(
                prefs.randomResetMinHours,
                prefs.randomResetMaxHours
            )
            now + (randomMinutes * 60 * 1000L)
        } else {
            val (_, endOfDay) = BudgetRepository.getTodayTimeRange()
            endOfDay
        }
        budgetRepository.setLockResetTimestamp(budget.packageName, lockTimestamp)
        return lockTimestamp
    }

    private val notifiedEightyPercent = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun maybeNotifyEightyPercent(budget: AppBudget, title: String, text: String) {
        val todayKey = BudgetRepository.getTodayDateKey()
        val cacheKey = "${budget.packageName}_$todayKey"
        if (notifiedEightyPercent.putIfAbsent(cacheKey, todayKey) == null) {
            showEarlyNotification(budget, title, text)
        }
    }

    private fun showEarlyNotification(budget: AppBudget, title: String, text: String) {
        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAV_TARGET", "DASHBOARD")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (budget.packageName.hashCode() * 31),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(budget.packageName.hashCode() + 1000, notification)
        } catch (_: SecurityException) {
        }
    }

    private suspend fun triggerEnforcement(
        budget: AppBudget,
        reason: LimitExhaustionReason,
        currentMetric: String,
        limitMetric: String,
        lockResetTimestamp: Long = 0L,
        onPerformHomeAction: () -> Unit
    ) {
        when (budget.lockMode) {
            LockMode.GENTLE -> {
                showWarningNotification(budget, reason, currentMetric, limitMetric, lockResetTimestamp)
            }
            LockMode.STANDARD, LockMode.STRICT -> {
                onPerformHomeAction()
                showLimitReachedScreen(budget, reason, currentMetric, limitMetric, lockResetTimestamp)
                showWarningNotification(budget, reason, currentMetric, limitMetric, lockResetTimestamp)
            }
        }

        budgetRepository.recordAuditLog(
            budget.packageName,
            "ENFORCEMENT_TRIGGERED",
            "Triggered ${budget.lockMode.name} restriction for ${budget.displayName}: ${reason.title} ($currentMetric / $limitMetric)"
        )
    }

    private fun showLimitReachedScreen(
        budget: AppBudget,
        reason: LimitExhaustionReason,
        currentMetric: String,
        limitMetric: String,
        lockResetTimestamp: Long
    ) {
        val intent = Intent(context, LimitReachedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LimitReachedActivity.EXTRA_PACKAGE_NAME, budget.packageName)
            putExtra(LimitReachedActivity.EXTRA_APP_NAME, budget.displayName)
            putExtra(LimitReachedActivity.EXTRA_REASON_NAME, reason.name)
            putExtra(LimitReachedActivity.EXTRA_CURRENT_METRIC, currentMetric)
            putExtra(LimitReachedActivity.EXTRA_LIMIT_METRIC, limitMetric)
            putExtra(LimitReachedActivity.EXTRA_IS_STRICT, budget.lockMode == LockMode.STRICT)
            putExtra(LimitReachedActivity.EXTRA_LOCK_RESET_TIMESTAMP, lockResetTimestamp)
        }
        context.startActivity(intent)
    }

    private fun showWarningNotification(
        budget: AppBudget,
        reason: LimitExhaustionReason,
        currentMetric: String,
        limitMetric: String,
        lockResetTimestamp: Long = 0L
    ) {
        val intent = Intent(context, LimitReachedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LimitReachedActivity.EXTRA_PACKAGE_NAME, budget.packageName)
            putExtra(LimitReachedActivity.EXTRA_APP_NAME, budget.displayName)
            putExtra(LimitReachedActivity.EXTRA_REASON_NAME, reason.name)
            putExtra(LimitReachedActivity.EXTRA_CURRENT_METRIC, currentMetric)
            putExtra(LimitReachedActivity.EXTRA_LIMIT_METRIC, limitMetric)
            putExtra(LimitReachedActivity.EXTRA_IS_STRICT, budget.lockMode == LockMode.STRICT)
            putExtra(LimitReachedActivity.EXTRA_LOCK_RESET_TIMESTAMP, lockResetTimestamp)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            budget.packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ScrollLock: ${reason.title}")
            .setContentText("${budget.displayName}: $currentMetric of $limitMetric used.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(budget.packageName.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun isCurrentlyInScheduleWindow(budget: AppBudget): Boolean {
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = budget.scheduleStartHour * 60 + budget.scheduleStartMinute
        val endMinutes = budget.scheduleEndHour * 60 + budget.scheduleEndMinute

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else if (startMinutes > endMinutes) {
            // Overnights (e.g., 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            false
        }
    }

    private fun formatScheduleWindow(budget: AppBudget): String {
        return String.format(
            "%02d:%02d - %02d:%02d",
            budget.scheduleStartHour,
            budget.scheduleStartMinute,
            budget.scheduleEndHour,
            budget.scheduleEndMinute
        )
    }

    companion object {
        private const val CHANNEL_ID = "scrolllock_limits_channel"
    }
}
