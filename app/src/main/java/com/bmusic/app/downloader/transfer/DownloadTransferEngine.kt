package com.bmusic.app.downloader.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.security.DownloadLimits
import com.bmusic.app.downloader.security.FileNameSanitizer
import com.bmusic.app.downloader.security.SsrfGuard
import com.bmusic.app.downloader.security.UrlValidator
import com.bmusic.app.data.local.EditTarget
import com.bmusic.app.data.local.MetadataWriter
import com.bmusic.app.downloader.storage.MediaStoreDownloadWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.coroutineContext

/**
 * Étape 3 : moteur de transfert.
 *
 * - Utilise le client HTTP dédié (`DownloaderHttpClient`).
 * - Valide l'URL du stream avec `UrlValidator` à chaque redirection
 *   (URL finale re-validée via `SsrfGuard`).
 * - Applique `DownloadLimits` (audio 200 Mo, arrêt si flux > annoncé).
 * - Reprise : en-têtes `Range: bytes=-` + fichier `.part`.
 * - Stockage Scoped Storage via `MediaStoreDownloadWriter`.
 */
class DownloadTransferEngine(
    private val appContext: Context,
    private val downloaderHttpClient: DownloaderHttpClient,
) {
    class TransferPaused : Exception("paused")
    class TransferCanceled : Exception("canceled")

    suspend fun download(
        entity: DownloadEntity,
        dao: DownloadDao,
        onProgress: suspend (doneBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Uri? = withContext(Dispatchers.IO) {
        ensureActive()
        val streamUrl = entity.streamUrl.ifBlank { entity.sourceUrl }
        val validated = UrlValidator.validate(streamUrl)
        if (validated !is UrlValidator.Validation.Valid) {
            throw IOException("invalid stream url")
        }
        try {
            SsrfGuard.resolveAndValidate(validated.normalized)
        } catch (_: Exception) {}

        val displayName = FileNameSanitizer.sanitize(
            networkProvided = entity.title,
            suggestedExtension = entity.format,
        )
        val isVideo = entity.isVideo
        val partFile = partFileFor(entity, displayName)
        var startByte = if (partFile.exists()) partFile.length() else 0L
        if (startByte < 0) startByte = 0

        val builder = Request.Builder().url(validated.normalized).get()
        if (startByte > 0) builder.addHeader("Range", "bytes=$startByte-")
        val response = downloaderHttpClient.client.newCall(builder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            throw IOException("HTTP ${response.code}")
        }
        val finalUrl = response.request.url.toString()
        if (finalUrl != validated.normalized) {
            val v = UrlValidator.validate(finalUrl)
            if (v !is UrlValidator.Validation.Valid) {
                throw IOException("redirect to invalid url: $finalUrl")
            }
            try {
                SsrfGuard.resolveAndValidate((v as UrlValidator.Validation.Valid).normalized)
            } catch (e: Exception) {
                throw IOException("redirect blocked: ${e.message}")
            }
        }

        val contentLengthHeader = response.header("Content-Length")?.toLongOrNull()
        val contentRange = response.header("Content-Range")?.let { parseTotal(it) }
        val headerTotal = contentRange ?: contentLengthHeader
        val totalBytes = headerTotal ?: -1L

        if (startByte > 0 && response.code == 200) {
            partFile.delete()
            startByte = 0
        }

        partFile.parentFile?.mkdirs()
        if (!partFile.exists()) partFile.createNewFile()
        val input: InputStream = response.body?.byteStream() ?: throw IOException("empty body")
        var written = startByte
        java.io.FileOutputStream(partFile, true).use { fos ->
            val buf = ByteArray(32 * 1024)
            var read: Int
            while (true) {
                coroutineContext.ensureActive()
                read = input.read(buf)
                if (read < 0) break
                val decision = DownloadLimits.evaluate(
                    bytesSoFar = written,
                    chunkSize = read.toLong(),
                    contentLength = if (totalBytes > 0) totalBytes else null,
                    isVideo = isVideo,
                )
                if (decision is DownloadLimits.Decision.Abort) {
                    throw IOException("limit: ${decision.reason}")
                }
                fos.write(buf, 0, read)
                written += read
                onProgress(written, totalBytes)
            }
        }
        ensureActive()

        val uri = MediaStoreDownloadWriter.insertMediaStore(
            context = appContext,
            partFile = partFile,
            displayName = displayName,
            isVideo = isVideo,
        )
        if (uri != null) writeTags(uri, entity)
        return@withContext uri
    }

    private suspend fun writeTags(uri: Uri, entity: DownloadEntity) {
        try {
            val content = EditTarget.Content(id = -1L, uri = uri, path = uri.toString())
            val target = EditTarget(
                type = EditTarget.Type.Song,
                name = entity.title,
                id = -1L,
                artworkId = -1L,
                contents = listOf(content),
            )
            val writer = MetadataWriter()
            writer.propertyMap(
                mapOf(
                    "TITLE" to entity.title,
                    "ARTIST" to entity.author,
                    "ALBUMARTIST" to entity.author,
                )
            )
            writer.write(appContext, target)
        } catch (e: Exception) {
            Log.w("DownloadTransfer", "tag write failed: ${e.message}")
        }
    }

    private fun partFileFor(entity: DownloadEntity, displayName: String): File {
        val safe = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(appContext.cacheDir, "downloads/parts")
        dir.mkdirs()
        return File(dir, "${entity.id}-$safe.part")
    }

    private fun parseTotal(contentRange: String): Long? =
        contentRange.substringAfterLast('/').toLongOrNull()
}
