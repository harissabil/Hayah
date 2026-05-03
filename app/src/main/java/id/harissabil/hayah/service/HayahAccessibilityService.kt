package id.harissabil.hayah.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import id.harissabil.hayah.data.settings.KEY_CUSTOM_KEYWORDS
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
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
        private const val SCAN_COOLDOWN_MS = 1000L // avoid processing too frequently
        private const val KEYWORD_RETRIGGER_COOLDOWN_MS = 3000L

        private val EXCLUDED_PACKAGES =
            setOf(
                "id.harissabil.hayah",
                "com.android.systemui",
                "com.android.settings",
            )
    }

    private val orchestrator: ReminderOrchestrator by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var customKeywords: Set<String> = emptySet()
    private var lastScanTime = 0L
    private val recentKeywordTimes = mutableMapOf<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val now = System.currentTimeMillis()
        if (now - lastScanTime < SCAN_COOLDOWN_MS) return

        val packageName = event.packageName?.toString()
        if (packageName in EXCLUDED_PACKAGES) {
            return
        }

//        val text = extractText(event)
//        if (text.isBlank()) return

        val eventText = extractText(event)
        val rootText = extractTextFromNode(rootInActiveWindow)

        val combinedText = "$eventText $rootText".trim()

        if (combinedText.isBlank()) {
            Log.d(TAG, "TEXT EMPTY")
            return
        }

        lastScanTime = now

        // Scan for trigger keywords
        val lowerText = combinedText.lowercase()
        val allKeywords = TriggerKeywords.ISLAMIC_KEYWORDS + customKeywords
        for (keyword in allKeywords) {
            if (!lowerText.contains(keyword)) continue

            val lastSeen = recentKeywordTimes[keyword] ?: 0L
            if (now - lastSeen < KEYWORD_RETRIGGER_COOLDOWN_MS) continue

            Log.d(TAG, "Keyword '$keyword' detected in ${eventTypeName(event.eventType)}")
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

        serviceScope.launch {
            applicationContext.hayahSettingsDataStore.data.collect { prefs ->
                customKeywords = prefs[KEY_CUSTOM_KEYWORDS] ?: emptySet()
            }
        }

        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

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
