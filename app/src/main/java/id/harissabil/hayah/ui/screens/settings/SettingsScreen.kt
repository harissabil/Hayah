package id.harissabil.hayah.ui.screens.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.harissabil.hayah.ui.screens.settings.components.AppearancePicker
import id.harissabil.hayah.ui.screens.settings.components.PermissionRow
import id.harissabil.hayah.ui.screens.settings.components.SettingsIconBox
import id.harissabil.hayah.ui.screens.settings.components.SettingsNavRow
import id.harissabil.hayah.ui.screens.settings.components.SettingsSection
import id.harissabil.hayah.ui.screens.settings.components.SettingsSectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = {
                Text("Settings", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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
                    icon = Icons.Filled.AccountCircle,
                    iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Account",
                    subtitle = "Manage your profile and data",
                    onClick = {}
                )
                SettingsNavRow(
                    icon = Icons.Filled.RecordVoiceOver,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Preferred Reciter",
                    subtitle = "Mishary Rashid Alafasy",
                    onClick = {}
                )
            }

            // Notifications
            SettingsSection(label = "Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
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
                            Text("Play Audio Instantly", style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Start recitation when page opens", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBox(
                                icon = Icons.Filled.NotificationsActive,
                                bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Max Reminders", style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Daily nudge frequency", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(uiState.maxReminders.toInt().toString(), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                SettingsNavRow(
                    icon = Icons.Filled.Bedtime,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Quiet Hours",
                    subtitle = "22:00 — 05:00",
                    onClick = {}
                )
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
                    iconBg = Color(0xFFDCFCE7), iconTint = Color(0xFF16A34A),
                    icon = Icons.Filled.CheckCircle,
                    title = "Activity Recognition", subtitle = "Authorized",
                    action = { Text("Manage", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {}) }
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                PermissionRow(
                    iconBg = Color(0xFFFFF7ED), iconTint = Color(0xFFEA580C),
                    icon = Icons.Filled.Warning,
                    title = "Notification Access", subtitle = "Action required",
                    action = {
                        Box(modifier = Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {}.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text("Enable", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                )
            }

            // Footer
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hayah v1.0.0", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("Crafted for mindful reflection",
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
