package com.bmusic.app.downloader.manager

import android.content.Context
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.worker.DownloadWorker
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.MessageDigest

/**
 * Étape 3 : point d'entrée unique du module.
 *
 * - `probe` : résolution seule (étape 2).
 * - `enqueue` : dedup (même id plateforme + format => pas de re-téléchargement),
 *   insertion FIFO, enqueue du worker avec contraintes.
 * - Tout derrière le flag (`downloaderEnabled`) : flag off = aucun worker
 *   enqueue, aucune notification, aucune requête.
 */
class DownloadManager(
    private val engine: MediaExtractorEngine,
    private val httpClient: DownloaderHttpClient,
    private val dao: DownloadDao,
    private val appContext: Context,
) : KoinComponent {

    private val prefs: Preferences by inject()

    suspend fun probe(url: String): MediaInfo = engine.resolve(url)

    /**
     * Enfile un téléchargement.
     * @return l'entité créée/réutilisée, ou `null` si le flag est off.
     */
    suspend fun enqueue(
        info: MediaInfo,
        platformId: String,
        sourceUrl: String,
        streamUrl: String,
        format: String,
        isVideo: Boolean = false,
        wifiOnly: Boolean = false,
        maxConcurrent: Int = DownloadWorker.DEFAULT_MAX_CONCURRENT,
    ): DownloadEntity? = withContext(Dispatchers.IO) {
        if (!prefs.downloaderEnabled) return@withContext null
        val dedup = dedupHash(platformId, format)
        dao.getByDedupHash(dedup)?.let { existing ->
            // Déduplication : proposer de ne pas retélécharger.
            return@withContext existing
        }
        val entity = DownloadEntity(
            sourceUrl = sourceUrl,
            platformId = platformId,
            dedupHash = dedup,
            title = info.title,
            author = info.author,
            state = DownloadState.QUEUED,
            streamUrl = streamUrl,
            format = format.lowercase(),
            isVideo = isVideo,
        )
        val id = dao.insert(entity)
        val stored = dao.getById(id) ?: entity
        DownloadWorker.enqueue(appContext, stored, wifiOnly, maxConcurrent)
        stored
    }

    suspend fun pause(id: Long) = withContext(Dispatchers.IO) {
        if (!prefs.downloaderEnabled) return@withContext
        dao.updateState(id, DownloadState.PAUSED, System.currentTimeMillis(), null)
        DownloadWorker.cancel(appContext, id)
    }

    suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        dao.updateState(id, DownloadState.CANCELED, System.currentTimeMillis(), null)
        DownloadWorker.cancel(appContext, id)
    }

    suspend fun resume(id: Long) = withContext(Dispatchers.IO) {
        if (!prefs.downloaderEnabled) return@withContext
        val entity = dao.getById(id) ?: return@withContext
        dao.updateState(id, DownloadState.QUEUED, System.currentTimeMillis(), null)
        val resumeEntity = entity.copy(state = DownloadState.QUEUED)
        DownloadWorker.enqueue(appContext, resumeEntity, wifiOnly = false)
    }

    companion object {
        /** Hash de déduplication : id plateforme + format (+ sorte audio/vidéo). */
        fun dedupHash(platformId: String, format: String): String {
            val input = "${platformId.trim().lowercase()}:${format.trim().lowercase()}"
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun streamIsVideo(kind: StreamKind): Boolean = kind == StreamKind.VIDEO
    }
}
