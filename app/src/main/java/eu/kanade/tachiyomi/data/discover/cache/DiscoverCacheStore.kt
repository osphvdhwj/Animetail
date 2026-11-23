package eu.kanade.tachiyomi.data.discover.cache

import android.content.Context
import android.text.format.Formatter
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAnime
import eu.kanade.tachiyomi.data.track.anilist.dto.ALStudios
import eu.kanade.tachiyomi.data.track.anilist.dto.ALStudiosEdge
import eu.kanade.tachiyomi.data.track.anilist.dto.ALStudiosNode
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.ui.discover.DailyALMediaDetails
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
import java.security.MessageDigest

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

@Serializable
data class DeepMediaDetailsCacheData(
    val timestamp: Long = System.currentTimeMillis(),
    val isReleasing: Boolean,
    val details: DailyALMediaDetails,
)

@Serializable
data class AdvancedFilterCacheData(
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CachedAdvancedFilterAnime> = emptyList(),
)

@Serializable
data class CachedAdvancedFilterAnime(
    val remoteId: Long,
    val title: String,
    val imageUrl: String,
    val description: String? = null,
    val format: String = "",
    val publishingStatus: String = "",
    val startDateFuzzy: Long = 0L,
    val totalEpisodes: Long = 0L,
    val averageScore: Int = -1,
    val studioNames: List<String> = emptyList(),
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

    private fun getDeepMediaFile(mediaId: Long): File {
        return File(cacheDir, "deep_media_$mediaId.json")
    }

    private fun getFilterFile(filterKey: String): File {
        val md5 = MessageDigest.getInstance("MD5")
            .digest(filterKey.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "filter_$md5.json")
    }

    /** Total bytes occupied by the Discover cache files. */
    fun sizeBytes(): Long {
        return cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    /** Human-readable total cache size (e.g. "1.2 MB"). */
    fun readableSize(): String {
        return Formatter.formatShortFileSize(context, sizeBytes())
    }

    /** Deletes every Discover cache file, returning how many were removed. */
    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        cacheDir.listFiles().orEmpty().count { it.delete() }
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

    suspend fun loadAiringCache(maxAgeMs: Long? = null): List<AiringEpisode>? = withContext(Dispatchers.IO) {
        try {
            val file = getAiringFile()
            if (file.exists()) {
                val data = json.decodeFromString<AiringScheduleCacheData>(file.readText())
                if (maxAgeMs != null && System.currentTimeMillis() - data.timestamp > maxAgeMs) {
                    return@withContext null
                }
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
                            } catch (_: Throwable) {}
                        }
                    }

                    if (tracker.supportsManga) {
                        val mangaService = trackerManager.get(tracker.id)?.mangaService
                        if (mangaService != null) {
                            try {
                                val res = mangaService.searchManga("")
                                trendingManga = res.take(15).map { it.toCached() }
                                popularManga = res.drop(15).take(15).map { it.toCached() }
                            } catch (_: Throwable) {}
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
                } catch (_: Throwable) {}
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

    /**
     * Loads cached deep media details for the given [mediaId] if still within its valid TTL.
     *
     * Applies a 24-hour TTL for completed series and a 3-hour TTL for currently airing series.
     * Expired cache entries are automatically purged.
     *
     * @param mediaId AniList media identifier.
     * @return Cached [DailyALMediaDetails] if valid, or null if expired or not found.
     */
    suspend fun getDeepMediaDetails(mediaId: Long): DailyALMediaDetails? = withContext(Dispatchers.IO) {
        try {
            val file = getDeepMediaFile(mediaId)
            if (file.exists()) {
                val data = json.decodeFromString<DeepMediaDetailsCacheData>(file.readText())
                val ttl = if (data.isReleasing) 3 * 3600 * 1000L else 24 * 3600 * 1000L
                if (System.currentTimeMillis() - data.timestamp < ttl) {
                    data.details
                } else {
                    file.delete()
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Persists deep media details to disk cache with the appropriate TTL.
     *
     * @param mediaId AniList media identifier.
     * @param details Complete media details structure.
     * @param isReleasing Whether the series is currently broadcasting/publishing.
     */
    suspend fun saveDeepMediaDetails(mediaId: Long, details: DailyALMediaDetails, isReleasing: Boolean) = withContext(Dispatchers.IO) {
        try {
            val file = getDeepMediaFile(mediaId)
            val data = DeepMediaDetailsCacheData(
                timestamp = System.currentTimeMillis(),
                isReleasing = isReleasing,
                details = details,
            )
            file.writeText(json.encodeToString(data))
        } catch (_: Exception) {
            // Ignored
        }
    }

    /**
     * Loads cached advanced search filter results corresponding to [filterKey].
     *
     * Implements a 3-hour TTL for filter search results.
     *
     * @param filterKey Unique cache key representing the filter parameters.
     * @return Cached list of [ALAnime], or null if expired or missing.
     */
    suspend fun getAdvancedFilterCache(filterKey: String): List<ALAnime>? = withContext(Dispatchers.IO) {
        try {
            val file = getFilterFile(filterKey)
            if (file.exists()) {
                val data = json.decodeFromString<AdvancedFilterCacheData>(file.readText())
                if (System.currentTimeMillis() - data.timestamp < 3 * 3600 * 1000L) {
                    data.items.map { it.toALAnime() }
                } else {
                    file.delete()
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Persists advanced filter search results to disk cache for fast repeat access.
     *
     * @param filterKey Unique cache key representing the filter parameters.
     * @param items List of [ALAnime] results to store.
     */
    suspend fun saveAdvancedFilterCache(filterKey: String, items: List<ALAnime>) = withContext(Dispatchers.IO) {
        try {
            val file = getFilterFile(filterKey)
            val cachedItems = items.map { it.toCached() }
            val data = AdvancedFilterCacheData(
                timestamp = System.currentTimeMillis(),
                items = cachedItems,
            )
            file.writeText(json.encodeToString(data))
        } catch (_: Exception) {
            // Ignored
        }
    }

    private fun ALAnime.toCached() = CachedAdvancedFilterAnime(
        remoteId = remoteId,
        title = title,
        imageUrl = imageUrl,
        description = description,
        format = format,
        publishingStatus = publishingStatus,
        startDateFuzzy = startDateFuzzy,
        totalEpisodes = totalEpisodes,
        averageScore = averageScore,
        studioNames = studios.edges.map { it.node.name },
    )

    private fun CachedAdvancedFilterAnime.toALAnime() = ALAnime(
        remoteId = remoteId,
        title = title,
        imageUrl = imageUrl,
        description = description,
        format = format,
        publishingStatus = publishingStatus,
        startDateFuzzy = startDateFuzzy,
        totalEpisodes = totalEpisodes,
        averageScore = averageScore,
        studios = ALStudios(
            edges = studioNames.map {
                ALStudiosEdge(
                    isMain = true,
                    node = ALStudiosNode(name = it),
                )
            },
        ),
    )
}
