package com.bmusic.app.downloader.security

/**
 * Miniatures distantes — données non fiables (étape 6, porté minimal depuis
 * feat/dl-securite). HTTPS uniquement, host whitelist, anti-SSRF, 2 Mo.
 */
object ImageUrlPolicy {

    const val MAX_IMAGE_BYTES = 2L * 1024L * 1024L
    const val MAX_CONCURRENT_IMAGES = 4

    val ALLOWED_HOSTS: Set<String> = setOf(
        "i.ytimg.com",
        "i1.ytimg.com",
        "i2.ytimg.com",
        "i3.ytimg.com",
        "i4.ytimg.com",
        "img.youtube.com",
        "i.vimeocdn.com",
    )

    fun isAllowedHost(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.isEmpty()) return false
        if (h in ALLOWED_HOSTS) return true
        val allowed = ALLOWED_HOSTS
        return allowed.any { a -> h.endsWith(".$a") || h.endsWith(".$a") }
    }

    fun validate(rawUrl: String?, resolver: SsrfGuard.HostResolver? = null): Boolean {
        if (rawUrl == null) return false
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.length > UrlValidator.MAX_LENGTH) return false
        val lower = trimmed.lowercase()
        if (!lower.startsWith("https://")) return false
        val after = trimmed.substring("https://".length)
        val authEnd = after.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (authEnd < 0) after else after.substring(0, authEnd)
        if ('@' in authority) return false
        val hostEnd = authority.indexOf(':').let { if (it < 0) authority.length else it }
        val host = authority.substring(0, hostEnd).lowercase()
        if (!isAllowedHost(host)) return false
        if (resolver != null) {
            return try {
                SsrfGuard.resolveAndValidate(trimmed, resolver)
                true
            } catch (_: Exception) {
                false
            }
        }
        val v = UrlValidator.validate(trimmed)
        return v is UrlValidator.Validation.Valid
    }
}
