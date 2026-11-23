package eu.kanade.tachiyomi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.home.HomeItemData
import eu.kanade.presentation.home.MediaType
import eu.kanade.tachiyomi.data.discover.cache.DiscoverCacheStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.history.anime.interactor.GetAnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.manga.interactor.GetMangaHistory
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.entries.manga.model.asMangaCover
import tachiyomi.source.local.entries.anime.isLocal
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

enum class HomeMediaFilter {
    ALL,
    VIDEO_ONLY,
    MANGA_ONLY,
}

enum class HeroSource {
    BOTH,
    LIBRARY_ONLY,
    TRACKERS_ONLY,
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HomeFeedScreenModel(
    private val getAnimeHistory: GetAnimeHistory,
    private val getMangaHistory: GetMangaHistory,
    private val getLibraryAnime: GetLibraryAnime,
    private val getLibraryManga: GetLibraryManga,
    private val sourcePreferences: SourcePreferences,
    private val trackerManager: TrackerManager,
    private val uiPreferences: UiPreferences,
    private val sourceManager: AnimeSourceManager,
    private val trackPreferences: TrackPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // Resolved via Injekt directly: this ViewModel's @Inject constructor is Metro-managed
    private val discoverCacheStore: DiscoverCacheStore = Injekt.get()

    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val heroList: List<HomeItemData> = emptyList(),
        val continueList: List<HomeItemData> = emptyList(),
        val airingToday: List<AiringEpisode> = emptyList(),
        val becauseYouWatchedTitle: String? = null,
        val becauseYouWatchedIsAnime: Boolean = true,
        val becauseYouWatchedList: List<HomeItemData> = emptyList(),
        val recommendedList: List<HomeItemData> = emptyList(),
        val animeList: List<HomeItemData> = emptyList(),
        val mangaList: List<HomeItemData> = emptyList(),
        val movieList: List<HomeItemData> = emptyList(),
        val seriesList: List<HomeItemData> = emptyList(),
        val showFeatured: Boolean = true,
        val showContinue: Boolean = true,
        val showAiringToday: Boolean = true,
        val showBecauseYouWatched: Boolean = true,
        val showRecommended: Boolean = true,
        val showPopularAnime: Boolean = true,
        val showPopularManga: Boolean = true,
        val showPopularMovies: Boolean = true,
        val showPopularSeries: Boolean = true,
        val mediaFilter: HomeMediaFilter = HomeMediaFilter.ALL,
        val autoScrollHero: Boolean = true,
        val heroSource: HeroSource = HeroSource.BOTH,
        val itemsPerSection: Int = 12,
        val hideCompletedInRecommended: Boolean = false,
        val enableTmdb: Boolean = true,
        val enableAnilist: Boolean = true,
    )

    // In-memory caches updated by individual observers
    private var cachedLibraryAnime: List<LibraryAnime> = emptyList()
    private var cachedLibraryManga: List<LibraryManga> = emptyList()
    private var cachedAnimeHistory: List<AnimeHistoryWithRelations> = emptyList()
    private var cachedMangaHistory: List<MangaHistoryWithRelations> = emptyList()
    private var cachedRemoteAnime: List<HomeItemData> = emptyList()
    private var cachedRemoteManga: List<HomeItemData> = emptyList()
    private var cachedRemoteMovieSeries: List<HomeItemData> = emptyList()

    private val remoteAnimeState = MutableStateFlow<List<HomeItemData>>(emptyList())
    private val remoteMangaState = MutableStateFlow<List<HomeItemData>>(emptyList())
    private val remoteMovieSeriesState = MutableStateFlow<List<HomeItemData>>(emptyList())

    init {
        observeHomeData()
        fetchRemoteTrendsAsync()
        loadAiringToday()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { current ->
                val newShuffledRecs = current.recommendedList.shuffled()
                current.copy(
                    isRefreshing = true,
                    recommendedList = newShuffledRecs,
                )
            }
            fetchRemoteTrendsAsync()
            loadAiringToday()
            delay(600L)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Loads today's airing episodes from the Discover airing cache (refreshed by the
     * Discover tab). Cache-only: the rail hides itself when the cache is empty or stale.
     */
    private fun loadAiringToday() {
        viewModelScope.launch(Dispatchers.IO) {
            val airing = runCatching {
                discoverCacheStore.loadAiringCache(maxAgeMs = 3 * 3600 * 1000L)
            }.getOrNull().orEmpty()
            _state.update { it.copy(airingToday = airing.take(15)) }
        }
    }

