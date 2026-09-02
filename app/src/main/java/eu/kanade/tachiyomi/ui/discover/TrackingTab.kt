package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.discover.DiscoverPreferences
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.AppFloatingActionButton
import eu.kanade.presentation.components.FabContextMode
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Tab as VoyagerTab
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.discover.components.AiringScheduleView
import eu.kanade.tachiyomi.ui.discover.components.AnimiteAnimeMediaRow
import eu.kanade.tachiyomi.ui.discover.components.AnimiteHeroBannerCard
import eu.kanade.tachiyomi.ui.discover.components.AnimiteMangaMediaRow
import eu.kanade.tachiyomi.ui.discover.components.AnimiteMediaCard
import eu.kanade.tachiyomi.ui.discover.components.MediaDetailsBottomSheet
import eu.kanade.tachiyomi.ui.discover.components.ShortVideoItem
import eu.kanade.tachiyomi.ui.discover.components.ShortsVideoFeed
import eu.kanade.tachiyomi.ui.discover.components.SwipeableMediaCard
import eu.kanade.tachiyomi.ui.discover.components.TinderDiscoveryDeck
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object TrackingTab : VoyagerTab {
    private fun readResolve(): Any = TrackingTab

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
        // No-op
    }

    @Composable
    override fun isEnabled(): Boolean {
        val scope = rememberCoroutineScope()
        return remember {
            Injekt.get<eu.kanade.domain.ui.UiPreferences>().showNavDiscover.asState(scope)
        }.value
    }

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
        val isRefreshing by screenModel.isRefreshing.collectAsState()

        val discoverPrefs = remember { Injekt.get<DiscoverPreferences>() }
        val scope = rememberCoroutineScope()
        val enableShorts by discoverPrefs.enableExperimentalShorts().asState(scope)
        val enableSwipeDeck by discoverPrefs.enableTinderSwipeDeck().asState(scope)

        var isSearchMode by remember { mutableStateOf(false) }
        var selectedDiscoverTab by remember { mutableIntStateOf(0) } // 0: Anime, 1: Airing, 2: Manga, 3: Swipe Deck, 4: Shorts
        var showMenu by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                SearchToolbar(
                    titleContent = {
                        AppBarTitle("Discover & Track")
                    },
                    searchEnabled = isSearchMode,
                    searchQuery = searchQuery,
                    onChangeSearchQuery = { screenModel.updateSearchQuery(it.orEmpty()) },
                    onSearch = { screenModel.loadTracking(searchQuery) },
                    actions = {
                        IconButton(onClick = { isSearchMode = !isSearchMode }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
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
                                        selectedDiscoverTab = 3
                                    },
                                )
                            }
                            if (enableShorts) {
                                DropdownMenuItem(
                                    text = { Text("Shorts Feed (Experimental)") },
                                    onClick = {
                                        showMenu = false
                                        selectedDiscoverTab = 4
                                    },
                                )
                            }
                        }
                    },
                    navigateUp = if (isSearchMode) {
                        {
                            isSearchMode = false
                            screenModel.updateSearchQuery("")
                        }
                    } else null,
                )
            },
            floatingActionButton = {
                AppFloatingActionButton(
                    mode = if (selectedDiscoverTab == 3) FabContextMode.TOGGLE_SWIPE_DECK else FabContextMode.TOGGLE_SWIPE_DECK,
                    isSwipeDeckActive = selectedDiscoverTab == 3,
                    onClick = {
                        selectedDiscoverTab = if (selectedDiscoverTab == 3) 0 else 3
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // Tracker Selection Chips Row (only when in standard feed)
                if (selectedDiscoverTab < 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    )
                                },
                                leadingIcon = if (isLoggedIn) {
                                    {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(8.dp),
                                        ) {}
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }

                // Primary Mode Switcher Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedDiscoverTab.coerceIn(0, 4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Tab(
                        selected = selectedDiscoverTab == 0,
                        onClick = { selectedDiscoverTab = 0 },
                        text = { TabText("Anime") },
                    )
                    Tab(
                        selected = selectedDiscoverTab == 1,
                        onClick = { selectedDiscoverTab = 1 },
                        text = { TabText("Airing Live") },
                    )
                    Tab(
                        selected = selectedDiscoverTab == 2,
                        onClick = { selectedDiscoverTab = 2 },
                        text = { TabText("Manga") },
                    )
                    if (enableSwipeDeck) {
                        Tab(
                            selected = selectedDiscoverTab == 3,
                            onClick = { selectedDiscoverTab = 3 },
                            text = { TabText("Swipe Deck") },
                        )
                    }
                    if (enableShorts) {
                        Tab(
                            selected = selectedDiscoverTab == 4,
                            onClick = { selectedDiscoverTab = 4 },
                            text = { TabText("Shorts") },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    when (selectedDiscoverTab) {
                        1 -> {
                            // Airing Schedule Calendar & Countdowns
                            AiringScheduleView(
                                airingList = airingSchedule,
                                selectedDay = selectedAiringDay,
                                onSelectDay = screenModel::selectAiringDay,
                                onEpisodeClick = { episode ->
                                    val animeSearch = AnimeTrackSearch.create(TrackerSource.ANILIST.id).apply {
                                        remote_id = episode.mediaId
                                        title = episode.title
                                        cover_url = episode.coverUrl
                                        summary = episode.summary
                                        score = episode.score
                                        publishing_type = episode.format
                                    }
                                    screenModel.openAnimeDetails(animeSearch)
                                },
                                onWatchClick = { episode ->
                                    navigator.push(GlobalAnimeSearchScreen(searchQuery = episode.title))
                                },
                            )
                        }
                        3 -> {
                            // Tinder-Style Discovery Swipe Deck
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
                                onPass = { /* Card dismissed */ },
                                onRefresh = { screenModel.loadTracking() },
                            )
                        }
                        4 -> {
                            // Experimental Shorts Feed
                            ShortsVideoFeed(
                                items = emptyList(),
                                onWatchFullEpisode = { item ->
                                    // Start standalone player
                                },
                            )
                        }
                        else -> {
                            // Discover / Search Feed
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
                                    if (selectedDiscoverTab == 0) {
                                        // Anime Feed
                                        if (currentState.trendingAnime.isEmpty() && currentState.popularAnime.isEmpty()) {
                                            EmptyTrackingView(
                                                message = if (searchQuery.isNotBlank()) "No anime found for \"$searchQuery\"" else "No anime available",
                                                onRetry = { screenModel.loadTracking() },
                                            )
                                        } else if (searchQuery.isNotBlank()) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 125.dp),
                                                contentPadding = PaddingValues(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                items(currentState.trendingAnime) { anime ->
                                                    val formattedScore = if (anime.score > 0) {
                                                        if (anime.score > 10) {
                                                            String.format(java.util.Locale.US, "%.1f", anime.score / 10f)
                                                        } else {
                                                            String.format(java.util.Locale.US, "%.1f", anime.score)
                                                        }
                                                    } else null

                                                    AnimiteMediaCard(
                                                        title = anime.title,
                                                        imageUrl = anime.cover_url,
                                                        score = formattedScore,
                                                        format = anime.publishing_type.ifBlank { null },
                                                        onClick = { screenModel.openAnimeDetails(anime) },
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                contentPadding = PaddingValues(vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                val heroAnime = currentState.trendingAnime.firstOrNull()
                                                if (heroAnime != null) {
                                                    item {
                                                        AnimiteHeroBannerCard(
                                                            title = heroAnime.title,
                                                            imageUrl = heroAnime.cover_url,
                                                            format = heroAnime.publishing_type,
                                                            status = heroAnime.publishing_status,
                                                            summary = heroAnime.summary,
                                                            onClick = { screenModel.openAnimeDetails(heroAnime) },
                                                        )
                                                    }
                                                }

                                                if (currentState.trendingAnime.isNotEmpty()) {
                                                    item {
                                                        AnimiteAnimeMediaRow(
                                                            title = "Trending Now",
                                                            items = currentState.trendingAnime.drop(1),
                                                            onItemClick = screenModel::openAnimeDetails,
                                                        )
                                                    }
                                                }

                                                if (currentState.popularAnime.isNotEmpty()) {
                                                    item {
                                                        AnimiteAnimeMediaRow(
                                                            title = "All Time Popular",
                                                            items = currentState.popularAnime,
                                                            onItemClick = screenModel::openAnimeDetails,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Manga Feed
                                        if (currentState.trendingManga.isEmpty() && currentState.popularManga.isEmpty()) {
                                            EmptyTrackingView(
                                                message = if (searchQuery.isNotBlank()) "No manga found for \"$searchQuery\"" else "No manga available",
                                                onRetry = { screenModel.loadTracking() },
                                            )
                                        } else if (searchQuery.isNotBlank()) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 125.dp),
                                                contentPadding = PaddingValues(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                items(currentState.trendingManga) { manga ->
                                                    val formattedScore = if (manga.score > 0) {
                                                        if (manga.score > 10) {
                                                            String.format(java.util.Locale.US, "%.1f", manga.score / 10f)
                                                        } else {
                                                            String.format(java.util.Locale.US, "%.1f", manga.score)
                                                        }
                                                    } else null

                                                    AnimiteMediaCard(
                                                        title = manga.title,
                                                        imageUrl = manga.cover_url,
                                                        score = formattedScore,
                                                        format = manga.publishing_type.ifBlank { null },
                                                        onClick = { screenModel.openMangaDetails(manga) },
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                contentPadding = PaddingValues(vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                val heroManga = currentState.trendingManga.firstOrNull()
                                                if (heroManga != null) {
                                                    item {
                                                        AnimiteHeroBannerCard(
                                                            title = heroManga.title,
                                                            imageUrl = heroManga.cover_url,
                                                            format = heroManga.publishing_type,
                                                            status = heroManga.publishing_status,
                                                            summary = heroManga.summary,
                                                            onClick = { screenModel.openMangaDetails(heroManga) },
                                                        )
                                                    }
                                                }

                                                if (currentState.trendingManga.isNotEmpty()) {
                                                    item {
                                                        AnimiteMangaMediaRow(
                                                            title = "Trending Manga",
                                                            items = currentState.trendingManga.drop(1),
                                                            onItemClick = screenModel::openMangaDetails,
                                                        )
                                                    }
                                                }

                                                if (currentState.popularManga.isNotEmpty()) {
                                                    item {
                                                        AnimiteMangaMediaRow(
                                                            title = "Popular Manga",
                                                            items = currentState.popularManga,
                                                            onItemClick = screenModel::openMangaDetails,
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
                }
            }
        }

        // Details Bottom Sheet
        if (selectedAnimeDetails != null) {
            MediaDetailsBottomSheet(
                anime = selectedAnimeDetails,
                onDismiss = screenModel::closeDetails,
                onSearchInSources = { query, _ ->
                    navigator.push(GlobalAnimeSearchScreen(searchQuery = query))
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
            )
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
