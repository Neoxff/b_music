package com.bmusic.app.downloader.error

import com.bmusic.app.downloader.engine.ExtractorError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Erreurs typées : chaque situation a son code, son message français et son action.
 * Aucune erreur ne doit être affichée sous un autre nom, aucun message vide.
 */
class DownloadErrorTest {

    @Test
    fun `chaque code a un message utilisateur et une action`() {
        DownloadErrorCode.entries.forEach { code ->
            val error = DownloadError(code)
            assertTrue("message vide pour $code", error.userMessage.isNotBlank())
            assertTrue("action absente pour $code", error.action.name.isNotBlank())
            assertTrue("ligne technique vide pour $code", error.technicalLine().isNotBlank())
        }
    }

    @Test
    fun `chaque erreur du moteur devient un code typé unique`() {
        assertEquals(
            DownloadErrorCode.LIEN_INVALIDE,
            DownloadError.fromExtractor(ExtractorError.UrlInvalid("x")).code,
        )
        assertEquals(
            DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE,
            DownloadError.fromExtractor(ExtractorError.Unsupported("x")).code,
        )
        assertEquals(
            DownloadErrorCode.CONTENU_PRIVE,
            DownloadError.fromExtractor(ExtractorError.ProtectedContent()).code,
        )
        assertEquals(
            DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR,
            DownloadError.fromExtractor(ExtractorError.SourceUnavailable("boom")).code,
        )
        assertEquals(
            DownloadErrorCode.HORS_LIGNE,
            DownloadError.fromExtractor(ExtractorError.Network("timeout")).code,
        )
        assertEquals(
            DownloadErrorCode.ECHEC_EXTRACTION,
            DownloadError.fromExtractor(IllegalStateException("?" )).code,
        )
    }

    @Test
    fun `les erreurs du transfert sont distinguees`() {
        assertEquals(
            DownloadErrorCode.HORS_LIGNE,
            DownloadError.fromTransfer(java.io.IOException("connection reset")).code,
        )
        assertEquals(
            DownloadErrorCode.STOCKAGE_INDISPONIBLE,
            DownloadError.fromTransfer(SecurityException("denied")).code,
        )
        assertEquals(
            DownloadErrorCode.TELECHARGEMENT_ECHEC,
            DownloadError.fromTransfer(IllegalStateException("x")).code,
        )
    }

    @Test
    fun `le message technique ne contient jamais le lien complet`() {
        val url = "https://www.youtube.com/watch?v=IDENTIFIANT_SECRET&t=30"
        val error = DownloadError.fromExtractor(ExtractorError.Unsupported(url))
        val message = error.technicalMessage.orEmpty()
        assertFalse("lien complet dans le message technique", message.contains(url))
        assertFalse("identifiant dans le message technique", message.contains("IDENTIFIANT_SECRET"))
        assertTrue("domaine attendu dans le message", message.contains("youtube"))
    }

    @Test
    fun `les actions proposees correspondent a la situation`() {
        assertEquals(
            ErrorAction.VOIR_SITES_SUPPORTS,
            DownloadError(DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE).action,
        )
        assertEquals(
            ErrorAction.OUVRIR_JOURNAL,
            DownloadError(DownloadErrorCode.ECHEC_EXTRACTION).action,
        )
        assertEquals(
            ErrorAction.REESSAYER,
            DownloadError(DownloadErrorCode.HORS_LIGNE).action,
        )
    }
}
