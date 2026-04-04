package id.harissabil.hayah.ui.screens.journal

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import id.harissabil.hayah.ui.screens.journal.components.JournalEntryCard
import id.harissabil.hayah.ui.screens.journal.components.JournalSearchBar

@Composable
fun JournalScreen(
    onNavigateToQuranReader: () -> Unit,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            JournalSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(
                    items = uiState.filteredEntries,
                    key = { it.surahVerse }
                ) { entry ->
                    JournalEntryCard(entry = entry, onClick = onNavigateToQuranReader)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
