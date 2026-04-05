package id.harissabil.hayah.data.api

import id.harissabil.hayah.data.model.ActivityDayRequest
import id.harissabil.hayah.data.model.AudioRecitationResponse
import id.harissabil.hayah.data.model.ChaptersResponse
import id.harissabil.hayah.data.model.RecitationsResponse
import id.harissabil.hayah.data.model.UserProfileResponse
import id.harissabil.hayah.data.model.VerseByKeyResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Quran Foundation content/user APIs.
 */
interface QuranApiService {

    @GET("quran-reflect/v1/users/profile")
    suspend fun getUserProfile(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
    ): UserProfileResponse

    @GET("content/api/v4/verses/by_key/{verse_key}")
    suspend fun getVerseByKey(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Path("verse_key") verseKey: String,
        @Query("translations") translations: String = "20", // Sahih International
        @Query("fields") fields: String = "text_uthmani",
        @Query("language") language: String = "en",
    ): VerseByKeyResponse

    @GET("content/api/v4/verses/random")
    suspend fun getRandomVerse(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Query("translations") translations: String = "20",
        @Query("fields") fields: String = "text_uthmani",
        @Query("language") language: String = "en",
    ): VerseByKeyResponse

    @GET("content/api/v4/recitations/{recitation_id}/by_ayah/{verse_key}")
    suspend fun getAudioForVerse(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Path("recitation_id") recitationId: Int,
        @Path("verse_key") verseKey: String,
    ): AudioRecitationResponse

    @GET("content/api/v4/chapters")
    suspend fun getChapters(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Query("language") language: String = "en",
    ): ChaptersResponse

    @GET("content/api/v4/resources/recitations")
    suspend fun getRecitations(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Query("language") language: String = "en",
    ): RecitationsResponse

    @GET("content/api/v4/verses/by_page/{page_number}")
    suspend fun getVersesByPage(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Path("page_number") pageNumber: Int,
        @Query("translations") translations: String = "20",
        @Query("fields") fields: String = "text_uthmani",
        @Query("language") language: String = "en",
    ): id.harissabil.hayah.data.model.VersesByPageResponse

    @POST("auth/v1/activity-days")
    suspend fun postActivityDays(
        @Header("x-auth-token") accessToken: String,
        @Header("x-client-id") clientId: String,
        @Header("x-timezone") timezone: String,
        @Body request: ActivityDayRequest,
    )
}
