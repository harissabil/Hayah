package id.harissabil.hayah.ui.screens.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Ambient "aura" blobs drawn via Canvas radial gradients.
 *
 * Using Canvas + Brush.radialGradient instead of Box+blur+background because:
 * - `Modifier.blur` applies RenderEffect to the entire drawable region (always rectangular).
 * - `radialGradient` fades naturally to transparent at the edges producing a true soft circle.
 */
@Composable
fun AuraBlobs(modifier: Modifier = Modifier) {
    val primaryAura = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    val tertiaryAura = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
    val transparent = primaryAura.copy(alpha = 0f)
    val transparentT = tertiaryAura.copy(alpha = 0f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Top-left primary blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryAura, transparent),
                center = Offset(x = 0f, y = 0f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(x = 0f, y = 0f)
        )

        // Bottom-right tertiary blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryAura, transparentT),
                center = Offset(x = size.width, y = size.height),
                radius = size.width * 0.55f
            ),
            radius = size.width * 0.55f,
            center = Offset(x = size.width, y = size.height)
        )
    }
}
