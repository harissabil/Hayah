package id.harissabil.hayah.ui.screens.reading.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.harissabil.hayah.ui.theme.ManropeFamily

@Composable
fun AudioPlayerBar(
    chapterName: String,
    verseNumber: Int,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val progress =
        if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f

    val elapsedText = formatMs(currentPositionMs)
    val totalText = if (durationMs > 0) formatMs(durationMs) else "--:--"

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                ).background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ).navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / pause / buffering control
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(48.dp),
        ) {
            when {
                isBuffering -> CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                isPlaying -> Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pause",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                else -> Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Verse info + progress bar
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$chapterName, Ayah $verseNumber",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$elapsedText / $totalText",
                    fontFamily = ManropeFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stop / close
        IconButton(
            onClick = onStopClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Stop playback",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
