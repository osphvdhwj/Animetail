package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.data.track.model.AiringEpisode

enum class AiringDay(val displayName: String, val dayOffset: Int) {
    TODAY("Today", 0),
    TOMORROW("Tomorrow", 1),
    DAY_AFTER("In 2 Days", 2),
    DAY_4("In 3 Days", 3),
    DAY_5("In 4 Days", 4),
    DAY_6("In 5 Days", 5),
    DAY_7("In 6 Days", 6),
    WEEK("This Week", 7);

    fun getComputedLabel(): String {
        if (this == WEEK) return "This Week"
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        val dayName = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(cal.time)
        return when (dayOffset) {
            0 -> "Today ($dayName)"
            1 -> "Tomorrow ($dayName)"
            else -> dayName
        }
    }
}

@Composable
fun AiringScheduleView(
    airingList: List<AiringEpisode>,
    selectedDay: AiringDay,
    onSelectDay: (AiringDay) -> Unit,
    onEpisodeClick: (AiringEpisode) -> Unit,
    onWatchClick: (AiringEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Day selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiringDay.entries.forEach { day ->
                val isSelected = day == selectedDay
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectDay(day) },
                    label = {
                        Text(
                            text = day.getComputedLabel(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        if (airingList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No airing episodes scheduled for ${selectedDay.getComputedLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(airingList, key = { it.id }) { episode ->
                    AiringEpisodeItemCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        onWatchClick = { onWatchClick(episode) },
                    )
                }
            }
        }
    }
}

@Composable
fun AiringEpisodeItemCard(
    episode: AiringEpisode,
    onClick: () -> Unit,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdownText = episode.formatCountdown()
    val isAired = countdownText.startsWith("Aired", true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Thumbnail
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .width(65.dp)
                    .aspectRatio(0.72f),
            ) {
                AsyncImage(
                    model = episode.coverUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Information Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Countdown Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAired) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isAired) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    ) {
                        Text(
                            text = countdownText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    if (episode.score > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Text(
                                text = "★ ${String.format(java.util.Locale.US, "%.1f", if (episode.score > 10) episode.score / 10f else episode.score)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (episode.format.isNotBlank()) {
                        Text(
                            text = episode.format,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Quick Play Shortcut
            Surface(
                onClick = onWatchClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Watch",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
