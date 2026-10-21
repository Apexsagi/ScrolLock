package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.BudgetRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.CapabilityState
import com.example.domain.model.MonitoringHealthInfo
import com.example.ui.ScreenDestination
import com.example.ui.components.AppleAppIcon
import com.example.ui.components.AppleCard
import com.example.ui.components.AppleCircularProgress
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
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
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
    val totalScreenTimeMinutes = screenTimesMap.values.sum() / (60 * 1000L)
    val totalScrolls = scrollCountsMap.values.sum()
    val totalLaunches = launchesMap.values.sum()

    // Calculate aggregate limits for progress ring
    val totalLimitMinutes = budgets.filter { it.isEnabled }.sumOf { it.screenTimeLimitMinutes }
    val overallTimeProgress = if (totalLimitMinutes > 0) {
        (totalScreenTimeMinutes.toFloat() / totalLimitMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Clean, Compact Apple Header
            item {
                AppleHeader(
                    onRefresh = onRefresh,
                    onOpenSettings = { onNavigate(ScreenDestination.BUDGET_SETTINGS) }
                )
            }

            // B. Compact, Elegant Permission Banner (shown only when required)
            if (health.capabilityState != CapabilityState.AVAILABLE) {
                item {
                    AppleHealthBanner(
                        health = health,
                        onFixClick = { onNavigate(ScreenDestination.MONITORING_HEALTH) }
                    )
                }
            }

            // C. Today's Screen Time Usage Summary (Hero Card)
            item {
                AppleTodayUsageSummary(
                    screenTimeMinutes = totalScreenTimeMinutes,
                    totalScrolls = totalScrolls,
                    totalLaunches = totalLaunches,
                    progress = overallTimeProgress
                )
            }

            // D. Restricted Apps Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Restricted Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${budgets.size} Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = { onNavigate(ScreenDestination.APP_SELECTION) },
                        modifier = Modifier.testTag("dashboard_add_app_text_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add App", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                    }
                }
            }

            // E. Restricted App Cards
            if (budgets.isEmpty()) {
                item {
                    AppleEmptyRestrictedApps(onAddApps = { onNavigate(ScreenDestination.APP_SELECTION) })
                }
            } else {
                items(budgets, key = { it.packageName }) { budget ->
                    val scrollCount = scrollCountsMap[budget.packageName] ?: 0
                    val screenTimeMillis = screenTimesMap[budget.packageName] ?: 0L
                    val launches = launchesMap[budget.packageName] ?: 0

                    AppleRestrictedAppCard(
                        budget = budget,
                        scrollCount = scrollCount,
                        screenTimeMillis = screenTimeMillis,
                        launchesCount = launches,
                        capabilityState = health.capabilityState,
                        onClick = { onSelectAppDetail(budget.packageName) }
                    )
                }

                // Inline secondary action card to add more apps
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .clickable { onNavigate(ScreenDestination.APP_SELECTION) },
                        color = ElevatedCard.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add More Apps to Restrict",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // F. Wellbeing Tools Section Header
            item {
                Text(
                    text = "Wellbeing Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleQuickNavCard(
                        title = "Analytics",
                        subtitle = "History & trends",
                        icon = Icons.Default.BarChart,
                        onClick = { onNavigate(ScreenDestination.ANALYTICS) },
                        modifier = Modifier.weight(1f)
                    )

                    AppleQuickNavCard(
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
                    AppleQuickNavCard(
                        title = "Monitoring Health",
                        subtitle = "Inspect services",
                        icon = Icons.Default.HealthAndSafety,
                        onClick = { onNavigate(ScreenDestination.MONITORING_HEALTH) },
                        modifier = Modifier.weight(1f)
                    )

                    AppleQuickNavCard(
                        title = "Privacy Vault",
                        subtitle = "Local-only storage",
                        icon = Icons.Default.Shield,
                        onClick = { onNavigate(ScreenDestination.PRIVACY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom clearance spacer: allows scrolling cards completely past the FAB!
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Floating Action Button: Pinned to bottom end with clear bottom padding
        ExtendedFloatingActionButton(
            onClick = { onNavigate(ScreenDestination.APP_SELECTION) },
            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            text = { Text("Add / Manage Apps", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .testTag("fab_add_apps")
        )
    }
}

/**
 * A. Clean, Compact Apple Header
 */
@Composable
private fun AppleHeader(
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryBlue, SecondaryViolet)
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "ScreenLock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    fontSize = 20.sp
                )
                Text(
                    text = "Focus more. Live better.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElevatedCard)
                    .border(1.dp, BorderColor.copy(alpha = 0.7f), CircleShape)
                    .clickable(onClick = onRefresh)
                    .testTag("dashboard_refresh_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Data",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElevatedCard)
                    .border(1.dp, BorderColor.copy(alpha = 0.7f), CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .testTag("dashboard_settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * B. Compact, Elegant Permission Warning Banner
 */
@Composable
private fun AppleHealthBanner(
    health: MonitoringHealthInfo,
    onFixClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_health_banner"),
        shape = RoundedCornerShape(16.dp),
        color = DangerRed.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Enable Accessibility to activate app restrictions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onFixClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * C. Today's Screen Time Usage Summary (Hero Card)
 */
@Composable
private fun AppleTodayUsageSummary(
    screenTimeMinutes: Long,
    totalScrolls: Int,
    totalLaunches: Int,
    progress: Float
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            ElevatedCard,
            CardBackground,
            SecondaryBackground
        )
    )

    AppleCard(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                // Top Row: Title + Countdown badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S SCREEN TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )

                    AppleResetCountdownBadge()
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Middle Row: Big time value + Circular progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val hours = screenTimeMinutes / 60
                        val mins = screenTimeMinutes % 60
                        val displayTime = when {
                            hours > 0 -> "${hours}h ${mins}m"
                            else -> "${mins}m"
                        }

                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            fontSize = 36.sp
                        )

                        Text(
                            text = if (screenTimeMinutes > 0) "Monitored across your restricted apps" else "No usage recorded yet today",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    AppleCircularProgress(
                        progress = progress,
                        size = 64.dp,
                        strokeWidth = 6.5.dp,
                        primaryColor = if (progress >= 1f) DangerRed else PrimaryBlue,
                        trackColor = BorderColor.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Divider & Stats Pills
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderColor.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppleStatPill(
                        label = "Feed Scrolls",
                        value = "$totalScrolls",
                        icon = Icons.Default.SwapVert,
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )

                    AppleStatPill(
                        label = "App Opens",
                        value = "$totalLaunches",
                        icon = Icons.Default.TouchApp,
                        tint = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleStatPill(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ElevatedCard.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    fontSize = 14.sp
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AppleResetCountdownBadge(modifier: Modifier = Modifier) {
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
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedCard)
            .border(0.5.dp, BorderColor.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Resets in $countdownFormatted",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = SecondaryText,
                fontSize = 10.5.sp
            )
        }
    }
}

/**
 * D. Restricted App Card
 */
@Composable
fun AppleRestrictedAppCard(
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

    val statusBadgeText = when {
        !budget.isEnabled -> "Disabled"
        budget.isPaused -> "Paused"
        isAnyExhausted -> "Limit Reached"
        capabilityState == CapabilityState.PERMISSION_REQUIRED -> "Permission Needed"
        capabilityState == CapabilityState.DEGRADED -> "Degraded"
        capabilityState != CapabilityState.AVAILABLE -> "Unavailable"
        else -> "Active"
    }

    val statusColor = when {
        !budget.isEnabled -> MutedText
        budget.isPaused -> WarningAmber
        isAnyExhausted -> DangerRed
        capabilityState == CapabilityState.PERMISSION_REQUIRED -> DangerRed
        capabilityState == CapabilityState.DEGRADED -> WarningAmber
        capabilityState != CapabilityState.AVAILABLE -> MutedText
        else -> SuccessGreen
    }

    AppleCard(
        modifier = modifier.testTag("budget_card_${budget.packageName}"),
        onClick = onClick,
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Top Row: App Icon + Name + Category + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleAppIcon(
                        packageName = budget.packageName,
                        displayName = budget.displayName,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = budget.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "${budget.category} • ${budget.lockMode.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Compact Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Middle Row: Screen Time Metric & Thin Progress Bar
            if (budget.screenTimeLimitMinutes > 0) {
                val timeProgress = (screenTimeMinutes.toFloat() / budget.screenTimeLimitMinutes.toFloat()).coerceIn(0f, 1f)
                val remainingMins = maxOf(0, budget.screenTimeLimitMinutes - screenTimeMinutes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screen Time: ${screenTimeMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "${budget.screenTimeLimitMinutes}m limit • ${remainingMins}m left",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = if (isTimeExhausted) DangerRed else SecondaryText,
                        fontSize = 11.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { timeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isTimeExhausted) DangerRed else PrimaryBlue,
                    trackColor = ElevatedCard
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
                        text = "Feed Scrolls: $scrollCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "${budget.dailyLimit} limit • $remainingScrolls left",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isScrollExhausted) DangerRed else SecondaryText,
                        fontSize = 11.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { scrollProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isScrollExhausted) DangerRed else SuccessGreen,
                    trackColor = ElevatedCard
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Bottom Row: App opens or schedule + "Configure" action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (budget.launchLimit > 0) {
                    Text(
                        text = "$launchesCount / ${budget.launchLimit} opens today",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLaunchExhausted) DangerRed else SecondaryText,
                        fontSize = 11.sp
                    )
                } else if (budget.lockResetTimestamp > System.currentTimeMillis()) {
                    val sdf = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                    val unlockTime = sdf.format(Date(budget.lockResetTimestamp))
                    Text(
                        text = "🔒 Unlocks at $unlockTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Resets tonight at midnight",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Configure",
                        fontSize = 12.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleEmptyRestrictedApps(onAddApps: () -> Unit) {
    AppleCard(
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Restricted Apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select apps like Instagram, YouTube, TikTok, Facebook, or Reddit to start monitoring screen time and enforcing mindful limits.",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddApps,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Apps Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AppleQuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier.testTag("quick_nav_${title.lowercase()}"),
        onClick = onClick,
        backgroundColor = CardBackground,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                fontSize = 14.sp
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}
