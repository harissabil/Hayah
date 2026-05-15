package id.harissabil.hayah.ui.screens.journal

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.harissabil.hayah.ui.screens.journal.components.EmptyJournalState
import id.harissabil.hayah.ui.screens.journal.components.JournalEntryCard
import id.harissabil.hayah.ui.screens.journal.components.JournalSearchBar
import id.harissabil.hayah.ui.theme.HayahTheme
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun JournalScreen(
    onNavigateToQuranReader: (entryId: Long, pageNumber: Int, verseKey: String?) -> Unit,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        JournalScreenContent(
            uiState = uiState,
            onQueryChange = viewModel::onSearchQueryChanged,
            onEntryClick = onNavigateToQuranReader,
            paddingValues = innerPadding,
        )
    }
}

@Composable
private fun JournalScreenContent(
    uiState: JournalUiState,
    onQueryChange: (String) -> Unit,
    onEntryClick: (entryId: Long, pageNumber: Int, verseKey: String?) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
    ) {
        JournalSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
        )

        if (uiState.filteredEntries.isEmpty()) {
            EmptyJournalState(
                isSearching = uiState.searchQuery.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(
                    items = uiState.filteredEntries,
                    key = { UUID.randomUUID() },
                ) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        onClick = {
                            onEntryClick(
                                entry.entryId,
                                entry.pageNumber,
                                entry.verseKey,
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(
    name = "Small Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=360dp,height=640dp,dpi=320,isRound=false,chinSize=0dp,orientation=portrait",
)
@Preview(
    name = "Small Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=360dp,height=640dp,dpi=320,isRound=false,chinSize=0dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Normal Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=393dp,height=851dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait",
)
@Preview(
    name = "Normal Phone",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=393dp,height=851dp,dpi=420,isRound=false,chinSize=0dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun JournalScreenPreview() {
    HayahTheme {
        Scaffold { padding ->
            JournalScreenContent(
                uiState =
                    JournalUiState(
                        allEntries =
                            listOf(
                                JournalEntry(
                                    entryId = 1L,
                                    surahVerse = "Al-Baqarah • 2:255 (Ayat Al-Kursi)",
                                    verseKey = "2:255",
                                    tag = "Faith",
                                    tagStyle = TagStyle.PRIMARY,
                                    reflection = "This verse speaks of the eternal throne of Allah and His absolute sovereignty over all creation. A reminder of who truly holds power.",
                                    date = "Mon, 12 May 2025",
                                    isUnread = false,
                                    pageNumber = 42,
                                ),
                                JournalEntry(
                                    entryId = 2L,
                                    surahVerse = "Ar-Rahman • 55:13",
                                    verseKey = "55:13",
                                    tag = "Gratitude",
                                    tagStyle = TagStyle.SECONDARY,
                                    reflection = "Which of the favors of your Lord would you deny? A verse of gratitude and reflection on blessings.",
                                    date = "Tue, 13 May 2025",
                                    isUnread = true,
                                    pageNumber = 531,
                                ),
                            ),
                        isLoading = false,
                    ),
                onQueryChange = {},
                onEntryClick = { _, _, _ -> },
                paddingValues = padding,
            )
        }
    }
}
