package id.harissabil.hayah.ui.screens.journal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val mockEntries = listOf(
    JournalEntry(
        surahVerse = "Al-Baqarah: 153",
        tag = "Patience",
        tagStyle = TagStyle.TERTIARY,
        reflection = "A profound reflection on how seeking help through patience and prayer aligns the heart with divine strength during times of trial.",
        date = "Oct 24, 2023",
        isUnread = true
    ),
    JournalEntry(
        surahVerse = "As-Sharh: 5-6",
        tag = "Ease",
        tagStyle = TagStyle.SECONDARY,
        reflection = "Understanding the duality of hardship and ease as a simultaneous reality rather than a sequential one, fostering continuous hope.",
        date = "Oct 21, 2023",
        isUnread = false
    ),
    JournalEntry(
        surahVerse = "Luqman: 17",
        tag = "Wisdom",
        tagStyle = TagStyle.TERTIARY,
        reflection = "Analyzing the advice of Luqman to his son regarding prayer and commanding right—the foundational pillars of a character of substance.",
        date = "Oct 15, 2023",
        isUnread = false
    ),
    JournalEntry(
        surahVerse = "An-Nahl: 90",
        tag = "Justice",
        tagStyle = TagStyle.SECONDARY,
        reflection = "A reflection on the divine command for justice, kindness, and generosity, and how these values transform social structures.",
        date = "Oct 12, 2023",
        isUnread = true
    ),
    JournalEntry(
        surahVerse = "Al-Imran: 200",
        tag = "Perseverance",
        tagStyle = TagStyle.TERTIARY,
        reflection = "Exploring the depth of steadfastness when facing trials, and how it molds the believer into a more resilient soul.",
        date = "Oct 8, 2023",
        isUnread = false
    ),
)

data class JournalUiState(
    val allEntries: List<JournalEntry> = mockEntries,
    val searchQuery: String = "",
) {
    val filteredEntries: List<JournalEntry>
        get() = if (searchQuery.isBlank()) allEntries
        else allEntries.filter {
            it.surahVerse.contains(searchQuery, ignoreCase = true) ||
                    it.reflection.contains(searchQuery, ignoreCase = true) ||
                    it.tag.contains(searchQuery, ignoreCase = true)
        }
}

class JournalViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
