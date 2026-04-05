package id.harissabil.hayah.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.harissabil.hayah.data.db.dao.JournalEntryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalUiState(
    val allEntries: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
) {
    val filteredEntries: List<JournalEntry>
        get() = if (searchQuery.isBlank()) allEntries
        else allEntries.filter {
            it.surahVerse.contains(searchQuery, ignoreCase = true) ||
                    it.reflection.contains(searchQuery, ignoreCase = true) ||
                    it.tag.contains(searchQuery, ignoreCase = true)
        }
}

class JournalViewModel(
    private val journalEntryDao: JournalEntryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        observeJournalEntries()
    }

    private fun observeJournalEntries() {
        viewModelScope.launch {
            journalEntryDao.getAllEntries().collect { entities ->
                val entries = entities.map { entity ->
                    JournalEntry(
                        entryId = entity.id,
                        surahVerse = entity.surahVerse,
                        verseKey = entity.verseKey,
                        tag = entity.tag,
                        tagStyle = TagStyle.entries.getOrElse(entity.tagStyleOrdinal) { TagStyle.PRIMARY },
                        reflection = entity.reflection,
                        date = formatTimestamp(entity.timestamp),
                        isUnread = entity.isUnread,
                        pageNumber = entity.pageNumber
                    )
                }
                _uiState.update {
                    it.copy(allEntries = entries, isLoading = false)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.ENGLISH)
        return sdf.format(java.util.Date(timestamp))
    }
}
