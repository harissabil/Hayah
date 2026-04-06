package id.harissabil.hayah.ui.screens.home.components

import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import id.harissabil.hayah.R

@Composable
fun AccessibilityTutorialDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val videoRef = remember { mutableStateOf<VideoView?>(null) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        // Apply padding only to top and bottom so the video can span edge-to-edge
                        .padding(vertical = 24.dp),
            ) {
                Text(
                    text = "Enable Accessibility",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp), // Added padding here
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The video box now has no horizontal padding, spanning full width
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx)
                                .apply {
                                    // Force the native VideoView to fully expand to the Compose Box bounds
                                    layoutParams =
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        )
                                    val uri =
                                        "android.resource://${ctx.packageName}/${R.raw.vid_accessibility_service_tutorial}".toUri()
                                    setVideoURI(uri)
                                    setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.isLooping = true
                                        start()
                                    }
                                }.also { videoRef.value = it }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "How to enable:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp), // Added padding here
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text =
                        "1. Open Accessibility Settings\n" +
                            "2. Scroll down and select Hayah\n" +
                            "3. Turn on the service\n" +
                            "4. Allow monitor and control device",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp), // Added padding here
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If not enabled, you can enable it later in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp), // Added padding here
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    // Added padding here
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onClose) {
                        Text("Close")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                },
                            )
                            onClose()
                        },
                    ) {
                        Text(
                            text = "Enable",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoRef.value?.stopPlayback()
            videoRef.value = null
        }
    }
}
