package com.bmusic.app.playback.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Non-regression: MediaItem ↔ Song resolution relies on these stable path IDs. */
class MediaIDsTest {

    @Test
    fun getPathId_joinsWithSeparator() {
        assertEquals("SONGS:42", MediaIDs.getPathId(MediaIDs.SONGS, 42L))
        assertEquals("ALBUMS:7", MediaIDs.getPathId(MediaIDs.ALBUMS, "7"))
    }

    @Test
    fun splitPath_roundTrips() {
        val parts = MediaIDs.splitPath(MediaIDs.getPathId(MediaIDs.PLAYLISTS, 99L))
        assertEquals(listOf(MediaIDs.PLAYLISTS, "99"), parts)
    }

    @Test
    fun isPath_distinguishesRootFromChild() {
        assertTrue(MediaIDs.isPath("SONGS:1"))
        assertFalse(MediaIDs.isPath(MediaIDs.ROOT))
        assertFalse(MediaIDs.isPath(MediaIDs.SONGS))
    }
}
