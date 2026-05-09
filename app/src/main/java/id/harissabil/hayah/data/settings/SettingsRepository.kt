package id.harissabil.hayah.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val KEY_PLAY_AUDIO = booleanPreferencesKey("play_audio_instantly")
        val KEY_MAX_REMINDERS = floatPreferencesKey("max_reminders")
        val KEY_QUIET_DURATION = floatPreferencesKey("quiet_duration_minutes")
        val KEY_DETECTION_THRESHOLD = floatPreferencesKey("detection_threshold")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_RECITER_ID = intPreferencesKey("reciter_id")
        val KEY_RECITER_NAME = stringPreferencesKey("reciter_name")
        val KEY_RECITERS_CACHE_JSON = stringPreferencesKey("reciters_cache_json")
        val KEY_RECITERS_CACHE_TIME = longPreferencesKey("reciters_cache_time")
        val KEY_CUSTOM_KEYWORDS = stringSetPreferencesKey("custom_keywords")
        val KEY_DISCLOSURE_ACCEPTED = booleanPreferencesKey("accessibility_disclosure_accepted")
        val KEY_DISCLOSURE_DECLINED = booleanPreferencesKey("accessibility_disclosure_declined")
        val KEY_ACCESSIBILITY_TUTORIAL_SHOWN = booleanPreferencesKey("accessibility_tutorial_shown_once")
    }

    val settingsFlow: Flow<Preferences> = dataStore.data

    suspend fun <T> get(
        key: Preferences.Key<T>,
        default: T,
    ): T = dataStore.data.first()[key] ?: default

    suspend fun <T> set(
        key: Preferences.Key<T>,
        value: T,
    ) {
        dataStore.edit { it[key] = value }
    }

    suspend fun saveRecitersCache(
        json: String,
        timestamp: Long,
    ) {
        dataStore.edit {
            it[KEY_RECITERS_CACHE_JSON] = json
            it[KEY_RECITERS_CACHE_TIME] = timestamp
        }
    }

    suspend fun saveReciterSelection(
        id: Int,
        name: String,
    ) {
        dataStore.edit {
            it[KEY_RECITER_ID] = id
            it[KEY_RECITER_NAME] = name
        }
    }

    suspend fun setDisclosureAccepted() {
        dataStore.edit {
            it[KEY_DISCLOSURE_ACCEPTED] = true
            it[KEY_DISCLOSURE_DECLINED] = false
        }
    }

    suspend fun setDisclosureDeclined() {
        dataStore.edit { it[KEY_DISCLOSURE_DECLINED] = true }
    }
}
