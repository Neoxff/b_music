package com.bmusic.app.downloader.error

/**
 * Erreurs typées du téléchargeur — une par situation, chacune avec un
 * message utilisateur en français et une action.
 *
 * Règle : **aucune erreur ne doit être affichée sous un autre nom** et
 * **aucun `catch` vide**. Toute exception du moteur ou du transfert est
 * convertie en [DownloadError] avant d'atteindre l'interface.
 */
enum class DownloadErrorCode {
    /** Ce n'est pas un lien valide (schéma, host, format). */
    LIEN_INVALIDE,

    /** Domaine connu mais sans moteur : « Ce site n'est pas encore pris en charge ». */
    SITE_NON_PRIS_EN_CHARGE,

    /** Contenu privé / supprimé / géobloqué. */
    CONTENU_PRIVE,

    /** Connexion requise : exclu par construction (aucun compte, aucun cookie). */
    CONNEXION_REQUISE,

    /** Contenu protégé (DRM). */
    CONTENU_PROTEGE,

    /** La source a changé : le moteur doit être mis à jour. */
    MOTEUR_A_METTRE_A_JOUR,

    /** Aucun réseau. */
    HORS_LIGNE,

    /** Échec d'extraction non qualifiable. */
    ECHEC_EXTRACTION,

    /** Permission de notifications refusée (téléchargement quand même possible). */
    PERMISSION_NOTIFICATIONS,

    /** Écriture impossible (stockage, MediaStore). */
    STOCKAGE_INDISPONIBLE,

    /** Téléchargement interrompu. */
    TELECHARGEMENT_ECHEC,
}

/** Action proposée à l'utilisateur pour chaque erreur. */
enum class ErrorAction {
    /** « Réessayer ». */
    REESSAYER,

    /** « Voir les sites pris en charge ». */
    VOIR_SITES_SUPPORTS,

    /** « Ouvrir le journal ». */
    OUVRIR_JOURNAL,

    /** « Activer les notifications ». */
    ACTIVER_NOTIFICATIONS,

    /** Aucune action possible. */
    AUCUNE,
}

/**
 * Erreur typée : [code] pour la machine, [userMessage] pour l'utilisateur,
 * [action] pour l'UI, [technicalMessage] pour le journal d'étapes.
 *
 * @param technicalMessage message court et technique (jamais de donnée
 *        personnelle : le lien est réduit au domaine + hash).
 */
data class DownloadError(
    val code: DownloadErrorCode,
    val technicalMessage: String? = null,
) {
    val userMessage: String
        get() = when (code) {
            DownloadErrorCode.LIEN_INVALIDE ->
                "Ce n'est pas un lien valide"
            DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE ->
                "Ce site n'est pas encore pris en charge"
            DownloadErrorCode.CONTENU_PRIVE ->
                "Ce contenu est privé, supprimé ou inaccessible"
            DownloadErrorCode.CONNEXION_REQUISE ->
                "Ce contenu demande une connexion, qui n'est pas supportée"
            DownloadErrorCode.CONTENU_PROTEGE ->
                "Ce contenu est protégé et ne peut pas être téléchargé"
            DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR ->
                "Ce site a changé : le moteur doit être mis à jour"
            DownloadErrorCode.HORS_LIGNE ->
                "Aucun réseau disponible"
            DownloadErrorCode.ECHEC_EXTRACTION ->
                "Impossible de lire ce lien"
            DownloadErrorCode.PERMISSION_NOTIFICATIONS ->
                "Notifications bloquées : le téléchargement continue, sans suivi"
            DownloadErrorCode.STOCKAGE_INDISPONIBLE ->
                "Impossible d'écrire le fichier sur l'appareil"
            DownloadErrorCode.TELECHARGEMENT_ECHEC ->
                "Le téléchargement a échoué"
        }

    val action: ErrorAction
        get() = when (code) {
            DownloadErrorCode.LIEN_INVALIDE -> ErrorAction.REESSAYER
            DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE -> ErrorAction.VOIR_SITES_SUPPORTS
            DownloadErrorCode.CONTENU_PRIVE -> ErrorAction.REESSAYER
            DownloadErrorCode.CONNEXION_REQUISE -> ErrorAction.AUCUNE
            DownloadErrorCode.CONTENU_PROTEGE -> ErrorAction.AUCUNE
            DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR -> ErrorAction.VOIR_SITES_SUPPORTS
            DownloadErrorCode.HORS_LIGNE -> ErrorAction.REESSAYER
            DownloadErrorCode.ECHEC_EXTRACTION -> ErrorAction.OUVRIR_JOURNAL
            DownloadErrorCode.PERMISSION_NOTIFICATIONS -> ErrorAction.ACTIVER_NOTIFICATIONS
            DownloadErrorCode.STOCKAGE_INDISPONIBLE -> ErrorAction.OUVRIR_JOURNAL
            DownloadErrorCode.TELECHARGEMENT_ECHEC -> ErrorAction.OUVRIR_JOURNAL
        }

    /** Une seule ligne technique, sans donnée personnelle. */
    fun technicalLine(): String =
        if (technicalMessage.isNullOrBlank()) code.name else "${code.name}: $technicalMessage"

    companion object {
        private val URL_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

        /**
         * Réduit toute URL présente dans un message technique à son **domaine**
         * (aucun chemin, aucun identifiant de vidéo, aucune donnée personnelle).
         */
        fun redact(message: String?): String? {
            if (message.isNullOrBlank()) return null
            return URL_REGEX.replace(message) { m ->
                val url = m.value
                val host = url.substringAfter("://").substringBefore('/').substringBefore('?')
                    .substringAfterLast('@').substringBefore(':')
                host.lowercase().ifBlank { "url" }
            }.take(160)
        }

        /** Conversion d'une erreur technique du moteur vers une erreur typée. */
        fun fromExtractor(error: Throwable): DownloadError {
            val code = when (error) {
                is com.bmusic.app.downloader.engine.ExtractorError.UrlInvalid ->
                    DownloadErrorCode.LIEN_INVALIDE
                is com.bmusic.app.downloader.engine.ExtractorError.Unsupported ->
                    DownloadErrorCode.SITE_NON_PRIS_EN_CHARGE
                is com.bmusic.app.downloader.engine.ExtractorError.ProtectedContent ->
                    DownloadErrorCode.CONTENU_PRIVE
                is com.bmusic.app.downloader.engine.ExtractorError.SourceUnavailable ->
                    DownloadErrorCode.MOTEUR_A_METTRE_A_JOUR
                is com.bmusic.app.downloader.engine.ExtractorError.Network ->
                    DownloadErrorCode.HORS_LIGNE
                else -> DownloadErrorCode.ECHEC_EXTRACTION
            }
            return DownloadError(code, redact(error.message))
        }

        /** Conversion d'une exception du transfert (réseau, stockage, limite). */
        fun fromTransfer(error: Throwable): DownloadError {
            val code = when {
                error is java.io.IOException &&
                    error.message?.contains("limit:", ignoreCase = true) == true ->
                    DownloadErrorCode.TELECHARGEMENT_ECHEC
                error is java.io.IOException ->
                    DownloadErrorCode.HORS_LIGNE
                error is java.lang.SecurityException ->
                    DownloadErrorCode.STOCKAGE_INDISPONIBLE
                else -> DownloadErrorCode.TELECHARGEMENT_ECHEC
            }
            return DownloadError(code, redact(error.message))
        }
    }
}
