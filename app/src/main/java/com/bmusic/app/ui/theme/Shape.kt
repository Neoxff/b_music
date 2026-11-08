package com.bmusic.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * B Music — formes. Rayons : 8 / 12 / 20 / 28 dp ; boutons principaux en cercle plein.
 * Source : docs/design/01-design-system.md §5.
 */
object BMusicShapes {
    val Small = RoundedCornerShape(8.dp)
    val Cover = RoundedCornerShape(12.dp)
    val Panel = RoundedCornerShape(16.dp)
    val Hero = RoundedCornerShape(20.dp)
    val HeroLarge = RoundedCornerShape(28.dp)
    val Circle = RoundedCornerShape(50)
}
