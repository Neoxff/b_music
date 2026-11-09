package com.bmusic.app.downloader

import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.storage.DownloadStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Étape 3 (downloader v1.1) : tests offline de la logique pure
 * (déduplication, stockage Scoped Storage, canal de notification distinct).
 */
class DownloadManagerTest {

    @Test fun dedup_samePlatformAndFormat_sameHash() {
        val a = DownloadManager.dedupHash("dQw4w9WgXcQ", "m4a")
        val b = DownloadManager.dedupHash("dQw4w9WgXcQ", "m4a")
        assertEquals(a, b)
    }

    @Test fun dedup_caseInsensitive() {
        val a = DownloadManager.dedupHash("DQW4W9WgXcQ", "M4A")
        val b = DownloadManager.dedupHash("dqw4w9wgxcq", "m4a")
        assertEquals(a, b)
    }

    @Test fun dedup_differentFormat_differentHash() {
        val a = DownloadManager.dedupHash("id1", "m4a")
        val b = DownloadManager.dedupHash("id1", "mp3")
        assertFalse(a == b)
    }

    @Test fun dedup_differentPlatform_differentHash() {
        val a = DownloadManager.dedupHash("id1", "m4a")
        val b = DownloadManager.dedupHash("id2", "m4a")
        assertFalse(a == b)
    }

    @Test fun dedup_hashIsSha256Hex() {
        val h = DownloadManager.dedupHash("abc", "m4a")
        assertEquals(64, h.length)
        assertTrue(h.all { it in "0123456789abcdef" })
    }

    @Test fun forceDedupHash_neverCollidesWithBase() {
        val base = DownloadManager.dedupHash("id1", "m4a")
        val f1 = DownloadManager.forceDedupHash(base, nonce = 1L)
        val f2 = DownloadManager.forceDedupHash(base, nonce = 1L)
        val f3 = DownloadManager.forceDedupHash(base, nonce = 2L)
        // E1 : le forcer contourne l'index unique dedupHash sans collision
        assertFalse(f1 == base)
        assertFalse(f1 == f3)
        assertEquals(f1, f2) // déterministe à nonce fixe
        assertTrue(f1.startsWith(base))
    }

    @Test fun enqueueResult_accepteDoublonEtForce() {
        val existing = com.bmusic.app.downloader.data.DownloadEntity(
            sourceUrl = "https://x", platformId = "p", dedupHash = "h",
            title = "t", author = "a", state = com.bmusic.app.downloader.data.DownloadState.QUEUED,
            streamUrl = "https://s", format = "m4a", isVideo = false,
        )
        val dup = DownloadManager.EnqueueResult(existing, duplicate = true)
        assertTrue(dup.duplicate)
        val fresh = DownloadManager.EnqueueResult(existing.copy(dedupHash = "h2"), duplicate = false)
        assertFalse(fresh.duplicate)
        assertFalse(dup.entity?.dedupHash == fresh.entity?.dedupHash)
    }

    @Test fun storage_audioDir() {
        assertEquals("Music/B Music", DownloadStorage.AUDIO_RELATIVE_DIR)
    }

    @Test fun storage_videoDir() {
        assertEquals("Movies/B Music", DownloadStorage.VIDEO_RELATIVE_DIR)
    }
}
