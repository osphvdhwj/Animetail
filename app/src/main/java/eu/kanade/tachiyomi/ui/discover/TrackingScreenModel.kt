package eu.kanade.tachiyomi.ui.discover

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

enum class TrackerSource(
    val id: Long,
    val displayName: String,
    val supportsAnime: Boolean,
    val supportsManga: Boolean
) {
    ANILIST(TrackerManager.ANILIST, "AniList", true, true),
    MYANIMELIST(1L, "MyAnimeList", true, true),
    KITSU(TrackerManager.KITSU, "Kitsu", true, true),
    SHIKIMORI(4L, "Shikimori", true, true),
    BANGUMI(5L, "Bangumi", true, true),
    SIMKL(TrackerManager.SIMKL, "Simkl", true, false),
    MANGAUPDATES(7L, "MangaUpdates", false, true);

    fun isTrackerLoggedIn(trackerManager: TrackerManager): Boolean {
        return trackerManager.get(id)?.isLoggedIn ?: false
    }
}

class TrackingScreenModel(
    private val trackerManager: TrackerManager = Injekt.get()
) : ScreenModel {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _selectedTracker = MutableStateFlow(TrackerSource.ANILIST)
    val selectedTracker: StateFlow<TrackerSource> = _selectedTracker.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentJob: Job? = null

    init {
        loadTracking()
    }

    fun isTrackerLoggedIn(source: TrackerSource): Boolean {
        return source.isTrackerLoggedIn(trackerManager)
    }

    fun selectTracker(tracker: TrackerSource) {
        _selectedTracker.value = tracker
        loadTracking()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadTracking(query)
    }

    fun loadTracking(query: String = _searchQuery.value) {
        currentJob?.cancel()
        currentJob = screenModelScope.launch {
            _state.update { TrackingState.Loading }
            val tracker = _selectedTracker.value
            try {
                var animeList: List<AnimeTrackSearch> = emptyList()
                var mangaList: List<MangaTrackSearch> = emptyList()

                when (tracker) {
                    TrackerSource.ANILIST -> {
                        val anilist = trackerManager.aniList
                        if (query.isNotBlank()) {
                            animeList = anilist.searchAnime(query)
                            mangaList = anilist.searchManga(query)
                        } else {
                            animeList = anilist.getTrendingAnime(1)
                            mangaList = anilist.getTrendingManga(1)
                        }
                    }
                    TrackerSource.MYANIMELIST -> {
                        val mal = trackerManager.myAnimeList
                        if (query.isNotBlank()) {
                            animeList = mal.searchAnime(query)
                            mangaList = mal.searchManga(query)
                        } else {
                            animeList = mal.getPopularAnime()
                            mangaList = mal.getPopularManga()
                        }
                    }
                    TrackerSource.KITSU -> {
                        val kitsu = trackerManager.kitsu
                        val q = query.ifBlank { "a" }
                        animeList = kitsu.searchAnime(q)
                        mangaList = kitsu.searchManga(q)
                    }
                    TrackerSource.SHIKIMORI -> {
                        val shikimori = trackerManager.shikimori
                        val q = query.ifBlank { "" }
                        animeList = shikimori.searchAnime(q)
                        mangaList = shikimori.searchManga(q)
                    }
                    TrackerSource.BANGUMI -> {
                        val bangumi = trackerManager.bangumi
                        val q = query.ifBlank { "2024" }
                        animeList = bangumi.searchAnime(q)
                        mangaList = bangumi.searchManga(q)
                    }
                    TrackerSource.SIMKL -> {
                        val simkl = trackerManager.simkl
                        val q = query.ifBlank { "a" }
                        animeList = simkl.searchAnime(q)
                    }
                    TrackerSource.MANGAUPDATES -> {
                        val mu = trackerManager.mangaUpdates
                        val q = query.ifBlank { "a" }
                        mangaList = mu.searchManga(q)
                    }
                }

                _state.update {
                    TrackingState.Success(
                        trendingAnime = animeList,
                        trendingManga = mangaList,
                        trackerSource = tracker
                    )
                }
            } catch (e: Exception) {
                // If unauthenticated or tracker error occurs, gracefully attempt public fallback
                if (tracker != TrackerSource.ANILIST) {
                    try {
                        val fallbackAnime = trackerManager.aniList.getTrendingAnime(1)
                        val fallbackManga = trackerManager.aniList.getTrendingManga(1)
                        _state.update {
                            TrackingState.Success(
                                trendingAnime = fallbackAnime,
                                trendingManga = fallbackManga,
                                trackerSource = TrackerSource.ANILIST
                            )
                        }
                        return@launch
                    } catch (_: Exception) {}
                }
                _state.update { TrackingState.Error(e.message ?: "Failed to load tracking data") }
            }
        }
    }
}

sealed interface TrackingState {
    data object Loading : TrackingState
    data class Success(
        val trendingAnime: List<AnimeTrackSearch>,
        val trendingManga: List<MangaTrackSearch>,
        val trackerSource: TrackerSource = TrackerSource.ANILIST
    ) : TrackingState
    data class Error(val message: String) : TrackingState
}