    fun toggleSection(key: String) {
        when (key) {
            "featured" -> uiPreferences.homeShowFeatured.set(!uiPreferences.homeShowFeatured.get())

            "continue" -> uiPreferences.homeShowContinue.set(!uiPreferences.homeShowContinue.get())

            "airing_today" -> uiPreferences.homeShowAiringToday.set(!uiPreferences.homeShowAiringToday.get())

            "because_you_watched" -> uiPreferences.homeShowBecauseYouWatched.set(
                !uiPreferences.homeShowBecauseYouWatched.get(),
            )

            "recommended" -> uiPreferences.homeShowRecommended.set(!uiPreferences.homeShowRecommended.get())

            "popular_anime" -> uiPreferences.homeShowPopularAnime.set(!uiPreferences.homeShowPopularAnime.get())

            "popular_manga" -> uiPreferences.homeShowPopularManga.set(!uiPreferences.homeShowPopularManga.get())

            "popular_movies" -> uiPreferences.homeShowPopularMovies.set(!uiPreferences.homeShowPopularMovies.get())

            "popular_series" -> uiPreferences.homeShowPopularSeries.set(!uiPreferences.homeShowPopularSeries.get())
        }
        updateStateWithData()
    }

    fun setMediaFilter(filter: HomeMediaFilter) {
        uiPreferences.homeMediaFilter.set(filter.ordinal)
        updateStateWithData()
    }

    fun toggleAutoScrollHero() {
        uiPreferences.homeAutoScrollHero.set(!uiPreferences.homeAutoScrollHero.get())
        updateStateWithData()
    }

    fun setHeroSource(source: HeroSource) {
        uiPreferences.homeHeroSource.set(source.ordinal)
        updateStateWithData()
        fetchRemoteTrendsAsync()
    }

    fun setItemsPerSection(count: Int) {
        uiPreferences.homeItemsPerSection.set(count)
        updateStateWithData()
    }

    fun toggleHideCompletedInRecommended() {
        uiPreferences.homeHideCompleted.set(!uiPreferences.homeHideCompleted.get())
        updateStateWithData()
    }

    fun toggleEnableTmdb() {
        uiPreferences.homeEnableTmdb.set(!uiPreferences.homeEnableTmdb.get())
        updateStateWithData()
        fetchRemoteTrendsAsync()
    }

    fun toggleEnableAnilist() {
        uiPreferences.homeEnableAnilist.set(!uiPreferences.homeEnableAnilist.get())
        updateStateWithData()
        fetchRemoteTrendsAsync()
    }

