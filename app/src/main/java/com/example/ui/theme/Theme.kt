package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = PosIndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = PosTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = PosDarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    background = PosDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = PosDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = PosDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = PosDarkOutline,
    error = PosRedError
)

private val LightColorScheme = lightColorScheme(
    primary = PosIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = PosIndigoContainer,
    onPrimaryContainer = PosIndigoOnContainer,
    secondary = PosTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = PosTealContainer,
    onSecondaryContainer = PosTealOnContainer,
    background = PosBackground,
    onBackground = PosTextPrimary,
    surface = PosSurface,
    onSurface = PosTextPrimary,
    surfaceVariant = PosSurfaceVariant,
    onSurfaceVariant = PosTextSecondary,
    outline = PosOutline,
    error = PosRedError
)

@Composable
fun MikroTikPosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MikroTikPosTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

