package com.boykta.net.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ── Deep containers derived from brand palette ────────────────────────────────
private val CyanContainer      = Color(0xFF003D50)   // deep cyan (dark)
private val CyanContainerLight = Color(0xFFCCEEFF)   // icy cyan (light)
private val RedContainer       = Color(0xFF4A0015)   // deep red (dark)
private val RedContainerLight  = Color(0xFFFFDADA)   // pale red (light)
private val OnCyanContainerL   = Color(0xFF004060)
private val OnRedContainerL    = Color(0xFF7A0000)

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
    onPrimary            = Color.White,
    primaryContainer     = CyanContainerLight,
    onPrimaryContainer   = OnCyanContainerL,
    secondary            = Accent,
    onSecondary          = Color.White,
    secondaryContainer   = RedContainerLight,
    onSecondaryContainer = OnRedContainerL,
    background           = LightAppColors.background,
    surface              = LightAppColors.surface,
    surfaceVariant       = LightAppColors.surfaceVariant,
    onBackground         = LightAppColors.textPrimary,
    onSurface            = LightAppColors.textPrimary,
    onSurfaceVariant     = LightAppColors.textSecondary,
    error                = Error,
    onError              = Color.White,
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
