package com.bmusic.app.downloader.security

/**
 * Compatibilité : point d'entrée historique du module.
 * La politique complète vit désormais dans [UrlValidator] + [SsrfGuard].
 */
object UrlPolicy {
    fun isSupportedScheme(url: String): Boolean {
        val validation = UrlValidator.validate(url)
        return validation is UrlValidator.Validation.Valid
    }

    fun isHttpsPreferred(url: String): Boolean =
        url.trim().lowercase().startsWith("https://")
}
