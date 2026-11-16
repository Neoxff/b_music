package com.bmusic.app.downloader.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Registre des sites — reconnaissance des formes d'URL réelles.
 *
 * Pour chaque site : au moins 5 formes réalistes (mobile, lien court, avec
 * paramètres de suivi, avec caractères encodés, sans `www`). Attendu : le site
 * est reconnu **avec le bon statut et le bon moteur**.
 *
 * Ce test est la preuve que le message affiché à l'utilisateur est honnête :
 * un site sans moteur doit être `NON_PRIS_EN_CHARGE`, jamais « pris en charge ».
 */
class SiteRegistryTest {

    /** 5 formes réalistes par site : id -> (formes, statut attendu, moteur attendu). */
    private val urlsPerSite: Map<String, Pair<List<String>, SiteStatus>> = mapOf(
        "youtube" to (listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&list=PLabc" +
                "&utm_source=newsletter&utm_medium=email&fbclid=xyz&igshid=abc",
            "https://youtube.com/shorts/abcDEF123",
        ) to SiteStatus.PRIS_EN_CHARGE),
        "soundcloud" to (listOf(
            "https://soundcloud.com/artiste/titre",
            "https://m.soundcloud.com/artiste/titre",
            "https://snd.sc/abc123",
            "https://soundcloud.com/artiste/titre?utm_source=clipboard&utm_medium=referral&fbclid=x",
            "https://soundcloud.com/artiste/titre%20avec%20espace",
        ) to SiteStatus.PRIS_EN_CHARGE),
        "bandcamp" to (listOf(
            "https://artiste.bandcamp.com/album/album",
            "https://bandcamp.com/track/xyz",
            "https://bandcamp.com/track/xyz?fbclid=abc&igshid=1",
            "https://bandcamp.com/song/xyz",
            "https://xxx.bandcamp.com/album/a%2Fb",
        ) to SiteStatus.PRIS_EN_CHARGE),
        "peertube" to (listOf(
            "https://peertube.tv/videos/watch/abc",
            "https://sepiaseconds.org/videos/watch/abc",
            "https://peertube.tv/videos/watch/abc?utm_source=x&fbclid=y",
            "https://m.peertube.tv/videos/watch/abc",
            "https://peertube.fr/videos/watch/abc",
        ) to SiteStatus.PRIS_EN_CHARGE),
        "vimeo" to (listOf(
            "https://vimeo.com/123456",
            "https://player.vimeo.com/video/123456",
            "https://vimeo.com/123456?utm_source=x",
            "https://vimeo.com/channels/staffpicks/123456",
            "https://vimeo.com/123456#t=30s",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "dailymotion" to (listOf(
            "https://www.dailymotion.com/video/x8abcde",
            "https://dai.ly/x8abcde",
            "https://www.dailymotion.com/video/x8abcde?fbclid=z",
            "https://geo.dailymotion.com/video/x8abcde",
            "https://dailymotion.com/video/x8abcde%2Fextra",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "tiktok" to (listOf(
            "https://www.tiktok.com/@user/video/1234567890",
            "https://vm.tiktok.com/ZMabcdef/",
            "https://www.tiktok.com/@user/video/1234567890?utm_source=copy&fbclid=x&igshid=y",
            "https://m.tiktok.com/@user/video/1234567890",
            "https://tiktok.com/t/ZTxyz/",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "instagram" to (listOf(
            "https://www.instagram.com/reel/ABCdef123/",
            "https://instagram.com/p/ABCdef123/",
            "https://www.instagram.com/reel/ABCdef123/?igsh=abc&fbclid=z",
            "https://instagr.am/p/ABCdef123/",
            "https://www.instagram.com/tv/channels/123/?hl=fr",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "facebook" to (listOf(
            "https://www.facebook.com/watch/?v=123456789",
            "https://fb.watch/abcdef/",
            "https://www.facebook.com/reel/123456789/?utm_source=share&fbclid=x",
            "https://m.facebook.com/watch/?v=123456789",
            "https://fb.com/page/videos/123456789/",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "x" to (listOf(
            "https://x.com/user/status/1234567890",
            "https://twitter.com/user/status/1234567890",
            "https://t.co/abcdef",
            "https://mobile.twitter.com/user/status/1234567890?s=20&fbclid=x",
            "https://x.com/i/web/status/1234567890",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "snapchat" to (listOf(
            "https://www.snapchat.com/spotlight/abc",
            "https://www.snapchat.com/add/abc",
            "https://www.snapchat.com/spotlight/abc?utm_source=x&fbclid=y",
            "https://m.snapchat.com/spotlight/abc",
            "https://snapchat.com/spotlight/abc%2Fdef",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "reddit" to (listOf(
            "https://www.reddit.com/r/videos/comments/abc/titre/",
            "https://v.redd.it/abc",
            "https://redd.it/abc",
            "https://www.reddit.com/r/videos/comments/abc/titre/?utm_source=share&fbclid=z",
            "https://old.reddit.com/r/videos/comments/abc/titre/",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
        "twitch" to (listOf(
            "https://www.twitch.tv/videos/123456789",
            "https://clips.twitch.tv/ClipAbc",
            "https://www.twitch.tv/videos/123456789?utm_source=x&fbclid=y",
            "https://m.twitch.tv/videos/123456789",
            "https://twitch.tv/channel/clip/ClipAbc",
        ) to SiteStatus.NON_PRIS_EN_CHARGE),
    )

    @Test
    fun `chaque forme d URL est reconnue avec le bon site et le bon statut`() {
        var checked = 0
        urlsPerSite.forEach { (siteId, pair) ->
            val (urls, expectedStatus) = pair
            urls.forEach { url ->
                val site = SiteRegistry.lookup(url)
                assertNotNull("site non reconnu pour $url", site)
                assertEquals("mauvais site pour $url", siteId, site!!.id)
                assertEquals("mauvais statut pour $url", expectedStatus, site.status)
                checked++
            }
        }
        assertTrue("trop peu d'URL testees : $checked", checked >= 60)
    }

    @Test
    fun `seuls les sites couverts par le moteur portent un moteur`() {
        SiteRegistry.all().forEach { site ->
            if (site.status == SiteStatus.PRIS_EN_CHARGE) {
                assertNotNull("moteur manquant pour ${site.id}", site.engineId)
            } else {
                assertNull("un site sans moteur ne doit pas avoir de moteur", site.engineId)
            }
        }
    }

    @Test
    fun `le registre couvre les douze plateformes annoncees`() {
        val ids = SiteRegistry.all().map { it.id }
        listOf(
            "youtube", "soundcloud", "bandcamp", "peertube", "vimeo", "dailymotion",
            "tiktok", "instagram", "facebook", "x", "snapchat", "reddit", "twitch",
        ).forEach { expected ->
            assertTrue("site manquant : $expected", ids.contains(expected))
        }
    }

    @Test
    fun `un domaine inconnu ou une URL invalide ne correspond a aucun site`() {
        assertNull(SiteRegistry.lookup("https://example.org/video/1"))
        assertNull(SiteRegistry.lookup("pas-une-url"))
        assertNull(SiteRegistry.lookup(""))
        assertNull(SiteRegistry.lookup("https://localhost/x"))
        assertNull(SiteRegistry.lookup("https://example"))
    }

    @Test
    fun `le domaine effectif ignore le prefixe www et les identifiants`() {
        assertEquals("youtube.com", SiteRegistry.domainOf("https://www.youtube.com/watch?v=1"))
        assertEquals("youtube.com", SiteRegistry.domainOf("https://youtube.com/watch?v=1"))
        assertEquals("youtube.com", SiteRegistry.domainOf("https://user@www.youtube.com/x"))
        assertNull(SiteRegistry.domainOf("pas-une-url"))
    }
}
