package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NasLightColorScheme = lightColorScheme(
    primary = NasIndigo,
    onPrimary = Color.White,
    primaryContainer = NasIndigoSoft,
    onPrimaryContainer = NasIndigoDark,
    secondary = NasEmerald,
    onSecondary = Color.White,
    secondaryContainer = NasEmeraldSoft,
    onSecondaryContainer = NasEmeraldDark,
    tertiary = NasAmber,
    onTertiary = Color.White,
    tertiaryContainer = NasAmberSoft,
    onTertiaryContainer = NasAmberDark,
    background = NasCanvas,
    onBackground = NasSlate800,
    surface = NasWhite,
    onSurface = NasSlate800,
    surfaceVariant = NasSurfaceVariant,
    onSurfaceVariant = NasSlate600,
    outline = NasBorder,
    error = NasCrimson,
    onError = Color.White,
    errorContainer = NasCrimsonSoft,
    onErrorContainer = NasCrimson
)

@Composable
fun NasFinanceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NasLightColorScheme,
        typography = Typography,
        content = content
    )
}
