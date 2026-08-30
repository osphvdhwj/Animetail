package eu.kanade.presentation.more.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.stats.components.BentoTimeWidget
import eu.kanade.presentation.more.stats.components.DonutSegment
import eu.kanade.presentation.more.stats.components.MultiTrackerStatsCard
import eu.kanade.presentation.more.stats.components.StatsDonutChart
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.components.TopGenresWidget
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun AnimeStatsScreenContent(
    state: StatsScreenState.SuccessAnime,
    paddingValues: PaddingValues,
) {
    val statListState = rememberLazyListState()
    LazyColumn(
        state = statListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        // Watch Time Bento Card
        item {
            SectionCard(AYMR.strings.label_watched_duration) {
                BentoTimeWidget(
                    totalDurationMs = state.overview.totalSeenDuration,
                    isAnime = true,
                )
            }
        }

        // Overview Numbers
        item { OverviewSection(state.overview) }

        // Watch Status Donut Chart
        item {
            val segments = listOf(
                DonutSegment("Watching", state.overview.watchingCount, Color(0xFF6366F1)),
                DonutSegment("Completed", state.overview.completedAnimeCount, Color(0xFF10B981)),
                DonutSegment("Plan to Watch", state.overview.planToWatchCount, Color(0xFF8B5CF6)),
                DonutSegment("On Hold", state.overview.onHoldCount, Color(0xFFF59E0B)),
                DonutSegment("Dropped", state.overview.droppedCount, Color(0xFFEF4444)),
            ).filter { it.count > 0 || state.overview.libraryAnimeCount == 0 }

            SectionCard("Status Distribution") {
                StatsDonutChart(
                    segments = segments,
                    centerLabel = "Anime",
                )
            }
        }

        // Top Genres Section
        if (state.overview.topGenres.isNotEmpty()) {
            item {
                SectionCard("Top Genres") {
                    TopGenresWidget(
                        genres = state.overview.topGenres,
                        barColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Top Studios Section
        if (state.overview.topStudios.isNotEmpty()) {
            item {
                SectionCard("Top Studios / Authors") {
                    TopGenresWidget(
                        genres = state.overview.topStudios,
                        title = "Top Studios",
                        barColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        // Multi-Tracker Website Breakdown
        if (state.trackers.trackerSiteStats.isNotEmpty()) {
            item {
                SectionCard(MR.strings.pref_category_tracking) {
                    MultiTrackerStatsCard(trackerStats = state.trackers.trackerSiteStats)
                }
            }
        }

        // Score Distribution
        item { ScoreDistributionSection(state.trackers) }

        // Titles and Episodes
        item { TitlesStats(state.titles) }
        item { EpisodeStats(state.episodes) }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.AnimeOverview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalSeenDuration) {
        data.totalSeenDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    SectionCard(MR.strings.label_overview_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsOverviewItem(
                title = data.libraryAnimeCount.toString(),
                subtitle = stringResource(MR.strings.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(AYMR.strings.label_watched_duration),
                icon = Icons.Outlined.Schedule,
            )
            StatsOverviewItem(
                title = data.completedAnimeCount.toString(),
                subtitle = stringResource(MR.strings.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
            )
        }
    }
}

@Composable
private fun LazyItemScope.TitlesStats(
    data: StatsData.AnimeTitles,
) {
    SectionCard(MR.strings.label_titles_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsItem(
                title = data.globalUpdateItemCount.toString(),
                subtitle = "In Library Updates",
            )
            StatsItem(
                title = data.startedAnimeCount.toString(),
                subtitle = stringResource(MR.strings.label_started),
            )
            StatsItem(
                title = data.localAnimeCount.toString(),
                subtitle = stringResource(MR.strings.label_local),
            )
        }
    }
}

@Composable
private fun LazyItemScope.EpisodeStats(
    data: StatsData.Episodes,
) {
    SectionCard(AYMR.strings.episodes) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsItem(
                title = data.totalEpisodeCount.toString(),
                subtitle = "Total Episodes",
            )
            StatsItem(
                title = data.readEpisodeCount.toString(),
                subtitle = "Watched Episodes",
            )
            StatsItem(
                title = data.downloadCount.toString(),
                subtitle = stringResource(MR.strings.label_downloaded),
            )
        }
    }
}

@Composable
private fun LazyItemScope.ScoreDistributionSection(
    data: StatsData.Trackers,
) {
    val meanScore = data.meanScore
    val meanScoreFormatted = if (meanScore.isNaN()) {
        "—"
    } else {
        String.format(Locale.getDefault(), "%.1f", meanScore)
    }

    SectionCard("Score Distribution") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Mean Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "★ $meanScoreFormatted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Proportional Score Color Band (Red <60 / Orange 60-74 / Blue 75-84 / Green 85+)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                Box(modifier = Modifier.weight(1.5f).background(Color(0xFFEF4444)))
                Box(modifier = Modifier.weight(2.5f).background(Color(0xFFF97316)))
                Box(modifier = Modifier.weight(3.5f).background(Color(0xFF3B82F6)))
                Box(modifier = Modifier.weight(2.5f).background(Color(0xFF10B981)))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScoreLegendDot(color = Color(0xFFEF4444), label = "<60")
                ScoreLegendDot(color = Color(0xFFF97316), label = "60-74")
                ScoreLegendDot(color = Color(0xFF3B82F6), label = "75-84")
                ScoreLegendDot(color = Color(0xFF10B981), label = "85+")
            }
        }
    }
}

@Composable
private fun ScoreLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}
