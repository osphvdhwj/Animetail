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
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun MangaStatsScreenContent(
    state: StatsScreenState.SuccessManga,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        // Read Time Bento Card
        item {
            SectionCard("Total Read Duration") {
                BentoTimeWidget(
                    totalDurationMs = state.overview.totalReadDuration,
                    isAnime = false,
                )
            }
        }

        // Overview Numbers
        item { OverviewSection(state.overview) }

        // Read Status Donut Chart
        item {
            val segments = listOf(
                DonutSegment("Reading", state.overview.readingCount, Color(0xFF6366F1)),
                DonutSegment("Completed", state.overview.completedMangaCount, Color(0xFF10B981)),
                DonutSegment("Plan to Read", state.overview.planToReadCount, Color(0xFF8B5CF6)),
                DonutSegment("On Hold", state.overview.onHoldCount, Color(0xFFF59E0B)),
                DonutSegment("Dropped", state.overview.droppedCount, Color(0xFFEF4444)),
            ).filter { it.count > 0 || state.overview.libraryMangaCount == 0 }

            SectionCard("Status Distribution") {
                StatsDonutChart(
                    segments = segments,
                    centerLabel = "Manga",
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

        // Titles and Chapters
        item { TitlesStats(state.titles) }
        item { ChapterStats(state.chapters) }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.MangaOverview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    SectionCard(MR.strings.label_overview_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsOverviewItem(
                title = data.libraryMangaCount.toString(),
                subtitle = stringResource(MR.strings.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = "Read Duration",
                icon = Icons.Outlined.Schedule,
            )
            StatsOverviewItem(
                title = data.completedMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
            )
        }
    }
}

@Composable
private fun LazyItemScope.TitlesStats(
    data: StatsData.MangaTitles,
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
                title = data.startedMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_started),
            )
            StatsItem(
                title = data.localMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_local),
            )
        }
    }
}

@Composable
private fun LazyItemScope.ChapterStats(
    data: StatsData.Chapters,
) {
    SectionCard(MR.strings.chapters) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsItem(
                title = data.totalChapterCount.toString(),
                subtitle = "Total Chapters",
            )
            StatsItem(
                title = data.readChapterCount.toString(),
                subtitle = "Read Chapters",
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
