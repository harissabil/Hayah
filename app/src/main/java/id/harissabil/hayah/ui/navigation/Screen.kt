package id.harissabil.hayah.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String? = null,
    val icon: ImageVector? = null,
    val iconOutlined: ImageVector? = null,
) {
    object Onboarding : Screen("onboarding")

    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)

    object Journal : Screen(
        "journal",
        "Journal",
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.AutoMirrored.Outlined.MenuBook,
    )

    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    object QuranReading : Screen("quran_reading/{pageNumber}?entryId={entryId}&highlightedVerseKey={highlightedVerseKey}") {
        fun createRoute(
            pageNumber: Int,
            entryId: Long = -1L,
            highlightedVerseKey: String? = null,
        ): String =
            buildString {
                append("quran_reading/$pageNumber")

                val queryParams = mutableListOf<String>()

                if (entryId != -1L) {
                    queryParams.add("entryId=$entryId")
                }
                if (highlightedVerseKey != null) {
                    queryParams.add("highlightedVerseKey=$highlightedVerseKey")
                }

                if (queryParams.isNotEmpty()) {
                    append("?${queryParams.joinToString("&")}")
                }
            }
    }
}
