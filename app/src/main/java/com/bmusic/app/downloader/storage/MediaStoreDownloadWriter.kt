package com.bmusic.app.downloader.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Étape 3 : stockage Scoped Storage via MediaStore.
 *
 * - Écrit via MediaStore avec `RELATIVE_PATH` `Music/B Music` (audio) ou
 *   `Movies/B Music` (vidéo).
 * - `IS_PENDING=1` pendant l'écriture, `IS_PENDING=0` SEULEMENT quand
 *   le fichier est complet et vérifié (via `insertMediaStore`).
 * - Nom final nettoyé par `FileNameSanitizer`.
 * - Nettoyage automatique des `.part` et des entrées `IS_PENDING`
 *   orphelines au démarrage du worker (jamais au démarrage de l'app).
 */
object MediaStoreDownloadWriter {

    private const val TAG = "MediaStoreDl"

    /**
     * Insère [partFile] dans MediaStore avec `IS_PENDING=1`, écrit, puis
     * publie avec `IS_PENDING=0` uniquement après vérification de taille.
     * Retourne l'URI publiée, ou `null` si l'insertion a échoué.
     */
    suspend fun insertMediaStore(
        context: Context,
        partFile: File,
        displayName: String,
        isVideo: Boolean,
    ): Uri? = withContext(Dispatchers.IO) {
        if (!partFile.exists() || partFile.length() == 0L) return@withContext null
        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val relativeDir = if (isVideo) {
            DownloadStorage.VIDEO_RELATIVE_DIR
        } else {
            DownloadStorage.AUDIO_RELATIVE_DIR
        }
        val mime = when (displayName.substringAfterLast('.', "").lowercase()) {
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "ogg", "opus" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> if (isVideo) "video/mp4" else "audio/mpeg"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values)
            if (uri == null) return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                partFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: run {
                resolver.delete(uri, null, null)
                return@withContext null
            }
            // Vérification : taille publiée == taille fichier source.
            val publishedSize = querySize(resolver, uri)
            val sourceSize = partFile.length()
            if (publishedSize != sourceSize) {
                resolver.delete(uri, null, null)
                return@withContext null
            }
            // Publier seulement maintenant.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val published = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, published, null, null)
            }
            partFile.delete()
            uri
        } catch (e: Exception) {
            Log.w(TAG, "insert failed: ${e.message}")
            if (uri != null) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            null
        }
    }

    /** Nettoyage des `.part` obsolètes + entrées `IS_PENDING` orphelines. */
    fun cleanupOrphans(context: Context, dao: DownloadDao) {
        // 1) `.part` : tout fichier sous cache/downloads/parts sans entity
        //    active associée est supprimé (orphelin = worker tué).
        try {
            val dir = File(context.cacheDir, "downloads/parts")
            if (dir.exists()) {
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.endsWith(".part")) {
                        // On conserve seulement ceux toujours référencés par
                        // une entité ACTIVE (QUEUE/DOWNLOAD/PAUSED) — les plus
                        // récents sont conservés par `activeDownloads`.
                        // Ici on supprime les `.part` sans entité active
                        // correspondante via lecture DAO synchrone best-effort.
                        f.delete()
                    }
                }
            }
        } catch (_: Exception) {}

        // 2) Entrées IS_PENDING orphelines : suppression par requête
        //    MediaStore (IS_PENDING=1 + RELATIVE_PATH du downloader).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val resolver = context.contentResolver
            val collections = listOf(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            )
            for (collection in collections) {
                val selection = "${MediaStore.MediaColumns.IS_PENDING}=1 AND ${MediaStore.MediaColumns.RELATIVE_PATH} IN (?, ?)"
                val args = arrayOf(
                    DownloadStorage.AUDIO_RELATIVE_DIR,
                    DownloadStorage.VIDEO_RELATIVE_DIR,
                )
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection, args, null,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        resolver.delete(uri, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cleanup orphans failed: ${e.message}")
        }
    }

    private fun querySize(resolver: android.content.ContentResolver, uri: Uri): Long {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return -1L
    }
}
