package com.bmusic.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/**
 * Library scroll baseline (v1.0.0): scrolls the track list with a test library
 * of 5 000 tracks. Requires the test library to be seeded on the device
 * (5000 files generated via `adb push` or the on-device seeder).
 */
class LibraryScrollBenchmark {

    @get:Rule
    val macrobenchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureLibraryScroll() {
        macrobenchmarkRule.measureRepeated(
            packageName = "com.bmusic.app",
            metrics = listOf(FrameTimingMetric()),
            iterations = 3,
            startupMode = androidx.benchmark.macro.StartupMode.COLD
        ) {
            startActivityAndWait()
            val list = device.findObject(By.scrollable(true))
            assertTrue("Library list must be scrollable", list != null)
            list!!.scrollForward()
            device.waitForIdle()
            repeat(20) { list!!.scrollForward() }
        }
    }
}
