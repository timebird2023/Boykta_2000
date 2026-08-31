package com.boykta.net.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand Palette — Direct match to boykta net Neon Bot & Ring Logo ──────────
val Primary        = Color(0xFF00E5FF)  // Electric Neon Cyan glow (Center BN & Bot)
val PrimaryDark    = Color(0xFF008BB8)  // Saturated Cyan for light surfaces / deep accents
val OnPrimary      = Color(0xFF001420)  // Deepest Navy text on Neon Cyan buttons
val Accent         = Color(0xFFFF2A54)  // Outer Neon Red / Crimson Ring Glow
val AccentRed      = Color(0xFFFF1744)  // Vivid Crimson
val NeonBlue       = Color(0xFF1E88E5)  // Cyber Circuit Blue
val Success        = Color(0xFF00E676)  // Flexy Neon Green Glow
val Error          = Color(0xFFFF2A54)  // Neon Red Alert
val Warning        = Color(0xFFFFB300)  // Vivid Gold / Amber

// ── AppColors Design Token Container ─────────────────────────────────────────
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

// ── DARK Theme — Deep Space / Cyber Navy Canvas with Neon Highlights ────────
val DarkAppColors = AppColors(
    background     = Color(0xFF070B14),  // Deep space midnight
    surface        = Color(0xFF0E1424),  // Dark cyber slate surface
    surfaceVariant = Color(0xFF141C30),  // Elevated container
    cardBg         = Color(0xFF0F172B),  // High-tech card background
    border         = Color(0xFF1E2B47),  // Crisp cyber blue border
    textPrimary    = Color(0xFFF0F6FF),  // Brilliant ice-white
    textSecondary  = Color(0xFF86A0C8),  // Luminous slate cyan
    textHint       = Color(0xFF435B80),  // Subdued navy hint
    isDark         = true
)

// ── LIGHT Theme — Modern High-Contrast Ice & Slate with Vivid Neon Accents ───
val LightAppColors = AppColors(
    background     = Color(0xFFF2F6FC),  // Crisp ultra-clean canvas
    surface        = Color(0xFFFFFFFF),  // Pure white card
    surfaceVariant = Color(0xFFE5EEF8),  // Soft cool-blue container
    cardBg         = Color(0xFFFFFFFF),  // Pure white
    border         = Color(0xFFC4D6EB),  // Clear contrast divider
    textPrimary    = Color(0xFF0A1326),  // Deep near-black navy for perfect readability
    textSecondary  = Color(0xFF324D6D),  // Deep slate navy
    textHint       = Color(0xFF6B87A8),  // Visible secondary hint
    isDark         = false
)

// ── CompositionLocal Accessor ────────────────────────────────────────────────
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ── Convenient @Composable Accessors ─────────────────────────────────────────
val Background: Color    @Composable get() = LocalAppColors.current.background
val Surface: Color       @Composable get() = LocalAppColors.current.surface
val SurfaceVariant: Color @Composable get() = LocalAppColors.current.surfaceVariant
val CardBg: Color        @Composable get() = LocalAppColors.current.cardBg
val Border: Color        @Composable get() = LocalAppColors.current.border
val TextPrimary: Color   @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val TextHint: Color      @Composable get() = LocalAppColors.current.textHint
