package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.ui.components.AppleAppIcon
import com.example.ui.components.AppleCard
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.VerifyPinDialog
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MainBackground
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun AppDetailSettingsScreen(
    budget: AppBudget,
    userPreferences: UserPreferences = UserPreferences(),
    onSaveBudget: (AppBudget) -> Unit,
    onDeleteBudget: (packageName: String) -> Unit,
    onPauseApp: (packageName: String, minutes: Int) -> Unit,
    onResumeApp: (packageName: String) -> Unit,
    onResetUsage: (packageName: String) -> Unit = {},
    onRecordFailedAttempt: () -> Unit = {},
    onResetFailedAttempts: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screenTimeMinutes by remember(budget) { mutableIntStateOf(budget.screenTimeLimitMinutes) }
    var scrollLimit by remember(budget) { mutableIntStateOf(budget.dailyLimit) }
    var launchLimit by remember(budget) { mutableIntStateOf(budget.launchLimit) }
    var weekendMinutes by remember(budget) { mutableIntStateOf(budget.weekendScreenTimeLimitMinutes) }
    var isScheduleEnabled by remember(budget) { mutableStateOf(budget.isScheduleEnabled) }
    var scheduleStartHour by remember(budget) { mutableIntStateOf(budget.scheduleStartHour) }
    var scheduleEndHour by remember(budget) { mutableIntStateOf(budget.scheduleEndHour) }
    var warnAtEighty by remember(budget) { mutableStateOf(budget.warnAtEightyPercent) }
    var lockMode by remember(budget) { mutableStateOf(budget.lockMode) }
    var isEnabled by remember(budget) { mutableStateOf(budget.isEnabled) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // PIN Protection state
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

    val screenTimePresets = listOf(15, 30, 45, 60, 90, 120)
    val scrollPresets = listOf(25, 50, 100, 150, 200)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Apple Top Bar: Navigation Back + Delete Action
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateBack)
                        .testTag("app_detail_back_button")
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dashboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedCard)
                        .clickable { showDeleteConfirm = true }
                        .testTag("delete_app_budget_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove App",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // App Hero Header: Large Apple Icon, Title & Category
        item {
            AppleCard(shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleAppIcon(
                        packageName = budget.packageName,
                        displayName = budget.displayName,
                        size = 56.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = budget.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${budget.category} • ${budget.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            }
        }

        // Quick Actions: Launch on Phone & Reset Usage / Unblock
        item {
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(budget.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            val url = when {
                                budget.packageName.contains("youtube") -> "https://www.youtube.com"
                                budget.packageName.contains("instagram") -> "https://www.instagram.com"
                                else -> "market://details?id=${budget.packageName}"
                            }
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open ${budget.displayName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("launch_app_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open App",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open on Phone", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        executeWithPinCheck(userPreferences.pinRequireChangeLimits) {
                            onResetUsage(budget.packageName)
                            Toast.makeText(context, "Today's usage & locks reset for ${budget.displayName}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("reset_app_usage_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Usage",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Usage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                }
            }
        }

        // Monitoring State & Pause Control
        item {
            AppleCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Enforce Restrictions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (isEnabled) "Active & Monitoring" else "Monitoring Paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnabled) SuccessGreen else MutedText
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                if (!checked && userPreferences.pinRequireDisableMonitoring) {
                                    executeWithPinCheck(true) {
                                        isEnabled = false
                                    }
                                } else {
                                    isEnabled = checked
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen
                            ),
                            modifier = Modifier.testTag("toggle_app_active")
                        )
                    }

                    if (isEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        if (budget.isPaused) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Currently paused until timer expires",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = { onResumeApp(budget.packageName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Mindful Pause",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 15, 30, 60).forEach { mins ->
                                    OutlinedButton(
                                        onClick = {
                                            executeWithPinCheck(userPreferences.pinRequireDisableMonitoring) {
                                                onPauseApp(budget.packageName, mins)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("${mins}m", fontSize = 12.sp, color = PrimaryText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Screen Time Limit
        item {
            AppleCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Screen Time Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = if (screenTimeMinutes > 0) "${screenTimeMinutes}m" else "Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = screenTimeMinutes.toFloat(),
                        onValueChange = { screenTimeMinutes = (it / 5).toInt() * 5 },
                        valueRange = 0f..180f,
                        steps = 35,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = ElevatedCard
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(screenTimePresets) { preset ->
                            FilterChip(
                                selected = screenTimeMinutes == preset,
                                onClick = { screenTimeMinutes = preset },
                                label = { Text("${preset}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Feed Scroll Limit (if supported)
        if (budget.scrollCountingSupported) {
            item {
                AppleCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Feed Scroll Limit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (scrollLimit > 0) "$scrollLimit scrolls" else "Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = scrollLimit.toFloat(),
                            onValueChange = { scrollLimit = (it / 10).toInt() * 10 },
                            valueRange = 0f..250f,
                            steps = 24,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue,
                                inactiveTrackColor = ElevatedCard
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(scrollPresets) { preset ->
                                FilterChip(
                                    selected = scrollLimit == preset,
                                    onClick = { scrollLimit = preset },
                                    label = { Text("$preset") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily App-Open Limit
        item {
            AppleCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily App-Open Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = if (launchLimit > 0) "$launchLimit opens" else "No limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 15, 25, 40).forEach { limit ->
                            FilterChip(
                                selected = launchLimit == limit,
                                onClick = { launchLimit = limit },
                                label = { Text(if (limit == 0) "Off" else "$limit") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Enforcement Mode
        item {
            AppleCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Enforcement Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LockMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { lockMode = mode }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lockMode == mode,
                                onClick = { lockMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (lockMode == mode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (lockMode == mode) PrimaryBlue else PrimaryText
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scheduled Downtime
        item {
            AppleCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quiet Hours / Scheduled Downtime",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "Lock this app during designated hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }

                        Switch(
                            checked = isScheduleEnabled,
                            onCheckedChange = { isScheduleEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }

                    if (isScheduleEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("From %02d:00 to %02d:00", scheduleStartHour, scheduleEndHour),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Buttons: Save Changes & Restore Defaults
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val limitsChanged = scrollLimit != budget.dailyLimit ||
                                screenTimeMinutes != budget.screenTimeLimitMinutes ||
                                launchLimit != budget.launchLimit ||
                                weekendMinutes != budget.weekendScreenTimeLimitMinutes

                        val disablingMonitoring = !isEnabled && budget.isEnabled

                        val requirePin = (limitsChanged && userPreferences.pinRequireChangeLimits) ||
                                (disablingMonitoring && userPreferences.pinRequireDisableMonitoring)

                        executeWithPinCheck(requirePin) {
                            val updated = budget.copy(
                                dailyLimit = scrollLimit,
                                screenTimeLimitMinutes = screenTimeMinutes,
                                launchLimit = launchLimit,
                                weekendScreenTimeLimitMinutes = weekendMinutes,
                                isScheduleEnabled = isScheduleEnabled,
                                scheduleStartHour = scheduleStartHour,
                                scheduleEndHour = scheduleEndHour,
                                warnAtEightyPercent = warnAtEighty,
                                lockMode = lockMode,
                                isEnabled = isEnabled
                            )
                            onSaveBudget(updated)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_app_detail_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        screenTimeMinutes = 45
                        scrollLimit = if (budget.scrollCountingSupported) 50 else 0
                        launchLimit = 0
                        lockMode = LockMode.STANDARD
                        isScheduleEnabled = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_defaults_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Defaults", color = SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Remove ${budget.displayName}?",
            text = "This will remove ${budget.displayName} from ScrollLock restrictions and stop tracking its limits.",
            confirmText = "Remove App",
            onConfirm = {
                showDeleteConfirm = false
                executeWithPinCheck(userPreferences.pinRequireRemoveApps) {
                    onDeleteBudget(budget.packageName)
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
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
