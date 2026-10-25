package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferences
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsNotificationsSection(
    userPreferences: UserPreferences,
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
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_notifications"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget & Usage Alerts
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
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Budget & Usage Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text("Paced notifications to keep you conscious of digital habits", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NotificationToggleRow(
                    title = "80% Budget Warning",
                    subtitle = "Gentle heads-up alert when approaching limit exhaustion",
                    checked = userPreferences.alertAt80Percent,
                    onCheckedChange = { onUpdateNotificationPreferences(it, null, null, null, null, null, null, null, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                NotificationToggleRow(
                    title = "100% Limit Reached Alert",
                    subtitle = "Immediate notification when app restriction is triggered",
                    checked = userPreferences.alertAt100Percent,
                    onCheckedChange = { onUpdateNotificationPreferences(null, it, null, null, null, null, null, null, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                NotificationToggleRow(
                    title = "Evening Usage Summary",
                    subtitle = "Daily report at 9:00 PM showing scrolls saved vs yesterday",
                    checked = userPreferences.dailyUsageSummaryEnabled,
                    onCheckedChange = { onUpdateNotificationPreferences(null, null, it, null, null, null, null, null, null) }
                )
            }
        }

        // Break Reminder Card
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
                                .background(SecondaryViolet.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = SecondaryViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Take-a-Break Reminders", fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text(
                                if (userPreferences.breakReminderEnabled) "Every ${userPreferences.breakReminderMinutes} min continuous use" else "Disabled",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Switch(
                        checked = userPreferences.breakReminderEnabled,
                        onCheckedChange = { onUpdateNotificationPreferences(null, null, null, it, null, null, null, null, null) }
                    )
                }

                if (userPreferences.breakReminderEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Trigger interval for mindful pause suggestion:", color = SecondaryText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 20, 30, 45).forEach { mins ->
                            FilterChip(
                                selected = userPreferences.breakReminderMinutes == mins,
                                onClick = { onUpdateNotificationPreferences(null, null, null, true, mins, null, null, null, null) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        }

        // Streak & Quiet Hours
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
                NotificationToggleRow(
                    title = "Streak & Weekly Progress",
                    subtitle = "Celebrates goal streaks and milestone achievements",
                    checked = userPreferences.streakNotificationsEnabled,
                    onCheckedChange = { onUpdateNotificationPreferences(null, null, null, null, null, it, null, null, null) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WarningAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Quiet Hours", fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text(
                                if (userPreferences.quietHoursEnabled) "${userPreferences.quietHoursStart} - ${userPreferences.quietHoursEnd}" else "Disabled",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Switch(
                        checked = userPreferences.quietHoursEnabled,
                        onCheckedChange = { onUpdateNotificationPreferences(null, null, null, null, null, null, it, null, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = PrimaryText, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = SecondaryText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
