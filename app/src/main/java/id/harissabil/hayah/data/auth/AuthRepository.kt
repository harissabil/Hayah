package id.harissabil.hayah.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import id.harissabil.hayah.data.api.QuranApiService
import id.harissabil.hayah.data.model.UserProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume

/**
 * Coordinates the full OAuth 2.0 lifecycle:
 * 1. Building the authorization intent (PKCE handled by AppAuth)
 * 2. Exchanging authorization code for tokens via the Cloudflare Worker proxy
 * 3. Fetching user profile from Quran Foundation API
 * 4. Refreshing tokens when expired
 * 5. Logging out (clearing persisted state)
 */
class AuthRepository(
    private val context: Context,
    private val authStateManager: AuthStateManager,
    private val apiService: QuranApiService,
) {
    companion object {
        private const val TAG = "AuthRepository"
        private const val TOKEN_REFRESH_BUFFER_MS = 30_000L
    }

    private val serviceConfig =
        AuthorizationServiceConfiguration(
            QuranOAuthConfig.authEndpoint, // authorization endpoint → real Quran Foundation
            QuranOAuthConfig.tokenEndpoint, // token endpoint → Cloudflare Worker proxy
        )

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val refreshMutex = Mutex()

    /**
     * Call once at app startup to restore auth state from DataStore.
     */
    suspend fun initialize() =
        withContext(Dispatchers.IO) {
            try {
                val authState = authStateManager.getAuthState()
                val hasTokens = authState.accessToken != null || authState.refreshToken != null
                _isAuthenticated.value = hasTokens

                if (hasTokens) {
                    // Try to load cached profile
                    _userProfile.value = authStateManager.getUserProfile()

                    // Proactively refresh when token is close to expiry.
                    if (shouldRefreshTokenSoon(authState)) {
                        getValidAccessToken()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize auth state", e)
                _isAuthenticated.value = false
            } finally {
                _isLoading.value = false
            }
        }

    /**
     * Returns a token that is safe to use for Quran.com API calls.
     * Refreshes proactively when expiry is within 30 seconds.
     */
    suspend fun getValidAccessToken(): String? =
        withContext(Dispatchers.IO) {
            val authState = authStateManager.getAuthState()
            val currentToken = authState.accessToken ?: return@withContext null

            if (!shouldRefreshTokenSoon(authState)) {
                return@withContext currentToken
            }

            val refreshed = refreshTokens()
            if (refreshed) {
                return@withContext authStateManager.getAuthState().accessToken
            }

            // If refresh failed but token is still technically valid, use it as a fallback.
            return@withContext if (isTokenStillUsable(authState)) currentToken else null
        }

    private fun shouldRefreshTokenSoon(state: net.openid.appauth.AuthState): Boolean {
        val expiresAt = state.accessTokenExpirationTime
        return if (expiresAt != null) {
            System.currentTimeMillis() >= (expiresAt - TOKEN_REFRESH_BUFFER_MS)
        } else {
            state.needsTokenRefresh
        }
    }

    private fun isTokenStillUsable(state: net.openid.appauth.AuthState): Boolean {
        val expiresAt = state.accessTokenExpirationTime ?: return state.accessToken != null
        return System.currentTimeMillis() < expiresAt
    }

    // ── Authorization Intent ───────────────────────

    /**
     * Builds the intent that opens the browser for Quran.com login.
     * AppAuth automatically generates PKCE code_verifier/code_challenge.
     */
    fun buildAuthIntent(): Intent {
        val request =
            AuthorizationRequest
                .Builder(
                    serviceConfig,
                    QuranOAuthConfig.clientId,
                    ResponseTypeValues.CODE,
                    QuranOAuthConfig.redirectUri,
                ).setScopes(QuranOAuthConfig.SCOPES)
                .build()

        val authService = AuthorizationService(context)
        return authService.getAuthorizationRequestIntent(request)
    }

    // ── Handle Authorization Response ──────────────

    /**
     * Processes the intent returned from the browser after the user logs in.
     * Exchanges the authorization code for tokens, then fetches the user profile.
     */
    suspend fun handleAuthResponse(intent: Intent): Boolean =
        withContext(Dispatchers.IO) {
            val response = AuthorizationResponse.fromIntent(intent)
            val exception = AuthorizationException.fromIntent(intent)

            // Update auth state with the authorization response
            val authState = authStateManager.getAuthState()
            authState.update(response, exception)
            authStateManager.saveAuthState(authState)

            if (response == null) {
                Log.e(TAG, "Authorization failed: ${exception?.errorDescription}")
                return@withContext false
            }

            // Exchange auth code for tokens (via CF Worker)
            val success = exchangeCodeForTokens(response, authState)
            if (success) {
                _isAuthenticated.value = true
                // Fetch and persist user profile
                fetchAndCacheProfile()
            }
            success
        }

    private suspend fun exchangeCodeForTokens(
        response: AuthorizationResponse,
        authState: net.openid.appauth.AuthState,
    ): Boolean {
        val tokenRequest = response.createTokenExchangeRequest()
        val authService = AuthorizationService(context)

        return try {
            val (tokenResponse, tokenException) =
                suspendCancellableCoroutine { continuation ->
                    authService.performTokenRequest(tokenRequest) { resp, ex ->
                        continuation.resume(resp to ex)
                    }
                }

            authState.update(tokenResponse, tokenException)
            authStateManager.saveAuthState(authState)

            if (tokenException != null) {
                Log.e(TAG, "Token exchange failed: ${tokenException.errorDescription}")
                false
            } else {
                Log.d(TAG, "Token exchange successful")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange error", e)
            false
        } finally {
            authService.dispose()
        }
    }

    // ── Token Refresh ──────────────────────────────

    /**
     * Refreshes the access token using the stored refresh token.
     */
    suspend fun refreshTokens(): Boolean =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                val authState = authStateManager.getAuthState()
                val hadSessionBeforeRefresh = authState.accessToken != null || authState.refreshToken != null

                if (authState.refreshToken == null) {
                    Log.w(TAG, "Cannot refresh token: refresh_token is missing")
                    _isAuthenticated.value = authState.accessToken != null
                    return@withLock false
                }

                val authService = AuthorizationService(context)
                try {
                    val tokenRequest = authState.createTokenRefreshRequest()

                    val (tokenResponse, tokenException) =
                        suspendCancellableCoroutine { continuation ->
                            authService.performTokenRequest(tokenRequest) { resp, ex ->
                                continuation.resume(resp to ex)
                            }
                        }

                    authState.update(tokenResponse, tokenException)
                    authStateManager.saveAuthState(authState)

                    if (tokenException != null) {
                        Log.e(TAG, "Token refresh failed: ${tokenException.errorDescription}")
                        _isAuthenticated.value =
                            if (isTransientRefreshFailure(tokenException) && hadSessionBeforeRefresh) {
                                true
                            } else {
                                false
                            }
                        false
                    } else {
                        _isAuthenticated.value = true
                        true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Token refresh error", e)
                    _isAuthenticated.value =
                        if (isTransientNetworkError(e) && hadSessionBeforeRefresh) {
                            true
                        } else {
                            false
                        }
                    false
                } finally {
                    authService.dispose()
                }
            }
        }

    private fun isTransientRefreshFailure(exception: AuthorizationException): Boolean =
        exception.type == AuthorizationException.TYPE_GENERAL_ERROR &&
            exception.code == AuthorizationException.GeneralErrors.NETWORK_ERROR.code

    private fun isTransientNetworkError(error: Throwable): Boolean =
        when (error) {
            is SocketTimeoutException,
            is UnknownHostException,
            is IOException,
            -> true
            else -> false
        }

    // ── User Profile ───────────────────────────────

    /**
     * Fetches the user profile from the Quran Foundation API and caches it.
     */
    suspend fun fetchAndCacheProfile() =
        withContext(Dispatchers.IO) {
            val accessToken = getValidAccessToken() ?: return@withContext

            try {
                val profile =
                    apiService.getUserProfile(
                        accessToken = accessToken,
                        clientId = QuranOAuthConfig.clientId,
                    )
                _userProfile.value = profile
                authStateManager.saveUserProfile(profile)
                Log.d(TAG, "Profile fetched: ${profile.firstName} ${profile.lastName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch profile", e)
                // If 401, try refresh and retry once
                if (e is retrofit2.HttpException && e.code() == 401) {
                    if (refreshTokens()) {
                        retryProfileFetch()
                    }
                }
            }
        }

    private suspend fun retryProfileFetch() {
        val accessToken = getValidAccessToken() ?: return
        try {
            val profile =
                apiService.getUserProfile(
                    accessToken = accessToken,
                    clientId = QuranOAuthConfig.clientId,
                )
            _userProfile.value = profile
            authStateManager.saveUserProfile(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Retry profile fetch failed", e)
        }
    }

    // ── Logout ─────────────────────────────────────

    /**
     * Clears all auth state and user data.
     */
    suspend fun logout() =
        withContext(Dispatchers.IO) {
            authStateManager.clearAuthState()
            _isAuthenticated.value = false
            _userProfile.value = null
        }
}
