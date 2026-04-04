package id.harissabil.hayah.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * Manages registration / de-registration of Activity Transition updates
 * via Google Play Services.
 */
class ActivityRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "ActivityRecognition"
        private const val REQUEST_CODE = 9001
    }

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Registers for ENTER transitions of the 4 activity types.
     * Call this after ACTIVITY_RECOGNITION permission is granted.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun startTracking() {
        val transitions = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
        ).flatMap { activityType ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activityType)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
            )
        }

        val request = ActivityTransitionRequest(transitions)

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Activity transition tracking started")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start activity transition tracking", e)
            }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    fun stopTracking() {
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                pendingIntent.cancel()
                Log.d(TAG, "Activity transition tracking stopped")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to stop activity transition tracking", e)
            }
    }
}
