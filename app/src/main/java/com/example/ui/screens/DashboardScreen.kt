package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.repository.BudgetRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.CapabilityState
import com.example.domain.model.MonitoringHealthInfo
import com.example.ui.ScreenDestination
import com.example.ui.components.MonitoringStatusBadge
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    budgets: List<AppBudget>,
    scrollCountsMap: Map<String, Int>,
    screenTimesMap: Map<String, Long>,
    launchesMap: Map<String, Int>,
    health: MonitoringHealthInfo,
    onNavigate: (ScreenDestination) -> Unit,
    onSelectAppDetail: (packageName: String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    val totalScreenTimeMinutes = screenTimesMap.values.sum() / (60 * 1000L)
    val totalScrolls = scrollCountsMap.values.sum()
    val totalLaunches = launchesMap.values.sum()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(ScreenDestination.APP_SELECTION) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add / Manage Apps") },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_apps")
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    Column {
                        Text(
                            text = "Today, $dateString",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ScrollLock",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("dashboard_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }

                        IconButton(
                            onClick = { onNavigate(ScreenDestination.BUDGET_SETTINGS) },
                            modifier = Modifier.testTag("dashboard_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }

            // Monitoring Health Banner (if permission is needed or degraded)
            if (health.capabilityState != CapabilityState.AVAILABLE) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_health_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (health.capabilityState == CapabilityState.PERMISSION_REQUIRED)
                                WellbeingRestriction.copy(alpha = 0.12f)
                            else WellbeingWarning.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = health.capabilityState.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (health.capabilityState == CapabilityState.PERMISSION_REQUIRED)
                                        WellbeingRestriction else WellbeingWarning
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = health.diagnosticMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onNavigate(ScreenDestination.MONITORING_HEALTH) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (health.capabilityState == CapabilityState.PERMISSION_REQUIRED)
                                        WellbeingRestriction else WellbeingWarning
                                )
                            ) {
                                Text("Fix", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Daily Summary Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Monitored Usage",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${totalScreenTimeMinutes}m Screen Time",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$totalScrolls scrolls • $totalLaunches app opens",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            ResetCountdownPill()
                        }
                    }
                }
            }

            // Section Header: Monitored Apps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restricted Apps (${budgets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = { onNavigate(ScreenDestination.APP_SELECTION) },
                        modifier = Modifier.testTag("dashboard_add_app_text_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add App", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Cards for each Monitored App
            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No apps selected yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add App' to pick Instagram, YouTube, Facebook, Snapchat, TikTok, or other apps on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigate(ScreenDestination.APP_SELECTION) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Select Apps Now")
                            }
                        }
                    }
                }
            } else {
                items(budgets, key = { it.packageName }) { budget ->
                    val scrollCount = scrollCountsMap[budget.packageName] ?: 0
                    val screenTimeMillis = screenTimesMap[budget.packageName] ?: 0L
                    val launches = launchesMap[budget.packageName] ?: 0

                    MultiAppBudgetCard(
                        budget = budget,
                        scrollCount = scrollCount,
                        screenTimeMillis = screenTimeMillis,
                        launchesCount = launches,
                        capabilityState = health.capabilityState,
                        onClick = { onSelectAppDetail(budget.packageName) }
                    )
                }
            }

            // Quick Navigation Tools Grid
            item {
                Text(
                    text = "Wellbeing Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickNavCard(
                        title = "Analytics",
                        subtitle = "History & trends",
                        icon = Icons.Default.BarChart,
                        onClick = { onNavigate(ScreenDestination.ANALYTICS) },
                        modifier = Modifier.weight(1f)
                    )

                    QuickNavCard(
                        title = "Feasibility Lab",
                        subtitle = "Test & validation",
                        icon = Icons.Default.Biotech,
                        onClick = { onNavigate(ScreenDestination.FEASIBILITY_SPIKE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickNavCard(
                        title = "Monitoring Health",
                        subtitle = "Inspect services",
                        icon = Icons.Default.HealthAndSafety,
                        onClick = { onNavigate(ScreenDestination.MONITORING_HEALTH) },
                        modifier = Modifier.weight(1f)
                    )

                    QuickNavCard(
                        title = "Privacy",
                        subtitle = "Local-only vault",
                        icon = Icons.Default.Shield,
                        onClick = { onNavigate(ScreenDestination.PRIVACY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp)) // Extra space for FAB
            }
        }
    }
}

@Composable
fun MultiAppBudgetCard(
    budget: AppBudget,
    scrollCount: Int,
    screenTimeMillis: Long,
    launchesCount: Int,
    capabilityState: CapabilityState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenTimeMinutes = (screenTimeMillis / (60 * 1000L)).toInt()
    val isTimeExhausted = budget.screenTimeLimitMinutes > 0 && screenTimeMinutes >= budget.screenTimeLimitMinutes
    val isScrollExhausted = budget.scrollCountingSupported && budget.dailyLimit > 0 && scrollCount >= budget.dailyLimit
    val isLaunchExhausted = budget.launchLimit > 0 && launchesCount >= budget.launchLimit
    val isAnyExhausted = isTimeExhausted || isScrollExhausted || isLaunchExhausted

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("app_card_${budget.packageName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Name, Category, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = budget.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = budget.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${budget.category} • Mode: ${budget.lockMode.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (budget.isPaused) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WellbeingWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = WellbeingWarning,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isAnyExhausted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WellbeingRestriction.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Limit Reached",
                            style = MaterialTheme.typography.labelSmall,
                            color = WellbeingRestriction,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    MonitoringStatusBadge(state = capabilityState)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Screen Time Metric
            if (budget.screenTimeLimitMinutes > 0) {
                val timeProgress = (screenTimeMinutes.toFloat() / budget.screenTimeLimitMinutes.toFloat()).coerceIn(0f, 1f)
                val remainingMins = maxOf(0, budget.screenTimeLimitMinutes - screenTimeMinutes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screen Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${screenTimeMinutes}m / ${budget.screenTimeLimitMinutes}m (${remainingMins}m left)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTimeExhausted) WellbeingRestriction else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { timeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isTimeExhausted) WellbeingRestriction else PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Scroll Count Metric (if supported)
            if (budget.scrollCountingSupported && budget.dailyLimit > 0) {
                val scrollProgress = (scrollCount.toFloat() / budget.dailyLimit.toFloat()).coerceIn(0f, 1f)
                val remainingScrolls = maxOf(0, budget.dailyLimit - scrollCount)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feed Scrolls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$scrollCount / ${budget.dailyLimit} ($remainingScrolls left)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScrollExhausted) WellbeingRestriction else PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { scrollProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isScrollExhausted) WellbeingRestriction else WellbeingSuccess,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Footer Row: Launch count / Schedules & Quick Edit action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (budget.launchLimit > 0) {
                    Text(
                        text = "$launchesCount / ${budget.launchLimit} opens today",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLaunchExhausted) WellbeingRestriction else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (budget.isScheduleEnabled) {
                    Text(
                        text = String.format("Downtime: %02d:00–%02d:00", budget.scheduleStartHour, budget.scheduleEndHour),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                } else {
                    Text(
                        text = "Resets tonight at midnight",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Configure", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
fun ResetCountdownPill(modifier: Modifier = Modifier) {
    var millisLeft by remember { mutableLongStateOf(BudgetRepository.getMillisUntilMidnight()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            millisLeft = BudgetRepository.getMillisUntilMidnight()
        }
    }

    val hours = (millisLeft / (1000 * 60 * 60)) % 24
    val minutes = (millisLeft / (1000 * 60)) % 60
    val seconds = (millisLeft / 1000) % 60
    val countdownFormatted = String.format("%02dh %02dm %02ds", hours, minutes, seconds)

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Resets in $countdownFormatted",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("quick_nav_${title.lowercase()}"),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
