package eu.kanade.tachiyomi.data.track.myanimelist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALAnime
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALAnimeMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALAnimeSearchResult
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListAnimeItem
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListAnimeItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListMangaItem
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListMangaItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALManga
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALMangaMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALSearchResult
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALUser
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
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
import eu.kanade.tachiyomi.util.PkceUtil
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import logcat.LogPriority
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class MyAnimeListApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: MyAnimeListInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val malRateLimiter = Mutex()
    private var lastRequestTimestamp = 0L

    private suspend fun throttleMal() {
        malRateLimiter.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTimestamp
            if (elapsed < 350L) {
                delay(350L - elapsed)
            }
            lastRequestTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun executeMalCall(url: String, requireAuth: Boolean = false): String {
        throttleMal()
        val baseReq = GET(url)
        return if (requireAuth) {
            authClient.newCall(baseReq).awaitSuccess().body.string()
        } else {
            val publicReq = baseReq.newBuilder().addHeader("X-MAL-CLIENT-ID", CLIENT_ID).build()
            try {
                authClient.newCall(baseReq).awaitSuccess().body.string()
            } catch (_: Exception) {
                client.newCall(publicReq).awaitSuccess().body.string()
            }
        }
    }

    suspend fun getAccessToken(authCode: String): MALOAuth {
        return withIOContext {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .add("grant_type", "authorization_code")
                .build()
            with(json) {
                client.newCall(POST("$BASE_OAUTH_URL/token", body = formBody))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    suspend fun getCurrentUser(): String {
        return withIOContext {
            val request = Request.Builder()
                .url("$BASE_API_URL/users/@me")
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<MALUser>()
                    .name
            }
        }
    }

    suspend fun search(query: String): List<MangaTrackSearch> {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                // MAL API throws a 400 when the query is over 64 characters...
                .appendQueryParameter("q", query.take(64))
                .appendQueryParameter("nsfw", "true")
                .appendQueryParameter("fields", SEARCH_FIELDS)
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALSearchResult>()
                    .data
                    .filter { !(it.node.mediaType.contains("novel")) }
                    .map { parseSearchItem(it.node) }
            }
        }
    }

    suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$BASE_API_URL/anime".toUri().buildUpon()
                // MAL API throws a 400 when the query is over 64 characters...
                .appendQueryParameter("q", query.take(64))
                .appendQueryParameter("nsfw", "true")
                .appendQueryParameter("fields", SEARCH_FIELDS_ANIME)
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALAnimeSearchResult>()
                    .data
                    .map { parseAnimeSearchItem(it.node) }
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendPath(id.toString())
                .appendQueryParameter("fields", SEARCH_FIELDS)
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALManga>()
                    .let { parseSearchItem(it) }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch {
        return withIOContext {
            val url = "$BASE_API_URL/anime".toUri().buildUpon()
                .appendPath(id.toString())
                .appendQueryParameter("fields", SEARCH_FIELDS_ANIME)
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALAnime>()
                    .let { parseAnimeSearchItem(it) }
            }
        }
    }

    suspend fun getPopularAnime(): List<AnimeTrackSearch> {
        return try {
            withIOContext {
                val url = "$BASE_API_URL/anime/ranking".toUri().buildUpon()
                    .appendQueryParameter("ranking_type", "all")
                    .appendQueryParameter("limit", "20")
                    .appendQueryParameter("fields", SEARCH_FIELDS_ANIME)
                    .build()
                with(json) {
                    authClient.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<MALAnimeSearchResult>()
                        .data
                        .map { parseAnimeSearchItem(it.node) }
                }
            }
        } catch (e: Exception) {
            searchAnime("a")
        }
    }

    suspend fun getPopularManga(): List<MangaTrackSearch> {
        return try {
            withIOContext {
                val url = "$BASE_API_URL/manga/ranking".toUri().buildUpon()
                    .appendQueryParameter("ranking_type", "all")
                    .appendQueryParameter("limit", "20")
                    .appendQueryParameter("fields", SEARCH_FIELDS)
                    .build()
                with(json) {
                    authClient.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<MALSearchResult>()
                        .data
                        .filter { !(it.node.mediaType.contains("novel")) }
                        .map { parseSearchItem(it.node) }
                }
            }
        } catch (e: Exception) {
            search("a")
        }
    }

    suspend fun updateItem(track: MangaTrack): MangaTrack {
        return withIOContext {
            val formBodyBuilder = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "reading")
                .add("is_rereading", (track.status == MyAnimeList.REREADING).toString())
                .add("score", track.score.toString())
                .add("num_chapters_read", track.last_chapter_read.toInt().toString())
            convertToIsoDate(track.started_reading_date)?.let {
                formBodyBuilder.add("start_date", it)
            }
            convertToIsoDate(track.finished_reading_date)?.let {
                formBodyBuilder.add("finish_date", it)
            }

            val request = Request.Builder()
                .url(mangaUrl(track.remote_id).toString())
                .put(formBodyBuilder.build())
                .build()
            with(json) {
                val response = authClient
                    .newCall(request)
                    .await()

                if (!response.isSuccessful) {
                    if (response.body.string().contains("invalid_content")) {
                        // MAL returns unapproved titles in search but does not allow adding them to the list
                        // returns 400 with this body: {"message":"Invalid content","error":"invalid_content"}
                        // These unapproved titles cannot be filtered out in search and are also returned by the
                        // endpoint we use for id prefix search
                        throw MALTitleNotApproved()
                    } else {
                        throw HttpException(response.code)
                    }
                }

                response
                    .parseAs<MALListMangaItemStatus>()
                    .let { parseMangaItem(it, track) }
            }
        }
    }

    suspend fun updateItem(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val formBodyBuilder = FormBody.Builder()
                .add("status", track.toMyAnimeListStatus() ?: "watching")
                .add("is_rewatching", (track.status == MyAnimeList.REWATCHING).toString())
                .add("score", track.score.toString())
                .add("num_watched_episodes", track.last_episode_seen.toInt().toString())
            convertToIsoDate(track.started_watching_date)?.let {
                formBodyBuilder.add("start_date", it)
            }
            convertToIsoDate(track.finished_watching_date)?.let {
                formBodyBuilder.add("finish_date", it)
            }

            val request = Request.Builder()
                .url(animeUrl(track.remote_id).toString())
                .put(formBodyBuilder.build())
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<MALListAnimeItemStatus>()
                    .let { parseAnimeItem(it, track) }
            }
        }
    }

    suspend fun deleteMangaItem(track: DomainMangaTrack) {
        withIOContext {
            authClient
                .newCall(DELETE(mangaUrl(track.remoteId).toString()))
                .awaitSuccess()
        }
    }

    suspend fun deleteAnimeItem(track: DomainAnimeTrack) {
        withIOContext {
            authClient
                .newCall(DELETE(animeUrl(track.remoteId).toString()))
                .awaitSuccess()
        }
    }

    suspend fun findListItem(track: MangaTrack): MangaTrack? {
        return withIOContext {
            val uri = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .appendQueryParameter("fields", "num_chapters,my_list_status{start_date,finish_date}")
                .build()
            with(json) {
                authClient.newCall(GET(uri.toString()))
                    .awaitSuccess()
                    .parseAs<MALListMangaItem>()
                    .let { item ->
                        track.total_chapters = item.numChapters
                        item.myListStatus?.let { parseMangaItem(it, track) }
                    }
            }
        }
    }

    suspend fun findListItem(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val uri = "$BASE_API_URL/anime".toUri().buildUpon()
                .appendPath(track.remote_id.toString())
                .appendQueryParameter("fields", "num_episodes,my_list_status{start_date,finish_date}")
                .build()
            with(json) {
                authClient.newCall(GET(uri.toString()))
                    .awaitSuccess()
                    .parseAs<MALListAnimeItem>()
                    .let { item ->
                        track.total_episodes = item.numEpisodes
                        item.myListStatus?.let { parseAnimeItem(it, track) }
                    }
            }
        }
    }

    suspend fun findListItems(query: String, offset: Int = 0): List<MangaTrackSearch> {
        return withIOContext {
            val myListSearchResult = getListPage(offset)

            val matches = myListSearchResult.data
                .filter { it.node.title.contains(query, ignoreCase = true) }
                .map { parseSearchItem(it.node) }

            // Check next page if there's more
            if (!myListSearchResult.paging.next.isNullOrBlank()) {
                matches + findListItems(query, offset + LIST_PAGINATION_AMOUNT)
            } else {
                matches
            }
        }
    }

    suspend fun findListItemsAnime(query: String, offset: Int = 0): List<AnimeTrackSearch> {
        return withIOContext {
            val myListSearchResult = getAnimeListPage(offset)

            val matches = myListSearchResult.data
                .filter { it.node.title.contains(query, ignoreCase = true) }
                .map { parseAnimeSearchItem(it.node) }

            // Check next page if there's more
            if (!myListSearchResult.paging.next.isNullOrBlank()) {
                matches + findListItemsAnime(query, offset + LIST_PAGINATION_AMOUNT)
            } else {
                matches
            }
        }
    }

    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata? {
        return withIOContext {
            val url = "$BASE_API_URL/anime".toUri().buildUpon()
                .appendPath(track.remoteId.toString())
                .appendQueryParameter(
                    "fields",
                    "id,title,synopsis,main_picture,studios{name}",
                )
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALAnimeMetadata>()
                    .let { it ->
                        TrackAnimeMetadata(
                            remoteId = it.id,
                            title = it.title,
                            thumbnailUrl = it.covers.large.ifEmpty { null } ?: it.covers.medium,
                            description = it.synopsis,
                            authors = it.studios
                                .map { it.name.trim() }
                                .joinToString(separator = ", ")
                                .ifEmpty { null },
                            artists = it.studios
                                .map { it.name.trim() }
                                .joinToString(separator = ", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata? {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendPath(track.remoteId.toString())
                .appendQueryParameter(
                    "fields",
                    "id,title,synopsis,main_picture,authors{first_name,last_name}",
                )
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MALMangaMetadata>()
                    .let {
                        TrackMangaMetadata(
                            remoteId = it.id,
                            title = it.title,
                            thumbnailUrl = it.covers.large.ifEmpty { null } ?: it.covers.medium,
                            description = it.synopsis,
                            authors = it.authors
                                .filter { it.role == "Story" || it.role == "Story & Art" }
                                .map { "${it.node.firstName} ${it.node.lastName}".trim() }
                                .joinToString(separator = ", ")
                                .ifEmpty { null },
                            artists = it.authors
                                .filter { it.role == "Art" || it.role == "Story & Art" }
                                .map { "${it.node.firstName} ${it.node.lastName}".trim() }
                                .joinToString(separator = ", ")
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    private suspend fun getListPage(offset: Int): MALSearchResult {
        return withIOContext {
            val urlBuilder = "$BASE_API_URL/users/@me/mangalist".toUri().buildUpon()
                .appendQueryParameter("fields", SEARCH_FIELDS)
                .appendQueryParameter("limit", LIST_PAGINATION_AMOUNT.toString())
            if (offset > 0) {
                urlBuilder.appendQueryParameter("offset", offset.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private suspend fun getAnimeListPage(offset: Int): MALAnimeSearchResult {
        return withIOContext {
            val urlBuilder = "$BASE_API_URL/users/@me/animelist".toUri().buildUpon()
                .appendQueryParameter("fields", SEARCH_FIELDS_ANIME)
                .appendQueryParameter("limit", LIST_PAGINATION_AMOUNT.toString())
            if (offset > 0) {
                urlBuilder.appendQueryParameter("offset", offset.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun parseMangaItem(listStatus: MALListMangaItemStatus, track: MangaTrack): MangaTrack {
        return track.apply {
            val isRereading = listStatus.isRereading
            status = if (isRereading) MyAnimeList.REREADING else getStatus(listStatus.status)
            last_chapter_read = listStatus.numChaptersRead
            score = listStatus.score.toDouble()
            listStatus.startDate?.let { started_reading_date = parseDate(it) }
            listStatus.finishDate?.let { finished_reading_date = parseDate(it) }
        }
    }

    private fun parseAnimeItem(listStatus: MALListAnimeItemStatus, track: AnimeTrack): AnimeTrack {
        return track.apply {
            val isRewatching = listStatus.isRewatching
            status = if (isRewatching) MyAnimeList.REWATCHING else getStatus(listStatus.status)
            last_episode_seen = listStatus.numEpisodesWatched
            score = listStatus.score.toDouble()
            listStatus.startDate?.let { started_watching_date = parseDate(it) }
            listStatus.finishDate?.let { finished_watching_date = parseDate(it) }
        }
    }

    private fun parseAnimeSearchItem(searchItem: MALAnime): AnimeTrackSearch {
        return AnimeTrackSearch.create(trackId).apply {
            remote_id = searchItem.id
            title = searchItem.title
            summary = searchItem.synopsis
            total_episodes = searchItem.numEpisodes
            score = searchItem.mean
            cover_url = searchItem.covers?.large.orEmpty()
            tracking_url = "https://myanimelist.net/anime/$remote_id"
            publishing_status = searchItem.status.replace("_", " ")
            publishing_type = searchItem.mediaType.replace("_", " ")
            start_date = searchItem.startDate ?: ""
            authors = searchItem.studios.map { it.name }
            artists = searchItem.studios.map { it.name }
        }
    }

    private fun parseSearchItem(searchItem: MALManga): MangaTrackSearch {
        return MangaTrackSearch.create(trackId).apply {
            remote_id = searchItem.id
            title = searchItem.title
            summary = searchItem.synopsis
            total_chapters = searchItem.numChapters
            score = searchItem.mean
            cover_url = searchItem.covers?.large.orEmpty()
            tracking_url = "https://myanimelist.net/manga/$remote_id"
            publishing_status = searchItem.status.replace("_", " ")
            publishing_type = searchItem.mediaType.replace("_", " ")
            start_date = searchItem.startDate ?: ""
            authors = searchItem.authors.mapNotNull { it.getFullName() }
            artists = authors
        }
    }

    private fun parseDate(isoDate: String): Long {
        val pattern = when (isoDate.length) {
            10 -> "yyyy-MM-dd"
            7 -> "yyyy-MM"
            4 -> "yyyy"
            else -> throw IllegalArgumentException("Unsupported date format: \"$isoDate\"")
        }
        return SimpleDateFormat(pattern, Locale.US).parse(isoDate)?.time ?: 0L
    }

    private fun convertToIsoDate(epochTime: Long): String? {
        if (epochTime == 0L) {
            return ""
        }
        return try {
            val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            outputDf.format(epochTime)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieves comprehensive deep media details from MyAnimeList ported from DailyAL's rich media graph.
     *
     * Queries MAL API v2 requesting core media metadata, alternative titles, broadcast information,
     * genres, studios, franchise relations, and community recommendations with client-side rate-limiting.
     *
     * @param mediaId MyAnimeList media identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val fields = if (isAnime) {
                    "id,title,main_picture,alternative_titles,start_date,end_date,synopsis,mean,rank,popularity,num_episodes,start_season,broadcast,genres,media_type,status,rating,pictures,background,related_anime{node{id,title,main_picture,media_type,status,mean}},related_manga{node{id,title,main_picture,media_type,status,mean}},recommendations{node{id,title,main_picture,mean},num_recommendations},studios"
                } else {
                    "id,title,main_picture,alternative_titles,start_date,end_date,synopsis,mean,rank,popularity,num_chapters,num_volumes,genres,media_type,status,pictures,background,related_anime{node{id,title,main_picture,media_type,status,mean}},related_manga{node{id,title,main_picture,media_type,status,mean}},recommendations{node{id,title,main_picture,mean},num_recommendations},authors{first_name,last_name,role}"
                }
                val path = if (isAnime) "anime" else "manga"
                val url = "$BASE_API_URL/$path/$mediaId?fields=$fields"
                val responseBody = executeMalCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val mediaObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in MyAnimeList response for id=$mediaId")
                parseMalDeepMediaDetails(mediaObj, mediaId, isAnime)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching MAL deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities.
     *
     * Supports search queries by text keywords, season/year, ranking filters (popularity, score, airing),
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
                val fields = "id,title,main_picture,synopsis,mean,rank,popularity,num_episodes,start_season,genres,media_type,status"
                val url = when {
                    filter.seasonYear != null && !filter.season.isNullOrBlank() -> {
                        "$BASE_API_URL/anime/season/${filter.seasonYear}/${filter.season.lowercase()}?limit=25&offset=$offset&fields=$fields"
                    }
                    filter.query.isNotBlank() -> {
                        val encodedQuery = Uri.encode(filter.query.take(64))
                        "$BASE_API_URL/anime?q=$encodedQuery&limit=25&offset=$offset&nsfw=true&fields=$fields"
                    }
                    else -> {
                        val rankType = when (filter.sortBy.uppercase()) {
                            "POPULARITY_DESC" -> "bypopularity"
                            "SCORE_DESC" -> "all"
                            "TRENDING_DESC" -> "airing"
                            else -> "bypopularity"
                        }
                        "$BASE_API_URL/anime/ranking?ranking_type=$rankType&limit=25&offset=$offset&fields=$fields"
                    }
                }

                val responseBody = executeMalCall(url)
                val jsonElement = json.parseToJsonElement(responseBody)
                val dataArray = jsonElement.asJsonObjectOrNull?.get("data").asJsonArrayOrNull ?: emptyList()
                dataArray.mapNotNull { item ->
                    val node = item.asJsonObjectOrNull?.get("node").asJsonObjectOrNull ?: return@mapNotNull null
                    val id = node["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    parseMalDeepMediaDetails(node, id, isAnime = true)
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing MAL advanced search query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated MyAnimeList user.
     *
     * Retrieves username, avatar, total days watched, mean score, completed/watching counts,
     * and status distribution matching DailyAL's user view.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val url = "$BASE_API_URL/users/@me?fields=anime_statistics"
                val responseBody = executeMalCall(url, requireAuth = true)
                val jsonElement = json.parseToJsonElement(responseBody)
                val userObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Failed to parse MAL user profile data")

                val userName = userObj["name"]?.jsonPrimitive?.contentOrNull ?: "MAL User"
                val avatarUrl = userObj["picture"]?.jsonPrimitive?.contentOrNull
                val stats = userObj["anime_statistics"].asJsonObjectOrNull

                val daysWatched = stats?.get("num_days_watched")?.jsonPrimitive?.doubleOrNull
                    ?: stats?.get("num_days")?.jsonPrimitive?.doubleOrNull
                    ?: 0.0
                val meanScore = (stats?.get("mean_score")?.jsonPrimitive?.doubleOrNull ?: 0.0) * 10.0
                val episodesWatched = stats?.get("num_episodes")?.jsonPrimitive?.intOrNull ?: 0
                val totalEntries = stats?.get("num_items")?.jsonPrimitive?.intOrNull ?: 0

                val statusMap = mutableMapOf<String, Int>()
                stats?.get("num_items_watching")?.jsonPrimitive?.intOrNull?.let { statusMap["CURRENT"] = it }
                stats?.get("num_items_completed")?.jsonPrimitive?.intOrNull?.let { statusMap["COMPLETED"] = it }
                stats?.get("num_items_on_hold")?.jsonPrimitive?.intOrNull?.let { statusMap["PAUSED"] = it }
                stats?.get("num_items_dropped")?.jsonPrimitive?.intOrNull?.let { statusMap["DROPPED"] = it }
                stats?.get("num_items_plan_to_watch")?.jsonPrimitive?.intOrNull?.let { statusMap["PLANNING"] = it }

                DailyALProfileStats(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    bannerUrl = null,
                    animeDaysWatched = daysWatched,
                    meanScore = meanScore,
                    totalEpisodes = episodesWatched,
                    totalEntries = totalEntries,
                    statusDistribution = statusMap,
                    scoreDistribution = emptyMap(),
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from MyAnimeList" }
            }
        }
    }

    private fun parseMalDeepMediaDetails(media: JsonObject, requestedId: Long, isAnime: Boolean): DailyALMediaDetails {
        val id = media["id"]?.jsonPrimitive?.longOrNull ?: requestedId
        val title = media["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val altTitles = media["alternative_titles"].asJsonObjectOrNull
        val enTitle = altTitles?.get("en")?.jsonPrimitive?.contentOrNull
        val jaTitle = altTitles?.get("ja")?.jsonPrimitive?.contentOrNull

        val mainPic = media["main_picture"].asJsonObjectOrNull
        val coverUrl = mainPic?.get("large")?.jsonPrimitive?.contentOrNull
            ?: mainPic?.get("medium")?.jsonPrimitive?.contentOrNull

        val bannerUrl = media["pictures"].asJsonArrayOrNull?.firstOrNull().asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
            ?: coverUrl

        val format = media["media_type"]?.jsonPrimitive?.contentOrNull?.replace("_", " ")?.uppercase()
        val episodes = if (isAnime) {
            media["num_episodes"]?.jsonPrimitive?.intOrNull
        } else {
            media["num_chapters"]?.jsonPrimitive?.intOrNull
        }

        val rawStatus = media["status"]?.jsonPrimitive?.contentOrNull
        val status = when (rawStatus) {
            "currently_airing", "currently_publishing" -> "RELEASING"
            "finished_airing", "finished" -> "FINISHED"
            "not_yet_aired" -> "NOT_YET_RELEASED"
            else -> rawStatus?.replace("_", " ")?.uppercase()
        }

        val startSeason = media["start_season"].asJsonObjectOrNull
        val season = startSeason?.get("season")?.jsonPrimitive?.contentOrNull?.uppercase()
        val seasonYear = startSeason?.get("year")?.jsonPrimitive?.intOrNull

        val meanScore = media["mean"]?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
        val popularity = media["popularity"]?.jsonPrimitive?.intOrNull
        val rank = media["rank"]?.jsonPrimitive?.intOrNull
        val synopsis = media["synopsis"]?.jsonPrimitive?.contentOrNull?.htmlDecode()

        val genres = media["genres"].asJsonArrayOrNull?.mapNotNull {
            it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull
        } ?: emptyList()

        val tags = media["studios"].asJsonArrayOrNull?.mapNotNull {
            it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull
        } ?: emptyList()

        val relations = mutableListOf<DailyALRelation>()
        val relAnimeList = media["related_anime"].asJsonArrayOrNull ?: emptyList()
        relAnimeList.forEach { itemElem ->
            val item = itemElem.asJsonObjectOrNull ?: return@forEach
            val node = item["node"].asJsonObjectOrNull ?: return@forEach
            val relId = node["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
            val relTitle = node["title"]?.jsonPrimitive?.contentOrNull ?: "Related Anime"
            val relFormat = node["media_type"]?.jsonPrimitive?.contentOrNull?.replace("_", " ")?.uppercase()
            val relStatus = node["status"]?.jsonPrimitive?.contentOrNull
            val relCover = node["main_picture"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                ?: node["main_picture"].asJsonObjectOrNull?.get("medium")?.jsonPrimitive?.contentOrNull
            val relScore = node["mean"]?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
            val relType = item["relation_type_formatted"]?.jsonPrimitive?.contentOrNull ?: "RELATED"
            relations.add(
                DailyALRelation(
                    id = relId,
                    title = relTitle,
                    format = relFormat,
                    status = relStatus,
                    relationType = relType,
                    coverUrl = relCover,
                    meanScore = relScore,
                ),
            )
        }

        val recommendations = mutableListOf<DailyALRecommendation>()
        val recList = media["recommendations"].asJsonArrayOrNull ?: emptyList()
        recList.forEach { itemElem ->
            val item = itemElem.asJsonObjectOrNull ?: return@forEach
            val node = item["node"].asJsonObjectOrNull ?: return@forEach
            val recId = node["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
            val recTitle = node["title"]?.jsonPrimitive?.contentOrNull ?: "Recommended"
            val recCover = node["main_picture"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                ?: node["main_picture"].asJsonObjectOrNull?.get("medium")?.jsonPrimitive?.contentOrNull
            val recScore = node["mean"]?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
            val ratingCount = item["num_recommendations"]?.jsonPrimitive?.intOrNull ?: 0
            recommendations.add(
                DailyALRecommendation(
                    id = recId,
                    title = recTitle,
                    coverUrl = recCover,
                    meanScore = recScore,
                    ratingCount = ratingCount,
                ),
            )
        }

        return DailyALMediaDetails(
            id = id,
            title = title,
            romajiTitle = enTitle,
            nativeTitle = jaTitle,
            coverUrl = coverUrl,
            bannerUrl = bannerUrl,
            format = format,
            episodes = episodes,
            duration = null,
            status = status,
            season = season,
            seasonYear = seasonYear,
            meanScore = meanScore,
            popularity = popularity,
            rank = rank,
            synopsis = synopsis,
            genres = genres,
            tags = tags,
            trailerUrl = null,
            characters = emptyList(),
            staff = emptyList(),
            relations = relations,
            recommendations = recommendations,
            reviews = emptyList(),
        )
    }

    companion object {
        private const val CLIENT_ID = "18e5087eaeef557833a075a4d30d2afe"

        private const val BASE_OAUTH_URL = "https://myanimelist.net/v1/oauth2"
        private const val BASE_API_URL = "https://api.myanimelist.net/v2"

        private const val SEARCH_FIELDS =
            "id,title,synopsis,num_chapters,mean,main_picture,status,media_type,start_date,authors{first_name,last_name}"
        private const val SEARCH_FIELDS_ANIME =
            "id,title,synopsis,num_episodes,mean,main_picture,status,media_type,start_date,studios"

        private const val LIST_PAGINATION_AMOUNT = 250

        private var codeVerifier: String = ""

        fun authUrl(): Uri = "$BASE_OAUTH_URL/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("code_challenge", getPkceChallengeCode())
            .appendQueryParameter("response_type", "code")
            .build()

        fun mangaUrl(id: Long): Uri = "$BASE_API_URL/manga".toUri().buildUpon()
            .appendPath(id.toString())
            .appendPath("my_list_status")
            .build()

        fun animeUrl(id: Long): Uri = "$BASE_API_URL/anime".toUri().buildUpon()
            .appendPath(id.toString())
            .appendPath("my_list_status")
            .build()

        fun refreshTokenRequest(oauth: MALOAuth): Request {
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("refresh_token", oauth.refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            // Add the Authorization header manually as this particular
            // request is called by the interceptor itself so it doesn't reach
            // the part where the token is added automatically.
            val headers = Headers.Builder()
                .add("Authorization", "Bearer ${oauth.accessToken}")
                .build()

            return POST("$BASE_OAUTH_URL/token", body = formBody, headers = headers)
        }

        private fun getPkceChallengeCode(): String {
            codeVerifier = PkceUtil.generateCodeVerifier()
            return codeVerifier
        }
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

