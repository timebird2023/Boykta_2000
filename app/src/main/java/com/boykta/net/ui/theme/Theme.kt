package com.boykta.net.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ── Deep containers derived from brand palette ────────────────────────────────
private val White             = Color(0xFFFFFFFF)
private val CyanContainer     = Color(0xFF003D50)   // deep cyan (dark mode)
private val CyanContainerL    = Color(0xFFCCEEFF)   // icy cyan (light mode)
private val RedContainer      = Color(0xFF4A0015)   // deep red (dark mode)
private val RedContainerL     = Color(0xFFFFDADA)   // pale red (light mode)
private val OnCyanContainerL  = Color(0xFF004060)
private val OnRedContainerL   = Color(0xFF7A0000)

private val DarkColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimary,
    primaryContainer     = CyanContainer,
    onPrimaryContainer   = Primary,
    secondary            = Accent,
    onSecondary          = OnPrimary,
    secondaryContainer   = RedContainer,
    onSecondaryContainer = Accent,
    background           = DarkAppColors.background,
    surface              = DarkAppColors.surface,
    surfaceVariant       = DarkAppColors.surfaceVariant,
    onBackground         = DarkAppColors.textPrimary,
    onSurface            = DarkAppColors.textPrimary,
    onSurfaceVariant     = DarkAppColors.textSecondary,
    error                = Error,
    onError              = DarkAppColors.textPrimary,
    outline              = DarkAppColors.border
)

private val LightColorScheme = lightColorScheme(
    primary              = PrimaryDark,
    onPrimary            = White,
    primaryContainer     = CyanContainerL,
    onPrimaryContainer   = OnCyanContainerL,
    secondary            = Accent,
    onSecondary          = White,
    secondaryContainer   = RedContainerL,
    onSecondaryContainer = OnRedContainerL,
    background           = LightAppColors.background,
    surface              = LightAppColors.surface,
    surfaceVariant       = LightAppColors.surfaceVariant,
    onBackground         = LightAppColors.textPrimary,
    onSurface            = LightAppColors.textPrimary,
    onSurfaceVariant     = LightAppColors.textSecondary,
    error                = Error,
    onError              = White,
    outline              = LightAppColors.border
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
