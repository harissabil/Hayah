package id.harissabil.hayah.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.harissabil.hayah.ui.screens.settings.AppTheme

@Composable
fun AppearancePicker(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppearanceCard(
            label = "Light",
            isSelected = selected == AppTheme.LIGHT,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AppTheme.LIGHT) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.LightMode, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)
                    )
                }
            },
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
        AppearanceCard(
            label = "Dark",
            isSelected = selected == AppTheme.DARK,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AppTheme.DARK) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.DarkMode, contentDescription = null,
                        tint = Color(0xFF9E9E9E), modifier = Modifier.size(32.dp)
                    )
                }
            },
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
        AppearanceCard(
            label = "System",
            isSelected = selected == AppTheme.SYSTEM,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AppTheme.SYSTEM) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppearanceCard(
    label: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon()
        Text(
            text = label, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold, color = labelColor
        )
    }
}
