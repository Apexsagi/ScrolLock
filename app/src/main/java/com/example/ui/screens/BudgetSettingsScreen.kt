package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.ui.components.ChangePinDialog
import com.example.ui.components.RemovePinDialog
import com.example.ui.components.SetPinDialog
import com.example.ui.components.SettingsSection
import com.example.ui.components.VerifyPinDialog
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning

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
    modifier: Modifier = Modifier
) {
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    // Protected action interceptor state
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("budget_settings_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Budget Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ==========================================
        // FEATURE 1: 4-DIGIT PIN SECURITY SECTION
        // ==========================================
        item {
            SettingsSection(title = "SECURITY") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (userPreferences.isPinSet) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (userPreferences.isPinSet) WellbeingSuccess else PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "🔒 App Protection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "PIN Status: ${if (userPreferences.isPinSet) "PIN Protected" else "Not Set"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (userPreferences.isPinSet) WellbeingSuccess else WellbeingWarning
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (userPreferences.isPinSet) WellbeingSuccess.copy(alpha = 0.15f) else WellbeingWarning.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (userPreferences.isPinSet) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = WellbeingSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (userPreferences.isPinSet) "PIN Protected" else "Not Set",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userPreferences.isPinSet) WellbeingSuccess else WellbeingWarning
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PIN Action Buttons
                        if (!userPreferences.isPinSet) {
                            Button(
                                onClick = { showSetPinDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("set_pin_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set PIN", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("change_pin_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Change PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { showRemovePinDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("remove_pin_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellbeingRestriction)
                                ) {
                                    Text("Remove PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Security Controls
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Security Controls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                SecurityControlRow(
                    title = "Require PIN to disable monitoring",
                    subtitle = "Protects against disabling monitoring or pausing restrictions",
                    isChecked = userPreferences.pinRequireDisableMonitoring,
                    isEnabled = userPreferences.isPinSet,
                    onCheckedChange = { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            onUpdateSecurityControl(checked, null, null, null, null)
                        }
                    },
                    testTag = "sec_control_disable_mon"
                )

                SecurityControlRow(
                    title = "Require PIN to change app limits",
                    subtitle = "Protects daily scroll quotas, screen time, and app launch budgets",
                    isChecked = userPreferences.pinRequireChangeLimits,
                    isEnabled = userPreferences.isPinSet,
                    onCheckedChange = { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            onUpdateSecurityControl(null, checked, null, null, null)
                        }
                    },
                    testTag = "sec_control_change_limits"
                )

                SecurityControlRow(
                    title = "Require PIN to remove restricted apps",
                    subtitle = "Prevents deleting or unselecting apps from the monitored list",
                    isChecked = userPreferences.pinRequireRemoveApps,
                    isEnabled = userPreferences.isPinSet,
                    onCheckedChange = { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            onUpdateSecurityControl(null, null, checked, null, null)
                        }
                    },
                    testTag = "sec_control_remove_apps"
                )

                SecurityControlRow(
                    title = "Require PIN to reset usage",
                    subtitle = "Protects today's usage statistics and database history erasure",
                    isChecked = userPreferences.pinRequireResetUsage,
                    isEnabled = userPreferences.isPinSet,
                    onCheckedChange = { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            onUpdateSecurityControl(null, null, null, checked, null)
                        }
                    },
                    testTag = "sec_control_reset_usage"
                )

                SecurityControlRow(
                    title = "Require PIN to change security settings",
                    subtitle = "Protects random reset delays and security toggle modifications",
                    isChecked = userPreferences.pinRequireChangeSecurity,
                    isEnabled = userPreferences.isPinSet,
                    onCheckedChange = { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            onUpdateSecurityControl(null, null, null, null, checked)
                        }
                    },
                    testTag = "sec_control_change_security"
                )
            }
        }

        // ==========================================
        // FEATURE 2: RANDOM RESET SYSTEM SECTION
        // ==========================================
        item {
            SettingsSection(title = "RANDOM RESET SYSTEM") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "🎲 Random Reset Protection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (userPreferences.randomResetEnabled) "Unpredictable lock duration" else "Fixed midnight reset",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = userPreferences.randomResetEnabled,
                                onCheckedChange = { enabled ->
                                    executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                        onUpdateRandomReset(
                                            enabled,
                                            userPreferences.randomResetMinHours,
                                            userPreferences.randomResetMaxHours
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("toggle_random_reset")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Prevents compulsive bingeing by generating a randomized delay each time an app's limit is exhausted, rather than letting you anticipate a fixed midnight reset.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        if (userPreferences.randomResetEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Current Range: ${userPreferences.randomResetMinHours}h to ${userPreferences.randomResetMaxHours}h (independent per lock)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Minimum Reset Delay Selector
                            Text(
                                text = "Minimum Reset Delay: ${userPreferences.randomResetMinHours} hours",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 2, 3, 4, 5, 6).forEach { h ->
                                    FilterChip(
                                        selected = userPreferences.randomResetMinHours == h,
                                        onClick = {
                                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                                onUpdateRandomReset(
                                                    userPreferences.randomResetEnabled,
                                                    h,
                                                    maxOf(h, userPreferences.randomResetMaxHours)
                                                )
                                            }
                                        },
                                        label = { Text("${h}h") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue,
                                            selectedLabelColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.testTag("random_min_${h}h")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Maximum Reset Delay Selector
                            Text(
                                text = "Maximum Reset Delay: ${userPreferences.randomResetMaxHours} hours",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3, 4, 5, 6, 8, 10).forEach { h ->
                                    FilterChip(
                                        selected = userPreferences.randomResetMaxHours == h,
                                        onClick = {
                                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                                onUpdateRandomReset(
                                                    userPreferences.randomResetEnabled,
                                                    minOf(userPreferences.randomResetMinHours, h),
                                                    h
                                                )
                                            }
                                        },
                                        label = { Text("${h}h") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue,
                                            selectedLabelColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.testTag("random_max_${h}h")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // MONITORED APPS CONFIGURATION SECTIONS
        // ==========================================
        items(budgets.size) { index ->
            val budget = budgets[index]
            AppBudgetConfigSection(
                budget = budget,
                onUpdateLimit = { newLimit ->
                    executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                        onUpdateLimit(budget.packageName, newLimit)
                    }
                },
                onUpdateLockMode = { newMode ->
                    executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                        onUpdateLockMode(budget.packageName, newMode)
                    }
                },
                onToggleEnabled = { enabled ->
                    // Protected if disabling monitoring
                    val requirePin = !enabled && userPreferences.pinRequireDisableMonitoring
                    executeWithPinCheck(requirePin) {
                        onToggleAppEnabled(budget.packageName, enabled)
                    }
                }
            )
        }

        // Global Safeguards & Preferences
        item {
            SettingsSection(title = "Safeguards & Reset Policy") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Mode Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enforces a 15-minute cool-down pause before allowing manual limit increases after exhaustion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.strictModeProtection,
                        onCheckedChange = { enabled ->
                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                onToggleStrictModeProtection(enabled)
                            }
                        },
                        modifier = Modifier.testTag("toggle_strict_mode")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Budget Exhaustion Alerts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Receive proactive push alerts when approaching or reaching quota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        modifier = Modifier.testTag("toggle_notifications")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Daily Reset Policy",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (userPreferences.randomResetEnabled) {
                                "Random Reset Protection is active. Monitored app restrictions generate an unpredictable cooldown between ${userPreferences.randomResetMinHours}h and ${userPreferences.randomResetMaxHours}h once limits are reached."
                            } else {
                                "Every day at 12:00 AM local midnight, observed usage and scroll counts reconcile into historical analytics and your allowance resets to full."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        executeWithPinCheck(userPreferences.pinRequireResetUsage) {
                            onResetTodayUsage()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reset_today_usage_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Text("Reset Today's Usage Allowance", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ==========================================
    // PIN SECURITY DIALOGS
    // ==========================================
    if (showSetPinDialog) {
        SetPinDialog(
            onPinCreated = { hash, salt ->
                onSetPin(hash, salt)
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            userPreferences = userPreferences,
            onPinChanged = { hash, salt ->
                onSetPin(hash, salt)
            },
            onRecordFailedAttempt = onRecordFailedAttempt,
            onResetFailedAttempts = onResetFailedAttempts,
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showRemovePinDialog) {
        RemovePinDialog(
            userPreferences = userPreferences,
            onPinRemoved = onRemovePin,
            onRecordFailedAttempt = onRecordFailedAttempt,
            onResetFailedAttempts = onResetFailedAttempts,
            onDismiss = { showRemovePinDialog = false }
        )
    }

    if (showVerifyPinDialog) {
        VerifyPinDialog(
            userPreferences = userPreferences,
            onSuccess = {
                val action = pendingProtectedAction
                pendingProtectedAction = null
                action?.invoke()
            },
            onDismiss = {
                showVerifyPinDialog = false
                pendingProtectedAction = null
            },
            onRecordFailedAttempt = onRecordFailedAttempt,
            onResetFailedAttempts = onResetFailedAttempts
        )
    }
}

@Composable
private fun SecurityControlRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AppBudgetConfigSection(
    budget: AppBudget,
    onUpdateLimit: (Int) -> Unit,
    onUpdateLockMode: (LockMode) -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val presets = listOf(25, 50, 100, 150, 200)

    SettingsSection(title = "${budget.displayName} (${budget.category})") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Enable Monitoring",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (budget.isEnabled) "Actively enforcing limits" else "Monitoring paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = budget.isEnabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("toggle_enabled_${budget.packageName}")
            )
        }

        if (budget.isEnabled) {
            Spacer(modifier = Modifier.height(14.dp))

            // Limits overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (budget.screenTimeLimitMinutes > 0) {
                    Text(
                        text = "Time Limit: ${budget.screenTimeLimitMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                if (budget.scrollCountingSupported && budget.dailyLimit > 0) {
                    Text(
                        text = "Scroll Quota: ${budget.dailyLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }

            if (budget.scrollCountingSupported) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Adjust Scroll Budget: ${budget.dailyLimit} scrolls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick preset pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = budget.dailyLimit == preset,
                            onClick = {
                                onUpdateLimit(preset)
                            },
                            label = { Text("$preset") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Lock & Enforcement Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LockModeOption(
                mode = LockMode.GENTLE,
                isSelected = budget.lockMode == LockMode.GENTLE,
                onSelect = { onUpdateLockMode(LockMode.GENTLE) }
            )

            LockModeOption(
                mode = LockMode.STANDARD,
                isSelected = budget.lockMode == LockMode.STANDARD,
                onSelect = { onUpdateLockMode(LockMode.STANDARD) }
            )

            LockModeOption(
                mode = LockMode.STRICT,
                isSelected = budget.lockMode == LockMode.STRICT,
                onSelect = { onUpdateLockMode(LockMode.STRICT) }
            )
        }
    }
}

@Composable
private fun LockModeOption(
    mode: LockMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
