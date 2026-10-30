package com.example

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ScrollLockDatabase
import com.example.data.repository.BudgetRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.AppBudget
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoredAppInfo
import com.example.monitoring.AccessibilityScrollMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoSignatureScrollCountingTest {

    private lateinit var database: ScrollLockDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var usageStatsRepository: UsageStatsRepository
    private lateinit var monitor: AccessibilityScrollMonitor
    private val igPkg = MonitoredAppInfo.INSTAGRAM.packageName

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScrollLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetRepository = BudgetRepository(database)
        usageStatsRepository = UsageStatsRepository(context)
        monitor = AccessibilityScrollMonitor(context, budgetRepository, usageStatsRepository)

        budgetRepository.initializeDefaultBudgetsIfEmpty()
        budgetRepository.updateLimit(igPkg, 50)
        budgetRepository.updateEnabled(igPkg, true)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCountingWhenNewVideoSignatureAppears() = runBlocking {
        // First video
        val event1 = createAccessibilityScrollEvent(igPkg, "@creator1 • Amazing reel 1")
        monitor.onAccessibilityEvent(event1) {}

        kotlinx.coroutines.delay(100)
        var count = budgetRepository.getScrollCountToday(igPkg)
        assertEquals("First video should increment count to 1", 1, count)

        // Same signature should NOT increment even after delay
        kotlinx.coroutines.delay(1100)
        val eventSame = createAccessibilityScrollEvent(igPkg, "@creator1 • Amazing reel 1")
        monitor.onAccessibilityEvent(eventSame) {}
        kotlinx.coroutines.delay(100)
        count = budgetRepository.getScrollCountToday(igPkg)
        assertEquals("Identical signature must not increment count", 1, count)

        // Different video signature after cooldown should increment to 2
        val event2 = createAccessibilityScrollEvent(igPkg, "@creator2 • Inspiring travel short")
        monitor.onAccessibilityEvent(event2) {}
        kotlinx.coroutines.delay(100)
        count = budgetRepository.getScrollCountToday(igPkg)
        assertEquals("New signature after cooldown should increment count to 2", 2, count)
    }

    @Test
    fun testCooldownPreventsImmediateDoubleCount() = runBlocking {
        // Video 1
        val event1 = createAccessibilityScrollEvent(igPkg, "@creator1 • Video 1")
        monitor.onAccessibilityEvent(event1) {}
        kotlinx.coroutines.delay(100)
        assertEquals(1, budgetRepository.getScrollCountToday(igPkg))

        // Video 2 immediately within 100ms (< 1000ms cooldown)
        val event2 = createAccessibilityScrollEvent(igPkg, "@creator2 • Video 2")
        monitor.onAccessibilityEvent(event2) {}
        kotlinx.coroutines.delay(100)
        assertEquals("Event within cooldown window must not be counted", 1, budgetRepository.getScrollCountToday(igPkg))

        // Check debug logs for cooldown reason
        val logs = monitor.debugLogs.first()
        assertTrue("Debug log should capture cooldown active", logs.any { it.reason.contains("Cooldown") })
    }

    @Test
    fun testSwipeBackToPreviousVideoInSameSessionNotCountedAgain() = runBlocking {
        // Video 1
        val event1 = createAccessibilityScrollEvent(igPkg, "@creator1 • Video 1")
        monitor.onAccessibilityEvent(event1) {}
        kotlinx.coroutines.delay(1100)

        // Video 2
        val event2 = createAccessibilityScrollEvent(igPkg, "@creator2 • Video 2")
        monitor.onAccessibilityEvent(event2) {}
        kotlinx.coroutines.delay(1100)
        assertEquals(2, budgetRepository.getScrollCountToday(igPkg))

        // User swipes BACK to Video 1 in the same session
        val eventBack = createAccessibilityScrollEvent(igPkg, "@creator1 • Video 1")
        monitor.onAccessibilityEvent(eventBack) {}
        kotlinx.coroutines.delay(100)
        assertEquals("Swiping back to already seen video in same session must not increment count", 2, budgetRepository.getScrollCountToday(igPkg))

        val logs = monitor.debugLogs.first()
        assertTrue("Debug log should capture already watched in this session", logs.any { it.reason.contains("Already watched") })
    }

    @Test
    fun testCommentsScreenDoesNotCountScrolls() = runBlocking {
        val eventInComments = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
            packageName = igPkg
            text.add("Comments")
            className = "android.widget.TextView"
        }
        monitor.onAccessibilityEvent(eventInComments) {}
        kotlinx.coroutines.delay(100)
        assertEquals("Comments screen scroll must not be counted", 0, budgetRepository.getScrollCountToday(igPkg))
    }

    @Test
    fun testUnreadableSignatureMarksAppAsApproximate() = runBlocking {
        // Event with no text or content description
        val blankEvent = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
            packageName = igPkg
            className = "android.view.View"
        }
        monitor.onAccessibilityEvent(blankEvent) {}
        kotlinx.coroutines.delay(100)

        assertTrue("Unreadable video signature should mark tracking as approximate", monitor.isTrackingApproximate(igPkg))
    }

    private fun createAccessibilityScrollEvent(pkg: String, videoSignatureText: String): AccessibilityEvent {
        return AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
            packageName = pkg
            text.add(videoSignatureText)
            className = "androidx.viewpager2.widget.ViewPager2"
            scrollY = 120
        }
    }
}
