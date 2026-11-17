package com.bmusic.app.downloader.progress

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.worker.DownloadWorker

/**
 * BUILD — notification de téléchargement (design doc 15).
 * Lit la MÊME source [DownloadProgress] que l'onglet Lien et l'onglet Historique.
 *
 * - setProgress(100, percent, indeterminate) — indéterminé si total inconnu
 * - texte « 63 % · 2,4 Mo/s »
 * - actions pause/reprendre + annuler (icônes ; libellé court imposé par Android)
 * - setOnlyAlertOnce + setOngoing pendant le téléchargement
 * - canal « Téléchargements » à faible importance (déjà créé par DownloadWorker)
 */
object DownloadNotification {

    /** Texte pur, sans Android Framework — testable en JVM. */
    fun text(progress: DownloadProgress): String {
        val pct = progress.percent?.let { "$it %" } ?: "…"
        val speed = ProgressFormatter.speed(progress.speedBytesPerSecond)
        return "$pct · $speed"
    }

    /**
     * Construit la notification. Les [PendingIntent] sont passés en paramètre
     * (le worker les crée en FLAG_IMMUTABLE) : cette fonction reste testable
     * seule sous Robolectric, sans dépendance au receiver.
     */
    fun build(
        context: Context,
        progress: DownloadProgress,
        pauseResumeIntent: PendingIntent,
        cancelIntent: PendingIntent,
    ): Notification {
        val pauseResumeLabel = if (progress.state == DownloadState.PAUSED) "Reprendre" else "Pause"
        return NotificationCompat.Builder(context, DownloadWorker.CHANNEL_ID)
            .setContentTitle("Téléchargement B Music")
            .setContentText(text(progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(
                progress.state == DownloadState.DOWNLOADING ||
                    progress.state == DownloadState.QUEUED ||
                    progress.state == DownloadState.PAUSED
            )
            .setProgress(
                100,
                progress.percent ?: 0,
                progress.percent == null,
            )
            .addAction(android.R.drawable.ic_media_pause, pauseResumeLabel, pauseResumeIntent)
            .addAction(android.R.drawable.ic_delete, "Annuler", cancelIntent)
            .build()
    }
}
