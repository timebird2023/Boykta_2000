package com.boykta.net.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryDark,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = Error,
    onError          = TextPrimary,
    outline          = Border
)

@Composable
fun BoykataNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
