package com.bmusic.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/** Cold start baseline (v1.0.0): forceStop + startActivity, StartupTimingMetric. */
class StartupBenchmark {

    @get:Rule
    val macrobenchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureColdStartup() {
        macrobenchmarkRule.measureRepeated(
            packageName = "com.bmusic.app",
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            iterations = 5
        ) {
            startActivityAndWait()
        }
    }

    @Test
    fun measureWarmStartup() {
        macrobenchmarkRule.measureRepeated(
            packageName = "com.bmusic.app",
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            startupMode = StartupMode.WARM,
            iterations = 5
        ) {
            startActivityAndWait()
        }
    }
}
