package com.bmusic.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/**
 * GlassPolicy — décide "flou ou opaque" selon le réglage, l'API, isLowRamDevice,
 * la batterie, les animations réduites et un signal de dégradation Lean.
 */
enum class GlassMode { AUTO, ON, OFF }

object GlassPolicy {

    data class Signals(
        val mode: GlassMode,
        val apiLevel: Int,
        val isLowRamDevice: Boolean,
        val isPowerSaveMode: Boolean,
        val reducedMotion: Boolean,
        val degraded: Boolean = false,
    )

    fun isActive(signals: Signals): Boolean {
        if (signals.mode == GlassMode.OFF) return false
        if (signals.mode == GlassMode.ON) return true
        // AUTO : tout signal dégradant force opaque.
        if (signals.apiLevel < 31) return false
        if (signals.isLowRamDevice) return false
        if (signals.isPowerSaveMode) return false
        if (signals.reducedMotion) return false
        if (signals.degraded) return false
        return true
    }

    @Composable
    fun isActiveComposed(
        mode: GlassMode,
        context: Context = LocalContext.current,
        reducedMotion: Boolean = rememberReducedMotion(),
    ): Boolean {
        val signals = Signals(
            mode = mode,
            apiLevel = Build.VERSION.SDK_INT,
            isLowRamDevice = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice,
            isPowerSaveMode = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode,
            reducedMotion = reducedMotion,
        )
        return isActive(signals)
    }
}
