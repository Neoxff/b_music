package com.bmusic.app.downloader.progress

import com.bmusic.app.downloader.data.DownloadState

/**
 * BUILD — modèle de progression (design doc 15). Source unique lue par
 * l'onglet Lien, l'onglet Historique et la notification.
 *
 * [bytesTotal] vaut null quand le serveur n'a pas envoyé de Content-Length
 * (flux direct) : dans ce cas [percent] est null et la barre est indéterminée.
 */
data class DownloadProgress(
    val id: Long,
    val bytesDownloaded: Long,
    val bytesTotal: Long?,
    val percent: Int?,
    val speedBytesPerSecond: Long,
    val remainingSeconds: Long?,
    val state: DownloadState,
)
