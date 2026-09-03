package eu.kanade.presentation.history.anime.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

private val HistoryItemHeight = 100.dp

@Composable
fun AnimeHistoryItem(
    history: AnimeHistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastSecondSeen: Long? by remember { mutableStateOf(null) }
    var totalSeconds: Long? by remember { mutableStateOf(null) }
    LaunchedEffect(history.episodeId) {
        val getEpisode: GetEpisode = Injekt.get()
        val ep = try {
            getEpisode.await(history.episodeId)
        } catch (e: Exception) {
            null
        }
        if (ep != null && !ep.seen && ep.lastSecondSeen > 0L) {
            lastSecondSeen = ep.lastSecondSeen
            totalSeconds = ep.totalSeconds
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClickResume),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Video Cover Thumbnail with progress bar overlay
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                ItemCover.Book(
                    modifier = Modifier.fillMaxSize(),
                    data = history.coverData,
                    onClick = onClickCover,
                )
                if (progressFraction != null && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp),
            ) {
                Text(
                    text = history.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )

                val seenAt = remember { history.seenAt?.toTimestampString() ?: "" }

                val watchProgress: String? = lastSecondSeen?.let { last ->
                    stringResource(
                        AYMR.strings.episode_progress,
                        formatProgress(last),
                        formatProgress(totalSeconds ?: 0L),
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = seenAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (watchProgress != null) {
                    Text(
                        text = watchProgress,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!history.coverData.isAnimeFavorite) {
                    IconButton(
                        onClick = onClickFavorite,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(MR.strings.add_to_library),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onClickResume,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(MR.strings.action_resume),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = onClickDelete,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(MR.strings.action_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
