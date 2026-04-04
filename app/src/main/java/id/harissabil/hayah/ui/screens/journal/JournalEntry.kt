package id.harissabil.hayah.ui.screens.journal

import androidx.compose.runtime.Immutable

enum class TagStyle { PRIMARY, SECONDARY, TERTIARY }

@Immutable
data class JournalEntry(
    val surahVerse: String,
    val tag: String,
    val tagStyle: TagStyle,
    val reflection: String,
    val date: String,
    val isUnread: Boolean,
)
