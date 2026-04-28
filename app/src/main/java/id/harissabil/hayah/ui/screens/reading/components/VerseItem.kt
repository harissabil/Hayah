package id.harissabil.hayah.ui.screens.reading.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.harissabil.hayah.data.model.VerseDetail
import id.harissabil.hayah.ui.theme.JakartaFamily
import id.harissabil.hayah.ui.theme.ManropeFamily
import id.harissabil.hayah.ui.theme.UthmaniFamily

@Composable
fun VerseItem(
    verse: VerseDetail,
    isHighlighted: Boolean,
    isPlaying: Boolean = false,
    isBuffering: Boolean = false,
    onPlayClick: () -> Unit = {},
) {
    val arabicText =
        verse.textUthmani
            ?.replace('۟', 'ْ')
            ?: ""

    val verseNumber = verse.verseNumber ?: 0
    val rawTranslation = verse.translations?.firstOrNull()?.text ?: ""
    val translation =
        rawTranslation
            .replace(Regex("<sup[^>]*>.*?</sup>"), "")
            .replace(Regex("<[^>]*>"), "")

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    when {
                        isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                ).padding(
                    top = if (isHighlighted) 48.dp else 24.dp,
                    bottom = if (isHighlighted) 40.dp else 24.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Verse number + speaker icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isHighlighted) {
                        Text(
                            text = verseNumber.toString(),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = verseNumber.toString(),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        when {
                            isBuffering -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            isPlaying -> Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pause recitation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            else -> Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play recitation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = arabicText,
                    fontFamily = UthmaniFamily,
                    fontSize = 32.sp,
                    lineHeight = 64.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isHighlighted) "\"$translation\"" else translation,
                fontFamily = JakartaFamily,
                fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                fontSize = if (isHighlighted) 18.sp else 16.sp,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp),
                fontStyle = if (isHighlighted) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 28.sp,
            )
        }
    }
}
