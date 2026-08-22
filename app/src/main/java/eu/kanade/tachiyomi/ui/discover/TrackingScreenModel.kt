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

class TrackingScreenModel(
    private val trackerManager: TrackerManager = Injekt.get()
) : ScreenModel {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    init {
        loadTracking()
    }

    fun loadTracking() {
        screenModelScope.launch {
            _state.update { TrackingState.Loading }
            val anilist = trackerManager.aniList
            try {
                val trendingAnime = anilist.getTrendingAnime(1)
                val trendingManga = anilist.getTrendingManga(1)
                _state.update { TrackingState.Success(trendingAnime, trendingManga) }
            } catch (e: Exception) {
                _state.update { TrackingState.Error(e.message ?: "Unknown error") }
            }
        }
    }
}

sealed interface TrackingState {
    data object Loading : TrackingState
    data class Success(
        val trendingAnime: List<AnimeTrackSearch>,
        val trendingManga: List<MangaTrackSearch>
    ) : TrackingState
    data class Error(val message: String) : TrackingState
}

