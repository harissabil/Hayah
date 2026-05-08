package id.harissabil.hayah.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import id.harissabil.hayah.R
import id.harissabil.hayah.data.settings.KEY_DISCLOSURE_ACCEPTED
import id.harissabil.hayah.data.settings.KEY_DISCLOSURE_DECLINED
import id.harissabil.hayah.data.settings.hayahSettingsDataStore
import id.harissabil.hayah.service.ActivityRecognitionManager
import id.harissabil.hayah.service.HayahAccessibilityService
import id.harissabil.hayah.ui.screens.home.components.AccessibilityDisclosureDialog
import id.harissabil.hayah.ui.screens.home.components.AccessibilityTutorialDialog
import id.harissabil.hayah.ui.screens.home.components.InstantReflectionButton
import id.harissabil.hayah.ui.screens.home.components.InstantReflectionDialog
import id.harissabil.hayah.ui.screens.home.components.PeriodSelector
import id.harissabil.hayah.ui.screens.home.components.SpiritualRing
import id.harissabil.hayah.ui.theme.CairoFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val showAccessibilityTutorialDialog = remember { mutableStateOf(false) }
    val hasEvaluatedAccessibilityTutorial = remember { mutableStateOf(false) }
    val keyAccessibilityTutorialShown =
        remember {
            booleanPreferencesKey("accessibility_tutorial_shown_once")
        }

    val showAccessibilityDisclosureDialog = remember { mutableStateOf(false) }
    val hasEvaluatedDisclosure = remember { mutableStateOf(false) }

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

    suspend fun maybeShowAccessibilityTutorialOnce() {
        if (hasEvaluatedAccessibilityTutorial.value) return
        hasEvaluatedAccessibilityTutorial.value = true

        val prefs = context.hayahSettingsDataStore.data.first()
        val alreadyShown = prefs[keyAccessibilityTutorialShown] ?: false
        if (alreadyShown) return

        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityTutorialDialog.value = true
            // Persist immediately so it won't reappear even if user closes without enabling.
            context.hayahSettingsDataStore.edit {
                it[keyAccessibilityTutorialShown] = true
            }
        }
    }

    suspend fun maybeShowDisclosureOnce() {
        if (hasEvaluatedDisclosure.value) return
        hasEvaluatedDisclosure.value = true

        val prefs = context.hayahSettingsDataStore.data.first()
        val accepted = prefs[KEY_DISCLOSURE_ACCEPTED] ?: false
        val declined = prefs[KEY_DISCLOSURE_DECLINED] ?: false

        when {
            accepted -> maybeShowAccessibilityTutorialOnce()
            declined -> { /* User declined, do not request accessibility service */ }
            else -> showAccessibilityDisclosureDialog.value = true
        }
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
            coroutineScope.launch {
                maybeShowDisclosureOnce()
            }
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
            maybeShowDisclosureOnce()
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

    if (showAccessibilityDisclosureDialog.value) {
        AccessibilityDisclosureDialog(
            onAccept = {
                showAccessibilityDisclosureDialog.value = false
                coroutineScope.launch {
                    context.hayahSettingsDataStore.edit { it[KEY_DISCLOSURE_ACCEPTED] = true }
                    maybeShowAccessibilityTutorialOnce()
                }
            },
            onDecline = {
                showAccessibilityDisclosureDialog.value = false
                coroutineScope.launch {
                    context.hayahSettingsDataStore.edit { it[KEY_DISCLOSURE_DECLINED] = true }
                }
            },
        )
    }

    if (showAccessibilityTutorialDialog.value) {
        AccessibilityTutorialDialog(
            onClose = { showAccessibilityTutorialDialog.value = false },
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Greeting
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Assalamu'alaikum,",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = uiState.userName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                SpiritualRing(
                    pagesRead = uiState.pagesRead,
//                totalVerses = uiState.totalVerses
                )
                if (uiState.isPagesReadLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.pagesReadError != null && !uiState.isPagesReadLoading) {
                TextButton(onClick = { viewModel.retryFetchPagesRead() }) {
                    Text(text = "Retry", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { viewModel.onPeriodSelected(it) },
            )

            Spacer(modifier = Modifier.weight(1f))

            InstantReflectionButton(
                isLoading = uiState.isInstantReflectionLoading,
                onClick = {
                    viewModel.generateInstantReflection()
                },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
