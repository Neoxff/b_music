package com.bmusic.app.downloader.progress

import com.bmusic.app.downloader.data.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {

    @Test
    fun knownPercentAndSpeed() {
        val p = DownloadProgress(1L, 30L * 1_048_576, 48L * 1_048_576, 63, 2_516_582L, 8L, DownloadState.DOWNLOADING)
        assertEquals("63 % · 2,4 Mo/s", DownloadNotification.text(p))
    }

    @Test
    fun unknownTotalShowsEllipsis() {
        val p = DownloadProgress(1L, 30L * 1_048_576, null, null, 800_000L, null, DownloadState.DOWNLOADING)
        assertEquals("… · 781 Ko/s", DownloadNotification.text(p))
    }

    @Test
    fun pausedStateIsVisible() {
        val p = DownloadProgress(1L, 10L, 100L, 10, 0L, null, DownloadState.PAUSED)
        assertEquals("10 % · 0 Ko/s", DownloadNotification.text(p))
    }
}
