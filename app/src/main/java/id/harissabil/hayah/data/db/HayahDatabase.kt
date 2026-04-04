package id.harissabil.hayah.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.dao.KeywordCacheDao
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import id.harissabil.hayah.data.db.entity.KeywordCacheEntity

@Database(
    entities = [KeywordCacheEntity::class, JournalEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HayahDatabase : RoomDatabase() {
    abstract fun keywordCacheDao(): KeywordCacheDao
    abstract fun journalEntryDao(): JournalEntryDao
}
