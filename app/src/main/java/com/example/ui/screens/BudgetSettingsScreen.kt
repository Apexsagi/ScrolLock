package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoringHealthInfo
import com.example.ui.components.ChangePinDialog
import com.example.ui.components.RemovePinDialog
import com.example.ui.components.SetPinDialog
import com.example.ui.components.VerifyPinDialog
import com.example.ui.screens.settings.SettingsAboutSection
import com.example.ui.screens.settings.SettingsAppearanceSection
import com.example.ui.screens.settings.SettingsLimitsSection
import com.example.ui.screens.settings.SettingsNotificationsSection
import com.example.ui.screens.settings.SettingsPermissionsSection
import com.example.ui.screens.settings.SettingsPrivacySection
import com.example.ui.screens.settings.SettingsSchedulesSection
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText

enum class SettingsCategoryTab(val label: String, val testTag: String) {
    ALL("All", "settings_category_all"),
    LIMITS("Limits & Protection", "settings_category_limits"),
    SCHEDULES("Schedules & Focus", "settings_category_schedules"),
    NOTIFICATIONS("Notifications", "settings_category_notifications"),
    APPEARANCE("Appearance", "settings_category_appearance"),
    PERMISSIONS("Permissions", "settings_category_permissions"),
    PRIVACY("Privacy & Data", "settings_category_privacy"),
    ABOUT("About & Support", "settings_category_about")
}

