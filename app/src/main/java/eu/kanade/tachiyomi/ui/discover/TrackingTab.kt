package eu.kanade.tachiyomi.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.presentation.design.DS
import eu.kanade.domain.track.service.TrackSyncEngine.SyncProgressState
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.library.components.LibraryMediaSwitcher
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import eu.kanade.presentation.track.components.SyncItemStatus
import eu.kanade.presentation.track.components.TrackerStatusItem
import eu.kanade.presentation.track.components.UniversalTrackerSyncDashboard
import eu.kanade.presentation.util.Tab as VoyagerTab
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.discover.components.AiringScheduleView
import eu.kanade.tachiyomi.ui.discover.components.DailyALAdvancedFilterSheet
import eu.kanade.tachiyomi.ui.discover.components.DailyALAiringRail
import eu.kanade.tachiyomi.ui.discover.components.DailyALAnimeRail
import eu.kanade.tachiyomi.ui.discover.components.DailyALGenreGrid
import eu.kanade.tachiyomi.ui.discover.components.DailyALHeroBanner
import eu.kanade.tachiyomi.ui.discover.components.DailyALMangaRail
import eu.kanade.tachiyomi.ui.discover.components.DailyALMediaCard
import eu.kanade.tachiyomi.ui.discover.components.DailyALRankingCategoryGrid
import eu.kanade.tachiyomi.ui.discover.components.DailyALSearchTagStrip
import eu.kanade.tachiyomi.ui.discover.components.DiscoverSectionBar
import eu.kanade.tachiyomi.ui.discover.ActivityState
import eu.kanade.tachiyomi.ui.discover.TrackedRailItem
import eu.kanade.tachiyomi.ui.discover.TrackedRailState
import eu.kanade.tachiyomi.ui.discover.components.DailyALActivityFeed
import eu.kanade.tachiyomi.ui.discover.components.DiscoverTrackedRail
import eu.kanade.tachiyomi.ui.discover.components.DailyALUserListBrowser
import eu.kanade.tachiyomi.ui.discover.components.DailyALProfileStateContainer
import eu.kanade.tachiyomi.ui.discover.components.GlobalAnimeFeaturesSheet
import eu.kanade.tachiyomi.ui.discover.components.MediaDetailsBottomSheet
import eu.kanade.tachiyomi.ui.discover.components.ShortsVideoFeed
import eu.kanade.tachiyomi.ui.discover.components.SwipeableMediaCard
import eu.kanade.tachiyomi.ui.discover.components.TinderDiscoveryDeck
import eu.kanade.tachiyomi.ui.discover.components.formatMediaCardScore
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The Discover page's internal sections. [index] matches the segmented picker
 * ordering (0..3); the remaining sections are reached via the FAB, Power Hub
 * sheet, or overflow menu.
 */
private enum class DiscoverSection(val index: Int) {
    AnimeHome(0),
    Airing(1),
    MangaHome(2),
    Explore(3),
    SwipeDeck(4),
    Shorts(5),
    SyncHub(6),
    Profile(7),
    ;

    companion object {
        fun fromIndex(index: Int): DiscoverSection = entries.firstOrNull { it.index == index } ?: AnimeHome
    }
}

private val MANGA_DETAIL_FORMATS = setOf("MANGA", "NOVEL", "ONE_SHOT", "ONE SHOT")

private fun relationIsManga(format: String?): Boolean = format?.uppercase() in MANGA_DETAIL_FORMATS

/** Maps the details-sheet status labels to AniList MediaListStatus values. */
private fun statusLabelToApi(label: String?): String? = when (label?.uppercase()) {
    "WATCHING", "READING" -> "CURRENT"
    "PLAN TO WATCH", "PLAN TO READ" -> "PLANNING"
    "COMPLETED" -> "COMPLETED"
    "ON HOLD" -> "PAUSED"
    "DROPPED" -> "DROPPED"
    else -> null
}

private val ANIME_FORMAT_FILTERS = listOf(
    "ALL" to "All Formats",
    "TV" to "TV Series",
    "MOVIE" to "Anime Movies",
    "OVA" to "OVA",
    "ONA" to "ONA / Web",
    "SPECIAL" to "Specials",
)

private val MANGA_FORMAT_FILTERS = listOf(
    "ALL" to "All Formats",
    "MANGA" to "Manga (JP)",
    "MANHWA" to "Manhwa (KR)",
    "MANHUA" to "Manhua (CN)",
    "NOVEL" to "Light Novels",
    "ONE_SHOT" to "One-Shots",
)

private fun animeFormatKey(item: AnimeTrackSearch): String =
    (item.publishing_type + " " + item.title).uppercase()

private fun matchesAnimeFormat(key: String, format: String): Boolean = when (format) {
    "TV" -> key.contains("TV")
    "MOVIE" -> key.contains("MOVIE")
    "OVA" -> key.contains("OVA")
    "ONA" -> key.contains("ONA")
    "SPECIAL" -> key.contains("SPECIAL")
    else -> true
}

private fun mangaFormatKey(item: MangaTrackSearch): String =
    (item.publishing_type + " " + item.title + " " + item.summary).uppercase()

private fun matchesMangaFormat(key: String, format: String): Boolean = when (format) {
    "MANHWA" -> key.contains("MANHWA") || key.contains("WEBTOON")
    "MANHUA" -> key.contains("MANHUA")
    "NOVEL" -> key.contains("NOVEL")
    "ONE_SHOT" -> key.contains("ONE_SHOT") || key.contains("ONE SHOT")
    "MANGA" -> !key.contains("MANHWA") && !key.contains("MANHUA") && !key.contains("NOVEL")
    else -> true
}

private fun top100Query(isManga: Boolean): String = if (isManga) "#all@manga" else "#all"

private fun episodeToAnimeSearch(episode: AiringEpisode): AnimeTrackSearch =
    AnimeTrackSearch.create(TrackerSource.ANILIST.id).apply {
        remote_id = episode.mediaId
        title = episode.title
        cover_url = episode.coverUrl
        summary = episode.summary
        score = episode.score
        publishing_type = episode.format
    }

