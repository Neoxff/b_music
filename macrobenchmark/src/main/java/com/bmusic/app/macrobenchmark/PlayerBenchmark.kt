package com.bmusic.app.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/** Mini-player → full player transition baseline (v1.0.0), spring 0.86/380. */
class PlayerBenchmark {

    @get:Rule
    val macrobenchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureMiniToFullPlayerTransition() {
        macrobenchmarkRule.measureRepeated(
            packageName = "com.bmusic.app",
            metrics = listOf(
                FrameTimingMetric(),
                TraceSectionMetric("miniToFullPlayer", TraceSectionMetric.Mode.Sum)
            ),
            iterations = 5,
            startupMode = StartupMode.WARM
        ) {
            startActivityAndWait()
            val miniPlayer = device.findObject(By.res("com.bmusic.app:id/mini_player_container"))
            assertTrue("Mini-player must be visible", miniPlayer != null)
            miniPlayer!!.click()
            device.waitForIdle()
        }
    }
}
