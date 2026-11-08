package com.bmusic.app.downloader.security

/**
 * Step 1 minimal policy: http/https only. Full SSRF defence (private-IP
 * blocking per redirect hop, hostname resolution checks) lands in step 6.
 */
object UrlPolicy {
    fun isSupportedScheme(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    fun isHttpsPreferred(url: String): Boolean =
        url.trim().lowercase().startsWith("https://")
}
