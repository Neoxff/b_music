package com.bmusic.app.downloader.trace

import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.error.DownloadErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Journal d'étapes — le diagnostic sans adb.
 *
 * Ces tests prouvent les propriétés exigées :
 * - les 10 étapes canoniques existent, dans l'ordre du pipeline ;
 * - chaque événement porte horodatage, durée, résultat, code typé, message court ;
 * - **aucune donnée personnelle** : le lien est réduit au domaine + hash ;
 * - le rapport est rejouable (persistance + export Partager).
 */
class DownloadTraceTest {

    @Before
    fun setUp() {
        DownloadTrace.clear()
    }

    @Test
    fun `les dix etapes canoniques sont dans l ordre du pipeline`() {
        assertEquals(
            listOf(
                "VALIDATION",
                "SITE_RECONNU",
                "MOTEUR_INITIALISE",
                "EXTRACTION",
                "APERCU",
                "FORMAT_CHOISI",
                "FLUX_OUVERT",
                "TELECHARGEMENT",
                "ENREGISTREMENT_MEDIASTORE",
                "VISIBLE_EN_BIBLIOTHEQUE",
            ),
            TraceStep.entries.map { it.code },
        )
    }

    @Test
    fun `chaque etape porte horodatage duree resultat et code typé`() {
        val session = DownloadTrace.startSession("https://www.youtube.com/watch?v=abc123")
        session.ok(TraceStep.VALIDATION)
        session.ok(TraceStep.SITE_RECONNU, "youtube")
        session.fail(
            TraceStep.EXTRACTION,
            DownloadError(DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR, "youtube"),
        )

        assertEquals(3, session.events.size)
        session.events.forEach { e ->
            assertTrue("horodatage manquant", e.atEpochMs > 0)
            assertTrue("durée négative", e.durationMs >= 0)
            assertNotNull("résultat manquant", e.result)
        }
        val failure = session.firstFailure()
        assertNotNull(failure)
        assertEquals(DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR, failure!!.errorCode)
        assertEquals("youtube", failure.technicalMessage)
        assertEquals(DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR, session.failureCode())
    }

    @Test
    fun `le lien est reduit au domaine et a un hash - aucune donnee personnelle`() {
        val secret = "https://www.youtube.com/watch?v=IDENTIFIANT_SECRET"
        val session = DownloadTrace.startSession(secret)
        session.ok(TraceStep.VALIDATION)

        assertEquals("www.youtube.com", session.domain)
        assertNotNull(session.linkHash)
        assertFalse("hash trop long", session.linkHash!!.length > 32)

        val report = DownloadTrace.report()
        assertTrue("domaine absent du rapport", report.contains("www.youtube.com"))
        assertFalse("lien complet present dans le rapport", report.contains(secret))
        assertFalse("identifiant de video present dans le rapport", report.contains("IDENTIFIANT_SECRET"))
    }

    @Test
    fun `les sessions sont bornees a 50`() {
        repeat(60) { DownloadTrace.startSession("https://example.org/$it") }
        assertEquals(DownloadTrace.MAX_SESSIONS, DownloadTrace.sessions().size)
    }

    @Test
    fun `le rapport se relit identiquement apres persistance`() {
        val session = DownloadTrace.startSession("https://soundcloud.com/artiste/track")
        session.ok(TraceStep.VALIDATION)
        session.ok(TraceStep.SITE_RECONNU, "soundcloud")
        session.fail(TraceStep.EXTRACTION, DownloadError(DownloadErrorCode.ECHEC_EXTRACTION, "boom"))

        val report = DownloadTrace.report()
        DownloadTrace.clear()
        assertTrue("journal non vide apres effacement", DownloadTrace.sessions().isEmpty())

        DownloadTrace.importReport(report)
        val reloaded = DownloadTrace.sessions()
        assertEquals(1, reloaded.size)
        val s = reloaded.first()
        assertEquals("soundcloud.com", s.domain)
        assertEquals(3, s.events.size)
        assertEquals(TraceStep.EXTRACTION, s.events.last().step)
        assertEquals(TraceResult.ECHEC, s.events.last().result)
        assertEquals(DownloadErrorCode.ECHEC_EXTRACTION, s.events.last().errorCode)
        assertEquals("boom", s.events.last().technicalMessage)
    }

    @Test
    fun `un rapport corrompu ne fait pas planter`() {
        DownloadTrace.importReport("ligne invalide\nSESSION|S1|dom|hash|123\nEVENT|INCONNU|OK|1|0|-|")
        // La session S1 existe (ligne SESSION valide) mais l'étape inconnue est ignorée.
        assertEquals(1, DownloadTrace.sessions().size)
        assertTrue(DownloadTrace.sessions().first().events.isEmpty())
    }

    @Test
    fun `un lien non exploitable ne produit pas de domaine`() {
        assertNull(LinkReduction.domain("pas-un-lien"))
        assertNull(LinkReduction.domain(""))
    }
}
