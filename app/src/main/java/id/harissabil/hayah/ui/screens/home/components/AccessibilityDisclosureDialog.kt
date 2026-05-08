package id.harissabil.hayah.ui.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AccessibilityDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(onDismissRequest = onDecline) {
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
                        .padding(24.dp),
            ) {
                Text(
                    text = "Data Access Disclosure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hayah uses Android's Accessibility Service to sense meaningful moments in your day and offer Quranic guidance. Here is exactly what it accesses:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                DisclosureSection(
                    heading = "What it reads",
                    body = "• On-screen text from any app you have open\n• Notification text and content descriptions",
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclosureSection(
                    heading = "How it's used",
                    body = "Text is analyzed locally on your device for Islamic keywords (e.g. \"patience\", \"stress\", \"gratitude\"). When a keyword is detected, Hayah suggests a relevant Quranic verse.",
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclosureSection(
                    heading = "What is stored",
                    body = "Only the matched keyword and the Quranic verse are saved to your private journal on this device. Raw screen content is never stored or sent to any external server.",
                )

                Spacer(modifier = Modifier.height(12.dp))

                DisclosureSection(
                    heading = "Your control",
                    body = "You can disable this service at any time via Settings > Accessibility.",
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDecline
                    ) {
                        Text("No Thanks")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAccept
                    ) {
                        Text(
                            text = "I Understand & Accept",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureSection(
    heading: String,
    body: String,
) {
    Text(
        text = heading,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
