package id.harissabil.hayah.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.harissabil.hayah.ui.theme.HayahTheme

@Composable
fun SpiritualRing(
    pagesRead: Int,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    // 1. Setup Transisi Tak Terbatas (Meditative Loop)
    val infiniteTransition = rememberInfiniteTransition(label = "morphing_ring")

    // Rotasi lambat berlawanan arah untuk ilusi bentuk organik yang terus berubah (Morphing)
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "rotation_clockwise",
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "rotation_counter_clockwise",
    )

    // Efek bernapas yang sangat halus (Breathing Depth)
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathing_scale",
    )

    // 2. Tonal Architecture Colors
    val baseGlowColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
    val gradientStart = MaterialTheme.colorScheme.secondary // Sand / Gold
    val gradientEnd = MaterialTheme.colorScheme.primaryContainer // Emerald

    val ringBrush =
        Brush.linearGradient(
            colors = listOf(gradientStart, gradientEnd),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val canvasCenter = center

            // Menggunakan efek scale untuk ilusi "napas"
            scale(scale = pulse, pivot = canvasCenter) {
                // Ring 1: Base Glow/Ambient Shadow (Aturan "Ghost Border" & Ambient Depth)
                drawCircle(
                    color = baseGlowColor,
                    style = Stroke(width = strokeWidth * 2.5f), // Lebih tebal, lebih samar
                    radius = (this.size.minDimension / 2) - strokeWidth,
                )

                // Ring 2: Cincin gradien organik pertama (Sedikit oval, berputar lambat)
                rotate(degrees = rotation1, pivot = canvasCenter) {
                    drawOval(
                        brush = ringBrush,
                        topLeft = Offset(strokeWidth, strokeWidth * 1.5f),
                        size =
                            Size(
                                width = this.size.width - (strokeWidth * 2),
                                height = this.size.height - (strokeWidth * 4), // Dibuat sedikit oval/squashed
                            ),
                        style = Stroke(width = strokeWidth),
                    )
                }

                // Ring 3: Cincin gradien organik kedua (Berputar berlawanan arah, offset berbeda)
                rotate(degrees = rotation2, pivot = canvasCenter) {
                    drawOval(
                        brush = ringBrush,
                        topLeft = Offset(strokeWidth * 1.5f, strokeWidth),
                        size =
                            Size(
                                width = this.size.width - (strokeWidth * 4),
                                height = this.size.height - (strokeWidth * 2),
                            ),
                        style = Stroke(width = strokeWidth * 1.2f),
                        alpha = 0.8f, // Sedikit transparan agar membaur indah saat tumpang tindih
                    )
                }
            }
        }

        // 3. Editorial Typography
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val digitCount = pagesRead.toString().length
            val fontSize =
                when {
                    digitCount <= 2 -> 72.sp
                    digitCount == 3 -> 58.sp
                    digitCount == 4 -> 46.sp
                    else -> 34.sp
                }
            Text(
                text = pagesRead.toString(),
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = fontSize,
                        letterSpacing = (-1.5).sp,
                    ),
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "PAGES READ",
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp, // Uppercase dengan tracking lebar ala "Curator's Tag"
                    ),
                color = MaterialTheme.colorScheme.secondary, // Memberikan "Warmth"
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpritualRingPreview() {
    HayahTheme {
        Surface {
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center,
            ) {
                SpiritualRing(pagesRead = 42)
            }
        }
    }
}
