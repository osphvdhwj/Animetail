package eu.kanade.tachiyomi.ui.stats.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class TrackerAccountInfo(
    val tracker: BaseTracker,
    val username: String,
    val trackerName: String,
)

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
            val loggedInTrackers = trackerManager.loggedInTrackers()

            if (loggedInTrackers.isEmpty()) {
                _state.update { ProfileState.NotLoggedIn }
                return@launch
            }

            val anilist = trackerManager.aniList
            var anilistStats: ALUserStats? = null

            if (anilist.isLoggedIn) {
                try {
                    anilistStats = anilist.getProfileStats()
                } catch (_: Exception) {}
            }

            val connectedAccounts = loggedInTrackers.map { tracker ->
                TrackerAccountInfo(
                    tracker = tracker,
                    username = tracker.getDisplayUsername(),
                    trackerName = tracker.name
                )
            }

            _state.update {
                ProfileState.Success(
                    anilistStats = anilistStats,
                    connectedAccounts = connectedAccounts
                )
            }
        }
    }
}

sealed interface ProfileState {
    data object Loading : ProfileState
    data object NotLoggedIn : ProfileState
    data class Success(
        val anilistStats: ALUserStats?,
        val connectedAccounts: List<TrackerAccountInfo>
    ) : ProfileState
    data class Error(val message: String) : ProfileState
}
