package com.bmusic.app.data

import com.bmusic.app.data.model.Song

interface SongProvider {
    val songs: List<Song>
}