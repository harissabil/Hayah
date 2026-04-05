package id.harissabil.hayah.data.model

import com.google.gson.annotations.SerializedName

// ── Verse by key response ─────────────────────────

data class VerseByKeyResponse(
    val verse: VerseDetail? = null,
)

data class VerseDetail(
    val id: Int? = null,
    @SerializedName("chapter_id") val chapterId: Int? = null,
    @SerializedName("verse_number") val verseNumber: Int? = null,
    @SerializedName("verse_key") val verseKey: String? = null,
    @SerializedName("text_uthmani") val textUthmani: String? = null,
    @SerializedName("page_number") val pageNumber: Int? = null,
    val translations: List<TranslationItem>? = null,
)

data class TranslationItem(
    val id: Int? = null,
    @SerializedName("resource_id") val resourceId: Int? = null,
    val text: String? = null,
)

// ── Audio recitation response ─────────────────────

data class AudioRecitationResponse(
    @SerializedName("audio_files") val audioFiles: List<AudioFile>? = null,
)

data class AudioFile(
    val url: String? = null,
    @SerializedName("verse_key") val verseKey: String? = null,
)

// ── Surah names lookup ────────────────────────────

data class ChaptersResponse(
    val chapters: List<ChapterInfo>? = null,
)

data class ChapterInfo(
    val id: Int? = null,
    @SerializedName("name_simple") val nameSimple: String? = null,
)

// ── Recitations lookup ───────────────────────────

data class RecitationsResponse(
    val recitations: List<RecitationItem>? = null,
)

data class RecitationItem(
    val id: Int? = null,
    @SerializedName("reciter_name") val reciterName: String? = null,
    val style: String? = null,
    @SerializedName("translated_name") val translatedName: TranslatedReciterName? = null,
)

data class TranslatedReciterName(
    val name: String? = null,
    @SerializedName("language_name") val languageName: String? = null,
)

// ── Verses by page response ───────────────────────

data class VersesByPageResponse(
    val verses: List<VerseDetail>? = null,
)

// ── Activity day request ──────────────────────────

data class ActivityDayRequest(
    val date: String,
    val type: String = "QURAN",
    val seconds: Int,
    val ranges: List<String>,
    val mushafId: Int = 4,
)
