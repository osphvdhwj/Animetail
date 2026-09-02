package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import eu.kanade.tachiyomi.ui.download.DownloadsTab
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.setting.PlayerSettingsScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsTab
import eu.kanade.tachiyomi.ui.storage.StorageTab
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import mihon.feature.support.SupportUsScreen
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
            return TabOptions(
                index = 4u,
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = metroViewModel<MoreViewModel>()
        val downloadQueueState by viewModel.downloadQueueState.collectAsState()
        val recentHistory by viewModel.recentHistory.collectAsState()
        val navStyle = currentNavigationStyle()
        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = viewModel.downloadedOnly,
            onDownloadedOnlyChange = { viewModel.downloadedOnly = it },
            incognitoMode = viewModel.incognitoMode,
            onIncognitoModeChange = { viewModel.incognitoMode = it },
            // SY -->
            showNavUpdates = viewModel.showNavUpdates,
            showNavHistory = viewModel.showNavHistory,
            // SY <--
            navStyle = navStyle,
            recentHistoryProvider = {
                recentHistory.map { entry ->
                    entry.copy(
                        onClick = {
                            if (entry.isAnime) {
                                navigator.push(AnimeScreen(entry.id))
                            } else {
                                navigator.push(MangaScreen(entry.id))
                            }
                        },
                        onResumeClick = {
                            if (entry.isAnime) {
                                context.startActivity(PlayerActivity.newIntent(context, entry.id, entry.id))
                            } else {
                                context.startActivity(eu.kanade.tachiyomi.ui.reader.ReaderActivity.newIntent(context, entry.id, entry.id))
                            }
                        },
                    )
                }
            },
            onClickHistory = { navigator.push(eu.kanade.tachiyomi.ui.history.HistoriesTab) },
            onClickAlt = { navigator.push(navStyle.moreTab) },
            onClickDownloadQueue = { navigator.push(DownloadsTab) },
            onClickCategories = { navigator.push(CategoriesTab) },
            onClickStats = { navigator.push(StatsTab) },
            onClickNetworkStream = { navigator.push(NetworkStreamScreen) },
            onClickStorage = { navigator.push(StorageTab) },
            onClickDataAndStorage = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
            onClickPlayerSettings = { navigator.push(PlayerSettingsScreen(mainSettings = false)) },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickSupport = { navigator.push(SupportUsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.MORE)
            // <-- AM (DISCORD)
        }
    }
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class MoreViewModel(
    private val downloadManager: MangaDownloadManager,
    private val animeDownloadManager: AnimeDownloadManager,
    private val getAnimeHistory: tachiyomi.domain.history.anime.interactor.GetAnimeHistory,
    private val getMangaHistory: tachiyomi.domain.history.manga.interactor.GetMangaHistory,
    preferences: BasePreferences,
    // SY -->
    uiPreferences: UiPreferences,
    // SY <--
) : ViewModel() {

    var downloadedOnly by preferences.downloadedOnly.asState(viewModelScope)
    var incognitoMode by preferences.incognitoMode.asState(viewModelScope)

    // SY -->
    val showNavUpdates by uiPreferences.showNavUpdates.asState(viewModelScope)
    val showNavHistory by uiPreferences.showNavHistory.asState(viewModelScope)
    // SY <--

    private var _downloadQueueState: MutableStateFlow<DownloadQueueState> = MutableStateFlow(
        DownloadQueueState.Stopped,
    )
    val downloadQueueState: StateFlow<DownloadQueueState> = _downloadQueueState.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<eu.kanade.presentation.history.components.RecentHistoryEntry>>(emptyList())
    val recentHistory: StateFlow<List<eu.kanade.presentation.history.components.RecentHistoryEntry>> = _recentHistory.asStateFlow()

    init {
        // Load YouTube-style recent history shelf
        viewModelScope.launchIO {
            try {
                combine(
                    getAnimeHistory.subscribe(""),
                    getMangaHistory.subscribe(""),
                ) { animeList: List<AnimeHistoryWithRelations>, mangaList: List<MangaHistoryWithRelations> ->
                    val animeEntries = animeList.take(8).map { anime ->
                        eu.kanade.presentation.history.components.RecentHistoryEntry(
                            id = anime.animeId,
                            title = anime.title,
                            subtitle = "Ep. ${anime.episodeNumber.toInt()}",
                            coverUrl = anime.coverData.url ?: "",
                            progress = 0.5f,
                            isAnime = true,
                            onClick = {},
                            onResumeClick = {},
                        )
                    }
                    val mangaEntries = mangaList.take(8).map { manga ->
                        eu.kanade.presentation.history.components.RecentHistoryEntry(
                            id = manga.mangaId,
                            title = manga.title,
                            subtitle = "Ch. ${manga.chapterNumber.toInt()}",
                            coverUrl = manga.coverData.url ?: "",
                            progress = 0.5f,
                            isAnime = false,
                            onClick = {},
                            onResumeClick = {},
                        )
                    }
                    (animeEntries + mangaEntries).take(12)
                }.collectLatest { items ->
                    _recentHistory.value = items
                }
            } catch (_: Exception) {}
        }
        // Handle running/paused status change and queue progress updating
        viewModelScope.launchIO {
            combine(
                downloadManager.isDownloaderRunning,
                downloadManager.queueState,
            ) { isRunningManga, mangaDownloadQueue -> Pair(isRunningManga, mangaDownloadQueue.size) }
                .collectLatest { (isDownloadingManga, mangaDownloadQueueSize) ->
                    combine(
                        animeDownloadManager.isDownloaderRunning,
                        animeDownloadManager.queueState,
                    ) { isRunningAnime, animeDownloadQueue ->
                        Pair(
                            isRunningAnime,
                            animeDownloadQueue.size,
                        )
                    }
                        .collectLatest { (isDownloadingAnime, animeDownloadQueueSize) ->
                            val isDownloading = isDownloadingAnime || isDownloadingManga
                            val downloadQueueSize = mangaDownloadQueueSize + animeDownloadQueueSize
                            val pendingDownloadExists = downloadQueueSize != 0
                            _downloadQueueState.value = when {
                                !pendingDownloadExists -> DownloadQueueState.Stopped
                                !isDownloading -> DownloadQueueState.Paused(downloadQueueSize)
                                else -> DownloadQueueState.Downloading(downloadQueueSize)
                            }
                        }
                }
        }
    }
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
