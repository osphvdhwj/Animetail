package eu.kanade.tachiyomi.ui.discover

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.tachiyomi.data.discover.cache.CachedAnimeEntry
import eu.kanade.tachiyomi.data.discover.cache.CachedMangaEntry
import eu.kanade.tachiyomi.data.discover.cache.DiscoverCacheStore
import eu.kanade.tachiyomi.data.discover.cache.TrackerCacheData
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.discover.components.AiringDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.TimeZone

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
    private val trackerManager: TrackerManager = Injekt.get(),
    private val cacheStore: DiscoverCacheStore = Injekt.get(),
    private val discoverPreferences: DiscoverPreferences = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _selectedTracker = MutableStateFlow(TrackerSource.ANILIST)
    val selectedTracker: StateFlow<TrackerSource> = _selectedTracker.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAnimeDetails = MutableStateFlow<AnimeTrackSearch?>(null)
    val selectedAnimeDetails: StateFlow<AnimeTrackSearch?> = _selectedAnimeDetails.asStateFlow()

    private val _selectedMangaDetails = MutableStateFlow<MangaTrackSearch?>(null)
    val selectedMangaDetails: StateFlow<MangaTrackSearch?> = _selectedMangaDetails.asStateFlow()

    private val _selectedAiringDay = MutableStateFlow(AiringDay.TODAY)
    val selectedAiringDay: StateFlow<AiringDay> = _selectedAiringDay.asStateFlow()

    private val _airingSchedule = MutableStateFlow<List<AiringEpisode>>(emptyList())
    val airingSchedule: StateFlow<List<AiringEpisode>> = _airingSchedule.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSwipeMode = MutableStateFlow(false)
    val isSwipeMode: StateFlow<Boolean> = _isSwipeMode.asStateFlow()

    private var currentJob: Job? = null
    private var airingJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            val tracker = _selectedTracker.value
            val cached = cacheStore.loadTrackerCache(tracker)
            if (cached != null && cached.trendingAnime.isNotEmpty()) {
                _state.update {
                    TrackingState.Success(
                        trendingAnime = cached.trendingAnime.map { it.toAnimeTrackSearch(tracker.id) },
                        popularAnime = cached.popularAnime.map { it.toAnimeTrackSearch(tracker.id) },
                        trendingManga = cached.trendingManga.map { it.toMangaTrackSearch(tracker.id) },
                        popularManga = cached.popularManga.map { it.toMangaTrackSearch(tracker.id) },
                        trackerSource = tracker,
                        isCached = true,
                    )
                }
            } else {
                loadTracking()
            }

            val cachedAiring = cacheStore.loadAiringCache()
            if (!cachedAiring.isNullOrEmpty()) {
                _airingSchedule.value = cachedAiring
            } else {
                loadAiringSchedule(AiringDay.TODAY)
            }
        }
    }

    fun toggleSwipeMode() {
        _isSwipeMode.value = !_isSwipeMode.value
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

    fun selectAiringDay(day: AiringDay) {
        _selectedAiringDay.value = day
        loadAiringSchedule(day)
    }

    fun openAnimeDetails(anime: AnimeTrackSearch) {
        _selectedAnimeDetails.value = anime
    }

    fun openMangaDetails(manga: MangaTrackSearch) {
        _selectedMangaDetails.value = manga
    }

    fun closeDetails() {
        _selectedAnimeDetails.value = null
        _selectedMangaDetails.value = null
    }

    fun loadAiringSchedule(day: AiringDay = _selectedAiringDay.value, forceRefresh: Boolean = false) {
        airingJob?.cancel()
        airingJob = screenModelScope.launch {
            try {
                if (!forceRefresh) {
                    val cached = cacheStore.loadAiringCache()
                    if (!cached.isNullOrEmpty()) {
                        _airingSchedule.value = cached
                        return@launch
                    }
                }

                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                cal.add(Calendar.DAY_OF_YEAR, day.dayOffset)
                val startTime = cal.timeInMillis / 1000L

                val endTime = if (day == AiringDay.WEEK) {
                    startTime + (7 * 86400L)
                } else {
                    startTime + 86400L
                }

                val episodes = trackerManager.aniList.getAiringSchedule(startTime, endTime)
                _airingSchedule.value = episodes
                cacheStore.saveAiringCache(episodes)
            } catch (_: Exception) {
                _airingSchedule.value = emptyList()
            }
        }
    }

    fun loadTracking(query: String = _searchQuery.value, forceRefresh: Boolean = false) {
        currentJob?.cancel()
        currentJob = screenModelScope.launch {
            val tracker = _selectedTracker.value
            if (!forceRefresh && query.isBlank()) {
                val cached = cacheStore.loadTrackerCache(tracker)
                if (cached != null && (cached.trendingAnime.isNotEmpty() || cached.trendingManga.isNotEmpty())) {
                    _state.update {
                        TrackingState.Success(
                            trendingAnime = cached.trendingAnime.map { it.toAnimeTrackSearch(tracker.id) },
                            popularAnime = cached.popularAnime.map { it.toAnimeTrackSearch(tracker.id) },
                            trendingManga = cached.trendingManga.map { it.toMangaTrackSearch(tracker.id) },
                            popularManga = cached.popularManga.map { it.toMangaTrackSearch(tracker.id) },
                            trackerSource = tracker,
                            isCached = true,
                        )
                    }
                    return@launch
                }
            }

            _state.update { TrackingState.Loading }
            try {
                var trendingAnime: List<AnimeTrackSearch> = emptyList()
                var popularAnime: List<AnimeTrackSearch> = emptyList()
                var trendingManga: List<MangaTrackSearch> = emptyList()
                var popularManga: List<MangaTrackSearch> = emptyList()

                when (tracker) {
                    TrackerSource.ANILIST -> {
                        val anilist = trackerManager.aniList
                        if (query.isNotBlank()) {
                            trendingAnime = anilist.searchAnime(query)
                            trendingManga = anilist.searchManga(query)
                        } else {
                            trendingAnime = anilist.getTrendingAnime(1)
                            popularAnime = anilist.getTrendingAnime(2)
                            trendingManga = anilist.getTrendingManga(1)
                            popularManga = anilist.getTrendingManga(2)
                        }
                    }
                    TrackerSource.MYANIMELIST -> {
                        val mal = trackerManager.myAnimeList
                        if (query.isNotBlank()) {
                            trendingAnime = mal.searchAnime(query)
                            trendingManga = mal.searchManga(query)
                        } else {
                            trendingAnime = mal.getPopularAnime()
                            trendingManga = mal.getPopularManga()
                        }
                    }
                    TrackerSource.KITSU -> {
                        val kitsu = trackerManager.kitsu
                        val q = query.ifBlank { "a" }
                        trendingAnime = kitsu.searchAnime(q)
                        trendingManga = kitsu.searchManga(q)
                    }
                    TrackerSource.SHIKIMORI -> {
                        val shikimori = trackerManager.shikimori
                        val q = query.ifBlank { "" }
                        trendingAnime = shikimori.searchAnime(q)
                        trendingManga = shikimori.searchManga(q)
                    }
                    TrackerSource.BANGUMI -> {
                        val bangumi = trackerManager.bangumi
                        val q = query.ifBlank { "2024" }
                        trendingAnime = bangumi.searchAnime(q)
                        trendingManga = bangumi.searchManga(q)
                    }
                    TrackerSource.SIMKL -> {
                        val simkl = trackerManager.simkl
                        val q = query.ifBlank { "a" }
                        trendingAnime = simkl.searchAnime(q)
                    }
                    TrackerSource.MANGAUPDATES -> {
                        val mu = trackerManager.mangaUpdates
                        val q = query.ifBlank { "a" }
                        trendingManga = mu.searchManga(q)
                    }
                }

                if (query.isBlank()) {
                    cacheStore.saveTrackerCache(
                        TrackerCacheData(
                            trackerId = tracker.id,
                            trackerName = tracker.name,
                            timestamp = System.currentTimeMillis(),
                            trendingAnime = trendingAnime.map { it.toCached() },
                            popularAnime = popularAnime.map { it.toCached() },
                            trendingManga = trendingManga.map { it.toCached() },
                            popularManga = popularManga.map { it.toCached() },
                        )
                    )
                }

                _state.update {
                    TrackingState.Success(
                        trendingAnime = trendingAnime,
                        popularAnime = popularAnime,
                        trendingManga = trendingManga,
                        popularManga = popularManga,
                        trackerSource = tracker,
                        isCached = false,
                    )
                }
            } catch (e: Exception) {
                if (tracker != TrackerSource.ANILIST) {
                    try {
                        val fallbackAnime = trackerManager.aniList.getTrendingAnime(1)
                        val fallbackManga = trackerManager.aniList.getTrendingManga(1)
                        _state.update {
                            TrackingState.Success(
                                trendingAnime = fallbackAnime,
                                popularAnime = emptyList(),
                                trendingManga = fallbackManga,
                                popularManga = emptyList(),
                                trackerSource = TrackerSource.ANILIST,
                                isCached = false,
                            )
                        }
                        return@launch
                    } catch (_: Exception) {}
                }
                _state.update { TrackingState.Error(e.message ?: "Failed to load tracking data") }
            }
        }
    }

    fun forceRefreshAllTrackers() {
        screenModelScope.launch {
            _isRefreshing.value = true
            try {
                cacheStore.preloadAllTrackers(force = true)
                loadTracking(forceRefresh = true)
                loadAiringSchedule(forceRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun CachedAnimeEntry.toAnimeTrackSearch(trackerId: Long) = AnimeTrackSearch.create(trackerId).apply {
        remote_id = media_id
        this.title = this@toAnimeTrackSearch.title
        cover_url = this@toAnimeTrackSearch.cover_url
        score = this@toAnimeTrackSearch.score
        publishing_type = this@toAnimeTrackSearch.publishing_type
        publishing_status = this@toAnimeTrackSearch.publishing_status
        summary = this@toAnimeTrackSearch.summary
        total_episodes = this@toAnimeTrackSearch.total_episodes
        tracking_url = this@toAnimeTrackSearch.tracking_url
    }

    private fun CachedMangaEntry.toMangaTrackSearch(trackerId: Long) = MangaTrackSearch.create(trackerId).apply {
        remote_id = media_id
        this.title = this@toMangaTrackSearch.title
        cover_url = this@toMangaTrackSearch.cover_url
        score = this@toMangaTrackSearch.score
        publishing_type = this@toMangaTrackSearch.publishing_type
        publishing_status = this@toMangaTrackSearch.publishing_status
        summary = this@toMangaTrackSearch.summary
        total_chapters = this@toMangaTrackSearch.total_chapters
        tracking_url = this@toMangaTrackSearch.tracking_url
    }

    private fun AnimeTrackSearch.toCached() = CachedAnimeEntry(
        media_id = remote_id,
        title = title,
        cover_url = cover_url,
        score = score,
        publishing_type = publishing_type,
        publishing_status = publishing_status,
        summary = summary,
        total_episodes = total_episodes,
        tracking_url = tracking_url,
    )

    private fun MangaTrackSearch.toCached() = CachedMangaEntry(
        media_id = remote_id,
        title = title,
        cover_url = cover_url,
        score = score,
        publishing_type = publishing_type,
        publishing_status = publishing_status,
        summary = summary,
        total_chapters = total_chapters,
        tracking_url = tracking_url,
    )
}

sealed interface TrackingState {
    data object Loading : TrackingState
    data class Success(
        val trendingAnime: List<AnimeTrackSearch>,
        val popularAnime: List<AnimeTrackSearch> = emptyList(),
        val trendingManga: List<MangaTrackSearch>,
        val popularManga: List<MangaTrackSearch> = emptyList(),
        val trackerSource: TrackerSource = TrackerSource.ANILIST,
        val isCached: Boolean = false,
    ) : TrackingState
    data class Error(val message: String) : TrackingState
}
