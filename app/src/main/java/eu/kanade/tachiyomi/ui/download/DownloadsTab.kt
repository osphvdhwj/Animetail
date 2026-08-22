package eu.kanade.tachiyomi.ui.download
import androidx.compose.material.icons.outlined.Delete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadHeaderItem
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadQueueViewModel
import eu.kanade.tachiyomi.ui.download.anime.animeDownloadTab
import eu.kanade.tachiyomi.ui.download.manga.MangaDownloadHeaderItem
import eu.kanade.tachiyomi.ui.download.manga.MangaDownloadQueueViewModel
import eu.kanade.tachiyomi.ui.download.manga.mangaDownloadTab
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

data object DownloadsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 6u,
                title = stringResource(MR.strings.label_download_queue),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val animeViewModel = metroViewModel<AnimeDownloadQueueViewModel>()
        val mangaScreenModel = metroViewModel<MangaDownloadQueueViewModel>()
        val animeDownloadList by animeViewModel.state.collectAsStateWithLifecycle()
        val mangaDownloadList by mangaScreenModel.state.collectAsStateWithLifecycle()
        val animeDownloadCount by remember {
            derivedStateOf { animeDownloadList.sumOf { it.subItems.size } }
        }
        val mangaDownloadCount by remember {
            derivedStateOf { mangaDownloadList.sumOf { it.subItems.size } }
        }
        val animeIsRunning by animeScreenModel.isDownloaderRunning.collectAsState()
        val mangaIsRunning by mangaScreenModel.isDownloaderRunning.collectAsState()

        val state = rememberPagerState { 2 }
        val snackbarHostState = remember { SnackbarHostState() }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        var fabExpanded by remember { mutableStateOf(true) }
        val nestedScrollConnection = remember {
            // All this lines just for fab state :/
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    fabExpanded = available.y >= 0
                    return scrollBehavior.nestedScrollConnection.onPreScroll(available, source)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    return scrollBehavior.nestedScrollConnection.onPostScroll(consumed, available, source)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return scrollBehavior.nestedScrollConnection.onPreFling(available)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    return scrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
                }
            }
        }

        val animeSelected by animeScreenModel.selectedItems.collectAsState()
        val mangaSelected by mangaScreenModel.selectedItems.collectAsState()

        val actionModeCounter = when (state.currentPage) {
            0 -> animeSelected.size
            1 -> mangaSelected.size
            else -> 0
        }

        val onCancelActionMode = {
            when (state.currentPage) {
                0 -> animeScreenModel.clearSelection()
                1 -> mangaScreenModel.clearSelection()
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    isActionMode = actionModeCounter > 0,
                    onCancelActionMode = onCancelActionMode,
                    titleContent = {
                        if (actionModeCounter > 0) {
                            eu.kanade.presentation.components.AppBarTitle(title = "", count = actionModeCounter)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(MR.strings.label_download_queue),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, false),
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (animeDownloadCount > 0) {
                                    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
                                    Pill(
                                        text = "$animeDownloadCount",
                                        modifier = Modifier.padding(start = 4.dp),
                                        color = MaterialTheme.colorScheme.onBackground
                                            .copy(alpha = pillAlpha),
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        if (actionModeCounter > 0) {
                            androidx.compose.material3.IconButton(onClick = {
                                when (state.currentPage) {
                                    0 -> animeScreenModel.startSelected()
                                    1 -> mangaScreenModel.startSelected()
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                                    contentDescription = "Start"
                                )
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                when (state.currentPage) {
                                    0 -> animeScreenModel.pauseSelected()
                                    1 -> mangaScreenModel.pauseSelected()
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Pause,
                                    contentDescription = "Pause"
                                )
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                when (state.currentPage) {
                                    0 -> animeScreenModel.deleteSelected()
                                    1 -> mangaScreenModel.deleteSelected()
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        } else {
                            when (state.currentPage) {
                                0 -> AnimeActions(animeScreenModel, animeDownloadList)
                                1 -> MangaActions(mangaScreenModel, mangaDownloadList)
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = when (state.currentPage) {
                        0 -> animeDownloadList.isNotEmpty()
                        1 -> mangaDownloadList.isNotEmpty()
                        else -> false
                    },
                    enter = fadeIn(),
                    val animeIsRunning by animeViewModel.isDownloaderRunning.collectAsStateWithLifecycle()
                    val mangaIsRunning by mangaScreenModel.isDownloaderRunning.collectAsStateWithLifecycle()
                    ExtendedFloatingActionButton(
                        text = {
                            val id = when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    AYMR.strings.action_stop
                                } else {
                                    AYMR.strings.action_continue
                                }

                                1 -> if (mangaIsRunning) {
                                    MR.strings.action_pause
                                } else {
                                    MR.strings.action_resume
                                }

                                else -> MR.strings.action_pause
                            }
                            Text(text = stringResource(id))
                        },
                        icon = {
                            val icon = when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                }

                                1 -> if (mangaIsRunning) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                }

                                else -> Icons.Filled.PlayArrow
                            }
                            Icon(imageVector = icon, contentDescription = null)
                        },
                        onClick = {
                            when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    animeViewModel.pauseDownloads()
                                } else {
                                    animeViewModel.startDownloads()
                                }

                                1 -> if (mangaIsRunning) {
                                    mangaScreenModel.pauseDownloads()
                                } else {
                                    mangaScreenModel.startDownloads()
                                }
                            }
                        },
                        expanded = fabExpanded,
                    )
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                // Modern Stats HUD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Device Storage",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getFreeStorageSpace(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Active Downloads",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val totalCount = animeDownloadCount + mangaDownloadCount
                            val statusText = if (animeIsRunning || mangaIsRunning) "Downloading" else "Paused"
                            Text(
                                text = "$totalCount items ($statusText)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (animeIsRunning || mangaIsRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                PrimaryTabRow(
                    selectedTabIndex = state.currentPage,
                    modifier = Modifier.zIndex(1f),
                ) {
                    listOf(
                        Tab(
                            selected = state.currentPage == 0,
                            onClick = { scope.launch { state.animateScrollToPage(0) } },
                            text = {
                                TabText(
                                    text = stringResource(AYMR.strings.label_anime),
                                    badgeCount = animeDownloadCount,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        Tab(
                            selected = state.currentPage == 1,
                            onClick = { scope.launch { state.animateScrollToPage(1) } },
                            text = {
                                TabText(
                                    text = stringResource(AYMR.strings.manga),
                                    badgeCount = mangaDownloadCount,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    verticalAlignment = Alignment.Top,
                    pageNestedScrollConnection = nestedScrollConnection,
                ) { page ->
                    when (page) {
                        0 -> animeDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )

                        1 -> mangaDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AnimeActions(
        animeViewModel: AnimeDownloadQueueViewModel,
        animeDownloadList: List<AnimeDownloadHeaderItem>,
    ) {
        if (animeDownloadList.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            val onDismissRequest = { sortExpanded = false }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = onDismissRequest,
            ) {
                NestedMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                            onClick = {
                                animeViewModel.reorderQueue(
                                    {
                                        it.download.episode.let { e ->
                                            e.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: e.dateUpload
                                        }
                                    },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                animeViewModel.reorderQueue(
                                    {
                                        it.download.episode.let { e ->
                                            e.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: e.dateUpload
                                        }
                                    },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
                NestedMenuItem(
                    text = {
                        Text(
                            text = stringResource(AYMR.strings.action_order_by_episode_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                animeViewModel.reorderQueue(
                                    { it.download.episode.episodeNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                animeViewModel.reorderQueue(
                                    { it.download.episode.episodeNumber },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
            }

            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_sort),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { sortExpanded = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_cancel_all),
                        onClick = { animeViewModel.clearQueue() },
                    ),
                    AppBar.OverflowAction(
                        title = "Clear completed",
                        onClick = { animeScreenModel.clearCompletedDownloads() },
                    ),
                    AppBar.OverflowAction(
                        title = "Clear errors",
                        onClick = { animeScreenModel.clearErrorDownloads() },
                    ),
                ),
            )
        }
    }

    @Composable
    private fun MangaActions(
        mangaScreenModel: MangaDownloadQueueViewModel,
        mangaDownloadList: List<MangaDownloadHeaderItem>,
    ) {
        if (mangaDownloadList.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            val onDismissRequest = { sortExpanded = false }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = onDismissRequest,
            ) {
                NestedMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    {
                                        it.download.chapter.let { c ->
                                            c.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: c.dateUpload
                                        }
                                    },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    {
                                        it.download.chapter.let { c ->
                                            c.dateUploadOverride.takeIf { d -> d > 0 }
                                                ?: c.dateUpload
                                        }
                                    },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
                NestedMenuItem(
                    text = {
                        Text(
                            text = stringResource(MR.strings.action_order_by_chapter_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.chapterNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.chapterNumber },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
            }

            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_sort),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { sortExpanded = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_cancel_all),
                        onClick = { mangaScreenModel.clearQueue() },
                    ),
                    AppBar.OverflowAction(
                        title = "Clear completed",
                        onClick = { mangaScreenModel.clearCompletedDownloads() },
                    ),
                    AppBar.OverflowAction(
                        title = "Clear errors",
                        onClick = { mangaScreenModel.clearErrorDownloads() },
                    ),
                ),
            )
        }
    }

    private fun getFreeStorageSpace(): String {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val gigabytes = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f GB Free", gigabytes)
        } catch (e: Exception) {
            "Unknown Free"
        }
    }
}

