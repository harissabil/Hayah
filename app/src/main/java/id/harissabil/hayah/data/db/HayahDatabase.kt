package id.harissabil.hayah.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.dao.KeywordCacheDao
import id.harissabil.hayah.data.db.dao.ReadHistoryDao
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import id.harissabil.hayah.data.db.entity.KeywordCacheEntity
import id.harissabil.hayah.data.db.entity.ReadHistoryEntity

@Database(
    entities = [KeywordCacheEntity::class, JournalEntryEntity::class, ReadHistoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class HayahDatabase : RoomDatabase() {
    abstract fun keywordCacheDao(): KeywordCacheDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun readHistoryDao(): ReadHistoryDao
}
