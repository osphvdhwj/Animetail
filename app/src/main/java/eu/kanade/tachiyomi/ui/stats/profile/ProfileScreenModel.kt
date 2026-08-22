package eu.kanade.tachiyomi.ui.stats.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ProfileScreenModel(
    private val trackerManager: TrackerManager = Injekt.get()
) : ScreenModel {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        screenModelScope.launch {
            _state.update { ProfileState.Loading }
            val anilist = trackerManager.aniList
            if (anilist.isLoggedIn) {
                try {
                    val stats = anilist.getProfileStats()
                    _state.update { ProfileState.SuccessAnilist(stats) }
                } catch (e: Exception) {
                    _state.update { ProfileState.Error(e.message ?: "Unknown error") }
                }
            } else {
                _state.update { ProfileState.NotLoggedIn }
            }
        }
    }
}

sealed interface ProfileState {
    data object Loading : ProfileState
    data object NotLoggedIn : ProfileState
    data class SuccessAnilist(val stats: ALUserStats) : ProfileState
    data class Error(val message: String) : ProfileState
}
