package com.bmusic.app.downloader.progress

import com.bmusic.app.downloader.data.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * BUILD — fenêtre de vitesse stateful (design doc 15).
 * Accumule les échantillons cumulés du transfert et produit une vitesse
 * lissée (EMA), jamais une valeur instantanée qui saute.
 */
class SpeedWindow {
    private var emaBps = 0.0
    private var firstSampleMs: Long? = null
    private var lastSampleMs: Long? = null
    private var lastBytes: Long? = null
    private var lastByteAtMs: Long? = null

    fun sample(nowMs: Long, cumulativeBytes: Long) {
        val prevAt = lastSampleMs
        val prevBytes = lastBytes
        if (prevAt == null || prevBytes == null) {
            firstSampleMs = nowMs
            lastSampleMs = nowMs
            lastBytes = cumulativeBytes
            lastByteAtMs = nowMs
            return
        }
        val dt = nowMs - prevAt
        if (dt <= 0L) return
        val delta = (cumulativeBytes - prevBytes).coerceAtLeast(0L)
        if (delta > 0L) lastByteAtMs = nowMs
        emaBps = SpeedCalculator.smooth(emaBps, delta, dt)
        lastSampleMs = nowMs
        lastBytes = cumulativeBytes
    }

    fun speed(nowMs: Long): Long {
        val at = lastByteAtMs ?: return 0L
        return SpeedCalculator.speedAfterIdle(at, nowMs, emaBps)
    }

    private fun stableElapsed(nowMs: Long): Long {
        val first = firstSampleMs ?: return 0L
        return (nowMs - first).coerceAtLeast(0L)
    }

    /** Construit le snapshot lu par les trois consommateurs (source unique). */
    fun snapshot(
        id: Long,
        bytesDownloaded: Long,
        totalBytes: Long,
        state: DownloadState,
        nowMs: Long,
    ): DownloadProgress {
        val knownTotal = if (totalBytes > 0L) totalBytes else null
        val currentSpeed = speed(nowMs)
        return DownloadProgress(
            id = id,
            bytesDownloaded = bytesDownloaded,
            bytesTotal = knownTotal,
            percent = ProgressLogic.percent(bytesDownloaded, knownTotal),
            speedBytesPerSecond = currentSpeed,
            remainingSeconds = knownTotal?.let {
                SpeedCalculator.remainingSeconds(bytesDownloaded, it, currentSpeed, stableElapsed(nowMs))
            },
            state = state,
        )
    }
}

/**
 * Bus partagé : source UNIQUE de progression pour l'onglet Lien, l'onglet
 * Historique et la notification. Émission bornée par le throttle applicatif.
 */
class DownloadProgressBus {
    private val _state = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val state: StateFlow<Map<Long, DownloadProgress>> = _state

    fun publish(progress: DownloadProgress) {
        _state.update { it + (progress.id to progress) }
    }

    fun latest(id: Long): DownloadProgress? = _state.value[id]

    fun remove(id: Long) {
        _state.update { it - id }
    }

    fun clear() {
        _state.value = emptyMap()
    }
}
