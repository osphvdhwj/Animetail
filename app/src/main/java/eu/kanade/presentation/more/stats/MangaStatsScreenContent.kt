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
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * World-Class Micro-to-Macro Statistics Screen for Manga.
 *
 * Provides layered visualization from high-level collection KPIs (Macro)
 * to deep-dive reading habits, release formats (Manga/Manhwa/Novels),
 * status distribution, authors/artists, genres, score histogram, and storage (Micro).
 */
@Composable
fun MangaStatsScreenContent(
    state: StatsScreenState.SuccessManga,
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
        item(key = "manga_granularity_selector") {
            StatsGranularitySelector(
                selectedMode = selectedGranularity,
                onSelectMode = { selectedGranularity = it },
            )
        }

        // --- SUPER MACRO SECTION ---
        if (showMacro) {
            // Hero Banner
            item(key = "manga_hero_banner") {
                StatsHeroBanner(
                    count = state.overview.libraryMangaCount,
                    label = "Manga & Novels in your library",
                    isAnime = false,
                    totalDurationMs = state.overview.totalReadDuration,
                )
            }

            // Quick Facts Bento Grid
            item(key = "manga_bento_grid") {
                StatsBentoGrid(
                    isAnime = false,
                    downloadCount = state.chapters.downloadCount,
                    completedCount = state.overview.completedMangaCount,
                    totalCount = state.overview.libraryMangaCount,
                    meanScore = state.trackers.meanScore,
                    activeCount = state.overview.readingCount,
                    backlogCount = state.overview.planToReadCount,
                    chaptersRead = state.chapters.readChapterCount,
                    totalChapters = state.chapters.totalChapterCount,
                )
            }

            // Read Time Bento Card
            item(key = "manga_read_time") {
                SectionCard("Total Read Duration") {
                    BentoTimeWidget(
                        totalDurationMs = state.overview.totalReadDuration,
                        isAnime = false,
                    )
                }
            }
        }

        // --- MICRO DEEP-DIVE SECTION ---
        if (showMicro) {
            // Reading Pace & Velocity
            item(key = "manga_pace_velocity") {
                PaceCard(
                    isAnime = false,
                    consumedCount = state.chapters.readChapterCount,
                    totalCount = state.chapters.totalChapterCount,
                    totalDurationMs = state.overview.totalReadDuration,
                    activeTitleCount = state.overview.readingCount,
                    averagePerTitle = state.overview.averageChaptersPerManga,
                )
            }

            // Format Distribution (Manga vs Manhwa vs Manhua vs Novels vs One-shot)
            if (state.overview.formatDistribution.isNotEmpty()) {
                item(key = "manga_format_distribution") {
                    FormatDistributionCard(
                        formatDistribution = state.overview.formatDistribution,
                        totalCount = state.overview.libraryMangaCount,
                        isAnime = false,
                    )
                }
            }

            // Granular Status Matrix Card
            item(key = "manga_status_matrix") {
                StatusMatrixCard(
                    isAnime = false,
                    watchingCount = state.overview.readingCount,
                    completedCount = state.overview.completedMangaCount,
                    planToWatchCount = state.overview.planToReadCount,
                    onHoldCount = state.overview.onHoldCount,
                    droppedCount = state.overview.droppedCount,
                    totalCount = state.overview.libraryMangaCount,
                )
            }
        }

        // Donut Chart (Shown in Macro or All)
        if (showMacro) {
            item(key = "manga_status_donut") {
                val segments = listOf(
                    DonutSegment("Reading", state.overview.readingCount, Color(0xFF6366F1)),
                    DonutSegment("Completed", state.overview.completedMangaCount, Color(0xFF10B981)),
                    DonutSegment("Plan to Read", state.overview.planToReadCount, Color(0xFF8B5CF6)),
                    DonutSegment("On Hold", state.overview.onHoldCount, Color(0xFFF59E0B)),
                    DonutSegment("Dropped", state.overview.droppedCount, Color(0xFFEF4444)),
                ).filter { it.count > 0 || state.overview.libraryMangaCount == 0 }

                SectionCard(stringResource(TLMR.strings.stats_status_distribution)) {
                    StatsDonutChart(
                        segments = segments,
                        centerLabel = "Manga",
                    )
                }
            }
        }

        // --- DETAILED BREAKDOWNS (Genres, Authors, Trackers, Scores) ---
        if (showMicro) {
            // Top Genres Section
            if (state.overview.topGenres.isNotEmpty()) {
                item(key = "manga_top_genres") {
                    SectionCard(stringResource(TLMR.strings.stats_top_genres_themes)) {
                        TopGenresWidget(
                            genres = state.overview.topGenres,
                            barColor = MaterialTheme.colorScheme.primary,
                            maxItems = 8,
                        )
                    }
                }
            }

            // Top Authors Section
            if (state.overview.topAuthors.isNotEmpty()) {
                item(key = "manga_top_authors") {
                    SectionCard("Top Authors & Creators") {
                        TopGenresWidget(
                            genres = state.overview.topAuthors,
                            title = "Top Authors",
                            barColor = MaterialTheme.colorScheme.secondary,
                            maxItems = 5,
                        )
                    }
                }
            }

            // Multi-Tracker Website Breakdown
            if (state.trackers.trackerSiteStats.isNotEmpty()) {
                item(key = "manga_multi_trackers") {
                    SectionCard(MR.strings.pref_category_tracking) {
                        MultiTrackerStatsCard(trackerStats = state.trackers.trackerSiteStats)
                    }
                }
            }

            // Score Distribution Histogram
            item(key = "manga_score_distribution") {
                SectionCard(stringResource(TLMR.strings.stats_score_distribution)) {
                    ScoreDistributionBarChart(
                        scoreDistribution = state.trackers.scoreDistribution,
                        meanScore = state.trackers.meanScore,
                    )
                }
            }

            // Storage & Local Footprint
            item(key = "manga_storage_footprint") {
                StorageFootprintCard(
                    isAnime = false,
                    downloadCount = state.chapters.downloadCount,
                    localTitlesCount = state.titles.localMangaCount,
                    globalUpdateCount = state.titles.globalUpdateItemCount,
                    totalTitles = state.overview.libraryMangaCount,
                )
            }

            // Classic Titles & Chapters Overview
            item(key = "manga_titles_stats") { TitlesStats(state.titles) }
            item(key = "manga_chapters_stats") { ChapterStats(state.chapters) }
        }
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
                subtitle = stringResource(MR.strings.label_read_duration),
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