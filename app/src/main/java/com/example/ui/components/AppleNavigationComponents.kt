package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenDestination
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramGradientEnd
import com.example.ui.theme.InstagramGradientMiddle
import com.example.ui.theme.InstagramGradientStart
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RedditOrange
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SnapchatYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.YouTubeRed

/**
 * Premium Apple-style Bottom Navigation Bar
 * 4 Destinations: Home, Stats, Rules, Profile
 */
@Composable
fun ScreenLockBottomBar(
    currentDestination: ScreenDestination?,
    onNavigate: (ScreenDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 1.dp,
                color = BorderColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = SecondaryBackground.copy(alpha = 0.96f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                selected = currentDestination == ScreenDestination.DASHBOARD,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onNavigate(ScreenDestination.DASHBOARD) },
                testTag = "nav_home"
            )

            BottomNavItem(
                label = "Stats",
                selected = currentDestination == ScreenDestination.ANALYTICS,
                selectedIcon = Icons.Filled.BarChart,
                unselectedIcon = Icons.Outlined.BarChart,
                onClick = { onNavigate(ScreenDestination.ANALYTICS) },
                testTag = "nav_stats"
            )

            BottomNavItem(
                label = "Rules",
                selected = currentDestination == ScreenDestination.BUDGET_SETTINGS,
                selectedIcon = Icons.Filled.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                onClick = { onNavigate(ScreenDestination.BUDGET_SETTINGS) },
                testTag = "nav_rules"
            )

            BottomNavItem(
                label = "Profile",
                selected = currentDestination == ScreenDestination.PRIVACY,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                onClick = { onNavigate(ScreenDestination.PRIVACY) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) PrimaryBlue else MutedText,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = PrimaryBlue.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(scale)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (selected) {
                        Modifier
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.16f))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = animatedColor,
            letterSpacing = 0.1.sp
        )
    }
}

/**
 * Authentic Apple-style rounded squircle App Icon
 * Accurately styles major apps (Instagram, YouTube, Facebook, TikTok, Snapchat, Reddit)
 * and falls back to polished colored glyphs.
 */
@Composable
fun AppleAppIcon(
    packageName: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val pkg = packageName.lowercase()

    val (brush, content) = remember(packageName, displayName) {
        when {
            pkg.contains("instagram") -> {
                val b = Brush.linearGradient(
                    listOf(InstagramGradientStart, InstagramGradientMiddle, InstagramGradientEnd)
                )
                Pair(b, "IG")
            }
            pkg.contains("youtube") -> {
                val b = Brush.linearGradient(listOf(YouTubeRed, Color(0xFFCC0000)))
                Pair(b, "YT")
            }
            pkg.contains("facebook") -> {
                val b = Brush.linearGradient(listOf(FacebookBlue, Color(0xFF0F5BBE)))
                Pair(b, "f")
            }
            pkg.contains("tiktok") -> {
                val b = Brush.linearGradient(listOf(Color(0xFF010101), TikTokCyan))
                Pair(b, "TT")
            }
            pkg.contains("snapchat") -> {
                val b = Brush.linearGradient(listOf(SnapchatYellow, Color(0xFFF0EC00)))
                Pair(b, "SC")
            }
            pkg.contains("reddit") -> {
                val b = Brush.linearGradient(listOf(RedditOrange, Color(0xFFD63600)))
                Pair(b, "RD")
            }
            else -> {
                val char = displayName.firstOrNull()?.uppercase() ?: "A"
                val b = Brush.linearGradient(listOf(ElevatedCard, PrimaryBlue.copy(alpha = 0.6f)))
                Pair(b, char)
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (pkg.contains("youtube")) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Text(
                text = content,
                color = if (pkg.contains("snapchat")) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (content.length > 1) (size.value * 0.35f).sp else (size.value * 0.45f).sp
            )
        }
    }
}

/**
 * Premium Apple-style Circular Progress Ring
 */
@Composable
fun AppleCircularProgress(
    progress: Float,
    size: Dp = 68.dp,
    strokeWidth: Dp = 7.dp,
    primaryColor: Color = PrimaryBlue,
    trackColor: Color = BorderColor.copy(alpha = 0.5f),
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "circular_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Background track
            drawCircle(
                color = trackColor,
                radius = (this.size.minDimension - strokeWidth.toPx()) / 2,
                style = stroke
            )
            // Progress arc
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke
            )
        }
        centerContent()
    }
}

/**
 * Apple-style Card container with subtle border, rounded corners, and depth.
 */
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = CardBackground,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, BorderColor.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
    } else {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, BorderColor.copy(alpha = 0.7f), shape)
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}
