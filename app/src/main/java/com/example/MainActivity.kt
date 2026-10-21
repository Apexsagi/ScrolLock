package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppBudget
import com.example.domain.model.MonitoredAppInfo
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppDetailSettingsScreen
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.BudgetSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeasibilitySpikeScreen
import com.example.ui.screens.MonitoringHealthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.WelcomeScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.example.ui.components.ScreenLockBottomBar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ScrollLockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNavIntent(intent)

        setContent {
            ScrollLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScrollLockApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHealthAndUsage()
    }

    private fun handleNavIntent(intent: Intent?) {
        val target = intent?.getStringExtra("NAV_TARGET")
        if (target == "ANALYTICS") {
            viewModel.navigateTo(ScreenDestination.ANALYTICS)
        } else if (target == "DASHBOARD") {
            viewModel.navigateTo(ScreenDestination.DASHBOARD)
        }
    }
}

@Composable
fun ScrollLockApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val scrollCountsMap by viewModel.scrollCountsMap.collectAsStateWithLifecycle()
    val screenTimesMap by viewModel.screenTimesMap.collectAsStateWithLifecycle()
    val launchesMap by viewModel.launchesMap.collectAsStateWithLifecycle()
    val discoveredApps by viewModel.discoveredApps.collectAsStateWithLifecycle()
    val isDiscoveringApps by viewModel.isDiscoveringApps.collectAsStateWithLifecycle()
    val selectedAppPackage by viewModel.selectedAppPackage.collectAsStateWithLifecycle()
    val health by viewModel.monitoringHealth.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val recentObs by viewModel.recentObservations.collectAsStateWithLifecycle()
    val dailySummaries by viewModel.allDailySummaries.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val spikeResults by viewModel.lastSpikeResults.collectAsStateWithLifecycle()
    val isSpikeRunning by viewModel.isSpikeRunning.collectAsStateWithLifecycle()
    val savedSpikeRecords by viewModel.spikeRecords.collectAsStateWithLifecycle()

    val isMainTab = currentScreen in listOf(
        ScreenDestination.DASHBOARD,
        ScreenDestination.ANALYTICS,
        ScreenDestination.BUDGET_SETTINGS,
        ScreenDestination.PRIVACY
    )

    // Handle system back navigation when not on root Dashboard or Welcome
    if (currentScreen != null && currentScreen != ScreenDestination.DASHBOARD && currentScreen != ScreenDestination.WELCOME) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isMainTab) {
                ScreenLockBottomBar(
                    currentDestination = currentScreen,
                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                )
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isMainTab) scaffoldPadding.calculateBottomPadding() else 0.dp)
        ) {
            when (currentScreen) {
                null -> {
                    // Calm initial splash surface while preferences load
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                ScreenDestination.WELCOME -> {
                    WelcomeScreen(
                        onGetStarted = { viewModel.navigateTo(ScreenDestination.ONBOARDING) }
                    )
                }

                ScreenDestination.ONBOARDING -> {
                    OnboardingScreen(
                        monitoringHealth = health,
                        onRefreshHealth = { viewModel.refreshHealthAndUsage() },
                        onSaveInitialBudgets = { igLimit, ytLimit ->
                            viewModel.updateBudgetLimit(MonitoredAppInfo.INSTAGRAM.packageName, igLimit)
                            viewModel.updateBudgetLimit(MonitoredAppInfo.YOUTUBE.packageName, ytLimit)
                        },
                        onComplete = { viewModel.completeOnboarding() }
                    )
                }

                ScreenDestination.DASHBOARD -> {
                    DashboardScreen(
                        budgets = budgets,
                        scrollCountsMap = scrollCountsMap,
                        screenTimesMap = screenTimesMap,
                        launchesMap = launchesMap,
                        health = health,
                        onNavigate = { dest -> viewModel.navigateTo(dest) },
                        onSelectAppDetail = { pkg -> viewModel.selectAppDetail(pkg) },
                        onRefresh = { viewModel.refreshHealthAndUsage() }
                    )
                }

                ScreenDestination.APP_SELECTION -> {
                    AppSelectionScreen(
                        discoveredApps = discoveredApps,
                        existingBudgets = budgets,
                        isLoading = isDiscoveringApps,
                        userPreferences = userPrefs,
                        onRecordFailedAttempt = { viewModel.recordFailedAttempt() },
                        onResetFailedAttempts = { viewModel.resetFailedAttempts() },
                        onSaveSelectedApps = { selected -> viewModel.saveSelectedApps(selected) },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.APP_DETAIL_SETTINGS -> {
                    val selectedBudget = budgets.find { it.packageName == selectedAppPackage }
                        ?: AppBudget(selectedAppPackage ?: "", selectedAppPackage ?: "App")

                    AppDetailSettingsScreen(
                        budget = selectedBudget,
                        userPreferences = userPrefs,
                        onSaveBudget = { updated -> viewModel.saveBudgetDetail(updated) },
                        onDeleteBudget = { pkg -> viewModel.deleteBudget(pkg) },
                        onPauseApp = { pkg, mins -> viewModel.pauseApp(pkg, mins) },
                        onResumeApp = { pkg -> viewModel.resumeApp(pkg) },
                        onRecordFailedAttempt = { viewModel.recordFailedAttempt() },
                        onResetFailedAttempts = { viewModel.resetFailedAttempts() },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.BUDGET_SETTINGS -> {
                    BudgetSettingsScreen(
                        budgets = budgets,
                        userPreferences = userPrefs,
                        onUpdateLimit = { pkg, limit -> viewModel.updateBudgetLimit(pkg, limit) },
                        onUpdateLockMode = { pkg, mode -> viewModel.updateLockMode(pkg, mode) },
                        onToggleAppEnabled = { pkg, enabled -> viewModel.toggleAppEnabled(pkg, enabled) },
                        onToggleStrictModeProtection = { enabled -> viewModel.setStrictModeProtection(enabled) },
                        onToggleNotifications = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                        onSetPin = { hash, salt -> viewModel.setPin(hash, salt) },
                        onRemovePin = { viewModel.removePin() },
                        onUpdateSecurityControl = { disableMon, changeLimits, removeApps, resetUsage, changeSecurity ->
                            viewModel.updateSecurityControl(disableMon, changeLimits, removeApps, resetUsage, changeSecurity)
                        },
                        onRecordFailedAttempt = { viewModel.recordFailedAttempt() },
                        onResetFailedAttempts = { viewModel.resetFailedAttempts() },
                        onUpdateRandomReset = { enabled, minH, maxH ->
                            viewModel.updateRandomReset(enabled, minH, maxH)
                        },
                        onResetTodayUsage = { viewModel.resetTodayUsage() },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.ANALYTICS -> {
                    AnalyticsScreen(
                        summaries = dailySummaries,
                        scrollCountsMap = scrollCountsMap,
                        screenTimesMap = screenTimesMap,
                        budgets = budgets,
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.MONITORING_HEALTH -> {
                    MonitoringHealthScreen(
                        health = health,
                        recentObservations = recentObs,
                        onRefreshHealth = { viewModel.refreshHealthAndUsage() },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.PRIVACY -> {
                    PrivacyScreen(
                        auditLogs = auditLogs,
                        userPreferences = userPrefs,
                        health = health,
                        onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                        onNavigateToHealth = { viewModel.navigateTo(ScreenDestination.MONITORING_HEALTH) },
                        onRecordFailedAttempt = { viewModel.recordFailedAttempt() },
                        onResetFailedAttempts = { viewModel.resetFailedAttempts() },
                        onClearAllData = { viewModel.clearAllData() },
                        onResetTodayUsage = { viewModel.resetTodayUsage() },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }

                ScreenDestination.FEASIBILITY_SPIKE -> {
                    FeasibilitySpikeScreen(
                        isSpikeRunning = isSpikeRunning,
                        lastSpikeResults = spikeResults,
                        savedSpikeRecords = savedSpikeRecords,
                        onRunSpike = { viewModel.runFeasibilitySpike() },
                        onSimulateGesture = { pkg -> viewModel.simulateGesture(pkg) },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.DASHBOARD) }
                    )
                }
            }
        }
    }
}
