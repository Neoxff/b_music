package com.bmusic.app.downloader.trace

import android.content.Context
import java.io.File

/**
 * Persistance du journal d'étapes sur disque (fichier privé de l'app).
 *
 * Le fichier contient **aucune donnée personnelle** : domaine + hash seulement.
 * Il est lisible par l'utilisateur via l'écran « Journal » → « Partager ».
 */
object DownloadTraceStore {

    private const val FILE_NAME = "download_trace.log"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Écrit le journal courant. */
    fun save(context: Context) {
        runCatching {
            file(context).writeText(DownloadTrace.report())
        }
    }

    /** Relit le journal depuis le disque (au démarrage de l'app). */
    fun load(context: Context) {
        runCatching {
            val f = file(context)
            if (f.exists() && f.length() > 0) {
                DownloadTrace.importReport(f.readText())
            }
        }
    }

    /** Efface le journal (fichier + mémoire). */
    fun clear(context: Context) {
        DownloadTrace.clear()
        runCatching { file(context).delete() }
    }
}
