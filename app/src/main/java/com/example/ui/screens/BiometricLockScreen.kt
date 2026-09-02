package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.entity.GymSetting
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.GymViewModel
import com.example.util.BiometricAuthManager
import com.example.util.BiometricResult
import com.example.util.BiometricStatus

@Composable
fun BiometricLockScreen(
    viewModel: GymViewModel,
    gymSetting: GymSetting
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricStatus(context) }
    val isBiometricEnabled = viewModel.securityManager.isBiometricEnabled()
    val isPinConfigured = viewModel.securityManager.isPinConfigured() || gymSetting.securityPin.isNotBlank()

    // Prevent bypassing lock screen with the Android Back button
    BackHandler {
        activity?.moveTaskToBack(true)
    }

    var lockMessage by remember {
        mutableStateOf(
            if (isBiometricEnabled && biometricStatus.isAvailable)
                "Touch sensor or tap unlock to continue"
            else if (isPinConfigured)
                "Enter 4-digit Passcode to unlock"
            else
                "Tap unlock to continue"
        )
    }
    var isPinMode by remember {
        mutableStateOf(!isBiometricEnabled || !biometricStatus.isAvailable)
    }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Pulse animation for biometric icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun launchBiometricPrompt() {
        if (activity == null) {
            if (isPinConfigured) {
                isPinMode = true
                lockMessage = "Enter 4-digit Passcode"
            }
            return
        }

        BiometricAuthManager.promptBiometric(
            activity = activity,
            title = "${gymSetting.gymName.ifBlank { "My Fitness" }} Security",
            subtitle = "Verify fingerprint or face to access ERP",
            description = "Owner security layer active",
            negativeButtonText = if (isPinConfigured) "Use PIN" else "Cancel",
            onResult = { result ->
                when (result) {
                    is BiometricResult.Success -> {
                        viewModel.unlockApp()
                    }
                    is BiometricResult.Failed -> {
                        lockMessage = "Biometric not recognized. Please try again."
                    }
                    is BiometricResult.Cancelled -> {
                        if (isPinConfigured) {
                            isPinMode = true
                            lockMessage = "Enter 4-digit Passcode"
                        } else {
                            lockMessage = "Authentication cancelled. Tap to retry."
                        }
                    }
                    is BiometricResult.Error -> {
                        if (isPinConfigured) {
                            isPinMode = true
                            lockMessage = "Enter 4-digit Passcode"
                        } else {
                            lockMessage = result.message
                        }
                    }
                }
            }
        )
    }

    // Auto-prompt on launch if biometrics are enabled and available
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && biometricStatus.isAvailable) {
            launchBiometricPrompt()
        } else if (isPinConfigured) {
            isPinMode = true
            lockMessage = "Enter your 4-digit Security PIN"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("biometric_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Gym Branding & Security Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = gymSetting.gymName.ifBlank { "My Fitness ERP" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = gymSetting.gymTagline.ifBlank { "Secure Gym Management Platform" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Security Enclave Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "OWNER BIOMETRIC LOCK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Middle: Biometric Touch Target or PIN Keypad
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPinMode) {
                        // Biometric Mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .clickable { launchBiometricPrompt() }
                                    .testTag("biometric_fingerprint_touch_target")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.size(84.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint Sensor",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "App Locked",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = lockMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { launchBiometricPrompt() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("biometric_unlock_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Biometrics", fontWeight = FontWeight.Bold)
                            }

                            if (isPinConfigured) {
                                Spacer(modifier = Modifier.height(10.dp))
                                TextButton(
                                    onClick = {
                                        isPinMode = true
                                        enteredPin = ""
                                        pinError = false
                                        lockMessage = "Enter 4-digit Passcode"
                                    },
                                    modifier = Modifier.testTag("switch_to_pin_button")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enter PIN Instead", fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        // PIN Mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enter 4-Digit Passcode",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // PIN Dots indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = enteredPin.length > i
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    pinError -> CrimsonPrimary
                                                    isFilled -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .border(
                                                1.5.dp,
                                                if (pinError) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            if (pinError) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Incorrect PIN. Try again.",
                                    color = CrimsonPrimary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 3x4 Numeric Keypad
                            val keys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf(if (isBiometricEnabled && biometricStatus.isAvailable) "BIO" else "", "0", "DEL")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                keys.forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.weight(0.1f))
                                        row.forEach { digit ->
                                            if (digit.isEmpty()) {
                                                Spacer(modifier = Modifier.size(64.dp))
                                            } else {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            when (digit) {
                                                                "BIO" -> {
                                                                    isPinMode = false
                                                                    enteredPin = ""
                                                                    pinError = false
                                                                    launchBiometricPrompt()
                                                                }
                                                                "DEL" -> {
                                                                    if (enteredPin.isNotEmpty()) {
                                                                        enteredPin = enteredPin.dropLast(1)
                                                                        pinError = false
                                                                    }
                                                                }
                                                                else -> {
                                                                    if (enteredPin.length < 4) {
                                                                        val next = enteredPin + digit
                                                                        enteredPin = next
                                                                        if (next.length == 4) {
                                                                            if (viewModel.verifySecurityPin(next)) {
                                                                                viewModel.unlockApp()
                                                                            } else {
                                                                                pinError = true
                                                                                enteredPin = ""
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .testTag("pin_key_$digit"),
                                                    shape = CircleShape,
                                                    color = when (digit) {
                                                        "BIO" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        "DEL" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        when (digit) {
                                                            "BIO" -> Icon(
                                                                Icons.Default.Fingerprint,
                                                                contentDescription = "Switch to biometric",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            "DEL" -> Icon(
                                                                Icons.Default.Backspace,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            else -> Text(
                                                                text = digit,
                                                                style = MaterialTheme.typography.titleLarge.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 22.sp
                                                                ),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight(0.1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom: Sign Out / Switch Account Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            viewModel.signOut()
                        },
                        modifier = Modifier.testTag("biometric_sign_out_button")
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign Out from Account",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}
