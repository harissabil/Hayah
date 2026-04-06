package id.harissabil.hayah.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single shared DataStore instance for app settings.
 *
 * Keep this declaration in one file to avoid multiple active DataStores
 * for the same backing file.
 */
val Context.hayahSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hayah_settings")
