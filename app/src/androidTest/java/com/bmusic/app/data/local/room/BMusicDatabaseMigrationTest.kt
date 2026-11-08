package com.bmusic.app.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.bmusic.app.core.BMusicDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Non-regression (step 0 baseline): data written under schema v8 survives a
 * close/reopen cycle. The migration 8 → 9 (downloader) must reuse this test:
 * create a v8 file with data via SQL, open it with the v9 Room builder and
 * assert playlists/history/play counts/queue are intact.
 *
 * NOTE: Room schema export is disabled in production code
 * (exportSchema = false in BMusicDatabase), so table SQL below mirrors the
 * v8 entities; when migration 8 → 9 lands, replace [openV8WithData] by a
 * MigrationTestHelper run against the exported v8 schema.
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
}
