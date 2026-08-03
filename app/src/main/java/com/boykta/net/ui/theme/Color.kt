package com.boykta.net.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand / semantic colors — identical in both themes ───────────────────────
val Primary     = Color(0xFF00D4FF)
val PrimaryDark = Color(0xFF0099BB)
val OnPrimary   = Color(0xFF000000)
val Success     = Color(0xFF00C853)
val Error       = Color(0xFFFF3B30)
val Warning     = Color(0xFFFF9500)

// ── AppColors holder ─────────────────────────────────────────────────────────

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBg: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background    = Color(0xFF000000),
    surface       = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFF1A1A1A),
    cardBg        = Color(0xFF111111),
    border        = Color(0xFF2A2A2A),
    textPrimary   = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    textHint      = Color(0xFF555555),
    isDark        = true
)

val LightAppColors = AppColors(
    background    = Color(0xFFF2F4F7),
    surface       = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1F5),
    cardBg        = Color(0xFFFFFFFF),
    border        = Color(0xFFD8DCE3),
    textPrimary   = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    textHint      = Color(0xFFB0B7C3),
    isDark        = false
)

// ── CompositionLocal — provides the active color set to every composable ──────
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ── @Composable color accessors ───────────────────────────────────────────────
// All screens use: Background, CardBg, TextPrimary, etc.
// They automatically resolve to dark or light based on the current theme.

val Background: Color
    @Composable get() = LocalAppColors.current.background

val Surface: Color
    @Composable get() = LocalAppColors.current.surface

val SurfaceVariant: Color
    @Composable get() = LocalAppColors.current.surfaceVariant

val CardBg: Color
    @Composable get() = LocalAppColors.current.cardBg

val Border: Color
    @Composable get() = LocalAppColors.current.border

val TextPrimary: Color
    @Composable get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable get() = LocalAppColors.current.textSecondary

val TextHint: Color
    @Composable get() = LocalAppColors.current.textHint
