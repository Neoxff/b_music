package com.bmusic.app.downloader.security

import com.bmusic.app.downloader.registry.SiteRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validateur **pas trop strict** — 60 URL légitimes réelles doivent passer.
 *
 * Chaque site connu est représenté par au moins 5 formes réelles : mobile,
 * lien court, paramètres de suivi, caractères encodés, sans `www`, fragment
 * de partage (`#t=30`), port explicite, casse mixte.
 *
 * Rappel : le bug observé côté utilisateur pouvait venir d'un validateur trop
 * strict. Ce test est la preuve qu'il ne l'est pas (ou qu'il l'est devenu).
 */
class UrlValidatorAcceptanceTest {

    private fun valid(url: String): UrlValidator.Validation.Valid {
        val result = UrlValidator.validate(url)
        assertTrue(
            "URL légitime refusée : $url -> ${(result as? UrlValidator.Validation.Invalid)?.reason}",
            result is UrlValidator.Validation.Valid,
        )
        return result as UrlValidator.Validation.Valid
    }

    @Test
    fun `soixante URL legitimes de tous les sites passent`() {
        val urls = legitimateUrls()
        assertTrue("moins de 60 URL testées : ${urls.size}", urls.size >= 60)

        val refus = urls.filter { UrlValidator.validate(it) !is UrlValidator.Validation.Valid }
        assertTrue(
            "URL légitimes refusées (${refus.size}) :\n" + refus.joinToString("\n"),
            refus.isEmpty(),
        )
    }

    @Test
    fun `chaque site du registre a au moins une URL legitime acceptee`() {
        SiteRegistry.all().forEach { site ->
            val one = legitimateUrls().firstOrNull { it.contains(site.domains.first(), ignoreCase = true) }
            assertTrue("aucune URL de test pour ${site.id}", one != null)
            valid(one!!)
        }
    }

    @Test
    fun `le fragment de partage est retire et non refuse`() {
        // Un lien partagee avec #t=30 est legitime : le fragment ne part
        // jamais sur le reseau, il doit etre retire, pas refuser.
        val result = valid("https://www.youtube.com/watch?v=aqz-KE-bpKQ#t=30")
        assertEquals(
            "le fragment doit etre retire de l URL normalisee",
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            result.normalized,
        )
    }

    @Test
    fun `les parametres de suivi sont retires`() {
        val result = valid(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ" +
                "&utm_source=newsletter&utm_medium=email&utm_campaign=x" +
                "&fbclid=abc&gclid=def&igshid=ghi&si=xyz&feature=share"
        )
        assertEquals(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            result.normalized,
        )
    }

    @Test
    fun `la normalisation conserve les parametres utiles`() {
        val result = valid("https://www.youtube.com/watch?v=aqz-KE-bpKQ&list=PL123&t=42")
        assertTrue("le parametre v doit etre conserve", result.normalized.contains("v=aqz-KE-bpKQ"))
        assertTrue("le parametre list doit etre conserve", result.normalized.contains("list=PL123"))
        assertTrue("le parametre t doit etre conserve", result.normalized.contains("t=42"))
    }

    @Test
    fun `le schema et l hote sont normalises en minuscules et le port par defaut retire`() {
        assertEquals(
            "https://www.youtube.com/watch?v=x",
            valid("HTTPS://WWW.YouTube.COM/watch?v=x").normalized,
        )
        assertEquals(
            "https://example.com/x",
            valid("https://example.com:443/x").normalized,
        )
        assertEquals(
            "http://example.com/x",
            valid("http://example.com:80/x").normalized,
        )
        assertEquals(
            "https://example.com:8443/x",
            valid("https://example.com:8443/x").normalized,
        )
    }

