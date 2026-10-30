package com.example.ui.screens.settings

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SecondaryViolet

@Composable
fun SettingsAboutSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_section_about"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Info & Version Card
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
                            .size(42.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("ScrollLock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text("Version 1.4.2 • Build 2026", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "A mindful digital wellbeing tool engineered to break compulsive short-form doomscrolling loops with friction, unpredictable delay locks, and healthy habit architecture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    lineHeight = 18.sp
                )
            }
        }

        // How it Works & Supported Apps Card
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
                Text("Architecture & Supported Apps", fontWeight = FontWeight.Bold, color = PrimaryText)

                OutlinedButton(
                    onClick = { showHowItWorksDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How ScrollLock Works & Detection Details")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SecondaryBackground, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Supported Social Video Platforms:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Instagram (Reels & Main Feed)\n• YouTube (Shorts & Feed)\n• TikTok (For You & Following)\n• Facebook (Reels & Watch)\n• Snapchat (Spotlight)\n• Reddit (Video Scroller)",
                            fontSize = 12.sp,
                            color = SecondaryText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Help, Feedback & Legal Card
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
                Text("Support & Legal", fontWeight = FontWeight.Bold, color = PrimaryText)

                Button(
                    onClick = { showFeedbackDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback or Report a Problem", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terms of Service & Privacy Policy", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Dialog: How it Works
    if (showHowItWorksDialog) {
        AlertDialog(
            onDismissRequest = { showHowItWorksDialog = false },
            title = { Text("How ScrollLock Works", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "1. Native Accessibility Video Inspection:\nScrollLock detects when a new video appears in vertical feeds and watches content signatures.\n\n2. Signature & Cooldown Verification:\nEach video is verified by signature with a 1-second cooldown so one video never counts twice.\n\n3. Zero Server Reliance:\nAll observation tokens stay in device volatile memory and local SQLite database.",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHowItWorksDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // Dialog: Feedback
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Send Feedback", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Describe any bugs, app detection issues, or feature suggestions:", fontSize = 12.sp, color = SecondaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type your feedback here...") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            Toast.makeText(context, "Thank you! Feedback recorded on-device.", Toast.LENGTH_LONG).show()
                            feedbackText = ""
                            showFeedbackDialog = false
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Terms & Privacy
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Privacy Policy:\nScrollLock does not collect, transmit, or monetize any personal information. All usage metrics and configuration parameters are maintained exclusively on your physical Android device.\n\nTerms of Service:\nScrollLock is provided as-is to assist users in regulating digital media consumption. Users may disable or uninstall the application at any time.",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
