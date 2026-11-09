package com.bmusic.app.downloader.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Étape 3 (downloader v1.1) : ligne de suivi d'un téléchargement.
 *
 * Champs demandés : id, url source, id plateforme, hash de déduplication,
 * titre, auteur, état, progression, taille, chemin final, date, erreur.
 * Mots-clés additionnels (`format`, `streamUrl`, `streamKind`) pour la
 * reprise (Range) et le stockage MediaStore.
 */
@Entity(
    tableName = "DownloadEntity",
    indices = [
        Index(value = ["dedup_hash"], unique = true),
        Index(value = ["state"]),
        Index(value = ["platform_id"]),
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "platform_id")
    val platformId: String,

    @ColumnInfo(name = "dedup_hash")
    val dedupHash: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "state")
    val state: DownloadState = DownloadState.QUEUED,

    @ColumnInfo(name = "progress_bytes")
    val progressBytes: Long = 0,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = -1,

    @ColumnInfo(name = "final_path")
    val finalPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "error")
    val error: String? = null,

    /** Format reporté par la source (m4a, mp4, webm, opus, ogg, mp3). */
    @ColumnInfo(name = "format")
    val format: String = "m4a",

    @ColumnInfo(name = "stream_url")
    val streamUrl: String = "",

    /** 0 = audio, 1 = vidéo (MediaStore audio vs video). */
    @ColumnInfo(name = "is_video")
    val isVideo: Boolean = false,
)

enum class DownloadState {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELED
}
