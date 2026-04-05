package id.harissabil.hayah.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives Activity Transition events from Google Play Services and
 * maps them to keywords for [ReminderOrchestrator].
 */
class ActivityTransitionReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "ActivityTransition"
    }

    private val orchestrator: ReminderOrchestrator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val activityName = when (event.activityType) {
                DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
                DetectedActivity.RUNNING -> "RUNNING"
                DetectedActivity.WALKING -> "WALKING"
                else -> null
            }

            Log.d(
                TAG,
                "Detected activity transition: type=${event.activityType} → $activityName, transition=${event.transitionType}"
            )

            if (activityName != null) {
                val keyword = TriggerKeywords.ACTIVITY_KEYWORDS[activityName]
                if (keyword != null) {
                    Log.d(TAG, "Activity detected: $activityName → keyword '$keyword'")
                    orchestrator.onKeywordDetected(keyword)
                }
            }
        }
    }
}
