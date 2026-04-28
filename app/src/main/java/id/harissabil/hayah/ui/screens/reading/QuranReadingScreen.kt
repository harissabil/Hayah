package id.harissabil.hayah.ui.screens.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.ui.screens.reading.components.AudioPlayerBar
import id.harissabil.hayah.ui.screens.reading.components.EndOfPageMarker
import id.harissabil.hayah.ui.screens.reading.components.VerseItem
import id.harissabil.hayah.ui.theme.ManropeFamily
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReadingScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuranReadingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            totalItemsNumber in 1..lastVisibleItemIndex
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !uiState.isLoading && uiState.verses.isNotEmpty()) {
            viewModel.onBottomReached()
        }
    }

    // Auto-scroll to the currently playing verse
    LaunchedEffect(uiState.audioState.currentVerseKey) {
        val verseKey = uiState.audioState.currentVerseKey ?: return@LaunchedEffect
        val index = uiState.verses.indexOfFirst { it.verseKey == verseKey }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 48.dp),
                        ) {
                            val titleText = uiState.chapterName.ifEmpty { "Unknown Surah" }

                            Text(
                                text = titleText,
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Page ${uiState.pageNumber}",
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    ),
                windowInsets = WindowInsets.statusBars,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.verses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                    text = "Error: ${uiState.error}",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.retry() }) {
                    Text("Retry")
                }
            }
        } else {
            val audioState = uiState.audioState
            val playerVisible = audioState.currentVerseKey != null

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = 16.dp,
                            // Extra bottom padding when the player bar is visible so
                            // EndOfPageMarker is never hidden behind it.
                            bottom = if (playerVisible) 120.dp else 48.dp,
                        ),
                ) {
                    items(uiState.verses, key = { it.verseKey ?: it.hashCode() }) { verse ->
                        val isHighlighted = verse.verseKey == uiState.highlightedVerseKey
                        val isVerseActive = verse.verseKey != null && verse.verseKey == audioState.currentVerseKey
                        VerseItem(
                            verse = verse,
                            isHighlighted = isHighlighted,
                            isPlaying = isVerseActive && audioState.isPlaying,
                            isBuffering = isVerseActive && audioState.isBuffering,
                            onPlayClick = {
                                val key = verse.verseKey ?: return@VerseItem
                                viewModel.onVersePlayRequested(key)
                            },
                        )
                    }

                    item {
                        EndOfPageMarker(
                            pageNumber = uiState.pageNumber,
                            isPosting = uiState.isPosting,
                            postSuccess = uiState.postSuccess,
                            error = if (uiState.hasReachedBottom) uiState.error else null,
                            readingSeconds = uiState.readingSeconds,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = playerVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    val playingVerseKey = audioState.currentVerseKey ?: ""
                    val chapterId = playingVerseKey.substringBefore(":").toIntOrNull() ?: 0
                    val verseNum = playingVerseKey.substringAfter(":").toIntOrNull() ?: 0
                    val chapterName = uiState.chapterNames[chapterId] ?: "Surah $chapterId"

                    AudioPlayerBar(
                        chapterName = chapterName,
                        verseNumber = verseNum,
                        isPlaying = audioState.isPlaying,
                        isBuffering = audioState.isBuffering,
                        currentPositionMs = audioState.currentPositionMs,
                        durationMs = audioState.durationMs,
                        onPlayPauseClick = viewModel::onTogglePlayPause,
                        onStopClick = viewModel::onStopPlayback,
                    )
                }
            }
        }
    }
}
