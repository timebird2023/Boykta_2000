package com.boykta.net.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand palette — extracted from boykta net neon logo ──────────────────────
val Primary      = Color(0xFF00D4FF)  // electric cyan  (BN letters glow)
val PrimaryDark  = Color(0xFF00A3CC)  // deeper cyan for light surfaces
val OnPrimary    = Color(0xFF000000)  // black text on cyan buttons
val Accent       = Color(0xFFFF1744)  // red neon ring arc
val NeonBlue     = Color(0xFF0D6EFD)  // electric blue (circuit board lines)
val Success      = Color(0xFF00E676)  // neon green
val Error        = Color(0xFFFF1744)  // same red as accent
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

// ── DARK theme — matches the icon's deep navy-black canvas with neon glow ─────
val DarkAppColors = AppColors(
    background     = Color(0xFF05091A),  // deep navy-black (icon outer field)
    surface        = Color(0xFF080E22),  // slightly lighter navy
    surfaceVariant = Color(0xFF0D1530),  // card variant
    cardBg         = Color(0xFF0A1228),  // cards
    border         = Color(0xFF162040),  // subtle navy border with blue tint
    textPrimary    = Color(0xFFECF4FF),  // almost white with cool blue tint
    textSecondary  = Color(0xFF7BBFDA),  // muted cyan-grey
    textHint       = Color(0xFF2C4060),  // dim navy hint
    isDark         = true
)

// ── LIGHT theme — vivid arctic palette: deep navy text, stronger cyan/red contrast ──
val LightAppColors = AppColors(
    background     = Color(0xFFE8F2FF),  // crisp ice-blue canvas (more saturated)
    surface        = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD0E6FF),  // deeper cyan tint for cards & rows
    cardBg         = Color(0xFFFFFFFF),
    border         = Color(0xFF7AAFD4),  // stronger cyan border — clearly visible
    textPrimary    = Color(0xFF040C1E),  // near-black navy — max contrast
    textSecondary  = Color(0xFF1A4060),  // deep slate-cyan — readable subtitle
    textHint       = Color(0xFF6890AA),  // medium-blue hint — not washed out
    isDark         = false
)

// ── CompositionLocal ──────────────────────────────────────────────────────────
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ── @Composable color accessors ───────────────────────────────────────────────
val Background: Color    @Composable get() = LocalAppColors.current.background
val Surface: Color       @Composable get() = LocalAppColors.current.surface
val SurfaceVariant: Color @Composable get() = LocalAppColors.current.surfaceVariant
val CardBg: Color        @Composable get() = LocalAppColors.current.cardBg
val Border: Color        @Composable get() = LocalAppColors.current.border
val TextPrimary: Color   @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val TextHint: Color      @Composable get() = LocalAppColors.current.textHint
