package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch

@Composable
fun AnimiteAnimeMediaRow(
    title: String,
    items: List<AnimeTrackSearch>,
    onItemClick: (AnimeTrackSearch) -> Unit,
    modifier: Modifier = Modifier,
    showRank: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (onSeeAllClick != null) {
                IconButton(onClick = onSeeAllClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "See all",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items) { index, anime ->
                val formattedScore = if (anime.score > 0) {
                    if (anime.score > 10) {
                        String.format(java.util.Locale.US, "%.1f", anime.score / 10f)
                    } else {
                        String.format(java.util.Locale.US, "%.1f", anime.score)
                    }
                } else null

                AnimiteMediaCard(
                    title = anime.title,
                    imageUrl = anime.cover_url,
                    score = formattedScore,
                    format = anime.publishing_type.ifBlank { null },
                    status = anime.publishing_status.ifBlank { null },
                    rank = if (showRank) index + 1 else null,
                    onClick = { onItemClick(anime) },
                )
            }
        }
    }
}

@Composable
fun AnimiteMangaMediaRow(
    title: String,
    items: List<MangaTrackSearch>,
    onItemClick: (MangaTrackSearch) -> Unit,
    modifier: Modifier = Modifier,
    showRank: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (onSeeAllClick != null) {
                IconButton(onClick = onSeeAllClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "See all",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items) { index, manga ->
                val formattedScore = if (manga.score > 0) {
                    if (manga.score > 10) {
                        String.format(java.util.Locale.US, "%.1f", manga.score / 10f)
                    } else {
                        String.format(java.util.Locale.US, "%.1f", manga.score)
                    }
                } else null

                AnimiteMediaCard(
                    title = manga.title,
                    imageUrl = manga.cover_url,
                    score = formattedScore,
                    format = manga.publishing_type.ifBlank { null },
                    status = manga.publishing_status.ifBlank { null },
                    rank = if (showRank) index + 1 else null,
                    onClick = { onItemClick(manga) },
                )
            }
        }
    }
}
