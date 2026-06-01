package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CinemaGold,
    secondary = ElectricCyan,
    tertiary = ActiveRed,
    background = MidnightNavy,
    surface = SteelSlate,
    surfaceVariant = SoftGrey,
    onPrimary = MidnightNavy,
    onSecondary = MidnightNavy,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
