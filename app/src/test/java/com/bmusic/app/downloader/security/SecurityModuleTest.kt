package com.bmusic.app.downloader.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests exhaustifs offline (jamais de DNS réel, résolution injectée).
 * Au moins 40 cas malveillants : schémas, credentials, encodages,
 * IPv6, redirects vers IPs privées, noms Unicode piégeux.
 */
class SecurityModuleTest {

    // ——— UrlValidator : schémas refusés ———

    @Test fun url_fileScheme_refused() = assertInvalid("file:///etc/passwd")
    @Test fun url_contentScheme_refused() = assertInvalid("content://media/external/audio")
    @Test fun url_intentScheme_refused() = assertInvalid("intent://scan#Intent;scheme=https;end")
    @Test fun url_javascript_refused() = assertInvalid("javascript:alert(1)")
    @Test fun url_data_refused() = assertInvalid("data:text/html,<h1>x</h1>")
    @Test fun url_ftp_refused() = assertInvalid("ftp://example.com/f.mp3")
    @Test fun url_smb_refused() = assertInvalid("smb://server/share")
    @Test fun url_noScheme_refused() = assertInvalid("example.com/watch?v=1")
    @Test fun url_uppercaseSchemeAccepted() = assertValid("HTTPS://example.com/x")

    // ——— UrlValidator : credentials, longueur, contrôles ———

    @Test fun url_userinfo_refused() = assertInvalid("https://user:pass@example.com/x")
    @Test fun url_userOnly_refused() = assertInvalid("https://user@example.com/x")
    @Test fun url_fragment_refused() = assertInvalid("https://example.com/x#frag")
    @Test fun url_tooLong_refused() = assertInvalid("https://example.com/" + "a".repeat(2048))
    @Test fun url_blank_refused() = assertInvalid("   ")
    @Test fun url_nullByte_refused() = assertInvalid("https://example.com/a\u0000b")
    @Test fun url_space_refused() = assertInvalid("https://example.com/a b")
    @Test fun url_crlfInjection_refused() = assertInvalid("https://example.com/\r\nInjected: 1")
    @Test fun url_tab_refused() = assertInvalid("https://example.com/\tx")
    @Test fun url_missingHost_refused() = assertInvalid("https://")
    @Test fun url_badPort_refused() = assertInvalid("https://example.com:99999/x")
    @Test fun url_hostUnderscore_refused() = assertInvalid("https://ex_ample.com/x")

    // ——— UrlValidator : normalisation ———

