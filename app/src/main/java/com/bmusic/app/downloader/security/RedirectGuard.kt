package com.bmusic.app.downloader.security

import okhttp3.HttpUrl

/**
 * Garde des redirections du client HTTP du téléchargeur.
 *
 * Chaque saut est **re-validé** (syntaxe + résolution DNS + plages IP
 * interdites) avant d'être suivi, et le nombre de sauts est plafonné à
 * [SsrfGuard.MAX_REDIRECTS]. Sans cette re-validation, un lien public
 * pouvait rediriger vers `http://127.0.0.1/...` ou vers une IP privée du
 * réseau local : le client suivait la redirection sans rien re-vérifier.
 *
 * La validation est **pure** (aucune I/O réelle) : le résolveur est injecté,
 * ce qui rend le test entièrement hors-ligne.
 */
object RedirectGuard {

    /** Exception levée quand un saut est refusé. */
    class RedirectRefused(message: String) : Exception(message)

    /**
     * URL du saut suivant, après validation.
     *
     * @param current URL courante (base de résolution des Locations relatifs)
     * @param location en-tête `Location` brut
     * @param hops nombre de sauts déjà suivis
     * @param resolver résolution DNS injectée
     * @throws RedirectRefused si le saut est trop nombreux, mal formé, ou
     *         pointe vers une adresse bloquée (loopback, privée, link-local).
     */
    fun nextUrl(
        current: HttpUrl,
        location: String,
        hops: Int,
        resolver: SsrfGuard.HostResolver = SsrfGuard.HostResolver { host ->
            runCatching {
                java.net.InetAddress.getAllByName(host).map { it.hostAddress }
            }.getOrDefault(emptyList())
        },
    ): HttpUrl {
        if (hops >= SsrfGuard.MAX_REDIRECTS) {
            throw RedirectRefused("too many redirects (${hops + 1} > ${SsrfGuard.MAX_REDIRECTS})")
        }
        val trimmed = location.trim()
        if (trimmed.isEmpty()) throw RedirectRefused("empty Location")
        val resolved = current.resolve(trimmed)
            ?: throw RedirectRefused("unresolvable Location")
        // Re-validation stricte du saut : c'est elle qui bloque le SSRF par
        // redirection (le client ne suivait aucune validation auparavant).
        try {
            SsrfGuard.resolveAndValidate(resolved.toString(), resolver)
        } catch (e: SsrfGuard.GuardError) {
            throw RedirectRefused("redirect blocked: ${e.message}")
        }
        return resolved
    }
}
