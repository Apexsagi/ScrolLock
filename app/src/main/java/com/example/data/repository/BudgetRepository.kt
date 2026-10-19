package com.example.data.repository

import com.example.data.local.ScrollLockDatabase
import com.example.data.local.entity.AppBudgetEntity
import com.example.data.local.entity.AppLaunchEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DailyUsageSummaryEntity
import com.example.data.local.entity.ScrollObservationEntity
import com.example.data.local.entity.SpikeTestEntity
import com.example.domain.model.AppBudget
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.EventClassification
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.ScrollObservation
import com.example.domain.model.SpikeSpikeRecord
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetRepository(private val database: ScrollLockDatabase) {

    private val appBudgetDao = database.appBudgetDao()
    private val appLaunchDao = database.appLaunchDao()
    private val scrollObservationDao = database.scrollObservationDao()
    private val dailyUsageSummaryDao = database.dailyUsageSummaryDao()
    private val auditLogDao = database.auditLogDao()
    private val spikeTestDao = database.spikeTestDao()

    val allBudgets: Flow<List<AppBudget>> = appBudgetDao.getAllBudgets().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun initializeDefaultBudgetsIfEmpty() {
        val all = appBudgetDao.getAllBudgetsOnce()
        if (all.isEmpty()) {
            val defaults = listOf(
                AppBudgetEntity(
                    packageName = MonitoredAppInfo.INSTAGRAM.packageName,
                    displayName = MonitoredAppInfo.INSTAGRAM.displayName,
                    dailyLimit = 50,
                    screenTimeLimitMinutes = 45,
                    launchLimit = 15,
                    scrollCountingSupported = true,
                    category = "Social",
                    isEnabled = true,
                    lockMode = LockMode.STANDARD.name
                ),
                AppBudgetEntity(
                    packageName = MonitoredAppInfo.YOUTUBE.packageName,
                    displayName = MonitoredAppInfo.YOUTUBE.displayName,
                    dailyLimit = 100,
                    screenTimeLimitMinutes = 60,
                    launchLimit = 20,
                    scrollCountingSupported = true,
                    category = "Video",
                    isEnabled = true,
                    lockMode = LockMode.STANDARD.name
                ),
                AppBudgetEntity(
                    packageName = MonitoredAppInfo.FACEBOOK.packageName,
                    displayName = MonitoredAppInfo.FACEBOOK.displayName,
                    dailyLimit = 0,
                    screenTimeLimitMinutes = 30,
                    launchLimit = 10,
                    scrollCountingSupported = false,
                    category = "Social",
                    isEnabled = true,
                    lockMode = LockMode.STANDARD.name
                ),
                AppBudgetEntity(
                    packageName = MonitoredAppInfo.SNAPCHAT.packageName,
                    displayName = MonitoredAppInfo.SNAPCHAT.displayName,
                    dailyLimit = 0,
                    screenTimeLimitMinutes = 30,
                    launchLimit = 10,
                    scrollCountingSupported = false,
                    category = "Social",
                    isEnabled = true,
                    lockMode = LockMode.STANDARD.name
                ),
                AppBudgetEntity(
                    packageName = MonitoredAppInfo.TIKTOK.packageName,
                    displayName = MonitoredAppInfo.TIKTOK.displayName,
                    dailyLimit = 60,
                    screenTimeLimitMinutes = 30,
                    launchLimit = 15,
                    scrollCountingSupported = true,
                    category = "Video",
                    isEnabled = true,
                    lockMode = LockMode.STANDARD.name
                )
            )
            appBudgetDao.insertAll(defaults)
        }
    }

    fun observeBudget(packageName: String): Flow<AppBudget?> {
        return appBudgetDao.observeBudgetByPackage(packageName).map { it?.toDomain() }
    }

    suspend fun getBudget(packageName: String): AppBudget? {
        return appBudgetDao.getBudgetByPackage(packageName)?.toDomain()
    }

    suspend fun isPackageMonitored(packageName: String): Boolean {
        return appBudgetDao.isPackageMonitored(packageName)
    }

    suspend fun getAllBudgetsOnce(): List<AppBudget> {
        return appBudgetDao.getAllBudgetsOnce().map { it.toDomain() }
    }

    suspend fun addOrUpdateBudget(budget: AppBudget) {
        appBudgetDao.insertBudget(budget.toEntity())
        recordAuditLog(budget.packageName, "BUDGET_CONFIGURED", "Limits set for ${budget.displayName}")
    }

    suspend fun addAppToMonitored(app: DiscoveredApp) {
        val existing = appBudgetDao.getBudgetByPackage(app.packageName)
        if (existing == null) {
            val entity = AppBudgetEntity(
                packageName = app.packageName,
                displayName = app.appName,
                dailyLimit = if (app.isScrollTrackingSupported) 50 else 0,
                screenTimeLimitMinutes = 45,
                launchLimit = 0,
                scrollCountingSupported = app.isScrollTrackingSupported,
                category = app.category,
                isEnabled = true,
                lockMode = LockMode.STANDARD.name
            )
            appBudgetDao.insertBudget(entity)
            recordAuditLog(app.packageName, "APP_ADDED", "Added ${app.appName} to monitored apps")
        } else {
            appBudgetDao.updateEnabled(app.packageName, true)
        }
    }

    suspend fun removeBudget(packageName: String) {
        val budget = getBudget(packageName)
        appBudgetDao.deleteByPackage(packageName)
        recordAuditLog(packageName, "APP_REMOVED", "Removed ${budget?.displayName ?: packageName} from monitored list")
    }

    suspend fun updateLimit(packageName: String, limit: Int) {
        val safeLimit = limit.coerceAtLeast(1)
        appBudgetDao.updateLimit(packageName, safeLimit)
        recordAuditLog(packageName, "LIMIT_ADJUSTED", "Scroll budget updated to $safeLimit scrolls")
        syncDailySummaryForToday(packageName)
    }

    suspend fun updateScreenTimeLimit(packageName: String, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(0)
        appBudgetDao.updateScreenTimeLimit(packageName, safeMinutes)
        recordAuditLog(packageName, "SCREEN_TIME_LIMIT_ADJUSTED", "Screen time limit set to $safeMinutes minutes")
        syncDailySummaryForToday(packageName)
    }

    suspend fun updateLaunchLimit(packageName: String, limit: Int) {
        val safeLimit = limit.coerceAtLeast(0)
        appBudgetDao.updateLaunchLimit(packageName, safeLimit)
        recordAuditLog(packageName, "LAUNCH_LIMIT_ADJUSTED", "Launch limit set to $safeLimit opens")
    }

    suspend fun updateLockMode(packageName: String, lockMode: LockMode) {
        appBudgetDao.updateLockMode(packageName, lockMode.name)
        recordAuditLog(packageName, "LOCK_MODE_CHANGED", "Lock mode set to ${lockMode.name}")
    }

    suspend fun updateEnabled(packageName: String, isEnabled: Boolean) {
        appBudgetDao.updateEnabled(packageName, isEnabled)
        val state = if (isEnabled) "enabled" else "disabled"
        recordAuditLog(packageName, "MONITORING_TOGGLED", "App monitoring $state")
    }

    suspend fun setLockResetTimestamp(packageName: String, timestamp: Long) {
        appBudgetDao.updateLockResetTimestamp(packageName, timestamp)
    }

    suspend fun clearLockResetTimestamp(packageName: String) {
        appBudgetDao.updateLockResetTimestamp(packageName, 0L)
    }

    suspend fun pauseApp(packageName: String, durationMinutes: Int = 15) {
        val pauseUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        appBudgetDao.updatePauseState(packageName, isPaused = true, pauseUntil = pauseUntil)
        recordAuditLog(packageName, "MONITORING_PAUSED", "Restrictions paused for $durationMinutes minutes")
    }

    suspend fun resumeApp(packageName: String) {
        appBudgetDao.updatePauseState(packageName, isPaused = false, pauseUntil = 0L)
        recordAuditLog(packageName, "MONITORING_RESUMED", "Restrictions resumed immediately")
    }

    fun observeScrollCountToday(packageName: String): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return scrollObservationDao.observeCountBetween(packageName, start, end)
    }

    suspend fun getScrollCountToday(packageName: String): Int {
        val (start, end) = getTodayTimeRange()
        return scrollObservationDao.getCountBetween(packageName, start, end)
    }

    suspend fun recordAppLaunch(packageName: String): Long {
        val now = System.currentTimeMillis()
        val dateKey = getTodayDateKey()
        return appLaunchDao.insertLaunch(
            AppLaunchEntity(
                packageName = packageName,
                timestamp = now,
                dateKey = dateKey
            )
        )
    }

    fun observeLaunchesToday(packageName: String): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return appLaunchDao.observeLaunchesBetween(packageName, start, end)
    }

    suspend fun getLaunchesToday(packageName: String): Int {
        val (start, end) = getTodayTimeRange()
        return appLaunchDao.getLaunchesBetween(packageName, start, end)
    }

    fun observeRemainingBudgetToday(packageName: String): Flow<Int> {
        val budgetFlow = observeBudget(packageName)
        val countFlow = observeScrollCountToday(packageName)
        return combine(budgetFlow, countFlow) { budget, count ->
            val limit = budget?.dailyLimit ?: 0
            if (limit <= 0) 0 else maxOf(0, limit - count)
        }
    }

    fun observeIsBudgetExhaustedToday(packageName: String): Flow<Boolean> {
        val budgetFlow = observeBudget(packageName)
        val countFlow = observeScrollCountToday(packageName)
        return combine(budgetFlow, countFlow) { budget, count ->
            if (budget == null || !budget.isEnabled) false
            else if (budget.dailyLimit > 0 && count >= budget.dailyLimit) true
            else false
        }
    }

    suspend fun recordObservation(observation: ScrollObservation): Long {
        val entity = ScrollObservationEntity(
            targetPackage = observation.targetPackage,
            timestamp = observation.timestamp,
            eventClassification = observation.eventClassification.name,
            verificationStatus = observation.verificationStatus.name,
            rawDeltaY = observation.rawDeltaY,
            sourceNodeClass = observation.sourceNodeClass
        )
        val insertedId = scrollObservationDao.insertObservation(entity)
        syncDailySummaryForToday(observation.targetPackage)
        return insertedId
    }

    val latestObservation: Flow<ScrollObservation?> = scrollObservationDao.observeLatestObservation().map { entity ->
        entity?.toDomain()
    }

    fun observeRecentObservations(limit: Int = 20): Flow<List<ScrollObservation>> {
        return scrollObservationDao.observeRecentObservations(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun syncDailySummaryForToday(packageName: String, screenTimeMillis: Long = 0L) {
        val dateKey = getTodayDateKey()
        val count = getScrollCountToday(packageName)
        val launches = getLaunchesToday(packageName)
        val budget = getBudget(packageName)
        val limit = budget?.dailyLimit ?: 50
        val screenLimitMillis = (budget?.screenTimeLimitMinutes ?: 0) * 60 * 1000L

        val isReached = (budget?.dailyLimit ?: 0) > 0 && count >= limit ||
                (screenLimitMillis > 0 && screenTimeMillis >= screenLimitMillis) ||
                ((budget?.launchLimit ?: 0) > 0 && launches >= (budget?.launchLimit ?: 0))

        val existing = dailyUsageSummaryDao.getSummary(dateKey, packageName)
        val summary = DailyUsageSummaryEntity(
            id = existing?.id ?: 0L,
            dateKey = dateKey,
            targetPackage = packageName,
            verifiedScrollCount = count,
            screenTimeMillis = if (screenTimeMillis > 0) screenTimeMillis else (existing?.screenTimeMillis ?: 0L),
            launchesCount = launches,
            budgetReached = isReached,
            budgetLimitAtExhaustion = limit,
            monitoringCompleteness = "COMPLETE"
        )
        dailyUsageSummaryDao.upsertSummary(summary)

        if (isReached && existing?.budgetReached != true) {
            recordAuditLog(
                packageName,
                "BUDGET_EXHAUSTED",
                "Daily restriction triggered for $packageName"
            )
        }
    }

    fun getRecentHistory(packageName: String, days: Int = 30): Flow<List<DailyUsageSummaryEntity>> {
        return dailyUsageSummaryDao.getRecentHistory(packageName, days)
    }

    fun getAllDailySummaries(): Flow<List<DailyUsageSummaryEntity>> {
        return dailyUsageSummaryDao.getAllSummaries()
    }

    suspend fun recordAuditLog(targetPackage: String, eventType: String, details: String) {
        auditLogDao.insertLog(
            AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                targetPackage = targetPackage,
                eventType = eventType,
                details = details
            )
        )
    }

    fun getAuditLogs(limit: Int = 30): Flow<List<AuditLogEntity>> {
        return auditLogDao.getRecentLogs(limit)
    }

    // Spike test records
    suspend fun recordSpikeTest(
        scenario: String,
        expectedSwipes: Int,
        detectedEvents: Int,
        falsePositives: Int,
        deduplicatedEvents: Int,
        precisionPercent: Int,
        recallPercent: Int,
        notes: String
    ): Long {
        return spikeTestDao.insertRecord(
            SpikeTestEntity(
                timestamp = System.currentTimeMillis(),
                scenario = scenario,
                expectedSwipes = expectedSwipes,
                detectedEvents = detectedEvents,
                falsePositives = falsePositives,
                deduplicatedEvents = deduplicatedEvents,
                precisionPercent = precisionPercent,
                recallPercent = recallPercent,
                notes = notes
            )
        )
    }

    fun getAllSpikeRecords(): Flow<List<SpikeSpikeRecord>> {
        return spikeTestDao.getAllRecords().map { entities ->
            entities.map {
                SpikeSpikeRecord(
                    id = it.id,
                    timestamp = it.timestamp,
                    scenario = it.scenario,
                    expectedSwipes = it.expectedSwipes,
                    detectedEvents = it.detectedEvents,
                    falsePositives = it.falsePositives,
                    deduplicatedEvents = it.deduplicatedEvents,
                    precisionPercent = it.precisionPercent,
                    recallPercent = it.recallPercent,
                    notes = it.notes
                )
            }
        }
    }

    suspend fun clearAllData() {
        scrollObservationDao.clearAll()
        dailyUsageSummaryDao.clearAll()
        auditLogDao.clearAll()
        spikeTestDao.clearAll()
        appLaunchDao.clearAll()
    }

    // Mapping extensions
    private fun AppBudgetEntity.toDomain() = AppBudget(
        packageName = packageName,
        displayName = displayName,
        dailyLimit = dailyLimit,
        screenTimeLimitMinutes = screenTimeLimitMinutes,
        launchLimit = launchLimit,
        scrollCountingSupported = scrollCountingSupported,
        weekendScreenTimeLimitMinutes = weekendScreenTimeLimitMinutes,
        scheduleStartHour = scheduleStartHour,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndHour = scheduleEndHour,
        scheduleEndMinute = scheduleEndMinute,
        isScheduleEnabled = isScheduleEnabled,
        warnAtEightyPercent = warnAtEightyPercent,
        isPaused = isPaused && System.currentTimeMillis() < pauseUntilTimestamp,
        pauseUntilTimestamp = pauseUntilTimestamp,
        category = category,
        isEnabled = isEnabled,
        lockMode = try { LockMode.valueOf(lockMode) } catch (_: Exception) { LockMode.STANDARD },
        resetHour = resetHour,
        resetMinute = resetMinute,
        lockResetTimestamp = lockResetTimestamp
    )

    private fun AppBudget.toEntity() = AppBudgetEntity(
        packageName = packageName,
        displayName = displayName,
        dailyLimit = dailyLimit,
        screenTimeLimitMinutes = screenTimeLimitMinutes,
        launchLimit = launchLimit,
        scrollCountingSupported = scrollCountingSupported,
        weekendScreenTimeLimitMinutes = weekendScreenTimeLimitMinutes,
        scheduleStartHour = scheduleStartHour,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndHour = scheduleEndHour,
        scheduleEndMinute = scheduleEndMinute,
        isScheduleEnabled = isScheduleEnabled,
        warnAtEightyPercent = warnAtEightyPercent,
        isPaused = isPaused,
        pauseUntilTimestamp = pauseUntilTimestamp,
        category = category,
        isEnabled = isEnabled,
        lockMode = lockMode.name,
        resetHour = resetHour,
        resetMinute = resetMinute,
        lockResetTimestamp = lockResetTimestamp
    )

    private fun ScrollObservationEntity.toDomain() = ScrollObservation(
        id = id,
        targetPackage = targetPackage,
        timestamp = timestamp,
        eventClassification = try { EventClassification.valueOf(eventClassification) } catch (_: Exception) { EventClassification.SCROLL_UNSPECIFIED },
        verificationStatus = try { VerificationStatus.valueOf(verificationStatus) } catch (_: Exception) { VerificationStatus.VERIFIED },
        rawDeltaY = rawDeltaY,
        sourceNodeClass = sourceNodeClass
    )

    companion object {
        fun getTodayTimeRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val end = cal.timeInMillis
            return Pair(start, end)
        }

        fun getTodayDateKey(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getMillisUntilMidnight(): Long {
            val now = System.currentTimeMillis()
            val (_, end) = getTodayTimeRange()
            return maxOf(0L, end - now)
        }

        fun isWeekendToday(): Boolean {
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }
    }
}
