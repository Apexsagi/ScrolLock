package com.example.domain.model

import android.graphics.drawable.Drawable

enum class LockMode(val label: String, val description: String) {
    GENTLE("Gentle", "Shows encouraging notifications and gentle warnings to help you exit the feed."),
    STANDARD("Standard", "Triggers immediate return-to-home and displays the full-screen Limit Reached barrier."),
    STRICT("Strict", "Enforces instant return-to-home with persistent cool-down and bypass prevention.")
}

enum class CapabilityState(val label: String) {
    NOT_CONFIGURED("Not Configured"),
    PERMISSION_REQUIRED("Permission Required"),
    AVAILABLE("Active & Monitoring"),
    DEGRADED("Degraded"),
    UNSUPPORTED("Unsupported"),
    ERROR("Error")
}

enum class VerificationStatus(val label: String) {
    VERIFIED("Verified"),
    ESTIMATED("Estimated"),
    UNAVAILABLE("Unavailable")
}

enum class LimitExhaustionReason(val title: String, val description: String) {
    SCROLL_LIMIT_REACHED("Scroll Quota Reached", "You have used your daily allowance of feed scrolls."),
    SCREEN_TIME_LIMIT_REACHED("Screen Time Exceeded", "You have reached your daily screen time quota."),
    LAUNCH_LIMIT_REACHED("App Opens Exceeded", "You have exceeded your daily allowed app launches."),
    SCHEDULED_DOWNTIME_ACTIVE("Scheduled Downtime", "This app is restricted during your scheduled focus hours.")
}

enum class EventClassification(val label: String) {
    SWIPE_FEED_REEL("Instagram Reel Scroll"),
    SWIPE_FEED_SHORTS("YouTube Short Scroll"),
    SWIPE_FEED_TIKTOK("TikTok Video Scroll"),
    SCROLL_UNSPECIFIED("Generic Feed Scroll"),
    APP_LAUNCH_OPEN("App Launch"),
    MANUAL_SPIKE_TEST("Spike Test Gesture")
}

data class MonitoredAppInfo(
    val packageName: String,
    val displayName: String,
    val shortName: String,
    val category: String = "Social",
    val isScrollTrackingSupported: Boolean = false,
    val isAvailable: Boolean = true
) {
    companion object {
        val INSTAGRAM = MonitoredAppInfo(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            shortName = "Instagram",
            category = "Social",
            isScrollTrackingSupported = true
        )
        val YOUTUBE = MonitoredAppInfo(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            shortName = "YouTube",
            category = "Video",
            isScrollTrackingSupported = true
        )
        val TIKTOK = MonitoredAppInfo(
            packageName = "com.zhiliaoapp.musically",
            displayName = "TikTok",
            shortName = "TikTok",
            category = "Video",
            isScrollTrackingSupported = true
        )
        val FACEBOOK = MonitoredAppInfo(
            packageName = "com.facebook.katana",
            displayName = "Facebook",
            shortName = "Facebook",
            category = "Social",
            isScrollTrackingSupported = false
        )
        val SNAPCHAT = MonitoredAppInfo(
            packageName = "com.snapchat.android",
            displayName = "Snapchat",
            shortName = "Snapchat",
            category = "Social",
            isScrollTrackingSupported = false
        )
        val REDDIT = MonitoredAppInfo(
            packageName = "com.reddit.frontpage",
            displayName = "Reddit",
            shortName = "Reddit",
            category = "Social",
            isScrollTrackingSupported = false
        )
        val X_TWITTER = MonitoredAppInfo(
            packageName = "com.twitter.android",
            displayName = "X (Twitter)",
            shortName = "X",
            category = "Social",
            isScrollTrackingSupported = false
        )
        val TWITCH = MonitoredAppInfo(
            packageName = "tv.twitch.android.app",
            displayName = "Twitch",
            shortName = "Twitch",
            category = "Video",
            isScrollTrackingSupported = false
        )

        val POPULAR_CATALOG = listOf(
            INSTAGRAM,
            YOUTUBE,
            TIKTOK,
            FACEBOOK,
            SNAPCHAT,
            REDDIT,
            X_TWITTER,
            TWITCH
        )

        fun isScrollTrackingSupportedFor(packageName: String): Boolean {
            return packageName == INSTAGRAM.packageName ||
                    packageName == YOUTUBE.packageName ||
                    packageName == TIKTOK.packageName ||
                    packageName == "com.ss.android.ugc.trill"
        }

        fun findByPackage(pkg: String): MonitoredAppInfo? {
            return POPULAR_CATALOG.find { it.packageName == pkg }
        }
    }
}

data class DiscoveredApp(
    val packageName: String,
    val appName: String,
    val category: String,
    val isInstalled: Boolean,
    val isScrollTrackingSupported: Boolean,
    val currentUsageMillis: Long = 0L,
    val iconDrawable: Drawable? = null
)

data class AppBudget(
    val packageName: String,
    val displayName: String,
    val dailyLimit: Int = 50, // Scrolls/video advances limit (0 = disabled)
    val screenTimeLimitMinutes: Int = 45, // Minutes (0 = disabled)
    val launchLimit: Int = 0, // App open limit (0 = disabled)
    val scrollCountingSupported: Boolean = true,
    val weekendScreenTimeLimitMinutes: Int = 0, // 0 = same as weekday
    val scheduleStartHour: Int = 0,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 0,
    val scheduleEndMinute: Int = 0,
    val isScheduleEnabled: Boolean = false,
    val warnAtEightyPercent: Boolean = true,
    val isPaused: Boolean = false,
    val pauseUntilTimestamp: Long = 0L,
    val category: String = "Social",
    val isEnabled: Boolean = true,
    val lockMode: LockMode = LockMode.STANDARD,
    val resetHour: Int = 0,
    val resetMinute: Int = 0,
    val lockResetTimestamp: Long = 0L
) {
    val isCurrentlyLocked: Boolean
        get() = lockResetTimestamp > System.currentTimeMillis()
}

data class ScrollObservation(
    val id: Long = 0,
    val targetPackage: String,
    val timestamp: Long,
    val eventClassification: EventClassification,
    val verificationStatus: VerificationStatus,
    val rawDeltaY: Int = 0,
    val sourceNodeClass: String = ""
)

data class DailyUsageSummary(
    val id: Long = 0,
    val dateKey: String, // YYYY-MM-DD
    val targetPackage: String,
    val verifiedScrollCount: Int,
    val screenTimeMillis: Long = 0L,
    val launchesCount: Int = 0,
    val budgetReached: Boolean = false,
    val budgetLimitAtExhaustion: Int = 0,
    val monitoringCompleteness: String = "COMPLETE"
)

data class MonitoringHealthInfo(
    val capabilityState: CapabilityState,
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
    val overlayGranted: Boolean,
    val notificationGranted: Boolean,
    val lastEventTimestamp: Long?,
    val diagnosticMessage: String,
    val observedEventsToday: Int
)

data class SpikeSpikeRecord(
    val id: Long = 0,
    val timestamp: Long,
    val scenario: String,
    val expectedSwipes: Int,
    val detectedEvents: Int,
    val falsePositives: Int,
    val deduplicatedEvents: Int,
    val precisionPercent: Int,
    val recallPercent: Int,
    val notes: String
)
