package com.bmusic.app.downloader.progress

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUILD — décision d'émission depuis le worker (design doc 15, §3).
 * - bus : au plus toutes les 250 ms ET seulement si pourcentage/vitesse changent
 * - notification : au plus toutes les 1 s
 */
class ProgressPublishPolicyTest {

    @Test
    fun bus_emitsAtMostEvery250ms() {
        val bus = ProgressThrottle(250L)
        assertTrue(bus.shouldEmit(0L, "0|0"))
        assertFalse(bus.shouldEmit(100L, "1|100"))
        assertTrue(bus.shouldEmit(250L, "1|100"))
    }

    @Test
    fun notification_emitsAtMostEvery1000ms() {
        val notif = ProgressThrottle(1_000L)
        assertTrue(notif.shouldEmit(0L, "0 % · 0 Ko/s"))
        assertFalse(notif.shouldEmit(500L, "1 % · 100 Ko/s"))
        assertTrue(notif.shouldEmit(1_000L, "1 % · 100 Ko/s"))
    }

    @Test
    fun unchangedKeyIsNeverEmitted() {
        val bus = ProgressThrottle(250L)
        assertTrue(bus.shouldEmit(0L, "5|512"))
        assertFalse(bus.shouldEmit(2_000L, "5|512"))
    }
}
