package com.bmusic.app.downloader.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUILD — progression de téléchargement (design doc 15).
 * Logique pure, JVM, hors ligne : lissage EMA, retour à zéro, total inconnu,
 * formatage, monotonie, limitation de fréquence. Aucun Android Framework.
 */
class DownloadProgressMathTest {

    private val mib = 1_048_576L

    @Test
    fun ema_smoothStaysBetweenPreviousAndInstant() {
        val instant = 1 * mib.toDouble() // 1 MiB/s sur 1 s
        val smoothed = SpeedCalculator.smooth(
            previousBps = 0.0,
            deltaBytes = mib,
            deltaMs = 1_000L,
        )
        assertTrue(smoothed > 0.0)
        assertTrue(smoothed < instant)
    }

    @Test
    fun ema_biggerDeltaGivesHigherSpeed() {
        val small = SpeedCalculator.smooth(0.0, mib / 2, 1_000L)
        val big = SpeedCalculator.smooth(0.0, mib, 1_000L)
        assertTrue(big > small)
    }

    @Test
    fun ema_zeroDeltaDecaysTowardZero() {
        val decayed = SpeedCalculator.smooth(1_000_000.0, 0L, 1_000L)
        assertTrue(decayed in 0.0..1_000_000.0)
        assertTrue(decayed < 1_000_000.0)
    }

    @Test
    fun speed_afterTwoSecondsIdle_isZero() {
        assertEquals(0L, SpeedCalculator.speedAfterIdle(0L, 2_000L, 500_000.0))
    }

    @Test
    fun speed_beforeTwoSecondsIdle_keepsEma() {
        assertEquals(500_000L, SpeedCalculator.speedAfterIdle(0L, 1_999L, 500_000.0))
    }

    @Test
    fun remaining_hiddenBeforeStableData() {
        assertNull(SpeedCalculator.remainingSeconds(30 * mib, 48 * mib, 2_516_582L, 1_999L))
    }

    @Test
    fun remaining_shownAfterStableData() {
        assertEquals(8L, SpeedCalculator.remainingSeconds(30 * mib, 48 * mib, 2_516_582L, 2_000L))
    }

    @Test
    fun remaining_nullWhenSpeedZeroOrTotalUnknown() {
        assertNull(SpeedCalculator.remainingSeconds(30 * mib, 48 * mib, 0L, 5_000L))
        assertNull(SpeedCalculator.remainingSeconds(30 * mib, -1L, 2_516_582L, 5_000L))
    }

    @Test
    fun percent_nullWhenTotalUnknown() {
        assertNull(ProgressLogic.percent(30 * mib, null))
        assertNull(ProgressLogic.percent(30 * mib, -1L))
    }

    @Test
    fun percent_computedAndClamped() {
        assertEquals(63, ProgressLogic.percent(30 * mib, 48 * mib))
        assertEquals(0, ProgressLogic.percent(0L, 48 * mib))
        assertEquals(100, ProgressLogic.percent(99 * mib, 48 * mib))
    }

    @Test
    fun percent_monotonicNeverGoesBack() {
        assertEquals(63, ProgressLogic.monotonic(63, 61))
        assertEquals(64, ProgressLogic.monotonic(63, 64))
        assertNull(ProgressLogic.monotonic(63, null))
    }

    @Test
    fun formatSpeed_koPerSecBelowOneMib() {
        assertEquals("781 Ko/s", ProgressFormatter.speed(800_000L))
    }

    @Test
    fun formatSpeed_moPerSecOneDecimalFrenchComma() {
        assertEquals("2,4 Mo/s", ProgressFormatter.speed(2_516_582L))
    }

    @Test
    fun formatSize_andRemaining() {
        assertEquals("30,0 / 48,0 Mo", ProgressFormatter.size(30 * mib, 48 * mib))
        assertEquals("8 s", ProgressFormatter.remaining(8L))
        assertEquals("—", ProgressFormatter.remaining(null))
    }

    @Test
    fun throttle_blocksTooFrequentAndUnchanged() {
        val t = ProgressThrottle(250L)
        assertTrue(t.shouldEmit(0L, "0 %|0 Ko/s"))
        // À 100 ms l'émission est bloquée (intervalle) : la clé "1 %" n'est PAS mémorisée.
        assertFalse(t.shouldEmit(100L, "1 %|100 Ko/s"))
        // À 300 ms, la clé a changé depuis la dernière émission (0 %) → émission légitime.
        assertTrue(t.shouldEmit(300L, "1 %|100 Ko/s"))
        // Même clé juste après → bloquée (rien n'a changé).
        assertFalse(t.shouldEmit(310L, "1 %|100 Ko/s"))
    }
}
