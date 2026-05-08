package id.harissabil.hayah.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.R
import id.harissabil.hayah.ui.screens.onboarding.components.AuraBlobs
import id.harissabil.hayah.ui.screens.onboarding.components.OnboardingPageContent
import id.harissabil.hayah.ui.screens.onboarding.components.PagerIndicator
import id.harissabil.hayah.ui.theme.CairoFamily
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onLoginClick: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })
    val context = LocalContext.current

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.onPageChanged(it) }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        // Layer 1 — soft radial aura blobs (behind everything)
        AuraBlobs()

        // Layer 2 — the HorizontalPager goes edge-to-edge so each page image fills the
        //            full width. The pager drives both the image AND text together,
        //            which is what creates the seamless panorama swipe.
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    // Pager height: leaves enough room at bottom for the action buttons
                    .padding(bottom = 220.dp),
        ) { page ->
            OnboardingPageContent(page = uiState.pages[page])
        }

        // Layer 3 — fixed UI overlay: brand title (top) + actions (bottom)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Brand title — sits above the pager image
            Spacer(modifier = Modifier.height(52.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_hayah_transparent),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "Hayah",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFamily,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom dock — indicators + CTAs are always pinned to the bottom
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PagerIndicator(
                    pageCount = uiState.pages.size,
                    currentPage = uiState.currentPage,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CTA — launches Quran.com OAuth login
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                ),
                            ).clickable { onLoginClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Quran.com",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // TODO: replace these URLs with your actual hosted pages before publishing
                val tosUrl = "https://github.com/harissabil/Hayah/blob/develop/docs/terms-of-service.md"
                val privacyUrl = "https://github.com/harissabil/Hayah/blob/develop/docs/privacy-policy.md"
                val legalText =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                            append("By continuing, you agree to our ")
                        }
                        pushStringAnnotation(tag = "TOS", annotation = tosUrl)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Terms of Service")
                        }
                        pop()
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.outline)) {
                            append(" & ")
                        }
                        pushStringAnnotation(tag = "PRIVACY", annotation = privacyUrl)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Privacy Policy")
                        }
                        pop()
                    }
                ClickableText(
                    text = legalText,
                    style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { offset ->
                        legalText.getStringAnnotations(tag = "TOS", start = offset, end = offset)
                            .firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                        legalText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                            .firstOrNull()?.let {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                            }
                    },
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
