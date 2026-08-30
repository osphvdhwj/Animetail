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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
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
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Tab as VoyagerTab
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.discover.components.AiringScheduleView
import eu.kanade.tachiyomi.ui.discover.components.AnimiteAnimeMediaRow
import eu.kanade.tachiyomi.ui.discover.components.AnimiteHeroBannerCard
import eu.kanade.tachiyomi.ui.discover.components.AnimiteMangaMediaRow
import eu.kanade.tachiyomi.ui.discover.components.AnimiteMediaCard
import eu.kanade.tachiyomi.ui.discover.components.MediaDetailsBottomSheet
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
                title = "Tracking",
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

        var isSearchMode by remember { mutableStateOf(false) }
        var selectedDiscoverTab by remember { mutableIntStateOf(0) } // 0: Discover, 1: Airing (Live), 2: Manga

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
                            screenModel.loadTracking()
                            screenModel.loadAiringSchedule()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                            )
                        }
                    },
                    navigateUp = if (isSearchMode) {
                        {
                            isSearchMode = false
                            screenModel.updateSearchQuery("")
                        }
                    } else null,
                )
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // Tracker Selection Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Primary 3-Way Mode Switcher: Discover (Anime) | Airing (Live) | Manga
                PrimaryTabRow(
                    selectedTabIndex = selectedDiscoverTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedDiscoverTab == 0,
                        onClick = { selectedDiscoverTab = 0 },
                        text = { TabText("Anime") }
                    )
                    Tab(
                        selected = selectedDiscoverTab == 1,
                        onClick = { selectedDiscoverTab = 1 },
                        text = { TabText("Airing Live") }
                    )
                    Tab(
                        selected = selectedDiscoverTab == 2,
                        onClick = { selectedDiscoverTab = 2 },
                        text = { TabText("Manga") }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
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
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Failed to load ${selectedTracker.displayName} feed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentState.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
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
                                                onRetry = { screenModel.loadTracking() }
                                            )
                                        } else if (searchQuery.isNotBlank()) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 125.dp),
                                                contentPadding = PaddingValues(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize()
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
                                                        status = anime.publishing_status.ifBlank { null },
                                                        onClick = { screenModel.openAnimeDetails(anime) },
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                            ) {
                                                // Hero Banner
                                                val featured = currentState.trendingAnime.firstOrNull()
                                                if (featured != null) {
                                                    item {
                                                        val formattedScore = if (featured.score > 0) {
                                                            if (featured.score > 10) {
                                                                String.format(java.util.Locale.US, "%.1f", featured.score / 10f)
                                                            } else {
                                                                String.format(java.util.Locale.US, "%.1f", featured.score)
                                                            }
                                                        } else null

                                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                            AnimiteHeroBannerCard(
                                                                title = featured.title,
                                                                imageUrl = featured.cover_url,
                                                                score = formattedScore,
                                                                format = featured.publishing_type.ifBlank { "TRENDING" },
                                                                status = featured.publishing_status.ifBlank { null },
                                                                summary = featured.summary,
                                                                onClick = { screenModel.openAnimeDetails(featured) },
                                                            )
                                                        }
                                                    }
                                                }

                                                // Trending Row
                                                item {
                                                    AnimiteAnimeMediaRow(
                                                        title = "Trending Anime",
                                                        items = currentState.trendingAnime,
                                                        showRank = true,
                                                        onItemClick = { screenModel.openAnimeDetails(it) },
                                                    )
                                                }

                                                // Popular Row
                                                if (currentState.popularAnime.isNotEmpty()) {
                                                    item {
                                                        AnimiteAnimeMediaRow(
                                                            title = "Popular This Season",
                                                            items = currentState.popularAnime,
                                                            showRank = false,
                                                            onItemClick = { screenModel.openAnimeDetails(it) },
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
                                                onRetry = { screenModel.loadTracking() }
                                            )
                                        } else if (searchQuery.isNotBlank()) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = 125.dp),
                                                contentPadding = PaddingValues(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxSize()
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
                                                        status = manga.publishing_status.ifBlank { null },
                                                        onClick = { screenModel.openMangaDetails(manga) },
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                            ) {
                                                val featured = currentState.trendingManga.firstOrNull()
                                                if (featured != null) {
                                                    item {
                                                        val formattedScore = if (featured.score > 0) {
                                                            if (featured.score > 10) {
                                                                String.format(java.util.Locale.US, "%.1f", featured.score / 10f)
                                                            } else {
                                                                String.format(java.util.Locale.US, "%.1f", featured.score)
                                                            }
                                                        } else null

                                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                            AnimiteHeroBannerCard(
                                                                title = featured.title,
                                                                imageUrl = featured.cover_url,
                                                                score = formattedScore,
                                                                format = featured.publishing_type.ifBlank { "TRENDING" },
                                                                status = featured.publishing_status.ifBlank { null },
                                                                summary = featured.summary,
                                                                onClick = { screenModel.openMangaDetails(featured) },
                                                            )
                                                        }
                                                    }
                                                }

                                                item {
                                                    AnimiteMangaMediaRow(
                                                        title = "Trending Manga",
                                                        items = currentState.trendingManga,
                                                        showRank = true,
                                                        onItemClick = { screenModel.openMangaDetails(it) },
                                                    )
                                                }

                                                if (currentState.popularManga.isNotEmpty()) {
                                                    item {
                                                        AnimiteMangaMediaRow(
                                                            title = "Popular Manga",
                                                            items = currentState.popularManga,
                                                            showRank = false,
                                                            onItemClick = { screenModel.openMangaDetails(it) },
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

            // Media Details Bottom Sheet Modal
            if (selectedAnimeDetails != null || selectedMangaDetails != null) {
                MediaDetailsBottomSheet(
                    anime = selectedAnimeDetails,
                    manga = selectedMangaDetails,
                    onDismiss = screenModel::closeDetails,
                    onSearchInSources = { title, isAnime ->
                        if (isAnime) {
                            navigator.push(GlobalAnimeSearchScreen(searchQuery = title))
                        } else {
                            navigator.push(GlobalMangaSearchScreen(searchQuery = title))
                        }
                    },
                )
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Refresh")
        }
    }
}