private fun rollRandomMedia(state: TrackingState, isManga: Boolean, screenModel: TrackingScreenModel) {
    val current = state as? TrackingState.Success ?: return
    val allMedia: List<Any> = if (isManga) {
        current.trendingManga + current.popularManga
    } else {
        current.trendingAnime + current.popularAnime
    }
    when (val pick = allMedia.randomOrNull()) {
        is AnimeTrackSearch -> screenModel.openAnimeDetails(pick)
        is MangaTrackSearch -> screenModel.openMangaDetails(pick)
    }
}

/**
 * Opens the details sheet for an entry surfaced from deep-details relations or
 * recommendations, constructing a lightweight search stub from the available data.
 */
private fun openDeepEntry(
    id: Long,
    title: String,
    coverUrl: String?,
    format: String?,
    isManga: Boolean,
    screenModel: TrackingScreenModel,
) {
    if (isManga) {
        val stub = MangaTrackSearch.create(TrackerSource.ANILIST.id).apply {
            remote_id = id
            this.title = title
            cover_url = coverUrl.orEmpty()
            publishing_type = format.orEmpty()
        }
        screenModel.openMangaDetails(stub)
    } else {
        val stub = AnimeTrackSearch.create(TrackerSource.ANILIST.id).apply {
            remote_id = id
            this.title = title
            cover_url = coverUrl.orEmpty()
            publishing_type = format.orEmpty()
        }
        screenModel.openAnimeDetails(stub)
    }
}

/**
 * TrackingTab: The centralized Discovery & Exploration powerhouse of Animetail.
 */
data object TrackingTab : VoyagerTab {
    private fun readResolve(): Any = TrackingTab

    val onNavReselectEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onNavDoubleTapEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun onReselectEvent() {
        onNavReselectEvent.tryEmit(Unit)
    }

