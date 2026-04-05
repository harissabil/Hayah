package id.harissabil.hayah.ui.screens.reading.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.harissabil.hayah.ui.theme.ManropeFamily

@Composable
fun EndOfPageMarker(
    pageNumber: Int,
    isPosting: Boolean,
    postSuccess: Boolean,
    error: String?,
    readingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "END OF PAGE $pageNumber",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Formulate reading time string:
        val minutes = readingSeconds / 60
        val seconds = readingSeconds % 60
        val timeString = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

        if (isPosting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Saving progress to quran.com...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (postSuccess) {
            Text("Progress saved to quran.com • Read in $timeString", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        } else if (error != null) {
            Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}