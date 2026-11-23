package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.core.util.ifAnimeSourcesLoaded
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.anime.GlobalAnimeSearchScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Native "recent searches" block shown before the first search runs, built from
 * the app's standard ListGroupHeader + AssistChip idioms.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentAnimeSearchesContent(
    queries: List<String>,
    onQueryClick: (String) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            ListGroupHeader(text = stringResource(TLMR.strings.recent_searches))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                queries.forEach { query ->
                    AssistChip(
                        onClick = { onQueryClick(query) },
                        label = { Text(text = query) },
                    )
                }
            }
        }
    }
}

/**
 * Screen-level composable hierarchy for GlobalAnimeSearchScreen.
 *
 * Configures the scaffold, top app bars, scroll behavior,
 * and responsive content layout for this screen.
 */
class GlobalAnimeSearchScreen(
    val searchQuery: String = "",
    private val extensionFilter: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifAnimeSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow

        val viewModel = assistedMetroViewModel<GlobalAnimeSearchViewModel, GlobalAnimeSearchViewModel.Factory> {
            create(searchQuery, extensionFilter)
        }
        val state by viewModel.state.collectAsState()
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val recentQueries = remember(state.searchQuery) {
            uiPreferences.recentAnimeSearches.get()
                .split('\n')
                .filter { it.isNotBlank() }
        }
        var showSingleLoadingScreen by remember {
            mutableStateOf(
                searchQuery.isNotEmpty() && !extensionFilter.isNullOrEmpty() && state.total == 1,
            )
        }

        if (showSingleLoadingScreen) {
            LoadingScreen()

            LaunchedEffect(state.items) {
                when (val result = state.items.values.singleOrNull()) {
                    AnimeSearchItemResult.Loading -> return@LaunchedEffect

                    is AnimeSearchItemResult.Success -> {
                        val anime = result.result.singleOrNull()
                        if (anime != null) {
                            navigator.replace(AnimeScreen(anime.id, true))
                        } else {
                            // Backoff to result screen
                            showSingleLoadingScreen = false
                        }
                    }

                    else -> showSingleLoadingScreen = false
                }
            }

            return
        }

        if (state.searchQuery.isNullOrBlank() && state.items.isEmpty() && recentQueries.isNotEmpty()) {
            RecentAnimeSearchesContent(
                queries = recentQueries,
                onQueryClick = {
                    viewModel.updateSearchQuery(it)
                    viewModel.search()
                },
            )
            return
        }

        GlobalAnimeSearchScreen(
            state = state,
            navigateUp = navigator::pop,
            onChangeSearchQuery = viewModel::updateSearchQuery,
            onSearch = { viewModel.search() },
            getAnime = viewModel::getAnime,
            onChangeSearchFilter = viewModel::setSourceFilter,
            onToggleResults = viewModel::toggleFilterResults,
            onClickSource = { source ->
                navigator.push(
                    BrowseAnimeSourceScreen(
                        source.id,
                        viewModel.state.value.searchQuery,
                    ),
                )
            },
            onClickItem = { navigator.push(AnimeScreen(it.id, true)) },
            onLongClickItem = { navigator.push(AnimeScreen(it.id, true)) },
        )
    }
}
