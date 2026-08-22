package eu.kanade.tachiyomi.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.core.preference.asState
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object TrackingTab : Tab {
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

        var isSearchMode by remember { mutableStateOf(false) }
        var selectedMediaTabIndex by remember { mutableIntStateOf(0) }

        // Adjust tab index if tracker only supports Anime or Manga
        LaunchedEffect(selectedTracker) {
            if (!selectedTracker.supportsAnime && selectedMediaTabIndex == 0) {
                selectedMediaTabIndex = 1
            } else if (!selectedTracker.supportsManga && selectedMediaTabIndex == 1) {
                selectedMediaTabIndex = 0
            }
        }

        Scaffold(
            topBar = {
                SearchToolbar(
                    titleContent = {
                        AppBarTitle("Tracking")
                    },
                    searchEnabled = isSearchMode,
                    searchQuery = searchQuery,
                    onChangeSearchQuery = { screenModel.updateSearchQuery(it) },
                    onSearch = { screenModel.loadTracking(searchQuery) },
                    actions = {
                        IconButton(onClick = { isSearchMode = !isSearchMode }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                            )
                        }
                        IconButton(onClick = { screenModel.loadTracking() }) {
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
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Media Type Tabs (Anime vs Manga)
                val showAnimeTab = selectedTracker.supportsAnime
                val showMangaTab = selectedTracker.supportsManga

                if (showAnimeTab && showMangaTab) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedMediaTabIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedMediaTabIndex == 0,
                            onClick = { selectedMediaTabIndex = 0 },
                            text = { TabText("Anime") }
                        )
                        Tab(
                            selected = selectedMediaTabIndex == 1,
                            onClick = { selectedMediaTabIndex = 1 },
                            text = { TabText("Manga") }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
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
                            val isAnimeActive = if (showAnimeTab && showMangaTab) {
                                selectedMediaTabIndex == 0
                            } else {
                                showAnimeTab
                            }

                            if (isAnimeActive) {
                                if (currentState.trendingAnime.isEmpty()) {
                                    EmptyTrackingState(
                                        message = if (searchQuery.isNotBlank()) "No anime found for \"$searchQuery\"" else "No anime available",
                                        onRetry = { screenModel.loadTracking() }
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 115.dp),
                                        contentPadding = PaddingValues(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(currentState.trendingAnime) { anime ->
                                            val formattedScore = if (anime.score > 0) {
                                                if (anime.score > 10) {
                                                    String.format("%.1f", anime.score / 10f)
                                                } else {
                                                    String.format("%.1f", anime.score)
                                                }
                                            } else null

                                            TrackingCard(
                                                title = anime.title,
                                                imageUrl = anime.cover_url,
                                                score = formattedScore,
                                                format = anime.publishing_type.ifBlank { null },
                                                status = anime.publishing_status.ifBlank { null },
                                                onClick = {
                                                    navigator.push(GlobalAnimeSearchScreen(searchQuery = anime.title))
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (currentState.trendingManga.isEmpty()) {
                                    EmptyTrackingState(
                                        message = if (searchQuery.isNotBlank()) "No manga found for \"$searchQuery\"" else "No manga available",
                                        onRetry = { screenModel.loadTracking() }
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 115.dp),
                                        contentPadding = PaddingValues(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(currentState.trendingManga) { manga ->
                                            val formattedScore = if (manga.score > 0) {
                                                if (manga.score > 10) {
                                                    String.format("%.1f", manga.score / 10f)
                                                } else {
                                                    String.format("%.1f", manga.score)
                                                }
                                            } else null

                                            TrackingCard(
                                                title = manga.title,
                                                imageUrl = manga.cover_url,
                                                score = formattedScore,
                                                format = manga.publishing_type.ifBlank { null },
                                                status = manga.publishing_status.ifBlank { null },
                                                onClick = {
                                                    navigator.push(GlobalMangaSearchScreen(searchQuery = manga.title))
                                                }
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

@Composable
private fun EmptyTrackingState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(onClick = onRetry) {
            Text("Reload")
        }
    }
}

@Composable
private fun TrackingCard(
    title: String,
    imageUrl: String?,
    score: String?,
    format: String? = null,
    status: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 60f
                        )
                    )
            )

            // Top Badges Row (Format & Score)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!format.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (!score.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ $score",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Bottom Info (Title & Status)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                if (!status.isNullOrEmpty()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
