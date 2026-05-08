package id.harissabil.hayah.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class VerseRecommendationService(
    private val mcpQuranUrl: String,
) {
    companion object {
        private const val TAG = "VerseRecommendation"
    }

    private val gson = Gson()
    private val httpClient by lazy { HttpClient(OkHttp) { install(SSE) } }

    // ── Lazy models ───────────────────────────────

    private val reflectionModel by lazy {
        val schema =
            Schema.obj(
                mapOf(
                    "reflections" to
                        Schema.array(
                            Schema.obj(
                                mapOf(
                                    "verse_key" to Schema.string(),
                                    "reflection" to Schema.string(),
                                ),
                            ),
                        ),
                ),
            )
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite",
            generationConfig =
                generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                },
            systemInstruction = content { text(REFLECTION_SYSTEM_PROMPT) },
        )
    }

    // ── Public API ────────────────────────────────

    /**
     * Returns up to 5 Quran verse keys (e.g. "2:156") relevant to [keyword].
     * Connects to mcp.quran.ai via Streamable HTTP so Firebase AI can call real search tools.
     */
    suspend fun recommendVerses(keyword: String): List<String> =
        try {
            val mcpClient = Client(clientInfo = Implementation("hayah", "1.0.0"))
            val transport = StreamableHttpClientTransport(client = httpClient, url = mcpQuranUrl)
            mcpClient.connect(transport)
            try {
                val mcpTools = mcpClient.listTools().tools
                Log.d(TAG, "MCP tools available: ${mcpTools.joinToString { it.name }}")
                val functionDecls =
                    mcpTools.map { tool ->
                        FunctionDeclaration(
                            name = tool.name,
                            description = tool.description.orEmpty(),
                            parameters =
                                tool.inputSchema.properties
                                    ?.mapValues { (_, v) ->
                                        val obj = v.jsonObject
                                        val type = obj["type"]?.jsonPrimitive?.content ?: "string"
                                        val desc = obj["description"]?.jsonPrimitive?.content ?: ""
                                        when (type) {
                                            "integer" -> Schema.integer(desc)
                                            "number" -> Schema.double(desc)
                                            "boolean" -> Schema.boolean(desc)
                                            else -> Schema.string(desc)
                                        }
                                    } ?: emptyMap(),
                            optionalParameters =
                                tool.inputSchema.properties
                                    ?.keys
                                    ?.filter { it !in (tool.inputSchema.required ?: emptyList()) }
                                    ?: emptyList(),
                        )
                    }
                val model =
                    Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                        modelName = "gemini-3.1-flash-lite",
                        tools = listOf(Tool.functionDeclarations(functionDecls)),
                        systemInstruction = content { text(VERSE_SYSTEM_PROMPT) },
                    )
                val chat = model.startChat()
                var response =
                    chat.sendMessage(
                        "Search for Quranic verses most relevant to the keyword or theme: \"$keyword\". " +
                            "Use the available search tools to find real, verified verses. " +
                            "After searching, return ONLY a JSON array of exactly 5 verse keys, e.g.: " +
                            "[\"6:11\", \"29:20\", \"67:15\", \"22:27\", \"3:137\"]. " +
                            "Do not include any text outside the JSON array.",
                    )
                while (true) {
                    val calls = response.functionCalls
                    if (calls.isEmpty()) {
                        Log.d(TAG, "No more function calls — final response incoming")
                        break
                    }
                    val funcResponses =
                        calls.map { call ->
                            val argsJson = JsonObject(call.args)
                            Log.d(TAG, "Function call → ${call.name}($argsJson)")
                            val result = mcpClient.callTool(name = call.name, arguments = argsJson)
                            val resultText =
                                result.content.joinToString("\n") { part ->
                                    (part as? TextContent)?.text ?: ""
                                }
                            Log.d(TAG, "Function result ← ${call.name}: $resultText")
                            FunctionResponsePart(
                                name = call.name,
                                response = buildJsonObject { put("result", resultText) },
                            )
                        }
                    response =
                        chat.sendMessage(
                            content("function") {
                                funcResponses.forEach { part(it) }
                            },
                        )
                }
                Log.d(TAG, "Verse recommendation response: ${response.text}")
                parseVerseKeys(response.text ?: "")
            } finally {
                mcpClient.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recommend verses for '$keyword'", e)
            emptyList()
        }

    /**
     * Generates brief Islamic reflections for each verse, grounded in the actual translation text.
     */
    suspend fun generateReflections(
        keyword: String,
        versesWithTranslations: List<Pair<String, String>>,
        tafsir: Map<String, String>? = null,
    ): Map<String, String> =
        try {
            val versesBlock =
                versesWithTranslations.joinToString("\n") { (key, translation) ->
                    val tafsirLine = tafsir?.get(key)?.let { "\n  Tafsir: $it" } ?: ""
                    "- Verse $key: \"$translation\"$tafsirLine"
                }
            val prompt =
                """
                The keyword/theme is: "$keyword"

                For each verse below, write a brief Islamic reflection (2-3 sentences) connecting it
                to "$keyword" in daily life. Return JSON only:
                {"reflections":[{"verse_key":"...","reflection":"..."}]}

                $versesBlock
                """.trimIndent()
            val result = reflectionModel.generateContent(prompt)
            Log.d(TAG, "Reflection response: ${result.text}")
            parseReflections(result.text ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate reflections for '$keyword'", e)
            emptyMap()
        }

    // ── Parsing helpers ────────────────────────────────────────────

    private fun parseVerseKeys(text: String): List<String> {
        try {
            val start = text.indexOf('[')
            val end = text.lastIndexOf(']')
            if (start != -1 && end > start) {
                val arr = gson.fromJson(text.substring(start, end + 1), Array<String>::class.java)
                val valid = arr.filter { it.matches(Regex("\\d{1,3}:\\d{1,3}")) }
                if (valid.isNotEmpty()) return valid.take(5)
            }
        } catch (_: Exception) {
        }
        return Regex("\\b(\\d{1,3}:\\d{1,3})\\b")
            .findAll(text)
            .map { it.value }
            .distinct()
            .take(5)
            .toList()
    }

    private fun parseReflections(text: String): Map<String, String> {
        return try {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start == -1 || end <= start) return emptyMap()
            val result = gson.fromJson(text.substring(start, end + 1), ReflectionResult::class.java)
            result?.reflections?.associate { it.verse_key to it.reflection } ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ── Internal models ────────────────────────────────────────────

    private data class ReflectionResult(
        val reflections: List<ReflectionItem>? = null,
    )

    private data class ReflectionItem(
        val verse_key: String = "",
        val reflection: String = "",
    )
}

// ── System Prompts ────────────────────────────────────────────────

private const val VERSE_SYSTEM_PROMPT = """
You are an Islamic scholar assistant for the Hayah Quran reminder app. Recommend
relevant Quranic verses based on keywords from the user's daily life.

CRITICAL RULES:
1. ALWAYS use the available search tools to find verses — NEVER recall verse references
   from memory. Every verse key you return must be grounded in a tool call result.
2. Each verse reference MUST use the format "chapter:verse" (e.g. "2:255").
3. The verse must be directly or thematically relevant to the keyword.
4. Return exactly 5 different verses, preferably from different surahs.
5. Your FINAL response must be ONLY a JSON array of verse keys with no other text:
   ["2:255", "3:200", "67:15", "6:11", "29:20"]
6. Do not include translations, commentary, or any prose in your final response.

Valid chapter range: 1-114.
"""

private const val REFLECTION_SYSTEM_PROMPT = """
You are an Islamic scholar assistant creating brief, heartfelt reflections for the Hayah Quran app.

RULES:
1. Each reflection must be grounded in the actual verse text provided — do not add meaning
   that isn't there.
2. If a Tafsir excerpt is provided for a verse, ground your reflection in it. Do not contradict
   it. If no tafsir is provided, reflect only on the translation text.
3. Keep reflections to 2-3 sentences, written in a warm, personal, and spiritually uplifting tone.
4. Connect the verse to the keyword/theme in a way that feels relevant to modern daily life.
5. Be respectful of Islamic scholarship — do not make claims that contradict established tafsir.
6. Write in English, using "we" or "one" rather than "you" to feel inclusive.
7. Avoid generic platitudes — each reflection should feel unique to its verse.
"""
