package id.harissabil.hayah.ui.screens.reading

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import id.harissabil.hayah.data.db.dao.ReadHistoryDao
import id.harissabil.hayah.data.db.entity.ReadHistoryEntity
import id.harissabil.hayah.data.model.ActivityDayRequest
import id.harissabil.hayah.data.model.VerseDetail
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.executeWithNetworkRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AudioPlaybackState(
    val audioUrls: Map<String, String> = emptyMap(),
    val isLoadingUrls: Boolean = false,
    val currentVerseKey: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null,
)

data class QuranReadingUiState(
    val pageNumber: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val verses: List<VerseDetail> = emptyList(),
    val chapterName: String = "",
    val chapterNames: Map<Int, String> = emptyMap(),
    val highlightedVerseKey: String? = null,
    val readingSeconds: Int = 0,
    val isPosting: Boolean = false,
    val postSuccess: Boolean = false,
    val hasReachedBottom: Boolean = false,
    val audioState: AudioPlaybackState = AudioPlaybackState(),
)

class QuranReadingViewModel(
    savedStateHandle: SavedStateHandle,
    private val quranApiService: QuranApiService,
    private val authRepository: AuthRepository,
    private val readHistoryDao: ReadHistoryDao,
    private val journalEntryDao: JournalEntryDao,
    private val context: Context,
) : ViewModel() {
    companion object {
        private const val TAG = "QuranReadingVM"
        private const val POST_ACTIVITY_TIMEOUT_MS = 8_000L
        private const val POST_ACTIVITY_MAX_ATTEMPTS = 3
        private const val AUDIO_CDN_BASE = "https://verses.quran.com/"
        private const val PROGRESS_POLL_MS = 500L
        private const val AUDIO_FETCH_CONCURRENCY = 3
        private val KEY_RECITER_ID = intPreferencesKey("reciter_id")
        private const val DEFAULT_RECITER_ID = 7
    }

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])
    private val pageNumber: Int = checkNotNull(savedStateHandle["pageNumber"])
    private val highlightedVerseKey: String? = savedStateHandle.get<String>("highlightedVerseKey")

    private val _uiState =
        MutableStateFlow(
            QuranReadingUiState(
                pageNumber = pageNumber,
                highlightedVerseKey = highlightedVerseKey,
            ),
        )
    val uiState: StateFlow<QuranReadingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var progressJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadPageData()
    }

    private fun loadPageData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val token =
                    authRepository.getValidAccessToken() ?: throw Exception("Not authenticated")

                val response =
                    executeWithNetworkRetry {
                        quranApiService.getVersesByPage(
                            accessToken = token,
                            clientId = QuranOAuthConfig.clientId,
                            pageNumber = pageNumber,
                        )
                    }

                val versesList = response.verses ?: emptyList()
                var finalChapterTitle = "Unknown Surah"
                var chapterNamesMap: Map<Int, String> = emptyMap()

                if (versesList.isNotEmpty()) {
                    val firstVerse = versesList.first()
                    val lastVerse = versesList.last()

                    fun getChapterIdSafe(verse: VerseDetail): Int =
                        verse.chapterId ?: verse.verseKey
                            ?.substringBefore(":")
                            ?.toIntOrNull() ?: 0

                    val firstChapterId = getChapterIdSafe(firstVerse)
                    val lastChapterId = getChapterIdSafe(lastVerse)

                    try {
                        val chaptersResponse =
                            executeWithNetworkRetry {
                                quranApiService.getChapters(
                                    accessToken = token,
                                    clientId = QuranOAuthConfig.clientId,
                                )
                            }
                        val chapters = chaptersResponse.chapters ?: emptyList()

                        chapterNamesMap = chapters.associate { (it.id ?: 0) to (it.nameSimple ?: "") }

                        val firstChapterName =
                            chapterNamesMap[firstChapterId] ?: "Surah $firstChapterId"
                        val lastChapterName =
                            chapterNamesMap[lastChapterId] ?: "Surah $lastChapterId"

                        finalChapterTitle =
                            if (firstChapterId != 0 && lastChapterId != 0 && firstChapterId != lastChapterId) {
                                "$firstChapterName - $lastChapterName"
                            } else {
                                firstChapterName
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch chapters", e)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verses = versesList,
                        chapterName = finalChapterTitle,
                        chapterNames = chapterNamesMap,
                    )
                }
                startTimer()
                fetchAudioUrls(versesList)
            } catch (e: Exception) {
                Log.e(TAG, "Load failed", e)
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun fetchAudioUrls(verses: List<VerseDetail>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(audioState = it.audioState.copy(isLoadingUrls = true)) }

            val token = authRepository.getValidAccessToken()
            if (token == null) {
                _uiState.update { it.copy(audioState = it.audioState.copy(isLoadingUrls = false)) }
                return@launch
            }

            val prefs = context.hayahSettingsDataStore.data.first()
            val reciterId = prefs[KEY_RECITER_ID] ?: DEFAULT_RECITER_ID

            val semaphore = Semaphore(AUDIO_FETCH_CONCURRENCY)
            val results =
                verses
                    .filter { it.verseKey != null }
                    .map { verse ->
                        async {
                            semaphore.withPermit {
                                runCatching {
                                    val response =
                                        executeWithNetworkRetry {
                                            quranApiService.getAudioForVerse(
                                                accessToken = token,
                                                clientId = QuranOAuthConfig.clientId,
                                                recitationId = reciterId,
                                                verseKey = verse.verseKey!!,
                                            )
                                        }
                                    val rawUrl = response.audioFiles?.firstOrNull()?.url
                                    if (rawUrl != null) {
                                        val fullUrl =
                                            if (rawUrl.startsWith("http")) rawUrl else AUDIO_CDN_BASE + rawUrl
                                        verse.verseKey!! to fullUrl
                                    } else {
                                        null
                                    }
                                }.getOrElse { e ->
                                    Log.w(TAG, "Audio fetch failed for ${verse.verseKey}", e)
                                    null
                                }
                            }
                        }
                    }.awaitAll()

            val audioUrls = results.filterNotNull().toMap()

            _uiState.update {
                it.copy(audioState = it.audioState.copy(audioUrls = audioUrls, isLoadingUrls = false))
            }

            // If the user tapped a verse before URLs finished loading, auto-start it now.
            val pending = _uiState.value.audioState
            if (pending.currentVerseKey != null && pending.isBuffering && !pending.isPlaying) {
                val pendingKey = pending.currentVerseKey
                if (audioUrls.containsKey(pendingKey)) {
                    playVerse(pendingKey)
                }
            }
        }
    }

    // ── Playback controls ─────────────────────────

    fun onVersePlayRequested(verseKey: String) {
        val audio = _uiState.value.audioState
        if (audio.currentVerseKey == verseKey) {
            onTogglePlayPause()
            return
        }
        // URL not ready yet but fetch is in progress — mark as pending so the UI shows buffering.
        if (audio.audioUrls[verseKey] == null && audio.isLoadingUrls) {
            _uiState.update {
                it.copy(
                    audioState =
                        it.audioState.copy(
                            currentVerseKey = verseKey,
                            isBuffering = true,
                            isPlaying = false,
                            error = null,
                        ),
                )
            }
            return
        }
        playVerse(verseKey)
    }

    private fun playVerse(verseKey: String) {
        val url = _uiState.value.audioState.audioUrls[verseKey]
        if (url == null) {
            Log.w(TAG, "No audio URL for $verseKey")
            _uiState.update {
                it.copy(audioState = it.audioState.copy(error = "Audio not available for this verse"))
            }
            return
        }

        releaseMediaPlayer()

        _uiState.update {
            it.copy(
                audioState =
                    it.audioState.copy(
                        currentVerseKey = verseKey,
                        isBuffering = true,
                        isPlaying = false,
                        currentPositionMs = 0,
                        durationMs = 0,
                        error = null,
                    ),
            )
        }

        val player = MediaPlayer()
        mediaPlayer = player

        try {
            player.setDataSource(url)
            player.setOnPreparedListener { mp ->
                mp.start()
                _uiState.update {
                    it.copy(
                        audioState =
                            it.audioState.copy(
                                isPlaying = true,
                                isBuffering = false,
                                durationMs = mp.duration,
                            ),
                    )
                }
                startProgressPolling()
            }
            player.setOnCompletionListener {
                _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = false)) }
                progressJob?.cancel()
                playNextVerse()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                _uiState.update {
                    it.copy(
                        audioState =
                            it.audioState.copy(
                                isPlaying = false,
                                isBuffering = false,
                                error = "Playback error",
                            ),
                    )
                }
                progressJob?.cancel()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set data source for $verseKey", e)
            _uiState.update {
                it.copy(
                    audioState =
                        it.audioState.copy(
                            isBuffering = false,
                            error = "Failed to load audio",
                        ),
                )
            }
            releaseMediaPlayer()
        }
    }

    private fun playNextVerse() {
        val state = _uiState.value
        val currentKey = state.audioState.currentVerseKey ?: return
        val verses = state.verses
        val currentIndex = verses.indexOfFirst { it.verseKey == currentKey }

        if (currentIndex >= 0 && currentIndex < verses.size - 1) {
            val nextKey = verses[currentIndex + 1].verseKey ?: return
            playVerse(nextKey)
        } else {
            onStopPlayback()
        }
    }

    fun onTogglePlayPause() {
        val player = mediaPlayer ?: return
        val isPlaying = _uiState.value.audioState.isPlaying

        if (isPlaying) {
            try {
                player.pause()
                progressJob?.cancel()
                _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = false)) }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Pause failed", e)
            }
        } else {
            try {
                player.start()
                _uiState.update { it.copy(audioState = it.audioState.copy(isPlaying = true)) }
                startProgressPolling()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Resume failed", e)
            }
        }
    }

    fun onStopPlayback() {
        releaseMediaPlayer()
        progressJob?.cancel()
        progressJob = null
        _uiState.update { it.copy(audioState = AudioPlaybackState(audioUrls = it.audioState.audioUrls)) }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob =
            viewModelScope.launch {
                while (true) {
                    delay(PROGRESS_POLL_MS)
                    val position = withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer?.currentPosition ?: 0
                        } catch (e: IllegalStateException) {
                            0
                        }
                    }
                    _uiState.update { it.copy(audioState = it.audioState.copy(currentPositionMs = position)) }
                }
            }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
    }

    // ── Reading timer ─────────────────────────────

    private fun isRetriablePostError(error: Throwable): Boolean =
        when (error) {
            is SocketTimeoutException,
            is TimeoutCancellationException,
            is IOException,
            -> true

            is HttpException -> error.code() in 500..599
            else -> false
        }

    private suspend fun postActivityWithRetry(
        timezone: String,
        request: ActivityDayRequest,
    ) {
        var lastError: Throwable? = null

        for (attempt in 1..POST_ACTIVITY_MAX_ATTEMPTS) {
            try {
                val token =
                    authRepository.getValidAccessToken() ?: throw Exception("Not authenticated")

                withTimeout(POST_ACTIVITY_TIMEOUT_MS) {
                    quranApiService.postActivityDays(
                        accessToken = token,
                        clientId = QuranOAuthConfig.clientId,
                        timezone = timezone,
                        request = request,
                    )
                }

                if (attempt > 1) {
                    Log.w(TAG, "postActivityDays succeeded on retry attempt=$attempt")
                }
                return
            } catch (e: Throwable) {
                lastError = e

                if (e is HttpException && e.code() == 401) {
                    val refreshed = authRepository.refreshTokens()
                    if (!refreshed) throw e
                } else if (!isRetriablePostError(e)) {
                    throw e
                }

                if (attempt < POST_ACTIVITY_MAX_ATTEMPTS) {
                    val backoffMs =
                        when (attempt) {
                            1 -> 400L
                            2 -> 900L
                            else -> 1_500L
                        }
                    Log.w(
                        TAG,
                        "postActivityDays failed attempt=$attempt, retrying in ${backoffMs}ms",
                        e,
                    )
                    delay(backoffMs)
                }
            }
        }

        throw lastError ?: Exception("Failed to post activity")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    _uiState.update { it.copy(readingSeconds = it.readingSeconds + 1) }
                }
            }
    }

    fun retry() {
        loadPageData()
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

                readHistoryDao.insert(
                    ReadHistoryEntity(
                        pageNumber = pageNumber,
                        timestamp = System.currentTimeMillis(),
                        totalTimeReadSeconds = state.readingSeconds,
                    ),
                )

                journalEntryDao.markAsRead(
                    id = entryId,
                )

                val currentTimeZone = TimeZone.getDefault().id
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = dateFormat.format(Date())

                val request =
                    ActivityDayRequest(
                        date = dateStr,
                        type = "QURAN",
                        seconds = state.readingSeconds,
                        ranges = listOf("$firstVerse-$lastVerse"),
                    )

                postActivityWithRetry(
                    timezone = currentTimeZone,
                    request = request,
                )

                _uiState.update { it.copy(isPosting = false, postSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post activity after retries", e)
                _uiState.update {
                    it.copy(
                        isPosting = false,
                        postSuccess = false,
                        error = "Failed to synchronize progress to Quran.com.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        progressJob?.cancel()
        releaseMediaPlayer()
    }
}
