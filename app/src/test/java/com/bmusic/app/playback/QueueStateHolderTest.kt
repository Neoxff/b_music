package com.bmusic.app.playback

import com.bmusic.app.core.model.queue.QueuePosition
import com.bmusic.app.data.model.QueueSong
import com.bmusic.app.data.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Non-regression: queue state is the single source of truth for mini/full player. */
class QueueStateHolderTest {

    private fun song(id: Long, title: String) = Song(
        id = id,
        data = "/music/$title.mp3",
        title = title,
        trackNumber = id.toInt(),
        year = 2024,
        size = 1_000L,
        duration = 60_000L,
        dateAdded = 0L,
        rawDateModified = 0L,
        albumId = 1L,
        albumName = "Album",
        artistId = 1L,
        artistName = "Artist",
        albumArtistName = null,
        genreName = null
    )

    @Test
    fun submitQueue_exposesSizeAndCurrentSong() = runBlocking {
        val holder = QueueStateHolder()
        val songs = listOf(song(1, "A"), song(2, "B"))
        holder.submitQueue(
            songs.mapIndexed { index, s -> QueueSong(index.toLong() to index, s) },
            QueuePosition(current = 0, indicesInTimeline = intArrayOf(0, 1))
        )

        assertEquals(2, holder.queueSize)
        assertEquals("A", holder.currentSong.first().title)
        assertEquals("B", holder.nextSong.first().title)
    }

    @Test
    fun setPlayerIndex_movesCurrentMarker() = runBlocking {
        val holder = QueueStateHolder()
        val songs = listOf(song(1, "A"), song(2, "B"), song(3, "C"))
        holder.submitQueue(
            songs.mapIndexed { index, s -> QueueSong(index.toLong() to index, s) },
            QueuePosition(current = 0, indicesInTimeline = intArrayOf(0, 1, 2))
        )

        holder.setPlayerIndex(2)

        assertEquals(2, holder.position.value.current)
        assertEquals("C", holder.currentSong.first().title)
    }

    @Test
    fun emptyQueue_returnsEmptySong() = runBlocking {
        val holder = QueueStateHolder()
        assertEquals(Song.emptySong, holder.currentSong.first())
        assertEquals(Song.emptySong, holder.nextSong.first())
    }
}
