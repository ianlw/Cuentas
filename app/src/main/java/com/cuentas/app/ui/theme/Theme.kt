package com.cuentas.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.cuentas.app.data.preferences.ThemeMode

// ─── Color Scheme Definitions ────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = DarkPrimary,
    onPrimary          = DarkOnPrimary,
    primaryContainer   = DarkPrimaryVar,
    secondary          = DarkSecondary,
    onSecondary        = Color.Black,
    tertiary           = DarkAccent,
    background         = DarkBackground,
    onBackground       = DarkOnBackground,
    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurfaceVar,
    outline            = DarkGlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary            = LightPrimary,
    onPrimary          = LightOnPrimary,
    primaryContainer   = LightPrimaryVar,
    secondary          = LightSecondary,
    onSecondary        = Color.White,
    tertiary           = LightAccent,
    background         = LightBackground,
    onBackground       = LightOnBackground,
    surface            = LightSurface,
    onSurface          = LightOnSurface,
    surfaceVariant     = LightSurfaceVar,
    outline            = LightGlassBorder
)

// ─── Glass Defaults per Theme ─────────────────────────────────────────────────

data class GlassConfig(
    val background: Color,
    val border: Color,
    val highlight: Color,
    val isDark: Boolean
)

val LocalGlass = compositionLocalOf {
    GlassConfig(DarkGlass, DarkGlassBorder, DarkGlassHighlight, true)
}

// ─── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun CuentasTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    customColors: Triple<Int, Int, Int> = Triple(
        0xFF7C3AED.toInt(), 0xFF06B6D4.toInt(), 0xFFF59E0B.toInt()
    ),
    content: @Composable () -> Unit
) {
    val isDark = themeMode != ThemeMode.LIGHT

    val colorScheme = when (themeMode) {
        ThemeMode.DARK   -> DarkColorScheme
        ThemeMode.LIGHT  -> LightColorScheme
        ThemeMode.CUSTOM -> {
            val c1 = Color(customColors.first)
            val c2 = Color(customColors.second)
            darkColorScheme(
                primary          = c1,
                onPrimary        = Color.White,
                primaryContainer = c1.copy(alpha = 0.7f),
                secondary        = c2,
                onSecondary      = Color.White,
                tertiary         = Color(customColors.third),
                background       = DarkBackground,
                onBackground     = DarkOnBackground,
                surface          = DarkSurface,
                onSurface        = DarkOnSurface,
                surfaceVariant   = DarkSurfaceVar,
                outline          = c1.copy(alpha = 0.4f)
            )
        }
    }

    val glassConfig = if (isDark) {
        GlassConfig(DarkGlass, DarkGlassBorder, DarkGlassHighlight, true)
    } else {
        GlassConfig(LightGlass, LightGlassBorder, LightGlassHighlight, false)
    }

    CompositionLocalProvider(LocalGlass provides glassConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content
        )
    }
}