    /** 60+ URL legitimes reelles : 5+ formes par site connu. */
    private fun legitimateUrls(): List<String> = listOf(
        // YouTube (5)
        "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
        "https://m.youtube.com/watch?v=aqz-KE-bpKQ",
        "https://youtu.be/aqz-KE-bpKQ",
        "https://www.youtube.com/watch?v=aqz-KE-bpKQ&list=PLabc&index=2",
        "https://youtube.com/shorts/aqz-KE-bpKQ",
        "https://music.youtube.com/watch?v=aqz-KE-bpKQ",
        "https://www.youtube-nocookie.com/embed/aqz-KE-bpKQ",
        // SoundCloud (5)
        "https://soundcloud.com/artiste/titre",
        "https://m.soundcloud.com/artiste/titre",
        "https://snd.sc/abcdef",
        "https://soundcloud.com/artiste/titre/s-AbCdEf",
        "https://soundcloud.com/user-1234/track/titre%20avec%20accents",
        // Bandcamp (5)
        "https://artiste.bandcamp.com/album/album-name",
        "https://bandcamp.com/track/xyz",
        "https://bandcamp.com/track/xyz?fbclid=abc",
        "https://xxx.bandcamp.com/song/song-name",
        "https://bandcamp.com/track/xyz#t=30",
        // PeerTube (5)
        "https://peertube.tv/videos/watch/abcdef",
        "https://sepiaseconds.org/videos/watch/abcdef",
        "https://peertube.fr/videos/watch/abcdef",
        "https://m.peertube.tv/videos/watch/abcdef",
        "https://peertube.tv/videos/watch/abcdef?start=5s",
        // Vimeo (5)
        "https://vimeo.com/123456789",
        "https://player.vimeo.com/video/123456789",
        "https://vimeo.com/channels/staffpicks/123456789",
        "https://vimeo.com/123456789#t=1m2s",
        "https://vimeo.com/user123/clip/abc",
        // Dailymotion (5)
        "https://www.dailymotion.com/video/x8abcde",
        "https://dai.ly/x8abcde",
        "https://geo.dailymotion.com/video/x8abcde",
        "https://www.dailymotion.com/video/x8abcde?playlist=x",
        "https://dailymotion.com/video/x8abcde%2Fextra",
        // TikTok (5)
        "https://www.tiktok.com/@user/video/1234567890123456789",
        "https://vm.tiktok.com/ZMabcdef/",
        "https://m.tiktok.com/@user/video/1234567890123456789",
        "https://tiktok.com/t/ZTabcdef/",
        "https://www.tiktok.com/@user.name/video/1234567890",
        // Instagram (5)
        "https://www.instagram.com/reel/ABCdef123/",
        "https://instagram.com/p/ABCdef123/",
        "https://instagr.am/p/ABCdef123/",
        "https://www.instagram.com/tv/channels/123/?hl=fr",
        "https://www.instagram.com/reel/ABCdef123/?igsh=abc",
        // Facebook (5)
        "https://www.facebook.com/watch/?v=123456789",
        "https://fb.watch/abcdef/",
        "https://m.facebook.com/watch/?v=123456789",
        "https://fb.com/page/videos/123456789/",
        "https://www.facebook.com/reel/123456789/?sfnsn=iw",
        // X (5)
        "https://x.com/user/status/1234567890123456789",
        "https://twitter.com/user/status/1234567890123456789",
        "https://t.co/abcdefgh",
        "https://mobile.twitter.com/user/status/1234567890123456789",
        "https://x.com/i/web/status/1234567890123456789",
        // Snapchat (5)
        "https://www.snapchat.com/spotlight/abc",
        "https://www.snapchat.com/add/abc",
        "https://m.snapchat.com/spotlight/abc",
        "https://snapchat.com/spotlight/abc%2Fdef",
        "https://www.snapchat.com/spotlight/abc?utm_source=x",
        // Reddit (5)
        "https://www.reddit.com/r/videos/comments/abc/titre/",
        "https://v.redd.it/abcdef",
        "https://redd.it/abcdef",
        "https://old.reddit.com/r/videos/comments/abc/titre/",
        "https://www.reddit.com/r/videos/comments/abc/titre/?utm_source=share",
        // Twitch (5)
        "https://www.twitch.tv/videos/123456789",
        "https://clips.twitch.tv/ClipAbcDef",
        "https://m.twitch.tv/videos/123456789",
        "https://twitch.tv/channel/clip/ClipAbcDef",
        "https://www.twitch.tv/videos/123456789?filter=highlights",
    )
}
