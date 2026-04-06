package id.harissabil.hayah.ui.screens.journal.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.sp
import id.harissabil.hayah.ui.screens.journal.JournalEntry
import id.harissabil.hayah.ui.screens.journal.TagStyle

@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable { onClick() }
                .padding(24.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (!entry.isUnread) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = entry.surahVerse,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TagPill(tag = entry.tag, style = entry.tagStyle)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = entry.reflection,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entry.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClick() },
            ) {
                Text(
                    text = "Read Full Page",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun TagPill(
    tag: String,
    style: TagStyle,
) {
    val bg: Color
    val fg: Color
    when (style) {
        TagStyle.TERTIARY -> {
            bg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            fg = MaterialTheme.colorScheme.tertiary
        }

        TagStyle.SECONDARY -> {
            bg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            fg = MaterialTheme.colorScheme.secondary
        }

        TagStyle.PRIMARY -> {
            bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            fg = MaterialTheme.colorScheme.primary
        }
    }
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = tag.uppercase(),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                ),
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}
