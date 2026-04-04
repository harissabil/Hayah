package id.harissabil.hayah.ui.screens.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpiritualRing(
    versesRead: Int,
    totalVerses: Int,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    val progress = if (totalVerses > 0) versesRead.toFloat() / totalVerses else 0f
    val sweepAngle = 300f * progress.coerceIn(0f, 1f)

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientStart = MaterialTheme.colorScheme.secondary
    val gradientEnd = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = trackColor, style = Stroke(width = 10.dp.toPx()))
            drawArc(
                brush = Brush.linearGradient(colors = listOf(gradientStart, gradientEnd)),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = versesRead.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Verses Read",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
