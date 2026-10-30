package com.example.monitoring

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.repository.BudgetRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.CapabilityState
import com.example.domain.model.EventClassification
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.MonitoringHealthInfo
import com.example.domain.model.ScrollObservation
import com.example.domain.model.VerificationStatus
import com.example.domain.model.VideoDetectionDebugLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Video Signature Detection State
    private val currentVideoSignature = ConcurrentHashMap<String, String>()
    private val seenVideoSignatures = ConcurrentHashMap<String, MutableSet<String>>()
    private val lastCountedTimestamp = ConcurrentHashMap<String, Long>()
    private val lastLaunchTimestamps = ConcurrentHashMap<String, Long>()
    private val trackingApproximateMap = ConcurrentHashMap<String, Boolean>()

    private val _debugLogs = MutableStateFlow<List<VideoDetectionDebugLogEntry>>(emptyList())
    override val debugLogs: StateFlow<List<VideoDetectionDebugLogEntry>> = _debugLogs.asStateFlow()

    private var currentForegroundPackage: String? = null
    private var lastRecordedTimestamp: Long? = null

    override val lastObservationTimestamp: Long?
        get() = lastRecordedTimestamp

    // Cooldown window (ms) to ensure one swipe/transition never counts twice
    private val cooldownWindowMs = 1000L
    // Debounce window (ms) for counting separate app launches
    private val launchDebounceMs = 30_000L

    override fun isTrackingApproximate(packageName: String): Boolean {
        return trackingApproximateMap[packageName] ?: false
    }

    fun clearSession(packageName: String) {
        seenVideoSignatures[packageName]?.clear()
        currentVideoSignature.remove(packageName)
    }

    private fun addDebugLog(
        packageName: String,
        signature: String,
        counted: Boolean,
        reason: String
    ) {
        val appName = MonitoredAppInfo.findByPackage(packageName)?.displayName ?: packageName
        val entry = VideoDetectionDebugLogEntry(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            appName = appName,
            signature = signature.ifBlank { "Unknown" },
            counted = counted,
            reason = reason
        )
        val current = _debugLogs.value
        val updated = (listOf(entry) + current).take(100)
        _debugLogs.value = updated
    }

    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo? = null,
        onPerformHomeAction: () -> Unit
    ) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == context.packageName) return // Ignore ScrollLock itself

        scope.launch {
            val budget = budgetRepository.getBudget(packageName) ?: return@launch
            if (!budget.isEnabled) return@launch

            val eventType = event.eventType
            val now = System.currentTimeMillis()

            // 1. App Launch / Session Change detection
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val lastLaunchTime = lastLaunchTimestamps[packageName] ?: 0L
                val isNewSession = (packageName != currentForegroundPackage) || (now - lastLaunchTime > launchDebounceMs)
                if (isNewSession) {
                    // Reset session video cache on fresh app session
                    if (packageName != currentForegroundPackage) {
                        currentForegroundPackage?.let { oldPkg ->
                            seenVideoSignatures[oldPkg]?.clear()
                            currentVideoSignature.remove(oldPkg)
                        }
                    }
                    lastLaunchTimestamps[packageName] = now
                    currentForegroundPackage = packageName
                    seenVideoSignatures.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }.clear()
                    currentVideoSignature.remove(packageName)
                    budgetRepository.recordAppLaunch(packageName)
                }
                checkExhaustionAndEnforce(packageName, onPerformHomeAction)
                return@launch
            }

            // 2. Video Feed Events
            val isPotentialVideoEvent = eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                    eventType == AccessibilityEvent.TYPE_VIEW_SELECTED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            if (!isPotentialVideoEvent) return@launch

            // Check if app supports short-form video feed counting
            if (!budget.scrollCountingSupported && !MonitoredAppInfo.isScrollTrackingSupportedFor(packageName)) {
                checkExhaustionAndEnforce(packageName, onPerformHomeAction)
                return@launch
            }

            // A. Check if the user is outside the video feed (comments, search, profile, settings)
            val isOutsideFeed = isUserOutsideVideoFeed(event, rootNode)
            if (isOutsideFeed) {
                // User is in comments, search, or profile - do not count
                return@launch
            }

            // B. Filter out horizontal sliders, progress seek bars, or tab switchers
            val className = event.className?.toString() ?: ""
            if (className.contains("HorizontalScrollView", ignoreCase = true) ||
                className.contains("SeekBar", ignoreCase = true) ||
                className.contains("ProgressBar", ignoreCase = true) ||
                className.contains("Slider", ignoreCase = true)
            ) {
                return@launch
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val deltaX = Math.abs(event.scrollDeltaX)
                val deltaY = Math.abs(event.scrollDeltaY)
                if (deltaX > 0 && deltaX > deltaY) {
                    return@launch // ignore horizontal scrubber or story swipe
                }
            }

            // C. Extract visible video signature
            val signature = extractVideoSignature(event, rootNode, packageName)

            if (signature.isNullOrBlank()) {
                // Signature could not be reliably extracted from this screen/app
                trackingApproximateMap[packageName] = true
                addDebugLog(
                    packageName = packageName,
                    signature = "N/A (Approximate Mode)",
                    counted = false,
                    reason = "Signature unreadable"
                )
                return@launch
            }

            // Signature was successfully extracted -> precise tracking
            trackingApproximateMap[packageName] = false

            // D. Check 1: Is this the same video currently active?
            val activeSig = currentVideoSignature[packageName]
            if (activeSig != null && activeSig == signature) {
                addDebugLog(
                    packageName = packageName,
                    signature = signature,
                    counted = false,
                    reason = "Same video signature"
                )
                return@launch
            }

            // E. Check 2: Has this video already been watched/swiped in this session?
            val sessionSeen = seenVideoSignatures.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }
            if (sessionSeen.contains(signature)) {
                currentVideoSignature[packageName] = signature
                addDebugLog(
                    packageName = packageName,
                    signature = signature,
                    counted = false,
                    reason = "Already watched in this session"
                )
                return@launch
            }

            // F. Check 3: 1-second cooldown window so one swipe cannot count twice
            val lastCounted = lastCountedTimestamp[packageName] ?: 0L
            if (now - lastCounted < cooldownWindowMs) {
                addDebugLog(
                    packageName = packageName,
                    signature = signature,
                    counted = false,
                    reason = "Cooldown active (< 1000ms)"
                )
                return@launch
            }

            // G. Video Verified! Count +1
            sessionSeen.add(signature)
            currentVideoSignature[packageName] = signature
            lastCountedTimestamp[packageName] = now
            lastRecordedTimestamp = now

            addDebugLog(
                packageName = packageName,
                signature = signature,
                counted = true,
                reason = "New video detected"
            )

            val classification = when {
                packageName == MonitoredAppInfo.INSTAGRAM.packageName -> EventClassification.SWIPE_FEED_REEL
                packageName == MonitoredAppInfo.YOUTUBE.packageName || packageName.contains("youtube", ignoreCase = true) -> EventClassification.SWIPE_FEED_SHORTS
                packageName == MonitoredAppInfo.TIKTOK.packageName || packageName.contains("musically", ignoreCase = true) -> EventClassification.SWIPE_FEED_TIKTOK
                else -> EventClassification.SCROLL_UNSPECIFIED
            }

            val observation = ScrollObservation(
                targetPackage = packageName,
                timestamp = now,
                eventClassification = classification,
                verificationStatus = VerificationStatus.VERIFIED,
                rawDeltaY = event.scrollY,
                sourceNodeClass = signature.take(64)
            )

            budgetRepository.recordObservation(observation)
            _observationEvents.tryEmit(observation)
            checkExhaustionAndEnforce(packageName, onPerformHomeAction)
        }
    }

    /**
     * Inspects active window hierarchy and event to determine if user is outside the video feed.
     * Prevents counting when user is reading comments, using search, browsing profile or settings.
     */
    private fun isUserOutsideVideoFeed(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        val className = event.className?.toString() ?: ""
        if (className.contains("EditText", ignoreCase = true)) return true

        // Check event text/descriptions
        val eventTexts = event.text.map { it.toString() }
        for (t in eventTexts) {
            if (t.equals("Comments", ignoreCase = true) ||
                t.contains("Add a comment", ignoreCase = true) ||
                t.contains("Top comments", ignoreCase = true) ||
                t.equals("Search", ignoreCase = true) ||
                t.contains("Search YouTube", ignoreCase = true)
            ) {
                return true
            }
        }

        val nodeToInspect = rootNode ?: try { event.source } catch (_: Exception) { null } ?: return false

        return checkNodeOutsideFeedRecursive(nodeToInspect, 0, 50)
    }

    private fun checkNodeOutsideFeedRecursive(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): Boolean {
        if (depth > maxDepth) return false

        val viewId = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val cls = node.className?.toString() ?: ""

        if (cls.contains("EditText", ignoreCase = true)) return true
        if (viewId.contains("comment", ignoreCase = true) ||
            viewId.contains("composer_edittext", ignoreCase = true) ||
            viewId.contains("search_src_text", ignoreCase = true) ||
            viewId.contains("search_edit_text", ignoreCase = true) ||
            viewId.contains("profile_header", ignoreCase = true)
        ) {
            return true
        }

        if (text.equals("Comments", ignoreCase = true) ||
            text.startsWith("Add a comment", ignoreCase = true) ||
            text.equals("Search", ignoreCase = true)
        ) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            if (checkNodeOutsideFeedRecursive(child, depth + 1, maxDepth)) {
                return true
            }
        }
        return false
    }

    /**
     * Extracts a unique signature for the currently visible video in Instagram Reels, YouTube Shorts, or TikTok.
     * Looks for creator handles, video captions/titles, audio names, or container resource IDs.
     */
    private fun extractVideoSignature(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        packageName: String
    ): String? {
        val root = rootNode ?: try { event.source } catch (_: Exception) { null }

        var creator: String? = null
        var titleOrCaption: String? = null
        var audioName: String? = null

        // 1. Check event text or content descriptions first (also supports test events)
        if (event.text.isNotEmpty()) {
            val combined = event.text.joinToString(" ").trim()
            if (combined.isNotBlank() && combined.length >= 3) {
                titleOrCaption = combined
            }
        }
        val eventDesc = event.contentDescription?.toString()?.trim()
        if (!eventDesc.isNullOrBlank() && titleOrCaption == null) {
            titleOrCaption = eventDesc
        }

        // 2. Traverse node hierarchy to find creator, caption, audio track
        if (root != null) {
            val extracted = extractFromHierarchy(root)
            if (extracted.creator != null) creator = extracted.creator
            if (extracted.caption != null) titleOrCaption = extracted.caption
            if (extracted.audio != null) audioName = extracted.audio
        }

        // Construct normalized signature
        val parts = mutableListOf<String>()
        if (!creator.isNullOrBlank()) parts.add(creator)
        if (!titleOrCaption.isNullOrBlank()) parts.add(titleOrCaption.take(50))
        if (!audioName.isNullOrBlank()) parts.add("♪$audioName")

        if (parts.isNotEmpty()) {
            return parts.joinToString(" • ")
        }

        // Fallback for spike simulation
        if (event.source?.viewIdResourceName != null) {
            return event.source?.viewIdResourceName
        }

        return null
    }

    private data class ExtractedInfo(
        val creator: String? = null,
        val caption: String? = null,
        val audio: String? = null
    )

    private fun extractFromHierarchy(root: AccessibilityNodeInfo): ExtractedInfo {
        var foundCreator: String? = null
        var foundCaption: String? = null
        var foundAudio: String? = null

        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        var inspected = 0

        val genericLabels = setOf(
            "like", "dislike", "share", "remix", "subscribe", "subscribed",
            "comment", "comments", "sound", "original sound", "save", "more",
            "report", "follow", "following", "reply", "reels", "shorts", "audio"
        )

        while (queue.isNotEmpty() && inspected < 70) {
            val (node, depth) = queue.removeFirst()
            inspected++

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            val candidate = text ?: desc
            if (!candidate.isNullOrBlank()) {
                val lowerCandidate = candidate.lowercase()
                if (!genericLabels.contains(lowerCandidate)) {
                    // Check creator
                    if (foundCreator == null && (
                        candidate.startsWith("@") ||
                        viewId.contains("channel") ||
                        viewId.contains("creator") ||
                        viewId.contains("author") ||
                        viewId.contains("username")
                    )) {
                        foundCreator = candidate
                    } else if (foundAudio == null && (viewId.contains("audio") || viewId.contains("music") || viewId.contains("sound"))) {
                        foundAudio = candidate
                    } else if (foundCaption == null && candidate.length >= 4 && !viewId.contains("button")) {
                        foundCaption = candidate
                    }
                }
            }

            if (foundCreator != null && foundCaption != null) break

            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                queue.add(child to (depth + 1))
            }
        }

        return ExtractedInfo(foundCreator, foundCaption, foundAudio)
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
    fun simulateSpikeEvent(
        packageName: String,
        classification: EventClassification = EventClassification.MANUAL_SPIKE_TEST,
        simulatedSignature: String? = null
    ) {
        val now = System.currentTimeMillis()
        val sig = simulatedSignature ?: "Simulated_Video_${now % 10000}"
        
        // Check cooldown
        val lastCounted = lastCountedTimestamp[packageName] ?: 0L
        if (now - lastCounted < cooldownWindowMs) {
            addDebugLog(packageName, sig, false, "Cooldown active (< 1000ms)")
            return
        }

        // Check if same signature
        if (currentVideoSignature[packageName] == sig) {
            addDebugLog(packageName, sig, false, "Same video signature")
            return
        }

        // Check if already seen in session
        val sessionSeen = seenVideoSignatures.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }
        if (sessionSeen.contains(sig)) {
            currentVideoSignature[packageName] = sig
            addDebugLog(packageName, sig, false, "Already watched in this session")
            return
        }

        sessionSeen.add(sig)
        currentVideoSignature[packageName] = sig
        lastCountedTimestamp[packageName] = now
        lastRecordedTimestamp = now
        trackingApproximateMap[packageName] = false

        addDebugLog(packageName, sig, true, "New video detected (Spike Simulation)")

        val observation = ScrollObservation(
            targetPackage = packageName,
            timestamp = now,
            eventClassification = classification,
            verificationStatus = VerificationStatus.VERIFIED,
            rawDeltaY = 100,
            sourceNodeClass = sig.take(64)
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
