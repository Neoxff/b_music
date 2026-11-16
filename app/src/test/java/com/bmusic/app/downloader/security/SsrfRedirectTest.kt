package com.bmusic.app.downloader.security

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Anti-SSRF des **redirections** — audit sécurité v3.
 *
 * Avant le durcissement, OkHttp suivait les redirections
 * (`followRedirects(true)`) **sans aucune re-validation** : un lien public
 * pouvait rediriger vers `http://127.0.0.1/…` ou vers une IP privée du
 * réseau local. La garde est ici prouvée hors-ligne (résolveur injecté).
 */
class SsrfRedirectTest {

    private val publicResolver = SsrfGuard.HostResolver { listOf("93.184.216.34") }

    private val privateResolver = SsrfGuard.HostResolver { host ->
        when {
            host == "localhost" -> listOf("127.0.0.1")
            // IP littérale : le résolveur du système renvoie l'adresse elle-même.
            host.contains(':') -> listOf(host)
            host == "127.0.0.1" || host == "10.0.0.1" ||
                host == "192.168.1.1" || host == "169.254.169.254" -> listOf(host)
            else -> listOf("93.184.216.34")
        }
    }

    @Test
    fun `un Location vers une IP privee est refuse`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        val e = assertThrows(RedirectGuard.RedirectRefused::class.java) {
            RedirectGuard.nextUrl(current, "http://127.0.0.1/admin", 0, privateResolver)
        }
        assertTrue("message attendu : ${e.message}", e.message!!.contains("blocked"))
    }

    @Test
    fun `un Location vers le metadata cloud est refuse`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        assertThrows(RedirectGuard.RedirectRefused::class.java) {
            RedirectGuard.nextUrl(current, "http://169.254.169.254/latest/meta-data/", 0, privateResolver)
        }
    }

    @Test
    fun `un Location vers une IP privee en IPv6 est refuse`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        assertThrows(RedirectGuard.RedirectRefused::class.java) {
            RedirectGuard.nextUrl(current, "http://[::1]/x", 0, privateResolver)
        }
    }

    @Test
    fun `un Location public est accepte et resolu`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        val next = RedirectGuard.nextUrl(current, "https://cdn.example.org/f.mp4", 0, publicResolver)
        assertEquals("https://cdn.example.org/f.mp4", next.toString())
    }

    @Test
    fun `un Location relatif est resolu contre la base`() {
        val current = "https://cdn.example.org/a/b".toHttpUrl()
        val next = RedirectGuard.nextUrl(current, "/c/d.mp4", 0, publicResolver)
        assertEquals("https://cdn.example.org/c/d.mp4", next.toString())
    }

    @Test
    fun `plus de cinq sauts sont refuses`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        assertThrows(RedirectGuard.RedirectRefused::class.java) {
            RedirectGuard.nextUrl(current, "https://cdn.example.org/x", SsrfGuard.MAX_REDIRECTS, publicResolver)
        }
    }

    @Test
    fun `un Location vide est refuse`() {
        val current = "https://www.youtube.com/watch?v=abc".toHttpUrl()
        assertThrows(RedirectGuard.RedirectRefused::class.java) {
            RedirectGuard.nextUrl(current, "   ", 0, publicResolver)
        }
    }

    @Test
    fun `la chaine complete de redirections est revalidee saut par saut`() {
        val locations = listOf(
            "https://cdn1.example.org/a",
            "https://cdn2.example.org/b",
        )
        val chain = SsrfGuard.validateRedirectChain(
            "https://www.youtube.com/watch?v=abc",
            locations,
            publicResolver,
        )
        assertEquals(3, chain.size)
        assertEquals("www.youtube.com", chain[0].host)
        assertEquals("cdn2.example.org", chain[2].host)
    }

    @Test
    fun `une redirection vers une IP privee est refusee dans la chaine`() {
        val locations = listOf("http://10.0.0.1/secret")
        assertThrows(SsrfGuard.GuardError::class.java) {
            SsrfGuard.validateRedirectChain(
                "https://www.youtube.com/watch?v=abc",
                locations,
                privateResolver,
            )
        }
    }

    @Test
    fun `plus de cinq redirections dans la chaine sont refusees`() {
        val locations = (1..6).map { "https://cdn$it.example.org/x" }
        assertThrows(SsrfGuard.GuardError::class.java) {
            SsrfGuard.validateRedirectChain(
                "https://www.youtube.com/watch?v=abc",
                locations,
                publicResolver,
            )
        }
    }
}
