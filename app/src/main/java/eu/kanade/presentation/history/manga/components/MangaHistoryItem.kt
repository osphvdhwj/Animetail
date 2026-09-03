package eu.kanade.presentation.history.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp

private val HISTORY_ITEM_HEIGHT = 100.dp

@Composable
fun MangaHistoryItem(
    history: MangaHistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            // Book Cover Thumbnail
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
                val readAt = remember { history.readAt?.toTimestampString() ?: "" }
                val lastPage = history.lastPageRead.takeIf { it > 0L }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (history.chapterNumber > -1) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "CH ${formatChapterNumber(history.chapterNumber)}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = readAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (lastPage != null) {
                    Text(
                        text = stringResource(MR.strings.chapter_progress, (lastPage + 1).toInt()),
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
                if (!history.coverData.isMangaFavorite) {
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
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = stringResource(MR.strings.action_resume),
                        modifier = Modifier.size(20.dp),
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

@PreviewLightDark
@Composable
internal fun HistoryItemPreviews(
    @PreviewParameter(MangaHistoryWithRelationsProvider::class)
    historyWithRelations: MangaHistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            MangaHistoryItem(
                history = historyWithRelations,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
