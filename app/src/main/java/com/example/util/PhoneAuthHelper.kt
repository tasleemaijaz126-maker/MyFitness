package com.example.util

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.PhoneAuthProvider

/**
 * Phone Authentication & SMS OTP Helper for Customer Mobile Number Verification via Firebase.
 */
object PhoneAuthHelper {

    fun normalizePhoneNumber(rawNumber: String?): String? {
        return PhoneUtils.normalizeToE164(rawNumber)
    }

    fun formatDisplayNumber(rawNumber: String?): String {
        return PhoneUtils.formatDisplayNumber(rawNumber)
    }

    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, token: Any?) -> Unit,
        onVerificationCompleted: (Any?) -> Unit,
        onVerificationFailed: (errorMessage: String) -> Unit
    ) {
        FirebasePhoneAuthHelper.sendOtp(
            activity = activity,
            phoneNumber = phoneNumber,
            forceResendingToken = null,
            onCodeSent = { vId, token -> onCodeSent(vId, token) },
            onVerificationCompleted = { cred -> onVerificationCompleted(cred) },
            onVerificationFailed = onVerificationFailed
        )
    }

    fun resendOtp(
        activity: Activity,
        phoneNumber: String,
        token: Any?,
        onCodeSent: (verificationId: String, token: Any?) -> Unit,
        onVerificationCompleted: (Any?) -> Unit,
        onVerificationFailed: (errorMessage: String) -> Unit
    ) {
        val resendToken = token as? PhoneAuthProvider.ForceResendingToken
        FirebasePhoneAuthHelper.sendOtp(
            activity = activity,
            phoneNumber = phoneNumber,
            forceResendingToken = resendToken,
            onCodeSent = { vId, t -> onCodeSent(vId, t) },
            onVerificationCompleted = { cred -> onVerificationCompleted(cred) },
            onVerificationFailed = onVerificationFailed
        )
    }

    suspend fun verifyOtpCode(
        context: Context,
        verificationId: String,
        otpCode: String
    ): Result<Unit> {
        return FirebasePhoneAuthHelper.verifyOtpCode(context, verificationId, otpCode)
    }
}
