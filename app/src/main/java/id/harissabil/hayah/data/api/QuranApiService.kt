package id.harissabil.hayah.data.api

import id.harissabil.hayah.data.model.UserProfileResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit interface for Quran Foundation content/user APIs.
 */
interface QuranApiService {

    @GET("quran-reflect/v1/users/profile")
    suspend fun getUserProfile(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
    ): UserProfileResponse
}
