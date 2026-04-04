package id.harissabil.hayah.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.ui.screens.settings.components.AppearancePicker
import id.harissabil.hayah.ui.screens.settings.components.PermissionRow
import id.harissabil.hayah.ui.screens.settings.components.SettingsIconBox
import id.harissabil.hayah.ui.screens.settings.components.SettingsNavRow
import id.harissabil.hayah.ui.screens.settings.components.SettingsSection
import id.harissabil.hayah.ui.screens.settings.components.SettingsSectionLabel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

    // Refresh accessibility state when screen resumes (user might toggle it in system settings)
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.refreshAccessibilityState()
        }
    }

    // Reciter Picker Dialog
    if (uiState.isReciterDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onReciterDialogDismissed,
            title = { Text("Choose Reciter") },
            text = {
                when {
                    uiState.isRecitersLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.reciterError != null -> {
                        uiState.reciterError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    uiState.reciterOptions.isEmpty() -> {
                        Text("No reciters available.")
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            uiState.reciterOptions.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            viewModel.onReciterSelected(option.id, option.name)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (option.id == uiState.reciterId) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onReciterDialogDismissed) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Settings", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Personalization
            SettingsSection(label = "Personalization") {
                SettingsNavRow(
                    icon = Icons.Filled.RecordVoiceOver,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Preferred Reciter",
                    subtitle = uiState.reciterName,
                    onClick = viewModel::onReciterPickerRequested
                )
            }

            // Notifications
            SettingsSection(label = "Notifications") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBox(
                            icon = Icons.Filled.PlayCircle,
                            bg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Play Audio Instantly",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "When notification pops up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.playAudioInstantly,
                        onCheckedChange = viewModel::onPlayAudioToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outlineVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBox(
                                icon = Icons.Filled.NotificationsActive,
                                bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Max Reminders",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Daily nudge frequency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            uiState.maxReminders.toInt().toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.maxReminders,
                        onValueChange = viewModel::onMaxRemindersChanged,
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }

            // Appearance
            Column {
                SettingsSectionLabel("Appearance")
                Spacer(modifier = Modifier.height(12.dp))
                AppearancePicker(
                    selected = uiState.appTheme,
                    onSelect = viewModel::onThemeSelected
                )
            }

            // Permissions Health
            SettingsSection(label = "Permissions Health") {
                PermissionRow(
                    iconBg = if (uiState.isAccessibilityEnabled) Color(0xFFDCFCE7) else Color(
                        0xFFFFF7ED
                    ),
                    iconTint = if (uiState.isAccessibilityEnabled) Color(0xFF16A34A) else Color(
                        0xFFEA580C
                    ),
                    icon = if (uiState.isAccessibilityEnabled) Icons.Filled.CheckCircle else Icons.Filled.Accessibility,
                    title = "Accessibility Service",
                    subtitle = if (uiState.isAccessibilityEnabled) "Enabled" else "Required for screen scanning",
                    action = {
                        if (uiState.isAccessibilityEnabled) {
                            Text(
                                "Active", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Text(
                                    "Enable", style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, color = Color.White
                                )
                            }
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                )
                PermissionRow(
                    iconBg = Color(0xFFDCFCE7), iconTint = Color(0xFF16A34A),
                    icon = Icons.Filled.CheckCircle,
                    title = "Activity Recognition", subtitle = "Authorized",
                    action = {
                        Text(
                            "Manage", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                context.startActivity(intent)
                            })
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                )
                PermissionRow(
                    iconBg = Color(0xFFDCFCE7), iconTint = Color(0xFF16A34A),
                    icon = Icons.Filled.CheckCircle,
                    title = "Notification Access", subtitle = "Authorized",
                    action = {
                        Text(
                            "Manage", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            })
                    }
                )
            }

            // Logout
            SettingsSection(label = "Account") {
                PermissionRow(
                    iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out", subtitle = "Sign out of Quran.com",
                    action = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626))
                                .clickable { onLogout() }
                                .padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(
                                "Log Out", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                )
            }

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Hayah v1.0.0", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Text(
                    "Crafted for mindful reflection",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
