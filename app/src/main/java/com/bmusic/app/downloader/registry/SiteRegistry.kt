package com.bmusic.app.downloader.registry

/** Statut d'un site vis-à-vis du moteur d'extraction. */
enum class SiteStatus {
    /** Le moteur sait extraire ce site. */
    PRIS_EN_CHARGE,

    /** Partiellement pris en charge (certains contenus seulement). */
    PARTIEL,

    /** Aucun moteur : le message affiché doit être honnête. */
    NON_PRIS_EN_CHARGE,
}

/**
 * Un site et son statut.
 *
 * @param id identifiant technique (`youtube`, `tiktok`…)
 * @param label libellé affiché
 * @param domains domaines et variantes reconnus (sans `www`)
 * @param engineId moteur utilisé (`newpipe`) ou null si aucun
 */
data class SiteDefinition(
    val id: String,
    val label: String,
    val status: SiteStatus,
    val domains: List<String>,
    val engineId: String?,
)

/**
 * Registre des sites : **domaine → moteur + statut**.
 *
 * Le moteur réellement livré (`NewPipeExtractorEngine`) ne couvre que
 * YouTube, SoundCloud, Bandcamp et PeerTube : tous les autres sites sont
 * déclarés `NON_PRIS_EN_CHARGE` pour que l'utilisateur reçoive un message
 * honnête plutôt qu'une erreur générique.
 *
 * La reconnaissance tolère les variantes réelles : mobile (`m.`), liens
 * courts (`youtu.be`, `vm.tiktok.com`), absence de `www`, paramètres de
 * suivi (`utm_*`, `fbclid`, `igshid`) et caractères encodés.
 */
object SiteRegistry {

    private val sites: List<SiteDefinition> = listOf(
        SiteDefinition(
            id = "youtube",
            label = "YouTube",
            status = SiteStatus.PRIS_EN_CHARGE,
            domains = listOf("youtube.com", "youtu.be", "youtube-nocookie.com", "music.youtube.com"),
            engineId = "newpipe",
        ),
        SiteDefinition(
            id = "soundcloud",
            label = "SoundCloud",
            status = SiteStatus.PRIS_EN_CHARGE,
            domains = listOf("soundcloud.com", "snd.sc"),
            engineId = "newpipe",
        ),
        SiteDefinition(
            id = "bandcamp",
            label = "Bandcamp",
            status = SiteStatus.PRIS_EN_CHARGE,
            domains = listOf("bandcamp.com"),
            engineId = "newpipe",
        ),
        SiteDefinition(
            id = "peertube",
            label = "PeerTube",
            status = SiteStatus.PRIS_EN_CHARGE,
            domains = listOf(
                "peertube.tv", "peertube.fr", "peertube.be", "peertube.video",
                "peertube.uno", "peertube.rocks", "sepiaseconds.org",
            ),
            engineId = "newpipe",
        ),
        SiteDefinition(
            id = "vimeo",
            label = "Vimeo",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("vimeo.com", "player.vimeo.com"),
            engineId = null,
        ),
        SiteDefinition(
            id = "dailymotion",
            label = "Dailymotion",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("dailymotion.com", "dai.ly"),
            engineId = null,
        ),
        SiteDefinition(
            id = "tiktok",
            label = "TikTok",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com"),
            engineId = null,
        ),
        SiteDefinition(
            id = "instagram",
            label = "Instagram",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("instagram.com", "instagr.am"),
            engineId = null,
        ),
        SiteDefinition(
            id = "facebook",
            label = "Facebook",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("facebook.com", "fb.watch", "fb.com"),
            engineId = null,
        ),
        SiteDefinition(
            id = "x",
            label = "X (Twitter)",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("x.com", "twitter.com", "t.co", "mobile.twitter.com"),
            engineId = null,
        ),
        SiteDefinition(
            id = "snapchat",
            label = "Snapchat",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("snapchat.com"),
            engineId = null,
        ),
        SiteDefinition(
            id = "reddit",
            label = "Reddit",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("reddit.com", "redd.it", "v.redd.it"),
            engineId = null,
        ),
        SiteDefinition(
            id = "twitch",
            label = "Twitch",
            status = SiteStatus.NON_PRIS_EN_CHARGE,
            domains = listOf("twitch.tv", "clips.twitch.tv", "m.twitch.tv"),
            engineId = null,
        ),
    )

    /** Tous les sites connus, dans l'ordre d'affichage. */
    fun all(): List<SiteDefinition> = sites

    /** Les sites pris en charge (statut != NON_PRIS_EN_CHARGE). */
    fun supported(): List<SiteDefinition> = sites.filter { it.status != SiteStatus.NON_PRIS_EN_CHARGE }

    /**
     * Domaine effectif d'une URL, sans `www.` ni identifiants.
     * Retourne null si l'URL n'est pas exploitable.
     */
    fun domainOf(url: String): String? {
        val trimmed = url.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        val authority = trimmed.substring(schemeEnd + 3).substringBefore('/').substringBefore('?')
        val host = authority.substringAfterLast('@').substringBefore(':').lowercase()
        if (host.isBlank() || !host.contains('.')) return null
        return host.removePrefix("www.")
    }

    /**
     * Site correspondant à une URL, ou null si le domaine est inconnu.
     * La correspondance est faite sur le domaine **suffixe** : `m.youtube.com`
     * et `music.youtube.com` retombent sur `youtube.com`.
     */
    fun lookup(url: String): SiteDefinition? {
        val host = domainOf(url) ?: return null
        return sites.firstOrNull { site ->
            site.domains.any { host == it || host.endsWith(".$it") }
        }
    }

    /** Statut d'une URL (null si domaine inconnu). */
    fun statusOf(url: String): SiteStatus? = lookup(url)?.status
}
