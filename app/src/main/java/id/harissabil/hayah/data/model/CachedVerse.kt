package id.harissabil.hayah.data.model

/**
 * A fully-resolved verse with all data needed for notification + journal storage.
 * Serialized to JSON and stored in [KeywordCacheEntity.versesJson].
 */
data class CachedVerse(
    val verseKey: String, // "2:156"
    val surahName: String, // "Al-Baqarah"
    val verseNumber: Int, // 156
    val textUthmani: String, // Arabic text
    val translation: String, // English translation (Sahih International)
    val reflection: String, // AI-generated reflection
    val audioUrl: String?, // Full audio file URL
    val pageNumber: Int = 0, // Mushaf page number
)
