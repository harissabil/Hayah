package id.harissabil.hayah.data.model

import com.google.gson.annotations.SerializedName

/**
 * Quran Foundation user profile response.
 * Mirrors the JSON returned by GET /quran-reflect/v1/users/profile.
 */
data class UserProfileResponse(
    val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    val username: String? = null,
    val email: String? = null,
    val verified: Boolean = false,
    @SerializedName("avatarUrls") val avatarUrls: AvatarUrls? = null,
    @SerializedName("postsCount") val postsCount: Int = 0,
    @SerializedName("followersCount") val followersCount: Int = 0,
    @SerializedName("likesCount") val likesCount: Int = 0,
    @SerializedName("languageIsoCode") val languageIsoCode: String? = null,
    val bio: String? = null,
    val country: String? = null,
)

data class AvatarUrls(
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
)
