package com.boykta.net.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryDark,
    background       = DarkAppColors.background,
    surface          = DarkAppColors.surface,
    surfaceVariant   = DarkAppColors.surfaceVariant,
    onBackground     = DarkAppColors.textPrimary,
    onSurface        = DarkAppColors.textPrimary,
    onSurfaceVariant = DarkAppColors.textSecondary,
    error            = Error,
    onError          = DarkAppColors.textPrimary,
    outline          = DarkAppColors.border
)

private val LightColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryDark,
    background       = LightAppColors.background,
    surface          = LightAppColors.surface,
    surfaceVariant   = LightAppColors.surfaceVariant,
    onBackground     = LightAppColors.textPrimary,
    onSurface        = LightAppColors.textPrimary,
    onSurfaceVariant = LightAppColors.textSecondary,
    error            = Error,
    onError          = LightAppColors.textPrimary,
    outline          = LightAppColors.border
)

@Composable
fun BoykataNetTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val appColors   = if (darkTheme) DarkAppColors   else LightAppColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            content     = content
        )
    }
}
