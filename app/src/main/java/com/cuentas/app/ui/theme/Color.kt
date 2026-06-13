package com.cuentas.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Dark Mode Palette ───────────────────────────────────────────────────────
val DarkBackground    = Color(0xFF080818)
val DarkSurface       = Color(0xFF111128)
val DarkSurfaceVar    = Color(0xFF1A1A35)
val DarkPrimary       = Color(0xFF9D5CFF)   // Vivid purple
val DarkPrimaryVar    = Color(0xFF7C3AED)
val DarkSecondary     = Color(0xFF22D3EE)   // Cyan
val DarkAccent        = Color(0xFFF59E0B)   // Amber
val DarkOnPrimary     = Color(0xFFFFFFFF)
val DarkOnBackground  = Color(0xFFE8E8FF)
val DarkOnSurface     = Color(0xFFCDCDEE)
val DarkGlass         = Color(0x1AFFFFFF)   // 10% white
val DarkGlassBorder   = Color(0x33FFFFFF)   // 20% white
val DarkGlassHighlight = Color(0x0DFFFFFF)  // 5% white

// ─── Light Mode Palette ──────────────────────────────────────────────────────
val LightBackground   = Color(0xFFEEF0FF)
val LightSurface      = Color(0xFFF8F9FF)
val LightSurfaceVar   = Color(0xFFECEEFF)
val LightPrimary      = Color(0xFF6D28D9)
val LightPrimaryVar   = Color(0xFF5B21B6)
val LightSecondary    = Color(0xFF0891B2)
val LightAccent       = Color(0xFFD97706)
val LightOnPrimary    = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1A1A35)
val LightOnSurface    = Color(0xFF2D2D50)
val LightGlass        = Color(0x80FFFFFF)   // 50% white
val LightGlassBorder  = Color(0xCCFFFFFF)   // 80% white
val LightGlassHighlight = Color(0x40FFFFFF) // 25% white

// ─── Category Colors ─────────────────────────────────────────────────────────
val CategoryColors = mapOf(
    "Comida"          to Color(0xFFFF6B6B),
    "Transporte"      to Color(0xFF4ECDC4),
    "Entretenimiento" to Color(0xFFFFE66D),
    "Ropa"            to Color(0xFFA78BFA),
    "Hogar"           to Color(0xFF6BCB77),
    "Salud"           to Color(0xFFFF9F43),
    "Educación"       to Color(0xFF48BEFF),
    "Tecnología"      to Color(0xFFFC5C7D),
    "Otro"            to Color(0xFF95A5A6)
)

fun categoryColor(category: String): Color =
    CategoryColors[category] ?: Color(0xFF95A5A6)

// ─── Chart gradient pairs ─────────────────────────────────────────────────────
val GradientPurpleCyan = listOf(Color(0xFF9D5CFF), Color(0xFF22D3EE))
val GradientPinkOrange = listOf(Color(0xFFFC5C7D), Color(0xFFFF9F43))
val GradientGreenTeal  = listOf(Color(0xFF6BCB77), Color(0xFF4ECDC4))
