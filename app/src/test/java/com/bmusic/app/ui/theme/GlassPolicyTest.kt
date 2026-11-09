package com.bmusic.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassPolicyTest {
    private fun signals(
        mode: GlassMode = GlassMode.AUTO,
        api: Int = 34,
        lowRam: Boolean = false,
        powerSave: Boolean = false,
        reduced: Boolean = false,
        degraded: Boolean = false,
    ) = GlassPolicy.Signals(mode = mode, apiLevel = api, isLowRamDevice = lowRam, isPowerSaveMode = powerSave, reducedMotion = reduced, degraded = degraded)

    @Test fun auto_allGood_active() = assertTrue(GlassPolicy.isActive(signals()))
    @Test fun auto_offMode_inactive() = assertFalse(GlassPolicy.isActive(signals(mode = GlassMode.OFF)))
    @Test fun onMode_forcedActive() = assertTrue(GlassPolicy.isActive(signals(mode = GlassMode.ON, api = 26, lowRam = true)))
    @Test fun auto_apiTooLow_inactive() = assertFalse(GlassPolicy.isActive(signals(api = 30)))
    @Test fun auto_lowRam_inactive() = assertFalse(GlassPolicy.isActive(signals(lowRam = true)))
    @Test fun auto_powerSave_inactive() = assertFalse(GlassPolicy.isActive(signals(powerSave = true)))
    @Test fun auto_reducedMotion_inactive() = assertFalse(GlassPolicy.isActive(signals(reduced = true)))
    @Test fun auto_degraded_inactive() = assertFalse(GlassPolicy.isActive(signals(degraded = true)))
    @Test fun tokens_radius() {
        assertEquals(16, GlassTokens.RadiusMinimal)
        assertEquals(20, GlassTokens.RadiusStandard)
        assertEquals(24, GlassTokens.RadiusProminent)
    }
    @Test fun tokens_opacity() {
        assertEquals(0.78f, GlassTokens.OpacityMinimalDark, 0.001f)
        assertEquals(0.88f, GlassTokens.OpacityProminentDark, 0.001f)
        assertEquals(0.82f, GlassTokens.OpacityMinimalLight, 0.001f)
        assertEquals(0.92f, GlassTokens.OpacityProminentLight, 0.001f)
    }
}
