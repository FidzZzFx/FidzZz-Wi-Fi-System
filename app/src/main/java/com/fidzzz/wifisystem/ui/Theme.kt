package com.fidzzz.wifisystem.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) { DARK("Dark"), LIGHT("Light"), SYSTEM("System") }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2F8BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF6FB1FF),
    background = Color(0xFF080B12),
    onBackground = Color(0xFFE8EDF7),
    surface = Color(0xFF0F1521),
    onSurface = Color(0xFFE8EDF7),
    surfaceVariant = Color(0xFF172033),
    onSurfaceVariant = Color(0xFFA9B4C9),
    outline = Color(0xFF2A3550)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0A6CE8),
    onPrimary = Color.White,
    background = Color(0xFFF3F6FB),
    onBackground = Color(0xFF0B1220),
    surface = Color.White,
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE6ECF6),
    onSurfaceVariant = Color(0xFF4A5670),
    outline = Color(0xFFB8C3D8)
)

@Composable
fun FidzzTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = Typography(), content = content)
}
