package id.harissabil.hayah.ui.screens.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.model.UserProfileResponse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared ViewModel that exposes authentication state to the entire app.
 * Lives at the Activity scope so all screens can observe it.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authUiState: StateFlow<AuthUiState> = combine(
        authRepository.isLoading,
        authRepository.isAuthenticated,
        authRepository.userProfile,
    ) { loading, authenticated, profile ->
        when {
            loading -> AuthUiState.Loading
            authenticated -> AuthUiState.Authenticated(profile)
            else -> AuthUiState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState.Loading,
    )

    init {
        viewModelScope.launch {
            authRepository.initialize()
        }
    }

    /**
     * Creates the intent that launches the browser for Quran.com login.
     */
    fun buildAuthIntent(): Intent = authRepository.buildAuthIntent()

    /**
     * Processes the callback intent from the browser after the user authenticates.
     */
    fun handleAuthResponse(intent: Intent) {
        viewModelScope.launch {
            authRepository.handleAuthResponse(intent)
        }
    }

    /**
     * Clears all tokens and profile data.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class Authenticated(val profile: UserProfileResponse?) : AuthUiState()
}
