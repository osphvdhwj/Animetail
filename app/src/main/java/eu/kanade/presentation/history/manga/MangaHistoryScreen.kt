package eu.kanade.presentation.history.manga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.manga.components.MangaHistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryViewModel
import kotlinx.datetime.LocalDate
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
/**
 * UI presentation composable for MangaHistoryScreen.
 *
 * Renders the interface in Material 3 design, binding reactive state
 * and dispatching user interactions to parent handlers.
 */
fun MangaHistoryScreen(
    state: MangaHistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickFavorite: (mangaId: Long) -> Unit,
    onDialogChange: (MangaHistoryViewModel.Dialog?) -> Unit,
    onMarkRead: (MangaHistoryWithRelations, Boolean) -> Unit,
    searchQuery: String? = null,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                MangaHistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { item -> onDialogChange(MangaHistoryViewModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.mangaId) },
                    onMarkRead = onMarkRead,
                )
            }
        }
    }
}

@Composable
private fun MangaHistoryScreenContent(
    history: List<MangaHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (MangaHistoryWithRelations) -> Unit,
    onClickResume: (MangaHistoryWithRelations) -> Unit,
    onClickDelete: (MangaHistoryWithRelations) -> Unit,
    onClickFavorite: (MangaHistoryWithRelations) -> Unit,
    onMarkRead: (MangaHistoryWithRelations, Boolean) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        // Native "resume" row for the most recent entry
        history.firstOrNull { it is MangaHistoryUiModel.Item }
            ?.let { (it as MangaHistoryUiModel.Item).item }
            ?.let { latest ->
                item(key = "resume_last") {
                    Column {
                        ListGroupHeader(text = stringResource(MR.strings.action_resume))
                        MangaHistoryItem(
                            history = latest,
                            onClickCover = { onClickCover(latest) },
                            onClickResume = { onClickResume(latest) },
                            onClickDelete = { onClickDelete(latest) },
                            onClickFavorite = { onClickFavorite(latest) },
                            onMarkRead = { onMarkRead(latest, it) },
                        )
                    }
                }
            }
        items(
            items = history,
            key = {
                when (it) {
                    is MangaHistoryUiModel.Header -> "header_${it.date}"
                    is MangaHistoryUiModel.Item -> "item_${it.item.id}"
                }
            },
            contentType = {
                when (it) {
                    is MangaHistoryUiModel.Header -> "header"
                    is MangaHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is MangaHistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItem(),
                        text = relativeDateText(item.date),
                    )
                }

                is MangaHistoryUiModel.Item -> {
                    val value = item.item
                    MangaHistoryItem(
                        modifier = Modifier.animateItem(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                        onMarkRead = { onMarkRead(value, it) },
                    )
                }
            }
        }
    }
}

sealed interface MangaHistoryUiModel {
    data class Header(val date: LocalDate) : MangaHistoryUiModel
    data class Item(val item: MangaHistoryWithRelations) : MangaHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(MangaHistoryScreenModelStateProvider::class)
    historyState: MangaHistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        MangaHistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            searchQuery = null,
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
            onMarkRead = { _, _ -> },
        )
    }
}