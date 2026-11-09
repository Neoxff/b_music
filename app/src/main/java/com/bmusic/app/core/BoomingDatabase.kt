package com.bmusic.app.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bmusic.app.data.local.room.AutoEqDao
import com.bmusic.app.data.local.room.AutoEqEntity
import com.bmusic.app.data.local.room.HistoryDao
import com.bmusic.app.data.local.room.HistoryEntity
import com.bmusic.app.data.local.room.InclExclDao
import com.bmusic.app.data.local.room.InclExclEntity
import com.bmusic.app.data.local.room.LyricsDao
import com.bmusic.app.data.local.room.LyricsEntity
import com.bmusic.app.data.local.room.PlayCountDao
import com.bmusic.app.data.local.room.PlayCountEntity
import com.bmusic.app.data.local.room.PlaylistDao
import com.bmusic.app.data.local.room.PlaylistEntity
import com.bmusic.app.data.local.room.QueueDao
import com.bmusic.app.data.local.room.QueueEntity
import com.bmusic.app.data.local.room.SongEntity
import com.bmusic.app.downloader.data.DownloadDao
import com.bmusic.app.downloader.data.DownloadEntity
import com.bmusic.app.downloader.data.SearchHistoryDao
import com.bmusic.app.downloader.data.SearchHistoryEntity

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        QueueEntity::class,
        InclExclEntity::class,
        LyricsEntity::class,
        AutoEqEntity::class,
        DownloadEntity::class,
        SearchHistoryEntity::class
    ],
    version = BMusicDatabase.VERSION,
    exportSchema = true
)
abstract class BMusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun inclExclDao(): InclExclDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun autoEqDao(): AutoEqDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        /**
         * Indicates the current version of the database.
         *
         * When increasing this value, we must ensure we also make any necessary
         * changes in [com.bmusic.app.data.local.backup.BackupManager].
         */
        const val VERSION = 10

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN custom_cover_uri TEXT")
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN description TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `QueueEntity` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `CanvasEntity` (`id` INT NOT NULL, `canvas_url` TEXT NOT NULL, `fetch_time` INT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS CanvasEntity")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS LyricsEntity")
                db.execSQL("""
                    CREATE TABLE LyricsEntity (
                        id INTEGER PRIMARY KEY NOT NULL,
                        lyrics TEXT,
                        provider TEXT,
                        is_instrumental INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AutoEqEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `label` TEXT NOT NULL, 
                        `source` TEXT NOT NULL, 
                        `rig` TEXT NOT NULL, 
                        `form` TEXT NOT NULL, 
                        `model_name` TEXT NOT NULL, 
                        `path` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_AutoEqEntity_label` ON `AutoEqEntity` (`label`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_AutoEqEntity_source` ON `AutoEqEntity` (`source`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_AutoEqEntity_model_name` ON `AutoEqEntity` (`model_name`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE _new_LyricsEntity (
                        id INTEGER PRIMARY KEY NOT NULL,
                        lyrics TEXT,
                        provider TEXT,
                        is_instrumental INTEGER NOT NULL DEFAULT 0,
                        offset INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    INSERT INTO _new_LyricsEntity (id, lyrics, provider, is_instrumental, offset)
                    SELECT id, lyrics, provider, COALESCE(is_instrumental, 0), 0 FROM LyricsEntity
                """)

                db.execSQL("DROP TABLE LyricsEntity")
                db.execSQL("ALTER TABLE _new_LyricsEntity RENAME TO LyricsEntity")
            }
        }

        /** Étape 3 — migration ADDITIVE uniquement : crée `DownloadEntity`, ne touche aucune entité existante. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `DownloadEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `source_url` TEXT NOT NULL,
                        `platform_id` TEXT NOT NULL,
                        `dedup_hash` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `progress_bytes` INTEGER NOT NULL,
                        `total_bytes` INTEGER NOT NULL,
                        `final_path` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `error` TEXT,
                        `format` TEXT NOT NULL,
                        `stream_url` TEXT NOT NULL,
                        `is_video` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_DownloadEntity_dedup_hash` ON `DownloadEntity` (`dedup_hash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_DownloadEntity_state` ON `DownloadEntity` (`state`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_DownloadEntity_platform_id` ON `DownloadEntity` (`platform_id`)")
            }
        }

        /** Étape 6 — migration ADDITIVE uniquement : crée `SearchHistory`, ne touche aucune entité existante. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `SearchHistory` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `normalized_query` TEXT NOT NULL,
                        `date` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_SearchHistory_normalized_query` ON `SearchHistory` (`normalized_query`)")
            }
        }
    }
}