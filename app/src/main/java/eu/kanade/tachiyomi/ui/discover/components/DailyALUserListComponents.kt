package eu.kanade.tachiyomi.ui.discover.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.design.DS
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.discover.ActivityState
import eu.kanade.tachiyomi.ui.discover.DailyALActivityEntry
import eu.kanade.tachiyomi.ui.discover.DailyALUserListEntry
import eu.kanade.tachiyomi.ui.discover.TrackedRailItem
import eu.kanade.tachiyomi.ui.discover.UserListState
import tachiyomi.domain.items.chapter.interactor.GetChapter
import tachiyomi.i18n.tail.TLMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** List-status tabs shown in the profile's list browser, in display order. */
val DAILY_AL_LIST_STATUSES = listOf(
    "CURRENT" to "Watching",
    "PLANNING" to "Planning",
    "COMPLETED" to "Completed",
    "PAUSED" to "Paused",
    "DROPPED" to "Dropped",
)

/**
 * The "My Lists" browser for the Discover profile section: status tabs over the
 * user's AniList entries, each with inline progress and score editing that syncs
 * back to AniList.
 */
@Composable
fun DailyALUserListBrowser(
    state: UserListState,
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    onRetry: () -> Unit,
    onEntryUpdate: (DailyALUserListEntry, progress: Int, scoreRaw: Int) -> Unit,
    onEntryClick: (DailyALUserListEntry) -> Unit,
    modifier: Modifier = Modifier,
    isManga: Boolean = false,
    onMediaTypeChange: ((Boolean) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "My AniList")

        // Anime | Manga mini-toggle (native pill style) above the status chips
        if (onMediaTypeChange != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = !isManga,
                    onClick = { if (isManga) onMediaTypeChange(false) },
                    label = { Text("Anime") },
                )
                FilterChip(
                    selected = isManga,
                    onClick = { if (!isManga) onMediaTypeChange(true) },
                    label = { Text("Manga") },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DAILY_AL_LIST_STATUSES.forEach { (key, label) ->
                FilterChip(
                    selected = selectedStatus == key,
                    onClick = { onSelectStatus(key) },
                    label = {
                        Text(
                            when {
                                key == "CURRENT" && isManga -> "Reading"
                                key == "CURRENT" -> "Watching"
                                key == "PLANNING" && isManga -> "Plan to read"
                                key == "PLANNING" -> "Plan to watch"
                                else -> label
                            },
                        )
                    },
                )
            }
        }

        when (state) {
            is UserListState.Loading, UserListState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UserListState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.TextButton(onClick = onRetry) {
                        Text("Failed to load list — tap to retry")
                    }
                }
            }

            is UserListState.Success -> {
                if (state.entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Nothing here yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.height((state.entries.size.coerceAtMost(8) * 118).dp)) {
                        items(state.entries, key = { it.mediaId }) { entry ->
                            DailyALUserListRow(
                                entry = entry,
                                onUpdate = onEntryUpdate,
                                onClick = onEntryClick,
                            )
                        }
                    }
                }
            }

            UserListState.NotLoggedIn -> {}
        }
    }
}

/**
 * One list entry: cover, title/format, progress stepper (− n/total +) and a
 * POINT_100 score stepper (±10).
 */
@Composable
private fun DailyALUserListRow(
    entry: DailyALUserListEntry,
    onUpdate: (DailyALUserListEntry, Int, Int) -> Unit,
    onClick: (DailyALUserListEntry) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick(entry) },
        shape = DS.radiusCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (entry.coverUrl != null) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 52.dp, height = 74.dp)
                        .clip(DS.radiusSmall),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        entry.format?.let { append(it) }
                        if (entry.averageScore > 0) {
                            if (isNotEmpty()) append("  •  ")
                            append("★ ${entry.averageScore}%")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress stepper
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdate(entry, (entry.progress - 1).coerceAtLeast(0), entry.scoreRaw) },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        text = if (entry.totalEpisodes > 0) {
                            "${entry.progress}/${entry.totalEpisodes}"
                        } else {
                            "${entry.progress}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = {
                            val max = entry.totalEpisodes.takeIf { it > 0 } ?: Int.MAX_VALUE
                            onUpdate(entry, (entry.progress + 1).coerceAtMost(max), entry.scoreRaw)
                        },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            }

            // Score stepper (POINT_100, ±10)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = DS.radiusMedium,
                    color = if (entry.scoreRaw > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = if (entry.scoreRaw > 0) "${entry.scoreRaw}" else "—",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.scoreRaw > 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Row {
                    IconButton(
                        onClick = { onUpdate(entry, entry.progress, (entry.scoreRaw - 10).coerceAtLeast(0)) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Lower score", modifier = Modifier.size(13.dp))
                    }
                    IconButton(
                        onClick = { onUpdate(entry, entry.progress, (entry.scoreRaw + 10).coerceAtMost(100)) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Raise score", modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

/**
 * Recent AniList activity feed for the profile section: native list rows with
 * cover, activity text, and relative timestamp. Rendered only when logged in.
 */
@Composable
fun DailyALActivityFeed(
    state: eu.kanade.tachiyomi.ui.discover.ActivityState,
    onRetry: () -> Unit,
    onEntryClick: (eu.kanade.tachiyomi.ui.discover.DailyALActivityEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is eu.kanade.tachiyomi.ui.discover.ActivityState.Idle,
        eu.kanade.tachiyomi.ui.discover.ActivityState.NotLoggedIn,
        -> {}

        is eu.kanade.tachiyomi.ui.discover.ActivityState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is eu.kanade.tachiyomi.ui.discover.ActivityState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.TextButton(onClick = onRetry) {
                    Text("Failed to load activity — tap to retry")
                }
            }
        }

        is eu.kanade.tachiyomi.ui.discover.ActivityState.Success -> {
            if (state.entries.isEmpty()) return
            Column(modifier = modifier.fillMaxWidth()) {
                DailyALSectionHeader(title = "Recent Activity")
                state.entries.take(10).forEach { activity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntryClick(activity) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (activity.mediaCoverUrl != null) {
                            AsyncImage(
                                model = activity.mediaCoverUrl,
                                contentDescription = activity.mediaTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 64.dp)
                                    .clip(DS.radiusSmall),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildString {
                                    append(activity.status.replace('_', ' '))
                                    if (activity.progress.isNotBlank()) append(" ${activity.progress}")
                                    append(" ")
                                    append(activity.mediaTitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (activity.createdAt > 0) {
                                Text(
                                    text = DateUtils.getRelativeTimeSpanString(
                                        activity.createdAt * 1000L,
                                    ).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Continue from your AniList" rail for the Discover home feed: CURRENT-list
 * entries rendered as poster cards; entries matching a library anime open that
 * entry directly, others fall back to global search.
 */
@Composable
fun DiscoverTrackedRail(
    items: List<eu.kanade.tachiyomi.ui.discover.TrackedRailItem>,
    onItemClick: (eu.kanade.tachiyomi.ui.discover.TrackedRailItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = stringResource(tachiyomi.i18n.tail.TLMR.strings.tracked_rail_title))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.mediaId }) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onItemClick(item) },
                ) {
                    Box {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 104.dp, height = 148.dp)
                                .clip(DS.radiusCard),
                        )
                        if (item.libraryAnimeId != null) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                            ) {
                                Text(
                                    text = "In library",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                        if (item.progress > 0) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                            ) {
                                Text(
                                    text = if (item.total > 0) {
                                        "EP ${item.progress}/${item.total}"
                                    } else {
                                        "EP ${item.progress}"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
