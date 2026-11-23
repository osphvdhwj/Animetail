package eu.kanade.tachiyomi.data.track.kitsu

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuAddEntryResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuAlgoliaSearchResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuCurrentUserResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuListSearchResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuMangaMetadata
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuOAuth
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuSearchResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuSingleAnime
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuSingleManga
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuUser
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import logcat.LogPriority
import okhttp3.FormBody
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class KitsuApi(private val client: OkHttpClient, interceptor: KitsuInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val kitsuRateLimiter = Mutex()
    private var lastKitsuTimestamp = 0L

    private suspend fun throttleKitsu() {
        kitsuRateLimiter.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastKitsuTimestamp
            if (elapsed < 300L) {
                delay(300L - elapsed)
            }
            lastKitsuTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun executeKitsuCall(url: String, requireAuth: Boolean = false): String {
        throttleKitsu()
        val request = GET(url, headers = headersOf("Accept", VND_API_JSON))
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

    suspend fun addLibManga(track: MangaTrack, userId: String): MangaTrack {
        return withIOContext {
            val data = buildJsonObject {
                putJsonObject("data") {
                    put("type", "libraryEntries")
                    putJsonObject("attributes") {
                        put("status", track.toApiStatus())
                        put("progress", track.last_chapter_read.toInt())
                        put("private", track.private)
                    }
                    putJsonObject("relationships") {
                        putJsonObject("user") {
                            putJsonObject("data") {
                                put("id", userId)
                                put("type", "users")
                            }
                        }
                        putJsonObject("media") {
                            putJsonObject("data") {
                                put("id", track.remote_id)
                                put("type", "manga")
                            }
                        }
                    }
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        "${BASE_URL}library-entries",
                        headers = headersOf("Content-Type", VND_API_JSON),
                        body = data.toString().toRequestBody(VND_JSON_MEDIA_TYPE),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuAddEntryResult>()
                    .let {
                        track.remote_id = it.data.id
                        track
                    }
            }
        }
    }

    suspend fun addLibAnime(track: AnimeTrack, userId: String): AnimeTrack {
        return withIOContext {
            val data = buildJsonObject {
                putJsonObject("data") {
                    put("type", "libraryEntries")
                    putJsonObject("attributes") {
                        put("status", track.toApiStatus())
                        put("progress", track.last_episode_seen.toInt())
                        put("private", track.private)
                    }
                    putJsonObject("relationships") {
                        putJsonObject("user") {
                            putJsonObject("data") {
                                put("id", userId)
                                put("type", "users")
                            }
                        }
                        putJsonObject("media") {
                            putJsonObject("data") {
                                put("id", track.remote_id)
                                put("type", "anime")
                            }
                        }
                    }
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        "${BASE_URL}library-entries",
                        headers = headersOf("Content-Type", VND_API_JSON),
                        body = data.toString().toRequestBody(VND_JSON_MEDIA_TYPE),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuAddEntryResult>()
                    .let {
                        track.remote_id = it.data.id
                        track
                    }
            }
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val data = buildJsonObject {
                putJsonObject("data") {
                    put("type", "libraryEntries")
                    put("id", track.remote_id)
                    putJsonObject("attributes") {
                        put("status", track.toApiStatus())
                        put("progress", track.last_chapter_read.toInt())
                        put("ratingTwenty", track.score.takeIf { it > 0 }?.toInt())
                        put("startedAt", KitsuDateHelper.convert(track.started_reading_date))
                        put("finishedAt", KitsuDateHelper.convert(track.finished_reading_date))
                        put("private", track.private)
                    }
                }
            }

            authClient.newCall(
                Request.Builder()
                    .url("${BASE_URL}library-entries/${track.remote_id}")
                    .headers(
                        headersOf("Content-Type", VND_API_JSON),
                    )
                    .patch(data.toString().toRequestBody(VND_JSON_MEDIA_TYPE))
                    .build(),
            )
                .awaitSuccess()

            track
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val data = buildJsonObject {
                putJsonObject("data") {
                    put("type", "libraryEntries")
                    put("id", track.remote_id)
                    putJsonObject("attributes") {
                        put("status", track.toApiStatus())
                        put("progress", track.last_episode_seen.toInt())
                        put("ratingTwenty", track.score.takeIf { it > 0 }?.toInt())
                        put("startedAt", KitsuDateHelper.convert(track.started_watching_date))
                        put("finishedAt", KitsuDateHelper.convert(track.finished_watching_date))
                        put("private", track.private)
                    }
                }
            }

            authClient.newCall(
                Request.Builder()
                    .url("${BASE_URL}library-entries/${track.remote_id}")
                    .headers(
                        headersOf("Content-Type", VND_API_JSON),
                    )
                    .patch(data.toString().toRequestBody(VND_JSON_MEDIA_TYPE))
                    .build(),
            )
                .awaitSuccess()

            track
        }
    }

    suspend fun removeLibManga(track: DomainMangaTrack) {
        withIOContext {
            authClient.newCall(
                DELETE(
                    "${BASE_URL}library-entries/${track.remoteId}",
                    headers = headersOf("Content-Type", VND_API_JSON),
                ),
            )
                .awaitSuccess()
        }
    }

    suspend fun removeLibAnime(track: DomainAnimeTrack) {
        withIOContext {
            authClient.newCall(
                DELETE(
                    "${BASE_URL}library-entries/${track.remoteId}",
                    headers = headersOf("Content-Type", VND_API_JSON),
                ),
            )
                .awaitSuccess()
        }
    }

    suspend fun search(query: String): List<MangaTrackSearch> {
        return withIOContext {
            with(json) {
                authClient.newCall(GET(ALGOLIA_KEY_URL))
                    .awaitSuccess()
                    .parseAs<KitsuSearchResult>()
                    .let {
                        algoliaSearch(it.media.key, query)
                    }
            }
        }
    }

    suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return withIOContext {
            with(json) {
                authClient.newCall(GET(ALGOLIA_KEY_URL))
                    .awaitSuccess()
                    .parseAs<KitsuSearchResult>()
                    .let {
                        algoliaSearchAnime(it.media.key, query)
                    }
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch? {
        return withIOContext {
            val url = "$BASE_URL/manga/$id"
            try {
                with(json) {
                    authClient.newCall(GET(url))
                        .await()
                        .parseAs<KitsuSingleManga>()
                        .toTrackSearch()
                }
            } catch (e: HttpException) {
                if (e.code == 404) {
                    null
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch? {
        return withIOContext {
            val url = "$BASE_URL/anime/$id"
            try {
                with(json) {
                    authClient.newCall(GET(url))
                        .await()
                        .parseAs<KitsuSingleAnime>()
                        .toTrackSearch()
                }
            } catch (e: HttpException) {
                if (e.code == 404) {
                    null
                } else {
                    throw e
                }
            }
        }
    }

    private suspend fun algoliaSearch(key: String, query: String): List<MangaTrackSearch> {
        return withIOContext {
            val jsonObject = buildJsonObject {
                put(
                    "params",
                    "query=${URLEncoder.encode(query, StandardCharsets.UTF_8.name())}$ALGOLIA_FILTER",
                )
            }

            with(json) {
                client.newCall(
                    POST(
                        ALGOLIA_URL,
                        headers = headersOf(
                            "X-Algolia-Application-Id",
                            ALGOLIA_APP_ID,
                            "X-Algolia-API-Key",
                            key,
                        ),
                        body = jsonObject.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuAlgoliaSearchResult>()
                    .hits
                    .filter { it.subtype != "novel" }
                    .map { it.toMangaTrack() }
            }
        }
    }

    private suspend fun algoliaSearchAnime(key: String, query: String): List<AnimeTrackSearch> {
        return withIOContext {
            val jsonObject = buildJsonObject {
                put(
                    "params",
                    "query=${URLEncoder.encode(query, StandardCharsets.UTF_8.name())}$ALGOLIA_FILTER_ANIME",
                )
            }

            with(json) {
                client.newCall(
                    POST(
                        ALGOLIA_URL,
                        headers = headersOf(
                            "X-Algolia-Application-Id",
                            ALGOLIA_APP_ID,
                            "X-Algolia-API-Key",
                            key,
                        ),
                        body = jsonObject.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuAlgoliaSearchResult>()
                    .hits
                    .filter { it.subtype != "novel" }
                    .map { it.toAnimeTrack() }
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack, userId: String): MangaTrack? {
        return withIOContext {
            val url = "${BASE_URL}library-entries".toUri().buildUpon()
                .encodedQuery("filter[manga_id]=${track.remote_id}&filter[user_id]=$userId")
                .appendQueryParameter("include", "manga")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<KitsuListSearchResult>()
                    .let {
                        if (it.data.isNotEmpty() && it.included.isNotEmpty()) {
                            it.firstToMangaTrack()
                        } else {
                            null
                        }
                    }
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack, userId: String): AnimeTrack? {
        return withIOContext {
            val url = "${BASE_URL}library-entries".toUri().buildUpon()
                .encodedQuery("filter[anime_id]=${track.remote_id}&filter[user_id]=$userId")
                .appendQueryParameter("include", "anime")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<KitsuListSearchResult>()
                    .let {
                        if (it.data.isNotEmpty() && it.included.isNotEmpty()) {
                            it.firstToAnimeTrack()
                        } else {
                            null
                        }
                    }
            }
        }
    }

    suspend fun getLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val url = "${BASE_URL}library-entries".toUri().buildUpon()
                .encodedQuery("filter[id]=${track.remote_id}")
                .appendQueryParameter("include", "manga")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<KitsuListSearchResult>()
                    .let {
                        if (it.data.isNotEmpty() && it.included.isNotEmpty()) {
                            it.firstToMangaTrack()
                        } else {
                            throw Exception("Could not find manga")
                        }
                    }
            }
        }
    }

    suspend fun getLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val url = "${BASE_URL}library-entries".toUri().buildUpon()
                .encodedQuery("filter[id]=${track.remote_id}")
                .appendQueryParameter("include", "anime")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<KitsuListSearchResult>()
                    .let {
                        if (it.data.isNotEmpty() && it.included.isNotEmpty()) {
                            it.firstToAnimeTrack()
                        } else {
                            throw Exception("Could not find manga")
                        }
                    }
            }
        }
    }

    suspend fun login(username: String, password: String): KitsuOAuth {
        return withIOContext {
            val formBody: RequestBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("grant_type", "password")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build()
            with(json) {
                client.newCall(POST(LOGIN_URL, body = formBody))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    suspend fun getCurrentUser(): KitsuUser {
        return withIOContext {
            val url = "${BASE_URL}users".toUri().buildUpon()
                .encodedQuery("filter[self]=true")
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<KitsuCurrentUserResult>()
                    .data[0]
            }
        }
    }
    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            val query = """
            |query(${'$'}libraryId: ID!, ${'$'}staffCount: Int) {
                |findLibraryEntryById(id: ${'$'}libraryId) {
                    |media {
                        |id
                        |titles {
                            |preferred
                        |}
                        |posterImage {
                            |original {
                                |url
                            |}
                        |}
                        |description
                        |staff(first: ${'$'}staffCount) {
                            |nodes {
                                |role
                                |person {
                                    |name
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("libraryId", track.remoteId)
                    put("staffCount", 25) // 25 based on nothing
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_URL,
                        headers = headersOf("Accept-Language", "en"),
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuMangaMetadata>()
                    .let {
                        val manga = it.data.findLibraryEntryById.media
                        TrackMangaMetadata(
                            remoteId = manga.id.toLong(),
                            title = manga.titles.preferred,
                            thumbnailUrl = manga.posterImage.original.url,
                            description = manga.description.en?.htmlDecode()?.ifEmpty { null },
                            authors = manga.staff.nodes
                                .filter { it.role == "Story" || it.role == "Story & Art" }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                            artists = manga.staff.nodes
                                .filter { it.role == "Art" || it.role == "Story & Art" }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata {
        return withIOContext {
            val query = """
            |query(${'$'}libraryId: ID!, ${'$'}staffCount: Int) {
                |findLibraryEntryById(id: ${'$'}libraryId) {
                    |media {
                        |id
                        |titles {
                            |preferred
                        |}
                        |posterImage {
                            |original {
                                |url
                            |}
                        |}
                        |description
                        |staff(first: ${'$'}staffCount) {
                            |nodes {
                                |role
                                |person {
                                    |name
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("libraryId", track.remoteId)
                    put("staffCount", 25) // 25 based on nothing
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_URL,
                        headers = headersOf("Accept-Language", "en"),
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<KitsuMangaMetadata>()
                    .let {
                        val anime = it.data.findLibraryEntryById.media
                        TrackAnimeMetadata(
                            remoteId = anime.id.toLong(),
                            title = anime.titles.preferred,
                            thumbnailUrl = anime.posterImage.original.url,
                            description = anime.description.en?.htmlDecode()?.ifEmpty { null },
                            authors = anime.staff.nodes
                                .filter { it.role == "Story" || it.role == "Story & Art" }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                            artists = anime.staff.nodes
                                .filter { it.role == "Art" || it.role == "Story & Art" }
                                .map { it.person.name }
                                .joinToString(", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }
    /**
     * Retrieves comprehensive deep media details from Kitsu ported from DailyAL's rich media graph.
     *
     * Queries Kitsu JSON:API including categories, characters, staff, and relations with
     * client-side rate-limiting and defensive JSON parsing.
     *
     * @param mediaId Kitsu media identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val type = if (isAnime) "anime" else "manga"
                val url = "${BASE_URL}$type/$mediaId?include=categories,characters.character,staff.person,mediaRelationships.destination"
                val responseBody = executeKitsuCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val rootObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Invalid Kitsu response for id=$mediaId")
                val dataObj = rootObj["data"].asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in Kitsu response for id=$mediaId")
                val includedArray = rootObj["included"].asJsonArrayOrNull ?: emptyList()
                parseKitsuDeepMediaDetails(dataObj, includedArray, mediaId, isAnime)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching Kitsu deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities on Kitsu.
     *
     * Supports search queries by text keywords, categories/genres, season/year, ranking filters (popularity, score),
     * and pagination with client-side rate-limiting and defensive JSON parsing.
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
                val type = if (isAnime) "anime" else "manga"
                val urlBuilder = "${BASE_URL}$type?page[limit]=25&page[offset]=$offset&include=categories".toUri().buildUpon()

                if (filter.query.isNotBlank()) {
                    urlBuilder.appendQueryParameter("filter[text]", filter.query.trim())
                }
                if (filter.includedGenres.isNotEmpty()) {
                    urlBuilder.appendQueryParameter("filter[categories]", filter.includedGenres.joinToString(","))
                }
                if (filter.seasonYear != null && filter.seasonYear > 0) {
                    urlBuilder.appendQueryParameter("filter[seasonYear]", filter.seasonYear.toString())
                }
                if (!filter.season.isNullOrBlank()) {
                    urlBuilder.appendQueryParameter("filter[season]", filter.season.lowercase())
                }
                val sortParam = when (filter.sortBy.uppercase()) {
                    "SCORE_DESC" -> "-averageRating"
                    "START_DATE_DESC" -> "-startDate"
                    else -> "-userCount"
                }
                urlBuilder.appendQueryParameter("sort", sortParam)

                val responseBody = executeKitsuCall(urlBuilder.build().toString())
                val jsonElement = json.parseToJsonElement(responseBody)
                val rootObj = jsonElement.asJsonObjectOrNull ?: return@runCatching emptyList()
                val dataArray = rootObj["data"].asJsonArrayOrNull ?: emptyList()
                val included = rootObj["included"].asJsonArrayOrNull ?: emptyList()

                dataArray.mapNotNull { itemElem ->
                    val dataObj = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                    val id = dataObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    parseKitsuDeepMediaDetails(dataObj, included, id, isAnime)
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing Kitsu advanced search query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated Kitsu user.
     *
     * Retrieves username, avatar, total days watched, completed/watching counts,
     * and status distribution matching DailyAL's user view.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val url = "${BASE_URL}users?filter[self]=true"
                val responseBody = executeKitsuCall(url, requireAuth = true)
                val jsonElement = json.parseToJsonElement(responseBody)
                val userArray = jsonElement.asJsonObjectOrNull?.get("data").asJsonArrayOrNull ?: emptyList()
                val userObj = userArray.firstOrNull().asJsonObjectOrNull
                    ?: throw IllegalStateException("Failed to parse Kitsu user profile data")

                val attrs = userObj["attributes"].asJsonObjectOrNull
                val userName = attrs?.get("name")?.jsonPrimitive?.contentOrNull ?: "Kitsu User"
                val avatarUrl = attrs?.get("avatar").asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                val bannerUrl = attrs?.get("coverImage").asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull

                val lifeMinutes = attrs?.get("lifeSpentOnAnime")?.jsonPrimitive?.longOrNull ?: 0L
                val daysWatched = if (lifeMinutes > 0) {
                    (lifeMinutes / 1440.0 * 10.0).toLong() / 10.0
                } else {
                    0.0
                }

                DailyALProfileStats(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    bannerUrl = bannerUrl,
                    animeDaysWatched = daysWatched,
                    meanScore = 0.0,
                    totalEpisodes = 0,
                    totalEntries = 0,
                    statusDistribution = emptyMap(),
                    scoreDistribution = emptyMap(),
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from Kitsu" }
            }
        }
    }

    private fun parseKitsuDeepMediaDetails(
        data: JsonObject,
        included: List<JsonElement>,
        requestedId: Long,
        isAnime: Boolean,
    ): DailyALMediaDetails {
        val id = data["id"]?.jsonPrimitive?.longOrNull ?: requestedId
        val attrs = data["attributes"].asJsonObjectOrNull
        val canonicalTitle = attrs?.get("canonicalTitle")?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val titlesObj = attrs?.get("titles").asJsonObjectOrNull
        val enTitle = titlesObj?.get("en")?.jsonPrimitive?.contentOrNull ?: titlesObj?.get("en_jp")?.jsonPrimitive?.contentOrNull
        val jaTitle = titlesObj?.get("ja_jp")?.jsonPrimitive?.contentOrNull

        val posterObj = attrs?.get("posterImage").asJsonObjectOrNull
        val coverUrl = posterObj?.get("large")?.jsonPrimitive?.contentOrNull
            ?: posterObj?.get("original")?.jsonPrimitive?.contentOrNull
            ?: posterObj?.get("medium")?.jsonPrimitive?.contentOrNull

        val coverImgObj = attrs?.get("coverImage").asJsonObjectOrNull
        val bannerUrl = coverImgObj?.get("large")?.jsonPrimitive?.contentOrNull
            ?: coverImgObj?.get("original")?.jsonPrimitive?.contentOrNull
            ?: coverUrl

        val format = attrs?.get("subtype")?.jsonPrimitive?.contentOrNull?.uppercase()
        val episodes = if (isAnime) {
            attrs?.get("episodeCount")?.jsonPrimitive?.intOrNull
        } else {
            attrs?.get("chapterCount")?.jsonPrimitive?.intOrNull
        }
        val duration = attrs?.get("episodeLength")?.jsonPrimitive?.intOrNull

        val rawStatus = attrs?.get("status")?.jsonPrimitive?.contentOrNull
        val status = when (rawStatus?.lowercase()) {
            "current" -> "RELEASING"
            "finished" -> "FINISHED"
            "tba", "unreleased" -> "NOT_YET_RELEASED"
            else -> rawStatus?.uppercase()
        }

        val startDate = attrs?.get("startDate")?.jsonPrimitive?.contentOrNull
        val seasonYear = startDate?.take(4)?.toIntOrNull()
        val month = startDate?.drop(5)?.take(2)?.toIntOrNull()
        val season = when (month) {
            1, 2, 3 -> "WINTER"
            4, 5, 6 -> "SPRING"
            7, 8, 9 -> "SUMMER"
            10, 11, 12 -> "FALL"
            else -> null
        }

        val meanScore = attrs?.get("averageRating")?.jsonPrimitive?.doubleOrNull?.toInt()
        val popularity = attrs?.get("popularityRank")?.jsonPrimitive?.intOrNull
        val rank = attrs?.get("ratingRank")?.jsonPrimitive?.intOrNull
        val synopsis = attrs?.get("synopsis")?.jsonPrimitive?.contentOrNull?.htmlDecode()

        val ytId = attrs?.get("youtubeVideoId")?.jsonPrimitive?.contentOrNull
        val trailerUrl = if (!ytId.isNullOrBlank()) "https://www.youtube.com/watch?v=$ytId" else null

        // Parse categories, characters, and relations from included list
        val genres = mutableListOf<String>()
        val characters = mutableListOf<DailyALCharacterRole>()
        val staff = mutableListOf<DailyALStaffRole>()
        val relations = mutableListOf<DailyALRelation>()

        included.forEach { incElem ->
            val incObj = incElem.asJsonObjectOrNull ?: return@forEach
            val incType = incObj["type"]?.jsonPrimitive?.contentOrNull
            val incAttrs = incObj["attributes"].asJsonObjectOrNull ?: return@forEach
            when (incType) {
                "categories" -> {
                    val catTitle = incAttrs["title"]?.jsonPrimitive?.contentOrNull
                    if (!catTitle.isNullOrBlank() && !genres.contains(catTitle)) {
                        genres.add(catTitle)
                    }
                }
                "characters" -> {
                    val charId = incObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                    val charName = incAttrs["canonicalName"]?.jsonPrimitive?.contentOrNull
                        ?: incAttrs["name"]?.jsonPrimitive?.contentOrNull ?: "Character"
                    val charImg = incAttrs["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                    characters.add(
                        DailyALCharacterRole(
                            id = charId,
                            name = charName,
                            imageUrl = charImg,
                            role = "MAIN",
                        ),
                    )
                }
                "people" -> {
                    val personId = incObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                    val personName = incAttrs["name"]?.jsonPrimitive?.contentOrNull ?: "Staff"
                    val personImg = incAttrs["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                    staff.add(
                        DailyALStaffRole(
                            id = personId,
                            name = personName,
                            role = "Staff",
                            imageUrl = personImg,
                        ),
                    )
                }
                "anime", "manga" -> {
                    val relId = incObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                    val relTitle = incAttrs["canonicalTitle"]?.jsonPrimitive?.contentOrNull ?: "Related"
                    val relCover = incAttrs["posterImage"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                    val relScore = incAttrs["averageRating"]?.jsonPrimitive?.doubleOrNull?.toInt()
                    val relFormat = incAttrs["subtype"]?.jsonPrimitive?.contentOrNull?.uppercase()
                    relations.add(
                        DailyALRelation(
                            id = relId,
                            title = relTitle,
                            format = relFormat,
                            status = incAttrs["status"]?.jsonPrimitive?.contentOrNull?.uppercase(),
                            relationType = "RELATED",
                            coverUrl = relCover,
                            meanScore = relScore,
                        ),
                    )
                }
            }
        }

        return DailyALMediaDetails(
            id = id,
            title = canonicalTitle,
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
            characters = characters,
            staff = staff,
            relations = relations,
            recommendations = emptyList(),
            reviews = emptyList(),
        )
    }

    companion object {
        private const val CLIENT_ID =
            "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val CLIENT_SECRET =
            "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"

        private const val BASE_URL = "https://kitsu.app/api/edge/"
        private const val GRAPHQL_URL = "https://kitsu.app/api/graphql"
        private const val LOGIN_URL = "https://kitsu.app/api/oauth/token"
        private const val BASE_MANGA_URL = "https://kitsu.app/manga/"
        private const val BASE_ANIME_URL = "https://kitsu.app/anime/"
        private const val ALGOLIA_KEY_URL = "https://kitsu.app/api/edge/algolia-keys/media/"

        private const val ALGOLIA_URL =
            "https://AWQO5J657S-dsn.algolia.net/1/indexes/production_media/query/"
        private const val ALGOLIA_APP_ID = "AWQO5J657S"
        private const val ALGOLIA_FILTER =
            "&facetFilters=%5B%22kind%3Amanga%22%5D&attributesToRetrieve=" +
                "%5B%22synopsis%22%2C%22averageRating%22%2C%22canonicalTitle%22%2C%22chapterCount%22%2C%22" +
                "posterImage%22%2C%22startDate%22%2C%22subtype%22%2C%22endDate%22%2C%20%22id%22%5D"
        private const val ALGOLIA_FILTER_ANIME =
            "&facetFilters=%5B%22kind%3Aanime%22%5D&attributesToRetrieve=" +
                "%5B%22synopsis%22%2C%22averageRating%22%2C%22canonicalTitle%22%2C%22episodeCount%22%2C%22" +
                "posterImage%22%2C%22startDate%22%2C%22subtype%22%2C%22endDate%22%2C%20%22id%22%5D"

        private const val VND_API_JSON = "application/vnd.api+json"
        private val VND_JSON_MEDIA_TYPE = VND_API_JSON.toMediaType()

        fun mangaUrl(remoteId: Long): String {
            return BASE_MANGA_URL + remoteId
        }

        fun animeUrl(remoteId: Long): String {
            return BASE_ANIME_URL + remoteId
        }

        fun refreshTokenRequest(token: String) = POST(
            LOGIN_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", token)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build(),
        )
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

