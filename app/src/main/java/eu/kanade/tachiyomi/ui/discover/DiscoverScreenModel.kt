package eu.kanade.tachiyomi.ui.discover

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DiscoverScreenModel(
    private val trackerManager: TrackerManager = Injekt.get()
) : ScreenModel {

    private val _state = MutableStateFlow<DiscoverState>(DiscoverState.Loading)
    val state: StateFlow<DiscoverState> = _state.asStateFlow()

    init {
        loadDiscover()
    }

    fun loadDiscover() {
        screenModelScope.launch {
            _state.update { DiscoverState.Loading }
            val anilist = trackerManager.aniList
            try {
                val trendingAnime = anilist.getTrendingAnime(1)
                val trendingManga = anilist.getTrendingManga(1)
                _state.update { DiscoverState.Success(trendingAnime, trendingManga) }
            } catch (e: Exception) {
                _state.update { DiscoverState.Error(e.message ?: "Unknown error") }
            }
        }
    }
}

sealed interface DiscoverState {
    data object Loading : DiscoverState
    data class Success(
        val trendingAnime: List<AnimeTrackSearch>,
        val trendingManga: List<MangaTrackSearch>
    ) : DiscoverState
    data class Error(val message: String) : DiscoverState
}
