package eu.kanade.tachiyomi.ui.stats.anime

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.stats.AnimeStatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.animeStatsTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = androidx.compose.ui.platform.LocalContext.current

    val viewModel = metroViewModel<AnimeStatsViewModel>()
    val state by viewModel.state.collectAsState()

    if (state is StatsScreenState.Loading) {
        LoadingScreen()
    }

    return TabContent(
        titleRes = AYMR.strings.label_anime,
        actions = kotlinx.collections.immutable.persistentListOf(
            eu.kanade.presentation.components.AppBar.Action(
                title = "Export CSV",
                icon = androidx.compose.material.icons.Icons.Default.Share,
                onClick = { screenModel.exportToCsv(context) },
            ),
        ),
        content = { contentPadding, _ ->

            if (state is StatsScreenState.Loading) {
                LoadingScreen()
            } else {
                AnimeStatsScreenContent(
                    state = state as StatsScreenState.SuccessAnime,
                    paddingValues = contentPadding,
                )
            }
        },
        navigateUp = navigator::pop,
    )
}
