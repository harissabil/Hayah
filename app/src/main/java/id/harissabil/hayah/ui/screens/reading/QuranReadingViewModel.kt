package id.harissabil.hayah.ui.screens.reading

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.auth.AuthStateManager
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.dao.ReadHistoryDao
import id.harissabil.hayah.data.db.entity.ReadHistoryEntity
import id.harissabil.hayah.data.model.ActivityDayRequest
import id.harissabil.hayah.data.model.VerseDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class QuranReadingUiState(
    val pageNumber: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val verses: List<VerseDetail> = emptyList(),
    val chapterName: String = "",
    val highlightedVerseKey: String? = null,
    val readingSeconds: Int = 0,
    val isPosting: Boolean = false,
    val postSuccess: Boolean = false,
    val hasReachedBottom: Boolean = false,
)

class QuranReadingViewModel(
    savedStateHandle: SavedStateHandle,
    private val quranApiService: QuranApiService,
    private val authStateManager: AuthStateManager,
    private val readHistoryDao: ReadHistoryDao,
    private val journalEntryDao: JournalEntryDao,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])
    private val pageNumber: Int = checkNotNull(savedStateHandle["pageNumber"])
    private val highlightedVerseKey: String? = savedStateHandle.get<String>("highlightedVerseKey")

    private val _uiState = MutableStateFlow(
        QuranReadingUiState(
            pageNumber = pageNumber,
            highlightedVerseKey = highlightedVerseKey
        )
    )
    val uiState: StateFlow<QuranReadingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadPageData()
    }

    private fun loadPageData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val authState = authStateManager.getAuthState()
                val token = authState.accessToken ?: throw Exception("Not authenticated")

                val response = quranApiService.getVersesByPage(
                    accessToken = token,
                    clientId = QuranOAuthConfig.clientId,
                    pageNumber = pageNumber
                )

                // For chapter name, we will try to extract from the first and last verse of the page. If they belong to different chapters, we will show both.
                val versesList = response.verses ?: emptyList()
                var finalChapterTitle = "Unknown Surah"

                if (versesList.isNotEmpty()) {
                    val firstVerse = versesList.first()
                    val lastVerse = versesList.last()

                    // Fungsi helper untuk mengekstrak chapterId (termasuk fallback verseKey)
                    fun getChapterIdSafe(verse: VerseDetail): Int {
                        // Asumsi verseKey formatnya "chapterId:verseNumber" misal "2:255"
                        return verse.chapterId ?: verse.verseKey?.substringBefore(":")
                            ?.toIntOrNull() ?: 0
                    }

                    val firstChapterId = getChapterIdSafe(firstVerse)
                    val lastChapterId = getChapterIdSafe(lastVerse)

                    try {
                        val chaptersResponse = quranApiService.getChapters(
                            accessToken = token,
                            clientId = QuranOAuthConfig.clientId
                        )
                        val chapters = chaptersResponse.chapters ?: emptyList()

                        val firstChapterName = chapters.find { it.id == firstChapterId }?.nameSimple
                            ?: "Surah $firstChapterId"
                        val lastChapterName = chapters.find { it.id == lastChapterId }?.nameSimple
                            ?: "Surah $lastChapterId"

                        // Jika chapter pertama dan terakhir berbeda, gabungkan. Jika sama, tampilkan satu saja.
                        finalChapterTitle =
                            if (firstChapterId != 0 && lastChapterId != 0 && firstChapterId != lastChapterId) {
                                "$firstChapterName - $lastChapterName"
                            } else {
                                firstChapterName
                            }
                    } catch (e: Exception) {
                        Log.e("QuranReadingVM", "Failed to fetch chapters", e)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verses = response.verses ?: emptyList(),
                        chapterName = finalChapterTitle
                    )
                }
                startTimer()
            } catch (e: Exception) {
                Log.e("QuranReadingVM", "Load failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(readingSeconds = it.readingSeconds + 1) }
            }
        }
    }

    fun onBottomReached() {
        if (_uiState.value.hasReachedBottom || _uiState.value.isLoading || _uiState.value.verses.isEmpty()) return

        _uiState.update { it.copy(hasReachedBottom = true, isPosting = true) }
        timerJob?.cancel()

        viewModelScope.launch {
            try {
                val state = _uiState.value

                val firstVerse = state.verses.first().verseKey ?: ""
                val lastVerse = state.verses.last().verseKey ?: ""

                val authState = authStateManager.getAuthState()
                val token = authState.accessToken ?: throw Exception("Not authenticated")

                // Insert into local DB
                readHistoryDao.insert(
                    ReadHistoryEntity(
                        pageNumber = pageNumber,
                        timestamp = System.currentTimeMillis(),
                        totalTimeReadSeconds = state.readingSeconds
                    )
                )

                journalEntryDao.markAsRead(
                    id = entryId,
                )

                // Post to API
                val currentTimeZone = TimeZone.getDefault().id
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = dateFormat.format(Date())

                quranApiService.postActivityDays(
                    accessToken = token,
                    clientId = QuranOAuthConfig.clientId,
                    timezone = currentTimeZone,
                    request = ActivityDayRequest(
                        date = dateStr,
                        type = "QURAN",
                        seconds = state.readingSeconds,
                        ranges = listOf("$firstVerse-$lastVerse")
                    )
                )

                _uiState.update { it.copy(isPosting = false, postSuccess = true) }

            } catch (e: Exception) {
                Log.e("QuranReadingVM", "Failed to post activity", e)
                // Even if API fails, we already inserted locally. We show an error slightly but don't unset hasReachedBottom.
                _uiState.update {
                    it.copy(
                        isPosting = false,
                        postSuccess = false,
                        error = "Failed to synchronize progress to Quran.com."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
