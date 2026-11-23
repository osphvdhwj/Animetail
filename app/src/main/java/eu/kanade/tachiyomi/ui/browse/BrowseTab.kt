package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.core.preference.asState
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.anime.extension.animeExtensionsTab
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.migrateAnimeSourceTab
import eu.kanade.tachiyomi.ui.browse.anime.source.animeSourcesTab
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenModel
import eu.kanade.tachiyomi.ui.browse.feed.feedTab
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.mangaExtensionsTab
import eu.kanade.tachiyomi.ui.browse.manga.migration.sources.migrateMangaSourceTab
import eu.kanade.tachiyomi.ui.browse.manga.source.mangaSourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.ui.UiPreferences

/**
 * Navigation tab descriptor for BrowseTab.
 *
 * Implements Voyager Tab navigation, defining icon, title, and routed content.
 */
data object BrowseTab : Tab {
    private fun readResolve(): Any = BrowseTab

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    // TODO: Find a way to let it open Global Anime/Manga Search depending on what Tab(e.g. Anime/Manga Source Tab) is open
    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalAnimeSearchScreen())
    }

    // SY -->
    @Composable
    override fun isEnabled(): Boolean {
        val scope = rememberCoroutineScope()
        return remember {
            Injekt.get<UiPreferences>().showNavBrowse.asState(scope)
        }.value
    }
    // SY <--

    private enum class PortalTabTarget {
        ANIME_EXTENSIONS,
        MANGA_EXTENSIONS,
        FEED,
        WEB_PORTALS,
        MIGRATE,
    }

    private val switchToPortalTabChannel = Channel<PortalTabTarget>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToPortalTabChannel.trySend(PortalTabTarget.MANGA_EXTENSIONS)
    }

    fun showAnimeExtension() {
        switchToPortalTabChannel.trySend(PortalTabTarget.ANIME_EXTENSIONS)
    }

    fun showFeed() {
        switchToPortalTabChannel.trySend(PortalTabTarget.FEED)
    }

    fun showWebPortals() {
        switchToPortalTabChannel.trySend(PortalTabTarget.WEB_PORTALS)
    }

    fun showMigrate() {
        switchToPortalTabChannel.trySend(PortalTabTarget.MIGRATE)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        // SY -->
        val uiPreferences = remember { context.appGraph.uiPreferences }
        val hideFeedTab by remember { uiPreferences.hideFeedTab.asState(scope) }
        val feedTabInFront by remember { uiPreferences.feedTabInFront.asState(scope) }
        // SY <--

        // Hoisted for extensions tab's search bar
        val mangaExtensionsViewModel = metroViewModel<MangaExtensionsViewModel>()
        val mangaExtensionsSearchQuery by mangaExtensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        val animeExtensionsViewModel = metroViewModel<AnimeExtensionsViewModel>()
        val animeExtensionsSearchQuery by animeExtensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        val animeExtensionsTabContent = animeExtensionsTab(animeExtensionsViewModel)
        val mangaExtensionsTabContent = mangaExtensionsTab(mangaExtensionsViewModel)

        // KMK -->
        val feedScreenModel = rememberScreenModel { FeedScreenModel() }
        // KMK <--

        // Hoisted so portal tiles can switch to these tabs by index
        val feedTabContent = feedTab(
            // KMK -->
            feedScreenModel,
            // KMK <--
        )
        val universalPortalTabContent = universalPortalTab()
        val migrateAnimeTabContent = migrateAnimeSourceTab()

        val tabs = when {
            hideFeedTab ->
                listOf(
                    animeSourcesTab(),
                    mangaSourcesTab(),
                    animeExtensionsTabContent,
                    mangaExtensionsTabContent,
                    migrateAnimeTabContent,
                    migrateMangaSourceTab(),
                )

            feedTabInFront ->
                listOf(
                    feedTabContent,
                    animeSourcesTab(),
                    mangaSourcesTab(),
                    animeExtensionsTabContent,
                    mangaExtensionsTabContent,
                    migrateAnimeTabContent,
                    migrateMangaSourceTab(),
                )

            else ->
                listOf(
                    animeSourcesTab(),
                    mangaSourcesTab(),
                    feedTabContent,
                    animeExtensionsTabContent,
                    mangaExtensionsTabContent,
                    universalPortalTabContent,
                    migrateAnimeTabContent,
                    migrateMangaSourceTab(),
                )
            // SY <--
        }

        val animeExtensionsTabIndex = remember(tabs, animeExtensionsTabContent) {
            tabs.indexOf(animeExtensionsTabContent)
        }
        val mangaExtensionsTabIndex = remember(tabs, mangaExtensionsTabContent) {
            tabs.indexOf(mangaExtensionsTabContent)
        }
        val feedTabIndex = remember(tabs, feedTabContent) { tabs.indexOf(feedTabContent) }
        val webPortalsTabIndex = remember(tabs, universalPortalTabContent) {
            tabs.indexOf(universalPortalTabContent)
        }
        val migrateTabIndex = remember(tabs, migrateAnimeTabContent) {
            tabs.indexOf(migrateAnimeTabContent)
        }

        val state = rememberPagerState { tabs.size }

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            mangaSearchQuery = mangaExtensionsSearchQuery,
            onChangeMangaSearchQuery = mangaExtensionsViewModel::search,
            animeSearchQuery = animeExtensionsSearchQuery,
            onChangeAnimeSearchQuery = animeExtensionsViewModel::search,
            animeExtensionsTabIndex = animeExtensionsTabIndex,
            mangaExtensionsTabIndex = mangaExtensionsTabIndex,
            // KMK -->
            feedScreenModel = feedScreenModel,
            // KMK <--
            scrollable = true,
        )
        LaunchedEffect(
            animeExtensionsTabIndex,
            mangaExtensionsTabIndex,
            feedTabIndex,
            webPortalsTabIndex,
            migrateTabIndex,
        ) {
            switchToPortalTabChannel.receiveAsFlow()
                .collectLatest { target ->
                    val tabIndex = when (target) {
                        PortalTabTarget.ANIME_EXTENSIONS -> animeExtensionsTabIndex
                        PortalTabTarget.MANGA_EXTENSIONS -> mangaExtensionsTabIndex
                        PortalTabTarget.FEED -> feedTabIndex
                        PortalTabTarget.WEB_PORTALS -> webPortalsTabIndex
                        PortalTabTarget.MIGRATE -> migrateTabIndex
                    }
                    if (tabIndex >= 0) {
                        state.scrollToPage(tabIndex)
                    }
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
        }
    }
}
