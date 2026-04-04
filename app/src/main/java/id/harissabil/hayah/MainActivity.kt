package id.harissabil.hayah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.ui.navigation.HayahNavGraph
import id.harissabil.hayah.ui.screens.auth.AuthViewModel
import id.harissabil.hayah.ui.theme.HayahTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register the Activity Result launcher for the OAuth browser flow.
        // AppAuth opens a Custom Tab, the user logs in, and the result returns here.
        val authLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (data != null) {
                authViewModel.handleAuthResponse(data)
            }
        }

        setContent {
            val authUiState by authViewModel.authUiState.collectAsStateWithLifecycle()

            HayahTheme {
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
}