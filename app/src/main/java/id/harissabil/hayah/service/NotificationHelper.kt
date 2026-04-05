package id.harissabil.hayah.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.harissabil.hayah.MainActivity
import id.harissabil.hayah.R
import id.harissabil.hayah.data.model.CachedVerse

/**
 * Builds and shows rich notifications for Quranic reminders.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "hayah_reminders"
        private const val CHANNEL_NAME = "Quranic Reminders"
        private const val TAG = "NotificationHelper"
        private var notificationId = 1000
    }

    private var mediaPlayer: MediaPlayer? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Contextual Quranic verse reminders based on your daily activities"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows a rich notification with the verse content.
     *
     * @param keyword   The trigger keyword (shown in title)
     * @param verse     The cached verse data
     * @param playAudio Whether to auto-play the audio when the notification shows
     */
    fun showVerseNotification(
        keyword: String,
        verse: CachedVerse,
        playAudio: Boolean,
    ) {
        val id = notificationId++

        // Tap intent → opens app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("verse_key", verse.verseKey)
        }
        val tapPending = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build big text: Arabic + translation
        val bigText = buildString {
            append(verse.reflection)
            append("\n\n")
            append(verse.textUthmani)
            append("\n\n")
            append(verse.translation)
            append("\n\n— ${verse.surahName} (${verse.verseKey})")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Reminder for you ($keyword)")
            .setContentText(verse.reflection)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setSummaryText(verse.reflection)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
            Log.d(TAG, "Notification shown for '$keyword' → ${verse.verseKey}")
        } catch (e: SecurityException) {
            Log.e(TAG, "POST_NOTIFICATIONS permission not granted", e)
        }

        // Auto-play audio if enabled
        if (playAudio && !verse.audioUrl.isNullOrBlank()) {
            playVerseAudio(verse.audioUrl)
        }
    }

    fun playVerseAudio(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: $url", e)
        }
    }

    fun stopVerseAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio", e)
        }
    }
}
