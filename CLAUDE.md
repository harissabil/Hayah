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
HAYAH_MCP_QURAN_URL=https://mcp.quran.ai
```

Also place `google-services.json` from Firebase Console into `app/google-services.json`.

## Architecture

**Single-module Android app** (`:app`), MVVM + Koin DI, Jetpack Compose UI, Room + DataStore persistence, Retrofit networking.

### Layer overview

| Package | Role |
|---|---|
| `data/auth` | OAuth 2.0 via AppAuth + Cloudflare Worker proxy; `AuthRepository` manages the full token lifecycle |
| `data/ai` | `VerseRecommendationService` — connects to `mcp.quran.ai` via MCP Kotlin SDK (Streamable HTTP), exposes MCP tools as Gemini `FunctionDeclaration`s, runs a function-call loop to get real verse keys, then generates Ibn Kathir–grounded reflections with a separate structured-output model |
| `data/api` | Retrofit `QuranApiService` talking to Quran Foundation REST API |
| `data/db` | Room database (`HayahDatabase`) with three tables: `keyword_cache`, `journal_entries`, `read_history` |
| `data/settings` | Single `DataStore<Preferences>` instance (`hayahSettingsDataStore` extension on `Context`) |
| `di` | Single Koin `appModule` — all singletons and ViewModels wired here |
| `service` | Background logic: `HayahAccessibilityService`, `ActivityRecognitionManager`, `ActivityTransitionReceiver`, `ReminderOrchestrator`, `NotificationHelper`, `ThemeEmbeddingManager` |
| `ui` | Compose screens (Home, Journal, Onboarding, QuranReading, Settings) + `HayahNavGraph` |

### Core reminder pipeline

`HayahAccessibilityService` / `ActivityTransitionReceiver` → `ReminderOrchestrator.onKeywordDetected()` → dedup/cooldown/threshold checks → `VerseRecommendationService.recommendVerses()` (MCP tool-call loop via Gemini) → `QuranApiService` (verse detail + tafsir + audio) → `VerseRecommendationService.generateReflections()` (structured JSON, tafsir-grounded) → `NotificationHelper` → `JournalEntryDao` (persist).

Caching: `KeywordCacheDao` stores up to 5 verses per keyword (JSON in `KeywordCacheEntity.versesJson`); `lastShownIndex` rotates through them on subsequent triggers to avoid repeating the same verse.

#### Detection modes

`HayahAccessibilityService` supports two modes, switchable at runtime via `SettingsRepository` (`KEY_DETECTION_MODE`):

**Keywords mode** — substring match against `TriggerKeywords`; 1 s per-word cooldown; `ReminderOrchestrator` applies a consecutive-hit threshold before forwarding.

**Semantic mode** — on-device MediaPipe `TextEmbedder` (`universal_sentence_encoder.tflite`, ~25 MB, downloaded once to `context.filesDir`). `ThemeEmbeddingManager` pre-computes 512-dim embeddings for 50+ `ThemeDefinition`s from `TriggerKeywords.THEME_DEFINITIONS` (rich descriptions rather than bare keywords, for better embedding quality), plus the user's custom keywords. At runtime, screen text is truncated to 500 chars, embedded, and compared via cosine similarity against all theme embeddings; a match above `KEY_SIMILARITY_THRESHOLD` (default 0.75, user-configurable) is forwarded directly to `ReminderOrchestrator`, bypassing the consecutive-hit threshold entirely. Cooldown is 3 s (vs 1 s in keywords mode).

#### VerseRecommendationService internals

`recommendVerses(keyword)`:
1. Opens a Streamable HTTP connection to `mcp.quran.ai` via MCP Kotlin SDK
2. Calls `listTools()` and maps each MCP tool to a Firebase AI `FunctionDeclaration` (required vs optional params)
3. Starts a Gemini chat and loops: sends tool results back as `FunctionResponsePart` until no more `functionCalls` in the response
4. Parses the final JSON array of verse keys (e.g. `["2:255", "67:15"]`)

`generateReflections(keyword, versesWithTranslations, tafsir?)`:
- Uses a lazy `reflectionModel` with `responseSchema` (structured JSON output)
- If `tafsir` map is provided (Ibn Kathir excerpts keyed by verse key), appended to each verse block in the prompt
- Returns `Map<verseKey, reflectionText>`

Tafsir source: resource 169 = Ibn Kathir Abridged, English. Both `ReminderOrchestrator` and `HomeViewModel` receive tafsir via the `?tafsirs=169` query param on their respective verse fetch endpoints (`getVerseByKey` and `getRandomVerse`).

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
