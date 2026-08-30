package eu.kanade.tachiyomi.ui.library

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.library.components.LibraryMediaSwitcher
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryViewModel
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryViewModel
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

enum class LibraryMediaType {
    ANIME,
    MANGA,
}

data object LibraryTab : Tab {
    private fun readResolve(): Any = LibraryTab

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(MR.strings.label_library)
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(
                R.drawable.anim_animelibrary_leave,
            )
            return TabOptions(
                index = 0u,
                title = title,
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        AnimeLibraryTab.onReselect(navigator)
    }

    @Composable
    override fun isEnabled(): Boolean = true

    @Composable
    override fun Content() {
        var selectedMediaType by rememberSaveable { mutableStateOf(LibraryMediaType.ANIME) }

        val animeViewModel = metroViewModel<AnimeLibraryViewModel>()
        val animeState by animeViewModel.state.collectAsStateWithLifecycle()
        val defaultAnimeTitle = stringResource(AYMR.strings.label_anime_library)
        val animeToolbarTitle = animeState.getToolbarTitle(
            defaultTitle = defaultAnimeTitle,
            defaultCategoryTitle = stringResource(MR.strings.label_default),
            page = animeViewModel.activeCategoryIndex,
        )

        val mangaViewModel = metroViewModel<MangaLibraryViewModel>()
        val mangaState by mangaViewModel.state.collectAsStateWithLifecycle()
        val defaultMangaTitle = stringResource(AYMR.strings.label_manga_library)
        val mangaToolbarTitle = mangaState.getToolbarTitle(
            defaultTitle = defaultMangaTitle,
            defaultCategoryTitle = stringResource(MR.strings.label_default),
            page = mangaViewModel.activeCategoryIndex,
        )

        val isAnime = selectedMediaType == LibraryMediaType.ANIME

        val mediaSwitcher: @Composable () -> Unit = {
            LibraryMediaSwitcher(
                isAnime = isAnime,
                onSelectAnime = { selectedMediaType = LibraryMediaType.ANIME },
                onSelectManga = { selectedMediaType = LibraryMediaType.MANGA },
                animeCount = animeToolbarTitle.numberOfEntries,
                mangaCount = mangaToolbarTitle.numberOfEntries,
            )
        }

        if (isAnime) {
            AnimeLibraryTab.AnimeLibraryScreen(mediaSwitcher = mediaSwitcher)
        } else {
            MangaLibraryTab.MangaLibraryScreen(mediaSwitcher = mediaSwitcher)
        }
    }

    suspend fun search(query: String) {
        AnimeLibraryTab.search(query)
        MangaLibraryTab.search(query)
    }
}
