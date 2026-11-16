package com.bmusic.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Test du script de politique DI `scripts/check-koin-bindings.py`.
 *
 * Le script detecte tout type que Koin doit resoudre mais qui n'a pas de
 * binding -> `NoDefinitionFoundException` -> crash. Il couvre trois chemins :
 * `get<T>()`, `by inject()` et les **parametres de constructeur** des classes
 * liees par Koin (c'est ce dernier chemin qui a laisse passer `Preferences`).
 *
 * Ce test verifie que :
 *  1. le script passe sur l'etat actuel du depot (exit 0) ;
 *  2. le script ECHOUE si un type non lie est introduit (exit != 0).
 */
class KoinBindingsScriptTest {

    private fun repoRoot(): File {
        val dir = File(System.getProperty("user.dir"))
        return if (File(dir, "scripts/check-koin-bindings.py").exists()) dir else File(dir, "../")
    }

    private fun runScript(workdir: File): Pair<Int, String> {
        val proc = ProcessBuilder("python3", "scripts/check-koin-bindings.py", "app/src/main/java")
            .directory(workdir)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        return proc.waitFor() to output
    }

    @Test
    fun `script passe sur le depot actuel`() {
        val root = repoRoot()
        assertTrue(
            "scripts/check-koin-bindings.py absent de $root",
            File(root, "scripts/check-koin-bindings.py").exists(),
        )
        val (code, output) = runScript(root)
        assertEquals("sortie: $output", 0, code)
    }

    @Test
    fun `script echoue si un type non lie est introduit`() {
        val root = repoRoot()
        val bad = File(root, "app/src/main/java/com/bmusic/app/ui/BadInjectedType.kt")
        bad.parentFile.mkdirs()
        bad.writeText(
            "package com.bmusic.app.ui\n" +
                "class BadInjectedType {\n" +
                "    val dao = org.koin.core.context.GlobalContext.get()\n" +
                "        .get<com.bmusic.app.ui.TypeJamaisLie>()\n" +
                "}\n",
        )
        try {
            val (code, output) = runScript(root)
            assertTrue(
                "le script doit echouer sur un type injecte non lie, sortie: $output",
                code != 0,
            )
            assertTrue(
                "le script doit nommer le type manquant : $output",
                output.contains("TypeJamaisLie"),
            )
        } finally {
            bad.delete()
        }
    }
}
