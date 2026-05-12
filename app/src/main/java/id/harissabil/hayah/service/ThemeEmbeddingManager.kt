package id.harissabil.hayah.service

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.sqrt

/**
 * Manages the MediaPipe Text Embedder for semantic theme detection.
 *
 * Handles model download, initialization, theme embedding pre-computation,
 * and cosine similarity matching against screen text.
 */
class ThemeEmbeddingManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "ThemeEmbedding"
        private const val MODEL_FILENAME = "universal_sentence_encoder.tflite"
        private const val MODEL_URL =
            "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
        private const val TEXT_SNIPPET_MAX_LENGTH = 500
    }

    data class MatchResult(
        val keyword: String,
        val similarity: Double,
    )

    private var textEmbedder: TextEmbedder? = null
    private var themeEmbeddings: List<Pair<String, FloatArray>> = emptyList()
    private var customEmbeddings: List<Pair<String, FloatArray>> = emptyList()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _lastInitError = MutableStateFlow<String?>(null)
    val lastInitError: StateFlow<String?> = _lastInitError.asStateFlow()

    private val modelFile: File
        get() = File(context.filesDir, MODEL_FILENAME)

    // ── Public API ───────────────────────────────

    /** Whether the model file has been downloaded to internal storage. */
    fun isModelAvailable(): Boolean = modelFile.exists() && modelFile.length() > 0

    /**
     * Downloads the USE model to internal storage.
     * Progress is exposed via [downloadProgress].
     */
    suspend fun downloadModel(): Boolean =
        withContext(Dispatchers.IO) {
            if (isModelAvailable()) {
                Log.d(TAG, "Model already downloaded")
                return@withContext true
            }

            _isDownloading.value = true
            _downloadProgress.value = 0f

            try {
                Log.d(TAG, "Downloading model from $MODEL_URL")
                val connection = URL(MODEL_URL).openConnection()
                connection.connect()

                val totalBytes = connection.contentLengthLong
                val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

                connection.getInputStream().buffered().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Long = 0
                        var count: Int

                        while (input.read(buffer).also { count = it } != -1) {
                            output.write(buffer, 0, count)
                            bytesRead += count
                            if (totalBytes > 0) {
                                _downloadProgress.value = bytesRead.toFloat() / totalBytes
                            }
                        }
                    }
                }

                // Atomic rename to avoid partial files
                tempFile.renameTo(modelFile)

                _downloadProgress.value = 1f
                Log.d(TAG, "Model downloaded successfully (${modelFile.length()} bytes)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Model download failed", e)
                // Clean up partial files
                File(context.filesDir, "$MODEL_FILENAME.tmp").delete()
                modelFile.delete()
                false
            } finally {
                _isDownloading.value = false
            }
        }

    /**
     * Initializes the TextEmbedder and pre-computes theme embeddings.
     * Must be called after model is downloaded and before [findBestMatch].
     * Returns true on success, false if the model is unavailable or initialization fails.
     */
    suspend fun initialize(customKeywords: Set<String> = emptySet()): Boolean =
        withContext(Dispatchers.IO) {
            if (!isModelAvailable()) {
                Log.w(TAG, "Cannot initialize — model not downloaded")
                return@withContext false
            }

            try {
                close()

                val baseOptions =
                    BaseOptions
                        .builder()
                        .setModelAssetPath(modelFile.absolutePath)
                        .build()

                val options =
                    TextEmbedder.TextEmbedderOptions
                        .builder()
                        .setBaseOptions(baseOptions)
                        .build()

                textEmbedder = TextEmbedder.createFromOptions(context, options)
                Log.d(TAG, "TextEmbedder initialized")

                computeThemeEmbeddings()
                updateCustomEmbeddings(customKeywords)
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize TextEmbedder", t)
                _lastInitError.value = "${t.javaClass.simpleName}: ${t.message}"
                textEmbedder = null
                false
            }
        }

    /**
     * Finds the theme with the highest cosine similarity to [text].
     *
     * @return The best [MatchResult] if similarity exceeds [threshold], or null.
     */
    fun findBestMatch(
        text: String,
        threshold: Float,
    ): MatchResult? {
        val embedder = textEmbedder ?: return null

        // Truncate to avoid excessive compute on huge UI tree text
        val snippet =
            text
                .take(TEXT_SNIPPET_MAX_LENGTH)
                .trim()
        if (snippet.isBlank()) return null

        return try {
            val textResult = embedder.embed(snippet)
            val textEmbedding =
                textResult
                    .embeddingResult()
                    .embeddings()[0]
                    .floatEmbedding()

            val allEmbeddings = themeEmbeddings + customEmbeddings

            var bestKeyword: String? = null
            var bestSimilarity = -1.0

            for ((keyword, themeVec) in allEmbeddings) {
                val sim = cosineSimilarity(textEmbedding, themeVec)
                if (sim > bestSimilarity) {
                    bestSimilarity = sim
                    bestKeyword = keyword
                }
            }

            Log.d(
                TAG,
                "Match: best='$bestKeyword' sim=${String.format("%.3f", bestSimilarity)} " +
                    "threshold=$threshold text=\"${snippet.take(80)}...\"",
            )

            if (bestKeyword != null && bestSimilarity >= threshold) {
                MatchResult(bestKeyword, bestSimilarity)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Embedding inference failed", e)
            null
        }
    }

    /** Updates custom keyword embeddings when the user adds/removes keywords. */
    fun updateCustomEmbeddings(keywords: Set<String>) {
        val embedder = textEmbedder ?: return
        customEmbeddings =
            keywords.mapNotNull { keyword ->
                try {
                    val result = embedder.embed(keyword)
                    val vec =
                        result
                            .embeddingResult()
                            .embeddings()[0]
                            .floatEmbedding()
                    keyword to vec
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to embed custom keyword '$keyword'", e)
                    null
                }
            }
        Log.d(TAG, "Updated ${customEmbeddings.size} custom keyword embeddings")
    }

    /** Releases the TextEmbedder resources. */
    fun close() {
        try {
            textEmbedder?.close()
        } catch (_: Exception) {
        }
        textEmbedder = null
        themeEmbeddings = emptyList()
        customEmbeddings = emptyList()
    }

    /** Whether the embedder is initialized and ready. */
    fun isReady(): Boolean = textEmbedder != null && themeEmbeddings.isNotEmpty()

    // ── Internal ─────────────────────────────────

    private fun computeThemeEmbeddings() {
        val embedder = textEmbedder ?: return
        val start = System.currentTimeMillis()

        themeEmbeddings =
            TriggerKeywords.THEME_DEFINITIONS.mapNotNull { theme ->
                try {
                    val result = embedder.embed(theme.description)
                    val vec =
                        result
                            .embeddingResult()
                            .embeddings()[0]
                            .floatEmbedding()
                    theme.keyword to vec
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to embed theme '${theme.keyword}'", e)
                    null
                }
            }

        val elapsed = System.currentTimeMillis() - start
        Log.d(TAG, "Pre-computed ${themeEmbeddings.size} theme embeddings in ${elapsed}ms")
    }

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
    ): Double {
        if (a.size != b.size) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0.0 else dot / denom
    }
}
