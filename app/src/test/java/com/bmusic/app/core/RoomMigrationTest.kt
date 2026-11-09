package com.bmusic.app.core

import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Migration Room v8 -> état actuel (v10) exécuté en JVM (Robolectric).
 * Étape 0 — sans appareil. Vérifie que les migrations additives 8→9→10
 * créent les nouvelles tables sans détruire les données v8.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class RoomMigrationTest {

    @Test
    fun `migration 8 vers 10 cree DownloadEntity et SearchHistory et conserve les donnees`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-test.db"
        val path = File(context.cacheDir, dbName)
        path.delete()

        // Phase 1 : crée la v8
        val v8Config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE PlaylistEntity (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT)")
                        db.execSQL("INSERT INTO PlaylistEntity (name) VALUES ('v8-row')")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }
            )
            .build()
        val v8Helper = FrameworkSQLiteOpenHelperFactory().create(v8Config)
        v8Helper.writableDatabase.close()
        v8Helper.close()

        // Phase 2 : rouvre en v10 via onUpgrade -> migrations additives
        val v10Config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(
                object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(8, oldVersion)
                        assertEquals(10, newVersion)
                        BMusicDatabase.MIGRATION_8_9.migrate(db)
                        BMusicDatabase.MIGRATION_9_10.migrate(db)
                    }
                }
            )
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(v10Config)
        val db = helper.writableDatabase
        assertEquals(10, db.version)

        // Donnée v8 conservée
        val cursor = db.query("SELECT name FROM PlaylistEntity WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("v8-row", cursor.getString(0))
        cursor.close()

        // Nouvelles tables
        assertTableExists(db, "DownloadEntity")
        assertTableExists(db, "SearchHistory")
        val dCursor = db.query("SELECT COUNT(*) FROM DownloadEntity")
        assertTrue(dCursor.moveToFirst())
        assertEquals(0L, dCursor.getLong(0))
        dCursor.close()

        db.close()
        helper.close()
    }

    private fun assertTableExists(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String) {
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
        assertTrue("table $table absente", cursor.moveToFirst())
        cursor.close()
    }
}
