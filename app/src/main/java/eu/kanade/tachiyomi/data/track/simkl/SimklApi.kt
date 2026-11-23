package eu.kanade.tachiyomi.data.track.simkl

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklAnimeResponse
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklOAuth
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSearchResult
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSyncResult
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklSyncWatched
import eu.kanade.tachiyomi.data.track.simkl.dto.SimklUser
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.ui.discover.DailyALCharacterRole
import eu.kanade.tachiyomi.ui.discover.DailyALFilterState
import eu.kanade.tachiyomi.ui.discover.DailyALMediaDetails
import eu.kanade.tachiyomi.ui.discover.DailyALProfileStats
import eu.kanade.tachiyomi.ui.discover.DailyALRecommendation
import eu.kanade.tachiyomi.ui.discover.DailyALRelation
import eu.kanade.tachiyomi.ui.discover.DailyALReview
import eu.kanade.tachiyomi.ui.discover.DailyALStaffRole
import eu.kanade.tachiyomi.ui.discover.DailyALVoiceActor
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import logcat.LogPriority
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack

class SimklApi(private val client: OkHttpClient, interceptor: SimklInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val simklRateLimiter = Mutex()
    private var lastSimklTimestamp = 0L

    private suspend fun throttleSimkl() {
        simklRateLimiter.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastSimklTimestamp
            if (elapsed < 350L) {
                delay(350L - elapsed)
            }
            lastSimklTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun executeSimklCall(url: String, requireAuth: Boolean = false): String {
        throttleSimkl()
        val request = GET(url, headers = headersOf("simkl-api-key", CLIENT_ID, "Accept", "application/json"))
        return if (requireAuth) {
            authClient.newCall(request).awaitSuccess().body.string()
        } else {
            try {
                authClient.newCall(request).awaitSuccess().body.string()
            } catch (_: Exception) {
                client.newCall(request).awaitSuccess().body.string()
            }
        }
    }

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            addToList(track, mediaType)

            track
        }
    }

    private suspend fun addToList(track: AnimeTrack, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("to", track.toSimklStatus())
                }
            }
        }.toString().toRequestBody(jsonMime)
        authClient.newCall(
            POST("$API_URL/sync/add-to-list", body = payload),
        ).awaitSuccess()
    }

    private suspend fun updateRating(track: AnimeTrack, mediaType: String) {
        val payload = buildJsonObject {
            putJsonArray(mediaType) {
                addJsonObject {
                    putJsonObject("ids") {
                        put("simkl", track.remote_id)
                    }
                    put("rating", track.score.toInt())
                }
            }
        }.toString().toRequestBody(jsonMime)

        if (track.score == 0.0) {
            authClient.newCall(
                POST("$API_URL/sync/ratings/remove", body = payload),
            ).awaitSuccess()
        } else {
            authClient.newCall(
                POST("$API_URL/sync/ratings", body = payload),
            ).awaitSuccess()
        }
    }

    private suspend fun updateProgress(track: AnimeTrack) {
        // first remove
        authClient.newCall(
            POST("$API_URL/sync/history/remove", body = buildProgressObject(track, false)),
        ).awaitSuccess()
        // then add again
        authClient.newCall(
            POST("$API_URL/sync/history", body = buildProgressObject(track, true)),
        ).awaitSuccess()
    }

    private fun buildProgressObject(track: AnimeTrack, add: Boolean = true) = buildJsonObject {
        putJsonArray("shows") {
            addJsonObject {
                putJsonObject("ids") {
                    put("simkl", track.remote_id)
                }
                putJsonArray("seasons") {
                    addJsonObject {
                        put("number", 1)
                        if (add) {
                            putJsonArray("episodes") {
                                for (epNum in 1..track.last_episode_seen.toInt()) {
                                    addJsonObject {
                                        put("number", epNum)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.toString().toRequestBody(jsonMime)

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            // determine media type
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val mediaType = if (type == "movies") "movies" else "shows"
            // update progress only for shows
            if (type != "movies") {
                updateProgress(track)
            }
            // add to correct list
            addToList(track, mediaType)
            // update rating
            updateRating(track, mediaType)

            track
        }
    }

    suspend fun searchAnime(search: String, type: String): List<AnimeTrackSearch> {
        return withIOContext {
            val searchUrl = "$API_URL/search/$type".toUri().buildUpon()
                .appendQueryParameter("q", search)
                .appendQueryParameter("extended", "full")
                .appendQueryParameter("client_id", CLIENT_ID)
                .build()
            with(json) {
                client.newCall(GET(searchUrl.toString()))
                    .awaitSuccess()
                    .parseAs<List<SimklSearchResult>>()
                    .map { it.toTrackSearch(type) }
            }
        }
    }

    suspend fun getAnimeDetails(id: Long): AnimeTrackSearch? {
        return withIOContext {
            val searchUrl = "$API_URL/search/id".toUri().buildUpon()
                .appendQueryParameter("simkl", id.toString())
                .appendQueryParameter("extended", "full")
                .appendQueryParameter("client_id", CLIENT_ID)
                .build()
            with(json) {
                try {
                    client.newCall(GET(searchUrl.toString()))
                        .awaitSuccess()
                        .parseAs<List<SimklSearchResult>>()
                        .firstOrNull()
                        ?.toTrackSearch("anime")
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Checks if the given [track] exists in the user's list and
     * returns all info about it or null if it isn't found.
     */
    suspend fun findLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val payload = buildJsonArray {
                addJsonObject {
                    put("simkl", track.remote_id)
                }
            }.toString().toRequestBody(jsonMime)
            val foundAnime = with(json) {
                authClient.newCall(
                    POST("$API_URL/sync/watched", body = payload),
                )
                    .awaitSuccess()
                    .parseAs<List<SimklSyncWatched>>()
                    .firstOrNull() ?: return@withIOContext null
            }

            if (foundAnime.result != true) return@withIOContext null
            val lastWatched = foundAnime.lastWatched ?: return@withIOContext null
            val status = foundAnime.list ?: return@withIOContext null
            val type = track.tracking_url
                .substringAfter("/")
                .substringBefore("/")
            val queryType = if (type == "tv") "shows" else type
            val url = "$API_URL/sync/all-items/$queryType/$status".toUri().buildUpon()
                .appendQueryParameter("date_from", lastWatched)
                .build()

            val typeName = if (type == "movies") "movie" else "show"
            val listAnime = with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<SimklSyncResult>()
                    .getFromType(queryType)
                    ?.firstOrNull { item ->
                        item.getFromType(typeName).ids.simkl == track.remote_id
                    } ?: return@withIOContext null
            }

            listAnime.toAnimeTrack(typeName, type, status)
        }
    }
    fun getCurrentUser(): Int {
        return runBlocking {
            with(json) {
                authClient.newCall(GET("$API_URL/users/settings"))
                    .awaitSuccess()
                    .parseAs<SimklUser>()
                    .account.id
            }
        }
    }
    suspend fun getSimklAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata {
        return withIOContext {
            val type = track.remoteUrl
                .substringAfter("/")
                .substringBefore("/")
            val url = "$API_URL/$type/${track.remoteId}?extended=full&client_id=$CLIENT_ID"

            with(json) {
                authClient.newCall(GET(url))
                    .awaitSuccess()
                    .parseAs<SimklAnimeResponse>()
                    .let {
                        TrackAnimeMetadata(
                            remoteId = it.id,
                            title = it.title,
                            thumbnailUrl = it.poster,
                            description = it.overview,
                            authors = if (type == "movies") it.director else it.network,
                            artists = it.network,
                        )
                    }
            }
        }
    }

    suspend fun accessToken(code: String): SimklOAuth {
        return withIOContext {
            with(json) {
                client.newCall(accessTokenRequest(code))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        OAUTH_URL,
        body = buildJsonObject {
            put("code", code)
            put("client_id", CLIENT_ID)
            put("client_secret", CLIENT_SECRET)
            put("redirect_uri", REDIRECT_URL)
            put("grant_type", "authorization_code")
        }.toString().toRequestBody(jsonMime),
    )

    /**
     * Retrieves comprehensive deep media details from Simkl ported from DailyAL's rich media graph.
     *
     * Queries Simkl anime endpoint requesting title translations, high-res posters, fanart banner,
     * episode count, ratings, and genre tags with client-side rate-limiting and defensive JSON parsing.
     *
     * @param mediaId Simkl media identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val url = "$API_URL/anime/$mediaId?extended=full&client_id=$CLIENT_ID"
                val responseBody = executeSimklCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val mediaObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in Simkl response for id=$mediaId")
                parseSimklDeepMediaDetails(mediaObj, mediaId)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching Simkl deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities on Simkl.
     *
     * Supports search queries by text keywords, seasonal browse, and trending rankings with
     * client-side rate-limiting and defensive JSON parsing.
     *
     * @param filter The active [DailyALFilterState] specifying search constraints.
     * @param page Page index for paginated browsing (1-based).
     * @return [Result] containing list of [DailyALMediaDetails], or error on failure.
     */
    suspend fun searchAdvanced(filter: DailyALFilterState, page: Int = 1): Result<List<DailyALMediaDetails>> {
        return withIOContext {
            runCatching {
                val url = if (filter.query.isNotBlank()) {
                    val encoded = Uri.encode(filter.query.trim())
                    "$API_URL/search/anime?q=$encoded&limit=25&page=$page&client_id=$CLIENT_ID&extended=full"
                } else {
                    val browseMode = when (filter.sortBy.uppercase()) {
                        "SCORE_DESC" -> "best?filter=all"
                        else -> "trending"
                    }
                    "$API_URL/anime/$browseMode?client_id=$CLIENT_ID&extended=full"
                }

                val responseBody = executeSimklCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val itemsArray = jsonElement.asJsonArrayOrNull ?: emptyList()

                itemsArray.mapNotNull { itemElem ->
                    val itemObj = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                    val id = itemObj["ids"].asJsonObjectOrNull?.get("simkl")?.jsonPrimitive?.longOrNull
                        ?: itemObj["id"]?.jsonPrimitive?.longOrNull
                        ?: return@mapNotNull null
                    parseSimklDeepMediaDetails(itemObj, id)
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing Simkl advanced search query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated Simkl user.
     *
     * Retrieves username, avatar, watch minutes, total episodes, and status counts
     * matching DailyAL's user view.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val settingsBody = executeSimklCall("$API_URL/users/settings", requireAuth = true)
                val settingsObj = json.parseToJsonElement(settingsBody).asJsonObjectOrNull
                    ?: throw IllegalStateException("Failed to retrieve Simkl user settings")
                val userObj = settingsObj["user"].asJsonObjectOrNull
                val userName = userObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Simkl User"
                val avatarUrl = userObj?.get("avatar")?.jsonPrimitive?.contentOrNull

                val statsBody = runCatching {
                    executeSimklCall("$API_URL/sync/stats?client_id=$CLIENT_ID", requireAuth = true)
                }.getOrNull()

                val statsObj = statsBody?.let { json.parseToJsonElement(it).asJsonObjectOrNull }
                val animeStats = statsObj?.get("anime").asJsonObjectOrNull

                val minutesWatched = animeStats?.get("total_mins")?.jsonPrimitive?.longOrNull ?: 0L
                val daysWatched = if (minutesWatched > 0) {
                    (minutesWatched / 1440.0 * 10.0).toLong() / 10.0
                } else {
                    0.0
                }
                val totalEpisodes = animeStats?.get("total_eps")?.jsonPrimitive?.intOrNull ?: 0

                val statusMap = mutableMapOf<String, Int>()
                animeStats?.get("watching")?.jsonPrimitive?.intOrNull?.let { statusMap["CURRENT"] = it }
                animeStats?.get("completed")?.jsonPrimitive?.intOrNull?.let { statusMap["COMPLETED"] = it }
                animeStats?.get("hold")?.jsonPrimitive?.intOrNull?.let { statusMap["PAUSED"] = it }
                animeStats?.get("dropped")?.jsonPrimitive?.intOrNull?.let { statusMap["DROPPED"] = it }
                animeStats?.get("plantowatch")?.jsonPrimitive?.intOrNull?.let { statusMap["PLANNING"] = it }

                val totalEntries = statusMap.values.sum()

                DailyALProfileStats(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    bannerUrl = null,
                    animeDaysWatched = daysWatched,
                    meanScore = 0.0,
                    totalEpisodes = totalEpisodes,
                    totalEntries = totalEntries,
                    statusDistribution = statusMap,
                    scoreDistribution = emptyMap(),
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from Simkl" }
            }
        }
    }

    private fun parseSimklDeepMediaDetails(media: JsonObject, requestedId: Long): DailyALMediaDetails {
        val idsObj = media["ids"].asJsonObjectOrNull
        val id = idsObj?.get("simkl")?.jsonPrimitive?.longOrNull
            ?: media["id"]?.jsonPrimitive?.longOrNull
            ?: requestedId

        val title = media["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val enTitle = media["en_title"]?.jsonPrimitive?.contentOrNull
        val jaTitle = media["ja_title"]?.jsonPrimitive?.contentOrNull

        val posterId = media["poster"]?.jsonPrimitive?.contentOrNull
        val coverUrl = if (!posterId.isNullOrBlank()) "https://simkl.in/posters/${posterId}_m.jpg" else null

        val fanartId = media["fanart"]?.jsonPrimitive?.contentOrNull
        val bannerUrl = if (!fanartId.isNullOrBlank()) "https://simkl.in/fanart/${fanartId}_medium.jpg" else coverUrl

        val format = media["anime_type"]?.jsonPrimitive?.contentOrNull?.uppercase()
        val episodes = media["episodes_count"]?.jsonPrimitive?.intOrNull
        val duration = media["runtime"]?.jsonPrimitive?.intOrNull

        val rawStatus = media["status"]?.jsonPrimitive?.contentOrNull
        val status = when (rawStatus?.lowercase()) {
            "airing" -> "RELEASING"
            "ended" -> "FINISHED"
            else -> rawStatus?.uppercase()
        }

        val seasonYear = media["year"]?.jsonPrimitive?.intOrNull
        val season = media["season"]?.jsonPrimitive?.contentOrNull?.uppercase()

        val ratingsObj = media["ratings"].asJsonObjectOrNull?.get("simkl").asJsonObjectOrNull
        val meanScore = ratingsObj?.get("rating")?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
        val rank = ratingsObj?.get("rank")?.jsonPrimitive?.intOrNull
        val popularity = ratingsObj?.get("votes")?.jsonPrimitive?.intOrNull

        val synopsis = media["overview"]?.jsonPrimitive?.contentOrNull?.htmlDecode()

        val genres = media["genres"].asJsonArrayOrNull?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val trailers = media["trailers"].asJsonArrayOrNull
        val ytTrailer = trailers?.firstOrNull()?.asJsonObjectOrNull?.get("youtube")?.jsonPrimitive?.contentOrNull
        val trailerUrl = if (!ytTrailer.isNullOrBlank()) "https://www.youtube.com/watch?v=$ytTrailer" else null

        return DailyALMediaDetails(
            id = id,
            title = title,
            romajiTitle = enTitle,
            nativeTitle = jaTitle,
            coverUrl = coverUrl,
            bannerUrl = bannerUrl,
            format = format,
            episodes = episodes,
            duration = duration,
            status = status,
            season = season,
            seasonYear = seasonYear,
            meanScore = meanScore,
            popularity = popularity,
            rank = rank,
            synopsis = synopsis,
            genres = genres,
            tags = emptyList(),
            trailerUrl = trailerUrl,
            characters = emptyList(),
            staff = emptyList(),
            relations = emptyList(),
            recommendations = emptyList(),
            reviews = emptyList(),
        )
    }

    companion object {
        const val CLIENT_ID = "125476f936eaf8029970919c3c77b63b9ecb1aa5d34f55c9255602e52042f5c2"
        private const val CLIENT_SECRET = "2f8200f7ec4c3743bd9efbb7f004a53d0d2f8551d7668750e37bbb88b8c4069c"

        private const val BASE_URL = "https://simkl.com"
        private const val API_URL = "https://api.simkl.com"
        private const val OAUTH_URL = "$API_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"
        const val POSTERS_URL = "https://simkl.in/posters/"

        private const val REDIRECT_URL = "animetail://simkl-auth"

        fun authUrl(): Uri =
            LOGIN_URL.toUri().buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .build()
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

