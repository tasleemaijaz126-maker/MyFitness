package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode

// 1. CLASSIC THEME (Traditional High-Contrast Dark Gym)
private val ClassicColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AmberAccent,
    onSecondary = Slate950,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = Color(0xFFEF4444),
    onError = Color.White
)

// 2. MODERN THEME (Cyberpunk Deep Midnight & Electric Cyan)
private val ModernColorScheme = darkColorScheme(
    primary = ModernCyanPrimary,
    onPrimary = Color(0xFF030712),
    primaryContainer = ModernCyanDark,
    onPrimaryContainer = Color.White,
    secondary = ModernLimeAccent,
    onSecondary = Color(0xFF030712),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color.White,
    background = ModernVoidBg,
    onBackground = ModernCyberTextPrimary,
    surface = ModernCyberSurface,
    onSurface = ModernCyberTextPrimary,
    surfaceVariant = ModernCyberElevated,
    onSurfaceVariant = ModernCyberTextSecondary,
    outline = ModernCyberBorder,
    outlineVariant = Color(0xFF2A3A6A),
    error = Color(0xFFF43F5E),
    onError = Color.White
)

// 3. PREMIUM THEME (Luxury Charcoal Onyx & Champagne Gold)
private val PremiumColorScheme = darkColorScheme(
    primary = PremiumGoldPrimary,
    onPrimary = Color(0xFF18181B),
    primaryContainer = PremiumGoldDark,
    onPrimaryContainer = Color.White,
    secondary = PremiumCopperAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFF10B981),
    onTertiary = Color.White,
    background = PremiumOnyxBg,
    onBackground = PremiumIvoryTextPrimary,
    surface = PremiumCharcoalSurface,
    onSurface = PremiumIvoryTextPrimary,
    surfaceVariant = PremiumCharcoalElevated,
    onSurfaceVariant = PremiumMutedText,
    outline = PremiumGoldBorder,
    outlineVariant = Color(0xFF52525B),
    error = Color(0xFFE11D48),
    onError = Color.White
)

// 4. MINIMAL THEME (Pure Snow/Slate Canvas & Fresh Emerald)
private val MinimalColorScheme = lightColorScheme(
    primary = MinimalEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = MinimalSlateAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = MinimalCanvasBg,
    onBackground = MinimalNavyTextPrimary,
    surface = MinimalPureSurface,
    onSurface = MinimalNavyTextPrimary,
    surfaceVariant = MinimalElevatedSurface,
    onSurfaceVariant = MinimalSlateTextSecondary,
    outline = MinimalBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MyFitnessTheme(
    themeMode: AppThemeMode = AppThemeMode.CLASSIC,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.CLASSIC -> ClassicColorScheme
        AppThemeMode.MODERN -> ModernColorScheme
        AppThemeMode.PREMIUM -> PremiumColorScheme
        AppThemeMode.MINIMAL -> MinimalColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
