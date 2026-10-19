package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DailyUsageSummaryEntity
import com.example.data.repository.BudgetRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.MonitoredAppInfo
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingSuccess
import com.example.ui.theme.WellbeingWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    summaries: List<DailyUsageSummaryEntity>,
    scrollCountsMap: Map<String, Int>,
    screenTimesMap: Map<String, Long>,
    budgets: List<AppBudget>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chartMetric by remember { mutableStateOf("Scrolls") } // "Scrolls" vs "Minutes"

    val totalDaysRecorded = summaries.map { it.dateKey }.distinct().size
    val daysUnderBudget = summaries.filter { !it.budgetReached }.map { it.dateKey }.distinct().size
    val daysLimitReached = summaries.filter { it.budgetReached }.map { it.dateKey }.distinct().size

    val totalTodayScrolls = scrollCountsMap.values.sum()
    val totalTodayMinutes = (screenTimesMap.values.sum() / (60 * 1000L)).toInt()
    val totalScrollsRecorded = summaries.sumOf { it.verifiedScrollCount } + totalTodayScrolls
    val estimatedMinutesSaved = (daysUnderBudget * 25).coerceAtLeast(0)

    val limitReachedEvents = remember(summaries) {
        summaries.filter { it.budgetReached }.sortedByDescending { it.dateKey }
    }

    // Prepare 7-day bar chart data
    val past7Days = remember(summaries, scrollCountsMap, screenTimesMap, chartMetric) {
        val list = mutableListOf<DayChartBar>()
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateKey = keyFormat.format(dayCal.time)
            val dayLabel = if (i == 0) "Today" else dayFormat.format(dayCal.time)

            val isToday = i == 0
            val daySummaries = summaries.filter { it.dateKey == dateKey }
            val hadLimitReached = daySummaries.any { it.budgetReached }

            val value = if (isToday) {
                if (chartMetric == "Scrolls") totalTodayScrolls else totalTodayMinutes
            } else {
                if (chartMetric == "Scrolls") {
                    daySummaries.sumOf { it.verifiedScrollCount }
                } else {
                    (daySummaries.sumOf { it.screenTimeMillis } / 60000L).toInt()
                }
            }

            list.add(
                DayChartBar(
                    dayLabel = dayLabel,
                    dateKey = dateKey,
                    value = value,
                    hadLimitReached = hadLimitReached,
                    isToday = isToday
                )
            )
        }
        list
    }

    val maxChartValue = remember(past7Days) {
        past7Days.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("analytics_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Wellbeing Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cross-app usage & scroll discipline trends",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Summary Metric Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Days in Control",
                    value = "$daysUnderBudget",
                    subtitle = "Under all limits",
                    icon = Icons.Default.CheckCircle,
                    tint = WellbeingSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Interventions",
                    value = "$daysLimitReached",
                    subtitle = "Limits enforced",
                    icon = Icons.Default.Lock,
                    tint = if (daysLimitReached > 0) WellbeingRestriction else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Estimated Saved",
                    value = "~${estimatedMinutesSaved}m",
                    subtitle = "Mindful pauses",
                    icon = Icons.Default.Schedule,
                    tint = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Swipes",
                    value = "$totalScrollsRecorded",
                    subtitle = "Verified feed advances",
                    icon = Icons.Default.TrendingUp,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Day Usage Trend Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Column {
                            Text(
                                text = "7-Day Usage Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Daily distribution across monitored apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = chartMetric == "Scrolls",
                                onClick = { chartMetric = "Scrolls" },
                                label = { Text("Scrolls", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = chartMetric == "Minutes",
                                onClick = { chartMetric = "Minutes" },
                                label = { Text("Mins", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bars Container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        past7Days.forEach { bar ->
                            val heightFraction = (bar.value.toFloat() / maxChartValue.toFloat()).coerceIn(0.08f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (bar.value > 0) "${bar.value}" else "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (bar.hadLimitReached) WellbeingRestriction else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((100 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            when {
                                                bar.hadLimitReached -> WellbeingRestriction
                                                bar.isToday -> PrimaryBlue
                                                else -> PrimaryBlue.copy(alpha = 0.45f)
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = bar.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (bar.isToday) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regular Usage", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(WellbeingRestriction, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limit Reached", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Today's breakdown across all monitored apps
        item {
            Text(
                text = "Today's Monitored Allowances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (budgets.isEmpty()) {
                        Text(
                            text = "No apps monitored yet. Tap + on the Dashboard to select apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        budgets.forEach { budget ->
                            val scrollCount = scrollCountsMap[budget.packageName] ?: 0
                            val screenMinutes = ((screenTimesMap[budget.packageName] ?: 0L) / 60000L).toInt()

                            val isScroll = budget.scrollCountingSupported && budget.dailyLimit > 0
                            val currentMetric = if (isScroll) scrollCount else screenMinutes
                            val limitMetric = if (isScroll) budget.dailyLimit else budget.screenTimeLimitMinutes
                            val unit = if (isScroll) "scrolls" else "mins"

                            AppConsumptionRow(
                                name = budget.displayName,
                                count = currentMetric,
                                limit = limitMetric,
                                unit = unit,
                                isPaused = budget.isPaused
                            )
                        }
                    }
                }
            }
        }

        // Limit-Reached Intervention Events
        item {
            Text(
                text = "Limit-Reached Events & History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (summaries.isEmpty()) {
            item {
                EmptyAnalyticsState(
                    title = "Building Your Digital History",
                    message = "Every midnight, your daily app usage and scroll counts are logged here. Stay intentional today!"
                )
            }
        } else {
            items(summaries.take(15)) { summary ->
                val appName = MonitoredAppInfo.findByPackage(summary.targetPackage)?.displayName
                    ?: budgets.find { it.packageName == summary.targetPackage }?.displayName
                    ?: summary.targetPackage

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (summary.budgetReached) WellbeingRestriction.copy(alpha = 0.12f)
                                        else WellbeingSuccess.copy(alpha = 0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (summary.budgetReached) Icons.Default.Lock else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (summary.budgetReached) WellbeingRestriction else WellbeingSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = summary.dateKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${summary.verifiedScrollCount} scrolls • ${summary.screenTimeMillis / 60000}m",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.budgetReached) WellbeingRestriction else WellbeingSuccess
                            )
                            Text(
                                text = if (summary.budgetReached) "Intervention triggered" else "Under limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (summary.budgetReached) WellbeingRestriction else WellbeingSuccess
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class DayChartBar(
    val dayLabel: String,
    val dateKey: String,
    val value: Int,
    val hadLimitReached: Boolean,
    val isToday: Boolean
)

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppConsumptionRow(
    name: String,
    count: Int,
    limit: Int,
    unit: String,
    isPaused: Boolean
) {
    val effectiveLimit = limit.coerceAtLeast(1)
    val progress = (count.toFloat() / effectiveLimit.toFloat()).coerceIn(0f, 1f)
    val isOver = count >= limit && limit > 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isPaused) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Paused)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WellbeingWarning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (limit > 0) "$count / $limit $unit" else "$count $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOver) WellbeingRestriction else PrimaryBlue
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (limit > 0) progress else 0.5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isOver) WellbeingRestriction else PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
