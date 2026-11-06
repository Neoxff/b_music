package com.bmusic.ui.theme

import androidx.compose.ui.graphics.Color

// B Music — tokens couleur. Source de vérité : docs/design/01-design-system.md
// Voir aussi docs/design/tokens/tokens.css (maquette). Contraste vérifié WCAG AA (voir doc).

// ── Thème sombre (défaut) ───────────────────────────────
private val DarkBg = Color(0xFF0B0B0E)          // fond application
private val DarkSurface1 = Color(0xFF131318)    // surface 1 (barre, sheet, rail)
private val DarkSurface2 = Color(0xFF1A1A21)    // surface 2 (cartes, lignes survolées)
private val DarkSurface3 = Color(0xFF22222B)    // surface 3 (appui, sélection)
private val DarkTrait = Color(0xFF2A2A34)       // séparateurs 1 px
private val DarkOnBg = Color(0xFFF2F2F0)        // texte primaire — 17,5:1
private val DarkOnBgSecondary = Color(0xFFB8B8C0) // texte secondaire — 9,9:1
private val DarkOnBgMuted = Color(0xFF8E8E98)   // légendes — 5,6:1 (Large Text AA)
private val DarkOnBgDisabled = Color(0xFF5A5A64)

// ── Thème clair ─────────────────────────────────────────
private val LightBg = Color(0xFFFAFAFB)
private val LightSurface1 = Color(0xFFFFFFFF)
private val LightSurface2 = Color(0xFFF2F2F5)
private val LightSurface3 = Color(0xFFE9E9EE)
private val LightTrait = Color(0xFFE4E4EA)
private val LightOnBg = Color(0xFF1B1B1F)       // 16,5:1
private val LightOnBgSecondary = Color(0xFF4A4A52) // 8,4:1
private val LightOnBgMuted = Color(0xFF6E6E78)  // 4,6:1
private val LightOnBgDisabled = Color(0xFFB9B9C2)

// ── Accent de marque B Music — « B Amber » ──────────────
// Un seul accent, décliné par thème (voir 01 §3 pour la justification WCAG).
private val AccentDark = Color(0xFFE8862E)      // sur #0B0B0E : 7,38:1 (AA)
private val AccentDarkPressed = Color(0xFFC96A1D)
private val AccentOnDark = Color(0xFF241203)    // texte sur accent sombre : 6,78:1
private val AccentLight = Color(0xFFA85B0B)     // sur #FAFAFB : 4,83:1 (AA)
private val AccentLightPressed = Color(0xFF83460A)
private val AccentOnLight = Color(0xFFFFFFFF)   // texte sur accent clair : 5,04:1

data class BMusicColors(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val trait: Color,
    val onBg: Color,
    val onBgSecondary: Color,
    val onBgMuted: Color,
    val onBgDisabled: Color,
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val BMusicDarkColors = BMusicColors(
    bg = DarkBg, surface1 = DarkSurface1, surface2 = DarkSurface2, surface3 = DarkSurface3,
    trait = DarkTrait, onBg = DarkOnBg, onBgSecondary = DarkOnBgSecondary,
    onBgMuted = DarkOnBgMuted, onBgDisabled = DarkOnBgDisabled,
    accent = AccentDark, accentPressed = AccentDarkPressed, onAccent = AccentOnDark, isDark = true,
)

val BMusicLightColors = BMusicColors(
    bg = LightBg, surface1 = LightSurface1, surface2 = LightSurface2, surface3 = LightSurface3,
    trait = LightTrait, onBg = LightOnBg, onBgSecondary = LightOnBgSecondary,
    onBgMuted = LightOnBgMuted, onBgDisabled = LightOnBgDisabled,
    accent = AccentLight, accentPressed = AccentLightPressed, onAccent = AccentOnLight, isDark = false,
)

// ── Accent dynamique (extrait de la pochette, Palette API) ──
// Usage autorisé UNIQUEMENT : bouton lecture, progression, halo opaque (tres discret).
// Jamais pour du texte, des fonds, la navigation, les chips.
data class DynamicAccent(
    val accent: Color,      // dominant « vibrant » désaturé si besoin
    val onAccent: Color,    // garanti ≥ 4,5:1 (calculé par l'extracteur, cf. 01 §3.4)
)

// ── Élévation tonale (aucune ombre portée décorative) ────
// 0.dp = bg, 1.dp = surface1, 3.dp = surface2, 6.dp = surface3 (+ trait 1 px si besoin)
