package id.harissabil.hayah.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Insert
    suspend fun insert(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET is_unread = 0 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("SELECT COUNT(*) FROM journal_entries WHERE tag = :keyword AND timestamp > :sinceTime")
    suspend fun countEntriesForKeywordSince(keyword: String, sinceTime: Long): Int

    @Query("SELECT COUNT(*) FROM journal_entries WHERE timestamp > :sinceTime")
    suspend fun countEntriesSince(sinceTime: Long): Int

    @Query("SELECT MAX(timestamp) FROM journal_entries")
    suspend fun getLatestEntryTimestamp(): Long?
}
