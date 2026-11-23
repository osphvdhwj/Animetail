package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.BentoTimeWidget
import eu.kanade.presentation.more.stats.components.DonutSegment
import eu.kanade.presentation.more.stats.components.FormatDistributionCard
import eu.kanade.presentation.more.stats.components.MultiTrackerStatsCard
import eu.kanade.presentation.more.stats.components.PaceCard
import eu.kanade.presentation.more.stats.components.ScoreDistributionBarChart
import eu.kanade.presentation.more.stats.components.StatsBentoGrid
import eu.kanade.presentation.more.stats.components.StatsDonutChart
import eu.kanade.presentation.more.stats.components.StatsGranularity
import eu.kanade.presentation.more.stats.components.StatsGranularitySelector
import eu.kanade.presentation.more.stats.components.StatsHeroBanner
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.components.StatusMatrixCard
import eu.kanade.presentation.more.stats.components.StorageFootprintCard
import eu.kanade.presentation.more.stats.components.TopGenresWidget
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * World-Class Micro-to-Macro Statistics Screen for Anime.
 *
 * Provides layered visualization from high-level collection KPIs (Macro)
 * to deep-dive consumption habits, release formats, status distribution,
 * studios, genres, score histogram, and storage footprint (Micro).
 */
@Composable
fun AnimeStatsScreenContent(
    state: StatsScreenState.SuccessAnime,
    paddingValues: PaddingValues,
) {
    val statListState = rememberLazyListState()
    var selectedGranularity by rememberSaveable { mutableStateOf(StatsGranularity.ALL) }

    val showMacro = selectedGranularity == StatsGranularity.MACRO || selectedGranularity == StatsGranularity.ALL
    val showMicro = selectedGranularity == StatsGranularity.MICRO || selectedGranularity == StatsGranularity.ALL

    LazyColumn(
        state = statListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Granularity Selector
        item(key = "granularity_selector") {
            StatsGranularitySelector(
                selectedMode = selectedGranularity,
                onSelectMode = { selectedGranularity = it },
            )
        }

        // --- SUPER MACRO SECTION ---
        if (showMacro) {
            // Hero Banner
            item(key = "hero_banner") {
                StatsHeroBanner(
                    count = state.overview.libraryAnimeCount,
                    label = "Anime in your library",
                    isAnime = true,
                    totalDurationMs = state.overview.totalSeenDuration,
                )
            }

            // Quick Facts Bento Grid
            item(key = "bento_grid") {
                StatsBentoGrid(
                    isAnime = true,
                    downloadCount = state.episodes.downloadCount,
                    completedCount = state.overview.completedAnimeCount,
                    totalCount = state.overview.libraryAnimeCount,
                    meanScore = state.trackers.meanScore,
                    activeCount = state.overview.watchingCount,
                    backlogCount = state.overview.planToWatchCount,
                    episodesWatched = state.episodes.readEpisodeCount,
                    totalEpisodes = state.episodes.totalEpisodeCount,
                )
            }

            // Watch Time Bento Card
            item(key = "watch_time") {
                SectionCard(AYMR.strings.label_watched_duration) {
                    BentoTimeWidget(
                        totalDurationMs = state.overview.totalSeenDuration,
                        isAnime = true,
                    )
                }
            }
        }

        // --- MICRO DEEP-DIVE SECTION ---
        if (showMicro) {
            // Watching Pace & Consumption Velocity
            item(key = "pace_velocity") {
                PaceCard(
                    isAnime = true,
                    consumedCount = state.episodes.readEpisodeCount,
                    totalCount = state.episodes.totalEpisodeCount,
                    totalDurationMs = state.overview.totalSeenDuration,
                    activeTitleCount = state.overview.watchingCount,
                    averagePerTitle = state.overview.averageEpisodesPerAnime,
                )
            }

            // Format Distribution (TV vs Movie vs OVA vs ONA vs Special)
            if (state.overview.formatDistribution.isNotEmpty()) {
                item(key = "format_distribution") {
                    FormatDistributionCard(
                        formatDistribution = state.overview.formatDistribution,
                        totalCount = state.overview.libraryAnimeCount,
                        isAnime = true,
                    )
                }
            }

            // Granular Status Matrix Card
            item(key = "status_matrix") {
                StatusMatrixCard(
                    isAnime = true,
                    watchingCount = state.overview.watchingCount,
                    completedCount = state.overview.completedAnimeCount,
                    planToWatchCount = state.overview.planToWatchCount,
                    onHoldCount = state.overview.onHoldCount,
                    droppedCount = state.overview.droppedCount,
                    totalCount = state.overview.libraryAnimeCount,
                )
            }
        }

        // Donut Chart (Shown in Macro or All)
        if (showMacro) {
            item(key = "status_donut") {
                val segments = listOf(
                    DonutSegment("Watching", state.overview.watchingCount, Color(0xFF6366F1)),
                    DonutSegment("Completed", state.overview.completedAnimeCount, Color(0xFF10B981)),
                    DonutSegment("Plan to Watch", state.overview.planToWatchCount, Color(0xFF8B5CF6)),
                    DonutSegment("On Hold", state.overview.onHoldCount, Color(0xFFF59E0B)),
                    DonutSegment("Dropped", state.overview.droppedCount, Color(0xFFEF4444)),
                ).filter { it.count > 0 || state.overview.libraryAnimeCount == 0 }

                SectionCard(stringResource(TLMR.strings.stats_status_distribution)) {
                    StatsDonutChart(
                        segments = segments,
                        centerLabel = "Anime",
                    )
                }
            }
        }

        // --- DETAILED BREAKDOWNS (Genres, Studios, Trackers, Scores) ---
        if (showMicro) {
            // Top Genres Section
            if (state.overview.topGenres.isNotEmpty()) {
                item(key = "top_genres") {
                    SectionCard(stringResource(TLMR.strings.stats_top_genres_themes)) {
                        TopGenresWidget(
                            genres = state.overview.topGenres,
                            barColor = MaterialTheme.colorScheme.primary,
                            maxItems = 8,
                        )
                    }
                }
            }

            // Top Studios Section
            if (state.overview.topStudios.isNotEmpty()) {
                item(key = "top_studios") {
                    SectionCard(stringResource(TLMR.strings.stats_top_studios)) {
                        TopGenresWidget(
                            genres = state.overview.topStudios,
                            title = stringResource(TLMR.strings.stats_top_studios_short),
                            barColor = MaterialTheme.colorScheme.secondary,
                            maxItems = 5,
                        )
                    }
                }
            }

            // Multi-Tracker Website Breakdown
            if (state.trackers.trackerSiteStats.isNotEmpty()) {
                item(key = "multi_trackers") {
                    SectionCard(MR.strings.pref_category_tracking) {
                        MultiTrackerStatsCard(trackerStats = state.trackers.trackerSiteStats)
                    }
                }
            }

            // Score Distribution Histogram
            item(key = "score_distribution") {
                SectionCard(stringResource(TLMR.strings.stats_score_distribution)) {
                    ScoreDistributionBarChart(
                        scoreDistribution = state.trackers.scoreDistribution,
                        meanScore = state.trackers.meanScore,
                    )
                }
            }

            // Storage & Local Footprint
            item(key = "storage_footprint") {
                StorageFootprintCard(
                    isAnime = true,
                    downloadCount = state.episodes.downloadCount,
                    localTitlesCount = state.titles.localAnimeCount,
                    globalUpdateCount = state.titles.globalUpdateItemCount,
                    totalTitles = state.overview.libraryAnimeCount,
                )
            }

            // Classic Titles & Episodes Overview
            item(key = "titles_stats") { TitlesStats(state.titles) }
            item(key = "episodes_stats") { EpisodeStats(state.episodes) }
        }
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
