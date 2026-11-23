package eu.kanade.presentation.history.anime

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
import eu.kanade.presentation.history.anime.components.AnimeHistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryViewModel
import kotlinx.datetime.LocalDate
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
/**
 * UI presentation composable for AnimeHistoryScreen.
 *
 * Renders the interface in Material 3 design, binding reactive state
 * and dispatching user interactions to parent handlers.
 */
fun AnimeHistoryScreen(
    state: AnimeHistoryViewModel.State,
    snackbarHostState: SnackbarHostState,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onClickFavorite: (animeId: Long) -> Unit,
    onDialogChange: (AnimeHistoryViewModel.Dialog?) -> Unit,
    onMarkSeen: (AnimeHistoryWithRelations, Boolean) -> Unit,
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
                    AYMR.strings.information_no_recent_anime
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                AnimeHistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.animeId) },
                    onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                    onClickDelete = { item -> onDialogChange(AnimeHistoryViewModel.Dialog.Delete(item)) },
                    onClickFavorite = { history -> onClickFavorite(history.animeId) },
                    onMarkSeen = onMarkSeen,
                )
            }
        }
    }
}

@Composable
private fun AnimeHistoryScreenContent(
    history: List<AnimeHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (AnimeHistoryWithRelations) -> Unit,
    onClickResume: (AnimeHistoryWithRelations) -> Unit,
    onClickDelete: (AnimeHistoryWithRelations) -> Unit,
    onClickFavorite: (AnimeHistoryWithRelations) -> Unit,
    onMarkSeen: (AnimeHistoryWithRelations, Boolean) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        // Native "resume" row for the most recent entry
        history.firstOrNull { it is AnimeHistoryUiModel.Item }
            ?.let { (it as AnimeHistoryUiModel.Item).item }
            ?.let { latest ->
                item(key = "resume_last") {
                    Column {
                        ListGroupHeader(text = stringResource(MR.strings.action_resume))
                        AnimeHistoryItem(
                            history = latest,
                            onClickCover = { onClickCover(latest) },
                            onClickResume = { onClickResume(latest) },
                            onClickDelete = { onClickDelete(latest) },
                            onClickFavorite = { onClickFavorite(latest) },
                            onMarkSeen = { onMarkSeen(latest, it) },
                        )
                    }
                }
            }
        items(
            items = history,
            key = {
                when (it) {
                    is AnimeHistoryUiModel.Header -> "header_${it.date}"
                    is AnimeHistoryUiModel.Item -> "item_${it.item.id}"
                }
            },
            contentType = {
                when (it) {
                    is AnimeHistoryUiModel.Header -> "header"
                    is AnimeHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is AnimeHistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItem(),
                        text = relativeDateText(item.date),
                    )
                }

                is AnimeHistoryUiModel.Item -> {
                    val value = item.item
                    AnimeHistoryItem(
                        modifier = Modifier.animateItem(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                        onMarkSeen = { onMarkSeen(value, it) },
                    )
                }
            }
        }
    }
}

sealed interface AnimeHistoryUiModel {
    data class Header(val date: LocalDate) : AnimeHistoryUiModel
    data class Item(val item: AnimeHistoryWithRelations) : AnimeHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(AnimeHistoryViewModelStateProvider::class)
    historyState: AnimeHistoryViewModel.State,
) {
    TachiyomiPreviewTheme {
        AnimeHistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            searchQuery = null,
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
            onMarkSeen = { _, _ -> },
        )
    }
}