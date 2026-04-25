package id.harissabil.hayah.data.api

import id.harissabil.hayah.BuildConfig
import id.harissabil.hayah.data.auth.QuranOAuthConfig
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
