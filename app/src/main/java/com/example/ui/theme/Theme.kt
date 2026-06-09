package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SchoolBusGold,
    secondary = SchoolBusGoldDark,
    tertiary = StatusBlue,
    background = SlateDarkNavy,
    surface = DeepCharcoal,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = LightSlateSurface,
    onSurfaceVariant = PureWhite
)

// Inline simple light colors to avoid extra file clutter
private val ColorBackgroundLight = androidx.compose.ui.graphics.Color(0xFFF3F4F9)
private val ColorSurfaceLight = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val ColorSurfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFE2E8F0)

private val LightColorScheme = lightColorScheme(
    primary = SchoolBusGoldDark,
    secondary = DeepCharcoal,
    tertiary = StatusBlue,
    background = ColorBackgroundLight,
    surface = ColorSurfaceLight,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = SlateDarkNavy,
    onSurface = SlateDarkNavy,
    surfaceVariant = ColorSurfaceVariantLight,
    onSurfaceVariant = DeepCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable to enforce consistent transportation branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
