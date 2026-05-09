package id.harissabil.hayah.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import id.harissabil.hayah.BuildConfig
import id.harissabil.hayah.data.ai.VerseRecommendationService
import id.harissabil.hayah.data.api.RetrofitClient
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.AuthStateManager
import id.harissabil.hayah.data.db.HayahDatabase
import id.harissabil.hayah.data.settings.SettingsRepository
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.ActivityRecognitionManager
import id.harissabil.hayah.service.NotificationHelper
import id.harissabil.hayah.service.ReminderOrchestrator
import id.harissabil.hayah.ui.screens.auth.AuthViewModel
import id.harissabil.hayah.ui.screens.home.HomeViewModel
import id.harissabil.hayah.ui.screens.journal.JournalViewModel
import id.harissabil.hayah.ui.screens.onboarding.OnboardingViewModel
import id.harissabil.hayah.ui.screens.reading.QuranReadingViewModel
import id.harissabil.hayah.ui.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {

        // ── Database ──────────────────────────────
        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    HayahDatabase::class.java,
                    "hayah_database",
                ).fallbackToDestructiveMigration(true)
                .build()
        }

        single { get<HayahDatabase>().keywordCacheDao() }
        single { get<HayahDatabase>().journalEntryDao() }
        single { get<HayahDatabase>().readHistoryDao() }

        // ── Settings ─────────────────────────────
        single<DataStore<Preferences>> { androidContext().hayahSettingsDataStore }
        single { SettingsRepository(get()) }

        // ── Auth data layer ──────────────────────
        single { AuthStateManager(androidContext()) }
        single { RetrofitClient.create() }
        single {
            AuthRepository(
                context = androidContext(),
                authStateManager = get(),
                apiService = get(),
            )
        }

        // ── AI ────────────────────────────────────
        single { VerseRecommendationService(BuildConfig.MCP_QURAN_URL) }

        // ── Services ─────────────────────────────
        single { NotificationHelper(androidContext()) }
        single { ActivityRecognitionManager(androidContext()) }
        single {
            ReminderOrchestrator(
                settingsRepository = get(),
                authRepository = get(),
                keywordCacheDao = get(),
                journalEntryDao = get(),
                quranApiService = get(),
                verseRecommendationService = get(),
                notificationHelper = get(),
            )
        }

        // ── ViewModels ───────────────────────────
        viewModel { AuthViewModel(get()) }
        viewModel {
            HomeViewModel(
                authRepository = get(),
                settingsRepository = get(),
                journalEntryDao = get(),
                quranApiService = get(),
                verseRecommendationService = get(),
                notificationHelper = get(),
            )
        }
        viewModel { OnboardingViewModel() }
        viewModel { JournalViewModel(get()) }
        viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
        viewModel {
            QuranReadingViewModel(
                savedStateHandle = get(),
                quranApiService = get(),
                authRepository = get(),
                readHistoryDao = get(),
                journalEntryDao = get(),
                settingsRepository = get(),
            )
        }
    }
