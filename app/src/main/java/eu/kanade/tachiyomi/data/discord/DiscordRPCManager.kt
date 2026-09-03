package eu.kanade.tachiyomi.data.discord

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

data class DiscordPresenceState(
    val isBroadcasting: Boolean = false,
    val mediaTitle: String = "",
    val subText: String = "", // e.g. "Episode 12" or "Chapter 45"
    val coverUrl: String? = null,
    val mediaType: DiscordMediaType = DiscordMediaType.ANIME,
    val startTimeStampMillis: Long = System.currentTimeMillis(),
)

enum class DiscordMediaType {
    ANIME,
    MANGA,
    LIGHT_NOVEL,
}

@Inject
@SingleIn(AppScope::class)
class DiscordRPCManager {

    private val _presenceState = MutableStateFlow(DiscordPresenceState())
    val presenceState: StateFlow<DiscordPresenceState> = _presenceState.asStateFlow()

    fun updateWatchingPresence(
        animeTitle: String,
        episodeName: String,
        coverUrl: String? = null,
    ) {
        logcat(LogPriority.INFO) { "Discord RPC: Now watching $animeTitle - $episodeName" }
        _presenceState.value = DiscordPresenceState(
            isBroadcasting = true,
            mediaTitle = animeTitle,
            subText = episodeName,
            coverUrl = coverUrl,
            mediaType = DiscordMediaType.ANIME,
            startTimeStampMillis = System.currentTimeMillis(),
        )
    }

    fun updateReadingPresence(
        mangaTitle: String,
        chapterName: String,
        coverUrl: String? = null,
        isNovel: Boolean = false,
    ) {
        logcat(LogPriority.INFO) { "Discord RPC: Now reading $mangaTitle - $chapterName" }
        _presenceState.value = DiscordPresenceState(
            isBroadcasting = true,
            mediaTitle = mangaTitle,
            subText = chapterName,
            coverUrl = coverUrl,
            mediaType = if (isNovel) DiscordMediaType.LIGHT_NOVEL else DiscordMediaType.MANGA,
            startTimeStampMillis = System.currentTimeMillis(),
        )
    }

    fun clearPresence() {
        logcat(LogPriority.INFO) { "Discord RPC: Cleared activity status" }
        _presenceState.value = DiscordPresenceState(isBroadcasting = false)
    }
}
