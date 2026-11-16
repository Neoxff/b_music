package com.bmusic.app.downloader.trace

import com.bmusic.app.downloader.error.DownloadErrorCode

/**
 * Sérialisation texte du journal d'étapes — format **unique** pour
 * l'affichage, l'export « Partager » et la persistance sur disque.
 *
 * `SESSION|id|domaine|hash|début_epoch_ms`
 * `EVENT|step|résultat|epoch_ms|durée_ms|code_erreur|message_technique`
 *
 * Le message technique est echappé (`|` → `/`, retours ligne → espace) pour
 * garder une ligne par événement.
 */
object DownloadTraceCodec {

    fun encode(sessions: List<TraceSession>): String = buildString {
        sessions.forEach { s ->
            append("SESSION|").append(s.id).append('|')
                .append(s.domain ?: "?").append('|')
                .append(s.linkHash ?: "?").append('|')
                .append(s.startedAtEpochMs).append('\n')
            s.events.forEach { e ->
                append("EVENT|").append(e.sessionId).append('|')
                    .append(e.step.code).append('|')
                    .append(e.result.name).append('|')
                    .append(e.atEpochMs).append('|')
                    .append(e.durationMs).append('|')
                    .append(e.errorCode?.name ?: "-").append('|')
                    .append(escape(e.technicalMessage)).append('\n')
            }
        }
    }

    /** Relit un rapport ; les lignes illisibles sont ignorées (jamais de crash). */
    fun decode(text: String): List<TraceSession> {
        val sessions = LinkedHashMap<String, TraceSession>()
        text.lineSequence().forEach { line ->
            val parts = line.split('|')
            when {
                parts.size >= 5 && parts[0] == "SESSION" -> {
                    val id = parts[1]
                    val started = parts[4].toLongOrNull() ?: 0L
                    sessions[id] = TraceSession(
                        id = id,
                        startedAtEpochMs = started,
                        domain = parts[2].takeIf { it != "?" },
                        linkHash = parts[3].takeIf { it != "?" },
                    )
                }

                parts.size >= 8 && parts[0] == "EVENT" -> {
                    val id = parts[1]
                    val step = TraceStep.entries.firstOrNull { it.code == parts[2] }
                    val result = runCatching { TraceResult.valueOf(parts[3]) }.getOrNull()
                    if (step != null && result != null) {
                        val session = sessions.getOrPut(id) {
                            TraceSession(id, 0L, null, null)
                        }
                        val at = parts[4].toLongOrNull() ?: 0L
                        val duration = parts[5].toLongOrNull() ?: 0L
                        val code = runCatching { DownloadErrorCode.valueOf(parts[6]) }.getOrNull()
                        val message = parts[7].takeIf { it != "-" }
                        session.restore(
                            TraceEvent(
                                sessionId = id,
                                step = step,
                                result = result,
                                atEpochMs = at,
                                durationMs = duration,
                                errorCode = code,
                                technicalMessage = message,
                            ),
                        )
                    }
                }
            }
        }
        return sessions.values.toList()
    }

    private fun escape(message: String?): String =
        (message ?: "-").replace('\n', ' ').replace('|', '/').take(160)
}
