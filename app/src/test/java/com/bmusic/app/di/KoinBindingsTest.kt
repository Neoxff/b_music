package com.bmusic.app.di

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import com.bmusic.app.core.BMusicDatabase
import com.bmusic.app.data.local.MediaStoreWriter
import com.bmusic.app.data.local.room.InclExclDao
import com.bmusic.app.data.repository.PlaylistRepository
import com.bmusic.app.data.repository.RealAlbumRepository
import com.bmusic.app.data.repository.RealArtistRepository
import com.bmusic.app.data.repository.RealSongRepository
import com.bmusic.app.data.repository.Repository
import com.bmusic.app.data.repository.SmartRepository
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.engine.MediaExtractorEngine
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.test.TestApplication
import com.bmusic.app.ui.screen.downloader.DownloadViewModel
import com.bmusic.app.ui.screen.library.search.SearchViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Régression : **tout type injecté par Koin doit être résolvable à l'exécution**.
 *
 * Cause racine du crash « l'onglet Téléchargement crache » (v1.4.3) :
 * `DownloadDao` n'était lié par AUCUN module Koin. Au premier accès à l'onglet,
 * `DownloadViewModel` demandait `get<DownloadDao>()` → `NoBeanDefFoundException`
 * → crash de l'application.
 *
 * Ce test résout à l'exécution **les 14 types injectés par le code applicatif**
 * (liste exhaustive établie par `scripts/check-koin-bindings.py`). Un binding
 * manquant le fait échouer ici, avant d'atteindre un téléphone.
 *
 * Contrôle statique complémentaire : `python3 scripts/check-koin-bindings.py app/src/main/java`
 * → exit 0 attendu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class KoinBindingsTest {

    private val koin get() = GlobalContext.get()

    @Test
    fun `DownloadDao est resolvable`() {
        // Binding ajouté dans roomModule (MainModule.kt) : c'est le correctif.
        assertNotNull(
            "DownloadDao non lie -> crash au tap sur l onglet Telecharger",
            koin.get<DownloadDao>(),
        )
    }

    @Test
    fun `le graphe du downloader est complet`() {
        assertNotNull(koin.get<DownloadDao>())
        assertNotNull(koin.get<MediaExtractorEngine>())
        assertNotNull(koin.get<DownloadManager>())
        assertNotNull(koin.get<DownloadViewModel>())
    }

    @Test
    fun `tous les types injectes dans le code applicatif sont resolubles`() {
        // Liste exhaustive des 14 types injectes (get<T>() / by inject() / viewModel<T>())
        // dans app/src/main, etablie par scripts/check-koin-bindings.py.
        val resolus = linkedMapOf<String, Any?>(
            "BMusicDatabase" to koin.get<BMusicDatabase>(),
            "ContentResolver" to koin.get<ContentResolver>(),
            "Context" to koin.get<Context>(),
            "DownloadDao" to koin.get<DownloadDao>(),
            "InclExclDao" to koin.get<InclExclDao>(),
            "MediaStoreWriter" to koin.get<MediaStoreWriter>(),
            "PlaylistRepository" to koin.get<PlaylistRepository>(),
            "RealAlbumRepository" to koin.get<RealAlbumRepository>(),
            "RealArtistRepository" to koin.get<RealArtistRepository>(),
            "RealSongRepository" to koin.get<RealSongRepository>(),
            "Repository" to koin.get<Repository>(),
            "SearchViewModel" to koin.get<SearchViewModel>(),
            "SharedPreferences" to koin.get<SharedPreferences>(),
            "SmartRepository" to koin.get<SmartRepository>(),
        )
        val manquants = resolus.filterValues { it == null }.keys
        assertTrue(
            "types injectes non resolus (NoBeanDefFoundException au runtime) : $manquants",
            manquants.isEmpty(),
        )
    }
}
