package com.bmusic.app.downloader.progress

import java.util.Locale
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * BUILD — progression de téléchargement (design doc 15).
 * Logique PURE, sans Android Framework : testable en JVM hors ligne.
 *
 * - Vitesse : moyenne mobile exponentielle (EMA) sur une fenêtre de ~3 s,
 *   avec retour à zéro après 2 s sans octet reçu.
 * - Total inconnu (Content-Length absent) : pourcentage NULL, jamais de faux chiffre.
 * - Temps restant masqué tant que la vitesse n'est pas stable (< 2 s de données).
 * - Formatage français (virgule décimale).
 */

internal const val SPEED_WINDOW_MS = 3_000L
internal const val IDLE_MS = 2_000L
internal const val STABLE_MS = 2_000L

object SpeedCalculator {

    /**
     * Lissage exponentiel : alpha = 1 - exp(-dt / (window/3)) — la fenêtre de
     * 3 s correspond à ~3 constantes de temps (95 % de la mémoire cumulée).
     */
    fun smooth(previousBps: Double, deltaBytes: Long, deltaMs: Long): Double {
        if (deltaMs <= 0L) return previousBps
        val tau = SPEED_WINDOW_MS / 3.0
        val alpha = 1.0 - exp(-deltaMs / tau)
        val instant = deltaBytes * 1_000.0 / deltaMs
        return previousBps + alpha * (instant - previousBps)
    }

    /** Vitesse nulle si aucun octet reçu depuis [IDLE_MS]. */
    fun speedAfterIdle(lastByteAtMs: Long, nowMs: Long, emaBps: Double): Long =
        if (nowMs - lastByteAtMs >= IDLE_MS) 0L else emaBps.toLong().coerceAtLeast(0L)

    /**
     * Temps restant, ou null si : total inconnu, vitesse nulle, ou données
     * de vitesse instables (moins de [STABLE_MS] de mesure).
     */
    fun remainingSeconds(
        doneBytes: Long,
        totalBytes: Long,
        speedBps: Long,
        stableElapsedMs: Long,
    ): Long? {
        if (totalBytes <= 0L || speedBps <= 0L || stableElapsedMs < STABLE_MS) return null
        val remaining = (totalBytes - doneBytes).coerceAtLeast(0L)
        return (remaining.toDouble() / speedBps).roundToLong().coerceAtLeast(0L)
    }
}

object ProgressLogic {

    /** Pourcentage, ou null si le total est inconnu (flux direct). */
    fun percent(doneBytes: Long, totalBytes: Long?): Int? {
        if (totalBytes == null || totalBytes <= 0L) return null
        // half-up déterministe : Math.rint (kotlin.math.round) arrondit 62,5 à 62 (au pair).
        return floor(doneBytes.toDouble() * 100.0 / totalBytes + 0.5).toInt().coerceIn(0, 100)
    }

    /** Monotonie : une valeur connue ne peut jamais reculer. */
    fun monotonic(previous: Int?, next: Int?): Int? = when {
        next == null -> null
        previous == null -> next
        else -> maxOf(previous, next)
    }
}

object ProgressFormatter {

    private val FR = Locale.FRANCE
    private const val MIB = 1_048_576L
    private const val KIB = 1_024L

    /** `781 Ko/s` sous 1 Mo/s, sinon `2,4 Mo/s` (une décimale, virgule FR). */
    fun speed(bytesPerSecond: Long): String = when {
        bytesPerSecond < MIB -> "${bytesPerSecond / KIB} Ko/s"
        else -> String.format(FR, "%.1f Mo/s", bytesPerSecond.toDouble() / MIB)
    }

    /** `30,0 / 48,0 Mo`, ou taille seule si total inconnu. */
    fun size(doneBytes: Long, totalBytes: Long?): String = when {
        totalBytes == null || totalBytes <= 0L -> String.format(FR, "%.1f Mo", doneBytes.toDouble() / MIB)
        else -> String.format(
            FR, "%.1f / %.1f Mo",
            doneBytes.toDouble() / MIB, totalBytes.toDouble() / MIB,
        )
    }

    /** `8 s`, ou tiret si inconnu. */
    fun remaining(seconds: Long?): String = if (seconds == null) "—" else "$seconds s"
}

/**
 * Limiteur de fréquence : émet au plus toutes les [minIntervalMs], et
 * uniquement si la clé affichée (pourcentage + vitesse) a changé.
 */
class ProgressThrottle(private val minIntervalMs: Long = 250L) {
    private var lastEmitAtMs: Long? = null
    private var lastKey: String? = null

    fun shouldEmit(nowMs: Long, key: String): Boolean {
        if (key == lastKey) return false
        val last = lastEmitAtMs
        if (last != null && nowMs - last < minIntervalMs) return false
        lastEmitAtMs = nowMs
        lastKey = key
        return true
    }
}
