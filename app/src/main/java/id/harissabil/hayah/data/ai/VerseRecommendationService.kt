package id.harissabil.hayah.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.gson.Gson

/**
 * Uses Firebase AI Logic (Gemini) to:
 * 1. Recommend 5 relevant Quran verse keys for a given keyword
 * 2. Generate Islamic reflections for each verse
 *
 * Model: gemini-3.1-flash-lite-preview (using the latest available model name)
 */
class VerseRecommendationService {

    companion object {
        private const val TAG = "VerseRecommendation"
    }

    private val gson = Gson()

    // ── Model for verse recommendations ───────────

    private val verseModel by lazy {
        val schema = Schema.obj(
            mapOf(
                "keyword" to Schema.string(),
                "verses" to Schema.array(Schema.string()),
            )
        )

        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-3.1-flash-lite-preview",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                },
                systemInstruction = content {
                    text(VERSE_SYSTEM_PROMPT)
                },
            )
    }

    // ── Model for reflections ─────────────────────

    private val reflectionModel by lazy {
        val schema = Schema.obj(
            mapOf(
                "reflections" to Schema.array(
                    Schema.obj(
                        mapOf(
                            "verse_key" to Schema.string(),
                            "reflection" to Schema.string(),
                        )
                    )
                ),
            )
        )

        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-3.1-flash-lite-preview",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                },
                systemInstruction = content {
                    text(REFLECTION_SYSTEM_PROMPT)
                },
            )
    }

    // ── Public API ────────────────────────────────

    /**
     * Returns up to 5 Quran verse keys (e.g. "2:156", "3:185") relevant to [keyword].
     */
    suspend fun recommendVerses(keyword: String): List<String> {
        return try {
            val response = verseModel.generateContent(
                "Find 5 Quranic verses most relevant to the keyword or theme: \"$keyword\". " +
                "Return only verses that genuinely exist in the Quran. " +
                "Format each verse as chapter:verse (e.g. 2:255)."
            )

            val json = response.text ?: return emptyList()
            Log.d(TAG, "Verse recommendation response: $json")

            val result = gson.fromJson(json, VerseRecommendation::class.java)
            result?.verses?.take(5) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recommend verses for '$keyword'", e)
            emptyList()
        }
    }

    /**
     * Generates brief Islamic reflections for each verse, grounded in the actual translation text.
     */
    suspend fun generateReflections(
        keyword: String,
        versesWithTranslations: List<Pair<String, String>>, // (verseKey, translationText)
    ): Map<String, String> {
        return try {
            val versesBlock = versesWithTranslations.joinToString("\n") { (key, translation) ->
                "- Verse $key: \"$translation\""
            }

            val prompt = """
                The keyword/theme is: "$keyword"
                
                For each of the following Quranic verses, write a brief, thoughtful Islamic reflection 
                (2-3 sentences) connecting the verse to the theme of "$keyword" in daily life:
                
                $versesBlock
            """.trimIndent()

            val response = reflectionModel.generateContent(prompt)
            val json = response.text ?: return emptyMap()
            Log.d(TAG, "Reflection response: $json")

            val result = gson.fromJson(json, ReflectionResult::class.java)
            result?.reflections?.associate { it.verseKey to it.reflection } ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate reflections for '$keyword'", e)
            emptyMap()
        }
    }

    // ── Internal models ───────────────────────────

    private data class VerseRecommendation(
        val keyword: String? = null,
        val verses: List<String>? = null,
    )

    private data class ReflectionResult(
        val reflections: List<ReflectionItem>? = null,
    )

    private data class ReflectionItem(
        val verse_key: String = "",
        val reflection: String = "",
    ) {
        val verseKey: String get() = verse_key
    }
}

// ── System Prompts ────────────────────────────────

private const val VERSE_SYSTEM_PROMPT = """
You are an Islamic scholar assistant for the Hayah Quran reminder app. Your role is to recommend 
relevant Quranic verses based on keywords detected from the user's daily life (activities, 
notifications, screen content).

CRITICAL RULES:
1. ONLY recommend verses that GENUINELY EXIST in the Holy Quran. Never fabricate or hallucinate 
   verse references. If you are uncertain whether a verse exists, do not include it.
2. Each verse reference MUST use the format "chapter:verse" (e.g. "2:255" for Ayat al-Kursi).
3. The verse must be directly or thematically relevant to the given keyword.
4. Recommend exactly 5 different verses, preferably from different surahs.
5. Prioritize well-known, impactful verses that provide genuine spiritual benefit.
6. Consider the full spectrum of Quranic wisdom: comfort, guidance, warning, gratitude, patience, etc.

Valid chapter range: 1-114. Verify verse numbers are within the actual ayah count of each surah.
"""

private const val REFLECTION_SYSTEM_PROMPT = """
You are an Islamic scholar assistant creating brief, heartfelt reflections for the Hayah Quran app.

RULES:
1. Each reflection must be grounded in the actual verse text provided — do not add meaning that 
   isn't there.
2. Keep reflections to 2-3 sentences, written in a warm, personal, and spiritually uplifting tone.
3. Connect the verse to the keyword/theme in a way that feels relevant to modern daily life.
4. Be respectful of Islamic scholarship — do not make claims that contradict established tafsir.
5. Write in English, using "we" or "one" rather than "you" to feel inclusive.
6. Avoid generic platitudes — each reflection should feel unique to its verse.
"""
