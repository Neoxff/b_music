package com.bmusic.app.downloader.security

/**
 * Limites du téléchargeur.
 *
 * Règles :
 * - taille maximale audio **200 Mo** (configurable pour la vidéo) ;
 * - arrêt dès que le flux dépasse la taille annoncée (`Content-Length`) —
 *   détection de mensonge de serveur (`early abort`, pas de remplissage) ;
 * - timeouts calés sur le client dédié (`connect 10 s`, `read 30 s`,
 *   `write 30 s`, `call 120 s`) ;
 * - pas de cookies (enforced par `CookieJar.NO_COOKIES` dans
 *   [com.bmusic.app.downloader.manager.DownloaderHttpClient] — constats
 *   ici : [USE_COOKIES] = false, et tout code qui tenterait un CookieJar
 *   dédié doit passer par ces limites).
 *
 * Pure logique (pas d'I/O) pour rester testable offline.
 */
object DownloadLimits {

    const val DEFAULT_MAX_AUDIO_BYTES = 200L * 1024L * 1024L
    const val DEFAULT_MAX_VIDEO_BYTES = 1L * 1024L * 1024L * 1024L

    /** Ce module est explicitement sans cookies. */
    const val USE_COOKIES = false

    fun maxBytesFor(isVideo: Boolean, maxVideoBytes: Long? = null): Long {
        if (isVideo) {
            return (maxVideoBytes ?: DEFAULT_MAX_VIDEO_BYTES)
                .coerceAtLeast(1L)
        }
        return DEFAULT_MAX_AUDIO_BYTES
    }

    /**
     * Évalue une lecture de chunk.
     * @param bytesSoFar octets déjà écrits, @param chunkSize taille du chunk
     *  lu, @param contentLength taille annoncée (`Content-Length` peut être
     *  fausse ou absente : -1/ null tolérés).
     * Retourne [Decision.Abort] dès qu'une borne est dépassée :
     * - `bytesSoFar + chunkSize > maxBytes` (plafond dur) ;
     * - `contentLength != null && bytesSoFar + chunkSize > contentLength`
     *   (le serveur ment — arrêt anticipé, fichier jamais présenté comme OK) ;
     * - `contentLength != null && contentLength > maxBytes` (déjà hors quota).
     */
    fun evaluate(
        bytesSoFar: Long,
        chunkSize: Long,
        contentLength: Long?,
        isVideo: Boolean,
        maxVideoBytes: Long? = null,
    ): Decision {
        val max = maxBytesFor(isVideo, maxVideoBytes)
        val newTotal = bytesSoFar + chunkSize
        if (contentLength != null && contentLength < 0) {
            return Decision.Abort("invalid Content-Length: $contentLength")
        }
        if (contentLength != null && contentLength > max) {
            return Decision.Abort(
                "announced $contentLength exceeds limit $max"
            )
        }
        if (newTotal > max) {
            return Decision.Abort("exceeded limit $max at $newTotal")
        }
        if (contentLength != null && newTotal > contentLength) {
            return Decision.Abort(
                "stream exceeds announced $contentLength"
            )
        }
        return Decision.Continue(newTotal)
    }

    sealed class Decision {
        data class Continue(val newTotalBytes: Long) : Decision()
        data class Abort(val reason: String) : Decision()
    }

    data class Timeouts(
        val connectSec: Long = 10,
        val readSec: Long = 30,
        val writeSec: Long = 30,
        val callSec: Long = 120,
    )
}
