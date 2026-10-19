package com.example.monitoring

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.example.data.repository.BudgetRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.CapabilityState
import com.example.domain.model.EventClassification
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.MonitoringHealthInfo
import com.example.domain.model.ScrollObservation
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AccessibilityScrollMonitor(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val usageStatsRepository: UsageStatsRepository
) : ScrollMonitoringProvider {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _observationEvents = MutableSharedFlow<ScrollObservation>(extraBufferCapacity = 64)
    override val observationEvents = _observationEvents.asSharedFlow()

    private val lastScrollTimestamps = ConcurrentHashMap<String, Long>()
    private var lastRecordedTimestamp: Long? = null

    override val lastObservationTimestamp: Long?
        get() = lastRecordedTimestamp

    // Debounce window (ms) to treat rapid swipe deceleration as one user swipe
    private val debounceWindowMs = 500L

    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        onPerformHomeAction: () -> Unit
    ) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == context.packageName) return // Ignore ScrollLock itself

        scope.launch {
            val budget = budgetRepository.getBudget(packageName) ?: return@launch
            if (!budget.isEnabled) return@launch

            val eventType = event.eventType
            val now = System.currentTimeMillis()

            // 1. App Launch / Window change detection
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                budgetRepository.recordAppLaunch(packageName)
                checkExhaustionAndEnforce(packageName, onPerformHomeAction)
                return@launch
            }

            // 2. Scroll detection for apps that support scroll counting
            val isScrollType = eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_VIEW_SELECTED

            if (!isScrollType) return@launch

            if (!budget.scrollCountingSupported && !MonitoredAppInfo.isScrollTrackingSupportedFor(packageName)) {
                // This app does not support vertical video feed counting; only time/open limits apply
                checkExhaustionAndEnforce(packageName, onPerformHomeAction)
                return@launch
            }

            // False positive filter: ignore horizontal scroll events (carousel/story)
            val className = event.className?.toString() ?: ""
            if (className.contains("HorizontalScrollView", ignoreCase = true)) {
                return@launch
            }

            // False positive filter: ignore comment drawers or search bars
            if (className.contains("EditText", ignoreCase = true) ||
                className.contains("Comment", ignoreCase = true)
            ) {
                return@launch
            }

            // Apply debounce per package to prevent 1 swipe from counting 10 times
            val lastTime = lastScrollTimestamps[packageName] ?: 0L
            if (now - lastTime < debounceWindowMs) {
                return@launch
            }
            lastScrollTimestamps[packageName] = now
            lastRecordedTimestamp = now

            // Classify observation
            val classification = when (packageName) {
                MonitoredAppInfo.INSTAGRAM.packageName -> EventClassification.SWIPE_FEED_REEL
                MonitoredAppInfo.YOUTUBE.packageName -> EventClassification.SWIPE_FEED_SHORTS
                MonitoredAppInfo.TIKTOK.packageName -> EventClassification.SWIPE_FEED_TIKTOK
                else -> EventClassification.SCROLL_UNSPECIFIED
            }

            val observation = ScrollObservation(
                targetPackage = packageName,
                timestamp = now,
                eventClassification = classification,
                verificationStatus = VerificationStatus.VERIFIED,
                rawDeltaY = event.scrollY,
                sourceNodeClass = className.take(64)
            )

            budgetRepository.recordObservation(observation)
            _observationEvents.tryEmit(observation)
            checkExhaustionAndEnforce(packageName, onPerformHomeAction)
        }
    }

    private fun checkExhaustionAndEnforce(
        packageName: String,
        onPerformHomeAction: () -> Unit
    ) {
        scope.launch {
            com.example.ScrollLockApplication.enforcementManager?.enforceIfBudgetExhausted(
                packageName,
                onPerformHomeAction
            )
        }
    }

    // Direct simulation / spike testing hook
    fun simulateSpikeEvent(packageName: String, classification: EventClassification = EventClassification.MANUAL_SPIKE_TEST) {
        val now = System.currentTimeMillis()
        lastRecordedTimestamp = now
        val observation = ScrollObservation(
            targetPackage = packageName,
            timestamp = now,
            eventClassification = classification,
            verificationStatus = VerificationStatus.VERIFIED,
            rawDeltaY = 100,
            sourceNodeClass = "SpikeSimulationNode"
        )
        scope.launch {
            budgetRepository.recordObservation(observation)
            _observationEvents.tryEmit(observation)
        }
    }

    override fun getCapabilityState(context: Context): CapabilityState {
        val isA11y = isAccessibilityServiceEnabled(context)
        val isUsage = isUsageAccessEnabled(context)

        return when {
            !isA11y -> CapabilityState.PERMISSION_REQUIRED
            !isUsage -> CapabilityState.DEGRADED // Accessibility works, but screen time stats degraded
            else -> CapabilityState.AVAILABLE
        }
    }

    override fun getMonitoringHealthInfo(context: Context, observedCountToday: Int): MonitoringHealthInfo {
        val a11y = isAccessibilityServiceEnabled(context)
        val usage = isUsageAccessEnabled(context)
        val overlay = isOverlayPermissionEnabled(context)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                true
            }
        } else {
            true
        }

        val state = getCapabilityState(context)
        val diagnostic = when (state) {
            CapabilityState.NOT_CONFIGURED -> "Initial setup required."
            CapabilityState.PERMISSION_REQUIRED -> "Accessibility Service is not enabled. Limit enforcement is inactive."
            CapabilityState.DEGRADED -> "Enforcement is active. Usage Access is missing for screen-time limits."
            CapabilityState.AVAILABLE -> "All monitoring and enforcement systems operational."
            CapabilityState.UNSUPPORTED -> "Device does not support required accessibility features."
            CapabilityState.ERROR -> "Service encountered an unexpected error."
        }

        return MonitoringHealthInfo(
            capabilityState = state,
            usageAccessGranted = usage,
            accessibilityGranted = a11y,
            overlayGranted = overlay,
            notificationGranted = notification,
            lastEventTimestamp = lastRecordedTimestamp,
            diagnosticMessage = diagnostic,
            observedEventsToday = observedCountToday
        )
    }

    override fun getSupportedApps(): List<MonitoredAppInfo> {
        return MonitoredAppInfo.POPULAR_CATALOG
    }

    override fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val myService = ComponentName(context, ScrollAccessibilityService::class.java).flattenToString()
            enabledServices.contains(myService) || am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC
            ).any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
        } catch (_: Exception) {
            false
        }
    }

    override fun isUsageAccessEnabled(context: Context): Boolean {
        return usageStatsRepository.hasUsagePermission()
    }

    override fun isOverlayPermissionEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }
}
