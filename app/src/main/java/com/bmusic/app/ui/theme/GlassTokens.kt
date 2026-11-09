package com.bmusic.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * B Music — Glass tokens (docs/design/06-liquid-leger.md).
 * IMPORTANT : concaténez le blur via GlassSurface uniquement.
 */
object GlassTokens {
    const val RadiusMinimal = 16
    const val RadiusStandard = 20
    const val RadiusProminent = 24

    const val OpacityMinimalDark = 0.78f
    const val OpacityMinimalLight = 0.82f
    const val OpacityStandardDark = 0.83f
    const val OpacityStandardLight = 0.87f
    const val OpacityProminentDark = 0.88f
    const val OpacityProminentLight = 0.92f

    const val EdgePercent = 8
    val EdgeDark = Color(0x14FFFFFF)   // 8% blanc sombre
    val EdgeLight = Color(0x14000000)  // 8% noir clair
    val EdgeProminentDark = Color(0x1EFFFFFF) // 12%
    val EdgeProminentLight = Color(0x1E000000)

    enum class Level { Minimal, Standard, Prominent }

    fun radius(level: Level): Dp = when(level) {
        Level.Minimal -> RadiusMinimal.dp
        Level.Standard -> RadiusStandard.dp
        Level.Prominent -> RadiusProminent.dp
    }

    fun opacity(level: Level, dark: Boolean): Float = when(level) {
        Level.Minimal -> if (dark) OpacityMinimalDark else OpacityMinimalLight
        Level.Standard -> if (dark) OpacityStandardDark else OpacityStandardLight
        Level.Prominent -> if (dark) OpacityProminentDark else OpacityProminentLight
    }

    fun blurRadius(level: Level): Dp = radius(level)
}
