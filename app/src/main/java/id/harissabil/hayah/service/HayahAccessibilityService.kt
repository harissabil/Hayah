package id.harissabil.hayah.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.edit
import id.harissabil.hayah.data.settings.SettingsRepository
import id.harissabil.hayah.ui.screens.settings.DetectionMode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Accessibility Service that scans notification text and on-screen content
 * for trigger keywords, then forwards them to [ReminderOrchestrator].
 *
 * Supports two detection modes:
 * - **Keywords**: Exact keyword matching against a predefined set (default)
 * - **Semantic**: Cosine similarity via MediaPipe text embeddings
 *
 * Listens to:
 * - TYPE_NOTIFICATION_STATE_CHANGED (notification content)
 * - TYPE_WINDOW_CONTENT_CHANGED (screen text)
 */
@SuppressLint("AccessibilityPolicy")
class HayahAccessibilityService :
    AccessibilityService(),
    KoinComponent {
    companion object {
        private const val TAG = "HayahAccessibility"
        private const val SCAN_COOLDOWN_KEYWORDS_MS = 1000L
        private const val SCAN_COOLDOWN_SEMANTIC_MS = 3000L
        private const val KEYWORD_RETRIGGER_COOLDOWN_MS = 3000L
        private const val KEY_EMBED_INIT_ATTEMPT = "embed_init_in_progress"

        private val EXCLUDED_PACKAGES =
            setOf(
                "id.harissabil.hayah",
                "com.android.systemui",
                "com.android.settings",
            )
    }

    private val orchestrator: ReminderOrchestrator by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val themeEmbeddingManager: ThemeEmbeddingManager by inject()
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, t ->
                Log.e(TAG, "Unhandled exception in service scope", t)
            },
    )

    private val recoveryPrefs by lazy {
        getSharedPreferences("hayah_recovery", MODE_PRIVATE)
    }

    private var customKeywords: Set<String> = emptySet()
    private var detectionMode: DetectionMode = DetectionMode.KEYWORDS
    private var similarityThreshold: Float = 0.5f
    private var lastScanTime = 0L
    private var lastTextHash = 0
    private val recentKeywordTimes = mutableMapOf<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString()
        if (packageName in EXCLUDED_PACKAGES) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        if (rootNode.packageName == null || rootNode.packageName in EXCLUDED_PACKAGES) {
            return
        }

        val now = System.currentTimeMillis()
        val cooldown =
            if (detectionMode == DetectionMode.SEMANTIC) {
                SCAN_COOLDOWN_SEMANTIC_MS
            } else {
                SCAN_COOLDOWN_KEYWORDS_MS
            }
        if (now - lastScanTime < cooldown) return

        val eventText = extractText(event)
        val rootText = extractTextFromNode(rootInActiveWindow)
        val combinedText = "$eventText $rootText".trim()

        if (combinedText.isBlank()) {
            Log.d(TAG, "TEXT EMPTY")
            return
        }

        lastScanTime = now

        when (detectionMode) {
            DetectionMode.KEYWORDS -> scanWithKeywords(combinedText, now, event.eventType)
            DetectionMode.SEMANTIC -> scanWithEmbeddings(combinedText, now, event.eventType)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info =
            AccessibilityServiceInfo().apply {
                eventTypes =
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED

                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                notificationTimeout = 100
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            }

        serviceInfo = info

        // Collect settings changes
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { prefs ->
                customKeywords = prefs[SettingsRepository.KEY_CUSTOM_KEYWORDS] ?: emptySet()

                val modeStr = prefs[SettingsRepository.KEY_DETECTION_MODE]
                val newMode =
                    modeStr?.let { str ->
                        DetectionMode.entries.find { it.name == str }
                    } ?: DetectionMode.KEYWORDS
                val modeChanged = newMode != detectionMode
                detectionMode = newMode

                similarityThreshold = prefs[SettingsRepository.KEY_SIMILARITY_THRESHOLD] ?: 0.75f

                // Initialize embedder when switching to semantic mode
                if (modeChanged && detectionMode == DetectionMode.SEMANTIC) {
                    initializeEmbedder()
                }

                // Update custom keyword embeddings if embedder is active
                if (themeEmbeddingManager.isReady()) {
                    themeEmbeddingManager.updateCustomEmbeddings(customKeywords)
                }
            }
        }

        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        themeEmbeddingManager.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Detection modes ──────────────────────────

    private fun scanWithKeywords(
        combinedText: String,
        now: Long,
        eventType: Int,
    ) {
        val lowerText = combinedText.lowercase()
        val allKeywords = TriggerKeywords.ISLAMIC_KEYWORDS + customKeywords
        for (keyword in allKeywords) {
            if (!lowerText.contains(keyword)) continue

            val lastSeen = recentKeywordTimes[keyword] ?: 0L
            if (now - lastSeen < KEYWORD_RETRIGGER_COOLDOWN_MS) continue

            Log.d(TAG, "Keyword '$keyword' detected in ${eventTypeName(eventType)}")
            recentKeywordTimes[keyword] = now
            orchestrator.onKeywordDetected(keyword)
            // Only trigger one keyword per scan to avoid spam
            break
        }

        // Periodic cleanup for stale entries
        if (recentKeywordTimes.size > 100) {
            val cutoff = now - (KEYWORD_RETRIGGER_COOLDOWN_MS * 4)
            recentKeywordTimes.entries.removeAll { it.value < cutoff }
        }
    }

    private fun scanWithEmbeddings(
        combinedText: String,
        now: Long,
        eventType: Int,
    ) {
        if (!themeEmbeddingManager.isReady()) {
            Log.d(TAG, "Embedder not ready, attempting initialization")
            serviceScope.launch { initializeEmbedder() }
            return
        }

        // Skip if the text hasn't changed (avoids redundant inference)
        val textHash = combinedText.hashCode()
        if (textHash == lastTextHash) return
        lastTextHash = textHash

        val match = themeEmbeddingManager.findBestMatch(combinedText, similarityThreshold)
        if (match != null) {
            Log.d(
                TAG,
                "Semantic match '${match.keyword}' (${String.format("%.1f", match.similarity * 100)}%) " +
                    "in ${eventTypeName(eventType)}",
            )
            orchestrator.onKeywordDetected(match.keyword)
        }
    }

    private suspend fun initializeEmbedder() {
        if (themeEmbeddingManager.isReady()) return
        if (!themeEmbeddingManager.isModelAvailable()) {
            Log.w(TAG, "Semantic mode selected but model not downloaded")
            return
        }

        // Crash-loop guard: if the previous init attempt crashed (flag survived the process kill),
        // revert to keywords mode so the service doesn't restart into the same crash.
        if (recoveryPrefs.getBoolean(KEY_EMBED_INIT_ATTEMPT, false)) {
            Log.e(TAG, "Previous embedder init crashed — reverting to keywords mode")
            recoveryPrefs.edit(commit = true) { remove(KEY_EMBED_INIT_ATTEMPT) }
            settingsRepository.set(SettingsRepository.KEY_DETECTION_MODE, DetectionMode.KEYWORDS.name)
            return
        }

        // Synchronous write so the flag is on disk before the crash-prone code runs.
        recoveryPrefs.edit(commit = true) { putBoolean(KEY_EMBED_INIT_ATTEMPT, true) }

        Log.d(TAG, "Initializing embedder for semantic detection")
        val success = themeEmbeddingManager.initialize(customKeywords)

        recoveryPrefs.edit(commit = true) {remove(KEY_EMBED_INIT_ATTEMPT)}

        if (!success) {
            Log.e(TAG, "Embedder init failed — reverting to keywords mode")
            settingsRepository.set(SettingsRepository.KEY_DETECTION_MODE, DetectionMode.KEYWORDS.name)
        }
    }

    // ── Text extraction ──────────────────────────

    private fun extractText(event: AccessibilityEvent): String {
        val builder = StringBuilder()

        // Direct event text
        event.text.forEach { cs ->
            if (!cs.isNullOrBlank()) {
                builder.append(cs).append(" ")
            }
        }

        // Content description
        event.contentDescription?.let {
            if (it.isNotBlank()) builder.append(it).append(" ")
        }

        return builder.toString().trim()
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""

        val builder = StringBuilder()

        node.text?.let {
            if (it.isNotBlank()) builder.append(it).append(" ")
        }

        node.contentDescription?.let {
            if (it.isNotBlank()) builder.append(it).append(" ")
        }

        for (i in 0 until node.childCount) {
            builder.append(extractTextFromNode(node.getChild(i)))
        }

        return builder.toString()
    }

    private fun eventTypeName(type: Int): String =
        when (type) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "notification"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "screen_content"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "screen_content"
            else -> "event_$type"
        }
}
