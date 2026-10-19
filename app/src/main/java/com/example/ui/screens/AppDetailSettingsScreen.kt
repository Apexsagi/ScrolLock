package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.SettingsSection
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning

@Composable
fun AppDetailSettingsScreen(
    budget: AppBudget,
    onSaveBudget: (AppBudget) -> Unit,
    onDeleteBudget: (packageName: String) -> Unit,
    onPauseApp: (packageName: String, minutes: Int) -> Unit,
    onResumeApp: (packageName: String) -> Unit,
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

    val screenTimePresets = listOf(15, 30, 45, 60, 90, 120)
    val scrollPresets = listOf(25, 50, 100, 150, 200)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("app_detail_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = budget.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = budget.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_app_budget_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove App", tint = WellbeingRestriction)
                }
            }
        }

        // Active Protection Toggle & Temporary Pause
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnabled) "Restrictions active" else "Monitoring paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
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
                                    text = "Currently paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WellbeingWarning,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = { onResumeApp(budget.packageName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WellbeingSuccess),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume Now", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onPauseApp(budget.packageName, 15) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pause 15m", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onPauseApp(budget.packageName, 60) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pause 1h", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Daily Screen Time Limit
        item {
            SettingsSection(title = "Daily Screen Time Limit") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (screenTimeMinutes > 0) "$screenTimeMinutes minutes / day" else "No time limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    screenTimePresets.forEach { preset ->
                        FilterChip(
                            selected = screenTimeMinutes == preset,
                            onClick = { screenTimeMinutes = preset },
                            label = { Text("${preset}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = screenTimeMinutes.toFloat(),
                    onValueChange = { screenTimeMinutes = it.toInt() },
                    valueRange = 0f..180f,
                    steps = 11
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Weekend limit override
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekend Limit (Sat / Sun)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (weekendMinutes > 0) "$weekendMinutes mins on weekends" else "Same as weekday",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = weekendMinutes == 0,
                            onClick = { weekendMinutes = 0 },
                            label = { Text("Same") }
                        )
                        FilterChip(
                            selected = weekendMinutes == 60,
                            onClick = { weekendMinutes = 60 },
                            label = { Text("60m") }
                        )
                        FilterChip(
                            selected = weekendMinutes == 90,
                            onClick = { weekendMinutes = 90 },
                            label = { Text("90m") }
                        )
                    }
                }
            }
        }

        // 2. Daily Scroll Quota Limit (if supported)
        item {
            SettingsSection(title = "Daily Feed Scroll Quota") {
                if (budget.scrollCountingSupported) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (scrollLimit > 0) "$scrollLimit vertical scrolls / day" else "No scroll limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scrollPresets.forEach { preset ->
                            FilterChip(
                                selected = scrollLimit == preset,
                                onClick = { scrollLimit = preset },
                                label = { Text("$preset") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = scrollLimit.toFloat(),
                        onValueChange = { scrollLimit = it.toInt() },
                        valueRange = 0f..250f,
                        steps = 24
                    )
                } else {
                    Text(
                        text = "Vertical feed scroll tracking is not applicable for this app. Use Screen Time & App Open limits above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Daily App Open Limit
        item {
            SettingsSection(title = "Daily App Launch Limit") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (launchLimit > 0) "$launchLimit opens / day" else "No launch limit",
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
                    listOf(0, 5, 10, 15, 20).forEach { preset ->
                        FilterChip(
                            selected = launchLimit == preset,
                            onClick = { launchLimit = preset },
                            label = { Text(if (preset == 0) "Off" else "$preset") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        // 4. Scheduled Downtime / Focus Window
        item {
            SettingsSection(title = "Scheduled Downtime Window") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restrict during specific hours",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isScheduleEnabled)
                                String.format("%02d:00 to %02d:00 (Focus mode)", scheduleStartHour, scheduleEndHour)
                            else "Downtime disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isScheduleEnabled,
                        onCheckedChange = { isScheduleEnabled = it }
                    )
                }

                if (isScheduleEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Work / Study hours preset: 9:00 AM – 5:00 PM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scheduleStartHour = 9
                                scheduleEndHour = 17
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("9 AM – 5 PM", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                scheduleStartHour = 22
                                scheduleEndHour = 7
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("10 PM – 7 AM", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Proactive Warning Alerts
        item {
            SettingsSection(title = "Proactive Warning Alerts") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Warning at 80% of limit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (warnAtEighty) "Heads-up alert sent when reaching 80% of scroll or time budget"
                            else "Only intervene when 100% limit is reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = warnAtEighty,
                        onCheckedChange = { warnAtEighty = it }
                    )
                }
            }
        }

        // 6. Lock Mode Selector
        item {
            SettingsSection(title = "Lock Mode") {
                listOf(LockMode.GENTLE, LockMode.STANDARD, LockMode.STRICT).forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { lockMode = mode }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lockMode == mode,
                            onClick = { lockMode = mode }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (lockMode == mode) FontWeight.Bold else FontWeight.Medium,
                                color = if (lockMode == mode) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Bottom Save Button
        item {
            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_app_detail_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Remove ${budget.displayName}?",
            text = "This will remove ${budget.displayName} from ScrollLock restrictions and stop tracking its limits.",
            confirmText = "Remove App",
            onConfirm = {
                onDeleteBudget(budget.packageName)
                showDeleteConfirm = false
                onNavigateBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