    @Test fun url_normalize_defaultPortStripped() {
        assertEquals(
            "https://example.com/x",
            (UrlValidator.validate("HTTPS://EXAMPLE.COM:443/x") as UrlValidator.Validation.Valid).normalized
        )
    }
    @Test fun url_normalize_nonDefaultPortKept() {
        assertEquals(
            "http://example.com:8080/x",
            (UrlValidator.validate("http://example.com:8080/x") as UrlValidator.Validation.Valid).normalized
        )
    }
    @Test fun url_normalize_ipv6Bracketed() {
        assertEquals(
            "http://[2606:4700:4700::1111]/x",
            (UrlValidator.validate("http://[2606:4700:4700::1111]/x") as UrlValidator.Validation.Valid).normalized
        )
    }
    @Test fun url_valid_youTubeForms() {
        assertValid("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertValid("https://youtu.be/dQw4w9WgXcQ")
    }

    // ——— SsrfGuard : plages privées/loopback ———

    @Test fun ssrf_127_blocked() = assertTrue(SsrfGuard.isBlockedAddress("127.0.0.1"))
    @Test fun ssrf_127full_blocked() = assertTrue(SsrfGuard.isBlockedAddress("127.255.255.254"))
    @Test fun ssrf_10_blocked() = assertTrue(SsrfGuard.isBlockedAddress("10.0.0.5"))
    @Test fun ssrf_172_16_blocked() = assertTrue(SsrfGuard.isBlockedAddress("172.16.0.1"))
    @Test fun ssrf_172_31_blocked() = assertTrue(SsrfGuard.isBlockedAddress("172.31.255.255"))
    @Test fun ssrf_172_32_public_allowed() = assertFalse(SsrfGuard.isBlockedAddress("172.32.0.1"))
    @Test fun ssrf_192168_blocked() = assertTrue(SsrfGuard.isBlockedAddress("192.168.1.1"))
    @Test fun ssrf_169254_blocked() = assertTrue(SsrfGuard.isBlockedAddress("169.254.169.254"))
    @Test fun ssrf_10064_blocked() = assertTrue(SsrfGuard.isBlockedAddress("100.64.0.1"))
    @Test fun ssrf_100127_blocked() = assertTrue(SsrfGuard.isBlockedAddress("100.127.255.255"))
    @Test fun ssrf_0_blocked() = assertTrue(SsrfGuard.isBlockedAddress("0.0.0.0"))
    @Test fun ssrf_public_allowed() = assertFalse(SsrfGuard.isBlockedAddress("8.8.8.8"))
    @Test fun ssrf_ipv6_loopback() = assertTrue(SsrfGuard.isBlockedAddress("::1"))
    @Test fun ssrf_ipv6_ula_fc() = assertTrue(SsrfGuard.isBlockedAddress("fc00::1"))
    @Test fun ssrf_ipv6_ula_fd() = assertTrue(SsrfGuard.isBlockedAddress("fd12:3456::1"))
    @Test fun ssrf_ipv6_linklocal() = assertTrue(SsrfGuard.isBlockedAddress("fe80::1"))
    @Test fun ssrf_ipv6_linklocal_feb() = assertTrue(SsrfGuard.isBlockedAddress("febf::1"))
    @Test fun ssrf_ipv6_public_allowed() = assertFalse(SsrfGuard.isBlockedAddress("2606:4700:4700::1111"))
    @Test fun ssrf_mapped_v4_private() = assertTrue(SsrfGuard.isBlockedAddress("::ffff:10.0.0.1"))
    @Test fun ssrf_mapped_v4_loopback() = assertTrue(SsrfGuard.isBlockedAddress("::ffff:127.0.0.1"))
    @Test fun ssrf_mapped_hex_loopback() = assertTrue(SsrfGuard.isBlockedAddress("::ffff:7f00:1"))
    @Test fun ssrf_mapped_public_allowed() = assertFalse(SsrfGuard.isBlockedAddress("::ffff:8.8.8.8"))

    @Test fun ssrf_resolve_mixed_publicAndPrivate_blocked() {
        val resolver = SsrfGuard.HostResolver { host ->
            if (host == "evil.example") listOf("8.8.8.8", "192.168.0.1") else listOf("8.8.8.8")
        }
        try {
            SsrfGuard.resolveAndValidate("https://evil.example/x", resolver)
            fail("expected BlockedAddress")
        } catch (e: SsrfGuard.GuardError.BlockedAddress) {
            // attendu
        }
    }
    @Test fun ssrf_resolve_public_pinned() {
        val resolver = SsrfGuard.HostResolver { listOf("93.184.216.34") }
        val target = SsrfGuard.resolveAndValidate("https://example.com/x", resolver)
        assertEquals(listOf("93.184.216.34"), target.addresses)
        assertEquals("example.com", target.host)
    }
    @Test fun ssrf_redirect_toPrivate_blocked() {
        val resolver = SsrfGuard.HostResolver { host ->
            if (host == "internal.example") listOf("10.1.0.1") else listOf("93.184.216.34")
        }
        try {
            SsrfGuard.validateRedirectChain(
                "https://example.com/start",
                listOf("https://internal.example/secret"),
                resolver,
            )
            fail("expected RedirectBlocked")
        } catch (e: SsrfGuard.GuardError.RedirectBlocked) {
            // attendu
        }
    }
    @Test fun ssrf_redirect_limitEnforced() {
        val resolver = SsrfGuard.HostResolver { listOf("93.184.216.34") }
        try {
            SsrfGuard.validateRedirectChain(
                "https://example.com/a",
                List(6) { "https://example.com/b$it" },
                resolver,
            )
            fail("expected TooManyRedirects")
        } catch (e: SsrfGuard.GuardError.TooManyRedirects) {
            // attendu
        }
    }
    @Test fun ssrf_redirect_relativeRevalidated() {
        val resolver = SsrfGuard.HostResolver { listOf("93.184.216.34") }
        val chain = SsrfGuard.validateRedirectChain(
            "https://example.com/base", listOf("/next"), resolver
        )
        assertEquals(2, chain.size)
        assertEquals("https://example.com/next", chain[1].normalizedUrl)
    }
    @Test fun ssrf_resolve_badUrl() {
        try {
            SsrfGuard.resolveAndValidate("ftp://example.com/x") { listOf("8.8.8.8") }
            fail("expected BadUrl")
        } catch (e: SsrfGuard.GuardError.BadUrl) {
            // attendu
        }
    }

    // ——— FileNameSanitizer ———

    @Test fun file_traversalStripped() {
        val name = FileNameSanitizer.sanitize("../../etc/passwd", "mp3")
        assertFalse(name.contains("/"))
        assertFalse(name.contains(".."))
        assertEquals("passwd.mp3", name)
    }
    @Test fun file_windowsTraversalStripped() {
        val name = FileNameSanitizer.sanitize("..\\..\\evil.exe", "m4a")
        assertEquals("evil.m4a", name)
    }
    @Test fun file_controlAndNullStripped() {
        val name = FileNameSanitizer.sanitize("a\u0000b\nc.mp3", "mp3")
        assertFalse(name.contains("\u0000"))
        assertTrue(FileNameSanitizer.isSafe(name))
    }
    @Test fun file_reservedWordPrefixed() {
        assertEquals("_CON.mp3", FileNameSanitizer.sanitize("CON", "mp3"))
        assertEquals("_aux.mp3", FileNameSanitizer.sanitize("aux", "mp3"))
    }
    @Test fun file_extensionWhitelist() {
        assertEquals("x.m4a", FileNameSanitizer.sanitize("x", "exe"))
        assertEquals("x.webm", FileNameSanitizer.sanitize("x", ".WEBM"))
    }
    @Test fun file_truncate100() {
        val name = FileNameSanitizer.sanitize("a".repeat(300), "mp3")
        assertTrue(name.length <= FileNameSanitizer.MAX_LENGTH)
        assertTrue(name.endsWith(".mp3"))
    }
    @Test fun file_bidiAndZeroWidthStripped() {
        val name = FileNameSanitizer.sanitize("ok\u202Eg\u0308p\u200B песенка", "ogg")
        assertFalse(name.contains("\u202E"))
        assertFalse(name.contains("\u200B"))
        assertTrue(FileNameSanitizer.isSafe(name))
    }
    @Test fun file_leadingDotsStripped() {
        assertEquals("hidden.mp3", FileNameSanitizer.sanitize(".hidden", "mp3"))
    }

    // ——— DownloadLimits ———

    @Test fun limits_audio200Mb() {
        assertEquals(200L * 1024 * 1024, DownloadLimits.DEFAULT_MAX_AUDIO_BYTES)
    }
    @Test fun limits_exceedAnnounced_abort() {
        val d = DownloadLimits.evaluate(
            bytesSoFar = 100, chunkSize = 50, contentLength = 120, isVideo = false
        )
        assertTrue(d is DownloadLimits.Decision.Abort)
    }
    @Test fun limits_underBudget_continue() {
        val d = DownloadLimits.evaluate(
            bytesSoFar = 100, chunkSize = 50, contentLength = 1000, isVideo = false
        )
        assertTrue(d is DownloadLimits.Decision.Continue)
    }
    @Test fun limits_announcedOverMax_abort() {
        val d = DownloadLimits.evaluate(
            bytesSoFar = 0, chunkSize = 1,
            contentLength = DownloadLimits.DEFAULT_MAX_AUDIO_BYTES + 1, isVideo = false
        )
        assertTrue(d is DownloadLimits.Decision.Abort)
    }
    @Test fun limits_noCookies() {
        assertFalse(DownloadLimits.USE_COOKIES)
    }

    private fun assertInvalid(url: String) {
        val v = UrlValidator.validate(url)
        if (v !is UrlValidator.Validation.Invalid) fail("expected Invalid for: $url got $v")
    }

    private fun assertValid(url: String) {
        val v = UrlValidator.validate(url)
        if (v !is UrlValidator.Validation.Valid) fail("expected Valid for: $url got $v")
    }
}