    fun onDoubleTapNav() {
        onNavDoubleTapEvent.tryEmit(Unit)
    }

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 2u,
                title = "Discover",
                icon = rememberVectorPainter(Icons.Outlined.Explore),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        onNavReselectEvent.emit(Unit)
    }

    @Composable
    override fun isEnabled(): Boolean = true

    @Composable
    override fun Content() {
        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { TrackingScreenModel() }
        val state by screenModel.state.collectAsState()
        val selectedTracker by screenModel.selectedTracker.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedAnimeDetails by screenModel.selectedAnimeDetails.collectAsState()
        val selectedMangaDetails by screenModel.selectedMangaDetails.collectAsState()
        val selectedAiringDay by screenModel.selectedAiringDay.collectAsState()
        val airingSchedule by screenModel.airingSchedule.collectAsState()
        val deepDetailsState by screenModel.deepDetailsState.collectAsState()
        val advancedFilter by screenModel.advancedFilter.collectAsState()
        val advancedFilterResults by screenModel.advancedFilterResults.collectAsState()
        val isLoadingFilterResults by screenModel.isLoadingFilterResults.collectAsState()
        val profileState by screenModel.profileState.collectAsState()
        val trackedRailState by screenModel.trackedRailState.collectAsState()
        val airingTrackedIds by screenModel.airingTrackedIds.collectAsState()
        LaunchedEffect(Unit) {
            screenModel.loadTrackedRail()
            screenModel.loadAiringTrackedIds()
        }

        val discoverPrefs = remember { Injekt.get<DiscoverPreferences>() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current
        val enableShorts by discoverPrefs.enableExperimentalShorts().asState(scope)
        val enableSwipeDeck by discoverPrefs.enableTinderSwipeDeck().asState(scope)
        val swipeBothSides by discoverPrefs.swipeBothSidesDismiss().asState(scope)

        var selectedSection by remember { mutableStateOf(DiscoverSection.AnimeHome) }
        var selectedAnimeFormat by remember { mutableStateOf("ALL") }
        var selectedMangaFormat by remember { mutableStateOf("ALL") }
        var showMenu by remember { mutableStateOf(false) }
        var showAdvancedFilterSheet by remember { mutableStateOf(false) }

        val animeListState = rememberLazyListState()
        val mangaListState = rememberLazyListState()

        var showGlobalFeaturesSheet by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            onNavReselectEvent.collect {
                val activeListState = if (selectedSection == DiscoverSection.MangaHome) mangaListState else animeListState
                if (activeListState.firstVisibleItemIndex > 0 || activeListState.firstVisibleItemScrollOffset > 0) {
                    activeListState.animateScrollToItem(0)
                } else {
                    showGlobalFeaturesSheet = true
                }
            }
        }

        LaunchedEffect(Unit) {
            onNavDoubleTapEvent.collect {
                showGlobalFeaturesSheet = true
            }
        }

        BackHandler(enabled = searchQuery != null || selectedSection != DiscoverSection.AnimeHome) {
            if (searchQuery != null) {
                screenModel.updateSearchQuery(null)
            } else if (selectedSection != DiscoverSection.AnimeHome) {
                selectedSection = DiscoverSection.AnimeHome
            }
        }

        // Collapsing large title state — declared before Scaffold so both the
        // toolbar title and the page body can read it
        val activeHomeListState = if (selectedSection == DiscoverSection.MangaHome) mangaListState else animeListState
        val collapseFraction by remember {
            derivedStateOf {
                if (selectedSection == DiscoverSection.AnimeHome || selectedSection == DiscoverSection.MangaHome) {
                    (activeHomeListState.firstVisibleItemScrollOffset / 220f).coerceIn(0f, 1f)
                } else {
                    1f
                }
            }
        }
        val titleAlpha by animateFloatAsState(
            targetValue = 1f - collapseFraction,
            animationSpec = DS.Motion.fast(),
            label = "title_alpha",
        )
        val titleScale by animateFloatAsState(
            targetValue = 1f - (collapseFraction * 0.25f),
            animationSpec = DS.Motion.fast(),
            label = "title_scale",
        )
        val titleHeight by animateDpAsState(
            targetValue = (44 * (1f - collapseFraction)).dp,
            animationSpec = DS.Motion.fast(),
            label = "title_height",
        )

        Scaffold(
            topBar = {
                SearchToolbar(
                    titleContent = {
                        // Compact title while the page-level hero (with the media
                        // switcher) is collapsed away by scrolling
                        androidx.compose.animation.AnimatedVisibility(
                            visible = collapseFraction > 0.9f,
                            enter = androidx.compose.animation.fadeIn(DS.Motion.emphasized()) +
                                androidx.compose.animation.expandHorizontally(DS.Motion.emphasized()),
                            exit = androidx.compose.animation.fadeOut(DS.Motion.fast()),
                        ) {
                            LibraryMediaSwitcher(
                                isAnime = selectedSection != DiscoverSection.MangaHome,
                                onSelectAnime = {
                                    if (selectedSection == DiscoverSection.MangaHome) {
                                        selectedSection = DiscoverSection.AnimeHome
                                    }
                                },
                                onSelectManga = {
                                    if (selectedSection != DiscoverSection.MangaHome) {
                                        selectedSection = DiscoverSection.MangaHome
                                    }
                                },
                            )
                        }
                    },
                    searchQuery = searchQuery,
                    onChangeSearchQuery = { screenModel.updateSearchQuery(it) },
                    placeholderText = "Search anime, manga, @tags, #genres...",
                    onSearch = { screenModel.loadTracking(searchQuery.orEmpty()) },
                    onClickCloseSearch = { screenModel.updateSearchQuery(null) },
                    actions = {
                        IconButton(onClick = { showGlobalFeaturesSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Discover Power Hub",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = {
                            screenModel.forceRefreshAllTrackers()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                            )
                        }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More Options",
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                text = { Text("✨ Discover Power Hub (Global)") },
                                onClick = {
                                    showMenu = false
                                    showGlobalFeaturesSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Force Reload All Sites") },
                                onClick = {
                                    showMenu = false
                                    screenModel.forceRefreshAllTrackers()
                                },
                            )
                            if (enableSwipeDeck) {
                                DropdownMenuItem(
                                    text = { Text("Tinder Swipe Deck") },
                                    onClick = {
                                        showMenu = false
                                        selectedSection = DiscoverSection.SwipeDeck
                                    },
                                )
                            }
                            if (enableShorts) {
                                DropdownMenuItem(
                                    text = { Text("Shorts Feed (Experimental)") },
                                    onClick = {
                                        showMenu = false
                                        selectedSection = DiscoverSection.Shorts
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Universal Multi-Sync") },
                                onClick = {
                                    showMenu = false
                                    selectedSection = DiscoverSection.SyncHub
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("⚙\uFE0F Advanced Filters") },
                                onClick = {
                                    showMenu = false
                                    showAdvancedFilterSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("AniList Profile") },
                                onClick = {
                                    showMenu = false
                                    selectedSection = DiscoverSection.Profile
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("🌐 Open AniList Website") },
                                onClick = {
                                    showMenu = false
                                    uriHandler.openUri("https://anilist.co")
                                },
                            )
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // Collapsing hero title (folds away as the home feed scrolls)
                if (titleHeight > 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(titleHeight)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Discover",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                alpha = titleAlpha
                                scaleX = titleScale
                                scaleY = titleScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.animation.AnimatedVisibility(
                            visible = collapseFraction < 0.9f,
                            enter = androidx.compose.animation.fadeIn(DS.Motion.emphasized()),
                            exit = androidx.compose.animation.fadeOut(DS.Motion.fast()),
                        ) {
                            LibraryMediaSwitcher(
                                isAnime = selectedSection != DiscoverSection.MangaHome,
                                onSelectAnime = {
                                    if (selectedSection == DiscoverSection.MangaHome) {
                                        selectedSection = DiscoverSection.AnimeHome
                                    }
                                },
                                onSelectManga = {
                                    if (selectedSection != DiscoverSection.MangaHome) {
                                        selectedSection = DiscoverSection.MangaHome
                                    }
                                },
                            )
                        }
                    }
                }

                // Tracker (website) source chips — one compact row
                if (selectedSection.index < 4 && searchQuery.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrackerSource.entries.forEach { tracker ->
                            val isSelected = tracker == selectedTracker
                            val isLoggedIn = screenModel.isTrackerLoggedIn(tracker)

                            FilterChip(
                                selected = isSelected,
                                onClick = { screenModel.selectTracker(tracker) },
                                label = {
                                    Text(
                                        text = tracker.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                    )
                                },
                                leadingIcon = if (isLoggedIn) {
                                    {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(6.dp),
                                        ) {}
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }

                // Contextual section picker: Home | Airing | Explore | Profile
                val isMangaMode = selectedSection == DiscoverSection.MangaHome
                val pickerTabs = listOf(
                    Triple(0, "Home", Icons.Outlined.Home as androidx.compose.ui.graphics.vector.ImageVector),
                    Triple(1, "Airing", Icons.Outlined.CalendarMonth),
                    Triple(2, "Explore", Icons.Outlined.Category),
                    Triple(3, "Profile", Icons.Outlined.Person),
                )
                val pickerSelection = when (selectedSection) {
                    DiscoverSection.Airing -> 1
                    DiscoverSection.Explore -> 2
                    DiscoverSection.Profile -> 3
                    else -> 0
                }
                DiscoverSectionBar(
                    tabs = pickerTabs,
                    selectedIndex = pickerSelection,
                    onSelect = { index ->
                        selectedSection = when (index) {
                            1 -> DiscoverSection.Airing
                            2 -> DiscoverSection.Explore
                            3 -> DiscoverSection.Profile
                            else -> if (isMangaMode) DiscoverSection.MangaHome else DiscoverSection.AnimeHome
                        }
                    },
                )

                if (searchQuery.isNullOrBlank() &&
                    (selectedSection == DiscoverSection.AnimeHome || selectedSection == DiscoverSection.MangaHome)
                ) {
                    DailyALSearchTagStrip(
                        isAnime = selectedSection == DiscoverSection.AnimeHome,
                        onTagClick = { tag ->
                            screenModel.updateSearchQuery(tag)
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = selectedSection,
                        transitionSpec = {
                            val forward = targetState.index >= initialState.index
                            (androidx.compose.animation.slideInHorizontally(
                                animationSpec = DS.Motion.slide(),
                                initialOffsetX = { if (forward) it / 6 else -it / 6 },
                            ) + androidx.compose.animation.fadeIn(animationSpec = DS.Motion.emphasized()))
                                .togetherWith(
                                    androidx.compose.animation.slideOutHorizontally(
                                        animationSpec = DS.Motion.fast(),
                                        targetOffsetX = { if (forward) -it / 8 else it / 8 },
                                    ) + androidx.compose.animation.fadeOut(animationSpec = DS.Motion.fast()),
                                )
                        },
                        label = "discover_section",
                    ) { section ->
                    when (section) {
                        DiscoverSection.Airing -> {
                            var airingLibraryOnly by remember { mutableStateOf(false) }
                            val airingListForView = if (airingLibraryOnly && airingTrackedIds.isNotEmpty()) {
                                airingSchedule.filter { it.mediaId in airingTrackedIds }
                            } else {
                                airingSchedule
                            }
                            AiringScheduleView(
                                airingList = airingListForView,
                                libraryOnly = airingLibraryOnly,
                                onToggleLibraryOnly = { airingLibraryOnly = !airingLibraryOnly },
                                selectedDay = selectedAiringDay,
                                onSelectDay = screenModel::selectAiringDay,
                                onEpisodeClick = { episode ->
                                    screenModel.openAnimeDetails(episodeToAnimeSearch(episode))
                                },
                                onWatchClick = { episode ->
                                    navigator.push(GlobalAnimeSearchScreen(searchQuery = episode.title))
                                },
                            )
                        }
                        DiscoverSection.Explore -> {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item(key = "advanced_filters_card") {
                                    androidx.compose.material3.OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clickable { showAdvancedFilterSheet = true },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FilterList,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Advanced Filters",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = "Search by genres, format, season, year and score",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            if (advancedFilter?.hasActiveFilters == true) {
                                                androidx.compose.material3.Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                ) {
                                                    Text(
                                                        text = "Active",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    DailyALGenreGrid(
                                        onGenreClick = { genre ->
                                            screenModel.updateSearchQuery(genre)
                                        },
                                    )
                                }
                                item {
                                    DailyALRankingCategoryGrid(
                                        isAnime = true,
                                        onCategoryClick = { categoryId ->
                                            when (categoryId) {
                                                "airing" -> selectedSection = DiscoverSection.Airing
                                                else -> screenModel.updateSearchQuery("#$categoryId")
                                            }
                                        },
                                    )
                                }
                                item {
                                    DailyALRankingCategoryGrid(
                                        isAnime = false,
                                        onCategoryClick = { categoryId ->
                                            when (categoryId) {
                                                "airing" -> selectedSection = DiscoverSection.Airing
                                                else -> screenModel.updateSearchQuery("#$categoryId")
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        DiscoverSection.SwipeDeck -> {
                            val swipeCards = remember(state) {
                                when (val curr = state) {
                                    is TrackingState.Success -> {
                                        val animeCards = (curr.trendingAnime + curr.popularAnime).map {
                                            SwipeableMediaCard(
                                                id = it.remote_id,
                                                title = it.title,
                                                coverUrl = it.cover_url,
                                                score = it.score.toFloat(),
                                                format = it.publishing_type,
                                                status = it.publishing_status,
                                                summary = it.summary,
                                                isAnime = true,
                                                animeItem = it,
                                            )
                                        }
                                        val mangaCards = (curr.trendingManga + curr.popularManga).map {
                                            SwipeableMediaCard(
                                                id = it.remote_id,
                                                title = it.title,
                                                coverUrl = it.cover_url,
                                                score = it.score.toFloat(),
                                                format = it.publishing_type,
                                                status = it.publishing_status,
                                                summary = it.summary,
                                                isAnime = false,
                                                mangaItem = it,
                                            )
                                        }
                                        (animeCards + mangaCards).shuffled()
                                    }
                                    else -> emptyList()
                                }
                            }

                            TinderDiscoveryDeck(
                                items = swipeCards,
                                onCardClick = { card ->
                                    if (card.isAnime && card.animeItem != null) {
                                        screenModel.openAnimeDetails(card.animeItem)
                                    } else if (!card.isAnime && card.mangaItem != null) {
                                        screenModel.openMangaDetails(card.mangaItem)
                                    }
                                },
                                onSaveToLibrary = { card ->
                                    if (card.isAnime) {
                                        navigator.push(GlobalAnimeSearchScreen(searchQuery = card.title))
                                    } else {
                                        navigator.push(GlobalMangaSearchScreen(searchQuery = card.title))
                                    }
                                },
                                onPass = { },
                                onRefresh = { screenModel.loadTracking() },
                                bothSidesDismiss = swipeBothSides,
                                onSearchInSources = { card ->
                                    if (card.isAnime) {
                                        navigator.push(GlobalAnimeSearchScreen(searchQuery = card.title))
                                    } else {
                                        navigator.push(GlobalMangaSearchScreen(searchQuery = card.title))
                                    }
                                },
                            )
                        }
                        DiscoverSection.Shorts -> {
                            ShortsVideoFeed(
                                items = emptyList(),
                                onWatchFullEpisode = { },
                            )
                        }
                        DiscoverSection.SyncHub -> {
                            val trackerManager = remember { Injekt.get<TrackerManager>() }
                            val uriHandler = LocalUriHandler.current

                            UniversalTrackerSyncDashboard(
                                trackersState = trackerManager.trackers.map { tracker ->
                                    TrackerStatusItem(
                                        tracker = tracker,
                                        isLoggedIn = tracker.isLoggedIn,
                                        syncStatus = if (tracker.isLoggedIn) SyncItemStatus.SYNCED else SyncItemStatus.NOT_LOGGED_IN,
                                    )
                                },
                                syncProgressState = SyncProgressState.Idle,
                                onSyncAll = {
                                    screenModel.forceRefreshAllTrackers()
                                },
                                onTrackerClick = { tracker ->
                                    tracker.trackerWebUrl()?.let { uriHandler.openUri(it) }
                                },
                            )
                        }
                        DiscoverSection.Profile -> {
                            LaunchedEffect(Unit) {
                                screenModel.loadProfileStats()
                            }
                            val userListState by screenModel.userListState.collectAsState()
                            val activityState by screenModel.activityState.collectAsState()
                            var selectedListStatus by remember { mutableStateOf("CURRENT") }
                            var selectedListIsManga by remember { mutableStateOf(false) }
                            LaunchedEffect(selectedListStatus, selectedListIsManga) {
                                screenModel.loadUserList(selectedListStatus, selectedListIsManga)
                            }
                            LaunchedEffect(Unit) {
                                screenModel.loadActivityFeed()
                            }
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                DailyALProfileStateContainer(
                                    state = profileState,
                                    onRetry = screenModel::loadProfileStats,
                                    onLoginClick = { navigator.push(SettingsTrackingScreen) },
                                    onRefresh = screenModel::loadProfileStats,
                                    internalScroll = false,
                                )
                                DailyALUserListBrowser(
                                    state = userListState,
                                    selectedStatus = selectedListStatus,
                                    onSelectStatus = { selectedListStatus = it },
                                    onRetry = {
                                        screenModel.loadUserList(
                                            selectedListStatus,
                                            selectedListIsManga,
                                            forceRefresh = true,
                                        )
                                    },
                                    onEntryUpdate = screenModel::updateUserListEntry,
                                    onEntryClick = { entry ->
                                        openDeepEntry(
                                            entry.mediaId,
                                            entry.title,
                                            entry.coverUrl,
                                            entry.format,
                                            isManga = entry.isManga,
                                            screenModel,
                                        )
                                    },
                                    isManga = selectedListIsManga,
                                    onMediaTypeChange = { selectedListIsManga = it },
                                )
                                DailyALActivityFeed(
                                    state = activityState,
                                    onRetry = { screenModel.loadActivityFeed(forceRefresh = true) },
                                    onEntryClick = { activity ->
                                        openDeepEntry(
                                            activity.mediaId,
                                            activity.mediaTitle,
                                            activity.mediaCoverUrl,
                                            null,
                                            isManga = false,
                                            screenModel,
                                        )
                                    },
                                )
                            }
                        }
                        DiscoverSection.AnimeHome, DiscoverSection.MangaHome -> {
                            val activeFilter = advancedFilter
                            if (activeFilter != null) {
                                DiscoverAdvancedFilterResults(
                                    filter = activeFilter,
                                    results = advancedFilterResults,
                                    isLoading = isLoadingFilterResults,
                                    onEditFilters = { showAdvancedFilterSheet = true },
                                    onClear = screenModel::clearAdvancedFilter,
                                    onOpenAnime = screenModel::openAnimeDetails,
                                )
                            } else {
                            when (val currentState = state) {
                                is TrackingState.Loading -> {
                                    LoadingScreen()
                                }
                                is TrackingState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = "Failed to load ${selectedTracker.displayName} feed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentState.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { screenModel.loadTracking() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                                is TrackingState.Success -> {
                                    DiscoverHomeFeed(
                                        state = currentState,
                                        isManga = selectedSection == DiscoverSection.MangaHome,
                                        trackedRailItems = (trackedRailState as? TrackedRailState.Success)?.items.orEmpty(),
                                        onOpenLibraryAnime = { navigator.push(AnimeScreen(it)) },
                                        searchQuery = searchQuery,
                                        listState = if (selectedSection == DiscoverSection.MangaHome) mangaListState else animeListState,
                                        airingSchedule = airingSchedule,
                                        selectedFormat = if (selectedSection == DiscoverSection.MangaHome) selectedMangaFormat else selectedAnimeFormat,
                                        onFormatChange = { value ->
                                            if (selectedSection == DiscoverSection.MangaHome) {
                                                selectedMangaFormat = value
                                            } else {
                                                selectedAnimeFormat = value
                                            }
                                        },
                                        onOpenAnime = screenModel::openAnimeDetails,
                                        onOpenManga = screenModel::openMangaDetails,
                                        onGoToSection = { selectedSection = it },
                                        onTagSearch = { screenModel.updateSearchQuery(it) },
                                        onRetry = { screenModel.loadTracking() },
                                    )
                                }
                            }
                            }
                        }
                    }
                    }
                }
            }
        }

        // Load rich details for whichever media the details sheet is showing
        LaunchedEffect(selectedAnimeDetails?.remote_id, selectedMangaDetails?.remote_id) {
            val animeId = selectedAnimeDetails?.remote_id
            val mangaId = selectedMangaDetails?.remote_id
            when {
                animeId != null -> screenModel.loadDeepDetails(animeId, isAnime = true)
                mangaId != null -> screenModel.loadDeepDetails(mangaId, isAnime = false)
                else -> screenModel.clearDeepDetails()
            }
        }

        val deepDetails = (deepDetailsState as? DeepDetailsState.Success)?.details

        if (selectedAnimeDetails != null) {
            MediaDetailsBottomSheet(
                anime = selectedAnimeDetails,
                onDismiss = screenModel::closeDetails,
                onSearchInSources = { query, _ ->
                    navigator.push(GlobalAnimeSearchScreen(searchQuery = query))
                },
                deepDetails = deepDetails,
                deepDetailsLoading = deepDetailsState is DeepDetailsState.Loading,
                onRelationClick = { relation ->
                    openDeepEntry(relation.id, relation.title, relation.coverUrl, relation.format, relationIsManga(relation.format), screenModel)
                },
                onRecommendationClick = { recommendation ->
                    openDeepEntry(
                        recommendation.id,
                        recommendation.title,
                        recommendation.coverUrl,
                        null,
                        false,
                        screenModel,
                    )
                },
                onTagClick = { tag ->
                    screenModel.updateSearchQuery(tag)
                },
                onProgressUpdate = { progress, statusLabel ->
                    selectedAnimeDetails?.let { details ->
                        screenModel.updateMediaProgress(
                            mediaId = details.remote_id,
                            progress = progress,
                            status = statusLabelToApi(statusLabel),
                        )
                    }
                },
            )
        }

        if (selectedMangaDetails != null) {
            MediaDetailsBottomSheet(
                manga = selectedMangaDetails,
                onDismiss = screenModel::closeDetails,
                onSearchInSources = { query, _ ->
                    navigator.push(GlobalMangaSearchScreen(searchQuery = query))
                },
                deepDetails = deepDetails,
                deepDetailsLoading = deepDetailsState is DeepDetailsState.Loading,
                onRelationClick = { relation ->
                    openDeepEntry(relation.id, relation.title, relation.coverUrl, relation.format, relationIsManga(relation.format), screenModel)
                },
                onRecommendationClick = { recommendation ->
                    openDeepEntry(
                        recommendation.id,
                        recommendation.title,
                        recommendation.coverUrl,
                        null,
                        false,
                        screenModel,
                    )
                },
                onTagClick = { tag ->
                    screenModel.updateSearchQuery(tag)
                },
            )
        }

        if (showAdvancedFilterSheet) {
            DailyALAdvancedFilterSheet(
                initial = advancedFilter ?: DailyALFilterState(),
                onApply = { filter ->
                    showAdvancedFilterSheet = false
                    if (selectedSection != DiscoverSection.AnimeHome) {
                        selectedSection = DiscoverSection.AnimeHome
                    }
                    screenModel.updateSearchQuery(null)
                    screenModel.applyAdvancedFilter(filter)
                },
                onDismissRequest = { showAdvancedFilterSheet = false },
            )
        }

        if (showGlobalFeaturesSheet) {
            GlobalAnimeFeaturesSheet(
                onDismissRequest = { showGlobalFeaturesSheet = false },
                onSelectMode = { mode ->
                    selectedSection = DiscoverSection.fromIndex(mode)
                    showGlobalFeaturesSheet = false
                },
                onRandomRoll = {
                    showGlobalFeaturesSheet = false
                    rollRandomMedia(state, selectedSection == DiscoverSection.MangaHome, screenModel)
                },
                onTop100 = {
                    showGlobalFeaturesSheet = false
                    screenModel.updateSearchQuery(top100Query(selectedSection == DiscoverSection.MangaHome))
                },
                onSeasonalChart = {
                    showGlobalFeaturesSheet = false
                    selectedSection = DiscoverSection.AnimeHome
                    selectedAnimeFormat = "ALL"
                },
            )
        }
    }

    /**
     * The anime/manga home feed. Both variants share the same structure; they only
     * differ in data lists, format filters, and a few anime-only rails.
     *
     * Arrangement: hero banner → format filters (adjacent to the content they
     * filter) → trending → airing today → this season → popular → top rated →
     * ranking shortcuts → genres.
     */
    @Composable
    private fun DiscoverHomeFeed(
        state: TrackingState.Success,
        isManga: Boolean,
        trackedRailItems: List<TrackedRailItem>,
        onOpenLibraryAnime: (Long) -> Unit,
        searchQuery: String?,
        listState: LazyListState,
        airingSchedule: List<AiringEpisode>,
        selectedFormat: String,
        onFormatChange: (String) -> Unit,
        onOpenAnime: (AnimeTrackSearch) -> Unit,
        onOpenManga: (MangaTrackSearch) -> Unit,
        onGoToSection: (DiscoverSection) -> Unit,
        onTagSearch: (String) -> Unit,
        onRetry: () -> Unit,
    ) {
        val trendingAnime = state.trendingAnime
        val popularAnime = state.popularAnime
        val trendingManga = state.trendingManga
        val popularManga = state.popularManga

        val hasContent = if (isManga) {
            trendingManga.isNotEmpty() || popularManga.isNotEmpty()
        } else {
            trendingAnime.isNotEmpty() || popularAnime.isNotEmpty()
        }
        if (!hasContent) {
            val media = if (isManga) "manga" else "anime"
            EmptyTrackingView(
                message = if (!searchQuery.isNullOrBlank()) "No $media found for \"$searchQuery\"" else "No $media available",
                onRetry = onRetry,
            )
            return
        }

        if (!searchQuery.isNullOrBlank()) {
            DiscoverSearchResults(
                state = state,
                isManga = isManga,
                onOpenAnime = onOpenAnime,
                onOpenManga = onOpenManga,
            )
            return
        }

        val filteredTrendingAnime = remember(trendingAnime, selectedFormat) {
            trendingAnime.filter { matchesAnimeFormat(animeFormatKey(it), selectedFormat) }
        }
        val filteredPopularAnime = remember(popularAnime, selectedFormat) {
            popularAnime.filter { matchesAnimeFormat(animeFormatKey(it), selectedFormat) }
        }
        val filteredTrendingManga = remember(trendingManga, selectedFormat) {
            trendingManga.filter { matchesMangaFormat(mangaFormatKey(it), selectedFormat) }
        }
        val filteredPopularManga = remember(popularManga, selectedFormat) {
            popularManga.filter { matchesMangaFormat(mangaFormatKey(it), selectedFormat) }
        }

        val topRatedAnime = remember(trendingAnime, popularAnime) {
            (popularAnime + trendingAnime)
                .distinctBy { it.remote_id }
                .filter { it.score > 0.0 }
                .sortedByDescending { it.score }
        }
        val topRatedManga = remember(trendingManga, popularManga) {
            (popularManga + trendingManga)
                .distinctBy { it.remote_id }
                .filter { it.score > 0.0 }
                .sortedByDescending { it.score }
        }

        val todayAiringEpisodes = remember(airingSchedule) { airingSchedule.take(15) }
        val seasonalAnime = if (isManga) {
            emptyList()
        } else {
            trendingAnime.filter {
                it.publishing_status.contains("AIR", true) || it.publishing_status.contains("RELEAS", true)
            }
        }

        val trendingTitle = if (isManga) {
            when (selectedFormat) {
                "MANHWA" -> "Trending Manhwa (Webtoons)"
                "MANHUA" -> "Trending Manhua"
                "NOVEL" -> "Trending Light Novels"
                "ONE_SHOT" -> "Trending One-Shots"
                else -> "Trending Now"
            }
        } else if (selectedFormat == "MOVIE") {
            "Trending Movies"
        } else {
            "Trending Now"
        }

        val popularTitle = if (isManga) {
            when (selectedFormat) {
                "MANHWA" -> "Popular Manhwa (Webtoons)"
                "MANHUA" -> "Popular Manhua"
                "NOVEL" -> "Popular Light Novels"
                "ONE_SHOT" -> "Popular One-Shots"
                else -> "All-Time Popular"
            }
        } else if (selectedFormat == "MOVIE") {
            "Popular Movies"
        } else {
            "All-Time Popular"
        }

        val formatFilters = if (isManga) MANGA_FORMAT_FILTERS else ANIME_FORMAT_FILTERS
        val heroAnime = if (isManga) null else filteredTrendingAnime.firstOrNull()
        val heroManga = if (isManga) filteredTrendingManga.firstOrNull() else null

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // "Continue from your AniList" rail — first thing on the page
            if (trackedRailItems.isNotEmpty()) {
                item(key = "tracked_rail") {
                    DiscoverTrackedRail(
                        items = trackedRailItems,
                        onItemClick = { item ->
                            val libraryId = item.libraryAnimeId
                            if (libraryId != null) {
                                onOpenLibraryAnime(libraryId)
                            } else {
                                onTagSearch(item.title)
                            }
                        },
                    )
                }
            }
            if (heroAnime != null || heroManga != null) {
                item(key = "hero") {
                    // Emphasized entrance: fade-up the hero on first composition
                    val heroEntered = remember { androidx.compose.animation.core.Animatable(0f) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        heroEntered.animateTo(
                            1f,
                            animationSpec = DS.Motion.emphasizedSlow(),
                        )
                    }
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = heroEntered.value
                            translationY = (1f - heroEntered.value) * 48f
                        },
                    ) {
                        if (heroManga != null) {
                            DailyALHeroBanner(
                                title = heroManga.title,
                                imageUrl = heroManga.cover_url,
                                seasonBadge = "📖 FEATURED MANGA",
                                score = formatMediaCardScore(heroManga.score),
                                format = heroManga.publishing_type.ifBlank { "MANGA" },
                                status = heroManga.publishing_status,
                                summary = heroManga.summary,
                                onClick = { onOpenManga(heroManga) },
                            )
                        } else if (heroAnime != null) {
                            DailyALHeroBanner(
                                title = heroAnime.title,
                                imageUrl = heroAnime.cover_url,
                                seasonBadge = "🌸 THIS SEASON",
                                score = formatMediaCardScore(heroAnime.score),
                                format = heroAnime.publishing_type.ifBlank { "TV" },
                                status = heroAnime.publishing_status,
                                summary = heroAnime.summary,
                                onClick = { onOpenAnime(heroAnime) },
                            )
                        }
                    }
                }
            }

            item(key = "format_strip") {
                DiscoverFormatStrip(
                    formats = formatFilters,
                    selectedFormat = selectedFormat,
                    onSelectFormat = onFormatChange,
                )
            }

            item(key = "trending_rail") {
                DiscoverMediaRail(
                    isManga = isManga,
                    title = trendingTitle,
                    animeItems = filteredTrendingAnime.drop(1),
                    mangaItems = filteredTrendingManga.drop(1),
                    showRank = true,
                    onOpenAnime = onOpenAnime,
                    onOpenManga = onOpenManga,
                    onViewAllClick = { onTagSearch(if (isManga) "#publishing" else "#airing") },
                )
            }

            if (!isManga && todayAiringEpisodes.isNotEmpty()) {
                item(key = "airing_today_rail") {
                    DailyALAiringRail(
                        title = "Airing Today",
                        items = todayAiringEpisodes,
                        onItemClick = { onOpenAnime(episodeToAnimeSearch(it)) },
                        onViewAllClick = { onGoToSection(DiscoverSection.Airing) },
                    )
                }
            }

            if (!isManga && seasonalAnime.isNotEmpty()) {
                item(key = "seasonal_anime_rail") {
                    DailyALAnimeRail(
                        title = "Popular This Season",
                        items = seasonalAnime,
                        showRank = false,
                        onItemClick = onOpenAnime,
                        onViewAllClick = { onTagSearch("#airing") },
                    )
                }
            }

            item(key = "popular_rail") {
                DiscoverMediaRail(
                    isManga = isManga,
                    title = popularTitle,
                    animeItems = filteredPopularAnime,
                    mangaItems = filteredPopularManga,
                    showRank = false,
                    onOpenAnime = onOpenAnime,
                    onOpenManga = onOpenManga,
                    onViewAllClick = { onTagSearch("#bypopularity") },
                )
            }

            item(key = "top_rated_rail") {
                DiscoverMediaRail(
                    isManga = isManga,
                    title = if (isManga) "Top Rated Manga" else "Top Rated Anime",
                    animeItems = topRatedAnime,
                    mangaItems = topRatedManga,
                    showRank = false,
                    onOpenAnime = onOpenAnime,
                    onOpenManga = onOpenManga,
                    onViewAllClick = { onTagSearch(top100Query(isManga)) },
                )
            }

            item(key = "ranking_grid") {
                if (isManga) {
                    DailyALRankingCategoryGrid(
                        isAnime = false,
                        onCategoryClick = { categoryId ->
                            when (categoryId) {
                                "manga" -> onFormatChange("MANGA")
                                "novels" -> onFormatChange("NOVEL")
                                "manhwa" -> onFormatChange("MANHWA")
                                "oneshots" -> onFormatChange("ONE_SHOT")
                                else -> onTagSearch("#$categoryId")
                            }
                        },
                    )
                } else {
                    DailyALRankingCategoryGrid(
                        isAnime = true,
                        onCategoryClick = { categoryId ->
                            when (categoryId) {
                                "airing" -> onGoToSection(DiscoverSection.Airing)
                                "movie" -> onFormatChange("MOVIE")
                                "ova" -> onFormatChange("OVA")
                                else -> onTagSearch("#$categoryId")
                            }
                        },
                    )
                }
            }

            item(key = "genres_grid") {
                DailyALGenreGrid(
                    onGenreClick = { genre ->
                        onTagSearch(genre)
                    },
                )
            }
        }
    }

    /** Search results rendered as an adaptive poster grid for the active media type. */
    @Composable
    private fun DiscoverSearchResults(
        state: TrackingState.Success,
        isManga: Boolean,
        onOpenAnime: (AnimeTrackSearch) -> Unit,
        onOpenManga: (MangaTrackSearch) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 135.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isManga) {
                items(state.trendingManga, key = { it.remote_id }) { manga ->
                    DailyALMediaCard(
                        title = manga.title,
                        imageUrl = manga.cover_url,
                        score = formatMediaCardScore(manga.score),
                        format = manga.publishing_type.ifBlank { null },
                        status = manga.publishing_status.ifBlank { null },
                        onClick = { onOpenManga(manga) },
                    )
                }
            } else {
                items(state.trendingAnime, key = { it.remote_id }) { anime ->
                    DailyALMediaCard(
                        title = anime.title,
                        imageUrl = anime.cover_url,
                        score = formatMediaCardScore(anime.score),
                        format = anime.publishing_type.ifBlank { null },
                        status = anime.publishing_status.ifBlank { null },
                        onClick = { onOpenAnime(anime) },
                    )
                }
            }
        }
    }

    /** Thin type-erasing wrapper so the anime and manga rails share one call site. */
    @Composable
    private fun DiscoverMediaRail(
        isManga: Boolean,
        title: String,
        animeItems: List<AnimeTrackSearch>,
        mangaItems: List<MangaTrackSearch>,
        showRank: Boolean,
        onOpenAnime: (AnimeTrackSearch) -> Unit,
        onOpenManga: (MangaTrackSearch) -> Unit,
        onViewAllClick: (() -> Unit)? = null,
    ) {
        if (isManga) {
            DailyALMangaRail(
                title = title,
                items = mangaItems,
                showRank = showRank,
                onItemClick = onOpenManga,
                onViewAllClick = onViewAllClick,
            )
        } else {
            DailyALAnimeRail(
                title = title,
                items = animeItems,
                showRank = showRank,
                onItemClick = onOpenAnime,
                onViewAllClick = onViewAllClick,
            )
        }
    }

    @Composable
    private fun DiscoverFormatStrip(
        formats: List<Pair<String, String>>,
        selectedFormat: String,
        onSelectFormat: (String) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            formats.forEach { (key, label) ->
                val isSel = selectedFormat == key
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectFormat(key) },
                    label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
    }

    /**
     * Grid of advanced-filter results with an active-filter summary header,
     * edit and clear actions, and a loading indicator while the query runs.
     */
    @Composable
    private fun DiscoverAdvancedFilterResults(
        filter: DailyALFilterState,
        results: List<AnimeTrackSearch>,
        isLoading: Boolean,
        onEditFilters: () -> Unit,
        onClear: () -> Unit,
        onOpenAnime: (AnimeTrackSearch) -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filter results (${results.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val summary = buildList {
                        filter.format?.let { add(it) }
                        filter.status?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                        filter.season?.let { season ->
                            add("$season${filter.seasonYear?.let { year -> " $year" } ?: ""}")
                        }
                        filter.minScore?.let { add("$it%+") }
                        if (filter.includedGenres.isNotEmpty()) add("+${filter.includedGenres.size} genres")
                        if (filter.excludedGenres.isNotEmpty()) add("−${filter.excludedGenres.size} genres")
                    }.joinToString("  •  ")
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                androidx.compose.material3.TextButton(onClick = onEditFilters) {
                    Text("Edit")
                }
                androidx.compose.material3.TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }

            when {
                isLoading -> LoadingScreen()
                results.isEmpty() -> EmptyTrackingView(
                    message = "No results match these filters",
                    onRetry = onEditFilters,
                )
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.remote_id }) { anime ->
                            DailyALMediaCard(
                                title = anime.title,
                                imageUrl = anime.cover_url,
                                score = formatMediaCardScore(anime.score),
                                format = anime.publishing_type.ifBlank { null },
                                status = anime.publishing_status.ifBlank { null },
                                onClick = { onOpenAnime(anime) },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyTrackingView(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Refresh")
            }
        }
    }
}

/** Website for a tracker, or null when it has no public web portal. */
private fun eu.kanade.tachiyomi.data.track.Tracker.trackerWebUrl(): String? = when (name.lowercase()) {
    "myanimelist" -> "https://myanimelist.net"
    "anilist" -> "https://anilist.co"
    "kitsu" -> "https://kitsu.app"
    "shikimori" -> "https://shikimori.one"
    "bangumi" -> "https://bgm.tv"
    "simkl" -> "https://simkl.com"
    "trakt" -> "https://trakt.tv"
    "mangaupdates", "manga updates" -> "https://www.mangaupdates.com"
    "mangabaka", "manga baka" -> "https://mangabaka.org"
    "hikka" -> "https://hikka.io"
    else -> null
}
