package id.harissabil.hayah.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.harissabil.hayah.ui.screens.settings.DetectionMode
import id.harissabil.hayah.ui.screens.settings.SettingsUiState

/**
 * Detection Mode section: mode picker, mode-specific slider, and model download UI.
 */
@Composable
fun DetectionModeSection(
    uiState: SettingsUiState,
    onModeSelected: (DetectionMode) -> Unit,
    onDetectionThresholdChanged: (Float) -> Unit,
    onSimilarityThresholdChanged: (Float) -> Unit,
    onDownloadModel: () -> Unit,
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        DetectionModeInfoDialog(onDismiss = { showInfoDialog = false })
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBox(
                icon = Icons.Default.Tune,
                bg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Detection Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "How screen content is analyzed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "Learn about detection modes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable { showInfoDialog = true }
                        .padding(4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetectionMode.entries.forEach { mode ->
                val selected = uiState.detectionMode == mode
                val enabled = mode != DetectionMode.SEMANTIC || uiState.isModelDownloaded
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            ).clickable(enabled = enabled) {
                                onModeSelected(mode)
                            }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            when (mode) {
                                DetectionMode.KEYWORDS -> "Keywords"
                                DetectionMode.SEMANTIC -> "Semantic"
                            },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color =
                            when {
                                selected -> Color.White
                                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }

        // Mode-specific controls
        when (uiState.detectionMode) {
            DetectionMode.KEYWORDS -> {
                DetectionSensitivitySlider(
                    value = uiState.detectionThreshold,
                    onValueChange = onDetectionThresholdChanged,
                )
            }

            DetectionMode.SEMANTIC -> {
                SimilarityThresholdSlider(
                    value = uiState.similarityThreshold,
                    onValueChange = onSimilarityThresholdChanged,
                )
            }
        }

        // Model download section
        if (!uiState.isModelDownloaded) {
            ModelDownloadCard(
                isDownloading = uiState.isModelDownloading,
                progress = uiState.modelDownloadProgress,
                onDownload = onDownloadModel,
            )
        }
    }
}

// ── Sub-components ───────────────────────────────

@Composable
private fun DetectionSensitivitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Detection Sensitivity",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Number of detections required before a nudge is sent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${value.toInt()}x",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 1f..10f,
        steps = 8,
        modifier = Modifier.fillMaxWidth(),
        colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
    )
}

@Composable
private fun SimilarityThresholdSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Similarity Threshold",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Higher values require closer semantic match to trigger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.2f..0.9f,
        steps = 11,
        modifier = Modifier.fillMaxWidth(),
        colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
    )
}

@Composable
private fun ModelDownloadCard(
    isDownloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
                .padding(12.dp),
    ) {
        Text(
            text = "Semantic mode requires a one-time ~25 MB model download.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isDownloading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            ElevatedButton(
                onClick = onDownload,
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Download Model",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DetectionModeInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detection Modes") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Matches exact words on screen against a predefined list. Fast and lightweight, but only triggers on specific words.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Semantic",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Uses an on-device AI model to understand the meaning of screen content, not just exact words. Can detect themes like sadness or gratitude even when no keyword is present. Requires a one-time ~25 MB model download.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}
