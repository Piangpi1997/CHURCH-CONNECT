package org.cmf.churchconnect.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LocalGlassDarkMode = staticCompositionLocalOf { false }

/** Shared geometry, spacing, elevation, and motion values for the CMF glass UI. */
object GlassTokens {
    val radiusSmall = 12.dp
    val radiusMedium = 18.dp
    val radiusLarge = 24.dp
    val radiusExtraLarge = 30.dp
    val screenPadding = 18.dp
    val spacingSmall = 8.dp
    val spacingCompact = 12.dp
    val spacingMedium = 14.dp
    val spacingLarge = 20.dp
    val minimumTouchTarget = 48.dp
    val contentMaxWidth = 760.dp
    val authMaxWidth = 560.dp
    val borderWidth = 1.dp
    val elevationLow = 2.dp
    val elevationCard = 5.dp
    const val pressMotionMillis = 120
    const val screenMotionMillis = 220

    val heroHighlight = Color(0xFFD7F2FF)
    val heroSecondaryText = Color(0xFFD7E8F6)
}

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF86D2FF),
            onPrimary = Color(0xFF00263D),
            primaryContainer = Color(0xFF173D5C),
            onPrimaryContainer = Color(0xFFD5EEFF),
            secondary = Color(0xFF6ED6E2),
            onSecondary = Color(0xFF00363D),
            secondaryContainer = Color(0xFF12454C),
            onSecondaryContainer = Color(0xFFBDECF0),
            tertiary = Color(0xFFB3C8E9),
            onTertiary = Color(0xFF1A304A),
            tertiaryContainer = Color(0xFF354C68),
            onTertiaryContainer = Color(0xFFE0ECFF),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF0B1524),
            onBackground = Color(0xFFE8F0FA),
            surface = Color(0xFF14243A),
            onSurface = Color(0xFFE8F0FA),
            surfaceVariant = Color(0xFF22364E),
            onSurfaceVariant = Color(0xFFB5C5D8),
            outline = Color(0xFF627A94),
            outlineVariant = Color(0xFF3A5069),
            surfaceTint = Color(0xFF86D2FF)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF075C96),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD5EDFF),
            onPrimaryContainer = Color(0xFF062E4A),
            secondary = Color(0xFF007A8C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFC8F1F4),
            onSecondaryContainer = Color(0xFF00363E),
            tertiary = Color(0xFF4B6280),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDCE8F7),
            onTertiaryContainer = Color(0xFF23364E),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFEEF4FA),
            onBackground = Color(0xFF13253B),
            surface = Color(0xFFF9FCFF),
            onSurface = Color(0xFF13253B),
            surfaceVariant = Color(0xFFE3EDF6),
            onSurfaceVariant = Color(0xFF4B6077),
            outline = Color(0xFF8298AE),
            outlineVariant = Color(0xFFC5D3E0),
            surfaceTint = Color(0xFF075C96)
        )
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(GlassTokens.radiusSmall),
        small = RoundedCornerShape(GlassTokens.radiusSmall),
        medium = RoundedCornerShape(GlassTokens.radiusMedium),
        large = RoundedCornerShape(GlassTokens.radiusLarge),
        extraLarge = RoundedCornerShape(GlassTokens.radiusExtraLarge)
    )
    CompositionLocalProvider(LocalGlassDarkMode provides dark) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), shapes = shapes, content = content)
    }
}

@Composable
internal fun isGlassDarkMode(): Boolean = LocalGlassDarkMode.current
