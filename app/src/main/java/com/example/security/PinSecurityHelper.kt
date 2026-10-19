package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurityHelper {

    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        // Multiple rounds of SHA-256 for key stretching
        var digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        for (i in 0 until 999) {
            md.reset()
            md.update(digest)
            digest = md.digest(salt.toByteArray(Charsets.UTF_8))
        }
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, salt: String, expectedHash: String): Boolean {
        if (enteredPin.length != 4 || salt.isEmpty() || expectedHash.isEmpty()) {
            return false
        }
        val computedHash = hashPin(enteredPin, salt)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }

    fun generateRandomDelayMinutes(minHours: Int, maxHours: Int): Int {
        val safeMin = maxOf(1, minHours) * 60
        val safeMax = maxOf(safeMin, maxHours * 60)
        val range = safeMax - safeMin
        return if (range <= 0) safeMin else safeMin + secureRandom.nextInt(range + 1)
    }

    fun formatDelayTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}
