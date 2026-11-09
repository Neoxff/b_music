package com.bmusic.app.downloader.security

/**
 * Nettoyage des noms de fichiers dérivés du réseau.
 *
 * Utilisé par TOUTES les sorties fichiers du module
 * [com.bmusic.app.downloader] : titre de la vidéo, auteur,
 * `Content-Disposition: filename`, segment d'URL, métadonnée.
 * Jamais de chemin dérivé du réseau sans filtrage ici.
 *
 * Garanties :
 * - supprime tout traversal (`../`, `..\`, `/`, `\`) et le contrôle indirect ;
 * - retire caractères de contrôle (`U+0000..U+001F`, `U+007F`), octets nuls
 *   et caractères découpants sur FS/Android (`<>:"|?*`) ;
 * - refuse les noms réservés Windows (`CON`, `PRN`, `AUX`, `NUL`,
 *   `COM1..9`, `LPT1..9`) en les préfixant ;
 * - normalise Unicode en NFC-ish (fiche en NFC n'est pas dispo partout en
 *   pur Kotlin : on collapse les formes dangereuses — trim, conversion des
 *   espaces insécables, suppression des caractères invisibles) ;
 * - troncature à 100 caractères (sans casser l'extension) ;
 * - extension issue d'une liste blanche uniquement
 *   (`m4a`, `mp4`, `webm`, `opus`, `ogg`, `mp3`) ;
 * - jamais de chemin absolu : le résultat est un nom simple, sans séparateur.
 */
object FileNameSanitizer {

    const val MAX_LENGTH = 100

    private val ALLOWED_EXTENSIONS = setOf("m4a", "mp4", "webm", "opus", "ogg", "mp3")

    private val RESERVED = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
    )

    private val ILLEGAL = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')

    /**
     * Construit un nom de fichier sûr à partir de [networkProvided] et
     * d'une extension proposée ([suggestedExtension], sans point).
     * Retourne toujours un nom simple (sans `/`/`\`), jamais vide.
     */
    fun sanitize(networkProvided: String, suggestedExtension: String): String {
        val cleanExt = normalizeExtension(suggestedExtension)
        var base = networkProvided.orEmpty()

        // Retirer un chemin : ne garder que le dernier segment.
        base = base.substringAfterLast('/').substringAfterLast('\\')
        // Traversal explicite (`..` seul, `.`, chaînes vides) :
        if (base.trim() == "." || base.trim() == ".." || base.isBlank()) {
            base = "download"
        }
        // Retirer occurrances `../` résiduelles et retours à la ligne.
        base = base.replace("..", "").replace("~", "")

        // Caractères illégaux + contrôles + invisibles.
        val builder = StringBuilder(base.length)
        for (ch in base) {
            if (ch.isISOControl() || ch == '\u0000' || ch == '\u007F') continue
            if (ch in ILLEGAL) {
                builder.append('_')
                continue
            }
            // Invisibles Unicode piégeux : RTL override, zero-width, bidi.
            if (ch == '\u200B' || ch == '\u200C' || ch == '\u200D' ||
                ch == '\uFEFF' || ch == '\u202A' || ch == '\u202B' ||
                ch == '\u202C' || ch == '\u202D' || ch == '\u202E' ||
                ch == '\u2066' || ch == '\u2067' || ch == '\u2068' ||
                ch == '\u2069'
            ) {
                continue
            }
            builder.append(ch)
        }
        base = builder.toString()

        // Espaces multiples / insécables -> espace simple, trim.
        base = base.replace(Regex("\\s+"), " ").trim()
            .replace('\u00A0', ' ')
            .trim()

        // Points en début/fin (cachent l'extension, fichiers Windows cachés).
        base = base.trim('.').trim()
        if (base.isEmpty()) base = "download"

        // Une extension éventuelle dans le nom réseau est neutralisée :
        // on ne garde que l'extension blanche suggérée (ou .bin jamais).
        val dotIdx = base.lastIndexOf('.')
        if (dotIdx > 0) {
            base = base.substring(0, dotIdx).trim().trim('.')
            if (base.isEmpty()) base = "download"
        }

        // Noms réservés Windows.
        if (base.uppercase() in RESERVED) {
            base = "_$base"
        }

        // Troncature à MAX_LENGTH - extension.
        val suffix = ".$cleanExt"
        if (base.length + suffix.length > MAX_LENGTH) {
            val keep = (MAX_LENGTH - suffix.length).coerceAtLeast(1)
            base = base.substring(0, keep).trim().trimEnd('.')
            if (base.isEmpty()) base = "download"
        }
        return base + suffix
    }

    /**
     * Normalise une extension proposée : lowercase, sans point ;
     * hors liste blanche -> `m4a` (audio par défaut, jamais d'exécutable).
     */
    fun normalizeExtension(ext: String?): String {
        val e = ext.orEmpty().trim().lowercase().removePrefix(".")
        return if (e in ALLOWED_EXTENSIONS) e else "m4a"
    }

    /** `true` si [name] est un nom simple sûr (aucun chemin, extension blanche). */
    fun isSafe(name: String): Boolean {
        if (name.isEmpty() || name.length > MAX_LENGTH) return false
        if ('/' in name || '\\' in name) return false
        if (name.startsWith(".") || name.startsWith("-")) return false
        if (name.any { it.isISOControl() } || '\u0000' in name) return false
        val ext = name.substringAfterLast('.', "")
        return ext.lowercase() in ALLOWED_EXTENSIONS
    }
}
