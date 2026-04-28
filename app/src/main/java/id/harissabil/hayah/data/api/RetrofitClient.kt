package id.harissabil.hayah.data.api

import id.harissabil.hayah.BuildConfig
import id.harissabil.hayah.data.auth.AuthRepository
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Refreshes the access token on a 401 response and retries the request once.
 *
 * Uses lazy Koin injection to avoid a circular dependency:
 * RetrofitClient creates this authenticator before AuthRepository exists,
 * but the Koin delegate only resolves AuthRepository on the first 401.
 */
class TokenAuthenticator :
    Authenticator,
    KoinComponent {
    private val authRepository: AuthRepository by inject()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.code != 401) return null
        if (response.priorResponse?.code == 401) return null

        val refreshed = runBlocking { authRepository.refreshTokens() }
        if (!refreshed) return null

        val newToken = runBlocking { authRepository.getValidAccessToken() } ?: return null
        return response.request
            .newBuilder()
            .header("x-auth-token", newToken)
            .build()
    }
}

/**
 * Singleton factory for creating the [QuranApiService] Retrofit instance.
 */
object RetrofitClient {
    fun create(): QuranApiService {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }

        val client =
            OkHttpClient
                .Builder()
                .authenticator(TokenAuthenticator())
                .addInterceptor(logging)
                .retryOnConnectionFailure(true)
                .connectionPool(ConnectionPool(5, 2, TimeUnit.MINUTES))
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(40, TimeUnit.SECONDS)
                .pingInterval(15, TimeUnit.SECONDS)
                .build()

        return Retrofit
            .Builder()
            .baseUrl(QuranOAuthConfig.apiBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranApiService::class.java)
    }
}
