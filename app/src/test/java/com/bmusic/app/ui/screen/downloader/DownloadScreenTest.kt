package com.bmusic.app.ui.screen.downloader

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.bmusic.app.core.BMusicDatabase
import com.bmusic.app.downloader.extractor.StubExtractorEngine
import com.bmusic.app.downloader.manager.DownloadManager
import com.bmusic.app.downloader.manager.DownloaderHttpClient
import com.bmusic.app.test.TestApplication
import com.bmusic.app.test.TestComposeActivity
import com.bmusic.app.util.Preferences
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Écran Télécharger — onglet **Lien** en premier et par défaut.
 *
 * Preuves automatisées (Robolectric + Compose, aucune commande adb) :
 * - l'onglet Lien est celui affiché par défaut (le contenu Historique n'est pas composé) ;
 * - la zone de saisie est **centrée horizontalement** (centre X du titre = centre X de l'écran) ;
 * - la zone de saisie est **centrée verticalement** (centre du bloc = centre de la zone de contenu).
 *
 * Le centrage est mesuré en pixels puis converti en dp : la tolérance est de 2 dp en
 * horizontal et de 16 dp en vertical (arrondi des métriques de texte Compose).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class DownloadScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<TestComposeActivity>()

    private fun newViewModel(): DownloadViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = GlobalContext.get().get<BMusicDatabase>().downloadDao()
        val engine = StubExtractorEngine()
        val manager = DownloadManager(engine, DownloaderHttpClient(), dao, context)
        return DownloadViewModel(engine, manager, dao, Preferences)
    }

    private fun density(): Float = compose.activity.resources.displayMetrics.density

    @Test
    fun `le ViewModel demarre sur l onglet Lien`() {
        val vm = newViewModel()
        assertEquals("onglet par défaut attendu = Lien (0)", 0, vm.tab.value)
    }

    @Test
    fun `prefill avec un lien conserve l onglet Lien`() {
        val vm = newViewModel()
        vm.prefill("https://www.youtube.com/watch?v=abc123", toLink = true)
        assertEquals(0, vm.tab.value)
        assertEquals("https://www.youtube.com/watch?v=abc123", vm.link.value.url)
    }

    @Test
    fun `onglet Lien affiche et actif par defaut`() {
        compose.activity.setContent { DownloadScreen(vm = newViewModel()) }

        compose.onNodeWithContentDescription("Onglet Lien").assertIsDisplayed()
        compose.onNodeWithContentDescription("Onglet Historique").assertIsDisplayed()

        // Contenu de l'onglet Lien composé…
        compose.onNodeWithText("Coller un lien").assertIsDisplayed()
        // …et contenu de l'onglet Historique absent.
        compose.onNodeWithText("Aucun téléchargement pour l'instant").assertDoesNotExist()
    }

    @Test
    fun `la zone de saisie est centree horizontalement`() {
        compose.activity.setContent { DownloadScreen(vm = newViewModel()) }

        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("Coller un lien").fetchSemanticsNode().boundsInRoot
        val deltaPx = abs(title.center.x - root.center.x)

        assertTrue(
            "titre décentré horizontalement de ${deltaPx}px (${deltaPx / density()}dp)",
            deltaPx <= 2f * density(),
        )
    }

    @Test
    fun `la zone de saisie est centree verticalement`() {
        compose.activity.setContent { DownloadScreen(vm = newViewModel()) }

        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("Coller un lien").fetchSemanticsNode().boundsInRoot
        val hint = compose
            .onNodeWithText("Collez un lien YouTube, TikTok, Instagram…")
            .fetchSemanticsNode().boundsInRoot

        // Bloc = icône (48 dp) + espacement (12 dp) + titre + surface verre.
        val blockTop = title.top - 60f * density()
        val blockCenter = (blockTop + hint.bottom) / 2f

        // Zone de contenu = sous la barre haute (TopAppBar 64 dp + barre d'onglets 56 dp).
        val topBar = 120f * density()
        val expected = topBar + (root.height - topBar) / 2f
        val deltaPx = abs(blockCenter - expected)

        assertTrue(
            "bloc non centré verticalement : centre=${blockCenter}px attendu=${expected}px " +
                "(écart ${deltaPx}px = ${deltaPx / density()}dp)",
            deltaPx <= 16f * density(),
        )
    }
}
