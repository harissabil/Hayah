package id.harissabil.hayah.ui.screens.settings

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import id.harissabil.hayah.data.model.RecitationItem
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.HayahAccessibilityService
import id.harissabil.hayah.service.executeWithNetworkRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTheme { LIGHT, DARK, SYSTEM }

data class ReciterOption(
    val id: Int,
    val name: String,
)

data class SettingsUiState(
    val playAudioInstantly: Boolean = true,
    val maxReminders: Float = 5f,
    val quietDuration: Float = 5f,
    val detectionThreshold: Float = 3f,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val isAccessibilityEnabled: Boolean = false,
    val reciterName: String = "Mishary Rashid Alafasy",
    val reciterId: Int = 7,
    val isReciterDialogOpen: Boolean = false,
    val isRecitersLoading: Boolean = false,
    val reciterOptions: List<ReciterOption> = emptyList(),
    val reciterError: String? = null,
)

class SettingsViewModel(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val quranApiService: QuranApiService,
) : ViewModel() {
    companion object {
        val KEY_PLAY_AUDIO = booleanPreferencesKey("play_audio_instantly")
        val KEY_MAX_REMINDERS = floatPreferencesKey("max_reminders")
        val KEY_QUIET_DURATION = floatPreferencesKey("quiet_duration_minutes")
        val KEY_DETECTION_THRESHOLD = floatPreferencesKey("detection_threshold")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_RECITER_ID = intPreferencesKey("reciter_id")
        val KEY_RECITER_NAME = stringPreferencesKey("reciter_name")
        private val KEY_RECITERS_CACHE_JSON = stringPreferencesKey("reciters_cache_json")
        private val KEY_RECITERS_CACHE_TIME = longPreferencesKey("reciters_cache_time")
        private const val RECITERS_CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }

    private val gson = Gson()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPersistedSettings()
    }

    private fun loadPersistedSettings() {
        viewModelScope.launch {
            context.hayahSettingsDataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        playAudioInstantly = prefs[KEY_PLAY_AUDIO] ?: true,
                        maxReminders = prefs[KEY_MAX_REMINDERS] ?: 5f,
                        quietDuration = prefs[KEY_QUIET_DURATION] ?: 5f,
                        detectionThreshold = prefs[KEY_DETECTION_THRESHOLD] ?: 3f,
                        appTheme =
                            prefs[KEY_APP_THEME]?.let { themeStr ->
                                AppTheme.entries.find { it.name == themeStr }
                            } ?: AppTheme.SYSTEM,
                        reciterId = prefs[KEY_RECITER_ID] ?: 7,
                        reciterName = prefs[KEY_RECITER_NAME] ?: "Mishary Rashid Alafasy",
                        isAccessibilityEnabled = isAccessibilityServiceEnabled(),
                    )
                }
            }
        }
    }

    fun refreshAccessibilityState() {
        _uiState.update { it.copy(isAccessibilityEnabled = isAccessibilityServiceEnabled()) }
    }

    fun onPlayAudioToggled(enabled: Boolean) {
        _uiState.update { it.copy(playAudioInstantly = enabled) }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit { it[KEY_PLAY_AUDIO] = enabled }
        }
    }

    fun onMaxRemindersChanged(value: Float) {
        _uiState.update { it.copy(maxReminders = value) }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit { it[KEY_MAX_REMINDERS] = value }
        }
    }

    fun onQuietDurationChanged(value: Float) {
        _uiState.update { it.copy(quietDuration = value) }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit { it[KEY_QUIET_DURATION] = value }
        }
    }

    fun onDetectionThresholdChanged(value: Float) {
        _uiState.update { it.copy(detectionThreshold = value) }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit { it[KEY_DETECTION_THRESHOLD] = value }
        }
    }

    fun onThemeSelected(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit { it[KEY_APP_THEME] = theme.name }
        }
    }

    fun onReciterPickerRequested() {
        _uiState.update { it.copy(isReciterDialogOpen = true, reciterError = null) }
        loadReciters()
    }

    fun onReciterDialogDismissed() {
        _uiState.update { it.copy(isReciterDialogOpen = false, reciterError = null) }
    }

    fun onReciterSelected(
        id: Int,
        name: String,
    ) {
        _uiState.update {
            it.copy(
                reciterId = id,
                reciterName = name,
                isReciterDialogOpen = false,
                reciterError = null,
            )
        }
        viewModelScope.launch {
            context.hayahSettingsDataStore.edit {
                it[KEY_RECITER_ID] = id
                it[KEY_RECITER_NAME] = name
            }
        }
    }

    private fun loadReciters() {
        viewModelScope.launch {
            if (_uiState.value.isRecitersLoading) return@launch

            _uiState.update { it.copy(isRecitersLoading = true, reciterError = null) }

            val prefs = context.hayahSettingsDataStore.data.first()
            val cachedJson = prefs[KEY_RECITERS_CACHE_JSON]
            val cachedAt = prefs[KEY_RECITERS_CACHE_TIME] ?: 0L
            val now = System.currentTimeMillis()

            if (!cachedJson.isNullOrBlank() && now - cachedAt < RECITERS_CACHE_TTL_MS) {
                val cached = parseReciterOptions(cachedJson)
                if (cached.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isRecitersLoading = false,
                            reciterOptions = cached,
                        )
                    }
                    return@launch
                }
            }

            val accessToken = authRepository.getValidAccessToken()
            if (accessToken.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isRecitersLoading = false,
                        reciterError = "Please login again to load reciters.",
                    )
                }
                return@launch
            }

            try {
                val response =
                    executeWithNetworkRetry {
                        quranApiService.getRecitations(
                            accessToken = accessToken,
                            clientId = QuranOAuthConfig.clientId,
                            language = "en",
                        )
                    }

                val options =
                    response.recitations
                        .orEmpty()
                        .mapNotNull(::toReciterOption)
                        .distinctBy { it.id }
                        .sortedBy { it.name.lowercase() }

                context.hayahSettingsDataStore.edit {
                    it[KEY_RECITERS_CACHE_JSON] = gson.toJson(options)
                    it[KEY_RECITERS_CACHE_TIME] = now
                }

                _uiState.update {
                    it.copy(
                        isRecitersLoading = false,
                        reciterOptions = options,
                        reciterError = if (options.isEmpty()) "No reciters found." else null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRecitersLoading = false,
                        reciterError = "Failed to load reciters.",
                    )
                }
            }
        }
    }

    private fun toReciterOption(item: RecitationItem): ReciterOption? {
        val id = item.id ?: return null
        val nameBase = item.translatedName?.name ?: item.reciterName ?: return null
        val style = item.style?.takeIf { it.isNotBlank() }
        val display = if (style != null) "$nameBase ($style)" else nameBase
        return ReciterOption(id = id, name = display)
    }

    private fun parseReciterOptions(json: String): List<ReciterOption> =
        try {
            val type = object : TypeToken<List<ReciterOption>>() {}.type
            gson.fromJson<List<ReciterOption>>(json, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName =
            "${context.packageName}/${HayahAccessibilityService::class.java.canonicalName}"
        return try {
            val enabledServices =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                )
            enabledServices?.contains(serviceName) == true
        } catch (_: Exception) {
            false
        }
    }
}
