package eu.kanade.tachiyomi.ui.discover

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.tachiyomi.data.discover.cache.CachedAnimeEntry
import eu.kanade.tachiyomi.data.discover.cache.CachedMangaEntry
import eu.kanade.tachiyomi.data.discover.cache.DiscoverCacheStore
import eu.kanade.tachiyomi.data.discover.cache.TrackerCacheData
import eu.kanade.tachiyomi.data.track.TrackerManager
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAnime
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.discover.components.AiringDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.TimeZone

/** Minimum delay between a keystroke and issuing a tracker search. */
private const val SEARCH_DEBOUNCE_MS = 350L

/** Airing schedule cache is only trusted for 3 hours before refreshing. */
private const val AIRING_CACHE_TTL_MS = 3 * 3600 * 1000L

/**
 * Enumeration of all supported tracking services and catalog providers for the Discover module.
 *
 * @property id The numeric tracker ID matching TrackerManager constants.
 * @property displayName Human-readable label for UI rendering.
 * @property supportsAnime Indicates whether this tracker can provide anime listings.
 * @property supportsManga Indicates whether this tracker can provide manga listings.
 */
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
    MANGAUPDATES(7L, "MangaUpdates", false, true),
    TRAKT(TrackerManager.TRAKT, "Trakt", true, false),
    SUWAYOMI(9L, "Suwayomi", true, true),
    KOMGA(6L, "Komga", false, true),
    KAVITA(TrackerManager.KAVITA, "Kavita", false, true),
    JELLYFIN(TrackerManager.JELLYFIN, "Jellyfin", true, false),
    TMDB(200L, "TMDB", true, false),
    HIKKA(10L, "Hikka", true, true),
    MANGABAKA(TrackerManager.MANGABAKA, "MangaBaka", false, true);

    fun isTrackerLoggedIn(trackerManager: TrackerManager): Boolean {
        return trackerManager.get(id)?.isLoggedIn ?: false
    }
}

/**
 * ScreenModel coordinating state and asynchronous operations for the Discover tab.
 *
 * Responsibilities:
 * - Manages discovery feed state [TrackingState] with automatic fallback to persistent local cache.
 * - Handles live search queries with coroutine debounce/cancellation of stale network requests.
 * - Fetches 7-day UTC airing schedules via AniList GraphQL API.
 * - Manages modal bottom sheets for anime/manga details preview.
 */
