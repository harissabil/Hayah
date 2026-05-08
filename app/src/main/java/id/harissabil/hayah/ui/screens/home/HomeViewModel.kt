package id.harissabil.hayah.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.ai.VerseRecommendationService
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import id.harissabil.hayah.data.model.UserProfileResponse
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.NotificationHelper
import id.harissabil.hayah.service.ReminderOrchestrator
import id.harissabil.hayah.service.executeWithNetworkRetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class Period { TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME }

data class HomeUiState(
    val userName: String = "",
    val pagesRead: Int = 0,
    val selectedPeriod: Period = Period.THIS_WEEK,
    val profilePhotoUrl: String? = null,
    val isPagesReadLoading: Boolean = false,
    val pagesReadError: String? = null,
    val isInstantReflectionLoading: Boolean = false,
    val instantReflectionError: String? = null,
    val newlyGeneratedEntry: JournalEntryEntity? = null,
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val context: Context,
    private val journalEntryDao: JournalEntryDao,
    private val quranApiService: QuranApiService,
    private val verseRecommendationService: VerseRecommendationService,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HomeViewModel"
        private const val AUDIO_CDN_BASE = "https://verses.quran.com/"
        private const val INSTANT_REFLECTION_KEYWORD = "Instant Reflection"
        private const val OPTIONAL_API_MAX_ATTEMPTS = 2
    }

    private var readCountJob: Job? = null
    private var chapterNameCache: Map<Int, String>? = null

    init {
        viewModelScope.launch {
            authRepository.userProfile.collect { profile ->
                updateFromProfile(profile)
            }
        }
        fetchPagesRead(Period.THIS_WEEK)
    }

    private fun fetchPagesRead(period: Period) {
        readCountJob?.cancel()
        readCountJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isPagesReadLoading = true, pagesReadError = null) }
                try {
                    val accessToken =
                        authRepository.getValidAccessToken()
                            ?: throw Exception("Authentication required.")
                    val (from, to) = getDateRange(period)
                    var total = 0.0
                    var cursor: String? = null
                    var hasNext = true
                    while (hasNext) {
                        val response =
                            quranApiService.getActivityDays(
                                accessToken = accessToken,
                                clientId = QuranOAuthConfig.clientId,
                                timezone = TimeZone.getDefault().id,
                                from = from,
                                to = to,
                                after = cursor,
                            )
                        total += response.data.sumOf { it.pagesRead }
                        hasNext = response.pagination.hasNextPage
                        cursor = response.pagination.endCursor
                    }
                    _uiState.update { it.copy(pagesRead = total.toInt(), isPagesReadLoading = false) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch pages read", e)
                    _uiState.update {
                        it.copy(
                            isPagesReadLoading = false,
                            pagesReadError = e.localizedMessage ?: "Failed to load pages read",
                        )
                    }
                }
            }
    }

    private fun getDateRange(period: Period): Pair<String?, String?> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        return when (period) {
            Period.TODAY -> {
                val todayStr = fmt.format(today.time)
                todayStr to todayStr
            }
            Period.THIS_WEEK -> {
                val monday = today.clone() as Calendar
                monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (monday.after(today)) monday.add(Calendar.WEEK_OF_YEAR, -1)
                fmt.format(monday.time) to fmt.format(today.time)
            }
            Period.THIS_MONTH -> {
                val firstDay = today.clone() as Calendar
                firstDay.set(Calendar.DAY_OF_MONTH, 1)
                fmt.format(firstDay.time) to fmt.format(today.time)
            }
            Period.ALL_TIME -> null to null
        }
    }

    fun updateFromProfile(profile: UserProfileResponse?) {
        _uiState.update {
            if (profile == null) {
                it.copy(
                    userName = "",
                    profilePhotoUrl = null,
                )
            } else {
                it.copy(
                    userName = profile.firstName ?: profile.username ?: "",
                    profilePhotoUrl =
                        profile.avatarUrls?.medium
                            ?: profile.avatarUrls?.small,
                )
            }
        }
    }

    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period) }
        fetchPagesRead(period)
    }

    fun retryFetchPagesRead() {
        fetchPagesRead(_uiState.value.selectedPeriod)
    }

    fun dismissInstantReflectionError() {
        _uiState.update { it.copy(instantReflectionError = null) }
    }

    fun dismissNewlyGeneratedEntry() {
        _uiState.update { it.copy(newlyGeneratedEntry = null) }
        notificationHelper.stopVerseAudio()
    }

    fun generateInstantReflection() {
        if (_uiState.value.isInstantReflectionLoading) return
        _uiState.update { it.copy(isInstantReflectionLoading = true, instantReflectionError = null) }

        viewModelScope.launch {
            try {
                val accessToken = authRepository.getValidAccessToken()
                Log.d(TAG, "Access token: $accessToken")
                if (accessToken == null) {
                    _uiState.update { it.copy(isInstantReflectionLoading = false, instantReflectionError = "Authentication required.") }
                    return@launch
                }

                val keyword = INSTANT_REFLECTION_KEYWORD

                // 1. Fetch Random Verse
                val randomVerseResponse =
                    executeWithNetworkRetry {
                        quranApiService.getRandomVerse(
                            accessToken = accessToken,
                            clientId = QuranOAuthConfig.clientId,
                        )
                    }
                val detail = randomVerseResponse.verse
                if (detail == null || detail.verseKey == null) {
                    _uiState.update {
                        it.copy(
                            isInstantReflectionLoading = false,
                            instantReflectionError = "Failed to fetch random verse.",
                        )
                    }
                    return@launch
                }

                val verseKey = detail.verseKey
                val parsedVerseKey = parseVerseKey(verseKey)
                val chapterId = detail.chapterId ?: parsedVerseKey?.first ?: 0
                val verseNum = detail.verseNumber ?: parsedVerseKey?.second ?: 0
                val pageNumber = detail.pageNumber ?: 0
                val rawTranslation = detail.translations?.firstOrNull()?.text ?: ""
                val translation =
                    rawTranslation
                        .replace(Regex("<sup[^>]*>.*?</sup>"), "")
                        .replace(Regex("<[^>]*>"), "")

                val prefs = context.hayahSettingsDataStore.data.first()
                val reciterId = prefs[ReminderOrchestrator.KEY_RECITER_ID] ?: 7 // Mishary Rashid Alafasy by default

                val (surahName, audioUrl, reflectionText) =
                    coroutineScope {
                        val surahNameDeferred =
                            async {
                                resolveSurahName(accessToken, chapterId)
                            }

                        val audioUrlDeferred =
                            async {
                                fetchAudioUrl(
                                    accessToken = accessToken,
                                    reciterId = reciterId,
                                    verseKey = verseKey,
                                )
                            }

                        val reflectionDeferred =
                            async {
                                val tafsirText = fetchTafsir(accessToken, verseKey)
                                generateTimedReflection(
                                    keyword = keyword,
                                    verseKey = verseKey,
                                    translation = translation,
                                    tafsir = tafsirText?.let { mapOf(verseKey to it) },
                                )
                            }

                        Triple(
                            surahNameDeferred.await(),
                            audioUrlDeferred.await(),
                            reflectionDeferred.await(),
                        )
                    }

                // 4. Save to Room as Unread
                val tagStyleOrdinal = (keyword.hashCode() and 0x7FFFFFFF) % 3
                val latestTimestamp = journalEntryDao.getLatestEntryTimestamp() ?: 0L
                val newTimestamp = maxOf(System.currentTimeMillis(), latestTimestamp + 1L)

                val journalEntry =
                    JournalEntryEntity(
                        surahVerse = "$surahName: $verseNum",
                        verseKey = verseKey,
                        tag = keyword,
                        tagStyleOrdinal = tagStyleOrdinal,
                        reflection = reflectionText,
                        translation = translation,
                        audioUrl = audioUrl,
                        pageNumber = pageNumber,
                        timestamp = newTimestamp,
                        isUnread = true, // Marked as unread as per user instructions
                    )
                journalEntryDao.insert(journalEntry)

                val playAudio = prefs[ReminderOrchestrator.KEY_PLAY_AUDIO] ?: true
                if (playAudio && audioUrl != null) {
                    notificationHelper.playVerseAudio(audioUrl)
                }

                _uiState.update {
                    it.copy(
                        isInstantReflectionLoading = false,
                        newlyGeneratedEntry = journalEntry,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating instant reflection", e)
                _uiState.update {
                    it.copy(
                        isInstantReflectionLoading = false,
                        instantReflectionError = e.localizedMessage ?: "An unexpected error occurred",
                    )
                }
            }
        }
    }

    private suspend fun resolveSurahName(
        accessToken: String,
        chapterId: Int,
    ): String {
        val fallback = "Surah $chapterId"
        if (chapterId <= 0) return fallback

        val cached = chapterNameCache
        if (cached != null) {
            return cached[chapterId] ?: fallback
        }

        return try {
            val chaptersRes =
                executeWithNetworkRetry(maxAttempts = OPTIONAL_API_MAX_ATTEMPTS) {
                    quranApiService.getChapters(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                    )
                }
            val loadedCache =
                chaptersRes.chapters
                    .orEmpty()
                    .mapNotNull { chapter ->
                        val id = chapter.id
                        val name = chapter.nameSimple
                        if (id == null || name.isNullOrBlank()) null else id to name
                    }.toMap()

            chapterNameCache = loadedCache
            loadedCache[chapterId] ?: fallback
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch chapters", e)
            fallback
        }
    }

    private suspend fun fetchAudioUrl(
        accessToken: String,
        reciterId: Int,
        verseKey: String,
    ): String? =
        try {
            val audioResponse =
                executeWithNetworkRetry(maxAttempts = OPTIONAL_API_MAX_ATTEMPTS) {
                    quranApiService.getAudioForVerse(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                        recitationId = reciterId,
                        verseKey = verseKey,
                    )
                }
            val rawUrl = audioResponse.audioFiles?.firstOrNull()?.url
            if (rawUrl != null && !rawUrl.startsWith("http")) {
                AUDIO_CDN_BASE + rawUrl
            } else {
                rawUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio fetch failed for $verseKey", e)
            null
        }

    private suspend fun generateTimedReflection(
        keyword: String,
        verseKey: String,
        translation: String,
        tafsir: Map<String, String>? = null,
    ): String {
        val fallback =
            "A reminder from the Quran about $keyword — reflect on this verse and its meaning in your daily life."
        return try {
            val reflectionMap =
                verseRecommendationService.generateReflections(
                    keyword = keyword,
                    versesWithTranslations = listOf(verseKey to translation),
                    tafsir = tafsir,
                )
            reflectionMap[verseKey] ?: fallback
        } catch (e: Exception) {
            Log.w(TAG, "Reflection generation failed for $verseKey", e)
            fallback
        }
    }

    private suspend fun fetchTafsir(
        accessToken: String,
        verseKey: String,
        resourceId: Int = 169,
    ): String? =
        try {
            val response =
                executeWithNetworkRetry(maxAttempts = OPTIONAL_API_MAX_ATTEMPTS) {
                    quranApiService.getTafsirForAyah(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                        resourceId = resourceId,
                        ayahKey = verseKey,
                    )
                }
            response.tafsir
                ?.text
                ?.replace(Regex("<[^>]*>"), "")
                ?.take(1500)
        } catch (e: Exception) {
            Log.w(TAG, "Tafsir fetch failed for $verseKey", e)
            null
        }

    private fun parseVerseKey(verseKey: String): Pair<Int, Int>? {
        val parts = verseKey.split(":")
        if (parts.size != 2) return null
        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        return chapter to verse
    }
}
