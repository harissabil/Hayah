package id.harissabil.hayah.data.auth

import android.annotation.SuppressLint
import android.net.Uri
import androidx.core.net.toUri
import id.harissabil.hayah.BuildConfig

/**
 * Central configuration for Quran.com OAuth 2.0.
 *
 * Runtime values are provided via BuildConfig fields from local.properties.
 * Client secrets are NOT stored in the app; they stay in the Cloudflare Worker.
 */
object QuranOAuthConfig {
    // ──────────────────────────────────────────────
    //  Environment and endpoints from BuildConfig
    // ──────────────────────────────────────────────
    val useProduction: Boolean
        get() = BuildConfig.USE_PRODUCTION

    val clientId: String
        get() = if (useProduction) BuildConfig.OAUTH_CLIENT_ID_PROD else BuildConfig.OAUTH_CLIENT_ID_TEST

    val authEndpoint: Uri
        @SuppressLint("UseKtx")
        get() =
            Uri.parse(
                if (useProduction) BuildConfig.OAUTH_AUTH_ENDPOINT_PROD else BuildConfig.OAUTH_AUTH_ENDPOINT_TEST,
            )

    val tokenEndpoint: Uri
        get() = BuildConfig.OAUTH_TOKEN_PROXY_URL.toUri()

    val revokeEndpoint: Uri
        get() = BuildConfig.OAUTH_REVOKE_PROXY_URL.toUri()

    val apiBaseUrl: String
        get() =
            (if (useProduction) BuildConfig.OAUTH_API_BASE_PROD else BuildConfig.OAUTH_API_BASE_TEST)
                .let { if (it.endsWith("/")) it else "$it/" }

    val redirectUri: Uri
        get() = BuildConfig.OAUTH_REDIRECT_URI.toUri()

    // ──────────────────────────────────────────────
    //  Scopes requested during authorization
    // ──────────────────────────────────────────────
    val SCOPES: List<String> =
        listOf(
            "offline_access",
            "content",
            "reading_session",
            "preference",
            "activity_day",
            "streak",
            "user",
        )
}
