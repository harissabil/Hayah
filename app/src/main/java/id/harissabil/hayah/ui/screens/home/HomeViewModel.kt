package id.harissabil.hayah.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.model.UserProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Period { THIS_WEEK, THIS_MONTH, ALL_TIME }

data class HomeUiState(
    val userName: String = "",
    val versesRead: Int = 7,
    val totalVerses: Int = 10,
    val selectedPeriod: Period = Period.THIS_WEEK,
    val profilePhotoUrl: String? = null,
)

class HomeViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.userProfile.collect { profile ->
                updateFromProfile(profile)
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
                    profilePhotoUrl = profile.avatarUrls?.medium
                        ?: profile.avatarUrls?.small,
                )
            }
        }
    }

    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }
}
