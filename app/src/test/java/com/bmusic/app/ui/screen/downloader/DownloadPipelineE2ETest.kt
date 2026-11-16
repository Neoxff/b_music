package com.bmusic.app.ui.screen.downloader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bmusic.app.core.BMusicDatabase
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadState
import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.MediaStream
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.error.DownloadErrorCode
import com.bmusic.app.downloader.error.ErrorAction
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.TraceSession
import com.bmusic.app.downloader.trace.TraceStep
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test **bout en bout** du pipeline « Lien » (Robolectric + moteur factice).
 *
 * Chaîne prouvée : lien valide → aperçu → téléchargement (entité en base) →
 * fichier simulé dans MediaStore → présent dans la liste de l'historique.
 *
 * Plus **un test par échec typé** : chaque situation doit produire son message
 * français et son action (jamais un message générique).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.bmusic.app.test.TestApplication::class)
class DownloadPipelineE2ETest {

    private lateinit var db: BMusicDatabase
    private lateinit var dao: DownloadDao
    private lateinit var vm: DownloadViewModel
    private lateinit var engine: FakeEngine

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Moteur factice : renvoie une info connue ou lève une erreur typée. */
    private class FakeEngine(
        private val error: ExtractorError? = null,
    ) : MediaExtractorEngine {
        override suspend fun resolve(url: String, trace: TraceSession?): MediaInfo {
            trace?.ok(TraceStep.VALIDATION, "youtube.com")
            trace?.ok(TraceStep.SITE_RECONNU, "FakeService")
            trace?.ok(TraceStep.MOTEUR_INITIALISE, "fake")
            if (error != null) {
                trace?.fail(TraceStep.EXTRACTION, DownloadError.fromExtractor(error))
                throw error
            }
            trace?.ok(TraceStep.EXTRACTION, "titre")
            return MediaInfo(
                title = "Titre de test",
                author = "Artiste de test",
                durationMs = 125_000,
                artworkUrl = null,
                streams = listOf(
                    MediaStream(
                        kind = StreamKind.AUDIO,
                        format = "m4a",
                        bitrateKbps = 128,
                        sizeBytes = 2_000_000,
                        url = "https://stream.example.org/audio.m4a",
                    ),
                    MediaStream(
                        kind = StreamKind.VIDEO,
                        format = "mp4",
                        bitrateKbps = 720,
                        sizeBytes = 9_000_000,
                        url = "https://stream.example.org/video.mp4",
                    ),
                ),
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(context, BMusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.downloadDao()
        engine = FakeEngine()
        val manager = DownloadManager(engine, DownloaderHttpClient(), dao, context)
        vm = DownloadViewModel(engine, manager, dao, Preferences)
        Preferences.downloaderEnabled = true
        DownloadTrace.clear()
        ensureWorkManagerDoesNotRunJobs()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
        DownloadTrace.clear()
    }

    /**
     * WorkManager est initialisé mais **n'exécute aucun job** : le test reste
     * déterministe (aucun accès réseau, aucune course de fond).
     */
    private fun ensureWorkManagerDoesNotRunJobs() {
        runCatching { WorkManager.getInstance(context) }.getOrElse {
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setExecutor { /* les jobs ne sont jamais exécutés */ }
                    .build(),
            )
        }
    }

    @Test
    fun `un lien valide produit un apercu`() = runTest {
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()

        val link = vm.link.value
        assertNull("erreur inattendue : ${link.error}", link.error)
        assertNotNull("aucun aperçu produit", link.info)
        assertEquals("Titre de test", link.info!!.title)
        assertEquals("m4a", link.quality)
        assertTrue("l'aperçu doit être prêt", !link.busy)
    }

    @Test
    fun `le journal contient les etapes du pipeline reussi`() = runTest {
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()

        val steps = DownloadTrace.sessions().first().events.map { it.step }
        assertTrue("VALIDATION absente", steps.contains(TraceStep.VALIDATION))
        assertTrue("SITE_RECONNU absent", steps.contains(TraceStep.SITE_RECONNU))
        assertTrue("EXTRACTION absente", steps.contains(TraceStep.EXTRACTION))
        assertTrue("APERCU absent", steps.contains(TraceStep.APERCU))
        assertTrue("FORMAT_CHOISI absent", steps.contains(TraceStep.FORMAT_CHOISI))
        assertNull("aucune erreur attendue", DownloadTrace.sessions().first().failureCode())
    }

    @Test
    fun `un telechargement cree l entite puis le fichier simule apparait dans la liste`() = runTest {
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()
        vm.enqueue()

        // `enqueue()` bascule sur Dispatchers.IO (vrai thread) : l'attente est
        // explicite pour rester deterministe.
        assertTrue(
            "l entite doit etre en base",
            awaitUntil { dao.recent(10).isNotEmpty() },
        )
        val entity = dao.recent(10).first()
        assertEquals("Titre de test", entity.title)
        assertEquals("m4a", entity.format)
        assertEquals(DownloadState.QUEUED, entity.state)

        // Fichier « dans MediaStore » : URI simulée + completion.
        val mediaStoreUri = "content://media/external/audio/media/4242"
        dao.markCompleted(entity.id, mediaStoreUri, System.currentTimeMillis())

        val completed = dao.recent(10).first()
        assertEquals(DownloadState.COMPLETED, completed.state)
        assertEquals(mediaStoreUri, completed.finalPath)
        assertTrue(
            "la liste doit contenir le telechargement",
            awaitUntil { vm.downloads.value.isNotEmpty() },
        )
        assertTrue(
            "la liste doit contenir l entite completee",
            awaitUntil { vm.downloads.value.any { it.id == entity.id } },
        )
    }

    /** Attend qu'une condition devienne vraie (travail sur Dispatchers.IO). */
    private suspend fun awaitUntil(
        timeoutMs: Long = 10_000,
        predicate: suspend () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return true
            delay(20)
        }
        return predicate()
    }

    @Test
    fun `un lien invalide affiche LIEN_INVALIDE et l action Reessayer`() = runTest {
        vm = newViewModel(ExtractorError.UrlInvalid("schema"))
        vm.onUrlChange("ftp://example.org/x")
        vm.resolveLink()

        assertEquals(DownloadErrorCode.LIEN_INVALIDE, vm.link.value.errorCode)
        assertEquals(ErrorAction.REESSAYER, vm.link.value.errorAction)
        assertNotNull(vm.link.value.error)
    }

    @Test
    fun `un site sans moteur affiche SITE_NON_PRIS_EN_CHARGE et l action sites`() = runTest {
        vm = newViewModel(ExtractorError.Unsupported("tiktok"))
        vm.onUrlChange("https://www.tiktok.com/@user/video/1")
        vm.resolveLink()

        assertEquals(DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE, vm.link.value.errorCode)
        assertEquals(ErrorAction.VOIR_SITES_SUPPORTS, vm.link.value.errorAction)
    }

    @Test
    fun `un contenu prive affiche CONTENU_PRIVE`() = runTest {
        vm = newViewModel(ExtractorError.ProtectedContent())
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()

        assertEquals(DownloadErrorCode.CONTENU_PRIVE, vm.link.value.errorCode)
    }

    @Test
    fun `un site change affiche MOTEUR_A_METTRE_A_JOUR`() = runTest {
        vm = newViewModel(ExtractorError.SourceUnavailable("site changed"))
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()

        assertEquals(DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR, vm.link.value.errorCode)
        assertEquals(ErrorAction.VOIR_SITES_SUPPORTS, vm.link.value.errorAction)
    }

    @Test
    fun `une panne reseau affiche HORS_LIGNE`() = runTest {
        vm = newViewModel(ExtractorError.Network("timeout"))
        vm.onUrlChange("https://www.youtube.com/watch?v=abc123")
        vm.resolveLink()

        assertEquals(DownloadErrorCode.HORS_LIGNE, vm.link.value.errorCode)
        assertEquals(ErrorAction.REESSAYER, vm.link.value.errorAction)
    }

    @Test
    fun `l erreur est tracee dans le journal avec son code`() = runTest {
        vm = newViewModel(ExtractorError.Unsupported("tiktok"))
        vm.onUrlChange("https://www.tiktok.com/@user/video/1")
        vm.resolveLink()

        val failure = DownloadTrace.sessions().first().firstFailure()
        assertNotNull("l echec doit etre trace", failure)
        assertEquals(DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE, failure!!.errorCode)
    }

    private fun newViewModel(error: ExtractorError): DownloadViewModel {
        val failing = FakeEngine(error)
        val manager = DownloadManager(failing, DownloaderHttpClient(), dao, context)
        return DownloadViewModel(failing, manager, dao, Preferences)
    }
}
