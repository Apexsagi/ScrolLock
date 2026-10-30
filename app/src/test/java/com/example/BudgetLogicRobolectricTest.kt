package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ScrollLockDatabase
import com.example.data.repository.BudgetRepository
import com.example.domain.model.EventClassification
import com.example.domain.model.LockMode
import com.example.domain.model.MonitoredAppInfo
import com.example.domain.model.ScrollObservation
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BudgetLogicRobolectricTest {

    private lateinit var db: ScrollLockDatabase
    private lateinit var repository: BudgetRepository
    private val igPkg = MonitoredAppInfo.INSTAGRAM.packageName
    private val ytPkg = MonitoredAppInfo.YOUTUBE.packageName

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ScrollLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDefaultBudgetsInitialization() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()
        val budgets = repository.allBudgets.first()
        assertEquals(5, budgets.size)

        val ig = budgets.find { it.packageName == igPkg }
        assertNotNull(ig)
        assertEquals(50, ig?.dailyLimit)
        assertEquals(LockMode.STANDARD, ig?.lockMode)

        val yt = budgets.find { it.packageName == ytPkg }
        assertNotNull(yt)
        assertEquals(100, yt?.dailyLimit)
    }

    @Test
    fun testBudgetIncreaseAndDecrease() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()

        // Increase Instagram limit to 75
        repository.updateLimit(igPkg, 75)
        var ig = repository.getBudget(igPkg)
        assertEquals(75, ig?.dailyLimit)

        // Decrease limit to 30
        repository.updateLimit(igPkg, 30)
        ig = repository.getBudget(igPkg)
        assertEquals(30, ig?.dailyLimit)

        // Negative limit should be clamped to at least 1
        repository.updateLimit(igPkg, -10)
        ig = repository.getBudget(igPkg)
        assertEquals(1, ig?.dailyLimit)
    }

    @Test
    fun testLockModeTransitions() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()

        repository.updateLockMode(igPkg, LockMode.GENTLE)
        assertEquals(LockMode.GENTLE, repository.getBudget(igPkg)?.lockMode)

        repository.updateLockMode(igPkg, LockMode.STRICT)
        assertEquals(LockMode.STRICT, repository.getBudget(igPkg)?.lockMode)

        repository.updateLockMode(igPkg, LockMode.STANDARD)
        assertEquals(LockMode.STANDARD, repository.getBudget(igPkg)?.lockMode)
    }

    @Test
    fun testScrollCountAndRemainingCalculation() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty() // limit 50

        // Initially 0 count, 50 remaining
        assertEquals(0, repository.getScrollCountToday(igPkg))
        val initialRemaining = repository.observeRemainingBudgetToday(igPkg).first()
        assertEquals(50, initialRemaining)

        // Record 5 observations
        val now = System.currentTimeMillis()
        for (i in 1..5) {
            repository.recordObservation(
                ScrollObservation(
                    targetPackage = igPkg,
                    timestamp = now + i,
                    eventClassification = EventClassification.SWIPE_FEED_REEL,
                    verificationStatus = VerificationStatus.VERIFIED,
                    rawDeltaY = 120,
                    sourceNodeClass = "ViewPager2"
                )
            )
        }

        val countAfter5 = repository.getScrollCountToday(igPkg)
        assertEquals(5, countAfter5)

        val remainingAfter5 = repository.observeRemainingBudgetToday(igPkg).first()
        assertEquals(45, remainingAfter5)

        val isExhaustedAfter5 = repository.observeIsBudgetExhaustedToday(igPkg).first()
        assertFalse(isExhaustedAfter5)
    }

    @Test
    fun testLimitExhaustionState() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()
        repository.updateLimit(igPkg, 3) // Set low limit of 3

        val now = System.currentTimeMillis()
        for (i in 1..3) {
            repository.recordObservation(
                ScrollObservation(
                    targetPackage = igPkg,
                    timestamp = now + i,
                    eventClassification = EventClassification.SWIPE_FEED_REEL,
                    verificationStatus = VerificationStatus.VERIFIED
                )
            )
        }

        val count = repository.getScrollCountToday(igPkg)
        assertEquals(3, count)

        val isExhausted = repository.observeIsBudgetExhaustedToday(igPkg).first()
        assertTrue(isExhausted)

        val remaining = repository.observeRemainingBudgetToday(igPkg).first()
        assertEquals(0, remaining)
    }

    @Test
    fun testAuditLoggingOnActions() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()
        repository.updateLimit(igPkg, 60)
        repository.updateLockMode(igPkg, LockMode.STRICT)

        val logs = repository.getAuditLogs(10).first()
        assertTrue(logs.any { it.eventType == "LIMIT_ADJUSTED" })
        assertTrue(logs.any { it.eventType == "LOCK_MODE_CHANGED" })
    }

    @Test
    fun testSpikeRecordPersistence() = runBlocking {
        val recordId = repository.recordSpikeTest(
            scenario = "Unit Test Scenario",
            expectedSwipes = 10,
            detectedEvents = 10,
            falsePositives = 0,
            deduplicatedEvents = 25,
            precisionPercent = 100,
            recallPercent = 100,
            notes = "Test pass"
        )
        assertTrue(recordId > 0)

        val records = repository.getAllSpikeRecords().first()
        assertEquals(1, records.size)
        assertEquals(100, records[0].precisionPercent)
    }

    @Test
    fun testYouTubeVariantRecognitionAndReset() = runBlocking {
        repository.initializeDefaultBudgetsIfEmpty()
        val yt = repository.getBudget(ytPkg)
        assertNotNull(yt)
        assertEquals(0, yt?.launchLimit) // 0 means no arbitrary launch lockout for YouTube
        assertEquals(100, yt?.dailyLimit)

        // Verify variant detection
        assertTrue(MonitoredAppInfo.isScrollTrackingSupportedFor(ytPkg))
        assertTrue(MonitoredAppInfo.isScrollTrackingSupportedFor("com.google.android.apps.youtube.mango"))
        assertEquals(MonitoredAppInfo.YOUTUBE, MonitoredAppInfo.findByPackage("com.google.android.youtube"))
        assertEquals(MonitoredAppInfo.YOUTUBE, MonitoredAppInfo.findByPackage("com.google.android.apps.youtube.mango"))

        // Record a YouTube short observation
        repository.recordObservation(
            ScrollObservation(
                targetPackage = ytPkg,
                timestamp = System.currentTimeMillis(),
                eventClassification = EventClassification.SWIPE_FEED_SHORTS,
                verificationStatus = VerificationStatus.VERIFIED,
                rawDeltaY = 150,
                sourceNodeClass = "ReelRecyclerView"
            )
        )
        assertEquals(1, repository.getScrollCountToday(ytPkg))

        // Reset YouTube usage
        repository.resetTodayUsage(ytPkg)
        assertEquals(0, repository.getScrollCountToday(ytPkg))
    }
}
