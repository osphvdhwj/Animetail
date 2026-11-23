package eu.kanade.presentation.entries.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
/**
 * UI presentation composable for DotSeparatorText.
 *
 * Renders the interface in Material 3 design, binding reactive state
 * and dispatching user interactions to parent handlers.
 */
fun DotSeparatorText(
    modifier: Modifier = Modifier,
) {
    Text(
        text = " • ",
        modifier = modifier,
    )
}

@Composable
fun DotSeparatorNoSpaceText(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "•",
        modifier = modifier,
    )
}
