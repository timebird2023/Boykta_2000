package com.boykta.net.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand colors extracted from the boykta net logo ──────────────────────────
// Cyan neon  : #00D4FF  (the glowing "BN" and circuit lines)
// Red neon   : #FF1744  (the outer neon ring arc)
// Dark navy  : #040D1E  (logo dark background)
val Primary      = Color(0xFF00D4FF)  // cyan neon
val PrimaryDark  = Color(0xFF0099BB)  // deeper cyan (used on light surfaces)
val OnPrimary    = Color(0xFF000000)  // black text on cyan buttons
val Accent       = Color(0xFFFF1744)  // red neon accent (alerts, badges)
val Success      = Color(0xFF00E676)  // neon green
val Error        = Color(0xFFFF1744)  // same red
val Warning      = Color(0xFFFFAB00)  // amber

// ── AppColors holder ──────────────────────────────────────────────────────────
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

// ── Dark theme — mirrors the logo's black/navy canvas with cyan glow ──────────
val DarkAppColors = AppColors(
    background     = Color(0xFF000000),   // pure black (logo outer bg)
    surface        = Color(0xFF060C18),   // deep navy (logo inner field)
    surfaceVariant = Color(0xFF0D1625),   // slightly lighter navy
    cardBg         = Color(0xFF091020),   // card background
    border         = Color(0xFF1A2A45),   // subtle navy border
    textPrimary    = Color(0xFFFFFFFF),   // white
    textSecondary  = Color(0xFF00D4FF),   // cyan — for subtitles / labels
    textHint       = Color(0xFF3A5070),   // muted navy-grey hint
    isDark         = true
)

// ── Light theme — icy white palette derived from brand cyan ──────────────────
val LightAppColors = AppColors(
    background     = Color(0xFFF0F8FF),   // alice blue (icy daylight)
    surface        = Color(0xFFFFFFFF),   // pure white cards
    surfaceVariant = Color(0xFFE3F3FF),   // very light cyan tint
    cardBg         = Color(0xFFFFFFFF),
    border         = Color(0xFFB0D8F0),   // light cyan border
    textPrimary    = Color(0xFF071525),   // near-black navy
    textSecondary  = Color(0xFF005577),   // darker cyan for readability
    textHint       = Color(0xFF90B8D0),   // muted cyan hint
    isDark         = false
)

// ── CompositionLocal ──────────────────────────────────────────────────────────
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ── @Composable color accessors ───────────────────────────────────────────────
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
