package com.bmusic.app.downloader.security

/**
 * Validation minimal des requêtes de recherche (2 à 200 caractères).
 * Doit rester cohérent avec `QueryValidator` de `feat/dl-securite`
 * (contrôles/octets nuls refusés, jamais journalisés en clair).
 */
object QueryValidator {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 200

    sealed class Validation {
        object Valid : Validation()
        data class Invalid(val reason: String) : Validation()
    }

    fun validate(raw: String?): Validation {
        if (raw == null) return Validation.Invalid("null")
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Validation.Invalid("empty")
        if ('\u0000' in trimmed) return Validation.Invalid("null byte")
        if (trimmed.any { it.isISOControl() }) return Validation.Invalid("control chars")
        val collapsed = trimmed.replace(Regex("\\s+"), " ")
        if (collapsed.length !in MIN_LENGTH..MAX_LENGTH) {
            return Validation.Invalid("length ${collapsed.length} out of 2..200")
        }
        return Validation.Valid
    }

    fun isValid(raw: String?): Boolean = validate(raw) == Validation.Valid

    fun cleaned(raw: String?): String? {
        if (validate(raw) !is Validation.Valid) return null
        return raw!!.trim().replace(Regex("\\s+"), " ")
    }
}