    private fun fetchRemoteTrendsAsync() {
        // 1. Tendencias de AniList (Anime)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!uiPreferences.homeEnableAnilist.get()) {
                    remoteAnimeState.value = emptyList()
                    return@launch
                }
                logcat(LogPriority.DEBUG) { "Fetching AniList popular anime..." }
                val popularAnime = trackerManager.aniList.getPopularAnime()
                logcat(LogPriority.DEBUG) { "AniList popular anime returned ${popularAnime.size} items" }
                val animeItems = popularAnime.map { track ->
                    val classified = classifyMedia(
                        title = track.title,
                        description = track.summary,
                    )
                    HomeItemData(
                        id = track.remote_id,
                        isAnime = true,
                        inLibrary = false,
                        title = track.title,
                        subtitle = classified.name,
                        coverUrl = track.cover_url,
                        coverData = track.cover_url,
                        mediaType = classified,
                        rating = if (track.score > 0) String.format("%.1f", track.score / 10.0) else "",
                        synopsis = track.summary,
                    )
                }
                remoteAnimeState.value = animeItems
                logcat(LogPriority.DEBUG) { "remoteAnimeState updated with ${animeItems.size} items" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch AniList popular anime" }
            }
        }

        // 2. Tendencias de AniList (Manga)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!uiPreferences.homeEnableAnilist.get()) {
                    remoteMangaState.value = emptyList()
                    return@launch
                }
                logcat(LogPriority.DEBUG) { "Fetching AniList popular manga..." }
                val popularManga = trackerManager.aniList.getPopularManga()
                logcat(LogPriority.DEBUG) { "AniList popular manga returned ${popularManga.size} items" }
                val mangaItems = popularManga.map { track ->
                    HomeItemData(
                        id = track.remote_id,
                        isAnime = false,
                        inLibrary = false,
                        title = track.title,
                        subtitle = "Manga",
                        coverUrl = track.cover_url,
                        coverData = track.cover_url,
                        mediaType = MediaType.MANGA,
                        rating = if (track.score > 0) String.format("%.1f", track.score / 10.0) else "",
                        synopsis = track.summary,
                    )
                }
                remoteMangaState.value = mangaItems
                logcat(LogPriority.DEBUG) { "remoteMangaState updated with ${mangaItems.size} items" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch AniList popular manga" }
            }
        }

        // 3. Tendencias de TMDB (Películas y Series)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!uiPreferences.homeEnableTmdb.get()) {
                    remoteMovieSeriesState.value = emptyList()
                    return@launch
                }
                val tmdbAvailable = trackerManager.tmdb.isAvailableForUse()
                val tmdbLoggedIn = trackerManager.tmdb.isLoggedIn
                logcat(LogPriority.DEBUG) {
                    "TMDB check: isAvailableForUse=$tmdbAvailable, isLoggedIn=$tmdbLoggedIn"
                }
                if (tmdbLoggedIn || tmdbAvailable) {
                    val movies = try {
                        trackerManager.tmdb.getTrendingMovies().also {
                            logcat(LogPriority.DEBUG) { "TMDB getTrendingMovies returned ${it.size} items" }
                        }.map { track ->
                            HomeItemData(
                                id = track.remote_id,
                                isAnime = true,
                                inLibrary = false,
                                title = track.title,
                                subtitle = "Película",
                                coverUrl = track.cover_url,
                                coverData = track.cover_url,
                                mediaType = MediaType.MOVIES,
                                rating = if (track.score > 0) String.format("%.1f", track.score) else "",
                                synopsis = track.summary,
                                genres = "Película, Cine, Movie",
                            )
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, e) { "Failed to fetch TMDB movies" }
                        emptyList()
                    }

                    val series = try {
                        trackerManager.tmdb.getTrendingTv().also {
                            logcat(LogPriority.DEBUG) { "TMDB getTrendingTv returned ${it.size} items" }
                        }.map { track ->
                            HomeItemData(
                                id = track.remote_id,
                                isAnime = true,
                                inLibrary = false,
                                title = track.title,
                                subtitle = "Serie",
                                coverUrl = track.cover_url,
                                coverData = track.cover_url,
                                mediaType = MediaType.SERIES,
                                rating = if (track.score > 0) String.format("%.1f", track.score) else "",
                                synopsis = track.summary,
                                genres = "Serie, TV, Show",
                            )
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, e) { "Failed to fetch TMDB series" }
                        emptyList()
                    }

                    remoteMovieSeriesState.value = movies + series
                    logcat(LogPriority.DEBUG) {
                        "remoteMovieSeriesState updated with ${movies.size + series.size} items"
                    }
                } else {
                    logcat(LogPriority.WARN) { "TMDB skipped: not logged in and not available for use" }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Critical error in TMDB fetch coroutine" }
            }
        }
    }

    private fun observeHomeData() {
        logcat(LogPriority.DEBUG) { "observeHomeData: initializing observers..." }
        // Set loading false immediately — UI is always interactive
        _state.update { it.copy(isLoading = false) }

        // Observe uiPreferences changes so UI state updates instantly in real time
        viewModelScope.launch {
            try {
                merge(
                    uiPreferences.homeShowFeatured.changes(),
                    uiPreferences.homeShowContinue.changes(),
                    uiPreferences.homeShowAiringToday.changes(),
                    uiPreferences.homeShowBecauseYouWatched.changes(),
                    uiPreferences.homeShowRecommended.changes(),
                    uiPreferences.homeShowPopularAnime.changes(),
                    uiPreferences.homeShowPopularManga.changes(),
                    uiPreferences.homeShowPopularMovies.changes(),
                    uiPreferences.homeShowPopularSeries.changes(),
                    uiPreferences.homeMediaFilter.changes(),
                    uiPreferences.homeAutoScrollHero.changes(),
                    uiPreferences.homeHeroSource.changes(),
                    uiPreferences.homeItemsPerSection.changes(),
                    uiPreferences.homeHideCompleted.changes(),
                    uiPreferences.homeEnableTmdb.changes(),
                    uiPreferences.homeEnableAnilist.changes(),
                )
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing uiPreferences" } }
                    .collect {
                        logcat(LogPriority.DEBUG) { "uiPreferences changed, updating state" }
                        updateStateWithData()
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error in uiPreferences observer" }
            }
        }

        // Observe TMDB API key changes to auto-refetch trends when key is configured
        viewModelScope.launch {
            try {
                trackPreferences.trackApiKey(trackerManager.tmdb as eu.kanade.tachiyomi.data.track.Tracker).changes()
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing TMDB API key" } }
                    .collect {
                        logcat(LogPriority.DEBUG) { "TMDB key changed, refetching trends" }
                        fetchRemoteTrendsAsync()
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error observing TMDB API key" }
            }
        }

        // Observe library anime
        viewModelScope.launch {
            try {
                getLibraryAnime.subscribe()
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing library anime" } }
                    .collect { libraryAnimeList ->
                        logcat(LogPriority.DEBUG) { "Library anime collected: size=${libraryAnimeList.size}" }
                        updateStateWithData(libraryAnime = libraryAnimeList)
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error in library anime observer" }
            }
        }

        // Observe library manga
        viewModelScope.launch {
            try {
                getLibraryManga.subscribe()
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing library manga" } }
                    .collect { libraryMangaList ->
                        logcat(LogPriority.DEBUG) { "Library manga collected: size=${libraryMangaList.size}" }
                        updateStateWithData(libraryManga = libraryMangaList)
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error in library manga observer" }
            }
        }

        // Observe anime history
        viewModelScope.launch {
            try {
                getAnimeHistory.subscribe("")
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing anime history" } }
                    .collect { histories ->
                        logcat(LogPriority.DEBUG) { "Anime history collected: size=${histories.size}" }
                        updateStateWithData(animeHistory = histories)
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error in anime history observer" }
            }
        }

        // Observe manga history
        viewModelScope.launch {
            try {
                getMangaHistory.subscribe("")
                    .catch { e -> logcat(LogPriority.ERROR, e) { "Error observing manga history" } }
                    .collect { histories ->
                        logcat(LogPriority.DEBUG) { "Manga history collected: size=${histories.size}" }
                        updateStateWithData(mangaHistory = histories)
                    }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Fatal error in manga history observer" }
            }
        }

        // Observe remote anime
        viewModelScope.launch {
            try {
                remoteAnimeState.collect { items ->
                    logcat(LogPriority.DEBUG) { "remoteAnimeState collected: size=${items.size}" }
                    updateStateWithData(remoteAnime = items)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error observing remote anime" }
            }
        }

        // Observe remote manga
        viewModelScope.launch {
            try {
                remoteMangaState.collect { items ->
                    logcat(LogPriority.DEBUG) { "remoteMangaState collected: size=${items.size}" }
                    updateStateWithData(remoteManga = items)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error observing remote manga" }
            }
        }

        // Observe remote movies and series
        viewModelScope.launch {
            try {
                remoteMovieSeriesState.collect { items ->
                    logcat(LogPriority.DEBUG) { "remoteMovieSeriesState collected: size=${items.size}" }
                    updateStateWithData(remoteMovieSeries = items)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error observing remote movie series" }
            }
        }
    }

    @Synchronized
    private fun updateStateWithData(
        libraryAnime: List<LibraryAnime>? = null,
        libraryManga: List<LibraryManga>? = null,
        animeHistory: List<AnimeHistoryWithRelations>? = null,
        mangaHistory: List<MangaHistoryWithRelations>? = null,
        remoteAnime: List<HomeItemData>? = null,
        remoteManga: List<HomeItemData>? = null,
        remoteMovieSeries: List<HomeItemData>? = null,
    ) {
        // Update caches with new data
        if (libraryAnime != null) cachedLibraryAnime = libraryAnime
        if (libraryManga != null) cachedLibraryManga = libraryManga
        if (animeHistory != null) cachedAnimeHistory = animeHistory
        if (mangaHistory != null) cachedMangaHistory = mangaHistory
        if (remoteAnime != null) cachedRemoteAnime = remoteAnime
        if (remoteManga != null) cachedRemoteManga = remoteManga
        if (remoteMovieSeries != null) cachedRemoteMovieSeries = remoteMovieSeries

        try {
            val libraryAnimeList = cachedLibraryAnime
            val libraryMangaList = cachedLibraryManga
            val animeHistories = cachedAnimeHistory
            val mangaHistories = cachedMangaHistory
            val rawRemoteAnime = cachedRemoteAnime
            val rawRemoteManga = cachedRemoteManga
            val movieSeriesList = cachedRemoteMovieSeries

            // Read preferences synchronously
            val filter = HomeMediaFilter.entries.getOrElse(uiPreferences.homeMediaFilter.get()) { HomeMediaFilter.ALL }
            val heroSource = HeroSource.entries.getOrElse(uiPreferences.homeHeroSource.get()) { HeroSource.BOTH }
            val limit = uiPreferences.homeItemsPerSection.get()
            val hideCompleted = uiPreferences.homeHideCompleted.get()
            val autoScrollHero = uiPreferences.homeAutoScrollHero.get()

            val showFeatured = uiPreferences.homeShowFeatured.get()
            val showContinue = uiPreferences.homeShowContinue.get()
            val showAiringToday = uiPreferences.homeShowAiringToday.get()
            val showBecauseYouWatched = uiPreferences.homeShowBecauseYouWatched.get()
            val showRecommended = uiPreferences.homeShowRecommended.get()
            val showPopularAnime = uiPreferences.homeShowPopularAnime.get()
            val showPopularManga = uiPreferences.homeShowPopularManga.get()
            val showPopularMovies = uiPreferences.homeShowPopularMovies.get()
            val showPopularSeries = uiPreferences.homeShowPopularSeries.get()

            val enableTmdb = uiPreferences.homeEnableTmdb.get()
            val enableAnilist = uiPreferences.homeEnableAnilist.get()

            val pinnedAnimeSources = sourcePreferences.pinnedAnimeSources.get()
            val pinnedMangaSources = sourcePreferences.pinnedMangaSources.get()

            val animeMap = libraryAnimeList.associateBy { it.anime.id }

            logcat(LogPriority.DEBUG) {
                "HomeFeed update: remoteAnime=${rawRemoteAnime.size}, remoteManga=${rawRemoteManga.size}, " +
                    "remoteMovies=${movieSeriesList.size}, animeHistory=${animeHistories.size}, " +
                    "mangaHistory=${mangaHistories.size}, libraryAnime=${libraryAnimeList.size}, " +
                    "libraryManga=${libraryMangaList.size}"
            }

            // 1. Continue Watching (Anime)
            val continueAnime = if (filter != HomeMediaFilter.MANGA_ONLY) {
                animeHistories.map { relation ->
                    val epNum = if (relation.episodeNumber % 1.0 == 0.0) {
                        relation.episodeNumber.toInt().toString()
                    } else {
                        relation.episodeNumber.toString()
                    }

                    val libAnime = animeMap[relation.animeId]?.anime
                    val realSourceName = libAnime?.source?.let { sourceManager.getOrStub(it).name } ?: ""

                    val classifiedType = classifyMedia(
                        title = relation.title,
                        genre = libAnime?.genre,
                        description = libAnime?.description,
                        sourceName = realSourceName,
                    )

                    val subtitleText = when (classifiedType) {
                        MediaType.MOVIES -> "Película"
                        MediaType.SERIES -> "Serie • Ep. $epNum"
                        else -> "Episodio $epNum"
                    }

                    HomeItemData(
                        id = relation.animeId,
                        isAnime = true,
                        inLibrary = true,
                        episodeId = relation.episodeId,
                        title = relation.title,
                        subtitle = subtitleText,
                        coverData = relation.coverData,
                        mediaType = classifiedType,
                        progress = 0.5f,
                        remainingInfo = "Ep. $epNum",
                        synopsis = libAnime?.description ?: relation.title,
                        genres = libAnime?.genre?.joinToString(", ") ?: "",
                        lastUpdatedTimestamp = relation.seenAt?.time ?: 0L,
                    )
                }
            } else {
                emptyList()
            }

            // 2. Continue Reading (Manga)
            val continueManga = if (filter != HomeMediaFilter.VIDEO_ONLY) {
                mangaHistories.map { relation ->
                    val chNum = if (relation.chapterNumber % 1.0 == 0.0) {
                        relation.chapterNumber.toInt().toString()
                    } else {
                        relation.chapterNumber.toString()
                    }

                    val libManga = libraryMangaList.find { it.id == relation.mangaId }?.manga
                    val genreStr = libManga?.genre?.joinToString(", ") ?: ""
                    val clean = "${relation.title} $genreStr".lowercase()
                    val detectedType = when {
                        clean.contains("manhwa") -> MediaType.MANHWA
                        clean.contains("manhua") -> MediaType.MANHUA
                        clean.contains("webtoon") -> MediaType.WEBTOON
                        clean.contains("light novel") || clean.contains("novel") -> MediaType.NOVEL
                        else -> MediaType.MANGA
                    }

                    HomeItemData(
                        id = relation.mangaId,
                        isAnime = false,
                        inLibrary = true,
                        chapterId = relation.chapterId,
                        title = relation.title,
                        subtitle = "Capítulo $chNum",
                        coverData = relation.coverData,
                        mediaType = detectedType,
                        progress = 0.8f,
                        remainingInfo = "Cap. $chNum",
                        synopsis = relation.title,
                        genres = genreStr,
                        lastUpdatedTimestamp = relation.readAt?.time ?: 0L,
                    )
                }
            } else {
                emptyList()
            }

            val unifiedContinue = (continueAnime + continueManga)
                .sortedByDescending { it.lastUpdatedTimestamp }
                .take(20)

            // 3. Library lists
            val nonLocalAnime = if (filter != HomeMediaFilter.MANGA_ONLY) {
                libraryAnimeList.filterNot { it.anime.isLocal() }
            } else {
                emptyList()
            }
            val nonLocalManga = if (filter != HomeMediaFilter.VIDEO_ONLY) {
                libraryMangaList.filterNot { it.manga.isLocal() }
            } else {
                emptyList()
            }

            val pinnedAnimeList = nonLocalAnime.filter { "${it.anime.source}" in pinnedAnimeSources }
            val sourceAnimeList = if (pinnedAnimeList.isNotEmpty()) pinnedAnimeList else nonLocalAnime

            val pinnedMangaList = nonLocalManga.filter { "${it.manga.source}" in pinnedMangaSources }
            val sourceMangaList = if (pinnedMangaList.isNotEmpty()) pinnedMangaList else nonLocalManga

            val animeItems = sourceAnimeList.map { lib ->
                val anime = lib.anime
                val realSourceName = sourceManager.getOrStub(anime.source).name

                val mediaType = classifyMedia(
                    title = anime.title,
                    genre = anime.genre,
                    description = anime.description,
                    sourceName = realSourceName,
                )

                HomeItemData(
                    id = anime.id,
                    isAnime = true,
                    inLibrary = true,
                    title = anime.title,
                    subtitle = anime.genre?.firstOrNull() ?: mediaType.name,
                    coverData = anime.asAnimeCover(),
                    mediaType = mediaType,
                    rating = "",
                    synopsis = anime.description ?: anime.title,
                    genres = anime.genre?.joinToString(", ") ?: "",
                )
            }

            val mangaItems = sourceMangaList.map { lib ->
                val manga = lib.manga
                val genreStr = manga.genre?.joinToString(", ") ?: ""
                val clean = "${manga.title} $genreStr".lowercase()
                val detectedType = when {
                    clean.contains("manhwa") -> MediaType.MANHWA
                    clean.contains("manhua") -> MediaType.MANHUA
                    clean.contains("webtoon") -> MediaType.WEBTOON
                    clean.contains("light novel") || clean.contains("novel") -> MediaType.NOVEL
                    else -> MediaType.MANGA
                }
                HomeItemData(
                    id = manga.id,
                    isAnime = false,
                    inLibrary = true,
                    title = manga.title,
                    subtitle = manga.genre?.firstOrNull() ?: detectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                    coverData = manga.asMangaCover(),
                    mediaType = detectedType,
                    rating = "",
                    synopsis = manga.description ?: manga.title,
                    genres = genreStr,
                )
            }

            // Cross-reference remote items with local library
            val animeTitleMap = libraryAnimeList.associateBy { it.anime.title.lowercase().trim() }
            val processedRemoteAnime = rawRemoteAnime.map { item ->
                val match = animeTitleMap[item.title.lowercase().trim()]
                if (match != null) {
                    item.copy(
                        id = match.anime.id,
                        inLibrary = true,
                        coverData = match.anime.asAnimeCover(),
                        genres = match.anime.genre?.joinToString(", ") ?: item.genres,
                    )
                } else {
                    item
                }
            }

            val mangaTitleMap = libraryMangaList.associateBy { it.manga.title.lowercase().trim() }
            val processedRemoteManga = rawRemoteManga.map { item ->
                val match = mangaTitleMap[item.title.lowercase().trim()]
                if (match != null) {
                    item.copy(
                        id = match.manga.id,
                        inLibrary = true,
                        coverData = match.manga.asMangaCover(),
                        genres = match.manga.genre?.joinToString(", ") ?: item.genres,
                    )
                } else {
                    item
                }
            }

            val finalPopularAnime = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> animeItems.filter { it.mediaType == MediaType.ANIME }

                HeroSource.TRACKERS_ONLY -> processedRemoteAnime

                HeroSource.BOTH -> processedRemoteAnime.ifEmpty {
                    animeItems.filter { it.mediaType == MediaType.ANIME }
                }
            }

            val finalPopularManga = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> mangaItems
                HeroSource.TRACKERS_ONLY -> processedRemoteManga
                HeroSource.BOTH -> processedRemoteManga.ifEmpty { mangaItems }
            }

            val movieItems = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> animeItems.filter { it.mediaType == MediaType.MOVIES }

                HeroSource.TRACKERS_ONLY -> movieSeriesList.filter { it.mediaType == MediaType.MOVIES }

                HeroSource.BOTH -> movieSeriesList.filter { it.mediaType == MediaType.MOVIES }.ifEmpty {
                    animeItems.filter {
                        it.mediaType ==
                            MediaType.MOVIES
                    }
                }
            }

            val seriesItems = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> animeItems.filter { it.mediaType == MediaType.SERIES }

                HeroSource.TRACKERS_ONLY -> movieSeriesList.filter { it.mediaType == MediaType.SERIES }

                HeroSource.BOTH -> movieSeriesList.filter { it.mediaType == MediaType.SERIES }.ifEmpty {
                    animeItems.filter {
                        it.mediaType ==
                            MediaType.SERIES
                    }
                }
            }

            val displayPopularAnime = if (filter != HomeMediaFilter.MANGA_ONLY) finalPopularAnime else emptyList()
            val displayPopularManga = if (filter != HomeMediaFilter.VIDEO_ONLY) finalPopularManga else emptyList()
            val displayPopularMovies = if (filter != HomeMediaFilter.MANGA_ONLY) movieItems else emptyList()
            val displayPopularSeries = if (filter != HomeMediaFilter.MANGA_ONLY) seriesItems else emptyList()

            // 4. "Because you watched"
            val lastInteractedItem = unifiedContinue.firstOrNull()
            val targetGenres =
                lastInteractedItem?.genres?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotBlank() }
                    ?: emptyList()

            val candidatePool = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> (animeItems + mangaItems)

                HeroSource.TRACKERS_ONLY -> (processedRemoteAnime + processedRemoteManga + movieSeriesList)

                HeroSource.BOTH -> (
                    processedRemoteAnime + processedRemoteManga + movieSeriesList + animeItems +
                        mangaItems
                    )
            }
                .distinctBy { "${it.mediaType.name}_${it.id}_${it.title.lowercase().trim()}" }

            val becauseYouWatchedList = if (lastInteractedItem != null && targetGenres.isNotEmpty()) {
                candidatePool
                    .filter { item ->
                        item.id != lastInteractedItem.id &&
                            item.title.lowercase().trim() != lastInteractedItem.title.lowercase().trim() &&
                            item.genres.split(",").any { g -> g.trim().lowercase() in targetGenres } &&
                            (!hideCompleted || item.progress < 1.0f)
                    }
            } else {
                emptyList()
            }

            val filteredRecs = candidatePool
                .filter { !hideCompleted || it.progress < 1.0f }
            val unifiedRecommended = filteredRecs.shuffled()

            val displayBecauseYouWatched = when (filter) {
                HomeMediaFilter.VIDEO_ONLY -> becauseYouWatchedList.filter { it.mediaType.isVideo }
                HomeMediaFilter.MANGA_ONLY -> becauseYouWatchedList.filter { it.mediaType.isReading }
                HomeMediaFilter.ALL -> becauseYouWatchedList
            }

            val displayRecommended = when (filter) {
                HomeMediaFilter.VIDEO_ONLY -> unifiedRecommended.filter { it.mediaType.isVideo }
                HomeMediaFilter.MANGA_ONLY -> unifiedRecommended.filter { it.mediaType.isReading }
                HomeMediaFilter.ALL -> unifiedRecommended
            }

            val allRemoteHeroItems = if (movieSeriesList.isNotEmpty()) {
                (
                    movieSeriesList.shuffled().take(
                        4,
                    ) + (processedRemoteAnime + processedRemoteManga).shuffled().take(3)
                    ).shuffled()
            } else {
                (processedRemoteAnime + processedRemoteManga).shuffled()
            }
            val videoItems = animeItems.filter { it.mediaType.isVideo }
            val fallbackCarousel = if (videoItems.isNotEmpty()) {
                videoItems.distinctBy { it.id }.take(7)
            } else {
                animeItems.distinctBy { it.id }.take(7)
            }

            val currentHeroList = when (heroSource) {
                HeroSource.LIBRARY_ONLY -> fallbackCarousel
                HeroSource.TRACKERS_ONLY -> allRemoteHeroItems
                HeroSource.BOTH -> allRemoteHeroItems.ifEmpty { fallbackCarousel }
            }

            logcat(LogPriority.DEBUG) {
                "HomeFeed State build: heroList=${currentHeroList.size}, " +
                    "allRemoteHero=${allRemoteHeroItems.size}, fallback=${fallbackCarousel.size}, " +
                    "remoteAnime=${processedRemoteAnime.size}, remoteManga=${processedRemoteManga.size}, " +
                    "movies=${movieItems.size}, series=${seriesItems.size}, " +
                    "heroSource=$heroSource"
            }

            _state.value = State(
                isLoading = false,
                isRefreshing = false,
                heroList = currentHeroList,
                continueList = unifiedContinue,
                becauseYouWatchedTitle = lastInteractedItem?.title,
                becauseYouWatchedIsAnime = lastInteractedItem?.isAnime ?: true,
                becauseYouWatchedList = displayBecauseYouWatched,
                recommendedList = displayRecommended,
                animeList = displayPopularAnime.take(limit),
                mangaList = displayPopularManga.take(limit),
                movieList = displayPopularMovies.take(limit),
                seriesList = displayPopularSeries.take(limit),
                showFeatured = showFeatured,
                showContinue = showContinue,
                showAiringToday = showAiringToday,
                showBecauseYouWatched = showBecauseYouWatched,
                showRecommended = showRecommended,
                showPopularAnime = showPopularAnime,
                showPopularManga = showPopularManga,
                showPopularMovies = showPopularMovies,
                showPopularSeries = showPopularSeries,
                mediaFilter = filter,
                autoScrollHero = autoScrollHero,
                heroSource = heroSource,
                itemsPerSection = limit,
                hideCompletedInRecommended = hideCompleted,
                enableTmdb = enableTmdb,
                enableAnilist = enableAnilist,
                // airingToday is loaded asynchronously from the Discover cache; keep the
                // latest loaded value instead of resetting it on every state rebuild
            ).copy(airingToday = _state.value.airingToday)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Error building home feed state" }
        }
    }

    private fun classifyMedia(
        title: String,
        genre: List<String>? = null,
        description: String? = null,
        sourceName: String? = null,
    ): MediaType {
        val titleClean = title.lowercase()
        val genreClean = genre?.joinToString(" ")?.lowercase() ?: ""
        val descClean = description?.lowercase() ?: ""
        val sourceClean = sourceName?.lowercase() ?: ""

        return when {
            sourceClean.contains("tmdb") || sourceClean.contains("cuevana") ||
                sourceClean.contains("pelis") || sourceClean.contains("cine") ||
                sourceClean.contains("movie") || sourceClean.contains("dorama") ||
                sourceClean.contains("kdrama") ||
                titleClean.contains("película") || titleClean.contains("movie") ||
                titleClean.contains("film") || genreClean.contains("película") ||
                genreClean.contains("movie") || genreClean.contains("cine") -> {
                if (titleClean.contains("película") || titleClean.contains("movie") ||
                    titleClean.contains("film") || genreClean.contains("película") ||
                    genreClean.contains("movie")
                ) {
                    MediaType.MOVIES
                } else {
                    MediaType.SERIES
                }
            }

            titleClean.contains("serie") || sourceClean.contains("series") ||
                sourceClean.contains("tv") || genreClean.contains("dorama") ||
                genreClean.contains("drama") -> MediaType.SERIES

            else -> MediaType.ANIME
        }
    }
}
