package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import eu.kanade.presentation.design.DS
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.discover.DailyALProfileStats
import eu.kanade.tachiyomi.ui.discover.ProfileState
import java.util.Locale

/**
 * Full AniList profile view for the Discover profile section: header with avatar
 * and name, summary stat cards, list-status donut, and score distribution chart.
 * Renders the appropriate state for loading / not-logged-in / error.
 */
@Composable
fun DailyALProfileContent(
    stats: DailyALProfileStats,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    internalScroll: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (internalScroll) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
            )
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DailyALProfileHeader(stats = stats, onRefresh = onRefresh)
        DailyALProfileSummaryCards(stats = stats)
        DailyALStatusDonut(stats = stats)
        DailyALScoreDistribution(stats = stats)
    }
}

/**
 * Banner-style header: avatar, username, and refresh action.
 */
@Composable
fun DailyALProfileHeader(
    stats: DailyALProfileStats,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = DS.radiusHero,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp),
            ) {
                if (stats.avatarUrl != null) {
                    AsyncImage(
                        model = stats.avatarUrl,
                        contentDescription = stats.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stats.userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "AniList profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

/**
 * Row of summary stat cards: days watched, episodes, entries, mean score.
 */
@Composable
fun DailyALProfileSummaryCards(
    stats: DailyALProfileStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val animatedEpisodes by androidx.compose.animation.core.animateIntAsState(
            targetValue = stats.totalEpisodes,
            animationSpec = DS.Motion.slide(),
            label = "profile_episodes",
        )
        val animatedEntries by androidx.compose.animation.core.animateIntAsState(
            targetValue = stats.totalEntries,
            animationSpec = DS.Motion.slide(),
            label = "profile_entries",
        )
        DailyALStatCard(
            label = "Days watched",
            value = String.format(Locale.US, "%.1f", stats.animeDaysWatched),
            modifier = Modifier.weight(1f),
        )
        DailyALStatCard(
            label = "Episodes",
            value = animatedEpisodes.toString(),
            modifier = Modifier.weight(1f),
        )
        DailyALStatCard(
            label = "Entries",
            value = animatedEntries.toString(),
            modifier = Modifier.weight(1f),
        )
        DailyALStatCard(
            label = "Mean score",
            value = if (stats.meanScore > 0) {
                String.format(Locale.US, "%.1f", stats.meanScore)
            } else {
                "—"
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DailyALStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = DS.radiusCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(DS.strokeHairline, DS.Glass.borderColor(0.30f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val STATUS_COLORS = mapOf(
    "CURRENT" to Color(0xFF4CAF50),
    "WATCHING" to Color(0xFF4CAF50),
    "PLANNING" to Color(0xFF2196F3),
    "COMPLETED" to Color(0xFF9C27B0),
    "PAUSED" to Color(0xFFFF9800),
    "DROPPED" to Color(0xFFF44336),
)

/**
 * Horizontal segmented bar of list statuses with counts and color legend.
 */
@Composable
fun DailyALStatusDonut(
    stats: DailyALProfileStats,
    modifier: Modifier = Modifier,
) {
    val entries = stats.statusDistribution.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
    if (entries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "List Status")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = DS.radiusCard,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(DS.radiusMedium),
                ) {
                    val total = entries.sumOf { it.value }.coerceAtLeast(1)
                    entries.forEach { (status, count) ->
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat() / total)
                                .fillMaxWidth()
                                .background(
                                    STATUS_COLORS[status.uppercase()] ?: Color(0xFF9E9E9E),
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                entries.forEach { (status, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        STATUS_COLORS[status.uppercase()] ?: Color(0xFF9E9E9E),
                                    ),
                            )
                            Text(
                                text = status.replace('_', ' ')
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1–10 score histogram built from the raw score distribution.
 */
@Composable
fun DailyALScoreDistribution(
    stats: DailyALProfileStats,
    modifier: Modifier = Modifier,
) {
    if (stats.scoreDistribution.isEmpty()) return

    val maxCount = stats.scoreDistribution.values.max().coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "Score Distribution")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = DS.radiusCard,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stats.scoreDistribution.entries
                    .sortedBy { it.key }
                    .forEach { (score, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = score.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(20.dp),
                                textAlign = TextAlign.End,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(DS.radiusMedium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(count.toFloat() / maxCount)
                                        .height(12.dp)
                                        .clip(DS.radiusMedium)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(28.dp),
                            )
                        }
                    }
            }
        }
    }
}

/**
 * Full-state wrapper rendering [DailyALProfileContent] or the matching
 * loading / login-required / error placeholder.
 */
@Composable
fun DailyALProfileStateContainer(
    state: ProfileState,
    onRetry: () -> Unit,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    internalScroll: Boolean = true,
) {
    when (state) {
        is ProfileState.Idle, is ProfileState.Loading -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading profile…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is ProfileState.NotLoggedIn -> {
            DailyALProfileMessage(
                message = "Log in to AniList to see your profile stats, streaks and score distribution.",
                actionLabel = "Open tracking settings",
                onAction = onLoginClick,
                modifier = modifier,
            )
        }

        is ProfileState.Error -> {
            DailyALProfileMessage(
                message = state.message,
                actionLabel = "Retry",
                onAction = onRetry,
                modifier = modifier,
            )
        }

        is ProfileState.Success -> DailyALProfileContent(
            stats = state.stats,
            onRefresh = onRefresh,
            modifier = modifier,
            internalScroll = internalScroll,
        )
    }
}

@Composable
private fun DailyALProfileMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
