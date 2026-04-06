package id.harissabil.hayah.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches Gemini AI verse recommendations per keyword so we don't re-query
 * the AI for the same keyword within a reasonable timeframe.
 *
 * [versesJson] stores a JSON array of [id.harissabil.hayah.data.model.CachedVerse] objects.
 */
@Entity(tableName = "keyword_cache")
data class KeywordCacheEntity(
    @PrimaryKey
    val keyword: String,
    @ColumnInfo(name = "verses_json")
    val versesJson: String,
    @ColumnInfo(name = "last_shown_index")
    val lastShownIndex: Int = 0,
    @ColumnInfo(name = "last_shown_time")
    val lastShownTime: Long = 0L, // epoch millis
)
