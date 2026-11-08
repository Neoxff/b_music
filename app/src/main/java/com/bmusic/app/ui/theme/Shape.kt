package com.bmusic.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** B Music — formes. Rayons : 8 / 12 / 20 / 28 dp + cercle plein (design 01 §5). */
val BMusicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val ShapeSmall: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
val ShapeCover: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
val ShapePanel: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)
val ShapeHero: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp)
val ShapeCircle: androidx.compose.ui.graphics.Shape = RoundedCornerShape(50)
