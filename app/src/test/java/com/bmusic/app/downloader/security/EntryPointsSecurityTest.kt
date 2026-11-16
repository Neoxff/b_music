package com.bmusic.app.downloader.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bmusic.app.core.BMusicDatabase
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.engine.ExtractorError
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.engine.MediaInfo
import com.bmusic.app.downloader.engine.MediaStream
import com.bmusic.app.downloader.engine.StreamKind
import com.bmusic.app.downloader.error.DownloadErrorCode
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.downloader.registry.SiteRegistry
import com.bmusic.app.downloader.registry.SiteStatus
import com.bmusic.app.downloader.trace.TraceSession
import com.bmusic.app.ui.screen.downloader.DownloadViewModel
import com.bmusic.app.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sécurité des **points d'entrée** et du **feature flag**.
 *
 * Prouve :
 * - flag off → **aucun** appel au moteur, aucune entité mise en file
 *   (donc aucun appel réseau possible) ;
 * - un partage (ACTION_SEND) ne déclenche **aucune** résolution ni
 *   téléchargement : le lien est seulement pré-rempli ;
 * - un texte forgé (javascript:, données malveillantes) est refusé **avant**
 *   d'atteindre le moteur ;
 * - une URL vers le réseau local est refusée par le registre des sites.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.bmusic.app.test.TestApplication::class)
class EntryPointsSecurityTest {

    private lateinit var db: BMusicDatabase
    private lateinit var dao: DownloadDao
    private lateinit var vm: DownloadViewModel
    private val engineCalls = AtomicInteger(0)

    /**
     * Moteur espion : compte les appels et applique le même pré-contrôle que
     * le moteur réel (registre des sites) — aucune requête réseau possible.
     */
    private inner class SpyEngine : MediaExtractorEngine {
        override suspend fun resolve(url: String, trace: TraceSession?): MediaInfo {
            engineCalls.incrementAndGet()
            val site = SiteRegistry.lookup(url)
            if (site == null || site.status == SiteStatus.NON_PRIS_EN_CHARGE) {
                throw ExtractorError.Unsupported(site?.id ?: "unknown-site")
            }
            return MediaInfo(
                title = "Titre",
                author = "Artiste",
                durationMs = 1_000,
                artworkUrl = null,
                streams = listOf(
                    MediaStream(
                        kind = StreamKind.AUDIO,
                        format = "m4a",
                        bitrateKbps = 128,
                        sizeBytes = 100,
                        url = "https://cdn.example.org/a.m4a",
                    ),
                ),
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BMusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.downloadDao()
        val engine = SpyEngine()
        val manager = DownloadManager(engine, DownloaderHttpClient(), dao, context)
        vm = DownloadViewModel(engine, manager, dao, Preferences)
        Preferences.downloaderEnabled = true
        engineCalls.set(0)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
        Preferences.downloaderEnabled = true
    }

    @Test
    fun `flag off - le moteur n est jamais appele`() = runTest {
        Preferences.downloaderEnabled = false
        vm.onUrlChange("https://www.youtube.com/watch?v=abc")
        vm.resolveLink()

        assertEquals("aucun appel moteur attendu avec le flag off", 0, engineCalls.get())
        assertNull("aucun aperçu attendu", vm.link.value.info)
    }

    @Test
    fun `flag off - aucune entite mise en file`() = runTest {
        Preferences.downloaderEnabled = false
        vm.onUrlChange("https://www.youtube.com/watch?v=abc")
        vm.resolveLink()
        vm.enqueue()

        assertEquals("rien ne doit etre mis en file", 0, dao.recent(10).size)
    }

    @Test
    fun `un partage ne declenche ni resolution ni telechargement`() = runTest {
        // Ce que fait le fragment sur un ACTION_SEND : prefill seul.
        vm.prefill("https://www.youtube.com/watch?v=abc", toLink = true)

        assertEquals("le partage ne doit pas lancer la resolution", 0, engineCalls.get())
        assertNull("aucun apercu attendu", vm.link.value.info)
        assertEquals("aucun telechargement attendu", 0, dao.recent(10).size)
    }

    @Test
    fun `un texte forge est refuse avant d atteindre le moteur`() = runTest {
        listOf(
            "javascript:alert(1)",
            "file:///etc/passwd",
            "content://media/external/audio/media/42",
            "https://user:pass@example.com/x",
            "https://example.com/a b",
        ).forEach { forged ->
            engineCalls.set(0)
            vm.prefill(forged, toLink = true)
            vm.resolveLink()

            assertEquals("moteur appele pour $forged", 0, engineCalls.get())
            assertEquals(
                "code attendu pour $forged",
                DownloadErrorCode.LIEN_INVALIDE,
                vm.link.value.errorCode,
            )
        }
    }

    @Test
    fun `une URL vers le reseau local est refusee par le registre`() = runTest {
        vm.prefill("http://127.0.0.1/admin", toLink = true)
        vm.resolveLink()

        assertNull("aucun apercu attendu", vm.link.value.info)
        assertEquals(
            DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE,
            vm.link.value.errorCode,
        )
        assertEquals("aucun telechargement attendu", 0, dao.recent(10).size)
    }

    @Test
    fun `l URL stockee dans l historique est normalisee`() = runTest {
        vm.prefill(
            "https://www.youtube.com/watch?v=abc&utm_source=newsletter&fbclid=xyz#t=30",
            toLink = true,
        )
        vm.resolveLink()

        assertEquals(
            "l historique et le moteur ne doivent voir que l URL nettoyee",
            "https://www.youtube.com/watch?v=abc",
            vm.link.value.url,
        )
    }
}
