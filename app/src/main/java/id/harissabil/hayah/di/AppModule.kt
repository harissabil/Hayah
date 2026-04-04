package id.harissabil.hayah.di

import id.harissabil.hayah.data.api.RetrofitClient
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.AuthStateManager
import id.harissabil.hayah.ui.screens.auth.AuthViewModel
import id.harissabil.hayah.ui.screens.home.HomeViewModel
import id.harissabil.hayah.ui.screens.journal.JournalViewModel
import id.harissabil.hayah.ui.screens.onboarding.OnboardingViewModel
import id.harissabil.hayah.ui.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Data layer ─────────────────────────

    single { AuthStateManager(androidContext()) }

    single { RetrofitClient.create() }

    single {
        AuthRepository(
            context = androidContext(),
            authStateManager = get(),
            apiService = get(),
        )
    }

    // ── ViewModels ─────────────────────────

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { JournalViewModel() }
    viewModel { SettingsViewModel() }
}
