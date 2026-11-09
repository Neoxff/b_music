package com.bmusic.app.diagnostics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.metrics.performance.JankStats
import com.bmusic.app.BuildConfig
import com.bmusic.app.ui.theme.BMusicTheme
import com.bmusic.app.ui.theme.GlassMode
import com.bmusic.app.ui.theme.GlassPolicy
import com.bmusic.app.ui.theme.rememberReducedMotion
import android.os.Build
import android.app.ActivityManager
import android.os.PowerManager
import android.content.Context

/**
 * Écran Diagnostics — debug uniquement (src/debug). Absent de la release.
 * Accès : entrée debug insérée dans Réglages (BuildConfig.DEBUG) — voir
 * PreferencesScreenFragment. Aucune donnée personnelle dans le rapport.
 */
class DiagnosticsActivity : ComponentActivity() {

    private val jank = JankAccumulator()
    private val scenarios = mutableListOf<ScenarioResult>()
    private var scenarioStartMs: Long = 0L
    private var scenarioStartJank: JankStatsSnapshot? = null
    private var scenarioName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        JankStats.createAndTrack(window) { frameData ->
            jank.addJankFrame(frameData.frameDurationUiNanos / 1_000_000.0)
        }

        setContent {
            BMusicTheme {
                var tick by remember { mutableLongStateOf(0L) }
                val context = LocalContext.current
                val reducedMotion = rememberReducedMotion()
                // Rafraîchir les compteurs via mesure (jank en direct)
                val device = remember {
                    val display = windowManager.defaultDisplay
                    val refresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        display.refreshRate
                    } else {
                        @Suppress("DEPRECATION") display.refreshRate
                    }
                    DeviceInfoSnapshot(
                        apiLevel = Build.VERSION.SDK_INT,
                        model = Build.MODEL ?: "unknown",
                        isLowRam = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice,
                        powerSave = (getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode,
                        refreshRateHz = refresh,
                    )
                }
                val blurActive = remember(reducedMotion) {
                    val signals = GlassPolicy.Signals(
                        mode = GlassMode.AUTO,
                        apiLevel = Build.VERSION.SDK_INT,
                        isLowRamDevice = device.isLowRam,
                        isPowerSaveMode = device.powerSave,
                        reducedMotion = reducedMotion,
                    )
                    val active = GlassPolicy.isActive(signals)
                    val reason = when {
                        signals.apiLevel < 31 -> "API < 31"
                        signals.isLowRamDevice -> "low RAM device"
                        signals.isPowerSaveMode -> "économiseur de batterie"
                        signals.reducedMotion -> "animations réduites"
                        active -> "AUTO : aucun signal dégradant"
                        else -> "opaque (AUTO)"
                    }
                    BlurStateSnapshot(active = active, reason = reason, surfaceCount = DiagnosticsBlurCounter.count())
                }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(500)
                        tick++
                    }
                }
                // tick lu pour forcer le rafraîchissement
                @Suppress("UNUSED_EXPRESSION")
                tick

                DiagnosticsScreen(
                    jank = jank.snapshot(),
                    memory = MemorySnapshot.current(),
                    device = device,
                    blur = blurActive,
                    scenarioRunning = scenarioStartMs > 0L,
                    scenarioName = scenarioName,
                    scenarioCount = scenarios.size,
                    onStartScenario = { name: String ->
                        scenarioName = name
                        scenarioStartMs = System.currentTimeMillis()
                        scenarioStartJank = jank.snapshot()
                    },
                    onEndScenario = {
                        val jankNow = jank.snapshot()
                        val start = scenarioStartJank ?: jankNow
                        val slice = JankStatsSnapshot(
                            totalFrames = (jankNow.totalFrames - start.totalFrames).coerceAtLeast(0),
                            slowFrames = (jankNow.slowFrames - start.slowFrames).coerceAtLeast(0),
                            worstMs = jankNow.worstMs,
                            avgMs = jankNow.avgMs,
                        )
                        scenarios.add(
                            ScenarioResult(
                                name = scenarioName.ifBlank { "scénario" },
                                startedAtMs = scenarioStartMs,
                                endedAtMs = System.currentTimeMillis(),
                                jank = slice,
                                memory = MemorySnapshot.current(),
                                blur = blurActive,
                            )
                        )
                        scenarioStartMs = 0L
                        scenarioStartJank = null
                        tick++
                    },
                    onResetJank = { jank.reset(); MemorySnapshot.resetPeak(); tick++ },
                    onExport = {
                        val report = DiagnosticsReport.build(
                            versionName = BuildConfig.VERSION_NAME,
                            versionCode = BuildConfig.VERSION_CODE,
                            device = device,
                            jank = jank.snapshot(),
                            memory = MemorySnapshot.current(),
                            blur = blurActive,
                            scenarios = scenarios.toList(),
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report)
                            putExtra(Intent.EXTRA_SUBJECT, "B-Music Diagnostics ${BuildConfig.VERSION_NAME}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager le rapport Diagnostics"))
                    },
                )
            }
        }
    }
}
