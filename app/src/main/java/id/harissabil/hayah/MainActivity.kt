package id.harissabil.hayah

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.service.ActivityRecognitionManager
import id.harissabil.hayah.ui.navigation.HayahNavGraph
import id.harissabil.hayah.ui.screens.auth.AuthViewModel
import id.harissabil.hayah.ui.screens.settings.AppTheme
import id.harissabil.hayah.ui.screens.settings.SettingsViewModel
import id.harissabil.hayah.ui.theme.HayahTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val activityRecognitionManager: ActivityRecognitionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register the Activity Result launcher for the OAuth browser flow.
        val authLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (data != null) {
                authViewModel.handleAuthResponse(data)
            }
        }

        // Request runtime permissions
        requestAppPermissions()

        setContent {
            val authUiState by authViewModel.authUiState.collectAsStateWithLifecycle()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (settingsUiState.appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            HayahTheme(
                darkTheme = isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HayahNavGraph(
                        authUiState = authUiState,
                        onLoginClick = {
                            val intent = authViewModel.buildAuthIntent()
                            authLauncher.launch(intent)
                        },
                        onLogout = { authViewModel.logout() },
                    )
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ACTIVITY_RECOGNITION (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val activityGranted = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
                if (activityGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    activityRecognitionManager.startTracking()
                }
            }
            launcher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted
            activityRecognitionManager.startTracking()
        }
    }
}