package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMCollectionResponse
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMSearchResult
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMSubject
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMUser
import eu.kanade.tachiyomi.data.track.bangumi.dto.Infobox
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
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
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class BangumiApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: BangumiInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val bangumiRateLimiter = Mutex()
    private var lastBangumiTimestamp = 0L

    private suspend fun throttleBangumi() {
        bangumiRateLimiter.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastBangumiTimestamp
            if (elapsed < 400L) {
                delay(400L - elapsed)
            }
            lastBangumiTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun executeBangumiCall(url: String, requireAuth: Boolean = false): String {
        throttleBangumi()
        val request = GET(url, headers = headersOf("User-Agent", "Animetail/1.0", "Accept", APP_JSON))
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

    suspend fun addLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val url = "$API_URL/v0/users/-/collections/${track.remote_id}"
            val body = buildJsonObject {
                put("type", track.toApiStatus())
                put("rate", track.score.toInt().coerceIn(0, 10))
                put("ep_status", track.last_chapter_read.toInt())
                put("private", track.private)
            }
                .toString()
                .toRequestBody()
            // Returns with 202 Accepted on success with no body
            authClient.newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess()
            track
        }
    }

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val url = "$API_URL/v0/users/-/collections/${track.remote_id}"
            val body = buildJsonObject {
                put("type", track.toApiStatus())
                put("rate", track.score.toInt().coerceIn(0, 10))
                put("ep_status", track.last_episode_seen.toInt())
                put("private", track.private)
            }
                .toString()
                .toRequestBody()
            // Returns with 202 Accepted on success with no body
            authClient.newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess()
            track
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val url = "$API_URL/v0/users/-/collections/${track.remote_id}"
            val body = buildJsonObject {
                put("type", track.toApiStatus())
                put("rate", track.score.toInt().coerceIn(0, 10))
                put("ep_status", track.last_chapter_read.toInt())
                put("private", track.private)
            }
                .toString()
                .toRequestBody()

            val request = Request.Builder()
                .url(url)
                .patch(body)
                .headers(headersOf("Content-Type", APP_JSON))
                .build()
            // Returns with 204 No Content
            authClient.newCall(request)
                .awaitSuccess()

            track
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val url = "$API_URL/v0/users/-/collections/${track.remote_id}"
            val body = buildJsonObject {
                put("type", track.toApiStatus())
                put("rate", track.score.toInt().coerceIn(0, 10))
                put("ep_status", track.last_episode_seen.toInt())
                put("private", track.private)
            }
                .toString()
                .toRequestBody()

            val request = Request.Builder()
                .url(url)
                .patch(body)
                .headers(headersOf("Content-Type", APP_JSON))
                .build()
            // Returns with 204 No Content
            authClient.newCall(request)
                .awaitSuccess()

            track
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        // This API is marked as experimental in the documentation
        // but that has been the case since 2022 with few significant
        // changes to the schema for this endpoint since
        // "实验性 API， 本 schema 和实际的 API 行为都可能随时发生改动"
        return withIOContext {
            val url = "$API_URL/v0/search/subjects?limit=20"
            val body = buildJsonObject {
                put("keyword", search)
                put("sort", "match")
                putJsonObject("filter") {
                    putJsonArray("type") {
                        add(1) // "Book" (书籍) type
                    }
                }
            }
                .toString()
                .toRequestBody()
            with(json) {
                authClient.newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                    .awaitSuccess()
                    .parseAs<BGMSearchResult>()
                    .data
                    .filter { it.platform == null || it.platform == "漫画" }
                    .map { it.toMangaTrackSearch(trackId) }
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch? {
        return withIOContext {
            val url = "$API_URL/v0/subjects/$id"

            with(json) {
                authClient.newCall(GET(url, headers = headersOf("Content-Type", APP_JSON)))
                    .awaitSuccess()
                    .parseAs<BGMSubject>()
                    .takeIf { it.platform == null || it.platform == "漫画" }
                    ?.toMangaTrackSearch(trackId)
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        // This API is marked as experimental in the documentation
        // but that has been the case since 2022 with few significant
        // changes to the schema for this endpoint since
        // "实验性 API， 本 schema 和实际的 API 行为都可能随时发生改动"
        return withIOContext {
            val url = "$API_URL/v0/search/subjects?limit=20"
            val body = buildJsonObject {
                put("keyword", search)
                put("sort", "match")
                putJsonObject("filter") {
                    putJsonArray("type") {
                        add(2) // "Anime" (动画) type
                    }
                }
            }
                .toString()
                .toRequestBody()
            with(json) {
                authClient.newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                    .awaitSuccess()
                    .parseAs<BGMSearchResult>()
                    .data
                    .map { it.toAnimeTrackSearch(trackId) }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch? {
        return withIOContext {
            val url = "$API_URL/v0/subjects/$id"

            with(json) {
                authClient.newCall(GET(url, headers = headersOf("Content-Type", APP_JSON)))
                    .awaitSuccess()
                    .parseAs<BGMSubject>()
                    .takeIf { it.platform != "漫画" }
                    ?.toAnimeTrackSearch(trackId)
            }
        }
    }

    suspend fun statusLibManga(track: MangaTrack, username: String): MangaTrack? {
        return withIOContext {
            val url = "$API_URL/v0/users/$username/collections/${track.remote_id}"
            with(json) {
                try {
                    authClient.newCall(GET(url, cache = CacheControl.FORCE_NETWORK))
                        .awaitSuccess()
                        .parseAs<BGMCollectionResponse>()
                        .let {
                            track.status = it.getStatus()
                            track.last_chapter_read = it.epStatus?.toDouble() ?: 0.0
                            track.score = it.rate?.toDouble() ?: 0.0
                            track.total_chapters = it.subject?.eps?.toLong() ?: 0L
                            track
                        }
                } catch (e: HttpException) {
                    if (e.code == 404) { // "subject is not collected by user"
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    suspend fun statusLibAnime(track: AnimeTrack, username: String): AnimeTrack? {
        return withIOContext {
            val url = "$API_URL/v0/users/$username/collections/${track.remote_id}"
            with(json) {
                try {
                    authClient.newCall(GET(url, cache = CacheControl.FORCE_NETWORK))
                        .awaitSuccess()
                        .parseAs<BGMCollectionResponse>()
                        .let {
                            track.status = it.getStatus()
                            track.last_episode_seen = it.epStatus?.toDouble() ?: 0.0
                            track.score = it.rate?.toDouble() ?: 0.0
                            track.total_episodes = it.subject?.eps?.toLong() ?: 0L
                            track
                        }
                } catch (e: HttpException) {
                    if (e.code == 404) { // "subject is not collected by user"
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    suspend fun accessToken(code: String): BGMOAuth {
        return withIOContext {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("code", code)
                .add("redirect_uri", REDIRECT_URL)
                .build()

            with(json) {
                client.newCall(POST(OAUTH_URL, body = body))
                    .awaitSuccess()
                    .parseAs<BGMOAuth>()
            }
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            val urlUserRead = "$API_URL/collection/${track.remoteId}"
            val requestUserRead = Request.Builder()
                .url(urlUserRead)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            with(json) {
                authClient.newCall(GET("${API_URL}/v0/subjects/${track.remoteId}"))
                    .awaitSuccess()
                    .parseAs<BGMSubject>()
                    .let {
                        TrackMangaMetadata(
                            remoteId = it.id,
                            title = it.nameCn,
                            thumbnailUrl = it.images?.common,
                            description = it.summary,
                            authors = it.infobox
                                .filter { it.key == "作者" }
                                .filterIsInstance<Infobox.SingleValue>().joinToString(", ") { it.value },
                            artists = it.infobox
                                .filter { it.key == "插图" }
                                .filterIsInstance<Infobox.SingleValue>().joinToString(", ") { it.value },
                        )
                    }
            }
        }
    }

    suspend fun getCurrentUser(): BGMUser {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("$API_URL/v0/me"))
                    .awaitSuccess()
                    .parseAs<BGMUser>()
            }
        }
    }

    /**
     * Retrieves comprehensive deep media details from Bangumi ported from DailyAL's rich media graph.
     *
     * Queries Bangumi v0 API endpoints requesting subject metadata, characters, staff/persons,
     * and franchise relations with client-side rate-limiting and defensive JSON parsing.
     *
     * @param mediaId Bangumi subject identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val subjectUrl = "$API_URL/v0/subjects/$mediaId"
                val responseBody = executeBangumiCall(subjectUrl)
                val jsonElement = json.parseToJsonElement(responseBody)
                val subjectObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in Bangumi response for id=$mediaId")

                val characters = runCatching {
                    val charBody = executeBangumiCall("$API_URL/v0/subjects/$mediaId/characters")
                    val charArray = json.parseToJsonElement(charBody).asJsonArrayOrNull ?: emptyList()
                    charArray.mapNotNull { itemElem ->
                        val item = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                        val id = item["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val name = item["name"]?.jsonPrimitive?.contentOrNull ?: "Character"
                        val relation = item["relation"]?.jsonPrimitive?.contentOrNull ?: "MAIN"
                        val img = item["images"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                            ?: item["images"].asJsonObjectOrNull?.get("common")?.jsonPrimitive?.contentOrNull
                        val actors = item["actors"].asJsonArrayOrNull?.mapNotNull { actElem ->
                            val actObj = actElem.asJsonObjectOrNull ?: return@mapNotNull null
                            val actId = actObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                            val actName = actObj["name"]?.jsonPrimitive?.contentOrNull ?: "Voice Actor"
                            val actImg = actObj["images"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                            DailyALVoiceActor(
                                id = actId,
                                name = actName,
                                imageUrl = actImg,
                            )
                        } ?: emptyList()
                        DailyALCharacterRole(
                            id = id,
                            name = name,
                            imageUrl = img,
                            role = relation,
                            voiceActors = actors,
                        )
                    }
                }.getOrDefault(emptyList())

                val staff = runCatching {
                    val staffBody = executeBangumiCall("$API_URL/v0/subjects/$mediaId/persons")
                    val staffArray = json.parseToJsonElement(staffBody).asJsonArrayOrNull ?: emptyList()
                    staffArray.mapNotNull { itemElem ->
                        val item = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                        val id = item["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val name = item["name"]?.jsonPrimitive?.contentOrNull ?: "Staff"
                        val relation = item["relation"]?.jsonPrimitive?.contentOrNull ?: "Staff"
                        val img = item["images"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                        DailyALStaffRole(
                            id = id,
                            name = name,
                            role = relation,
                            imageUrl = img,
                        )
                    }
                }.getOrDefault(emptyList())

                val relations = runCatching {
                    val relBody = executeBangumiCall("$API_URL/v0/subjects/$mediaId/subjects")
                    val relArray = json.parseToJsonElement(relBody).asJsonArrayOrNull ?: emptyList()
                    relArray.mapNotNull { itemElem ->
                        val item = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                        val id = item["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val name = item["name"]?.jsonPrimitive?.contentOrNull
                            ?: item["name_cn"]?.jsonPrimitive?.contentOrNull ?: "Related"
                        val relationType = item["relation"]?.jsonPrimitive?.contentOrNull ?: "RELATED"
                        val img = item["images"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                        DailyALRelation(
                            id = id,
                            title = name,
                            relationType = relationType,
                            coverUrl = img,
                        )
                    }
                }.getOrDefault(emptyList())

                parseBangumiDeepMediaDetails(subjectObj, mediaId, characters, staff, relations)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching Bangumi deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities on Bangumi.
     *
     * Supports search queries by text keywords, release year/season, and sort ranking with
     * client-side rate-limiting and defensive JSON parsing.
     *
     * @param filter The active [DailyALFilterState] specifying search constraints.
     * @param page Page index for paginated browsing (1-based).
     * @return [Result] containing list of [DailyALMediaDetails], or error on failure.
     */
    suspend fun searchAdvanced(filter: DailyALFilterState, page: Int = 1): Result<List<DailyALMediaDetails>> {
        return withIOContext {
            runCatching {
                val offset = ((page.coerceAtLeast(1) - 1) * 25)
                val isAnime = filter.format?.contains("MANGA", ignoreCase = true) != true
                val typeId = if (isAnime) 2 else 1

                val url = if (filter.query.isNotBlank()) {
                    val encoded = Uri.encode(filter.query.trim())
                    "$API_URL/search/subject/$encoded?type=$typeId&responseGroup=large&max_results=25&start=$offset"
                } else {
                    "$API_URL/v0/subjects?type=$typeId&sort=rank&limit=25&offset=$offset"
                }

                val responseBody = executeBangumiCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val list = jsonElement.asJsonObjectOrNull?.get("list").asJsonArrayOrNull
                    ?: jsonElement.asJsonObjectOrNull?.get("data").asJsonArrayOrNull
                    ?: jsonElement.asJsonArrayOrNull
                    ?: emptyList()

                list.mapNotNull { itemElem ->
                    val itemObj = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                    val id = itemObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    parseBangumiDeepMediaDetails(
                        itemObj,
                        id,
                        characters = emptyList(),
                        staff = emptyList(),
                        relations = emptyList(),
                    )
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing Bangumi advanced search query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated Bangumi user.
     *
     * Retrieves username, avatar, collection counts (wish, doing, collect, on_hold, dropped)
     * matching DailyAL's user view.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val user = getCurrentUser()
                val username = user.username
                val avatarUrl = user.avatar?.large ?: user.avatar?.medium

                val collectionsBody = runCatching {
                    executeBangumiCall("$API_URL/v0/users/$username/collections?subject_type=2&limit=1", requireAuth = true)
                }.getOrNull()

                val totalCount = if (collectionsBody != null) {
                    json.parseToJsonElement(collectionsBody).asJsonObjectOrNull?.get("total")?.jsonPrimitive?.intOrNull ?: 0
                } else {
                    0
                }

                DailyALProfileStats(
                    userName = user.nickname?.ifBlank { username } ?: username,
                    avatarUrl = avatarUrl,
                    bannerUrl = null,
                    animeDaysWatched = 0.0,
                    meanScore = 0.0,
                    totalEpisodes = 0,
                    totalEntries = totalCount,
                    statusDistribution = emptyMap(),
                    scoreDistribution = emptyMap(),
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from Bangumi" }
            }
        }
    }

    private fun parseBangumiDeepMediaDetails(
        subject: JsonObject,
        requestedId: Long,
        characters: List<DailyALCharacterRole>,
        staff: List<DailyALStaffRole>,
        relations: List<DailyALRelation>,
    ): DailyALMediaDetails {
        val id = subject["id"]?.jsonPrimitive?.longOrNull ?: requestedId
        val name = subject["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val nameCn = subject["name_cn"]?.jsonPrimitive?.contentOrNull

        val imagesObj = subject["images"].asJsonObjectOrNull
        val coverUrl = imagesObj?.get("large")?.jsonPrimitive?.contentOrNull
            ?: imagesObj?.get("common")?.jsonPrimitive?.contentOrNull
            ?: imagesObj?.get("medium")?.jsonPrimitive?.contentOrNull

        val platform = subject["platform"]?.jsonPrimitive?.contentOrNull?.uppercase()
        val eps = subject["eps"]?.jsonPrimitive?.intOrNull
            ?: subject["total_episodes"]?.jsonPrimitive?.intOrNull

        val dateStr = subject["date"]?.jsonPrimitive?.contentOrNull
        val seasonYear = dateStr?.take(4)?.toIntOrNull()
        val month = dateStr?.drop(5)?.take(2)?.toIntOrNull()
        val season = when (month) {
            1, 2, 3 -> "WINTER"
            4, 5, 6 -> "SPRING"
            7, 8, 9 -> "SUMMER"
            10, 11, 12 -> "FALL"
            else -> null
        }

        val ratingObj = subject["rating"].asJsonObjectOrNull
        val meanScore = ratingObj?.get("score")?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
        val rank = ratingObj?.get("rank")?.jsonPrimitive?.intOrNull
        val popularity = ratingObj?.get("total")?.jsonPrimitive?.intOrNull

        val summary = subject["summary"]?.jsonPrimitive?.contentOrNull?.htmlDecode()

        val tags = subject["tags"].asJsonArrayOrNull?.mapNotNull {
            it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull
        } ?: emptyList()

        return DailyALMediaDetails(
            id = id,
            title = name,
            romajiTitle = nameCn,
            nativeTitle = name,
            coverUrl = coverUrl,
            bannerUrl = coverUrl,
            format = platform,
            episodes = eps,
            duration = null,
            status = null,
            season = season,
            seasonYear = seasonYear,
            meanScore = meanScore,
            popularity = popularity,
            rank = rank,
            synopsis = summary,
            genres = tags.take(5),
            tags = tags,
            trailerUrl = null,
            characters = characters,
            staff = staff,
            relations = relations,
            recommendations = emptyList(),
            reviews = emptyList(),
        )
    }

    companion object {
        private const val CLIENT_ID = "bgm293165b66d7e58156"
        private const val CLIENT_SECRET = "21d5f5c19ac24b4bc9c855ffa2387030"

        private const val API_URL = "https://api.bgm.tv"
        private const val OAUTH_URL = "https://bgm.tv/oauth/access_token"
        private const val LOGIN_URL = "https://bgm.tv/oauth/authorize"

        private const val REDIRECT_URL = "animetail://bangumi-auth"

        private const val APP_JSON = "application/json"

        fun authUrl(): Uri =
            LOGIN_URL.toUri().buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .build()

        fun refreshTokenRequest(token: String) = POST(
            OAUTH_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("refresh_token", token)
                .add("redirect_uri", REDIRECT_URL)
                .build(),
        )
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

