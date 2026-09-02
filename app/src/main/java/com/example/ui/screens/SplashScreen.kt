package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.delay

/**
 * Premium Modern Fitness App Launch Splash & Loading Screen.
 * Displays the centered MyFitness logo inside a clean circular container
 * enveloped by a smooth, high-frame-rate rotating circular loading ring.
 */
@Composable
fun SplashScreen(
    isReadyToTransition: Boolean,
    onSplashFinished: () -> Unit
) {
    // Entrance animations
    val scaleAnim = remember { Animatable(0.82f) }
    val alphaAnim = remember { Animatable(0f) }

    // Minimum display timer to guarantee a smooth, non-jarring launch experience
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = LinearEasing)
        )
    }

    // Handle transition with smooth min duration (1.2s) and max timeout safety (4.0s)
    LaunchedEffect(isReadyToTransition) {
        val startTime = System.currentTimeMillis()
        val minDuration = 1200L
        val maxTimeout = 4000L

        // Wait until ready or until timeout
        while (!isReadyToTransition && (System.currentTimeMillis() - startTime) < maxTimeout) {
            delay(50L)
        }

        // Ensure minimum pleasant viewing duration has passed
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < minDuration) {
            delay(minDuration - elapsed)
        }

        onSplashFinished()
    }

    // Infinite rotation for the circular loading ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Subtle breathing pulse for background ambiance
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val backgroundGradient = Brush.radialGradient(
        colors = listOf(
            CrimsonPrimary.copy(alpha = 0.15f),
            Color(0xFF0F172A).copy(alpha = 0.95f),
            Color(0xFF050811)
        ),
        radius = 800f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow centered behind the logo
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .scale(pulseScale)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonPrimary.copy(alpha = 0.35f * alphaAnim.value),
                        Color(0xFFE11D48).copy(alpha = 0.12f * alphaAnim.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // Main Animated Container (Logo + Circular Loading Ring)
        Box(
            modifier = Modifier
                .size(176.dp)
                .scale(scaleAnim.value),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Circular Loading Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val arcCenter = Offset(size.width / 2f, size.height / 2f)

                // Background track ring (subtle translucent)
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f * alphaAnim.value),
                    radius = radius,
                    center = arcCenter,
                    style = Stroke(width = strokeWidth)
                )

                // Active spinning gradient arc
                rotate(degrees = rotationAngle, pivot = arcCenter) {
                    val arcBrush = Brush.sweepGradient(
                        0.0f to CrimsonPrimary.copy(alpha = 0.1f),
                        0.5f to CrimsonPrimary,
                        1.0f to Color(0xFFFDA4AF)
                    )

                    drawArc(
                        brush = arcBrush,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - strokeWidth,
                            size.height - strokeWidth
                        ),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Inner Circular Logo Container
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                CrimsonPrimary.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gym_logo),
                    contentDescription = "MyFitness Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
    }
}