class TrackingScreenModel(
    private val trackerManager: TrackerManager = Injekt.get(),
    private val cacheStore: DiscoverCacheStore = Injekt.get(),
    private val getAnimeTracks: GetAnimeTracks = Injekt.get(),
    private val discoverPreferences: DiscoverPreferences = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _selectedTracker = MutableStateFlow(TrackerSource.ANILIST)
    val selectedTracker: StateFlow<TrackerSource> = _selectedTracker.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

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
    private var searchDebounceJob: Job? = null
    private var refreshJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            val tracker = _selectedTracker.value
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
            } else {
                loadTracking()
            }

            val cachedAiring = cacheStore.loadAiringCache(maxAgeMs = AIRING_CACHE_TTL_MS)
            if (!cachedAiring.isNullOrEmpty()) {
                _airingSchedule.value = cachedAiring
            } else {
                loadAiringSchedule(AiringDay.TODAY)
            }

            maybeAutoRefreshCaches()
        }
    }

    /**
     * Preloads tracker caches in the background when the last preload is older than the
     * user's configured [DiscoverPreferences.autoRefreshIntervalHours] interval.
     */
    private suspend fun maybeAutoRefreshCaches() {
        val intervalHours = discoverPreferences.autoRefreshIntervalHours().get()
        if (intervalHours <= 0) return
        val lastPreload = discoverPreferences.lastPreloadTimestamp().get()
        val now = System.currentTimeMillis()
        if (now - lastPreload < intervalHours * 3_600_000L) return

        discoverPreferences.lastPreloadTimestamp().set(now)
        try {
            cacheStore.preloadAllTrackers(force = false)
        } catch (_: Throwable) {
            // Background preload is best-effort; failures surface on explicit refresh
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

    fun updateSearchQuery(query: String?) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = screenModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadTracking(query.orEmpty())
        }
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

    fun loadAiringSchedule(day: AiringDay = _selectedAiringDay.value, forceRefresh: Boolean = false): Job {
        airingJob?.cancel()
        return screenModelScope.launch {
            refreshAiringSchedule(day, forceRefresh)
        }
    }

    private suspend fun refreshAiringSchedule(
        day: AiringDay = _selectedAiringDay.value,
        forceRefresh: Boolean = false,
    ) {
        try {
            if (!forceRefresh) {
                val cached = cacheStore.loadAiringCache(maxAgeMs = AIRING_CACHE_TTL_MS)
                if (!cached.isNullOrEmpty()) {
                    _airingSchedule.value = cached
                    return
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
        } catch (_: Throwable) {
            _airingSchedule.value = emptyList()
        }
    }

    fun loadTracking(query: String = _searchQuery.value.orEmpty(), forceRefresh: Boolean = false): Job {
        currentJob?.cancel()
        return screenModelScope.launch {
            refreshTracking(query, forceRefresh)
        }
    }

    private suspend fun refreshTracking(query: String, forceRefresh: Boolean) {
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
                return
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
                        coroutineScope {
                            val anime = async { anilist.searchAnime(query) }
                            val manga = async { anilist.searchManga(query) }
                            trendingAnime = anime.await()
                            trendingManga = manga.await()
                        }
                    } else {
                        coroutineScope {
                            val trendingAnimeJob = async { anilist.getTrendingAnime(1) }
                            val popularAnimeJob = async { anilist.getTrendingAnime(2) }
                            val trendingMangaJob = async { anilist.getTrendingManga(1) }
                            val popularMangaJob = async { anilist.getTrendingManga(2) }
                            trendingAnime = trendingAnimeJob.await()
                            popularAnime = popularAnimeJob.await()
                            trendingManga = trendingMangaJob.await()
                            popularManga = popularMangaJob.await()
                        }
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
                else -> {
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
        } catch (e: Throwable) {
            // Trackers may also throw fatal stub errors (TODO/NotImplementedError), so
            // best-effort loads must catch Throwable, not just Exception.
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
                    return
                } catch (_: Throwable) {}
            }
            _state.update { TrackingState.Error(e.message ?: "Failed to load tracking data") }
        }
    }

    /**
     * Force-refreshes every tracker cache plus the current feed and airing schedule.
     * [TrackingState.isRefreshing][isRefreshing] stays true until all loads finish.
     */
    fun forceRefreshAllTrackers() {
        refreshJob?.cancel()
        refreshJob = screenModelScope.launch {
            _isRefreshing.value = true
            try {
                cacheStore.preloadAllTrackers(force = true)
                coroutineScope {
                    launch { refreshTracking(query = _searchQuery.value.orEmpty(), forceRefresh = true) }
                    launch { refreshAiringSchedule(forceRefresh = true) }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── Deep media details (M4) ──────────────────────────────────────────────

    private val _deepDetailsState = MutableStateFlow<DeepDetailsState>(DeepDetailsState.Idle)
    val deepDetailsState: StateFlow<DeepDetailsState> = _deepDetailsState.asStateFlow()

    private var deepDetailsJob: Job? = null

    /**
     * Loads rich media details (characters, staff, relations, reviews) for a media
     * entry, preferring the persistent cache and falling back to the GraphQL API.
     */
    fun loadDeepDetails(mediaId: Long, isAnime: Boolean) {
        deepDetailsJob?.cancel()
        deepDetailsJob = screenModelScope.launch {
            _deepDetailsState.value = DeepDetailsState.Loading
            try {
                val cached = cacheStore.getDeepMediaDetails(mediaId)
                if (cached != null) {
                    _deepDetailsState.value = DeepDetailsState.Success(cached)
                    return@launch
                }

                val fresh = trackerManager.aniList.api.getDeepMediaDetails(mediaId, isAnime)
                    .getOrElse { e ->
                        _deepDetailsState.value = DeepDetailsState.Error(e.message ?: "Failed to load details")
                        return@launch
                    }
                cacheStore.saveDeepMediaDetails(mediaId, fresh, fresh.isReleasing)
                _deepDetailsState.value = DeepDetailsState.Success(fresh)
            } catch (_: Throwable) {
                _deepDetailsState.value = DeepDetailsState.Error("Failed to load details")
            }
        }
    }

    fun clearDeepDetails() {
        deepDetailsJob?.cancel()
        _deepDetailsState.value = DeepDetailsState.Idle
    }

    // ── Advanced filtering (M4) ──────────────────────────────────────────────

    private val _advancedFilter = MutableStateFlow<DailyALFilterState?>(null)
    val advancedFilter: StateFlow<DailyALFilterState?> = _advancedFilter.asStateFlow()

    private val _advancedFilterResults = MutableStateFlow<List<AnimeTrackSearch>>(emptyList())
    val advancedFilterResults: StateFlow<List<AnimeTrackSearch>> = _advancedFilterResults.asStateFlow()

    private val _isLoadingFilterResults = MutableStateFlow(false)
    val isLoadingFilterResults: StateFlow<Boolean> = _isLoadingFilterResults.asStateFlow()

    private var filterJob: Job? = null

    /**
     * Applies a [DailyALFilterState]: results come from the filter cache when fresh,
     * otherwise from the advanced-search GraphQL query, and are exposed as
     * [AnimeTrackSearch] items so the standard Discover result cards render them.
     */
    fun applyAdvancedFilter(filter: DailyALFilterState) {
        _advancedFilter.value = filter
        filterJob?.cancel()
        filterJob = screenModelScope.launch {
            _isLoadingFilterResults.value = true
            try {
                val cacheKey = filter.toCacheKey()
                val cached = cacheStore.getAdvancedFilterCache(cacheKey)
                val results: List<AnimeTrackSearch>
                if (cached != null) {
                    results = cached.map { it.toAnimeTrackSearch() }
                } else {
                    val fresh = trackerManager.aniList.api.searchAdvanced(filter)
                        .getOrElse {
                            _advancedFilterResults.value = emptyList()
                            return@launch
                        }
                    cacheStore.saveAdvancedFilterCache(cacheKey, fresh)
                    results = fresh.map { it.toAnimeTrackSearch() }
                }
                _advancedFilterResults.value = results
            } catch (_: Throwable) {
                _advancedFilterResults.value = emptyList()
            } finally {
                _isLoadingFilterResults.value = false
            }
        }
    }

    fun clearAdvancedFilter() {
        filterJob?.cancel()
        _advancedFilter.value = null
        _advancedFilterResults.value = emptyList()
    }

    // ── AniList profile & activity sync (M5) ─────────────────────────────────

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private var profileJob: Job? = null

    /**
     * Loads the logged-in AniList user's profile stats (streak-adjacent counts,
     * status and score distributions). No-ops into [ProfileState.NotLoggedIn]
     * when AniList isn't authenticated.
     */
    fun loadProfileStats() {
        profileJob?.cancel()
        profileJob = screenModelScope.launch {
            val aniList = trackerManager.aniList
            if (!aniList.isLoggedIn) {
                _profileState.value = ProfileState.NotLoggedIn
                return@launch
            }
            _profileState.value = ProfileState.Loading
            try {
                val stats = aniList.api.getUserProfileStats()
                    .getOrElse { e ->
                        _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
                        return@launch
                    }
                _profileState.value = ProfileState.Success(stats)
            } catch (_: Throwable) {
                _profileState.value = ProfileState.Error("Failed to load profile")
            }
        }
    }

    /**
     * Upserts watch progress (and optionally status and a 0-100 raw score) for a
     * media entry on AniList, then refreshes the profile so counts stay in sync.
     */
    fun updateMediaProgress(mediaId: Long, progress: Int, status: String? = null, score: Int? = null) {
        screenModelScope.launch {
            try {
                trackerManager.aniList.api.updateMediaProgress(mediaId, progress, status, score)
                    .onSuccess { loadProfileStats() }
            } catch (_: Throwable) {
                // Best-effort remote edit; profile refresh surfaces any persistent failure
            }
        }
    }

    // ── AniList user lists (M7) ──────────────────────────────────────────────

    private val _userListState = MutableStateFlow<UserListState>(UserListState.Idle)
    val userListState: StateFlow<UserListState> = _userListState.asStateFlow()

    /** In-memory per-type+status cache of the user's list entries. */
    private val userListCache = mutableMapOf<String, List<DailyALUserListEntry>>()

    private var userListJob: Job? = null

    /**
     * Loads the user's anime or manga list for a status (CURRENT / PLANNING /
     * COMPLETED / PAUSED / DROPPED), serving from the in-memory cache when available.
     */
    fun loadUserList(status: String, isManga: Boolean = false, forceRefresh: Boolean = false) {
        val cacheKey = userListCacheKey(status, isManga)
        userListJob?.cancel()
        userListJob = screenModelScope.launch {
            val aniList = trackerManager.aniList
            if (!aniList.isLoggedIn) {
                _userListState.value = UserListState.NotLoggedIn
                return@launch
            }
            if (!forceRefresh) {
                userListCache[cacheKey]?.let {
                    _userListState.value = UserListState.Success(status, it)
                    return@launch
                }
            }
            _userListState.value = UserListState.Loading
            try {
                val result = if (isManga) {
                    aniList.api.getUserMangaList(status)
                } else {
                    aniList.api.getUserAnimeList(status)
                }
                val entries = result
                    .getOrElse { e ->
                        _userListState.value = UserListState.Error(e.message ?: "Failed to load list")
                        return@launch
                    }
                userListCache[cacheKey] = entries
                _userListState.value = UserListState.Success(status, entries)
            } catch (_: Throwable) {
                _userListState.value = UserListState.Error("Failed to load list")
            }
        }
    }

    private fun userListCacheKey(status: String, isManga: Boolean) = "${if (isManga) "MANGA" else "ANIME"}_$status"

    // ── Airing schedule library filter ───────────────────────────────────────

    private val _airingTrackedIds = MutableStateFlow<Set<Long>>(emptySet())
    val airingTrackedIds: StateFlow<Set<Long>> = _airingTrackedIds.asStateFlow()

    private var airingTrackedIdsLoaded = false

    /**
     * One-shot load of the AniList remote ids of every tracked library anime,
     * used by the Airing schedule's "My library" filter.
     */
    fun loadAiringTrackedIds() {
        if (airingTrackedIdsLoaded) return
        airingTrackedIdsLoaded = true
        screenModelScope.launch {
            try {
                _airingTrackedIds.value = getAnimeTracks.await()
                    .filter { it.trackerId == TrackerSource.ANILIST.id }
                    .map { it.remoteId }
                    .toSet()
            } catch (_: Throwable) {
                // Filter stays empty — the schedule shows everything
            }
        }
    }

    /**
     * Optimistically applies a progress/score edit to the loaded list and pushes
     * the change to AniList; the profile stats refresh afterwards.
     */
    fun updateUserListEntry(entry: DailyALUserListEntry, newProgress: Int, newScoreRaw: Int) {
        // Optimistic local update
        val current = _userListState.value as? UserListState.Success ?: return
        val updated = current.entries.map {
            if (it.mediaId == entry.mediaId) {
                it.copy(
                    progress = newProgress,
                    scoreRaw = newScoreRaw,
                    status = if (
                        entry.totalEpisodes > 0 &&
                        newProgress >= entry.totalEpisodes &&
                        entry.status == "CURRENT"
                    ) "COMPLETED" else it.status,
                )
            } else {
                it
            }
        }
        userListCache[userListCacheKey(current.status, entry.isManga)] = updated
        _userListState.value = UserListState.Success(current.status, updated)

        val remoteStatus = if (
            entry.totalEpisodes > 0 &&
            newProgress >= entry.totalEpisodes &&
            entry.status == "CURRENT"
        ) {
            "COMPLETED"
        } else {
            null
        }
        updateMediaProgress(entry.mediaId, newProgress, remoteStatus, newScoreRaw.takeIf { it > 0 })
    }

    private val _activityState = MutableStateFlow<ActivityState>(ActivityState.Idle)
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    // ── Tracked continue rail (Discover home) ────────────────────────────────

    private val _trackedRailState = MutableStateFlow<TrackedRailState>(TrackedRailState.Idle)
    val trackedRailState: StateFlow<TrackedRailState> = _trackedRailState.asStateFlow()

    private var trackedRailLoaded = false

    /**
     * Builds "continue from your AniList" rail items from the user's CURRENT
     * list, matching each entry against library tracks (AniList remote_id →
     * library anime) so in-library entries resume directly.
     */
    fun loadTrackedRail(forceRefresh: Boolean = false) {
        if (trackedRailLoaded && !forceRefresh) return
        trackedRailLoaded = true
        screenModelScope.launch {
            val aniList = trackerManager.aniList
            if (!aniList.isLoggedIn) {
                _trackedRailState.value = TrackedRailState.Idle
                return@launch
            }
            _trackedRailState.value = TrackedRailState.Loading
            try {
                val entries = aniList.api.getUserAnimeList("CURRENT")
                    .getOrElse {
                        _trackedRailState.value = TrackedRailState.Idle
                        return@launch
                    }

                val anilistRemoteIds = entries.map { it.mediaId }.toSet()
                val libraryIdsByRemoteId = getAnimeTracks.await()
                    .filter { it.trackerId == TrackerSource.ANILIST.id && it.remoteId in anilistRemoteIds }
                    .associateBy { it.remoteId }
                    .mapValues { it.value.animeId }

                val items = entries.map { entry ->
                    TrackedRailItem(
                        mediaId = entry.mediaId,
                        title = entry.title,
                        coverUrl = entry.coverUrl,
                        progress = entry.progress,
                        total = entry.totalEpisodes,
                        libraryAnimeId = libraryIdsByRemoteId[entry.mediaId],
                    )
                }
                _trackedRailState.value = TrackedRailState.Success(items)
            } catch (_: Throwable) {
                _trackedRailState.value = TrackedRailState.Idle
            }
        }
    }

    private var activityLoaded = false

    /**
     * Loads the user's recent AniList activity feed once per session; later
     * calls are no-ops unless forced.
     */
    fun loadActivityFeed(forceRefresh: Boolean = false) {
        if (activityLoaded && !forceRefresh) return
        activityLoaded = true
        screenModelScope.launch {
            val aniList = trackerManager.aniList
            if (!aniList.isLoggedIn) {
                _activityState.value = ActivityState.NotLoggedIn
                return@launch
            }
            _activityState.value = ActivityState.Loading
            try {
                val entries = aniList.api.getUserActivityFeed()
                    .getOrElse { e ->
                        _activityState.value = ActivityState.Error(e.message ?: "Failed to load activity")
                        return@launch
                    }
                _activityState.value = ActivityState.Success(entries)
            } catch (_: Throwable) {
                _activityState.value = ActivityState.Error("Failed to load activity")
            }
        }
    }

    private fun ALAnime.toAnimeTrackSearch(): AnimeTrackSearch = AnimeTrackSearch.create(
        TrackerSource.ANILIST.id,
    ).apply {
        remote_id = remoteId
        title = this@toAnimeTrackSearch.title
        cover_url = imageUrl
        summary = description.orEmpty()
        publishing_type = format
        publishing_status = publishingStatus
        total_episodes = totalEpisodes
        score = if (averageScore > 0) averageScore.toDouble() else -1.0
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

/** Rich per-media details lifecycle for the deep details bottom sheet. */
sealed interface DeepDetailsState {
    data object Idle : DeepDetailsState
    data object Loading : DeepDetailsState
    data class Success(val details: DailyALMediaDetails) : DeepDetailsState
    data class Error(val message: String) : DeepDetailsState
}

/** AniList profile stats lifecycle for the Discover profile section. */
sealed interface ProfileState {
    data object Idle : ProfileState
    data object Loading : ProfileState
    data object NotLoggedIn : ProfileState
    data class Success(val stats: DailyALProfileStats) : ProfileState
    data class Error(val message: String) : ProfileState
}

/** Lifecycle for the user's AniList list browser inside the profile section. */
sealed interface UserListState {
    data object Idle : UserListState
    data object Loading : UserListState
    data object NotLoggedIn : UserListState
    data class Error(val message: String) : UserListState
    data class Success(
        val status: String,
        val entries: List<DailyALUserListEntry>,
    ) : UserListState
}

/** Lifecycle for the AniList activity feed inside the profile section. */
sealed interface ActivityState {
    data object Idle : ActivityState
    data object Loading : ActivityState
    data object NotLoggedIn : ActivityState
    data class Error(val message: String) : ActivityState
    data class Success(val entries: List<DailyALActivityEntry>) : ActivityState
}

/** One "continue from your AniList" rail item on the Discover home feed. */
data class TrackedRailItem(
    val mediaId: Long,
    val title: String,
    val coverUrl: String?,
    val progress: Int,
    val total: Int,
    val libraryAnimeId: Long?,
)

/** Lifecycle for the tracked-continue rail on the Discover home feed. */
sealed interface TrackedRailState {
    data object Idle : TrackedRailState
    data object Loading : TrackedRailState
    data class Success(val items: List<TrackedRailItem>) : TrackedRailState
}
