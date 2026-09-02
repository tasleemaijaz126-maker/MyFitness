package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.util.PhoneAuthHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpVerificationDialog(
    mobileNumber: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<Any?>(null) }

    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Sending verification code...") }
    var isSuccess by remember { mutableStateOf(false) }

    var resendCooldown by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }

    val formattedNumber = remember(mobileNumber) {
        PhoneAuthHelper.formatDisplayNumber(mobileNumber)
    }

    // Function to trigger real OTP sending
    fun initiateSendOtp() {
        if (activity == null) {
            errorMessage = "Unable to start verification: Activity context not available."
            return
        }
        isSendingOtp = true
        errorMessage = null
        statusMessage = "Sending real SMS OTP to $formattedNumber..."

        PhoneAuthHelper.sendOtp(
            activity = activity,
            phoneNumber = mobileNumber,
            onCodeSent = { vId, token ->
                verificationId = vId
                resendToken = token
                isSendingOtp = false
                statusMessage = "OTP sent successfully! Please enter the 6-digit code from SMS."
                resendCooldown = 60
                isTimerRunning = true
            },
            onVerificationCompleted = { credential ->
                isSendingOtp = false
                isVerifyingOtp = false
                isSuccess = true
                statusMessage = "Phone number verified automatically via SMS!"
                scope.launch {
                    delay(1200)
                    onVerified()
                }
            },
            onVerificationFailed = { error ->
                isSendingOtp = false
                errorMessage = error
                statusMessage = ""
            }
        )
    }

    // Function to resend OTP
    fun initiateResendOtp() {
        if (activity == null || resendCooldown > 0) return
        isSendingOtp = true
        errorMessage = null
        statusMessage = "Resending OTP SMS to $formattedNumber..."

        PhoneAuthHelper.resendOtp(
            activity = activity,
            phoneNumber = mobileNumber,
            token = resendToken,
            onCodeSent = { vId, token ->
                verificationId = vId
                resendToken = token
                isSendingOtp = false
                otpCode = ""
                statusMessage = "A fresh SMS OTP has been sent."
                resendCooldown = 60
                isTimerRunning = true
            },
            onVerificationCompleted = { credential ->
                isSendingOtp = false
                isVerifyingOtp = false
                isSuccess = true
                statusMessage = "Phone number verified automatically via SMS!"
                scope.launch {
                    delay(1200)
                    onVerified()
                }
            },
            onVerificationFailed = { error ->
                isSendingOtp = false
                errorMessage = error
                statusMessage = ""
            }
        )
    }

    // Function to verify entered OTP
    fun submitOtpVerification() {
        if (otpCode.length != 6) {
            errorMessage = "Please enter all 6 digits of the verification code."
            return
        }
        if (isVerifyingOtp) return

        isVerifyingOtp = true
        errorMessage = null

        scope.launch {
            val result = PhoneAuthHelper.verifyOtpCode(
                context = context,
                verificationId = verificationId,
                otpCode = otpCode
            )
            isVerifyingOtp = false
            if (result.isSuccess) {
                isSuccess = true
                statusMessage = "Mobile number verified successfully!"
                delay(1000)
                onVerified()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage
                    ?: "Incorrect OTP. Please check the code and try again."
            }
        }
    }

    // Send OTP on initial launch
    LaunchedEffect(Unit) {
        initiateSendOtp()
        delay(300)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Resend countdown timer
    LaunchedEffect(isTimerRunning, resendCooldown) {
        if (isTimerRunning && resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        } else if (resendCooldown <= 0) {
            isTimerRunning = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isVerifyingOtp) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isVerifyingOtp,
            dismissOnClickOutside = !isVerifyingOtp,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("otp_verification_dialog"),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        EmeraldSuccess.copy(alpha = 0.5f),
                        Color(0xFF1E293B),
                        CrimsonPrimary.copy(alpha = 0.3f)
                    )
                ),
                width = 1.2.dp
            ),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSuccess) EmeraldSuccess.copy(alpha = 0.2f)
                                    else EmeraldSuccess.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Verify Mobile Number",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Customer Phone Verification",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isVerifyingOtp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Info Banner with Formatted Mobile Number
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS OTP will be sent to:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedNumber,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = EmeraldSuccess
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6-Digit Visual OTP Display Input Boxes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Hidden BasicTextField capturing keyboard inputs
                    BasicTextField(
                        value = otpCode,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(6)
                            otpCode = clean
                            errorMessage = null
                            if (clean.length == 6) {
                                // Auto verify when 6th digit is typed
                                submitOtpVerification()
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitOtpVerification() }
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .testTag("otp_input_field")
                            .size(1.dp), // Tiny invisible field
                        singleLine = true
                    )

                    // 6 Visual Digit Cells
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val digit = otpCode.getOrNull(i)?.toString() ?: ""
                            val isFocused = otpCode.length == i || (otpCode.length == 6 && i == 5)

                            val borderColor = when {
                                isSuccess -> EmeraldSuccess
                                errorMessage != null -> CrimsonPrimary
                                isFocused -> EmeraldSuccess
                                digit.isNotEmpty() -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            }

                            val cellBg = when {
                                isSuccess -> EmeraldSuccess.copy(alpha = 0.1f)
                                isFocused -> EmeraldSuccess.copy(alpha = 0.08f)
                                digit.isNotEmpty() -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellBg)
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    color = if (isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Message or Status Message
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonPrimary.copy(alpha = 0.12f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CrimsonPrimary.copy(alpha = 0.35f))),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = CrimsonPrimary,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                } else if (statusMessage.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSendingOtp || isVerifyingOtp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resend OTP Row with 30s Countdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resendCooldown > 0) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resend OTP in ${resendCooldown}s",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = AmberAccent
                        )
                    } else {
                        TextButton(
                            onClick = { initiateResendOtp() },
                            enabled = !isSendingOtp && !isVerifyingOtp,
                            modifier = Modifier.testTag("resend_otp_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Didn't receive OTP? Resend OTP",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Verify Button
                Button(
                    onClick = { submitOtpVerification() },
                    enabled = otpCode.length == 6 && !isVerifyingOtp && !isSendingOtp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_otp_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) EmeraldSuccess else EmeraldSuccess,
                        disabledContainerColor = EmeraldSuccess.copy(alpha = 0.3f)
                    )
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else if (isSuccess) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✓ Mobile Number Verified", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify OTP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isVerifyingOtp
                ) {
                    Text(
                        text = "Cancel / Back to Form",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
