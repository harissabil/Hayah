package id.harissabil.hayah.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.harissabil.hayah.R
import id.harissabil.hayah.service.ActivityRecognitionManager
import id.harissabil.hayah.service.HayahAccessibilityService
import id.harissabil.hayah.ui.screens.home.components.AccessibilityDisclosureDialog
import id.harissabil.hayah.ui.screens.home.components.AccessibilityTutorialDialog
import id.harissabil.hayah.ui.screens.home.components.InstantReflectionButton
import id.harissabil.hayah.ui.screens.home.components.InstantReflectionDialog
import id.harissabil.hayah.ui.screens.home.components.PeriodSelector
import id.harissabil.hayah.ui.screens.home.components.SpiritualRing
import id.harissabil.hayah.ui.theme.CairoFamily
import id.harissabil.hayah.ui.theme.HayahTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val activityRecognitionManager: ActivityRecognitionManager = koinInject()

    fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName =
            "${context.packageName}/${HayahAccessibilityService::class.java.canonicalName}"
        val enabledServices =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        return enabledServices?.contains(serviceName) == true
    }

    // 1. Set up the Compose Permission Launcher
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val activityGranted = results[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
            if (activityGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                activityRecognitionManager.startTracking()
            }
            viewModel.evaluateInitialDialogs(isAccessibilityServiceEnabled())
        }

    // 2. Trigger the permission check safely when the screen loads
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()

        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ACTIVITY_RECOGNITION (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted
            activityRecognitionManager.startTracking()
            viewModel.evaluateInitialDialogs(isAccessibilityServiceEnabled())
        }
    }

    LaunchedEffect(uiState.instantReflectionError) {
        val error = uiState.instantReflectionError
        if (error != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Retry",
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.generateInstantReflection()
            } else {
                viewModel.dismissInstantReflectionError()
            }
        }
    }

    if (uiState.newlyGeneratedEntry != null) {
        InstantReflectionDialog(
            entry = uiState.newlyGeneratedEntry!!,
            onDismiss = {
                viewModel.dismissNewlyGeneratedEntry()
            },
        )
    }

    if (uiState.showDisclosureDialog) {
        AccessibilityDisclosureDialog(
            onAccept = { viewModel.acceptDisclosure(isAccessibilityServiceEnabled()) },
            onDecline = { viewModel.declineDisclosure() },
        )
    }

    if (uiState.showTutorialDialog) {
        AccessibilityTutorialDialog(
            onClose = { viewModel.dismissTutorial() },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_hayah_transparent),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Hayah",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = CairoFamily,
                        )
                    }
                },
                actions = {
                    // User profile photo or fallback icon
                    if (uiState.profilePhotoUrl != null) {
                        AsyncImage(
                            model = uiState.profilePhotoUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                "Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp),
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        HomeScreenContent(
            uiState = uiState,
            onPeriodSelected = { viewModel.onPeriodSelected(it) },
            onInstantReflection = { viewModel.generateInstantReflection() },
            onRetry = { viewModel.retryFetchPagesRead() },
            paddingValues = paddingValues,
        )
    }
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onPeriodSelected: (Period) -> Unit,
    onInstantReflection: () -> Unit,
    onRetry: () -> Unit,
    paddingValues: PaddingValues,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
    ) {
        val isSmallScreen = maxHeight < 600.dp
        val ringSize = if (isSmallScreen) 220.dp else 280.dp
        val topSpacing = if (isSmallScreen) 12.dp else 24.dp
        val greetingSize = if (isSmallScreen) 26.sp else 34.sp
        val nameSize = if (isSmallScreen) 18.sp else 22.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(topSpacing))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Assalamu'alaikum,",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = greetingSize),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = uiState.userName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = nameSize),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                SpiritualRing(
                    pagesRead = uiState.pagesRead,
                    size = ringSize,
                )
                if (uiState.isPagesReadLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.pagesReadError != null && !uiState.isPagesReadLoading) {
                TextButton(onClick = onRetry) {
                    Text(text = "Retry", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = onPeriodSelected,
            )

            Spacer(modifier = Modifier.weight(1f))

            InstantReflectionButton(
                isLoading = uiState.isInstantReflectionLoading,
                onClick = onInstantReflection,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(
    name = "Small Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=360dp,height=640dp,dpi=320,isRound=false,chinSize=0dp,orientation=portrait",
)
@Preview(
    name = "Small Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=360dp,height=640dp,dpi=320,isRound=false,chinSize=0dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Normal Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=393dp,height=851dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait",
)
@Preview(
    name = "Normal Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=393dp,height=851dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenPreview() {
    HayahTheme {
        Scaffold { padding ->
            HomeScreenContent(
                uiState =
                    HomeUiState(
                        userName = "Muhammad Haris",
                        pagesRead = 1000,
                        selectedPeriod = Period.THIS_WEEK,
                    ),
                onPeriodSelected = {},
                onInstantReflection = {},
                onRetry = {},
                paddingValues = padding,
            )
        }
    }
}
