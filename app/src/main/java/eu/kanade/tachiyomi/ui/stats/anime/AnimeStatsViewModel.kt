package eu.kanade.tachiyomi.ui.stats.anime

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.core.util.fastCountNot
import eu.kanade.core.util.fastFilterNot
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.source.local.entries.anime.isLocal

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class AnimeStatsViewModel(
    private val downloadManager: AnimeDownloadManager,
    private val getAnimelibAnime: GetLibraryAnime,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    private val getTracks: GetAnimeTracks,
    private val preferences: LibraryPreferences,
    private val trackerManager: TrackerManager,
) : ViewModel() {

    val state: StateFlow<StatsScreenState>
        field = MutableStateFlow<StatsScreenState>(StatsScreenState.Loading)

    private val loggedInTrackers by lazy { trackerManager.loggedInTrackers().filter { it is AnimeTracker } }

    init {
        viewModelScope.launchIO {
            val animelibAnime = getAnimelibAnime.await()

            val distinctLibraryAnime = animelibAnime.fastDistinctBy { it.id }

            val animeTrackMap = getAnimeTrackMap(distinctLibraryAnime)
            val scoredAnimeTrackerMap = getScoredAnimeTrackMap(animeTrackMap)

            val meanScore = getTrackMeanScore(scoredAnimeTrackerMap)

            val watchingCount = distinctLibraryAnime.count { it.hasStarted && it.unseenCount > 0L }
            val completedCount = distinctLibraryAnime.count {
                it.anime.status.toInt() == SAnime.COMPLETED && it.unseenCount == 0L
            }
            val planToWatchCount = distinctLibraryAnime.count { !it.hasStarted }
            val onHoldCount = distinctLibraryAnime.count { it.anime.status.toInt() == SAnime.ON_HIATUS }
            val droppedCount = distinctLibraryAnime.count { it.anime.status.toInt() == SAnime.CANCELLED }

            // Extract genres
            val genreCounts = mutableMapOf<String, Int>()
            distinctLibraryAnime.forEach { libAnime ->
                libAnime.anime.genre?.forEach { rawGenre ->
                    rawGenre.split(",", "/").map { it.trim() }.filter { it.isNotBlank() }.forEach { g ->
                        genreCounts[g] = (genreCounts[g] ?: 0) + 1
                    }
                }
            }
            val topGenres = genreCounts.entries
                .sortedByDescending { it.value }
                .take(8)
                .map { it.key to it.value }

            // Extract studios / authors
            val studioCounts = mutableMapOf<String, Int>()
            distinctLibraryAnime.forEach { libAnime ->
                val studio = libAnime.anime.author?.trim()?.takeIf { it.isNotBlank() }
                    ?: libAnime.anime.artist?.trim()?.takeIf { it.isNotBlank() }
                if (studio != null) {
                    studioCounts[studio] = (studioCounts[studio] ?: 0) + 1
                }
            }
            val topStudios = studioCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            // Score Distribution across trackers
            val scoreDist = mutableMapOf<Int, Int>()
            scoredAnimeTrackerMap.values.flatten().forEach { track ->
                val s = get10PointScore(track).toInt().coerceIn(1, 10)
                scoreDist[s] = (scoreDist[s] ?: 0) + 1
            }

            // Per-Tracker Statistics Breakdown (AniList, MAL, Kitsu, Shikimori, Bangumi, Simkl, etc.)
            val allAnimeTrackers = trackerManager.trackers.filter { it is AnimeTracker }
            val trackerSiteStats = allAnimeTrackers.map { tracker ->
                val tracksForTracker = animeTrackMap.values.flatten().filter { it.trackerId == tracker.id }
                val scoredTracks = tracksForTracker.filter { it.score > 0.0 }
                val avgScore = if (scoredTracks.isNotEmpty()) {
                    scoredTracks.map(::get10PointScore).filter { !it.isNaN() }.average()
                } else Double.NaN

                StatsData.TrackerSiteStat(
                    trackerId = tracker.id,
                    trackerName = tracker.name,
                    username = if (tracker.isLoggedIn) tracker.getDisplayUsername().ifBlank { tracker.getUsername() } else null,
                    isLoggedIn = tracker.isLoggedIn,
                    trackedCount = tracksForTracker.size,
                    meanScore = avgScore,
                    logoRes = tracker.getLogo(),
                )
            }

            val overviewStatData = StatsData.AnimeOverview(
                libraryAnimeCount = distinctLibraryAnime.size,
                completedAnimeCount = completedCount,
                totalSeenDuration = getWatchTime(distinctLibraryAnime),
                watchingCount = watchingCount,
                planToWatchCount = planToWatchCount,
                onHoldCount = onHoldCount,
                droppedCount = droppedCount,
                topGenres = topGenres,
                topStudios = topStudios,
            )

            val titlesStatData = StatsData.AnimeTitles(
                globalUpdateItemCount = getGlobalUpdateItemCount(animelibAnime),
                startedAnimeCount = distinctLibraryAnime.count { it.hasStarted },
                localAnimeCount = distinctLibraryAnime.count { it.anime.isLocal() },
            )

            val chaptersStatData = StatsData.Episodes(
                totalEpisodeCount = distinctLibraryAnime.sumOf { it.totalCount }.toInt(),
                readEpisodeCount = distinctLibraryAnime.sumOf { it.seenCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = animeTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedInTrackers.size,
                trackerSiteStats = trackerSiteStats,
                scoreDistribution = scoreDist,
            )

            state.update {
                StatsScreenState.SuccessAnime(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    episodes = chaptersStatData,
                    trackers = trackersStatData,
                )
            }
        }
    }

    private fun getGlobalUpdateItemCount(libraryAnime: List<LibraryAnime>): Int {
        val includedCategories = preferences.animeUpdateCategories.get().map { it.toLong() }
        val includedAnime = if (includedCategories.isNotEmpty()) {
            libraryAnime.filter { it.category in includedCategories }
        } else {
            libraryAnime
        }

        val excludedCategories = preferences.animeUpdateCategoriesExclude.get().map { it.toLong() }
        val excludedMangaIds = if (excludedCategories.isNotEmpty()) {
            libraryAnime.fastMapNotNull { anime ->
                anime.id.takeIf { anime.category in excludedCategories }
            }
        } else {
            emptyList()
        }

        val updateRestrictions = preferences.autoUpdateMangaRestrictions.get()
        return includedAnime
            .fastFilterNot { it.anime.id in excludedMangaIds }
            .fastDistinctBy { it.anime.id }
            .fastCountNot {
                (ENTRY_NON_COMPLETED in updateRestrictions && it.anime.status.toInt() == SAnime.COMPLETED) ||
                    (ENTRY_HAS_UNVIEWED in updateRestrictions && it.unseenCount != 0L) ||
                    (ENTRY_NON_VIEWED in updateRestrictions && it.totalCount > 0 && !it.hasStarted)
            }
    }

    private suspend fun getAnimeTrackMap(libraryAnime: List<LibraryAnime>): Map<Long, List<AnimeTrack>> {
        val loggedInTrackerIds = loggedInTrackers.map { it.id }.toHashSet()
        return libraryAnime.associate { anime ->
            val tracks = getTracks.await(anime.id)
                .fastFilter { it.trackerId in loggedInTrackerIds }

            anime.id to tracks
        }
    }

    private suspend fun getWatchTime(libraryAnimeList: List<LibraryAnime>): Long {
        var watchTime = 0L
        libraryAnimeList.forEach { libraryAnime ->
            getEpisodesByAnimeId.await(libraryAnime.anime.id).forEach { episode ->
                watchTime += if (episode.seen) {
                    episode.totalSeconds
                } else {
                    episode.lastSecondSeen
                }
            }
        }

        return watchTime
    }

    private fun getScoredAnimeTrackMap(animeTrackMap: Map<Long, List<AnimeTrack>>): Map<Long, List<AnimeTrack>> {
        return animeTrackMap.mapNotNull { (animeId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            animeId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredAnimeTrackMap: Map<Long, List<AnimeTrack>>): Double {
        return scoredAnimeTrackMap
            .map { (_, tracks) ->
                tracks.map(::get10PointScore).average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: AnimeTrack): Double {
        val service = trackerManager.get(track.trackerId)!!
        return service.animeService.get10PointScore(track)
    }

    fun exportToCsv(context: android.content.Context) {
        viewModelScope.launchIO {
            try {
                val animelibAnime = getAnimelibAnime.await()
                val distinctLibraryAnime = animelibAnime.fastDistinctBy { it.id }

                val csvBuilder = StringBuilder()
                csvBuilder.append("Title,Status,Episodes Seen,Total Episodes,Time Watched (Minutes),Last Watched\n")

                distinctLibraryAnime.forEach { libAnime ->
                    val title = libAnime.anime.title.replace("\"", "\"\"")
                    val status = when (libAnime.anime.status.toInt()) {
                        SAnime.COMPLETED -> "Completed"
                        SAnime.ONGOING -> "Ongoing"
                        else -> "Unknown"
                    }
                    val seen = libAnime.seenCount
                    val total = libAnime.totalCount

                    var watchTimeSeconds = 0L
                    getEpisodesByAnimeId.await(libAnime.anime.id).forEach { episode ->
                        watchTimeSeconds += if (episode.seen) {
                            episode.totalSeconds
                        } else {
                            episode.lastSecondSeen
                        }
                    }
                    val watchTimeMinutes = watchTimeSeconds / 60

                    val lastWatched = if (libAnime.lastSeen > 0) {
                        val date = java.util.Date(libAnime.lastSeen)
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        format.format(date)
                    } else {
                        "Never"
                    }

                    csvBuilder.append("\"$title\",$status,$seen,$total,$watchTimeMinutes,$lastWatched\n")
                }

                val filename = "Animetail_Watch_Stats_${System.currentTimeMillis()}.csv"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvBuilder.toString().toByteArray())
                        }
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, filename)
                    file.writeBytes(csvBuilder.toString().toByteArray())
                }

                tachiyomi.core.common.util.lang.withUIContext {
                    context.toast("Stats exported to Downloads folder!")
                }
            } catch (e: Exception) {
                tachiyomi.core.common.util.lang.withUIContext {
                    context.toast("Failed to export stats: ${e.message}")
                }
            }
        }
    }
}
