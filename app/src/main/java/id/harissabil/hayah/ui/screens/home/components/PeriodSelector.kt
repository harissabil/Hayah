package id.harissabil.hayah.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.harissabil.hayah.ui.screens.home.Period

@Composable
fun PeriodSelector(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // 1. Create a state to hold the width of the button
    var dropDownWidth by remember { mutableStateOf(0.dp) }
    // 2. Get the current density to convert pixels to dp
    val density = LocalDensity.current

    val label =
        when (selectedPeriod) {
            Period.THIS_WEEK -> "This Week"
            Period.THIS_MONTH -> "This Month"
            Period.ALL_TIME -> "All Time"
        }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier =
                Modifier
                    .clickable { expanded = true }
                    // 3. Measure the width of the Surface when it's positioned
                    .onGloballyPositioned { coordinates ->
                        dropDownWidth = with(density) { coordinates.size.width.toDp() }
                    },
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Change period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            // 4. Apply the exact measured width to the DropdownMenu
            modifier = Modifier.width(dropDownWidth),
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 4.dp,
        ) {
            Period.entries.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                when (period) {
                                    Period.THIS_WEEK -> "This Week"
                                    Period.THIS_MONTH -> "This Month"
                                    Period.ALL_TIME -> "All Time"
                                },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    },
                )
            }
        }
    }
}
