package com.bmusic.app.downloader.security

/**
 * Anti-SSRF du téléchargeur.
 *
 * Défend les entrées réseau du module [com.bmusic.app.downloader] contre
 * l'usage du client HTTP dédié pour sonder le réseau local / privé.
 *
 * Règles :
 * - refuse loopback, IPv4 privées et link-local : `10/8`, `172.16/12`,
 *   `192.168/16`, `127/8`, `169.254/16`, `100.64/10` (CGNAT), `0.0.0.0` ;
 * - refuse `::1`, `fc00::/7` (ULA), `fe80::/10` (link-local IPv6) ;
 * - refuse les IPv4 mappées en IPv6 (`::ffff:10.0.0.1`, `::ffff:7f00:1`) ;
 * - vérification AVANT la connexion ET après chaque redirection
 *   (5 redirections maximum, re-validation à chaque saut) ;
 * - DNS rebinding : l'hôte est résolu une fois, la liste d'IP validée est
 *   **épinglée** — la connexion doit se faire contre ces adresses
 *   ([pinnedAddressesProvider]), jamais par une résolution ultérieure
 *   qui pourrait retourner une IP privée.
 *
 * Aucune connexion réelle ici : la résolution est injectée
 * ([HostResolver]) pour que les tests soient entièrement offline
 * (jamais de DNS réel en test).
 *
 * Exemple d'usage côté client (OkHttp) :
 * ```
 * val resolved = SsrfGuard.resolveAndValidate(url)          // AVANT connexion
 * val freeSocket = ssrfPinnedSocketFactory(resolved.addresses) // connexion épinglée
 * chain.request().copy(.., url = resolved.normalized, socketFactory = freeSocket)
 * ```
 * Pour chaque redirection, rappeler `resolveAndValidate` sur `Location`.
 */
object SsrfGuard {

    const val MAX_REDIRECTS = 5

    /** Résolution DNS injectable (défaut : [java.net.InetAddress]). */
    fun interface HostResolver {
        /** Retourne les adresses textuelles pour [host] (v4/v6, sans crochets). */
        fun resolve(host: String): List<String>
    }

    sealed class GuardError(msg: String) : Exception(msg) {
        class BlockedAddress(host: String, ip: String) :
            GuardError("SSRF-Guard: blocked $ip for $host")
        class NoAddress(host: String) :
            GuardError("SSRF-Guard: no address for $host")
        class BadUrl(val url: String) :
            GuardError("SSRF-Guard: invalid URL: $url")
        class TooManyRedirects(val count: Int) :
            GuardError("SSRF-Guard: $count redirects, limit $MAX_REDIRECTS")
        class RedirectBlocked(val url: String) :
            GuardError("SSRF-Guard: redirect blocked to $url")
    }

    /** Résultat d'une validation : URL normalisée + IP validées et épinglées. */
    data class ResolvedTarget(
        val normalizedUrl: String,
        val host: String,
        val port: Int,
        val addresses: List<String>,
        val isHttps: Boolean,
    )

