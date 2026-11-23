package eu.kanade.presentation.history.anime.components

import androidx.collection.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

private data class EpisodeMetadata(
    val episodeName: String?,
    val isSeen: Boolean,
    val lastSecondSeen: Long?,
    val totalSeconds: Long?,
)

private val episodeMetadataCache = LruCache<Long, EpisodeMetadata>(300)
private val THUMBNAIL_GRADIENT = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
)

/**
 * Modern YouTube-style anime history list item with thumbnail preview, progress bar,
 * and contextual actions (Resume, Favorite, Delete).
 */
@Composable
fun AnimeHistoryItem(
    history: AnimeHistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    onMarkSeen: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cached = remember(history.episodeId) { episodeMetadataCache[history.episodeId] }
    var episodeName: String? by remember(history.episodeId) { mutableStateOf(cached?.episodeName) }
    var lastSecondSeen: Long? by remember(history.episodeId) { mutableStateOf(cached?.lastSecondSeen) }
    var totalSeconds: Long? by remember(history.episodeId) { mutableStateOf(cached?.totalSeconds) }
    var isSeen: Boolean by remember(history.episodeId) { mutableStateOf(cached?.isSeen ?: false) }
    var showMenu by remember { mutableStateOf(false) }

    if (cached == null) {
        LaunchedEffect(history.episodeId) {
            val getEpisode: GetEpisode = Injekt.get()
            val ep = try {
                getEpisode.await(history.episodeId)
            } catch (_: Throwable) {
                null
            }
            if (ep != null) {
                val name = ep.name.takeIf { it.isNotBlank() }
                val seen = ep.seen
                val lastSec = if (!ep.seen && ep.lastSecondSeen > 0L) ep.lastSecondSeen else null
                val totalSec = if (!ep.seen && ep.lastSecondSeen > 0L) ep.totalSeconds else null
                episodeName = name
                isSeen = seen
                lastSecondSeen = lastSec
                totalSeconds = totalSec
                episodeMetadataCache.put(history.episodeId, EpisodeMetadata(name, seen, lastSec, totalSec))
            }
        }
    }

    val progressFraction = remember(lastSecondSeen, totalSeconds) {
        val last = lastSecondSeen
        val total = totalSeconds
        if (last != null && total != null && total > 0L) {
            (last.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onClickResume),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // YouTube-style Widescreen Video Thumbnail with Duration / Watched Overlay & Red Progress Bar
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                ItemCover.Book(
                    modifier = Modifier.fillMaxSize(),
                    data = history.coverData,
                    onClick = onClickCover,
                )

                // Dark vignette gradient for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(THUMBNAIL_GRADIENT),
                )

                // YouTube-style Duration / Progress / Watched badge in bottom right corner
                val durationBadgeText: String? = when {
                    isSeen -> "✓ Watched"
                    lastSecondSeen != null && totalSeconds != null && totalSeconds!! > 0L -> {
                        val remainingSec = (totalSeconds!! - lastSecondSeen!!).coerceAtLeast(0L)
                        val remainingMin = TimeUnit.MILLISECONDS.toMinutes(remainingSec)
                        if (remainingMin > 0) "${remainingMin}m left" else formatProgress(lastSecondSeen!!)
                    }
                    totalSeconds != null && totalSeconds!! > 0L -> formatProgress(totalSeconds!!)
                    else -> null
                }

                if (durationBadgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 6.dp, end = 5.dp),
                    ) {
                        Text(
                            text = durationBadgeText,
                            color = if (isSeen) Color(0xFF4CAF50) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }

                // YouTube Signature Red Watch Progress Bar
                if (progressFraction != null && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color(0xFFFF0000), // YouTube Red
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // Rich Details Column (Series Title, Episode Tag + Name, Timestamp, Progress Percentage)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp),
            ) {
                Text(
                    text = history.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Episode Number & Episode Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (history.episodeNumber > -1) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "EP ${formatEpisodeNumber(history.episodeNumber)}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (!episodeName.isNullOrBlank()) {
                        Text(
                            text = episodeName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Seen timestamp & watch progress percentage
                val seenAt = remember { history.seenAt?.toTimestampString() ?: "" }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Text(
                        text = seenAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )

                    if (progressFraction != null && progressFraction > 0f) {
                        Text(
                            text = " • ${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Modern Actions: Filled Resume Play Button + 3-Dots Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledIconButton(
                    onClick = onClickResume,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(MR.strings.action_resume),
                        modifier = Modifier.size(18.dp),
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (onMarkSeen != null) {
                            DropdownMenuItem(
                                text = { Text(if (isSeen) "Mark as unseen" else "Mark as seen") },
                                leadingIcon = {
                                    Icon(
                                        if (isSeen) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMarkSeen(!isSeen)
                                },
                            )
                        }
                        if (!history.coverData.isAnimeFavorite) {
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.add_to_library)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onClickFavorite()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.action_delete)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClickDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(AnimeHistoryWithRelationsProvider::class)
    historyWithRelations: AnimeHistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            AnimeHistoryItem(
                history = historyWithRelations,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}