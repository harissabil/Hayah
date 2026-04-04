package id.harissabil.hayah.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Period { THIS_WEEK, THIS_MONTH, ALL_TIME }

data class HomeUiState(
    val userName: String = "Abdullah",
    val versesRead: Int = 7,
    val totalVerses: Int = 10,
    val selectedPeriod: Period = Period.THIS_WEEK,
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }
}
