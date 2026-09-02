package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric capability states on user device.
 */
enum class BiometricStatus(val displayName: String, val isAvailable: Boolean) {
    AVAILABLE("Fingerprint & Face Unlock Available", true),
    NOT_ENROLLED("Sensor present, no biometrics enrolled in device settings", false),
    NO_HARDWARE("No biometric hardware detected on this device", false),
    HARDWARE_UNAVAILABLE("Biometric hardware temporarily unavailable", false),
    UNSUPPORTED("Biometric authentication unsupported on this system", false)
}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val errorCode: Int, val message: String) : BiometricResult()
    data class Cancelled(val reason: String) : BiometricResult()
    object Failed : BiometricResult()
}

object BiometricAuthManager {

    /**
     * Check if device supports Biometric Authentication and whether user has fingerprints/face enrolled.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK

            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.UNSUPPORTED
                else -> BiometricStatus.UNSUPPORTED
            }
        } catch (e: Exception) {
            BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Displays system native BiometricPrompt dialog.
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Gym Owner Authentication",
        subtitle: String = "Verify your fingerprint or face to unlock ERP",
        description: String = "Keep member records, revenue and invoices secure",
        negativeButtonText: String = "Use Passcode",
        onResult: (BiometricResult) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onResult(BiometricResult.Success)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        ) {
                            onResult(BiometricResult.Cancelled(errString.toString()))
                        } else {
                            onResult(BiometricResult.Error(errorCode, errString.toString()))
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onResult(BiometricResult.Failed)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onResult(BiometricResult.Error(-1, e.localizedMessage ?: "Biometric prompt failure"))
        }
    }
}
