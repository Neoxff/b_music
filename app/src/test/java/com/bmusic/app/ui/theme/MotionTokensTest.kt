package com.bmusic.app.ui.theme

import org.junit.Assert.*
import org.junit.Test

class MotionTokensTest {

    @Test
    fun `durees motion tokens stables`() {
        assertEquals(120, Motion.DurationShort)
        assertEquals(220, Motion.DurationBase)
        assertEquals(300, Motion.DurationLong)
        assertEquals(420, Motion.DurationXLong)
        assertEquals(1200, Motion.DurationSkeleton)
    }

    @Test
    fun `scaleDuration zero quand animations reduites`() {
        assertEquals(0, Motion.scaleDuration(220, reduced = true))
        assertEquals(220, Motion.scaleDuration(220, reduced = false))
        assertEquals(0, Motion.scaleDuration(420, reduced = true))
    }

    @Test
    fun `stagger borne`() {
        assertEquals(24, Motion.StaggerMs)
        assertEquals(8, Motion.MaxStaggerItems)
    }
}
