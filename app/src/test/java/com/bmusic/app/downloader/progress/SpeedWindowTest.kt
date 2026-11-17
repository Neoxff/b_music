package com.bmusic.app.downloader.progress

import com.bmusic.app.downloader.data.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedWindowTest {

    private val mib = 1_048_576L

    @Test
    fun speedRisesWithSamples() {
        val w = SpeedWindow()
        w.sample(0L, 0L)
        w.sample(1_000L, mib)
        val s1 = w.speed(1_000L)
        w.sample(2_000L, 2 * mib)
        val s2 = w.speed(2_000L)
        assert(s1 > 0L)
        assert(s2 >= s1)
    }

    @Test
    fun speedFallsToZeroAfterIdle() {
        val w = SpeedWindow()
        w.sample(0L, 0L)
        w.sample(1_000L, mib)
        assert(w.speed(1_000L) > 0L)
        assertEquals(0L, w.speed(3_000L))
    }

    @Test
    fun snapshotUnknownTotalHasNullPercentAndRemaining() {
        val w = SpeedWindow()
        w.sample(0L, 0L)
        w.sample(1_000L, mib)
        val p = w.snapshot(7L, mib, -1L, DownloadState.DOWNLOADING, 1_000L)
        assertNull(p.percent)
        assertNull(p.remainingSeconds)
        assertNull(p.bytesTotal)
    }

    @Test
    fun snapshotKnownTotalHasPercent() {
        val w = SpeedWindow()
        w.sample(0L, 0L)
        w.sample(3_000L, 30 * mib)
        val p = w.snapshot(7L, 30 * mib, 48 * mib, DownloadState.DOWNLOADING, 3_000L)
        assertEquals(63, p.percent)
        assertEquals(48 * mib, p.bytesTotal)
    }

    @Test
    fun busPublishesAndRemoves() {
        val bus = DownloadProgressBus()
        val p = DownloadProgress(1L, 10L, 100L, 10, 512L, null, DownloadState.DOWNLOADING)
        bus.publish(p)
        assertEquals(p, bus.latest(1L))
        bus.remove(1L)
        assertNull(bus.latest(1L))
    }
}
