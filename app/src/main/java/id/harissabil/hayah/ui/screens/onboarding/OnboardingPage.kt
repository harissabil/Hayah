package id.harissabil.hayah.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingPage(
    val title: String,
    val body: String,
    @DrawableRes val imageRes: Int,
)
