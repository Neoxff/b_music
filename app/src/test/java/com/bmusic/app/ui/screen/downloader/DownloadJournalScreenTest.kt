package com.bmusic.app.ui.screen.downloader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.activity.compose.setContent
import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.error.DownloadErrorCode
import com.bmusic.app.downloader.trace.DownloadTrace
import com.bmusic.app.downloader.trace.TraceStep
import com.bmusic.app.test.TestComposeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Écran « Journal » — l'outil de diagnostic dubuild **release**.
 *
 * Prouve : la liste des tentatives, le détail des étapes, l'export du rapport
 * (« Partager ») et l'effacement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.bmusic.app.test.TestApplication::class)
class DownloadJournalScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<TestComposeActivity>()

    @Before
    fun setUp() {
        DownloadTrace.clear()
    }

    @Test
    fun `etat vide quand aucune tentative`() {
        compose.activity.setContent { DownloadJournalContent(sessions = emptyList()) }
        compose.onNodeWithText("No attempt recorded yet.").assertIsDisplayed()
    }

    @Test
    fun `une tentative est listee avec son domaine et son hash`() {
        val session = DownloadTrace.startSession("https://www.youtube.com/watch?v=abc")
        session.ok(TraceStep.VALIDATION)
        session.ok(TraceStep.SITE_RECONNU, "YoutubeService")

        compose.activity.setContent { DownloadJournalContent(sessions = DownloadTrace.sessions()) }

        compose.onNodeWithText("www.youtube.com").assertIsDisplayed()
        compose.onNodeWithText("Link hash: ${session.linkHash} · 2 steps").assertIsDisplayed()
    }

    @Test
    fun `une erreur typee est visible dans la liste`() {
        val session = DownloadTrace.startSession("https://vimeo.com/12345")
        session.fail(
            TraceStep.SITE_RECONNU,
            DownloadError(DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE, "vimeo"),
        )

        compose.activity.setContent { DownloadJournalContent(sessions = DownloadTrace.sessions()) }
        compose.onNodeWithText("vimeo.com").assertIsDisplayed()
        compose.onNodeWithText("SITE_NON_PRIS_EN_CHARGE").assertIsDisplayed()
    }

    @Test
    fun `le detail d une tentative affiche ses etapes`() {
        val session = DownloadTrace.startSession("https://soundcloud.com/x/y")
        session.ok(TraceStep.VALIDATION)
        session.ok(TraceStep.EXTRACTION, "ok")

        compose.activity.setContent { DownloadJournalContent(sessions = DownloadTrace.sessions()) }
        compose.onNodeWithText("soundcloud.com").performClick()

        compose.onNodeWithText("VALIDATION").assertIsDisplayed()
        compose.onNodeWithText("EXTRACTION").assertIsDisplayed()
    }

    @Test
    fun `le bouton Partager produit le rapport texte`() {
        var shared: String? = null
        DownloadTrace.startSession("https://youtube.com/watch?v=xyz").ok(TraceStep.VALIDATION)

        compose.activity.setContent {
            DownloadJournalContent(
                sessions = DownloadTrace.sessions(),
                onShare = { shared = it },
            )
        }

        compose.onNodeWithText("Share report").performClick()
        assertTrue("rapport vide", !shared.isNullOrBlank())
        assertTrue("le rapport doit contenir l'étape", shared!!.contains("VALIDATION"))
        assertTrue("le rapport doit contenir le domaine", shared!!.contains("youtube.com"))
        assertTrue(
            "le rapport ne doit pas contenir le lien complet",
            !shared!!.contains("watch?v=xyz"),
        )
    }

    @Test
    fun `le bouton Effacer vide le journal`() {
        DownloadTrace.startSession("https://youtube.com/watch?v=xyz").ok(TraceStep.VALIDATION)
        var cleared = false

        compose.activity.setContent {
            DownloadJournalContent(
                sessions = DownloadTrace.sessions(),
                onClear = {
                    DownloadTrace.clear()
                    cleared = true
                },
            )
        }

        compose.onNodeWithText("Clear").performClick()
        assertTrue("le journal n'a pas ete efface", cleared)
        assertEquals(0, DownloadTrace.sessions().size)
    }
}
