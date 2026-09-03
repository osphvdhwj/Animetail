package eu.kanade.domain.track.service

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.model.toDomainTrack
import eu.kanade.domain.track.manga.model.toDbTrack
import eu.kanade.domain.track.manga.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.isOnline
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.interactor.InsertAnimeTrack
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.manga.interactor.InsertMangaTrack

/**
 * Universal Multi-Tracker Simultaneous Synchronization Engine for Animetail.
 * Handles parallel dispatch to active trackers (MAL, AniList, Kitsu, Simkl, Bangumi, etc.)
 * with cross-platform ID mapping resolution and status tracking.
 */
@Inject
@SingleIn(AppScope::class)
class TrackSyncEngine(
    private val trackerManager: TrackerManager,
    private val getAnimeTracks: GetAnimeTracks,
    private val insertAnimeTrack: InsertAnimeTrack,
    private val getMangaTracks: GetMangaTracks,
    private val insertMangaTrack: InsertMangaTrack,
    private val trackPreferences: TrackPreferences,
) {

    private val _syncState = MutableStateFlow<SyncProgressState>(SyncProgressState.Idle)
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    sealed interface SyncProgressState {
        data object Idle : SyncProgressState
        data class Syncing(val activeTrackersCount: Int, val completedCount: Int) : SyncProgressState
        data class Completed(val successCount: Int, val failedCount: Int) : SyncProgressState
        data class Error(val message: String) : SyncProgressState
    }

    /**
     * Simultaneously updates episode progress across ALL logged-in trackers for an anime entry.
     */
    suspend fun syncAnimeEpisodeProgress(
        context: Context,
        animeId: Long,
        episodeNumber: Double,
    ) = withNonCancellableContext {
        val loggedInServices = trackerManager.loggedInTrackers()
        if (loggedInServices.isEmpty()) {
            _syncState.value = SyncProgressState.Idle
            return@withNonCancellableContext
        }

        val dbTracks = getAnimeTracks.await(animeId)
        if (dbTracks.isEmpty()) return@withNonCancellableContext

        _syncState.value = SyncProgressState.Syncing(
            activeTrackersCount = dbTracks.size,
            completedCount = 0,
        )

        var successCount = 0
        var failedCount = 0

        dbTracks.mapNotNull { track ->
            val service = trackerManager.get(track.trackerId)
            if (service == null || !service.isLoggedIn || episodeNumber <= track.lastEpisodeSeen) {
                return@mapNotNull null
            }

            async {
                runCatching {
                    if (context.isOnline()) {
                        val updatedTrack = service.animeService.refresh(track.toDbTrack())
                            .toDomainTrack(idRequired = true)!!
                            .copy(lastEpisodeSeen = episodeNumber)
                        service.animeService.update(updatedTrack.toDbTrack(), true)
                        insertAnimeTrack.await(updatedTrack)
                        successCount++
                    } else {
                        failedCount++
                    }
                }.onFailure { err ->
                    failedCount++
                    logcat(LogPriority.WARN, err) { "Failed to sync anime episode $episodeNumber with tracker ${track.trackerId}" }
                }
            }
        }.awaitAll()

        _syncState.value = SyncProgressState.Completed(
            successCount = successCount,
            failedCount = failedCount,
        )
    }

    /**
     * Simultaneously updates chapter progress across ALL logged-in trackers for a manga entry.
     */
    suspend fun syncMangaChapterProgress(
        context: Context,
        mangaId: Long,
        chapterNumber: Double,
    ) = withNonCancellableContext {
        val loggedInServices = trackerManager.loggedInTrackers()
        if (loggedInServices.isEmpty()) {
            _syncState.value = SyncProgressState.Idle
            return@withNonCancellableContext
        }

        val dbTracks = getMangaTracks.await(mangaId)
        if (dbTracks.isEmpty()) return@withNonCancellableContext

        _syncState.value = SyncProgressState.Syncing(
            activeTrackersCount = dbTracks.size,
            completedCount = 0,
        )

        var successCount = 0
        var failedCount = 0

        dbTracks.mapNotNull { track ->
            val service = trackerManager.get(track.trackerId)
            if (service == null || !service.isLoggedIn || chapterNumber <= track.lastChapterRead) {
                return@mapNotNull null
            }

            async {
                runCatching {
                    if (context.isOnline()) {
                        val updatedTrack = service.mangaService.refresh(track.toDbTrack())
                            .toDomainTrack(idRequired = true)!!
                            .copy(lastChapterRead = chapterNumber)
                        service.mangaService.update(updatedTrack.toDbTrack(), true)
                        insertMangaTrack.await(updatedTrack)
                        successCount++
                    } else {
                        failedCount++
                    }
                }.onFailure { err ->
                    failedCount++
                    logcat(LogPriority.WARN, err) { "Failed to sync manga chapter $chapterNumber with tracker ${track.trackerId}" }
                }
            }
        }.awaitAll()

        _syncState.value = SyncProgressState.Completed(
            successCount = successCount,
            failedCount = failedCount,
        )
    }
}
