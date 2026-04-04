package id.harissabil.hayah.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists every notification-delivered verse for display in the Journal screen.
 */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "surah_verse")
    val surahVerse: String,          // e.g. "Al-Baqarah: 153"

    @ColumnInfo(name = "verse_key")
    val verseKey: String,            // e.g. "2:153"

    val tag: String,                 // keyword that triggered this entry

    @ColumnInfo(name = "tag_style")
    val tagStyleOrdinal: Int,        // TagStyle.ordinal

    val reflection: String,

    val translation: String,

    @ColumnInfo(name = "audio_url")
    val audioUrl: String? = null,

    val timestamp: Long,             // epoch millis

    @ColumnInfo(name = "is_unread")
    val isUnread: Boolean = true,
)
