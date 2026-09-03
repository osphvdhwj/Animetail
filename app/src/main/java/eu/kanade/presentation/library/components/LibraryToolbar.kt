package eu.kanade.presentation.library.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomEntry: () -> Unit,
    onClickSyncNow: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigateUp: (() -> Unit)? = null,
    mediaSwitcher: (@Composable () -> Unit)? = null,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )

    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomEntry = onClickOpenRandomEntry,
        onClickSyncNow = onClickSyncNow,
        scrollBehavior = scrollBehavior,
        navigateUp = navigateUp,
        mediaSwitcher = mediaSwitcher,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomEntry: () -> Unit,
    onClickSyncNow: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigateUp: (() -> Unit)?,
    mediaSwitcher: (@Composable () -> Unit)? = null,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    SearchToolbar(
        titleContent = {
            if (mediaSwitcher != null) {
                mediaSwitcher()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title.text,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, false),
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (title.numberOfEntries != null) {
                        Pill(
                            text = "${title.numberOfEntries}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomEntry,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(TLMR.strings.sync_library),
                        onClick = onClickSyncNow,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
        navigateUp = navigateUp,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfEntries: Int? = null,
)

@Composable
fun LibraryMediaSwitcher(
    isAnime: Boolean,
    onSelectAnime: () -> Unit,
    onSelectManga: () -> Unit,
    animeCount: Int? = null,
    mangaCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Anime Pill
            Surface(
                onClick = onSelectAnime,
                shape = RoundedCornerShape(50),
                color = if (isAnime) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isAnime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Anime",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isAnime) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (isAnime && animeCount != null && animeCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = "$animeCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Manga Pill
            Surface(
                onClick = onSelectManga,
                shape = RoundedCornerShape(50),
                color = if (!isAnime) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (!isAnime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Manga",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (!isAnime) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (!isAnime && mangaCount != null && mangaCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = "$mangaCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
