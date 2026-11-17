package com.bmusic.app.core.appwidgets

import com.bmusic.app.core.appwidgets.state.PlaybackState

/**
 * BUILD — widgets liquid glass (design doc 14).
 *
 * Décisions pures, sans dépendance Android, testables en JVM :
 *  - repli opaque (API < 31, économiseur de batterie, pochette absente, lanceur sans transparence)
 *  - opacité de surface dans la plage du design (jamais sous le plancher de lisibilité)
 *  - clé de cache de la pochette floutée (id de pochette + taille)
 *  - décision de repaint : ignore les ticks de progression, réagit au titre / lecture-pause / pochette
 */
object WidgetGlassPolicy {

    /** Plancher de lisibilité absolu, quel que soit le fond d'écran. */
    const val OPACITY_FLOOR = 0.55f

    private const val OPACITY_DARK = 0.70f // milieu de 0,60 – 0,80
    private const val OPACITY_LIGHT = 0.75f // milieu de 0,65 – 0,85

    /** Taille de la pochette pré-floutée servie au widget. */
    const val BLURRED_ARTWORK_PX = 96

    /**
     * Vrai dès qu'une seule condition de repli est réunie.
     * Aucune de ces valeurs ne nécessite le framework : l'appelant les collecte.
     */
    fun needsOpaqueFallback(
        apiLevel: Int,
        powerSaveMode: Boolean,
        artworkAvailable: Boolean,
        launcherSupportsTransparency: Boolean
    ): Boolean = apiLevel < 31 ||
        powerSaveMode ||
        !artworkAvailable ||
        !launcherSupportsTransparency

    /** Opacité de la surface ; 1f = repli opaque. */
    fun surfaceOpacity(dark: Boolean, opaque: Boolean): Float = when {
        opaque -> 1f
        dark -> OPACITY_DARK.coerceAtLeast(OPACITY_FLOOR)
        else -> OPACITY_LIGHT.coerceAtLeast(OPACITY_FLOOR)
    }

    /** Clé stable du cache LRU de la pochette floutée. */
    fun artworkCacheKey(songId: Long, sizePx: Int): String = "$songId:$sizePx"

    /**
     * Un repaint n'est nécessaire que si un champ *visible* hors timeline a changé.
     * La progression (positionMs / durationMs) est volontairement exclue : elle est
     * poussée à part par [WidgetPresenter.refreshPosition] à un intervalle ≥ 1 s.
     */
    fun needsRepaint(before: PlaybackState, after: PlaybackState): Boolean =
        before.isPlaying != after.isPlaying ||
            before.currentTitle != after.currentTitle ||
            before.currentArtist != after.currentArtist ||
            before.songId != after.songId ||
            before.seedColor != after.seedColor ||
            before.isFavorite != after.isFavorite ||
            before.isShuffleMode != after.isShuffleMode ||
            before.repeatMode != after.repeatMode ||
            before.songIds != after.songIds
}
