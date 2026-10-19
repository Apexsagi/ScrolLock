package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CapabilityState
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.MonitoringHealthInfo
import com.example.ui.components.PermissionStatusRow
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning

@Composable
fun OnboardingScreen(
    monitoringHealth: MonitoringHealthInfo,
    onRefreshHealth: () -> Unit,
    onSaveInitialBudgets: (igLimit: Int, ytLimit: Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 4
    val context = LocalContext.current

    var igSelected by remember { mutableStateOf(true) }
    var ytSelected by remember { mutableStateOf(true) }
    var igLimit by remember { mutableIntStateOf(50) }
    var ytLimit by remember { mutableIntStateOf(100) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Bar with back button and progress step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(
                        onClick = { step-- },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = "Step $step of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )

                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content Switcher
            AnimatedContent(targetState = step, label = "onboarding_step_content") { currentStep ->
                when (currentStep) {
                    1 -> StepTheProblem()
                    2 -> StepSelectApps(
                        igSelected = igSelected,
                        onToggleIg = { igSelected = it },
                        ytSelected = ytSelected,
                        onToggleYt = { ytSelected = it }
                    )
                    3 -> StepSetBudgets(
                        igLimit = igLimit,
                        onIgLimitChange = { igLimit = it },
                        ytLimit = ytLimit,
                        onYtLimitChange = { ytLimit = it }
                    )
                    4 -> StepPermissionsCheck(
                        context = context,
                        health = monitoringHealth,
                        onRefreshHealth = onRefreshHealth
                    )
                }
            }
        }

        // Navigation CTA buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Button(
                onClick = {
                    if (step < totalSteps) {
                        step++
                    } else {
                        onSaveInitialBudgets(igLimit, ytLimit)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = if (step < totalSteps) "Continue" else "Finish Setup & Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StepTheProblem() {
    Column {
        Text(
            text = "Why Scroll Limits?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Short-form video feeds are engineered to remove stopping cues. Traditional app timers only track minutes, not the rapid dopamine cycle of the swipe.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ComparisonRow(
                    label = "Time Limits",
                    value = "You can watch 30 videos in 10 minutes without realizing the extent of exposure.",
                    isPositive = false
                )
                Spacer(modifier = Modifier.height(16.dp))
                ComparisonRow(
                    label = "Scroll Budgets",
                    value = "Every feed swipe counts. A clear budget of 50 scrolls creates an intentional physical stopping point.",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
private fun StepSelectApps(
    igSelected: Boolean,
    onToggleIg: (Boolean) -> Unit,
    ytSelected: Boolean,
    onToggleYt: (Boolean) -> Unit
) {
    var ttSelected by remember { mutableStateOf(true) }
    var fbSelected by remember { mutableStateOf(false) }
    var snapSelected by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Select Apps to Restrict",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ScrollLock supports Instagram, YouTube, TikTok, Facebook, Snapchat, and any other installed app on your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        AppToggleCard(
            title = "Instagram",
            packageId = "com.instagram.android",
            isSelected = igSelected,
            onToggle = onToggleIg
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppToggleCard(
            title = "YouTube",
            packageId = "com.google.android.youtube",
            isSelected = ytSelected,
            onToggle = onToggleYt
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppToggleCard(
            title = "TikTok",
            packageId = "com.zhiliaoapp.musically",
            isSelected = ttSelected,
            onToggle = { ttSelected = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppToggleCard(
            title = "Facebook",
            packageId = "com.facebook.katana",
            isSelected = fbSelected,
            onToggle = { fbSelected = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppToggleCard(
            title = "Snapchat",
            packageId = "com.snapchat.android",
            isSelected = snapSelected,
            onToggle = { snapSelected = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "✨ You can search and add any other installed game or entertainment app from your dashboard anytime.",
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AppToggleCard(
    title: String,
    packageId: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = packageId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun StepSetBudgets(
    igLimit: Int,
    onIgLimitChange: (Int) -> Unit,
    ytLimit: Int,
    onYtLimitChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Set Initial Budgets",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Decide your daily swipe allowance. You can fine-tune this anytime in settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        BudgetSliderCard(
            title = "Instagram Reels",
            limit = igLimit,
            onLimitChange = onIgLimitChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        BudgetSliderCard(
            title = "YouTube Shorts",
            limit = ytLimit,
            onLimitChange = onYtLimitChange
        )
    }
}

@Composable
private fun BudgetSliderCard(
    title: String,
    limit: Int,
    onLimitChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$limit scrolls / day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = limit.toFloat(),
                onValueChange = { onLimitChange(it.toInt()) },
                valueRange = 10f..250f,
                steps = 23
            )
        }
    }
}

@Composable
private fun StepPermissionsCheck(
    context: Context,
    health: MonitoringHealthInfo,
    onRefreshHealth: () -> Unit
) {
    Column {
        Text(
            text = "Permissions & Monitoring",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Android strictly isolates apps. To observe reels scrolls and enforce limits, ScrollLock relies on user-authorized system capabilities.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        PermissionStatusRow(
            title = "Accessibility Service",
            description = "Required to observe vertical feed scrolls and perform home-return on limit reach.",
            isGranted = health.accessibilityGranted,
            onGrantClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionStatusRow(
            title = "Usage Access",
            description = "Allows querying foreground screen time to show comprehensive usage insights.",
            isGranted = health.usageAccessGranted,
            onGrantClick = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Honest Capability Status Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (health.capabilityState) {
                    CapabilityState.AVAILABLE -> WellbeingSuccess.copy(alpha = 0.1f)
                    CapabilityState.DEGRADED -> WellbeingWarning.copy(alpha = 0.1f)
                    else -> WellbeingRestriction.copy(alpha = 0.1f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Status: ${health.capabilityState.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (health.capabilityState) {
                            CapabilityState.AVAILABLE -> WellbeingSuccess
                            CapabilityState.DEGRADED -> WellbeingWarning
                            else -> WellbeingRestriction
                        }
                    )

                    IconButton(
                        onClick = onRefreshHealth,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = health.diagnosticMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: String, isPositive: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (isPositive) WellbeingSuccess.copy(alpha = 0.15f)
                    else WellbeingRestriction.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = if (isPositive) WellbeingSuccess else WellbeingRestriction,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
