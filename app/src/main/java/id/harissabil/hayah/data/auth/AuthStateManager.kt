package id.harissabil.hayah.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import id.harissabil.hayah.data.model.UserProfileResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.openid.appauth.AuthState

/**
 * Persists AppAuth [AuthState] and [UserProfileResponse] using Jetpack DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hayah_auth")

class AuthStateManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val KEY_AUTH_STATE = stringPreferencesKey("auth_state_json")
        private val KEY_USER_PROFILE = stringPreferencesKey("user_profile_json")
    }

    // ── AuthState ──────────────────────────────────

    suspend fun getAuthState(): AuthState {
        val json = context.dataStore.data
            .map { prefs -> prefs[KEY_AUTH_STATE] }
            .first()
        return if (json != null) {
            AuthState.jsonDeserialize(json)
        } else {
            AuthState()
        }
    }

    suspend fun saveAuthState(state: AuthState) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_STATE] = state.jsonSerializeString()
        }
    }

    suspend fun clearAuthState() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_STATE)
            prefs.remove(KEY_USER_PROFILE)
        }
    }

    // ── User Profile ───────────────────────────────

    suspend fun getUserProfile(): UserProfileResponse? {
        val json = context.dataStore.data
            .map { prefs -> prefs[KEY_USER_PROFILE] }
            .first()
        return json?.let {
            try {
                gson.fromJson(it, UserProfileResponse::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfileResponse) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_PROFILE] = gson.toJson(profile)
        }
    }
}
