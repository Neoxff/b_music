package com.bmusic.app.core.appwidgets

import com.bmusic.app.core.appwidgets.component.progressTickInterval
import com.bmusic.app.core.appwidgets.state.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUILD — widgets liquid glass (design doc 14).
 * Logique pure : repli opaque, opacité, clé de cache, décision de repaint, intervalle progression.
 * Aucune Android Framework dependency → test JVM hors ligne.
 */
class WidgetGlassPolicyTest {

    @Test
    fun opaqueFallback_apiBelow31() {
        assertTrue(WidgetGlassPolicy.needsOpaqueFallback(30, false, true, true))
    }

    @Test
    fun opaqueFallback_batterySaver() {
        assertTrue(WidgetGlassPolicy.needsOpaqueFallback(33, true, true, true))
    }

    @Test
    fun opaqueFallback_artworkAbsent() {
        assertTrue(WidgetGlassPolicy.needsOpaqueFallback(33, false, false, true))
    }

    @Test
    fun opaqueFallback_launcherWithoutTransparency() {
        assertTrue(WidgetGlassPolicy.needsOpaqueFallback(33, false, true, false))
    }

    @Test
    fun glassAllowed_onlyWhenAllSignalsGreen() {
        assertFalse(WidgetGlassPolicy.needsOpaqueFallback(33, false, true, true))
    }

    @Test
    fun surfaceOpacity_neverBelowLegibilityFloor() {
        assertTrue(WidgetGlassPolicy.surfaceOpacity(dark = true, opaque = false) >= 0.55f)
        assertTrue(WidgetGlassPolicy.surfaceOpacity(dark = false, opaque = false) >= 0.55f)
    }

    @Test
    fun surfaceOpacity_inDesignRange() {
        val dark = WidgetGlassPolicy.surfaceOpacity(dark = true, opaque = false)
        val light = WidgetGlassPolicy.surfaceOpacity(dark = false, opaque = false)
        assertTrue(dark in 0.60f..0.80f)
        assertTrue(light in 0.65f..0.85f)
    }

    @Test
    fun surfaceOpacity_opaqueIsOne() {
        assertEquals(1f, WidgetGlassPolicy.surfaceOpacity(true, true), 0f)
        assertEquals(1f, WidgetGlassPolicy.surfaceOpacity(false, true), 0f)
    }

    @Test
    fun cacheKey_stableAndSizeAware() {
        val a = WidgetGlassPolicy.artworkCacheKey(42L, 96)
        assertEquals(a, WidgetGlassPolicy.artworkCacheKey(42L, 96))
        assertFalse(a == WidgetGlassPolicy.artworkCacheKey(42L, 128))
        assertFalse(a == WidgetGlassPolicy.artworkCacheKey(43L, 96))
    }

    @Test
    fun repaint_ignoresProgressOnly() {
        val before = PlaybackState(
            isPlaying = true, currentTitle = "Titre", songId = 1L,
            positionMs = 1_000L, durationMs = 200_000L
        )
        val after = before.copy(positionMs = 2_000L)
        assertFalse(WidgetGlassPolicy.needsRepaint(before, after))
    }

    @Test
    fun repaint_onPlayPauseChange() {
        val before = PlaybackState(isPlaying = true, songId = 1L)
        assertTrue(WidgetGlassPolicy.needsRepaint(before, before.copy(isPlaying = false)))
    }

    @Test
    fun repaint_onTitleChange() {
        val before = PlaybackState(songId = 1L, currentTitle = "A")
        assertTrue(WidgetGlassPolicy.needsRepaint(before, before.copy(currentTitle = "B")))
    }

    @Test
    fun repaint_onArtworkChange() {
        val before = PlaybackState(songId = 1L, seedColor = 0x112233)
        assertTrue(WidgetGlassPolicy.needsRepaint(before, before.copy(songId = 2L)))
    }

    @Test
    fun progressInterval_neverBelowOneSecond() {
        assertTrue(progressTickInterval(60_000L) >= 1_000L)
        assertTrue(progressTickInterval(0L) >= 1_000L)
    }
}
