package com.bmusic.app.diagnostics

import android.os.Debug
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logique pure de l'écran Diagnostics (debug uniquement, src/debug).
 * Aucune donnée personnelle : jamais de tille, chemin, URI ou historique.
 */
data class JankStatsSnapshot(
    val totalFrames: Long = 0,
    val slowFrames: Long = 0,
    val worstMs: Double = 0.0,
    val avgMs: Double = 0.0,
) {
    val slowPercent: Double
        get() = if (totalFrames == 0L) 0.0 else (slowFrames * 100.0 / totalFrames)
}

class JankAccumulator(private val thresholdMs: Double = 16.0) {
    private var total = 0L
    private var slow = 0L
    private var worst = 0.0
    private var sum = 0.0

    @Synchronized
    fun addJankFrame(frameDurationMs: Double) {
        if (frameDurationMs <= 0.0) return
        total++
        sum += frameDurationMs
        if (frameDurationMs > worst) worst = frameDurationMs
        if (frameDurationMs >= thresholdMs) slow++
    }

    @Synchronized
    fun snapshot(): JankStatsSnapshot = JankStatsSnapshot(
        totalFrames = total,
        slowFrames = slow,
        worstMs = worst,
        avgMs = if (total == 0L) 0.0 else sum / total,
    )

    @Synchronized
    fun reset() {
        total = 0L
        slow = 0L
        worst = 0.0
        sum = 0.0
    }
}

data class MemorySnapshot(
    val pssKb: Int,
    val javaHeapKb: Int,
    val nativeHeapKb: Int,
    val pssPeakKb: Int,
) {
    companion object {
        private var peakKb: Int = 0

        fun current(): MemorySnapshot {
            val info = Debug.MemoryInfo()
            Debug.getMemoryInfo(info)
            if (info.totalPss > peakKb) peakKb = info.totalPss
            return MemorySnapshot(
                pssKb = info.totalPss,
                javaHeapKb = info.dalvikPss,
                nativeHeapKb = info.nativePss,
                pssPeakKb = peakKb,
            )
        }

        fun resetPeak() {
            peakKb = 0
        }

        fun peakKb(): Int = peakKb
    }
}

data class DeviceInfoSnapshot(
    val apiLevel: Int,
    val model: String,
    val isLowRam: Boolean,
    val powerSave: Boolean,
    val refreshRateHz: Float,
)

data class BlurStateSnapshot(
    val active: Boolean,
    val reason: String,
    val surfaceCount: Int,
)

data class ScenarioResult(
    val name: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val jank: JankStatsSnapshot,
    val memory: MemorySnapshot,
    val blur: BlurStateSnapshot,
)

/** Compteur de surfaces floutées actives (étape 3 : incrémentée par GlassSurface via DiagnosticsBlurCounter). */
object DiagnosticsBlurCounter {
    private var count: Int = 0

    @Synchronized
    fun setCount(value: Int) {
        count = value.coerceAtLeast(0)
    }

    @Synchronized
    fun count(): Int = count
}

object DiagnosticsReport {
    fun build(
        versionName: String,
        versionCode: Int,
        device: DeviceInfoSnapshot,
        jank: JankStatsSnapshot,
        memory: MemorySnapshot,
        blur: BlurStateSnapshot,
        scenarios: List<ScenarioResult>,
        generatedAtMs: Long = System.currentTimeMillis(),
    ): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(generatedAtMs))
        return buildString {
            appendLine("B-Music Diagnostics (debug)")
            appendLine("Généré le: $date")
            appendLine("Version: $versionName ($versionCode)")
            appendLine()
            appendLine("=== Appareil ===")
            appendLine("API: ${device.apiLevel} | Modèle: ${device.model}")
            appendLine("Low RAM: ${device.isLowRam} | Économiseur batterie: ${device.powerSave}")
            appendLine("Rafraîchissement: ${device.refreshRateHz} Hz")
            appendLine()
            appendLine("=== Jank (JankStats) ===")
            appendLine("Images totales: ${jank.totalFrames}")
            appendLine("Images lentes: ${jank.slowFrames} (${"%.2f".format(jank.slowPercent)} %)")
            appendLine("Pire durée: ${"%.2f".format(jank.worstMs)} ms")
            appendLine("Durée moyenne: ${"%.2f".format(jank.avgMs)} ms")
            appendLine()
            appendLine("=== Mémoire ===")
            appendLine("PSS: ${memory.pssKb} Ko | Java heap: ${memory.javaHeapKb} Ko | Native heap: ${memory.nativeHeapKb} Ko")
            appendLine("PSS pic session: ${memory.pssPeakKb} Ko")
            appendLine()
            appendLine("=== Flou (GlassPolicy) ===")
            appendLine("Mode: ${if (blur.active) "flou" else "opaque"} | Raison: ${blur.reason}")
            appendLine("Surfaces floutées actives: ${blur.surfaceCount}")
            if (scenarios.isNotEmpty()) {
                appendLine()
                appendLine("=== Scénarios ===")
                for (s in scenarios) {
                    appendLine("- ${s.name} (${s.endedAtMs - s.startedAtMs} ms) : " +
                        "lentes ${s.jank.slowFrames}/${s.jank.totalFrames} " +
                        "(${"%.2f".format(s.jank.slowPercent)} %), " +
                        "pire ${"%.2f".format(s.jank.worstMs)} ms, " +
                        "moyenne ${"%.2f".format(s.jank.avgMs)} ms, " +
                        "PSS ${s.memory.pssKb} Ko (pic ${s.memory.pssPeakKb} Ko), " +
                        "flou=${if (s.blur.active) "on" else "off"} (${s.blur.reason})")
                }
            }
            appendLine()
            appendLine("Note: rapport sans données personnelles (aucun titre, chemin ou URI).")
        }
    }
}
