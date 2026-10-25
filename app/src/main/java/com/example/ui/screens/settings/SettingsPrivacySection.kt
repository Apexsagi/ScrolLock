package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsPrivacySection(
    userPreferences: UserPreferences,
    onResetTodayUsage: () -> Unit,
    onClearAllData: () -> Unit,
    onSetAppLockEnabled: (Boolean) -> Unit,
    executeWithPinCheck: (requiresPin: Boolean, action: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_privacy"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Data Collection & On-Device Processing Card
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
                            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% On-Device Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text("Zero cloud tracking, zero personal data leaves your device", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ScreenLock operates completely locally using Android's native Accessibility and UsageStats APIs. No account registration, analytics telemetry, or server communication occurs. All your statistics are stored in your phone's encrypted Room database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    lineHeight = 18.sp
                )
            }
        }

        // 2. App Lock for ScreenLock itself
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
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Lock ScreenLock App", fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text("Require PIN to open ScreenLock settings and dashboard", fontSize = 12.sp, color = SecondaryText)
                        }
                    }

                    Switch(
                        checked = userPreferences.appLockEnabled,
                        onCheckedChange = { checked ->
                            executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                                onSetAppLockEnabled(checked)
                            }
                        }
                    )
                }
            }
        }

        // 3. Data Management Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Data Management", fontWeight = FontWeight.Bold, color = PrimaryText)

                // Export Personal Statistics Button
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Personal Statistics", fontWeight = FontWeight.SemiBold)
                }

                // Reset Today's Usage
                OutlinedButton(
                    onClick = {
                        executeWithPinCheck(userPreferences.pinRequireResetUsage) {
                            showClearHistoryDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Today's Usage History", fontWeight = FontWeight.SemiBold)
                }

                // Destructive Reset All Data
                OutlinedButton(
                    onClick = {
                        executeWithPinCheck(userPreferences.pinRequireChangeSecurity) {
                            showResetAllDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Reset All Data", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Dialog: Clear Today's History
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Today's History?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset today's scroll count, launches, and active screen times back to zero.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTodayUsage()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Today's usage history reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Reset Usage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Export Statistics
    if (showExportDialog) {
        val exportJson = """
            {
              "app": "ScreenLock",
              "exportedAt": "${System.currentTimeMillis()}",
              "deviceProtection": "ACTIVE",
              "settings": {
                "defaultScrollBudget": ${userPreferences.defaultDailyScrollBudget},
                "defaultTimeMinutes": ${userPreferences.defaultDailyTimeMinutes},
                "strictMode": ${userPreferences.strictModeProtection},
                "randomReset": ${userPreferences.randomResetEnabled}
              }
            }
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Personal Statistics Export", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your privacy-preserving JSON summary:", fontSize = 12.sp, color = SecondaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SecondaryBackground, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(exportJson, fontSize = 11.sp, color = PrimaryText, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("ScreenLock Export", exportJson))
                        showExportDialog = false
                        Toast.makeText(context, "Export copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Reset All Data
    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("Delete All Data & Reset?", fontWeight = FontWeight.Bold, color = DangerRed) },
            text = { Text("This is a permanent action. All customized budgets, PIN security credentials, historical audit logs, and preferences will be permanently wiped.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showResetAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Permanently Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
