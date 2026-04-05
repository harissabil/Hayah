package id.harissabil.hayah.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koin.java.KoinJavaComponent.inject

class OnBootReceiver : BroadcastReceiver() {
    val activityRecognitionManager: ActivityRecognitionManager by inject(ActivityRecognitionManager::class.java)

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return

        val canTrack = context != null && (
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACTIVITY_RECOGNITION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )

        if (canTrack) {
            activityRecognitionManager.startTracking()
        } else {
            Log.w("OnBootReceiver", "ACTIVITY_RECOGNITION not granted; skip startTracking")
        }
    }
}