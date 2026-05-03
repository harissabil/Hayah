package id.harissabil.hayah.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.hayahSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hayah_settings")

val KEY_CUSTOM_KEYWORDS = stringSetPreferencesKey("custom_keywords")
