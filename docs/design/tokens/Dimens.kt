package com.bmusic.ui.theme

import androidx.compose.ui.unit.dp

// B Music — dimensions. Grille 4 dp, marges d'écran 20 dp.
// Source de vérité : docs/design/01-design-system.md §5.

val BMusicDimens = object {
    val touchTarget = 44.dp      // cible tactile minimale (accessibilité)
    val screenMargin = 20.dp     // marge horizontale d'écran
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp

    val progressBar = 4.dp       // piste normale
    val progressBarActive = 8.dp // piste au toucher / seek
    val progressThumb = 14.dp    // curseur (visible au toucher)
    val miniPlayerHeight = 64.dp // mini-lecteur (contenu) — +8 dp de séparateur au-dessus
    val artworkMini = 48.dp      // pochette mini-player
    val artworkGrid = 0.dp       // carré : largeur (parent - 2×margin - 12) / 2
    val navBar = 64.dp           // barre de navigation basse
    val playerButtonRing = 72.dp // bouton lecture plein écran
    val divider = 1.dp           // trait fin
}
