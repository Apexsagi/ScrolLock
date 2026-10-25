package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsSchedulesSection(
    userPreferences: UserPreferences,
    onSetSleepSchedule: (enabled: Boolean, start: String, end: String) -> Unit,
    onSetWorkStudySchedule: (enabled: Boolean, start: String, end: String, days: String) -> Unit,
    onStartFocusSession: (durationMinutes: Int) -> Unit,
    onCancelFocusSession: () -> Unit,
    onPauseProtection: (durationMinutes: Int) -> Unit,
    onResumeProtection: () -> Unit,
    executeWithPinCheck: (requiresPin: Boolean, action: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_schedules"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Temporary Focus Session Card
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
                                .background(SecondaryViolet.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = SecondaryViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Temporary Focus Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (userPreferences.isFocusSessionActive) "Focus mode active" else "Instant distraction block",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (userPreferences.isFocusSessionActive) SuccessGreen else SecondaryText
                            )
                        }
                    }

                    if (userPreferences.isFocusSessionActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "RUNNING",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (userPreferences.isFocusSessionActive) {
                    val remainingMinutes = ((userPreferences.focusSessionActiveUntil - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SecondaryViolet.copy(alpha = 0.1f))
                            .border(1.dp, SecondaryViolet.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Deep Focus Active", fontWeight = FontWeight.Bold, color = PrimaryText)
                                Text("$remainingMinutes minutes remaining", fontSize = 12.sp, color = SecondaryViolet)
                            }
                            Button(
                                onClick = onCancelFocusSession,
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End Session", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Instantly locks all addictive social video feeds for a distraction-free work block.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 45, 60).forEach { mins ->
                            Button(
                                onClick = { onStartFocusSession(mins) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet.copy(alpha = 0.85f))
                            ) {
                                Text("${mins}m Focus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Pause Protection Card
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
                                .background(WarningAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pause Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (userPreferences.isProtectionPaused) "Limits currently paused" else "Temporarily disable blocking",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (userPreferences.isProtectionPaused) WarningAmber else SecondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (userPreferences.isProtectionPaused) {
                    val remainingMins = ((userPreferences.protectionPausedUntil - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paused: $remainingMins min left", color = WarningAmber, fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = onResumeProtection,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Resume Protection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { mins ->
                            OutlinedButton(
                                onClick = {
                                    executeWithPinCheck(userPreferences.pinRequireDisableMonitoring) {
                                        onPauseProtection(mins)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Pause ${mins}m", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Sleep Schedule Card
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
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sleep Schedule", fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text(
                                "${userPreferences.sleepScheduleStart} - ${userPreferences.sleepScheduleEnd}",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Switch(
                        checked = userPreferences.sleepScheduleEnabled,
                        onCheckedChange = { checked ->
                            onSetSleepSchedule(checked, userPreferences.sleepScheduleStart, userPreferences.sleepScheduleEnd)
                        }
                    )
                }

                if (userPreferences.sleepScheduleEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Automatically activates strict lock and silences feed alerts during your bedtime window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "22:00 - 06:00" to Pair("22:00", "06:00"),
                            "23:00 - 07:00" to Pair("23:00", "07:00"),
                            "00:00 - 08:00" to Pair("00:00", "08:00")
                        ).forEach { (label, range) ->
                            val isSelected = userPreferences.sleepScheduleStart == range.first
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetSleepSchedule(true, range.first, range.second) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Work & Study Hours Card
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
                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Work or Study Hours", fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text(
                                "${userPreferences.workStudyStart} - ${userPreferences.workStudyEnd} (${userPreferences.workStudyDays})",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Switch(
                        checked = userPreferences.workStudyScheduleEnabled,
                        onCheckedChange = { checked ->
                            onSetWorkStudySchedule(
                                checked,
                                userPreferences.workStudyStart,
                                userPreferences.workStudyEnd,
                                userPreferences.workStudyDays
                            )
                        }
                    )
                }

                if (userPreferences.workStudyScheduleEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Enforces 0-scroll limit during working hours on selected days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Day Selector Chips
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.forEach { day ->
                            val isDaySelected = userPreferences.workStudyDays.contains(day)
                            FilterChip(
                                selected = isDaySelected,
                                onClick = {
                                    val currentList = userPreferences.workStudyDays.split(",").filter { it.isNotBlank() }.toMutableList()
                                    if (isDaySelected) currentList.remove(day) else currentList.add(day)
                                    onSetWorkStudySchedule(
                                        true,
                                        userPreferences.workStudyStart,
                                        userPreferences.workStudyEnd,
                                        currentList.joinToString(",")
                                    )
                                },
                                label = { Text(day.take(1), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
