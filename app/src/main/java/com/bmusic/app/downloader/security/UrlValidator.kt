package com.bmusic.app.downloader.security

/**
 * Validation des URL d'entrée du téléchargeur.
 *
 * Utilisé par TOUTES les entrées du module [com.bmusic.app.downloader] :
 * collage/link intent, [com.bmusic.app.downloader.engine.MediaExtractorEngine.resolve],
 * redirection suivie par [SsrfGuard]. Aucune I/O réseau ici.
 *
 * Règles :
 * - schéma `http` ou `https` uniquement (refuse `file://`, `content://`,
 *   `intent://`, `javascript:`, `ftp:`, `data:`, `smb:`, `intent:`, etc.) ;
 * - longueur maximale 2 048 caractères (avant normalisation) ;
 * - pas d'identifiants dans l'URL (`user:pass@host`, `user@host` refusés) ;
 * - pas de caractères de contrôle, espaces non encodés, ni octets nuls ;
 * - normalisation : lowercase schéma+hôte, port par défaut retiré,
 *   pas de fragment (refusé plutôt que tronqué pour le partage).
 */
object UrlValidator {

    const val MAX_LENGTH = 2048

    sealed class Validation {
        data class Valid(val normalized: String) : Validation()
        data class Invalid(val reason: Reason) : Validation()
    }

    enum class Reason {
        BLANK,
        TOO_LONG,
        BAD_SCHEME,
        BAD_SYNTAX,
        HAS_USER_INFO,
        HAS_FRAGMENT,
        HAS_CONTROL_CHARS,
        MISSING_HOST,
        BAD_HOST,
        BAD_PORT,
    }

    fun validate(raw: String?): Validation {
        if (raw == null) return Validation.Invalid(Reason.BLANK)
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Validation.Invalid(Reason.BLANK)
        if (trimmed.length > MAX_LENGTH) return Validation.Invalid(Reason.TOO_LONG)
        if (trimmed.any { it.isISOControl() } || '\u0000' in trimmed) {
            return Validation.Invalid(Reason.HAS_CONTROL_CHARS)
        }
        // Rejeter les espaces internes non encodés (ambiguïté de parsing).
        if (trimmed.any { it == ' ' }) return Validation.Invalid(Reason.BAD_SYNTAX)

        val lower = trimmed.lowercase()
        val schemeEnd = lower.indexOf("://")
        if (schemeEnd <= 0) return Validation.Invalid(Reason.BAD_SCHEME)
        val scheme = lower.substring(0, schemeEnd)
        if (scheme != "http" && scheme != "https") return Validation.Invalid(Reason.BAD_SCHEME)

        // Pas d'identifiants : userinfo présent si `@` avant le premier `/`.
        val afterScheme = trimmed.substring(schemeEnd + 3)
        val authorityEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (authorityEnd < 0) afterScheme else afterScheme.substring(0, authorityEnd)
        if ('@' in authority) return Validation.Invalid(Reason.HAS_USER_INFO)
        if (authority.isEmpty()) return Validation.Invalid(Reason.MISSING_HOST)

        if ('#' in trimmed) return Validation.Invalid(Reason.HAS_FRAGMENT)

        // Hôte/port : supporter `[ipv6]:port` et `host:port`.
        val hostPort: String
        val portPart: String?
        if (authority.startsWith("[", ignoreCase = true)) {
            val close = authority.indexOf(']')
            if (close < 0) return Validation.Invalid(Reason.BAD_HOST)
            hostPort = authority.substring(1, close)
            val rest = authority.substring(close + 1)
            if (rest.isNotEmpty()) {
                if (!rest.startsWith(":")) return Validation.Invalid(Reason.BAD_PORT)
                portPart = rest.substring(1)
            } else {
                portPart = null
            }
        } else {
            val colon = authority.lastIndexOf(':')
            if (colon >= 0) {
                hostPort = authority.substring(0, colon)
                portPart = authority.substring(colon + 1)
            } else {
                hostPort = authority
                portPart = null
            }
        }
        if (hostPort.isEmpty()) return Validation.Invalid(Reason.MISSING_HOST)
        val port = portPart?.toIntOrNull()
        if (portPart != null) {
            if (port == null || port !in 1..65535) return Validation.Invalid(Reason.BAD_PORT)
        }

        if (!isValidHostSyntax(hostPort)) return Validation.Invalid(Reason.BAD_HOST)

        // Normalisation : lowercase schéma+hôte, port défaut retiré.
        val defaultPort = if (scheme == "http") 80 else 443
        val normalizedHost =
            if (hostPort.contains(':')) "[$hostPort]" else hostPort.lowercase()
        val normalizedPort = if (portPart != null && port != defaultPort) ":$portPart" else ""
        val rest = if (authorityEnd < 0) "" else afterScheme.substring(authorityEnd)
        val normalized = "$scheme://$normalizedHost$normalizedPort$rest"
        return Validation.Valid(normalized)
    }

    fun isValid(raw: String?): Boolean = validate(raw) is Validation.Valid

    private fun isValidHostSyntax(host: String): Boolean {
        if (' ' in host || '_' in host) return false
        if (host.contains(':')) {
            // Littéral IPv6 : adresses hexadecimales, `:` , `.` (IPv4 embedded).
            return host.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' || it == '.' }
        }
        if (host.length > 253) return false
        val labels = host.split('.')
        if (labels.isEmpty()) return false
        return labels.all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                label.all { it.isLetterOrDigit() || it == '-' } &&
                !label.startsWith("-") && !label.endsWith("-")
        }
    }
}
