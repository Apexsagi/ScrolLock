package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.ui.components.AppleAppIcon
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramGradientStart
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RedditOrange
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SnapchatYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning
import com.example.ui.theme.YouTubeRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsLimitsSection(
    budgets: List<AppBudget>,
    userPreferences: UserPreferences,
    onUpdateLimit: (packageName: String, limit: Int) -> Unit,
    onUpdateLockMode: (packageName: String, lockMode: LockMode) -> Unit,
    onToggleAppEnabled: (packageName: String, isEnabled: Boolean) -> Unit,
    onToggleStrictModeProtection: (Boolean) -> Unit,
    onSetDefaultLimits: (scrollBudget: Int, timeMinutes: Int) -> Unit,
    onSetLimitReachedAction: (action: String) -> Unit,
    onSetEmergencyGraceSettings: (graceMinutes: Int, overrideAvailable: Boolean) -> Unit,
    onOpenSetPin: () -> Unit,
    onOpenChangePin: () -> Unit,
    onOpenRemovePin: () -> Unit,
    onUpdateSecurityControl: (
        disableMon: Boolean?,
        changeLimits: Boolean?,
        removeApps: Boolean?,
        resetUsage: Boolean?,
        changeSecurity: Boolean?
    ) -> Unit,
    onUpdateRandomReset: (enabled: Boolean, minHours: Int, maxHours: Int) -> Unit,
    executeWithPinCheck: (requiresPin: Boolean, action: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_limits"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Default Daily Budgets Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Default Daily Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Baseline limits automatically applied to newly monitored apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scroll Budget Slider / Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Feed Scroll Budget", style = MaterialTheme.typography.bodyMedium, color = PrimaryText)
                    Text(
                        "${userPreferences.defaultDailyScrollBudget} scrolls",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 150, 200).forEach { scrolls ->
                        FilterChip(
                            selected = userPreferences.defaultDailyScrollBudget == scrolls,
                            onClick = {
                                executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                    onSetDefaultLimits(scrolls, userPreferences.defaultDailyTimeMinutes)
                                }
                            },
                            label = { Text("$scrolls") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Screen Time Budget Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Screen Time", style = MaterialTheme.typography.bodyMedium, color = PrimaryText)
                    Text(
                        "${userPreferences.defaultDailyTimeMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryViolet
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = userPreferences.defaultDailyTimeMinutes == mins,
                            onClick = {
                                executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                    onSetDefaultLimits(userPreferences.defaultDailyScrollBudget, mins)
                                }
                            },
                            label = { Text("${mins}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecondaryViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 2. Per-App Limits
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Per-App Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "Configure custom scroll quotas and lock enforcement for each app",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                budgets.forEach { budget ->
                    AppLimitSettingRow(
                        budget = budget,
                        userPreferences = userPreferences,
                        onUpdateLimit = onUpdateLimit,
                        onUpdateLockMode = onUpdateLockMode,
                        onToggleAppEnabled = onToggleAppEnabled,
                        executeWithPinCheck = executeWithPinCheck
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // 3. What happens when a limit is reached
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "When a Limit is Reached",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "Select what action ScreenLock triggers immediately upon limit exhaustion",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                val actions = listOf(
                    Triple("BLOCK_OVERLAY", "Full Screen Block Overlay", "Displays an impassable lock screen overlay"),
                    Triple("GRAYSCALE", "Gentle Pause Warning", "Shows mindful pause prompt with 10s breathing timer"),
                    Triple("NOTIFICATION", "Alert Notification Only", "Sends a high-priority heads-up reminder"),
                    Triple("CLOSE_APP", "Immediate App Closure", "Sends device to home screen automatically")
                )

                actions.forEach { (actionKey, title, desc) ->
                    val isSelected = userPreferences.limitReachedAction == actionKey
                    val borderColor = if (isSelected) PrimaryBlue else BorderColor
                    val bg = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else SecondaryBackground

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                    onSetLimitReachedAction(actionKey)
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(2.dp, if (isSelected) PrimaryBlue else SecondaryText, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(PrimaryBlue, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, color = PrimaryText, fontSize = 14.sp)
                                Text(desc, color = SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 4. Strict Mode & Emergency Override Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Mode Protection", fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text(
                            "Locks down settings and requires countdown cooldown to change or bypass limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    Switch(
                        checked = userPreferences.strictModeProtection,
                        onCheckedChange = { checked ->
                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                onToggleStrictModeProtection(checked)
                            }
                        },
                        modifier = Modifier.testTag("strict_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emergency Grace Period", fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text(
                            "Allow a single 5-minute unlock per day for urgent communication",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    Switch(
                        checked = userPreferences.emergencyOverrideAvailable,
                        onCheckedChange = { checked ->
                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                onSetEmergencyGraceSettings(5, checked)
                            }
                        }
                    )
                }
            }
        }

        // 5. PIN Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                                text = "4-Digit PIN Security",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (userPreferences.isPinSet) "Protected with custom PIN" else "No PIN configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (userPreferences.isPinSet) SuccessGreen else WarningAmber
                            )
                        }
                    }

                    if (userPreferences.isPinSet) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!userPreferences.isPinSet) {
                    Button(
                        onClick = onOpenSetPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("set_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set 4-Digit PIN", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenChangePin,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("change_pin_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Change PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onOpenRemovePin,
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

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Protected Actions", fontWeight = FontWeight.Bold, color = PrimaryText, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    SecurityCheckboxRow("Disable Monitoring", userPreferences.pinRequireDisableMonitoring, "sec_control_disable_mon") { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) { onUpdateSecurityControl(checked, null, null, null, null) }
                    }
                    SecurityCheckboxRow("Change Limits", userPreferences.pinRequireChangeLimits, "sec_control_change_limits") { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) { onUpdateSecurityControl(null, checked, null, null, null) }
                    }
                    SecurityCheckboxRow("Remove Apps", userPreferences.pinRequireRemoveApps, "sec_control_remove_apps") { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) { onUpdateSecurityControl(null, null, checked, null, null) }
                    }
                    SecurityCheckboxRow("Reset Usage", userPreferences.pinRequireResetUsage, "sec_control_reset_usage") { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) { onUpdateSecurityControl(null, null, null, checked, null) }
                    }
                    SecurityCheckboxRow("Change Security Settings", userPreferences.pinRequireChangeSecurity, "sec_control_change_security") { checked ->
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) { onUpdateSecurityControl(null, null, null, null, checked) }
                    }
                }
            }
        }

        // 6. Random Reset System Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🎲 Random Reset Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (userPreferences.randomResetEnabled) "Unpredictable lock intervals" else "Fixed midnight reset",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }

                    Switch(
                        checked = userPreferences.randomResetEnabled,
                        onCheckedChange = { enabled ->
                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                onUpdateRandomReset(enabled, userPreferences.randomResetMinHours, userPreferences.randomResetMaxHours)
                            }
                        },
                        modifier = Modifier.testTag("toggle_random_reset")
                    )
                }

                if (userPreferences.randomResetEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Prevents anticipation bingeing by generating randomized lock release delays (${userPreferences.randomResetMinHours}h - ${userPreferences.randomResetMaxHours}h).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { h ->
                            FilterChip(
                                selected = userPreferences.randomResetMinHours == h,
                                onClick = {
                                    executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                        onUpdateRandomReset(true, h, maxOf(h, userPreferences.randomResetMaxHours))
                                    }
                                },
                                label = { Text("${h}h") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCheckboxRow(
    title: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = SecondaryText, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AppLimitSettingRow(
    budget: AppBudget,
    userPreferences: UserPreferences,
    onUpdateLimit: (packageName: String, limit: Int) -> Unit,
    onUpdateLockMode: (packageName: String, lockMode: LockMode) -> Unit,
    onToggleAppEnabled: (packageName: String, isEnabled: Boolean) -> Unit,
    executeWithPinCheck: (requiresPin: Boolean, action: () -> Unit) -> Unit
) {
    var customInputText by remember(budget.dailyLimit) {
        mutableStateOf(if (budget.dailyLimit !in listOf(25, 50, 100)) budget.dailyLimit.toString() else "")
    }
    var inputError by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SecondaryBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        val isNarrow = maxWidth < 310.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header row: App icon on left, Name & summary in middle, Switch on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleAppIcon(
                        packageName = budget.packageName,
                        displayName = budget.displayName,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = budget.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (budget.isEnabled) "${budget.dailyLimit} scrolls/day • ${budget.screenTimeLimitMinutes}m limit" else "Monitoring Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (budget.isEnabled) SecondaryText else MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = budget.isEnabled,
                    onCheckedChange = { enabled ->
                        executeWithPinCheck(userPreferences.pinRequireDisableMonitoring) {
                            onToggleAppEnabled(budget.packageName, enabled)
                        }
                    },
                    modifier = Modifier
                        .testTag("toggle_app_${budget.packageName}")
                        .minimumInteractiveComponentSize()
                )
            }

            // Controls active only when app is enabled
            if (budget.isEnabled) {
                // 2. Budget selection row: Three equal-width preset buttons (25, 50, 100 scrolls)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preset Daily Limits",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(25, 50, 100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = budget.dailyLimit == preset
                            val btnBg = if (isSelected) PrimaryBlue else ElevatedCard
                            val btnBorder = if (isSelected) PrimaryBlue else BorderColor
                            val textColor = if (isSelected) Color.White else PrimaryText

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        customInputText = ""
                                        inputError = false
                                        executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                            onUpdateLimit(budget.packageName, preset)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = btnBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 6.dp)
                                ) {
                                    if (isNarrow) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$preset",
                                                color = textColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "scrolls",
                                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else SecondaryText,
                                                fontSize = 10.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "$preset scrolls",
                                            color = textColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Custom budget row: Label and full-width numeric input placed below presets
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom daily limit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryText
                        )
                        if (budget.dailyLimit !in listOf(25, 50, 100)) {
                            Text(
                                text = "Active: ${budget.dailyLimit} scrolls",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customInputText,
                            onValueChange = { newVal ->
                                val digitsOnly = newVal.filter { it.isDigit() }
                                customInputText = digitsOnly
                                inputError = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("custom_scroll_input_${budget.packageName}"),
                            placeholder = {
                                Text(
                                    text = "e.g. 150 scrolls",
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            suffix = if (customInputText.isNotEmpty()) {
                                {
                                    Text(
                                        text = "scrolls",
                                        color = SecondaryText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else null,
                            singleLine = true,
                            isError = inputError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val parsed = customInputText.toIntOrNull()
                                    if (parsed != null && parsed > 0) {
                                        executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                            onUpdateLimit(budget.packageName, parsed)
                                        }
                                    } else {
                                        inputError = true
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedCard,
                                unfocusedContainerColor = ElevatedCard,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                errorBorderColor = DangerRed
                            )
                        )

                        Button(
                            onClick = {
                                val parsed = customInputText.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                                        onUpdateLimit(budget.packageName, parsed)
                                    }
                                } else {
                                    inputError = true
                                }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("apply_custom_scroll_${budget.packageName}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (inputError) {
                        Text(
                            text = "Please enter a positive number of scrolls (e.g. 150)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}
