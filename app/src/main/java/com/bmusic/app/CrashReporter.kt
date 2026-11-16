package com.bmusic.app

import android.content.Context
import android.os.Build
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.DownloadTraceStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Filet de diagnostic : **gestionnaire d'exceptions global**.
 *
 * Sans adb, c'est le seul moyen d'obtenir la cause réelle d'un crash : le
 * rapport est écrit sur disque au moment du crash, puis proposé au prochain
 * lancement (« L'app s'est arrêtée » → « Partager le rapport »).
 *
 * Le rapport contient : type d'exception, message, 30 premières lignes de pile,
 * version de l'application, version d'Android, appareil, et l'étape du journal
 * en cours. **Aucune donnée personnelle** : toute URL est réduite à son
 * domaine.
 */
object CrashReporter {

    private const val FILE_NAME = "crash_report.txt"
    private const val MAX_STACK_LINES = 30

    private var installed = false

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Installe le gestionnaire global (en chaînant l'ancien). */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeReport(appContext, thread, error) }
            // On laisse l'ancien gestionnaire faire son travail (l'app se
            // ferme comme d'habitude) : on ne change pas le comportement.
            previous?.uncaughtException(thread, error)
        }
    }

    /** Écrit le rapport de crash sur disque. */
    fun writeReport(context: Context, thread: Thread, error: Throwable) {
        val report = buildReport(context, thread, error)
        runCatching { file(context).writeText(report) }
        // L'étape en cours est également ajoutée au journal d'étapes.
        val step = DownloadTrace.events().firstOrNull()?.step?.code ?: "AUCUNE"
        runCatching {
            val session = DownloadTrace.startSession("crash://${error::class.java.simpleName}")
            session.fail(
                com.bmusic.app.downloader.trace.TraceStep.EXTRACTION,
                DownloadError(
                    com.bmusic.app.downloader.error.DownloadErrorCode.ECHEC_EXTRACTION,
                    "crash: ${error::class.java.simpleName}",
                ),
            )
            DownloadTraceStore.save(context)
        }
    }

    /** Construit le rapport texte (aucune donnée personnelle). */
    fun buildReport(context: Context, thread: Thread, error: Throwable): String {
        val stack = StringWriter().also { sw ->
            PrintWriter(sw).use { error.printStackTrace(it) }
        }.toString()
        val lines = stack.lines().take(MAX_STACK_LINES)
        val step = DownloadTrace.events().firstOrNull()?.step?.code ?: "AUCUNE"
        val lastSession = DownloadTrace.sessions().firstOrNull()

        return buildString {
            append("B Music - rapport de crash\n")
            append("app_version=").append(appVersion(context)).append('\n')
            append("version_code=").append(BuildConfig.VERSION_CODE).append('\n')
            append("android=").append(Build.VERSION.RELEASE)
                .append(" (api ").append(Build.VERSION.SDK_INT).append(")\n")
            append("device=").append(Build.MANUFACTURER).append(' ')
                .append(Build.MODEL).append('\n')
            append("thread=").append(thread.name).append('\n')
            append("type=").append(error::class.java.name).append('\n')
            append("message=").append(DownloadError.redact(error.message) ?: "-").append('\n')
            append("current_step=").append(step).append('\n')
            append("session_domain=")
                .append(lastSession?.domain ?: "-").append('\n')
            append("session_hash=").append(lastSession?.linkHash ?: "-").append('\n')
            append("--- pile (30 premieres lignes) ---\n")
            // La pile est redactée elle aussi : la première ligne contient
            // le message de l'exception, qui peut embarquer une URL.
            lines.forEach { append(DownloadError.redact(it) ?: it).append('\n') }
        }
    }

    /** Un rapport de crash en attente ? */
    fun hasPendingReport(context: Context): Boolean = file(context).let { it.exists() && it.length() > 0 }

    /** Rapport en attente (ou null). */
    fun pendingReport(context: Context): String? =
        runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()

    /** Efface le rapport en attente. */
    fun clearPendingReport(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun appVersion(context: Context): String =
        runCatching {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: "?"
        }.getOrDefault("?")
}
