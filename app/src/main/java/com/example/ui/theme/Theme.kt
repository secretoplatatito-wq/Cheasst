package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CheasstDarkColorScheme = darkColorScheme(
    primary = TacticalEmerald,
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005328),
    onPrimaryContainer = Color(0xFF88FFAA),
    secondary = CyberCyan,
    onSecondary = Color(0xFF00363E),
    secondaryContainer = Color(0xFF004E58),
    onSecondaryContainer = Color(0xFF8CF4FF),
    tertiary = TacticalAmber,
    onTertiary = Color(0xFF3F3000),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ElevatedSlate,
    onSurface = TextPrimary,
    surfaceVariant = CardBorderSlate,
    onSurfaceVariant = TextSecondary,
    error = TacticalRed,
    onError = Color.White
)

@Composable
fun CheasstTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CheasstDarkColorScheme,
        typography = Typography,
        content = content
    )
}
