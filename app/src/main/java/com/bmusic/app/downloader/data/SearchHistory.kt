package com.bmusic.app.downloader.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Étape 6 : historique local des recherches (max 10, dédoublonné).
 * Aucun envoi réseau.
 */
@Entity(
    tableName = "SearchHistory",
    indices = [androidx.room.Index(value = ["normalized_query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "normalized_query")
    val normalizedQuery: String,

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),
)

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM SearchHistory ORDER BY `date` DESC LIMIT 10")
    suspend fun recent(): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity): Long

    @Query("DELETE FROM SearchHistory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM SearchHistory")
    suspend fun clearAll()

    @Query("DELETE FROM SearchHistory WHERE id NOT IN (SELECT id FROM SearchHistory ORDER BY `date` DESC LIMIT 10)")
    suspend fun trimToMax10()
}
