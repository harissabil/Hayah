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
import id.harissabil.hayah.data.db.dao.ReadHistoryDao
import id.harissabil.hayah.data.db.entity.JournalEntryEntity
import id.harissabil.hayah.data.model.UserProfileResponse
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.NotificationHelper
import id.harissabil.hayah.service.ReminderOrchestrator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Period { THIS_WEEK, THIS_MONTH, ALL_TIME }

data class HomeUiState(
    val userName: String = "",
    val pagesRead: Int = 0,
    val selectedPeriod: Period = Period.THIS_WEEK,
    val profilePhotoUrl: String? = null,
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
    private val readHistoryDao: ReadHistoryDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HomeViewModel"
        private const val AUDIO_CDN_BASE = "https://verses.quran.com/"
    }

    private var readCountJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.userProfile.collect { profile ->
                updateFromProfile(profile)
            }
        }
        observePagesRead(Period.THIS_WEEK)
    }

    private fun observePagesRead(period: Period) {
        readCountJob?.cancel()
        readCountJob =
            viewModelScope.launch {
                val flow =
                    when (period) {
                        Period.ALL_TIME -> readHistoryDao.countAllEntries()
                        Period.THIS_WEEK -> {
                            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                            readHistoryDao.countEntriesSince(sevenDaysAgo)
                        }
                        Period.THIS_MONTH -> {
                            val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
                            readHistoryDao.countEntriesSince(thirtyDaysAgo)
                        }
                    }
                flow.collect { count ->
                    _uiState.update { it.copy(pagesRead = count) }
                }
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
        observePagesRead(period)
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
                if (accessToken == null) {
                    _uiState.update { it.copy(isInstantReflectionLoading = false, instantReflectionError = "Authentication required.") }
                    return@launch
                }

                val keyword = "Instant Reflection"

                // 1. Fetch Random Verse
                val randomVerseResponse =
                    quranApiService.getRandomVerse(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                    )
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
                val textUthmani = detail.textUthmani ?: ""
                val pageNumber = detail.pageNumber ?: 0
                val rawTranslation = detail.translations?.firstOrNull()?.text ?: ""
                val translation =
                    rawTranslation
                        .replace(Regex("<sup[^>]*>.*?</sup>"), "")
                        .replace(Regex("<[^>]*>"), "")

                // Fetch Chapters for surah name
                var surahName = "Surah $chapterId"
                try {
                    val chaptersRes =
                        quranApiService.getChapters(
                            accessToken = accessToken,
                            clientId = QuranOAuthConfig.clientId,
                        )
                    surahName = chaptersRes.chapters?.find { it.id == chapterId }?.nameSimple ?: surahName
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch chapters", e)
                }

                // 2. Fetch Audio from preferred reciter
                val prefs = context.hayahSettingsDataStore.data.first()
                val reciterId = prefs[ReminderOrchestrator.KEY_RECITER_ID] ?: 7 // Mishary Rashid Alafasy by default

                var audioUrl: String? = null
                try {
                    val audioResponse =
                        quranApiService.getAudioForVerse(
                            accessToken = accessToken,
                            clientId = QuranOAuthConfig.clientId,
                            recitationId = reciterId,
                            verseKey = verseKey,
                        )
                    val rawUrl = audioResponse.audioFiles?.firstOrNull()?.url
                    audioUrl =
                        if (rawUrl != null && !rawUrl.startsWith("http")) {
                            AUDIO_CDN_BASE + rawUrl
                        } else {
                            rawUrl
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio fetch failed for $verseKey", e)
                }

                // 3. Generate AI Reflection
                val reflectionMap =
                    verseRecommendationService.generateReflections(
                        keyword = keyword,
                        versesWithTranslations = listOf(verseKey to translation),
                    )
                val reflectionText =
                    reflectionMap[verseKey]
                        ?: "A reminder from the Quran about $keyword — reflect on this verse and its meaning in your daily life."

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

    private fun parseVerseKey(verseKey: String): Pair<Int, Int>? {
        val parts = verseKey.split(":")
        if (parts.size != 2) return null
        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        return chapter to verse
    }
}
