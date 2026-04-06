package id.harissabil.hayah.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
fun SettingsSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        SettingsSectionLabel(label = label)
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.padding(top = 12.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            androidx.compose.foundation.layout
                .Column { content() }
        }
    }
}
