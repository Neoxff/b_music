package com.bmusic.app.security

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Audit v1.1 F1 (HAUTE) : allowBackup=true sans exclude des tables sensibles
 * DownloadEntity/SearchHistory (incluses dans music_database.db).
 * Correctif : AndroidManifest.xml:38 allowBackup="false".
 * Ce test échoue avant (allowBackup="true") et passe après ("false").
 */
class AllowBackupTest {

    @Test
    fun `manifeste desactive allowBackup pour proteger les tables sensibles`() {
        val manifest = File("src/main/AndroidManifest.xml")
        val candidates = listOf(
            manifest,
            File("app/src/main/AndroidManifest.xml"),
        )
        val file = candidates.first { it.exists() }
        val content = file.readText()
        assertFalse("allowBackup doit être false (audit F1)", content.contains("android:allowBackup=\"true\""))
        org.junit.Assert.assertTrue("allowBackup doit être false (audit F1)", content.contains("android:allowBackup=\"false\""))
    }
}
