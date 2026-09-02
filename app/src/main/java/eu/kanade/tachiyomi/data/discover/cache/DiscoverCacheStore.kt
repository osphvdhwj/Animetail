package eu.kanade.tachiyomi.data.discover.cache

import android.content.Context
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.discover.TrackerSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

@Serializable
data class TrackerCacheData(
    val trackerId: Long,
    val trackerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val trendingAnime: List<CachedAnimeEntry> = emptyList(),
    val popularAnime: List<CachedAnimeEntry> = emptyList(),
    val trendingManga: List<CachedMangaEntry> = emptyList(),
    val popularManga: List<CachedMangaEntry> = emptyList(),
)

@Serializable
data class CachedAnimeEntry(
    val media_id: Long,
    val title: String,
    val cover_url: String,
    val score: Double = -1.0,
    val publishing_type: String = "",
    val publishing_status: String = "",
    val summary: String = "",
    val total_episodes: Long = 0L,
    val tracking_url: String = "",
)

@Serializable
data class CachedMangaEntry(
    val media_id: Long,
    val title: String,
    val cover_url: String,
    val score: Double = -1.0,
    val publishing_type: String = "",
    val publishing_status: String = "",
    val summary: String = "",
    val total_chapters: Long = 0L,
    val tracking_url: String = "",
)

@Serializable
data class AiringScheduleCacheData(
    val timestamp: Long = System.currentTimeMillis(),
    val episodes: List<CachedAiringEpisode> = emptyList(),
)

@Serializable
data class CachedAiringEpisode(
    val id: Long,
    val mediaId: Long,
    val title: String,
    val coverUrl: String,
    val episode: Int,
    val airingAt: Long,
    val format: String = "TV",
    val score: Double = -1.0,
    val summary: String = "",
)

class DiscoverCacheStore(
    private val context: Context,
    private val trackerManager: TrackerManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val cacheDir = File(context.filesDir, "discover_cache").apply {
        if (!exists()) mkdirs()
    }

    private fun getTrackerFile(tracker: TrackerSource): File {
        return File(cacheDir, "tracker_${tracker.name.lowercase()}.json")
    }

    private fun getAiringFile(): File {
        return File(cacheDir, "airing_schedule.json")
    }

    suspend fun loadTrackerCache(tracker: TrackerSource): TrackerCacheData? = withContext(Dispatchers.IO) {
        try {
            val file = getTrackerFile(tracker)
            if (file.exists()) {
                json.decodeFromString<TrackerCacheData>(file.readText())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveTrackerCache(data: TrackerCacheData) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "tracker_${data.trackerName.lowercase()}.json")
            file.writeText(json.encodeToString(data))
        } catch (_: Exception) {
            // Ignored
        }
    }

    suspend fun loadAiringCache(): List<AiringEpisode>? = withContext(Dispatchers.IO) {
        try {
            val file = getAiringFile()
            if (file.exists()) {
                val data = json.decodeFromString<AiringScheduleCacheData>(file.readText())
                data.episodes.map {
                    AiringEpisode(
                        id = it.id,
                        mediaId = it.mediaId,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        episode = it.episode,
                        airingAt = it.airingAt,
                        format = it.format,
                        score = it.score,
                        summary = it.summary,
                    )
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveAiringCache(episodes: List<AiringEpisode>) = withContext(Dispatchers.IO) {
        try {
            val cached = episodes.map {
                CachedAiringEpisode(
                    id = it.id,
                    mediaId = it.mediaId,
                    title = it.title,
                    coverUrl = it.coverUrl,
                    episode = it.episode,
                    airingAt = it.airingAt,
                    format = it.format,
                    score = it.score,
                    summary = it.summary,
                )
            }
            getAiringFile().writeText(json.encodeToString(AiringScheduleCacheData(episodes = cached)))
        } catch (_: Exception) {
            // Ignored
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
            // Ignored
        }
    }

    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun preloadAllTrackers(force: Boolean = false): Unit = coroutineScope {
        TrackerSource.entries.map { tracker ->
            async(Dispatchers.IO) {
                try {
                    val existing = if (!force) loadTrackerCache(tracker) else null
                    if (existing != null && System.currentTimeMillis() - existing.timestamp < 24 * 3600 * 1000L) {
                        return@async
                    }

                    var trendingAnime = emptyList<CachedAnimeEntry>()
                    var popularAnime = emptyList<CachedAnimeEntry>()
                    var trendingManga = emptyList<CachedMangaEntry>()
                    var popularManga = emptyList<CachedMangaEntry>()

                    if (tracker.supportsAnime) {
                        val animeService = trackerManager.get(tracker.id)?.animeService
                        if (animeService != null) {
                            try {
                                val res = animeService.searchAnime("")
                                trendingAnime = res.take(15).map { it.toCached() }
                                popularAnime = res.drop(15).take(15).map { it.toCached() }
                            } catch (_: Exception) {}
                        }
                    }

                    if (tracker.supportsManga) {
                        val mangaService = trackerManager.get(tracker.id)?.mangaService
                        if (mangaService != null) {
                            try {
                                val res = mangaService.searchManga("")
                                trendingManga = res.take(15).map { it.toCached() }
                                popularManga = res.drop(15).take(15).map { it.toCached() }
                            } catch (_: Exception) {}
                        }
                    }

                    if (trendingAnime.isNotEmpty() || trendingManga.isNotEmpty()) {
                        saveTrackerCache(
                            TrackerCacheData(
                                trackerId = tracker.id,
                                trackerName = tracker.name,
                                timestamp = System.currentTimeMillis(),
                                trendingAnime = trendingAnime,
                                popularAnime = popularAnime,
                                trendingManga = trendingManga,
                                popularManga = popularManga,
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()
    }

    private fun AnimeTrackSearch.toCached() = CachedAnimeEntry(
        media_id = remote_id,
        title = title,
        cover_url = cover_url,
        score = score,
        publishing_type = publishing_type,
        publishing_status = publishing_status,
        summary = summary,
        total_episodes = total_episodes,
        tracking_url = tracking_url,
    )

    private fun MangaTrackSearch.toCached() = CachedMangaEntry(
        media_id = remote_id,
        title = title,
        cover_url = cover_url,
        score = score,
        publishing_type = publishing_type,
        publishing_status = publishing_status,
        summary = summary,
        total_chapters = total_chapters,
        tracking_url = tracking_url,
    )
}
