package com.bmusic.app.ui.theme

import com.bmusic.app.ui.screen.other.glassBackgroundAlpha
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

/**
 * Tests Étape 3, surface (1) mini-player : tokens, contraste AA,
 * repli opaque (Désactivé = v1.1.x, mise en page inchangée).
 * Pur JVM (pas d'android.graphics.Color, mocké en unit tests).
 */
class MiniPlayerGlassTest {

    @Test
    fun `alpha actif correspond aux tokens Minimal sombre et clair`() {
        assertEquals(0.78f, glassBackgroundAlpha(active = true, dark = true), 0.001f)
        assertEquals(0.82f, glassBackgroundAlpha(active = true, dark = false), 0.001f)
    }

    @Test
    fun `alpha opaque si repli ou desactive`() {
        assertEquals(1f, glassBackgroundAlpha(active = false, dark = true), 0.001f)
        assertEquals(1f, glassBackgroundAlpha(active = false, dark = false), 0.001f)
    }

    @Test
    fun `contraste AA texte sur fond mini-player extremes`() {
        val darkSurface = blend(0xFF121212.toInt(), 0.78f, 0xFF000000.toInt())
        assertTrue(contrast(0xFFFFFFFF.toInt(), darkSurface) >= 4.5)
        val lightSurface = blend(0xFFFFFFFF.toInt(), 0.82f, 0xFFFFFFFF.toInt())
        assertTrue(contrast(0xFF121212.toInt(), lightSurface) >= 4.5)
        val saturated = blend(0xFF121212.toInt(), 0.78f, 0xFFFF0000.toInt())
        assertTrue(contrast(0xFFFFFFFF.toInt(), saturated) >= 4.5)
    }

    private fun blend(surface: Int, alpha: Float, bg: Int): Int {
        fun ch(shift: Int): Int = (((surface shr shift) and 0xFF) * alpha + ((bg shr shift) and 0xFF) * (1f - alpha)).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun luminance(color: Int): Double {
        fun lin(v: Double): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = lin(((color shr 16) and 0xFF).toDouble())
        val g = lin(((color shr 8) and 0xFF).toDouble())
        val b = lin((color and 0xFF).toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrast(a: Int, b: Int): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }
}
