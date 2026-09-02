package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.GymSetting
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Enterprise-grade App Lock & Biometric Security Manager.
 * 
 * Provides synchronous security state evaluation at cold-start, secure salted SHA-256 PIN hashing,
 * reliable background-timeout tracking across the Android lifecycle, and in-memory process session guards.
 */
class AppSecurityManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory process session authentication state.
    // Volatile & never persisted to disk as true across app restarts/process recreation.
    @Volatile
    private var isSessionAuthenticated: Boolean = false

    @Volatile
    private var lastAuthenticatedTimestamp: Long = 0L

    companion object {
        private const val PREFS_NAME = "app_security_vault_prefs"

        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_PIN_ENABLED = "key_pin_enabled"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_AUTO_LOCK_TIMEOUT_MINUTES = "key_auto_lock_timeout_minutes"
        private const val KEY_LAST_BACKGROUND_TIMESTAMP = "key_last_background_timestamp"

        @Volatile
        private var INSTANCE: AppSecurityManager? = null

        fun getInstance(context: Context): AppSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Computes salted SHA-256 hash for secure PIN storage.
         */
        fun hashPin(pin: String, salt: String): String {
            val input = "$salt:$pin:IronForgeSecureVault2026"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        /**
         * Generates a cryptographically secure random salt.
         */
        private fun generateSalt(): String {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            return saltBytes.joinToString("") { "%02x".format(it) }
        }
    }

    // -------------------------------------------------------------
    // Synchronous Security State Queries (Cold Start Safe)
    // -------------------------------------------------------------

    /**
     * Checks whether App Lock is enabled (either Biometric or PIN).
     */
    fun isAppLockEnabled(): Boolean {
        val overall = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val pin = isPinEnabled()
        return overall || biometric || pin
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun isPinEnabled(): Boolean {
        val hasHash = prefs.getString(KEY_PIN_HASH, null)?.isNotBlank() == true
        val pinEnabledFlag = prefs.getBoolean(KEY_PIN_ENABLED, false)
        return hasHash && pinEnabledFlag
    }

    fun isPinConfigured(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null)?.isNotBlank() == true
    }

    fun getAutoLockTimeoutMinutes(): Int {
        return prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, 0)
    }

    fun isSessionAuthenticated(): Boolean {
        return isSessionAuthenticated
    }

    /**
     * Evaluates on cold-start whether the app MUST display the lock screen immediately.
     * Returns true if App Lock is enabled and session has not been authenticated in this process.
     */
    fun isLockRequiredOnLaunch(): Boolean {
        if (!isAppLockEnabled()) {
            return false
        }
        return !isSessionAuthenticated
    }

    // -------------------------------------------------------------
    // Lifecycle & Timeout Evaluation
    // -------------------------------------------------------------

    /**
     * Records the exact timestamp when the application transitions to background.
     */
    fun onAppBackgrounded() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIMESTAMP, now).apply()
    }

    /**
     * Evaluates whether the app should be locked when returning to foreground.
     */
    fun shouldLockOnForeground(): Boolean {
        if (!isAppLockEnabled()) {
            return false
        }

        // If not authenticated in this session, lock immediately
        if (!isSessionAuthenticated) {
            return true
        }

        val timeoutMinutes = getAutoLockTimeoutMinutes()

        // 0 = Lock immediately upon minimize/exit
        if (timeoutMinutes == 0) {
            isSessionAuthenticated = false
            return true
        }

        // -1 = Never auto-lock during current process lifetime (except on app restart)
        if (timeoutMinutes < 0) {
            return false
        }

        // Check elapsed timeout
        val lastBackground = prefs.getLong(KEY_LAST_BACKGROUND_TIMESTAMP, 0L)
        if (lastBackground <= 0L) {
            return false
        }

        val elapsed = System.currentTimeMillis() - lastBackground
        val timeoutMillis = timeoutMinutes * 60 * 1000L

        if (elapsed >= timeoutMillis) {
            isSessionAuthenticated = false
            return true
        }

        return false
    }

    // -------------------------------------------------------------
    // Session State Mutations
    // -------------------------------------------------------------

    /**
     * Unlocks and marks the active session as authenticated.
     */
    fun authenticateSession() {
        isSessionAuthenticated = true
        lastAuthenticatedTimestamp = System.currentTimeMillis()
    }

    /**
     * Explicitly invalidates the active session and locks the app.
     */
    fun lockSession() {
        isSessionAuthenticated = false
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIMESTAMP, 0L).apply()
    }

    // -------------------------------------------------------------
    // Configuration Mutations
    // -------------------------------------------------------------

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled || isPinEnabled())
            .apply()
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_PIN_ENABLED, enabled)
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled || isBiometricEnabled())
            .apply()
    }

    fun setAutoLockTimeoutMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, minutes)
            .apply()
    }

    /**
     * Sets and securely hashes a new 4-digit PIN.
     */
    fun setPin(pin: String) {
        val cleanPin = pin.trim()
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return
        }

        val salt = generateSalt()
        val hash = hashPin(cleanPin, salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_PIN_ENABLED, true)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .apply()
    }

    /**
     * Removes the configured PIN.
     */
    fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putBoolean(KEY_APP_LOCK_ENABLED, isBiometricEnabled())
            .apply()
    }

    /**
     * Verifies user input against stored salted SHA-256 hash in constant time.
     */
    fun verifyPin(inputPin: String): Boolean {
        val cleanInput = inputPin.trim()
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val computedHash = hashPin(cleanInput, storedSalt)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    // -------------------------------------------------------------
    // Synchronization with Room & Firestore GymSetting Entity
    // -------------------------------------------------------------

    /**
     * Syncs security preferences from GymSetting if Room/Firestore holds newer or migrated state.
     */
    fun syncFromGymSetting(setting: GymSetting) {
        val editor = prefs.edit()
        var changed = false

        if (setting.isBiometricEnabled != isBiometricEnabled()) {
            editor.putBoolean(KEY_BIOMETRIC_ENABLED, setting.isBiometricEnabled)
            changed = true
        }

        if (setting.biometricAutoLockMinutes != getAutoLockTimeoutMinutes()) {
            editor.putInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, setting.biometricAutoLockMinutes)
            changed = true
        }

        // Migrate plain-text PIN if present in GymSetting from older versions
        if (setting.securityPin.isNotBlank() && !isPinConfigured()) {
            val salt = generateSalt()
            val hash = hashPin(setting.securityPin.trim(), salt)
            editor.putString(KEY_PIN_HASH, hash)
            editor.putString(KEY_PIN_SALT, salt)
            editor.putBoolean(KEY_PIN_ENABLED, true)
            changed = true
        }

        val effectiveLock = setting.isBiometricEnabled || isPinEnabled() || setting.securityPin.isNotBlank()
        editor.putBoolean(KEY_APP_LOCK_ENABLED, effectiveLock)

        if (changed) {
            editor.apply()
        }
    }

    /**
     * Applies the current security preferences to a GymSetting copy for Room & Firestore.
     */
    fun applyToGymSetting(current: GymSetting): GymSetting {
        return current.copy(
            isBiometricEnabled = isBiometricEnabled(),
            biometricAutoLockMinutes = getAutoLockTimeoutMinutes(),
            securityPin = if (isPinConfigured()) "ENCRYPTED_PIN_HASH" else ""
        )
    }
}
