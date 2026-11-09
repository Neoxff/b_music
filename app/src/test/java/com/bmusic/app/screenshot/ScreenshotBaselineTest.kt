package com.bmusic.app.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.bmusic.app.ui.theme.BMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Captures de référence v1.1.x (sans appareil) — étape 0.
 * Génère les PNG dans docs/baseline/captures/ en clair et sombre.
 * Composables minimalistes inspirés des surfaces (opaque, sans flou) :
 * mini-player, barre de navigation, lecteur plein écran, liste d'albums,
 * Réglages, feuille d'aperçu.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class ScreenshotBaselineTest {

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

    private val surfaces = listOf(
        "mini-player",
        "barre-navigation",
        "lecteur-plein-ecran",
        "liste-albums",
        "reglages",
        "feuille-apercu",
    )

    @Test
    fun capture_baselines_clair_et_sombre() {
        val outDir = resolveCapturesDir()
        outDir.mkdirs()
        val surfaceState = mutableStateOf(surfaces.first())
        val darkState = mutableStateOf(false)

        // setContent unique (re-setContent interdit par ComposeTestRule)
        composeRule.setContent {
            BMusicTheme(darkTheme = darkState.value, blackTheme = false, dynamicColor = false, customFont = false) {
                BaselineSurface(surface = surfaceState.value, dark = darkState.value)
            }
        }

        for (surface in surfaces) {
            for (dark in listOf(false, true)) {
                composeRule.runOnIdle {
                    surfaceState.value = surface
                    darkState.value = dark
                }
                composeRule.waitForIdle()
                val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                val suffix = if (dark) "dark" else "light"
                val file = File(outDir, "$surface-$suffix.png")
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

    @androidx.compose.runtime.Composable
    private fun BaselineSurface(surface: String, dark: Boolean) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (dark) androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(surface, style = MaterialTheme.typography.titleLarge)
                Text(if (dark) "clair/sombre : dark" else "clair/sombre : light")
                Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("contenu $surface")
                }
                Button(onClick = {}) { Text("Bouton") }
            }
        }
    }
}
