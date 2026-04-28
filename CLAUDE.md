# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Lint & format (ktlint)
./gradlew ktlintCheck
./gradlew ktlintFormat

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "id.harissabil.hayah.SomeTest"

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Required Setup (local.properties)

All config values are injected as `BuildConfig` fields — the build fails without them. Copy `local.properties.example` and fill in:

```properties
HAYAH_USE_PRODUCTION=true
HAYAH_CLIENT_ID_PROD=...
HAYAH_CLIENT_ID_TEST=...
HAYAH_AUTH_ENDPOINT_PROD=https://oauth2.quran.foundation/oauth2/auth
HAYAH_AUTH_ENDPOINT_TEST=https://prelive-oauth2.quran.foundation/oauth2/auth
HAYAH_TOKEN_PROXY_URL=https://<your-worker>/token
HAYAH_REVOKE_PROXY_URL=https://<your-worker>/revoke
HAYAH_API_BASE_PROD=https://apis.quran.foundation/
HAYAH_API_BASE_TEST=https://apis-prelive.quran.foundation/
HAYAH_REDIRECT_URI=id.harissabil.hayah://callback
```

Also place `google-services.json` from Firebase Console into `app/google-services.json`.

## Architecture

**Single-module Android app** (`:app`), MVVM + Koin DI, Jetpack Compose UI, Room + DataStore persistence, Retrofit networking.

### Layer overview

| Package | Role |
|---|---|
| `data/auth` | OAuth 2.0 via AppAuth + Cloudflare Worker proxy; `AuthRepository` manages the full token lifecycle |
| `data/ai` | `VerseRecommendationService` — calls Firebase AI Logic (Gemini) to get verse keys and generate reflections |
| `data/api` | Retrofit `QuranApiService` talking to Quran Foundation REST API |
| `data/db` | Room database (`HayahDatabase`) with three tables: `keyword_cache`, `journal_entries`, `read_history` |
| `data/settings` | Single `DataStore<Preferences>` instance (`hayahSettingsDataStore` extension on `Context`) |
| `di` | Single Koin `appModule` — all singletons and ViewModels wired here |
| `service` | Background logic: `HayahAccessibilityService`, `ActivityRecognitionManager`, `ActivityTransitionReceiver`, `ReminderOrchestrator`, `NotificationHelper` |
| `ui` | Compose screens (Home, Journal, Onboarding, QuranReading, Settings) + `HayahNavGraph` |

### Core reminder pipeline

`HayahAccessibilityService` / `ActivityTransitionReceiver` → `ReminderOrchestrator.onKeywordDetected()` → dedup/cooldown/threshold checks → `VerseRecommendationService` (Gemini) → `QuranApiService` (verse + audio) → `NotificationHelper` → `JournalEntryDao` (persist).

Caching: `KeywordCacheDao` stores up to 5 verses per keyword (JSON in `KeywordCacheEntity.versesJson`); `lastShownIndex` rotates through them on subsequent triggers to avoid repeating the same verse.

All API calls inside the pipeline use `executeWithNetworkRetry()` (`service/ApiRetryPolicy.kt`) — exponential backoff, retries on IO errors, 5xx, and 429.

### Navigation

`HayahNavGraph` hosts a `NavHost` with bottom-bar screens (Home, Journal, Settings) and a full-screen `QuranReading` route that accepts `pageNumber`, optional `entryId`, and optional `highlightedVerseKey` as arguments.

Auth state (from `AuthViewModel`) determines whether to start at `Onboarding` or `Home`.

### Room schema

Schema files are exported to `app/schemas/`. The database uses `fallbackToDestructiveMigration = true` — add explicit migrations for production-ready releases.

## Jetpack Compose Guidelines

For all Compose work, follow `.agents/rules/compose-skill.md` (always-on rule): consult `.agents/skills/jetpack-compose-expert-skill/SKILL.md` and the reference files under `.agents/skills/jetpack-compose-expert-skill/references/` before implementing.

## Code Style

ktlint is enforced (`ignoreFailures = false`). Run `./gradlew ktlintFormat` before committing. Only `**/java/**` and `**/kotlin/**` sources are checked; generated files are excluded.
