package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.presentation.components.FloatingIslandNavigationBar
import eu.kanade.presentation.design.DS
import eu.kanade.presentation.design.glassSurface
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadsTab
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.discover.TrackingTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.cast.CastMiniController
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 200
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = context.appGraph.uiPreferences
        val navStyle by uiPreferences.navStyle.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        // SY -->
        val scope = rememberCoroutineScope()
        val alwaysShowLabel by remember {
            uiPreferences.bottomBarLabels.asState(scope)
        }
        // SY <--
        val activity = context as? ComponentActivity
        val preferences = context.appGraph.preferenceStore
        val castManager = remember { CastManager(activity!!, preferences) }

        LaunchedEffect(Unit) {
            castManager.startDeviceDiscovery()
            castManager.reconnect()
        }

        val showHomeTab by uiPreferences.showHomeTab.collectAsState()
        val startScreenPref by uiPreferences.startScreen.collectAsState()

        val effectiveStartTab = remember(showHomeTab, startScreenPref) {
            val configuredTab = startScreenPref.tab
            if (configuredTab == HomeTab && !showHomeTab) {
                LibraryTab
            } else {
                configuredTab
            }
        }

        TabNavigator(
            tab = effectiveStartTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .glassSurface(
                                        shape = DS.radiusHero,
                                        containerColor = DS.Glass.containerColor(0.85f),
                                        shadowElevation = DS.elevationMedium,
                                    ),
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            ) {
                                navStyle.tabs
                                    .fastFilter { it.isEnabled() }
                                    .fastForEach {
                                        NavigationRailItem(it, alwaysShowLabel)
                                    }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            Column {
                                val isConnected =
                                    castManager.castState.collectAsState().value == CastManager.CastState.CONNECTED
                                AnimatedVisibility(
                                    visible = isConnected,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    CastMiniController(
                                        castManager = castManager,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .glassSurface(shape = DS.radiusCard),
                                    )
                                }

                                val bottomNavVisible by produceState(initialValue = true) {
                                    showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                                }

                                AnimatedVisibility(
                                    visible = bottomNavVisible && tabNavigator.current != navStyle.moreTab,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    FloatingIslandNavigationBar {
                                        navStyle.tabs
                                            .fastFilter { it.isEnabled() }
                                            .fastForEach {
                                                NavigationBarItem(it, alwaysShowLabel)
                                            }
                                    }
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    initialScale = 1f,
                                    durationMillis = TAB_FADE_DURATION,
                                ) togetherWith
                                    materialFadeThroughOut(durationMillis = TAB_FADE_DURATION)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            val goToStartScreen = {
                if (effectiveStartTab != navStyle.moreTab) {
                    tabNavigator.current = effectiveStartTab
                } else {
                    tabNavigator.current = if (showHomeTab) HomeTab else LibraryTab
                }
            }
            BackHandler(
                enabled = (tabNavigator.current == navStyle.moreTab || tabNavigator.current != effectiveStartTab) &&
                    (tabNavigator.current != LibraryTab || effectiveStartTab != navStyle.moreTab),
                onBack = goToStartScreen,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToStartScreen()
                        when (effectiveStartTab) {
                            LibraryTab -> LibraryTab.search(it)
                            AnimeLibraryTab -> AnimeLibraryTab.search(it)
                            MangaLibraryTab -> MangaLibraryTab.search(it)
                            else -> {}
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Home -> if (showHomeTab) HomeTab else LibraryTab

                            is Tab.AnimeLib -> LibraryTab

                            is Tab.Library -> LibraryTab

                            is Tab.Updates -> UpdatesTab

                            is Tab.History -> HistoriesTab

                            is Tab.Discover -> {
                                HomeTab.activeSubTab.value = 1
                                HomeTab
                            }

                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    if (!it.anime) {
                                        BrowseTab.showExtension()
                                    } else {
                                        BrowseTab.showAnimeExtension()
                                    }
                                }
                                BrowseTab
                            }

                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.AnimeLib && it.animeIdToOpen != null) {
                            navigator.push(AnimeScreen(it.animeIdToOpen))
                        }
                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadsTab)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun RowScope.NavigationBarItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        val lastClickTime = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
        NavigationBarItem(
            selected = selected,
            onClick = {
                val now = System.currentTimeMillis()
                if (selected && now - lastClickTime.longValue < 350L) {
                    lastClickTime.longValue = 0L
                    scope.launch {
                        if (tab is eu.kanade.tachiyomi.ui.home.HomeTab) {
                            eu.kanade.tachiyomi.ui.discover.TrackingTab.onDoubleTapNav()
                        } else {
                            tab.onReselect(navigator)
                        }
                    }
                } else {
                    lastClickTime.longValue = now
                    if (!selected) {
                        tabNavigator.current = tab
                    } else {
                        scope.launch { tab.onReselect(navigator) }
                    }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            ),
            alwaysShowLabel = alwaysShowLabel,
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        val lastRailClickTime = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
        NavigationRailItem(
            selected = selected,
            onClick = {
                val now = System.currentTimeMillis()
                if (selected && now - lastRailClickTime.longValue < 350L) {
                    lastRailClickTime.longValue = 0L
                    scope.launch {
                        if (tab is eu.kanade.tachiyomi.ui.home.HomeTab) {
                            eu.kanade.tachiyomi.ui.discover.TrackingTab.onDoubleTapNav()
                        } else {
                            tab.onReselect(navigator)
                        }
                    }
                } else {
                    lastRailClickTime.longValue = now
                    if (!selected) {
                        tabNavigator.current = tab
                    } else {
                        scope.launch { tab.onReselect(navigator) }
                    }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            ),
            alwaysShowLabel = alwaysShowLabel,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        val context = LocalContext.current
        BadgedBox(
            badge = {
                when {
                    UpdatesTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = context.appGraph.libraryPreferences
                            combine(
                                pref.newAnimeUpdatesCount.changes(),
                                pref.newMangaUpdatesCount.changes(),
                            ) { countAnime, countManga -> countAnime + countManga }
                                .collectLatest { value = if (pref.newShowUpdatesCount.get()) it else 0 }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }

                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = context.appGraph.sourcePreferences
                            combine(
                                pref.extensionUpdatesCount.changes(),
                                pref.animeExtensionUpdatesCount.changes(),
                            ) { extCount, animeExtCount -> extCount + animeExtCount }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            val isTrackingTab = eu.kanade.tachiyomi.ui.discover.TrackingTab::class.isInstance(tab)
            val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current
            val isSelected = tabNavigator.current::class == tab::class
            // Discover icon: spring pop when selected, gentle settle when not
            val iconScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = if (isSelected) {
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    )
                } else {
                    androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    )
                },
                label = "nav_icon_scale",
            )

            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                tint = LocalContentColor.current,
                modifier = Modifier.graphicsLayer(
                    scaleX = iconScale,
                    scaleY = iconScale,
                ),
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data object Home : Tab
        data class AnimeLib(val animeIdToOpen: Long? = null) : Tab
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data object Discover : Tab
        data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}


