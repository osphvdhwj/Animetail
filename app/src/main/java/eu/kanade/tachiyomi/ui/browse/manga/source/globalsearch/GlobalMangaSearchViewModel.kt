package eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.manga.service.MangaSourceManager

@AssistedInject
/**
 * State and business logic coordinator for GlobalMangaSearchViewModel.
 *
 * Manages reactive UI state flows, coroutine lifecycle scopes,
 * and interactions with domain repositories and preferences.
 */
class GlobalMangaSearchViewModel(
    @Assisted initialQuery: String,
    @Assisted initialExtensionFilter: String?,
    sourcePreferences: SourcePreferences,
    sourceManager: MangaSourceManager,
    extensionManager: MangaExtensionManager,
    networkToLocalManga: NetworkToLocalManga,
    getManga: GetManga,
    preferences: SourcePreferences,
    private val uiPreferences: UiPreferences,
) : MangaSearchViewModel(
    initialState = State(searchQuery = initialQuery),
    sourcePreferences = sourcePreferences,
    sourceManager = sourceManager,
    extensionManager = extensionManager,
    networkToLocalManga = networkToLocalManga,
    getManga = getManga,
    preferences = preferences,
) {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(initialQuery: String, initialExtensionFilter: String?): GlobalMangaSearchViewModel
    }

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(MangaSourceFilter.All)
            }
            search()
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != MangaSourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }

    override fun search() {
        state.value.searchQuery?.takeIf { it.isNotBlank() }?.let(::recordRecentSearch)
        super.search()
    }

    private fun recordRecentSearch(query: String) {
        val current = uiPreferences.recentMangaSearches.get().split('\n').filter { it.isNotBlank() }
        uiPreferences.recentMangaSearches.set(
            (listOf(query) + current.filter { it != query }).take(10).joinToString("\n"),
        )
    }
}
