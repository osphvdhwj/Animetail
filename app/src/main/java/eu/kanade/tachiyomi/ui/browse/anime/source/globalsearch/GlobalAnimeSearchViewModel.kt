package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager

@AssistedInject
/**
 * State and business logic coordinator for GlobalAnimeSearchViewModel.
 *
 * Manages reactive UI state flows, coroutine lifecycle scopes,
 * and interactions with domain repositories and preferences.
 */
class GlobalAnimeSearchViewModel(
    @Assisted initialQuery: String,
    @Assisted initialExtensionFilter: String?,
    sourcePreferences: SourcePreferences,
    sourceManager: AnimeSourceManager,
    extensionManager: AnimeExtensionManager,
    networkToLocalAnime: NetworkToLocalAnime,
    getAnime: GetAnime,
    preferences: SourcePreferences,
    private val uiPreferences: UiPreferences,
) : AnimeSearchViewModel(
    initialState = State(searchQuery = initialQuery),
    sourcePreferences = sourcePreferences,
    sourceManager = sourceManager,
    extensionManager = extensionManager,
    networkToLocalAnime = networkToLocalAnime,
    getAnime = getAnime,
    preferences = preferences,
) {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(initialQuery: String, initialExtensionFilter: String?): GlobalAnimeSearchViewModel
    }

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(AnimeSourceFilter.All)
            }
            search()
        }
    }

    override fun getEnabledSources(): List<AnimeSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != AnimeSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }

    override fun search() {
        state.value.searchQuery?.takeIf { it.isNotBlank() }?.let(::recordRecentSearch)
        super.search()
    }

    private fun recordRecentSearch(query: String) {
        val current = uiPreferences.recentAnimeSearches.get().split('\n').filter { it.isNotBlank() }
        uiPreferences.recentAnimeSearches.set(
            (listOf(query) + current.filter { it != query }).take(10).joinToString("\n"),
        )
    }
}
