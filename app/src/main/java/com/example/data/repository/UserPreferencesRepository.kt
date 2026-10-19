package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val strictModeProtection: Boolean,
    val notificationsEnabled: Boolean,
    val emergencyOverrideUsed: Boolean,
    val lastResetDate: String,
    // PIN Security System
    val pinHash: String = "",
    val pinSalt: String = "",
    val isPinSet: Boolean = false,
    val pinRequireDisableMonitoring: Boolean = true,
    val pinRequireChangeLimits: Boolean = true,
    val pinRequireRemoveApps: Boolean = true,
    val pinRequireResetUsage: Boolean = true,
    val pinRequireChangeSecurity: Boolean = true,
    val failedPinAttempts: Int = 0,
    val lockoutUntilTimestamp: Long = 0L,
    // Random Reset System
    val randomResetEnabled: Boolean = true,
    val randomResetMinHours: Int = 3,
    val randomResetMaxHours: Int = 6
) {
    val isLockedOut: Boolean
        get() = System.currentTimeMillis() < lockoutUntilTimestamp

    val lockoutSecondsRemaining: Int
        get() {
            val diff = lockoutUntilTimestamp - System.currentTimeMillis()
            return if (diff > 0) ((diff + 999) / 1000).toInt() else 0
        }
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STRICT_MODE_PROTECTION = booleanPreferencesKey("strict_mode_protection")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val EMERGENCY_OVERRIDE_USED = booleanPreferencesKey("emergency_override_used")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")

        // Feature 1: 4-digit PIN keys
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_REQUIRE_DISABLE_MONITORING = booleanPreferencesKey("pin_req_disable_mon")
        val PIN_REQUIRE_CHANGE_LIMITS = booleanPreferencesKey("pin_req_change_limits")
        val PIN_REQUIRE_REMOVE_APPS = booleanPreferencesKey("pin_req_remove_apps")
        val PIN_REQUIRE_RESET_USAGE = booleanPreferencesKey("pin_req_reset_usage")
        val PIN_REQUIRE_CHANGE_SECURITY = booleanPreferencesKey("pin_req_change_security")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val LOCKOUT_UNTIL_TIMESTAMP = longPreferencesKey("lockout_until_timestamp")

        // Feature 2: Random Reset keys
        val RANDOM_RESET_ENABLED = booleanPreferencesKey("random_reset_enabled")
        val RANDOM_RESET_MIN_HOURS = intPreferencesKey("random_reset_min_hours")
        val RANDOM_RESET_MAX_HOURS = intPreferencesKey("random_reset_max_hours")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val hash = preferences[PreferencesKeys.PIN_HASH] ?: ""
        UserPreferences(
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            strictModeProtection = preferences[PreferencesKeys.STRICT_MODE_PROTECTION] ?: false,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            emergencyOverrideUsed = preferences[PreferencesKeys.EMERGENCY_OVERRIDE_USED] ?: false,
            lastResetDate = preferences[PreferencesKeys.LAST_RESET_DATE] ?: "",
            pinHash = hash,
            pinSalt = preferences[PreferencesKeys.PIN_SALT] ?: "",
            isPinSet = hash.isNotEmpty(),
            pinRequireDisableMonitoring = preferences[PreferencesKeys.PIN_REQUIRE_DISABLE_MONITORING] ?: true,
            pinRequireChangeLimits = preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_LIMITS] ?: true,
            pinRequireRemoveApps = preferences[PreferencesKeys.PIN_REQUIRE_REMOVE_APPS] ?: true,
            pinRequireResetUsage = preferences[PreferencesKeys.PIN_REQUIRE_RESET_USAGE] ?: true,
            pinRequireChangeSecurity = preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_SECURITY] ?: true,
            failedPinAttempts = preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] ?: 0,
            lockoutUntilTimestamp = preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] ?: 0L,
            randomResetEnabled = preferences[PreferencesKeys.RANDOM_RESET_ENABLED] ?: true,
            randomResetMinHours = preferences[PreferencesKeys.RANDOM_RESET_MIN_HOURS] ?: 3,
            randomResetMaxHours = preferences[PreferencesKeys.RANDOM_RESET_MAX_HOURS] ?: 6
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setStrictModeProtection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_MODE_PROTECTION] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setEmergencyOverrideUsed(used: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_OVERRIDE_USED] = used
        }
    }

    suspend fun setLastResetDate(dateKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_RESET_DATE] = dateKey
        }
    }

    // Feature 1: PIN Management
    suspend fun setPin(hash: String, salt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_HASH] = hash
            preferences[PreferencesKeys.PIN_SALT] = salt
            // Default all security protections to ON after a PIN is created
            preferences[PreferencesKeys.PIN_REQUIRE_DISABLE_MONITORING] = true
            preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_LIMITS] = true
            preferences[PreferencesKeys.PIN_REQUIRE_REMOVE_APPS] = true
            preferences[PreferencesKeys.PIN_REQUIRE_RESET_USAGE] = true
            preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_SECURITY] = true
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    suspend fun removePin() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PIN_HASH)
            preferences.remove(PreferencesKeys.PIN_SALT)
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    suspend fun updateSecurityControl(
        requireDisableMonitoring: Boolean? = null,
        requireChangeLimits: Boolean? = null,
        requireRemoveApps: Boolean? = null,
        requireResetUsage: Boolean? = null,
        requireChangeSecurity: Boolean? = null
    ) {
        context.dataStore.edit { preferences ->
            requireDisableMonitoring?.let { preferences[PreferencesKeys.PIN_REQUIRE_DISABLE_MONITORING] = it }
            requireChangeLimits?.let { preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_LIMITS] = it }
            requireRemoveApps?.let { preferences[PreferencesKeys.PIN_REQUIRE_REMOVE_APPS] = it }
            requireResetUsage?.let { preferences[PreferencesKeys.PIN_REQUIRE_RESET_USAGE] = it }
            requireChangeSecurity?.let { preferences[PreferencesKeys.PIN_REQUIRE_CHANGE_SECURITY] = it }
        }
    }

    suspend fun recordFailedPinAttempt(): Boolean {
        var lockedOut = false
        context.dataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] ?: 0) + 1
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = current
            if (current >= 5) {
                // 30 seconds cooldown lockout
                preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = System.currentTimeMillis() + 30_000L
                lockedOut = true
            }
        }
        return lockedOut
    }

    suspend fun resetFailedPinAttempts() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAILED_PIN_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL_TIMESTAMP] = 0L
        }
    }

    // Feature 2: Random Reset Configuration
    suspend fun setRandomResetSettings(enabled: Boolean, minHours: Int, maxHours: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RANDOM_RESET_ENABLED] = enabled
            preferences[PreferencesKeys.RANDOM_RESET_MIN_HOURS] = minHours
            preferences[PreferencesKeys.RANDOM_RESET_MAX_HOURS] = maxOf(minHours, maxHours)
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
