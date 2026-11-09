package com.bmusic.app.diagnostics

import org.junit.Assert.*
import org.junit.Test

class DiagnosticsStateTest {

    @Test
    fun `jank accumulator calcule % lent, pire et moyenne`() {
        val acc = JankAccumulator()
        acc.addJankFrame(8.0)
        acc.addJankFrame(16.0)
        acc.addJankFrame(32.0)
        acc.addJankFrame(24.0)
        val snap = acc.snapshot()
        assertEquals(4L, snap.totalFrames)
        assertEquals(3L, snap.slowFrames) // >=16ms : 16,32,24
        assertEquals(32.0, snap.worstMs, 0.001)
        assertEquals(20.0, snap.avgMs, 0.001)
        assertEquals(75.0, snap.slowPercent, 0.001)
    }

    @Test
    fun `jank accumulator reset`() {
        val acc = JankAccumulator()
        acc.addJankFrame(32.0)
        acc.reset()
        val snap = acc.snapshot()
        assertEquals(0L, snap.totalFrames)
        assertEquals(0.0, snap.slowPercent, 0.001)
    }

    @Test
    fun `rapport sans donnees personnelles`() {
        val report = DiagnosticsReport.build(
            versionName = "1.0.0",
            versionCode = 1,
            device = DeviceInfoSnapshot(34, "model", false, false, 60f),
            jank = JankStatsSnapshot(100, 5, 32.0, 10.0),
            memory = MemorySnapshot(1000, 200, 300, 1500),
            blur = BlurStateSnapshot(false, "API < 31", 0),
            scenarios = listOf(
                ScenarioResult(
                    name = "défilement bibliothèque",
                    startedAtMs = 1000L,
                    endedAtMs = 31000L,
                    jank = JankStatsSnapshot(200, 8, 28.0, 9.5),
                    memory = MemorySnapshot(1200, 250, 300, 1500),
                    blur = BlurStateSnapshot(false, "API < 31", 0),
                )
            ),
            generatedAtMs = 0L,
        )
        assertTrue(report.contains("Images lentes"))
        assertTrue(report.contains("PSS"))
        assertTrue(report.contains("défilement bibliothèque"))
        // Aucune donnée personnelle : pas de chemin, pas d'URI, pas de titre
        assertFalse(report.contains("/storage"))
        assertFalse(report.contains("content://"))
        assertFalse(report.contains(".mp3"))
    }

    @Test
    fun `rapport encode version et horodatage`() {
        val report = DiagnosticsReport.build(
            versionName = "9.9.9",
            versionCode = 42,
            device = DeviceInfoSnapshot(31, "m", false, false, 120f),
            jank = JankStatsSnapshot(),
            memory = MemorySnapshot(0, 0, 0, 0),
            blur = BlurStateSnapshot(true, "AUTO", 1),
            scenarios = emptyList(),
        )
        assertTrue(report.contains("9.9.9 (42)"))
        assertTrue(report.contains("Généré le:"))
    }
}
