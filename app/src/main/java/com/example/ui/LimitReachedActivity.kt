package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.ScrollLockApplication
import com.example.data.repository.BudgetRepository
import com.example.domain.model.LimitExhaustionReason
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ScrollLockTheme
import com.example.ui.theme.WellbeingRestriction
import com.example.ui.theme.WellbeingWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LimitReachedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.instagram.android"
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Selected App"
        val reasonName = intent.getStringExtra(EXTRA_REASON_NAME) ?: LimitExhaustionReason.SCROLL_LIMIT_REACHED.name
        val currentMetric = intent.getStringExtra(EXTRA_CURRENT_METRIC) ?: "Limit reached"
        val limitMetric = intent.getStringExtra(EXTRA_LIMIT_METRIC) ?: "Budget exhausted"
        val isStrict = intent.getBooleanExtra(EXTRA_IS_STRICT, false)
        val lockResetTimestamp = intent.getLongExtra(EXTRA_LOCK_RESET_TIMESTAMP, 0L)

        val reason = try {
            LimitExhaustionReason.valueOf(reasonName)
        } catch (_: Exception) {
            LimitExhaustionReason.SCROLL_LIMIT_REACHED
        }

        setContent {
            ScrollLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LimitReachedContent(
                        appName = appName,
                        reason = reason,
                        currentMetric = currentMetric,
                        limitMetric = limitMetric,
                        isStrict = isStrict,
                        lockResetTimestamp = lockResetTimestamp,
                        onReturnHome = {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        },
                        onReviewProgress = {
                            val mainIntent = Intent(this@LimitReachedActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("NAV_TARGET", "DASHBOARD")
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onMindfulPause = {
                            lifecycleScope.launch {
                                ScrollLockApplication.instance.budgetRepository.pauseApp(packageName, 5)
                                Toast.makeText(
                                    this@LimitReachedActivity,
                                    "Restrictions paused for 5 minutes. Take your time.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                        },
                        onEmergencyAccess = {
                            val dialIntent = Intent(Intent.ACTION_DIAL)
                            if (dialIntent.resolveActivity(packageManager) != null) {
                                startActivity(dialIntent)
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_REASON_NAME = "extra_reason_name"
        const val EXTRA_CURRENT_METRIC = "extra_current_metric"
        const val EXTRA_LIMIT_METRIC = "extra_limit_metric"
        const val EXTRA_IS_STRICT = "extra_is_strict"
        const val EXTRA_DAILY_LIMIT = "extra_daily_limit"
        const val EXTRA_OBSERVED_COUNT = "extra_observed_count"
        const val EXTRA_LOCK_RESET_TIMESTAMP = "extra_lock_reset_timestamp"
    }
}

@Composable
fun LimitReachedContent(
    appName: String,
    reason: LimitExhaustionReason,
    currentMetric: String,
    limitMetric: String,
    isStrict: Boolean,
    lockResetTimestamp: Long = 0L,
    onReturnHome: () -> Unit,
    onReviewProgress: () -> Unit,
    onMindfulPause: () -> Unit,
    onEmergencyAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Large Padlock / Hourglass Icon
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(WellbeingRestriction.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reason == LimitExhaustionReason.SCREEN_TIME_LIMIT_REACHED)
                        Icons.Default.HourglassTop else Icons.Default.Lock,
                    contentDescription = "Limit Reached",
                    tint = WellbeingRestriction,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = reason.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${reason.description} $appName is currently restricted to support your digital wellbeing.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Metric Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Configured Limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = limitMetric,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Observed Usage",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentMetric,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WellbeingRestriction
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LimitCountdownBox(lockResetTimestamp = lockResetTimestamp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Calming encouragement text
            Text(
                text = "✨ Mindful tip: Take a breath, stretch, drink water, or connect with someone in real life.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("limit_reached_home_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Return to Home Screen", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onReviewProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("limit_reached_progress_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Limits in ScrollLock", fontSize = 15.sp)
            }

            // Mindful 5-minute pause (only available if not strict mode)
            if (!isStrict) {
                OutlinedButton(
                    onClick = onMindfulPause,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("limit_reached_pause_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PauseCircle, contentDescription = null, tint = WellbeingWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("5-Minute Mindful Pause", color = WellbeingWarning, fontSize = 14.sp)
                }
            }

            TextButton(
                onClick = onEmergencyAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("limit_reached_emergency_button")
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Emergency Access / Phone",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun LimitCountdownBox(
    lockResetTimestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    val isRandomReset = lockResetTimestamp > 0L
    fun computeRemaining(): Long {
        return if (isRandomReset) {
            maxOf(0L, lockResetTimestamp - System.currentTimeMillis())
        } else {
            BudgetRepository.getMillisUntilMidnight()
        }
    }

    var millisLeft by remember(lockResetTimestamp) { mutableLongStateOf(computeRemaining()) }

    LaunchedEffect(lockResetTimestamp) {
        while (true) {
            millisLeft = computeRemaining()
            if (isRandomReset && millisLeft <= 0L) {
                break
            }
            delay(1000L)
        }
    }

    val hours = (millisLeft / (1000 * 60 * 60))
    val minutes = (millisLeft / (1000 * 60)) % 60
    val seconds = (millisLeft / 1000) % 60
    val countdownFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val unlockTimeStr = remember(lockResetTimestamp) {
        if (isRandomReset) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(lockResetTimestamp))
        } else {
            "local midnight"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isRandomReset) "Random Reset Active" else "Limits reset in",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isRandomReset && millisLeft <= 0L) "Allowance Reset" else countdownFormatted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = if (isRandomReset) "Resets at $unlockTimeStr" else "Resets at 12:00 AM (midnight)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
