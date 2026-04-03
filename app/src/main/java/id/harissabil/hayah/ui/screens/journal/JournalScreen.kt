package id.harissabil.hayah.ui.screens.journal

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class TagStyle { PRIMARY, SECONDARY, TERTIARY }

private data class JournalEntry(
    val surahVerse: String,
    val tag: String,
    val tagStyle: TagStyle,
    val reflection: String,
    val date: String,
    val isUnread: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onNavigateToQuranReader: () -> Unit) {
    val entries = listOf(
        JournalEntry(
            surahVerse = "Al-Baqarah: 153",
            tag = "Patience",
            tagStyle = TagStyle.TERTIARY,
            reflection = "A profound reflection on how seeking help through patience and prayer aligns the heart with divine strength during times of trial.",
            date = "Oct 24, 2023",
            isUnread = true
        ),
        JournalEntry(
            surahVerse = "As-Sharh: 5-6",
            tag = "Ease",
            tagStyle = TagStyle.SECONDARY,
            reflection = "Understanding the duality of hardship and ease as a simultaneous reality rather than a sequential one, fostering continuous hope.",
            date = "Oct 21, 2023",
            isUnread = false
        ),
        JournalEntry(
            surahVerse = "Luqman: 17",
            tag = "Wisdom",
            tagStyle = TagStyle.TERTIARY,
            reflection = "Analyzing the advice of Luqman to his son regarding prayer and commanding right—the foundational pillars of a character of substance.",
            date = "Oct 15, 2023",
            isUnread = false
        ),
        JournalEntry(
            surahVerse = "An-Nahl: 90",
            tag = "Justice",
            tagStyle = TagStyle.SECONDARY,
            reflection = "A reflection on the divine command for justice, kindness, and generosity, and how these values transform social structures.",
            date = "Oct 12, 2023",
            isUnread = true
        ),
        JournalEntry(
            surahVerse = "Al-Imran: 200",
            tag = "Perseverance",
            tagStyle = TagStyle.TERTIARY,
            reflection = "Exploring the depth of steadfastness when facing trials, and how it molds the believer into a more resilient soul.",
            date = "Oct 8, 2023",
            isUnread = false
        ),
    )

    var searchQuery by remember { mutableStateOf("") }
    val filtered = entries.filter {
        searchQuery.isEmpty() ||
            it.surahVerse.contains(searchQuery, ignoreCase = true) ||
            it.reflection.contains(searchQuery, ignoreCase = true) ||
            it.tag.contains(searchQuery, ignoreCase = true)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            // Sticky frosted search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    placeholder = {
                        Text(
                            text = "Search reflections...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }

            // Card list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(filtered) { entry ->
                    JournalEntryCard(entry = entry, onClick = onNavigateToQuranReader)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        // Header row: unread dot + title + tag pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Unread indicator dot with ring
                if (entry.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = entry.surahVerse,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tag pill
            val tagBg: Color
            val tagFg: Color
            when (entry.tagStyle) {
                TagStyle.TERTIARY -> {
                    tagBg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    tagFg = MaterialTheme.colorScheme.tertiary
                }
                TagStyle.SECONDARY -> {
                    tagBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    tagFg = MaterialTheme.colorScheme.secondary
                }
                TagStyle.PRIMARY -> {
                    tagBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    tagFg = MaterialTheme.colorScheme.primary
                }
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tagBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = entry.tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = tagFg
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reflection body — 2-line clamp
        Text(
            text = entry.reflection,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: date + Read Verse button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = entry.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = "Read Verse",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
