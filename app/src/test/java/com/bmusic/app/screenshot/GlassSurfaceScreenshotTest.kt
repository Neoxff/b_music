package com.bmusic.app.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.bmusic.app.ui.theme.BMusicTheme
import com.bmusic.app.ui.theme.GlassSurface
import com.bmusic.app.ui.theme.GlassTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Captures GlassSurface Étape 2 — flou et opaque, clair et sombre.
 * Prouve le repli opaque de même mise en page (Box, rayon, contour).
 * PNG dans docs/baseline/captures/glass-*.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp", application = com.bmusic.app.test.EmptyTestApplication::class)
class GlassSurfaceScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @org.junit.Before
    fun stopKoinBefore() {
        try { org.koin.core.context.stopKoin() } catch (_: Exception) {}
    }

    @org.junit.After
    fun stopKoinAfter() {
        try { org.koin.core.context.stopKoin() } catch (_: Exception) {}
    }

    @Test
    fun capture_glasssurface_flou_et_opaque_clair_et_sombre() {
        val outDir = resolveCapturesDir()
        outDir.mkdirs()
        val darkState = mutableStateOf(false)
        val levelState = mutableStateOf(GlassTokens.Level.Standard)

        composeRule.setContent {
            BMusicTheme(darkTheme = darkState.value, blackTheme = false, dynamicColor = false, customFont = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GlassSurface niveau ${levelState.value}")
                    GlassSurface(level = levelState.value, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Text("contenu")
                    }
                }
            }
        }

        for (dark in listOf(false, true)) {
            for (level in GlassTokens.Level.values()) {
                composeRule.runOnIdle {
                    darkState.value = dark
                    levelState.value = level
                }
                composeRule.waitForIdle()
                val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                val file = File(outDir, "glass-${level.name.lowercase()}-${if (dark) "dark" else "light"}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                org.junit.Assert.assertTrue(file.length() > 0)
            }
        }
    }

    private fun resolveCapturesDir(): File {
        val userDir = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(userDir, "docs/baseline/captures"),
            File(userDir, "../docs/baseline/captures"),
        )
        return candidates.firstOrNull { it.parentFile?.exists() == true } ?: candidates.last()
    }
}
