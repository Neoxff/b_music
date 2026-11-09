package com.bmusic.app.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.bmusic.app.core.BMusicDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Non-regression (step 0 baseline) + étape 3 : data written under schema v8
 * survives a close/reopen cycle; la migration 8 → 9 est additive
 * (crée `DownloadEntity` sans toucher l'existant).
 */
@RunWith(AndroidJUnit4::class)
class BMusicDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbFile = File(context.filesDir, "migration-baseline-test.db")
    private lateinit var db: BMusicDatabase

    @Before
    fun setUp() {
        dbFile.delete()
    }

    @After
    fun tearDown() {
        if (this::db.isInitialized) db.close()
        dbFile.delete()
    }

    private fun openDb(): BMusicDatabase =
        Room.databaseBuilder(context, BMusicDatabase::class.java, dbFile.absolutePath).build()

    private fun count(db: BMusicDatabase, table: String): Long {
        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table")
        cursor.moveToFirst()
        val count = cursor.getLong(0)
        cursor.close()
        return count
    }

    @Test
    fun v8Data_survivesCloseAndReopen() {
        // Seed v8 data (playlists, queue) through the same tables Room uses.
        db = openDb()
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO PlaylistEntity (playlist_name) VALUES ('Baseline playlist')")
        sql.execSQL("INSERT INTO QueueEntity (id, order) VALUES ('1', 0)")
        sql.execSQL("INSERT INTO QueueEntity (id, order) VALUES ('2', 1)")
        db.close()

        // Reopen: v8 data must be intact.
        db = openDb()
        assertEquals(1L, count(db, "PlaylistEntity"))
        assertEquals(2L, count(db, "QueueEntity"))

        val cursor = db.openHelper.readableDatabase.query(
            "SELECT playlist_name FROM PlaylistEntity"
        )
        cursor.moveToFirst()
        assertEquals("Baseline playlist", cursor.getString(0))
        cursor.close()
    }

    @Test
    fun v8Data_migratesToV9_withoutLoss() {
        // Base remplie lue comme v8 (DownloadEntity retirée, user_version 8),
        // puis ouverte en v9 avec la migration additive 8→9.
        db = openDb()
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO PlaylistEntity (playlist_name) VALUES ('v8 playlist')")
        sql.execSQL("INSERT INTO QueueEntity (id, order) VALUES ('9', 0)")
        db.close()

        val raw = SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE
        )
        raw.execSQL("DROP TABLE IF EXISTS `DownloadEntity`")
        raw.version = 8
        raw.close()

        db = Room.databaseBuilder(context, BMusicDatabase::class.java, dbFile.absolutePath)
            .addMigrations(BMusicDatabase.MIGRATION_8_9)
            .build()
        assertEquals(1L, count(db, "PlaylistEntity"))
        assertEquals(1L, count(db, "QueueEntity"))
        assertEquals(0L, count(db, "DownloadEntity"))
    }
}
