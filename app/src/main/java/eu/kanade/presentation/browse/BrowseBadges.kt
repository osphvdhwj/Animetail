package eu.kanade.presentation.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import tachiyomi.presentation.core.components.Badge

@Composable
/**
 * Core component for BrowseBadges within the Animetail presentation and UI layer.
 */
fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}
