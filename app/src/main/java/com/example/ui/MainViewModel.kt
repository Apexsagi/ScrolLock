package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollLockApplication
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DailyUsageSummaryEntity
import com.example.data.repository.BudgetRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.UsageStatsRepository
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.CapabilityState
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.MonitoringHealthInfo
import com.example.domain.model.ScrollObservation
import com.example.domain.model.SpikeSpikeRecord
import com.example.monitoring.AccessibilityScrollMonitor
import com.example.monitoring.FeasibilitySpikeManager
import com.example.monitoring.SpikeScenarioResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    WELCOME,
    ONBOARDING,
    DASHBOARD,
    BUDGET_SETTINGS,
    ANALYTICS,
    MONITORING_HEALTH,
    PRIVACY,
    FEASIBILITY_SPIKE,
    APP_SELECTION,
    APP_DETAIL_SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScrollLockApplication
    private val budgetRepository: BudgetRepository = app.budgetRepository
    private val usageStatsRepository: UsageStatsRepository = app.usageStatsRepository
    private val installedAppsRepository: InstalledAppsRepository = app.installedAppsRepository
    private val userPreferencesRepository: UserPreferencesRepository = app.userPreferencesRepository
    private val scrollMonitor: AccessibilityScrollMonitor? = ScrollLockApplication.scrollMonitor
    private val feasibilitySpikeManager: FeasibilitySpikeManager = app.feasibilitySpikeManager

    // Initial state is null until user preference determines whether to show Welcome or Dashboard
    private val _currentScreen = MutableStateFlow<ScreenDestination?>(null)
    val currentScreen: StateFlow<ScreenDestination?> = _currentScreen.asStateFlow()

    private val _selectedAppPackage = MutableStateFlow<String?>(null)
    val selectedAppPackage: StateFlow<String?> = _selectedAppPackage.asStateFlow()

    val allBudgets: StateFlow<List<AppBudget>> = budgetRepository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Discovered apps on device
    private val _discoveredApps = MutableStateFlow<List<DiscoveredApp>>(emptyList())
    val discoveredApps: StateFlow<List<DiscoveredApp>> = _discoveredApps.asStateFlow()

    private val _isDiscoveringApps = MutableStateFlow(false)
    val isDiscoveringApps: StateFlow<Boolean> = _isDiscoveringApps.asStateFlow()

    // Real-time maps for multi-app metrics
    private val _scrollCountsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val scrollCountsMap: StateFlow<Map<String, Int>> = _scrollCountsMap.asStateFlow()

    private val _screenTimesMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val screenTimesMap: StateFlow<Map<String, Long>> = _screenTimesMap.asStateFlow()

    private val _launchesMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val launchesMap: StateFlow<Map<String, Int>> = _launchesMap.asStateFlow()

    private val _monitoringHealth = MutableStateFlow(
        scrollMonitor?.getMonitoringHealthInfo(application, 0) ?: MonitoringHealthInfo(
            capabilityState = CapabilityState.NOT_CONFIGURED,
            usageAccessGranted = false,
            accessibilityGranted = false,
            overlayGranted = false,
            notificationGranted = false,
            lastEventTimestamp = null,
            diagnosticMessage = "Initializing...",
            observedEventsToday = 0
        )
    )
    val monitoringHealth: StateFlow<MonitoringHealthInfo> = _monitoringHealth.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferences(
                onboardingCompleted = false,
                strictModeProtection = false,
                notificationsEnabled = true,
                emergencyOverrideUsed = false,
                lastResetDate = ""
            )
        )

    val recentObservations: StateFlow<List<ScrollObservation>> = budgetRepository.observeRecentObservations(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailySummaries: StateFlow<List<DailyUsageSummaryEntity>> = budgetRepository.getAllDailySummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = budgetRepository.getAuditLogs(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spikeRecords: StateFlow<List<SpikeSpikeRecord>> = budgetRepository.getAllSpikeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastSpikeResults = MutableStateFlow<List<SpikeScenarioResult>>(emptyList())
    val lastSpikeResults: StateFlow<List<SpikeScenarioResult>> = _lastSpikeResults.asStateFlow()

    private val _isSpikeRunning = MutableStateFlow(false)
    val isSpikeRunning: StateFlow<Boolean> = _isSpikeRunning.asStateFlow()

    init {
        refreshHealthAndUsage()
        loadDiscoveredApps()
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (_currentScreen.value == null) {
                _currentScreen.value = if (prefs.onboardingCompleted) {
                    ScreenDestination.DASHBOARD
                } else {
                    ScreenDestination.WELCOME
                }
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        if (destination == ScreenDestination.APP_SELECTION) {
            loadDiscoveredApps()
        }
        _currentScreen.value = destination
    }

    fun selectAppDetail(packageName: String) {
        _selectedAppPackage.value = packageName
        _currentScreen.value = ScreenDestination.APP_DETAIL_SETTINGS
    }

    fun loadDiscoveredApps() {
        viewModelScope.launch {
            _isDiscoveringApps.value = true
            val apps = installedAppsRepository.getDiscoveredApps()
            _discoveredApps.value = apps
            _isDiscoveringApps.value = false
        }
    }

    fun saveSelectedApps(selected: List<DiscoveredApp>) {
        viewModelScope.launch {
            val existing = budgetRepository.getAllBudgetsOnce()
            val existingPackages = existing.map { it.packageName }.toSet()
            val selectedPackages = selected.map { it.packageName }.toSet()

            // Add newly selected apps
            selected.forEach { app ->
                if (!existingPackages.contains(app.packageName)) {
                    budgetRepository.addAppToMonitored(app)
                }
            }

            // Remove unselected apps
            existing.forEach { budget ->
                if (!selectedPackages.contains(budget.packageName)) {
                    budgetRepository.removeBudget(budget.packageName)
                }
            }

            refreshHealthAndUsage()
        }
    }

    fun saveBudgetDetail(budget: AppBudget) {
        viewModelScope.launch {
            budgetRepository.addOrUpdateBudget(budget)
            refreshHealthAndUsage()
        }
    }

    fun deleteBudget(packageName: String) {
        viewModelScope.launch {
            budgetRepository.removeBudget(packageName)
            refreshHealthAndUsage()
        }
    }

    fun pauseApp(packageName: String, minutes: Int = 15) {
        viewModelScope.launch {
            budgetRepository.pauseApp(packageName, minutes)
            refreshHealthAndUsage()
        }
    }

    fun resumeApp(packageName: String) {
        viewModelScope.launch {
            budgetRepository.resumeApp(packageName)
            refreshHealthAndUsage()
        }
    }

    fun refreshHealthAndUsage() {
        viewModelScope.launch {
            val currentBudgets = budgetRepository.getAllBudgetsOnce()
            val counts = mutableMapOf<String, Int>()
            val times = mutableMapOf<String, Long>()
            val launches = mutableMapOf<String, Int>()

            currentBudgets.forEach { budget ->
                val pkg = budget.packageName
                counts[pkg] = budgetRepository.getScrollCountToday(pkg)
                val screenTime = usageStatsRepository.getScreenTimeTodayMillis(pkg)
                times[pkg] = screenTime
                launches[pkg] = budgetRepository.getLaunchesToday(pkg)

                // Sync daily summaries
                budgetRepository.syncDailySummaryForToday(pkg, screenTime)
            }

            _scrollCountsMap.value = counts
            _screenTimesMap.value = times
            _launchesMap.value = launches

            val totalEvents = counts.values.sum()
            scrollMonitor?.let { monitor ->
                _monitoringHealth.value = monitor.getMonitoringHealthInfo(getApplication(), totalEvents)
            }
        }
    }

    fun updateBudgetLimit(packageName: String, limit: Int) {
        viewModelScope.launch {
            budgetRepository.updateLimit(packageName, limit)
            refreshHealthAndUsage()
        }
    }

    fun updateLockMode(packageName: String, lockMode: LockMode) {
        viewModelScope.launch {
            budgetRepository.updateLockMode(packageName, lockMode)
            refreshHealthAndUsage()
        }
    }

    fun toggleAppEnabled(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            budgetRepository.updateEnabled(packageName, isEnabled)
            refreshHealthAndUsage()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _currentScreen.value = ScreenDestination.DASHBOARD
        }
    }

    fun simulateGesture(packageName: String) {
        scrollMonitor?.simulateSpikeEvent(packageName)
        refreshHealthAndUsage()
    }

    fun runFeasibilitySpike() {
        viewModelScope.launch {
            _isSpikeRunning.value = true
            val results = feasibilitySpikeManager.runAutomatedSpikeValidation(MonitoredAppInfo.INSTAGRAM.packageName)
            _lastSpikeResults.value = results
            _isSpikeRunning.value = false
            refreshHealthAndUsage()
        }
    }

    fun resetTodayUsage(packageName: String? = null) {
        viewModelScope.launch {
            budgetRepository.resetTodayUsage(packageName)
            refreshHealthAndUsage()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            budgetRepository.clearAllData()
            budgetRepository.initializeDefaultBudgetsIfEmpty()
            userPreferencesRepository.clearAllPreferences()
            _lastSpikeResults.value = emptyList()
            _currentScreen.value = ScreenDestination.WELCOME
            refreshHealthAndUsage()
        }
    }

    fun setStrictModeProtection(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setStrictModeProtection(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    // Feature 1: PIN Security Methods
    fun setPin(pinHash: String, salt: String) {
        viewModelScope.launch {
            userPreferencesRepository.setPin(pinHash, salt)
            budgetRepository.recordAuditLog("SYSTEM", "PIN_CREATED", "4-digit PIN protection enabled")
        }
    }

    fun removePin() {
        viewModelScope.launch {
            userPreferencesRepository.removePin()
            budgetRepository.recordAuditLog("SYSTEM", "PIN_REMOVED", "4-digit PIN protection removed")
        }
    }

    fun updateSecurityControl(
        requireDisableMonitoring: Boolean? = null,
        requireChangeLimits: Boolean? = null,
        requireRemoveApps: Boolean? = null,
        requireResetUsage: Boolean? = null,
        requireChangeSecurity: Boolean? = null
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateSecurityControl(
                requireDisableMonitoring = requireDisableMonitoring,
                requireChangeLimits = requireChangeLimits,
                requireRemoveApps = requireRemoveApps,
                requireResetUsage = requireResetUsage,
                requireChangeSecurity = requireChangeSecurity
            )
        }
    }

    fun recordFailedPinAttempt() {
        viewModelScope.launch {
            userPreferencesRepository.recordFailedPinAttempt()
        }
    }

    fun resetFailedPinAttempts() {
        viewModelScope.launch {
            userPreferencesRepository.resetFailedPinAttempts()
        }
    }

    // Feature 2: Random Reset Methods
    fun setRandomResetSettings(enabled: Boolean, minHours: Int, maxHours: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setRandomResetSettings(enabled, minHours, maxHours)
            budgetRepository.recordAuditLog(
                "SYSTEM",
                "RANDOM_RESET_CONFIG",
                "Random reset ${if (enabled) "enabled" else "disabled"} ($minHours - $maxHours hours)"
            )
        }
    }

    fun recordFailedAttempt() = recordFailedPinAttempt()
    fun resetFailedAttempts() = resetFailedPinAttempts()
    fun updateRandomReset(enabled: Boolean, minHours: Int, maxHours: Int) =
        setRandomResetSettings(enabled, minHours, maxHours)

    // Settings Categories Helpers
    fun setDefaultLimits(scrollBudget: Int, timeMinutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultLimits(scrollBudget, timeMinutes)
        }
    }

    fun setLimitReachedAction(action: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLimitReachedAction(action)
        }
    }

    fun setEmergencyGraceSettings(graceMinutes: Int, overrideAvailable: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setEmergencyGraceSettings(graceMinutes, overrideAvailable)
        }
    }

    fun setSleepSchedule(enabled: Boolean, start: String, end: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSleepSchedule(enabled, start, end)
        }
    }

    fun setWorkStudySchedule(enabled: Boolean, start: String, end: String, days: String) {
        viewModelScope.launch {
            userPreferencesRepository.setWorkStudySchedule(enabled, start, end, days)
        }
    }

    fun startFocusSession(durationMinutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.startFocusSession(durationMinutes)
            budgetRepository.recordAuditLog("USER", "FOCUS_SESSION", "Started ${durationMinutes}m focus session")
        }
    }

    fun cancelFocusSession() {
        viewModelScope.launch {
            userPreferencesRepository.cancelFocusSession()
        }
    }

    fun pauseProtection(durationMinutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.pauseProtection(durationMinutes)
            budgetRepository.recordAuditLog("USER", "PAUSE_PROTECTION", "Paused protection for ${durationMinutes}m")
        }
    }

    fun resumeProtection() {
        viewModelScope.launch {
            userPreferencesRepository.resumeProtection()
            budgetRepository.recordAuditLog("USER", "RESUME_PROTECTION", "Protection resumed")
        }
    }

    fun updateNotificationPreferences(
        alert80: Boolean? = null,
        alert100: Boolean? = null,
        dailySummary: Boolean? = null,
        breakReminder: Boolean? = null,
        breakMinutes: Int? = null,
        streak: Boolean? = null,
        quietHours: Boolean? = null,
        quietStart: String? = null,
        quietEnd: String? = null
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateNotificationPreferences(
                alert80, alert100, dailySummary, breakReminder, breakMinutes, streak, quietHours, quietStart, quietEnd
            )
        }
    }

    fun updateAppearancePreferences(
        theme: String? = null,
        language: String? = null,
        fontScale: String? = null,
        haptic: Boolean? = null,
        compactCards: Boolean? = null
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppearancePreferences(theme, language, fontScale, haptic, compactCards)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLockEnabled(enabled)
        }
    }
}
