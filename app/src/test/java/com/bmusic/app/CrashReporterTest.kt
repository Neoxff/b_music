package com.bmusic.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.TraceStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Filet de crash — le seul moyen de connaître la cause d'un crash sans adb.
 *
 * Prouve : le rapport est écrit au crash, il contient le type, le message,
 * la pile (30 lignes max), la version, l'appareil et l'étape du journal en
 * cours — et **aucune donnée personnelle** (les URL sont réduites à leur
 * domaine).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.bmusic.app.test.TestApplication::class)
class CrashReporterTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        CrashReporter.clearPendingReport(context)
        DownloadTrace.clear()
    }

    @Test
    fun `le rapport contient type pile version appareil et etape courante`() {
        DownloadTrace.startSession("https://www.youtube.com/watch?v=abc")
            .ok(TraceStep.EXTRACTION, "ok")

        val report = CrashReporter.buildReport(
            context, Thread.currentThread(), IllegalStateException("boom")
        )

        assertTrue(report.contains("type=java.lang.IllegalStateException"))
        assertTrue(report.contains("message=boom"))
        assertTrue(report.contains("app_version="))
        assertTrue(report.contains("version_code="))
        assertTrue(report.contains("android="))
        assertTrue(report.contains("device="))
        assertTrue(report.contains("current_step=EXTRACTION"))
        assertTrue("domaine attendu dans le rapport", report.contains("www.youtube.com"))
    }

    @Test
    fun `le rapport ne contient aucune URL complete`() {
        val report = CrashReporter.buildReport(
            context,
            Thread.currentThread(),
            IllegalStateException("echec sur https://vimeo.com/12345/secret"),
        )
        assertFalse("identifiant dans le rapport", report.contains("secret"))
        assertFalse("chemin dans le rapport", report.contains("vimeo.com/12345"))
        assertTrue("domaine attendu dans le rapport", report.contains("vimeo.com"))
    }

    @Test
    fun `la pile est limitee a 30 lignes`() {
        val report = CrashReporter.buildReport(
            context, Thread.currentThread(), RuntimeException("deep")
        )
        val lines = report
            .substringAfter("--- pile (30 premieres lignes) ---")
            .lines()
            .filter { it.isNotBlank() }
        assertTrue("trop de lignes de pile : ${lines.size}", lines.size <= 30)
    }

    @Test
    fun `un crash ecrit un rapport que l on peut relire puis effacer`() {
        CrashReporter.writeReport(
            context, Thread.currentThread(), IllegalStateException("boom")
        )
        assertTrue(CrashReporter.hasPendingReport(context))
        val pending = CrashReporter.pendingReport(context)
        assertTrue(pending!!.contains("type=java.lang.IllegalStateException"))

        CrashReporter.clearPendingReport(context)
        assertFalse(CrashReporter.hasPendingReport(context))
    }

    @Test
    fun `le gestionnaire global ecrit le rapport lors d un crash de thread`() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        try {
            CrashReporter.install(context)
            val t = Thread { throw IllegalStateException("crash de thread") }
            t.start()
            t.join()

            assertTrue("rapport de crash non ecrit", CrashReporter.hasPendingReport(context))
            assertTrue(
                CrashReporter.pendingReport(context)!!.contains("crash de thread")
            )
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }
}