    private val defaultResolver = HostResolver { host ->
        try {
            java.net.InetAddress.getAllByName(host).map { it.hostAddress }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Valide [rawUrl] (syntaxe + plages interdites) puis résout son hôte
     * et épingle les adresses autorisées.
     * @throws GuardError si l'URL est invalide ou une IP est bloquée.
     */
    fun resolveAndValidate(
        rawUrl: String,
        resolver: HostResolver = defaultResolver,
    ): ResolvedTarget {
        val validation = UrlValidator.validate(rawUrl)
        val normalized = when (validation) {
            is UrlValidator.Validation.Valid -> validation.normalized
            is UrlValidator.Validation.Invalid ->
                throw GuardError.BadUrl(rawUrl)
        }

        val parsed = parse(normalized) ?: throw GuardError.BadUrl(normalized)
        val (host, port, isHttps) = parsed

        val addresses = resolver.resolve(host)
        if (addresses.isEmpty()) throw GuardError.NoAddress(host)
        for (ip in addresses) {
            if (isBlockedAddress(ip)) throw GuardError.BlockedAddress(host, ip)
        }
        return ResolvedTarget(normalized, host, port, addresses, isHttps)
    }

    /**
     * Pour une chaîne de redirections : valide chaque `Location`
     * (relative résolue contre [base], absolue re-validée) et impose
     * [MAX_REDIRECTS]. Retourne les cibles épinglées dans l'ordre.
     */
    fun validateRedirectChain(
        initialUrl: String,
        locations: List<String>,
        resolver: HostResolver = defaultResolver,
    ): List<ResolvedTarget> {
        if (locations.size > MAX_REDIRECTS) {
            throw GuardError.TooManyRedirects(locations.size)
        }
        val out = mutableListOf<ResolvedTarget>()
        var currentBase = initialUrl
        out += resolveAndValidate(currentBase, resolver)
        for (loc in locations) {
            val absolute = resolveReference(currentBase, loc)
            val target = try {
                resolveAndValidate(absolute, resolver)
            } catch (e: GuardError) {
                throw GuardError.RedirectBlocked(absolute)
            }
            out += target
            currentBase = target.normalizedUrl
        }
        return out
    }

    /** `true` si [ip] est loopback / privée / link-local / CGNAT / ULA. */
    fun isBlockedAddress(ip: String): Boolean {
        val clean = ip.trim().removeSurrounding("[", "]").lowercase()
        if (clean.isEmpty()) return true
        // IPv4-mappée en IPv6 : ::ffff:a.b.c.d ou ::ffff:hhhh:hhhh
        if (clean.startsWith("::ffff:")) {
            val embedded = clean.removePrefix("::ffff:")
            if (isBlockedAddress(embedded)) return true
            val mapped = parseMappedV4(embedded)
            if (mapped != null && isBlockedAddress(mapped)) return true
            // `::ffff:` seul sans repli = suspicieux, bloquer.
            if (mapped == null) return true
            return false
        }
        if (':' in clean) {
            // IPv6
            if (clean == "::1" || clean == "::") return true
            if (clean.startsWith("fe8") || clean.startsWith("fe9") ||
                clean.startsWith("fea") || clean.startsWith("feb")
            ) {
                // fe80::/10 -> fe8..., fe9..., fea..., feb...
                // (vérifié par premier octet 0xfe + nibble 8..b)
            }
            if (isV6LinkLocal(clean)) return true
            if (isV6Ula(clean)) return true
            // Toute IPv6 dont la forme est une IPv4 embedded couverte ci-dessus,
            // sinon autoriser uniquement si pas dans les plages ci-dessus.
            return false
        }
        // IPv4
        val octets = clean.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) {
            // Format inattendu : fail-closed.
            return true
        }
        val a = octets[0]
        val b = octets[1]
        if (a == 0 && b == 0 && octets[2] == 0 && octets[3] == 0) return true // 0.0.0.0
        if (a == 10) return true // 10/8
        if (a == 127) return true // 127/8
        if (a == 192 && b == 168) return true // 192.168/16
        if (a == 172 && b in 16..31) return true // 172.16/12
        if (a == 169 && b == 254) return true // 169.254/16
        if (a == 100 && b in 64..127) return true // 100.64/10 CGNAT
        if (a == 0) return true // 0.0.0.0/8 "this network"
        return false
    }

    // ——— Parsing interne (pas de java.net.URL pour éviter ses quirks) ———

    private data class HostPort(val host: String, val port: Int, val isHttps: Boolean)

    private fun parse(url: String): HostPort? {
        val lower = url.lowercase()
        val isHttps = lower.startsWith("https://")
        val isHttp = lower.startsWith("http://")
        if (!isHttp && !isHttps) return null
        val after = url.substring(url.indexOf("://") + 3)
        val end = after.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (end < 0) after else after.substring(0, end)
        if (authority.isEmpty()) return null
        if ('@' in authority) return null
        if (authority.startsWith("[")) {
            val close = authority.indexOf(']')
            if (close < 0) return null
            val host = authority.substring(1, close)
            val rest = authority.substring(close + 1)
            val port = if (rest.startsWith(":")) rest.substring(1).toIntOrNull() ?: -1
            else if (rest.isEmpty()) if (isHttps) 443 else 80 else -1
            if (port !in 1..65535 || host.isEmpty()) return null
            return HostPort(host.lowercase(), port, isHttps)
        }
        val colon = authority.lastIndexOf(':')
        if (colon >= 0) {
            val port = authority.substring(colon + 1).toIntOrNull() ?: return null
            if (port !in 1..65535) return null
            return HostPort(authority.substring(0, colon).lowercase(), port, isHttps)
        }
        return HostPort(authority.lowercase(), if (isHttps) 443 else 80, isHttps)
    }

    private fun resolveReference(base: String, ref: String): String {
        val t = ref.trim()
        if (t.isEmpty()) return base
        if (t.lowercase().startsWith("http://") ||
            t.lowercase().startsWith("https://")
        ) {
            return t
        }
        // Référence relative : réutiliser schéma+autorité de base.
        val schemeSep = base.indexOf("://")
        if (schemeSep < 0) return t
        val afterFull = base.substring(schemeSep + 3)
        val authEnd = afterFull.indexOfAny(charArrayOf('/', '?', '#'))
        val origin = base.substring(0, schemeSep + 3 + if (authEnd < 0) afterFull.length else authEnd)
        return when {
            t.startsWith("//") -> base.substring(0, schemeSep) + ":" + t
            t.startsWith("/") -> origin + t
            else -> origin + "/" + t
        }
    }

    private fun isV6Ula(s: String): Boolean {
        // fc00::/7 -> premier octet fc ou fd.
        if (!s.contains(':')) return false
        val first = s.substringBefore(':')
        if (first.length < 2) return false
        val byte0 = first.substring(0, 2).toIntOrNull(16) ?: return false
        return (byte0 and 0xFE) == 0xFC
    }

    private fun isV6LinkLocal(s: String): Boolean {
        // fe80::/10 -> premier octet fe, deuxième nibble 8..b (0x80..0xBF).
        if (!s.contains(':')) return false
        val first = s.substringBefore(':')
        if (first.length < 3) return false
        val hi = first.substring(0, 2).toIntOrNull(16) ?: return false
        if (hi != 0xFE) return false
        val secondNibble = first[2].lowercaseChar()
        return secondNibble in "89ab"
    }

    private fun parseMappedV4(hexPart: String): String? {
        // `::ffff:7f00:1` -> 127.0.0.1 ; `::ffff:10.0.0.1` géré par isBlockedAddress récursif.
        if ('.' in hexPart) return hexPart
        val groups = hexPart.split(':')
        if (groups.size != 2) return null
        val hi = groups[0].toIntOrNull(16) ?: return null
        val lo = groups[1].toIntOrNull(16) ?: return null
        return "${(hi shr 8) and 0xFF}.${hi and 0xFF}.${(lo shr 8) and 0xFF}.${lo and 0xFF}"
    }
}
