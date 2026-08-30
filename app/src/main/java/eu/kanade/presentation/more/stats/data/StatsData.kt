package eu.kanade.presentation.more.stats.data

import androidx.compose.ui.graphics.Color

sealed interface StatsData {

    data class MangaOverview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
        val readingCount: Int = 0,
        val planToReadCount: Int = 0,
        val onHoldCount: Int = 0,
        val droppedCount: Int = 0,
        val topGenres: List<Pair<String, Int>> = emptyList(),
    ) : StatsData

    data class AnimeOverview(
        val libraryAnimeCount: Int,
        val completedAnimeCount: Int,
        val totalSeenDuration: Long,
        val watchingCount: Int = 0,
        val planToWatchCount: Int = 0,
        val onHoldCount: Int = 0,
        val droppedCount: Int = 0,
        val topGenres: List<Pair<String, Int>> = emptyList(),
        val topStudios: List<Pair<String, Int>> = emptyList(),
    ) : StatsData

    data class MangaTitles(
        val globalUpdateItemCount: Int,
        val startedMangaCount: Int,
        val localMangaCount: Int,
    ) : StatsData

    data class AnimeTitles(
        val globalUpdateItemCount: Int,
        val startedAnimeCount: Int,
        val localAnimeCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Episodes(
        val totalEpisodeCount: Int,
        val readEpisodeCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
        val trackerSiteStats: List<TrackerSiteStat> = emptyList(),
        val scoreDistribution: Map<Int, Int> = emptyMap(),
    ) : StatsData

    data class TrackerSiteStat(
        val trackerId: Long,
        val trackerName: String,
        val username: String? = null,
        val isLoggedIn: Boolean = false,
        val trackedCount: Int = 0,
        val meanScore: Double = Double.NaN,
        val logoRes: Int = 0,
        val color: Color = Color(0xFF6366F1),
    )
}
