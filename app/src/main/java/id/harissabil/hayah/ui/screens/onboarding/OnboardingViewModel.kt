package id.harissabil.hayah.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import id.harissabil.hayah.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val pages: List<OnboardingPage> =
        listOf(
            OnboardingPage(
                title = "Busy World?\nLet the Quran Greet You",
                body = "Step away from the noise. Receive proactive Quranic guidance that fits seamlessly into your modern, daily life.",
                imageRes = R.drawable.img_onboarding_1,
            ),
            OnboardingPage(
                title = "Reminders That\nUnderstand You",
                body = "Whether you are walking, working, or resting, Hayah senses your context and delivers the perfect verse for that exact moment.",
                imageRes = R.drawable.img_onboarding_2,
            ),
            OnboardingPage(
                title = "One Verse,\nAt The Right Time",
                body = "Transform your phone from a distraction into a source of spiritual grounding with smart, context-aware reminders.",
                imageRes = R.drawable.img_onboarding_3,
            ),
        ),
    val currentPage: Int = 0,
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }
}