@Composable
fun BudgetSettingsScreen(
    budgets: List<AppBudget>,
    userPreferences: UserPreferences,
    onUpdateLimit: (packageName: String, limit: Int) -> Unit,
    onUpdateLockMode: (packageName: String, lockMode: LockMode) -> Unit,
    onToggleAppEnabled: (packageName: String, isEnabled: Boolean) -> Unit,
    onToggleStrictModeProtection: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSetPin: (pinHash: String, salt: String) -> Unit,
    onRemovePin: () -> Unit,
    onUpdateSecurityControl: (
        disableMon: Boolean?,
        changeLimits: Boolean?,
        removeApps: Boolean?,
        resetUsage: Boolean?,
        changeSecurity: Boolean?
    ) -> Unit,
    onRecordFailedAttempt: () -> Unit,
    onResetFailedAttempts: () -> Unit,
    onUpdateRandomReset: (enabled: Boolean, minHours: Int, maxHours: Int) -> Unit,
    onResetTodayUsage: () -> Unit = {},
    onNavigateBack: () -> Unit,
    health: MonitoringHealthInfo? = null,
    onSetDefaultLimits: (scrollBudget: Int, timeMinutes: Int) -> Unit = { _, _ -> },
    onSetLimitReachedAction: (action: String) -> Unit = {},
    onSetEmergencyGraceSettings: (graceMinutes: Int, overrideAvailable: Boolean) -> Unit = { _, _ -> },
    onSetSleepSchedule: (enabled: Boolean, start: String, end: String) -> Unit = { _, _, _ -> },
    onSetWorkStudySchedule: (enabled: Boolean, start: String, end: String, days: String) -> Unit = { _, _, _, _ -> },
    onStartFocusSession: (durationMinutes: Int) -> Unit = {},
    onCancelFocusSession: () -> Unit = {},
    onPauseProtection: (durationMinutes: Int) -> Unit = {},
    onResumeProtection: () -> Unit = {},
    onUpdateNotificationPreferences: (
        alert80: Boolean?,
        alert100: Boolean?,
        dailySummary: Boolean?,
        breakReminder: Boolean?,
        breakMinutes: Int?,
        streak: Boolean?,
        quietHours: Boolean?,
        quietStart: String?,
        quietEnd: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUpdateAppearancePreferences: (
        theme: String?,
        language: String?,
        fontScale: String?,
        haptic: Boolean?,
        compactCards: Boolean?
    ) -> Unit = { _, _, _, _, _ -> },
    onSetAppLockEnabled: (Boolean) -> Unit = {},
    onClearAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SettingsCategoryTab.ALL) }

    // PIN Dialog States
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var showVerifyPinDialog by remember { mutableStateOf(false) }
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun executeWithPinCheck(requiresPin: Boolean, action: () -> Unit) {
        if (userPreferences.isPinSet && requiresPin) {
            pendingProtectedAction = action
            showVerifyPinDialog = true
        } else {
            action()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("budget_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = PrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Limits, Schedules, Privacy & Personalization",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            // Top Horizontal Scrollable Category Filter Chips
            item {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategoryTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            // SECTION 1: LIMITS & PROTECTION
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.LIMITS) {
                item {
                    SettingsLimitsSection(
                        budgets = budgets,
                        userPreferences = userPreferences,
                        onUpdateLimit = onUpdateLimit,
                        onUpdateLockMode = onUpdateLockMode,
                        onToggleAppEnabled = onToggleAppEnabled,
                        onToggleStrictModeProtection = onToggleStrictModeProtection,
                        onSetDefaultLimits = onSetDefaultLimits,
                        onSetLimitReachedAction = onSetLimitReachedAction,
                        onSetEmergencyGraceSettings = onSetEmergencyGraceSettings,
                        onOpenSetPin = { showSetPinDialog = true },
                        onOpenChangePin = { showChangePinDialog = true },
                        onOpenRemovePin = { showRemovePinDialog = true },
                        onUpdateSecurityControl = onUpdateSecurityControl,
                        onUpdateRandomReset = onUpdateRandomReset,
                        executeWithPinCheck = ::executeWithPinCheck
                    )
                }
            }

            // SECTION 2: SCHEDULES & FOCUS
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.SCHEDULES) {
                item {
                    SettingsSchedulesSection(
                        userPreferences = userPreferences,
                        onSetSleepSchedule = onSetSleepSchedule,
                        onSetWorkStudySchedule = onSetWorkStudySchedule,
                        onStartFocusSession = onStartFocusSession,
                        onCancelFocusSession = onCancelFocusSession,
                        onPauseProtection = onPauseProtection,
                        onResumeProtection = onResumeProtection,
                        executeWithPinCheck = ::executeWithPinCheck
                    )
                }
            }

            // SECTION 3: NOTIFICATIONS & REMINDERS
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.NOTIFICATIONS) {
                item {
                    SettingsNotificationsSection(
                        userPreferences = userPreferences,
                        onUpdateNotificationPreferences = onUpdateNotificationPreferences
                    )
                }
            }

            // SECTION 4: APPEARANCE & PERSONALIZATION
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.APPEARANCE) {
                item {
                    SettingsAppearanceSection(
                        userPreferences = userPreferences,
                        onUpdateAppearancePreferences = onUpdateAppearancePreferences
                    )
                }
            }

            // SECTION 5: PERMISSIONS & PROTECTION STATUS
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.PERMISSIONS) {
                item {
                    SettingsPermissionsSection(
                        health = health
                    )
                }
            }

            // SECTION 6: PRIVACY, DATA & ACCOUNT
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.PRIVACY) {
                item {
                    SettingsPrivacySection(
                        userPreferences = userPreferences,
                        onResetTodayUsage = onResetTodayUsage,
                        onClearAllData = onClearAllData,
                        onSetAppLockEnabled = onSetAppLockEnabled,
                        executeWithPinCheck = ::executeWithPinCheck
                    )
                }
            }

            // SECTION 7: ABOUT & SUPPORT
            if (selectedTab == SettingsCategoryTab.ALL || selectedTab == SettingsCategoryTab.ABOUT) {
                item {
                    SettingsAboutSection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Dialog 1: Set PIN Dialog
        if (showSetPinDialog) {
            SetPinDialog(
                onDismiss = { showSetPinDialog = false },
                onPinCreated = { pinHash: String, salt: String ->
                    onSetPin(pinHash, salt)
                    showSetPinDialog = false
                }
            )
        }

        // Dialog 2: Change PIN Dialog
        if (showChangePinDialog) {
            ChangePinDialog(
                userPreferences = userPreferences,
                onDismiss = { showChangePinDialog = false },
                onPinChanged = { pinHash, salt ->
                    onSetPin(pinHash, salt)
                    showChangePinDialog = false
                },
                onRecordFailedAttempt = onRecordFailedAttempt,
                onResetFailedAttempts = onResetFailedAttempts
            )
        }

        // Dialog 3: Remove PIN Dialog
        if (showRemovePinDialog) {
            RemovePinDialog(
                userPreferences = userPreferences,
                onDismiss = { showRemovePinDialog = false },
                onPinRemoved = {
                    onRemovePin()
                    showRemovePinDialog = false
                },
                onRecordFailedAttempt = onRecordFailedAttempt,
                onResetFailedAttempts = onResetFailedAttempts
            )
        }

        // Dialog 4: Verify PIN Dialog
        if (showVerifyPinDialog) {
            VerifyPinDialog(
                userPreferences = userPreferences,
                onDismiss = {
                    showVerifyPinDialog = false
                    pendingProtectedAction = null
                },
                onSuccess = {
                    showVerifyPinDialog = false
                    pendingProtectedAction?.invoke()
                    pendingProtectedAction = null
                },
                onRecordFailedAttempt = onRecordFailedAttempt,
                onResetFailedAttempts = onResetFailedAttempts
            )
        }
    }
}
