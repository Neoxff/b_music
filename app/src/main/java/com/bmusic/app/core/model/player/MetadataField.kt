/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bmusic.app.core.model.player

import android.os.Parcelable
import com.bmusic.app.R
import com.bmusic.app.core.model.player.MetadataField.Content.Bitrate
import com.bmusic.app.core.model.player.MetadataField.Content.Format
import com.bmusic.app.core.model.player.MetadataField.Content.SampleRate
import com.bmusic.app.data.local.MetadataReader
import com.bmusic.app.data.model.Song
import com.bmusic.app.extensions.files.toAudioFile
import com.bmusic.app.extensions.utilities.DEFAULT_INFO_DELIMITER
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Parcelize
@Serializable
class MetadataField(
    @SerialName("info")
    val content: Content,
    var isEnabled: Boolean
) : Parcelable {

    @Serializable
    enum class Content(val displayNameRes: Int, val key: String?) {
        Album(R.string.album, MetadataReader.ALBUM),
        AlbumArtist(R.string.album_artist, MetadataReader.ALBUM_ARTIST),
        Genre(R.string.genre, MetadataReader.GENRE),
        Year(R.string.year, MetadataReader.YEAR),
        Composer(R.string.composer, MetadataReader.COMPOSER),
        Conductor(R.string.conductor, MetadataReader.PRODUCER),
        Publisher(R.string.publisher, MetadataReader.COPYRIGHT),
        Lyricist(R.string.lyricist, MetadataReader.LYRICIST),
        Arranger(R.string.arranger, MetadataReader.ARRANGER),
        Format(R.string.label_file_format, null),
        Bitrate(R.string.label_bit_rate, null),
        SampleRate(R.string.label_sampling_rate, null);
    }

    companion object {
        fun getMetadataValue(song: Song, fields: List<MetadataField>): String? {
            if (song == Song.emptySong || fields.isEmpty())
                return null

            val metadataReader = MetadataReader(song.uri)
            if (metadataReader.hasMetadata) {
                return fields.filter { it.isEnabled }
                    .mapNotNull {
                        when (it.content.key) {
                            null -> when (it.content) {
                                Bitrate -> metadataReader.bitrate()
                                SampleRate -> metadataReader.sampleRate()
                                Format -> File(song.data).toAudioFile()
                                    ?.audioHeader
                                    ?.format

                                else -> null
                            }
                            MetadataReader.GENRE -> metadataReader.genre()
                            else -> metadataReader.first(it.content.key)
                        }
                    }
                    .joinToString(separator = DEFAULT_INFO_DELIMITER)
            }
            return null
        }
    }
}