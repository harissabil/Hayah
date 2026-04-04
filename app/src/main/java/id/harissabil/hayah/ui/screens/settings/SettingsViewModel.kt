package id.harissabil.hayah.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppTheme { LIGHT, DARK }

data class SettingsUiState(
    val playAudioInstantly: Boolean = true,
    val maxReminders: Float = 5f,
    val appTheme: AppTheme = AppTheme.LIGHT,
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onPlayAudioToggled(enabled: Boolean) {
        _uiState.update { it.copy(playAudioInstantly = enabled) }
    }

    fun onMaxRemindersChanged(value: Float) {
        _uiState.update { it.copy(maxReminders = value) }
    }

    fun onThemeSelected(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme) }
    }
}
