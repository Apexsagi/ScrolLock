package com.example

import com.example.data.repository.UserPreferences
import com.example.domain.model.AppBudget
import com.example.security.PinSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityAndRandomResetTest {

    @Test
    fun testPinValidation() {
        assertTrue(PinSecurityHelper.isValidPin("1234"))
        assertTrue(PinSecurityHelper.isValidPin("0000"))
        assertTrue(PinSecurityHelper.isValidPin("9876"))

        // Reject non-4-digit
        assertFalse(PinSecurityHelper.isValidPin("123"))
        assertFalse(PinSecurityHelper.isValidPin("12345"))
        assertFalse(PinSecurityHelper.isValidPin(""))

        // Reject non-digits
        assertFalse(PinSecurityHelper.isValidPin("12a4"))
        assertFalse(PinSecurityHelper.isValidPin("abcd"))
        assertFalse(PinSecurityHelper.isValidPin("12 4"))
    }

    @Test
    fun testPinHashingAndVerification() {
        val salt = PinSecurityHelper.generateSalt()
        val pin = "4826"
        val hash = PinSecurityHelper.hashPin(pin, salt)

        // Raw PIN is never equal to hash
        assertNotEquals(pin, hash)
        assertTrue(hash.isNotEmpty())

        // Correct verification
        assertTrue(PinSecurityHelper.verifyPin(pin, salt, hash))

        // Incorrect PIN fails
        assertFalse(PinSecurityHelper.verifyPin("4827", salt, hash))
        assertFalse(PinSecurityHelper.verifyPin("0000", salt, hash))
    }

    @Test
    fun testSaltUniqueness() {
        val salt1 = PinSecurityHelper.generateSalt()
        val salt2 = PinSecurityHelper.generateSalt()
        assertNotEquals(salt1, salt2)

        val hash1 = PinSecurityHelper.hashPin("1234", salt1)
        val hash2 = PinSecurityHelper.hashPin("1234", salt2)
        // Same PIN with different salts produces distinct hashes
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testRandomResetDelayBounds() {
        val minHours = 3
        val maxHours = 6
        val minMinutes = minHours * 60
        val maxMinutes = maxHours * 60

        val observedDelays = mutableSetOf<Int>()
        for (i in 1..100) {
            val delayMinutes = PinSecurityHelper.generateRandomDelayMinutes(minHours, maxHours)
            assertTrue("Delay $delayMinutes must be >= $minMinutes", delayMinutes >= minMinutes)
            assertTrue("Delay $delayMinutes must be <= $maxMinutes", delayMinutes <= maxMinutes)
            observedDelays.add(delayMinutes)
        }

        // Must show random variation (not always the same delay)
        assertTrue("Expected variety of delays across 100 iterations", observedDelays.size > 10)
    }

    @Test
    fun testLockoutStateCalculation() {
        val now = System.currentTimeMillis()
        val lockedUserPrefs = UserPreferences(
            isPinSet = true,
            failedPinAttempts = 5,
            lockoutUntilTimestamp = now + 30_000L
        )

        assertTrue(lockedUserPrefs.isLockedOut)
        assertTrue(lockedUserPrefs.lockoutSecondsRemaining in 1..30)

        val expiredUserPrefs = UserPreferences(
            isPinSet = true,
            failedPinAttempts = 5,
            lockoutUntilTimestamp = now - 1000L
        )
        assertFalse(expiredUserPrefs.isLockedOut)
        assertEquals(0, expiredUserPrefs.lockoutSecondsRemaining)
    }

    @Test
    fun testAppBudgetLockResetTimestamp() {
        val now = System.currentTimeMillis()
        val lockedBudget = AppBudget(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            lockResetTimestamp = now + 60_000L
        )
        assertTrue(lockedBudget.isCurrentlyLocked)

        val unlockedBudget = AppBudget(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            lockResetTimestamp = now - 10_000L
        )
        assertFalse(unlockedBudget.isCurrentlyLocked)
    }
}
