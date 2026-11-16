package com.bmusic.app.downloader.trace

import com.bmusic.app.downloader.error.DownloadError
import com.bmusic.app.downloader.error.DownloadErrorCode
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/**
 * Journal d'étapes du téléchargeur — **le seul moyen de diagnostic sans adb**.
 *
 * Pour chaque tentative de lien, les étapes canoniques sont tracées :
 * `VALIDATION → SITE_RECONNU → MOTEUR_INITIALISE → EXTRACTION → APERCU →
 * FORMAT_CHOISI → FLUX_OUVERT → TELECHARGEMENT → ENREGISTREMENT_MEDIASTORE →
 * VISIBLE_EN_BIBLIOTHEQUE`.
 *
 * Chaque événement porte : horodatage, durée depuis l'étape précédente,
 * résultat (OK / ECHEC), code d'erreur typé, message technique court.
 *
 * **Aucune donnée personnelle** : le lien est réduit à son domaine + un hash
 * SHA-256 tronqué (jamais le lien complet, jamais le titre, jamais l'identifiant
 * de plateforme).
 */
enum class TraceStep(val code: String) {
    VALIDATION("VALIDATION"),
    SITE_RECONNU("SITE_RECONNU"),
    MOTEUR_INITIALISE("MOTEUR_INITIALISE"),
    EXTRACTION("EXTRACTION"),
    APERCU("APERCU"),
    FORMAT_CHOISI("FORMAT_CHOISI"),
    FLUX_OUVERT("FLUX_OUVERT"),
    TELECHARGEMENT("TELECHARGEMENT"),
    ENREGISTREMENT_MEDIASTORE("ENREGISTREMENT_MEDIASTORE"),
    VISIBLE_EN_BIBLIOTHEQUE("VISIBLE_EN_BIBLIOTHEQUE"),
}

enum class TraceResult { OK, ECHEC }

/** Un événement du journal. */
data class TraceEvent(
    val sessionId: String,
    val step: TraceStep,
    val result: TraceResult,
    val atEpochMs: Long,
    val durationMs: Long,
    val errorCode: DownloadErrorCode?,
    val technicalMessage: String?,
)

/**
 * Réduction d'un lien : **domaine + hash**, jamais le lien complet.
 * Sert à la fois à la confidentialité et à la lisibilité du rapport.
 */
object LinkReduction {
    /** Domaine du lien, ou `null` si le lien n'est pas exploitable. */
    fun domain(url: String): String? {
        val trimmed = url.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        val authority = trimmed.substring(schemeEnd + 3).substringBefore('/').substringBefore('?')
        val host = authority.substringAfterLast('@').substringBefore(':')
        return host.lowercase().ifBlank { null }
    }

    /** Hash SHA-256 tronqué du lien complet : permet de regrouper sans exposer. */
    fun hash(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.trim().toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Une tentative de lien = une session du journal.
 * Les étapes sont horodatées et leur durée est mesurée depuis l'étape précédente.
 */
class TraceSession internal constructor(
    val id: String,
    val startedAtEpochMs: Long,
    val domain: String?,
    val linkHash: String?,
) {
    private val _events = mutableListOf<TraceEvent>()
    private var lastAt = startedAtEpochMs

    val events: List<TraceEvent> get() = _events.toList()

    /** Étape réussie. */
    fun ok(step: TraceStep, message: String? = null): TraceEvent =
        record(step, TraceResult.OK, null, message)

    /** Étape en échec : le code typé est obligatoire. */
    fun fail(step: TraceStep, error: DownloadError): TraceEvent =
        record(step, TraceResult.ECHEC, error.code, error.technicalMessage)

    /** Étape en échec avec un code typé et un message technique libre. */
    fun fail(
        step: TraceStep,
        code: DownloadErrorCode,
        technicalMessage: String? = null,
    ): TraceEvent = record(step, TraceResult.ECHEC, code, technicalMessage)

    private fun record(
        step: TraceStep,
        result: TraceResult,
        errorCode: DownloadErrorCode?,
        technicalMessage: String?,
    ): TraceEvent {
        val now = System.currentTimeMillis()
        val event = TraceEvent(
            sessionId = id,
            step = step,
            result = result,
            atEpochMs = now,
            durationMs = (now - lastAt).coerceAtLeast(0),
            errorCode = errorCode,
            technicalMessage = technicalMessage?.take(160),
        )
        _events += event
        lastAt = now
        return event
    }

    /** Première étape en échec (null si tout est vert). */
    fun firstFailure(): TraceEvent? = _events.firstOrNull { it.result == TraceResult.ECHEC }

    /** Code d'erreur de la première étape en échec. */
    fun failureCode(): DownloadErrorCode? = firstFailure()?.errorCode

    /** Réinsertion d'un événement relu depuis un rapport persisté. */
    internal fun restore(event: TraceEvent) {
        _events += event
        if (event.atEpochMs > lastAt) lastAt = event.atEpochMs
    }
}

/**
 * Stockage mémoire du journal : les [MAX_SESSIONS] dernières tentatives,
 * [MAX_EVENTS] événements au total.
 */
object DownloadTrace {

    const val MAX_SESSIONS = 50
    const val MAX_EVENTS = 200

    private val lock = Any()
    private val counter = AtomicLong(0)
    private val _sessions = LinkedHashMap<String, TraceSession>()

    /** Démarre une session pour un lien (le lien est immédiatement réduit). */
    fun startSession(url: String, now: Long = System.currentTimeMillis()): TraceSession {
        val id = "S" + counter.incrementAndGet()
        val session = TraceSession(
            id = id,
            startedAtEpochMs = now,
            domain = LinkReduction.domain(url),
            linkHash = LinkReduction.hash(url),
        )
        synchronized(lock) {
            _sessions[id] = session
            while (_sessions.size > MAX_SESSIONS) {
                val oldest = _sessions.keys.firstOrNull() ?: break
                _sessions.remove(oldest)
            }
        }
        return session
    }

    /** Sessions les plus récentes d'abord. */
    fun sessions(): List<TraceSession> = synchronized(lock) { _sessions.values.toList().asReversed() }

    /** Événements de toutes les sessions, les plus récents d'abord. */
    fun events(): List<TraceEvent> = synchronized(lock) {
        _sessions.values.flatMap { it.events }.sortedByDescending { it.atEpochMs }
            .take(MAX_EVENTS)
    }

    /** Efface le journal. */
    fun clear() {
        synchronized(lock) { _sessions.clear() }
    }

    /** Une session par identifiant. */
    fun session(id: String): TraceSession? = synchronized(lock) { _sessions[id] }

    /**
     * Rapport texte (partageable) : une ligne par événement.
     * Format : `SESSION|id|domaine|hash|début` puis
     * `EVENT|step|résultat|horodatage|durée_ms|code|message`.
     * Le même format est relu par [DownloadTraceCodec.decode] (persistance).
     */
    fun report(): String = DownloadTraceCodec.encode(sessions().asReversed())

    /** Recharge des sessions depuis un rapport texte (persistance). */
    fun importReport(text: String) {
        val loaded = DownloadTraceCodec.decode(text)
        synchronized(lock) {
            _sessions.clear()
            loaded.forEach { _sessions[it.id] = it }
        }
    }
}
