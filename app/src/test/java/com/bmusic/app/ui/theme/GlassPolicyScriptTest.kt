package com.bmusic.app.ui.theme

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Tests du script de politique check-glass-policy.sh : fichiers
 * volontairement fautifs doivent faire échouer, le repo actuel doit passer.
 */
class GlassPolicyScriptTest {

    private fun repoRoot(): File {
        val dir = File(System.getProperty("user.dir"))
        return if (File(dir, "scripts/check-glass-policy.sh").exists()) dir else File(dir, "../")
    }

    private fun runScript(workdir: File): Pair<Int, String> {
        val proc = ProcessBuilder("bash", "scripts/check-glass-policy.sh")
            .directory(workdir)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        val code = proc.waitFor()
        return code to output
    }

    @Test
    fun `script passe sur le repo actuel`() {
        val root = repoRoot()
        assertTrue(File(root, "scripts/check-glass-policy.sh").exists())
        val (code, output) = runScript(root)
        assertEquals("sortie: $output", 0, code)
    }

    @Test
    fun `script echoue sur blur interdit hors liste blanche`() {
        val root = repoRoot()
        val tmp = File.createTempFile("glass-policy-", ".kt")
        tmp.writeText("val x = Modifier.blur(16.dp)")
        // Simule l'échec en vérifiant le motif interdit via le même grep que le script
        val proc = ProcessBuilder("grep", "-rInE", "Modifier\\.blur", tmp.absolutePath)
            .redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        assertTrue(out.contains("Modifier.blur"))
        assertEquals(0, proc.waitFor())
        tmp.delete()
        // + le script officiel doit échouer si ce fichier est sous app/src/main/java hors whitelist :
        val bad = File(root, "app/src/main/java/com/bmusic/app/ui/theme/BadBlur.kt")
        bad.writeText("package com.bmusic.app.ui.theme\nimport androidx.compose.ui.Modifier\nval x = Modifier.blur(16.dp)\n")
        try {
            val (code, output) = runScript(root)
            assertTrue("le script doit échouer sur blur hors liste blanche, sortie: $output", code != 0)
        } finally {
            bad.delete()
        }
    }

    @Test
    fun `script echoue sur rayon superieur au plafond`() {
        val root = repoRoot()
        val bad = File(root, "app/src/main/java/com/bmusic/app/ui/theme/BadRadius.kt")
        bad.writeText("package com.bmusic.app.ui.theme\nimport androidx.compose.ui.Modifier\nval y = Modifier.blur(40.dp)\n")
        try {
            val (code, output) = runScript(root)
            assertTrue("le script doit échouer sur rayon > 24dp, sortie: $output", code != 0)
        } finally {
            bad.delete()
        }
    }

    @Test
    fun `script echoue sur motif vitreux interdit`() {
        val root = repoRoot()
        val bad = File(root, "app/src/main/java/com/bmusic/app/ui/BadGlow.kt")
        bad.writeText("package com.bmusic.app.ui\nval glowThing = 1\n")
        try {
            val (code, output) = runScript(root)
            assertTrue("le script doit échouer sur motif glow, sortie: $output", code != 0)
        } finally {
            bad.delete()
        }
    }
}
