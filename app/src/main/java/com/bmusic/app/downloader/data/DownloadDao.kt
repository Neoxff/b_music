package com.bmusic.app.downloader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Accès FIFO à la table de téléchargement.
 * FIFO : [pendingQueue] est triée par id croissant (= ordre d'arrivée).
 */
@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("SELECT * FROM DownloadEntity WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM DownloadEntity WHERE id = :id LIMIT 1")
    fun getByIdSync(id: Long): DownloadEntity?

    /** FIFO : les téléchargements en attente dans leur ordre d'arrivée. */
    @Query("SELECT * FROM DownloadEntity WHERE state = 'QUEUED' ORDER BY id ASC")
    suspend fun pendingQueue(): List<DownloadEntity>

    @Query("SELECT * FROM DownloadEntity WHERE state IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY id ASC")
    suspend fun activeDownloads(): List<DownloadEntity>

    @Query("SELECT * FROM DownloadEntity ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<DownloadEntity>

    @Query("SELECT * FROM DownloadEntity WHERE dedup_hash = :hash LIMIT 1")
    suspend fun getByDedupHash(hash: String): DownloadEntity?

    @Query("SELECT * FROM DownloadEntity WHERE platform_id = :platformId AND state = 'COMPLETED' ORDER BY updated_at DESC LIMIT 1")
    suspend fun completedForPlatform(platformId: String): DownloadEntity?

    @Query("UPDATE DownloadEntity SET state = :state, updated_at = :updatedAt, error = :error WHERE id = :id")
    suspend fun updateState(id: Long, state: DownloadState, updatedAt: Long, error: String? = null)

    @Query("UPDATE DownloadEntity SET progress_bytes = :progress, total_bytes = :total, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Long, total: Long, updatedAt: Long)

    @Query("UPDATE DownloadEntity SET final_path = :finalPath, state = 'COMPLETED', error = NULL, updated_at = :updatedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, finalPath: String, updatedAt: Long)

    @Query("UPDATE DownloadEntity SET state = 'QUEUED', updated_at = :updatedAt WHERE state = 'DOWNLOADING'")
    suspend fun resetDownloadingToQueued(updatedAt: Long)

    @Query("DELETE FROM DownloadEntity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM DownloadEntity WHERE state = 'COMPLETED'")
    suspend fun clearCompleted()
}
