package id.harissabil.hayah.service

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.harissabil.hayah.data.ai.VerseRecommendationService
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.auth.AuthStateManager
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.dao.KeywordCacheDao
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import id.harissabil.hayah.data.db.entity.KeywordCacheEntity
import id.harissabil.hayah.data.model.CachedVerse
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Central coordinator that connects detection → AI → API → notification → journal.
 *
 * Flow:
 * 1. Receives keyword from ActivityTransitionReceiver or HayahAccessibilityService
 * 2. Checks daily dedup (one notification per keyword per day)
 * 3. Checks KeywordCache in Room
 *    - Hit: picks next verse by rotating lastShownIndex
 *    - Miss: calls Gemini AI → fetches verse data from Quran.com API → caches
 * 4. Shows notification via NotificationHelper
 * 5. Inserts JournalEntryEntity into Room
 */
class ReminderOrchestrator(
    private val context: Context,
    private val authStateManager: AuthStateManager,
    private val keywordCacheDao: KeywordCacheDao,
    private val journalEntryDao: JournalEntryDao,
    private val quranApiService: QuranApiService,
    private val verseRecommendationService: VerseRecommendationService,
    private val notificationHelper: NotificationHelper,
) {
    companion object {
        private const val TAG = "ReminderOrchestrator"
        val KEY_PLAY_AUDIO = booleanPreferencesKey("play_audio_instantly")
        val KEY_RECITER_ID = intPreferencesKey("reciter_id")
        private val KEY_MAX_REMINDERS = floatPreferencesKey("max_reminders")
        private const val DEFAULT_RECITER_ID = 7 // Mishary Rashid Alafasy
        private const val DEFAULT_MAX_REMINDERS = 5

        private const val REMINDER_COOLDOWN_MS = 10 * 60 * 1000L
        private const val AUDIO_CDN_BASE = "https://verses.quran.com/"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Surah name cache (loaded once)
    private var surahNames: Map<Int, String>? = null

    /**
     * Called by detection sources. Runs the full pipeline on IO dispatcher.
     */
    fun onKeywordDetected(keyword: String) {
        scope.launch {
            try {
                processKeyword(keyword.lowercase().trim())
            } catch (e: Exception) {
                Log.e(TAG, "Error processing keyword '$keyword'", e)
            }
        }
    }

    private suspend fun processKeyword(keyword: String) {
        val now = System.currentTimeMillis()
        val todayStart = todayStartMillis()
        val prefs = context.hayahSettingsDataStore.data.first()

        val maxDailyReminders = (prefs[KEY_MAX_REMINDERS] ?: DEFAULT_MAX_REMINDERS.toFloat())
            .toInt()
            .coerceAtLeast(1)

        val shownTodayTotal = journalEntryDao.countEntriesSince(todayStart)
        if (shownTodayTotal >= maxDailyReminders) {
            Log.d(TAG, "Daily max reached ($shownTodayTotal/$maxDailyReminders), skipping '$keyword'")
            return
        }

        val lastReminderAt = journalEntryDao.getLatestEntryTimestamp() ?: 0L
        if (lastReminderAt > 0L && (now - lastReminderAt) < REMINDER_COOLDOWN_MS) {
            Log.d(TAG, "Global cooldown active, skipping '$keyword'")
            return
        }

        // 1. Per-keyword daily dedup
        val shownToday = journalEntryDao.countEntriesForKeywordSince(keyword, todayStart)
        if (shownToday > 0) {
            Log.d(TAG, "Keyword '$keyword' already shown today, skipping")
            return
        }

        // 2. Check keyword cache
        val cache = keywordCacheDao.getByKeyword(keyword)
        val verses: List<CachedVerse>
        val nextIndex: Int

        if (cache != null) {
            // Cache hit — parse cached verses
            val type = object : TypeToken<List<CachedVerse>>() {}.type
            verses = gson.fromJson(cache.versesJson, type)
            nextIndex = (cache.lastShownIndex + 1) % verses.size
        } else {
            // Cache miss — generate new verses via AI + API
            verses = generateAndCacheVerses(keyword) ?: return
            nextIndex = 0
        }

        if (verses.isEmpty()) {
            Log.w(TAG, "No verses available for '$keyword'")
            return
        }

        val verse = verses[nextIndex]

        // 3. Update cache with new shown state
        keywordCacheDao.updateShownState(
            keyword = keyword,
            index = nextIndex,
            time = System.currentTimeMillis()
        )

        // 4. Read playAudio setting
        val playAudio = prefs[KEY_PLAY_AUDIO] ?: true

        // 5. Show notification
        notificationHelper.showVerseNotification(keyword, verse, playAudio)

        // 6. Insert journal entry
        val tagStyleOrdinal =
            (keyword.hashCode() and 0x7FFFFFFF) % 3 // cycle PRIMARY/SECONDARY/TERTIARY
        journalEntryDao.insert(
            JournalEntryEntity(
                surahVerse = "${verse.surahName}: ${verse.verseNumber}",
                verseKey = verse.verseKey,
                tag = keyword,
                tagStyleOrdinal = tagStyleOrdinal,
                reflection = verse.reflection,
                translation = verse.translation,
                audioUrl = verse.audioUrl,
                timestamp = now,
                isUnread = true,
            )
        )

        Log.d(TAG, "Reminder delivered: '$keyword' → ${verse.verseKey}")
    }

    /**
     * Full pipeline: AI → API → cache.
     */
    private suspend fun generateAndCacheVerses(keyword: String): List<CachedVerse>? {
        val authState = authStateManager.getAuthState()
        val accessToken = authState.accessToken ?: return null

        // Step 1: Ask Gemini for 5 verse keys
        val verseKeys = verseRecommendationService.recommendVerses(keyword)
        if (verseKeys.isEmpty()) {
            Log.w(TAG, "Gemini returned no verses for '$keyword'")
            return null
        }

        // Ensure surah names are loaded
        if (surahNames == null) {
            loadSurahNames()
        }

        // Read reciter preference
        val prefs = context.hayahSettingsDataStore.data.first()
        val reciterId = prefs[KEY_RECITER_ID] ?: DEFAULT_RECITER_ID

        // Step 2: Fetch verse data from Quran.com API
        val versesWithTranslations = mutableListOf<Triple<String, String, CachedVerse>>()

        for (verseKey in verseKeys) {
            try {
                val response = quranApiService.getVerseByKey(
                    accessToken = accessToken,
                    clientId = QuranOAuthConfig.clientId,
                    verseKey = verseKey
                )
                val detail = response.verse ?: continue

                val parsedVerseKey = parseVerseKey(verseKey)
                val chapterId = detail.chapterId ?: parsedVerseKey?.first ?: continue
                val verseNum = detail.verseNumber ?: parsedVerseKey?.second ?: continue
                val textUthmani = detail.textUthmani ?: ""
                val pageNumber = detail.pageNumber ?: 0
                val translation = detail.translations?.firstOrNull()?.text
                    ?.replace(Regex("<[^>]*>"), "") // strip HTML tags
                    ?: ""

                val surahName = surahNames?.get(chapterId) ?: "Surah $chapterId"

                // Fetch audio
                var audioUrl: String? = null
                try {
                    val audioResponse = quranApiService.getAudioForVerse(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                        recitationId = reciterId,
                        verseKey = verseKey
                    )
                    val rawUrl = audioResponse.audioFiles?.firstOrNull()?.url
                    audioUrl = if (rawUrl != null && !rawUrl.startsWith("http")) {
                        AUDIO_CDN_BASE + rawUrl
                    } else {
                        rawUrl
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio fetch failed for $verseKey", e)
                }

                val partial = CachedVerse(
                    verseKey = verseKey,
                    surahName = surahName,
                    verseNumber = verseNum,
                    textUthmani = textUthmani,
                    translation = translation,
                    reflection = "", // filled later
                    audioUrl = audioUrl,
                    pageNumber = pageNumber,
                )

                versesWithTranslations.add(Triple(verseKey, translation, partial))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch verse $verseKey", e)
            }
        }

        if (versesWithTranslations.isEmpty()) {
            Log.w(
                TAG,
                "No verse payloads could be materialized for '$keyword'. Likely missing chapter_id/verse_number in API payload."
            )
            return null
        }

        // Step 3: Generate reflections via AI
        val translationPairs = versesWithTranslations.map { (key, translation, _) ->
            key to translation
        }
        val reflections = verseRecommendationService.generateReflections(keyword, translationPairs)

        // Step 4: Merge reflections into verses
        val finalVerses = versesWithTranslations.map { (key, _, partial) ->
            partial.copy(
                reflection = reflections[key]
                    ?: "A reminder from the Quran about $keyword — reflect on this verse and its meaning in your daily life."
            )
        }

        // Step 5: Cache
        keywordCacheDao.insertOrUpdate(
            KeywordCacheEntity(
                keyword = keyword,
                versesJson = gson.toJson(finalVerses),
                lastShownIndex = -1, // will be incremented to 0 on first show
                lastShownTime = 0L,
            )
        )

        return finalVerses
    }

    private suspend fun loadSurahNames() {
        val authState = authStateManager.getAuthState()
        val accessToken = authState.accessToken ?: return

        try {
            val response = quranApiService.getChapters(
                accessToken = accessToken,
                clientId = QuranOAuthConfig.clientId,
            )
            surahNames = response.chapters?.associate { (it.id ?: 0) to (it.nameSimple ?: "") }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load surah names", e)
            surahNames = emptyMap()
        }
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun parseVerseKey(verseKey: String): Pair<Int, Int>? {
        val parts = verseKey.split(":")
        if (parts.size != 2) return null
        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        return chapter to verse
    }
}
