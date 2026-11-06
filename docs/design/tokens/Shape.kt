package com.bmusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// B Music — formes et rayons. Source de vérité : docs/design/01-design-system.md §5

val BMusicShapes = object {
    val r8 = RoundedCornerShape(8.dp)     // boutons secondaires, chips, inputs
    val r12 = RoundedCornerShape(12.dp)   // pochettes, cartes
    val r20 = RoundedCornerShape(20.dp)   // sheets, feuilles d'options
    val r28 = RoundedCornerShape(28.dp)   // lecteur plein écran (pochette agrandie)
    val circle = RoundedCornerShape(50)   // boutons principaux (circle plein)
}
