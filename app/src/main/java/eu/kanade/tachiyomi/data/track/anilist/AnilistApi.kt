package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.animesource.model.Credit
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAddEntryResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAnimeMetadata
import eu.kanade.tachiyomi.data.track.anilist.dto.ALCurrentUserResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALProfileStatsResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserStats
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMangaMetadata
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import eu.kanade.tachiyomi.data.track.anilist.dto.ALSearchResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALSearchItem
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListEntryQueryResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserViewerData
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAnime
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.ui.discover.DailyALCharacterRole
import eu.kanade.tachiyomi.ui.discover.DailyALFilterState
import eu.kanade.tachiyomi.ui.discover.DailyALMediaDetails
import eu.kanade.tachiyomi.ui.discover.DailyALActivityEntry
import eu.kanade.tachiyomi.ui.discover.DailyALProfileStats
import eu.kanade.tachiyomi.ui.discover.DailyALUserListEntry
import eu.kanade.tachiyomi.ui.discover.DailyALRecommendation
import eu.kanade.tachiyomi.ui.discover.DailyALRelation
import eu.kanade.tachiyomi.ui.discover.DailyALReview
import eu.kanade.tachiyomi.ui.discover.DailyALStaffRole
import eu.kanade.tachiyomi.ui.discover.DailyALVoiceActor
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class AnilistApi(val client: OkHttpClient, val interceptor: AnilistInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    private val publicClient = client.newBuilder()
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    suspend fun addLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val query = $$"""
            |mutation AddManga($mangaId: Int, $progress: Int, $status: MediaListStatus, $private: Boolean) {
                |SaveMediaListEntry (mediaId: $mangaId, progress: $progress, status: $status, private: $private) {
                |   id
                |   status
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("mangaId", track.remote_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toApiStatus())
                    put("private", track.private)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAddEntryResult>()
                    .let {
                        track
                    }
            }
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val query = $$"""
            |mutation UpdateManga(
                |$listId: Int, $progress: Int, $status: MediaListStatus, $private: Boolean,
                |$score: Int, $startedAt: FuzzyDateInput, $completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: $listId, progress: $progress, status: $status, private: $private,
                    |scoreRaw: $score, startedAt: $startedAt, completedAt: $completedAt
                |) {
                    |id
                    |status
                    |progress
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.library_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toApiStatus())
                    put("score", track.score.toInt())
                    put("startedAt", createDate(track.started_reading_date))
                    put("completedAt", createDate(track.finished_reading_date))
                    put("private", track.private)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
            track
        }
    }

    suspend fun deleteLibManga(track: DomainMangaTrack) {
        withIOContext {
            val query = $$"""
            |mutation DeleteManga($listId: Int) {
                |DeleteMediaListEntry(id: $listId) {
                    |deleted
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.libraryId)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
        }
    }

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val query = """
            |mutation AddAnime(${'$'}animeId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean) {
                |SaveMediaListEntry (mediaId: ${'$'}animeId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private) {
                |   id
                |   status
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("animeId", track.remote_id)
                    put("progress", track.last_episode_seen.toInt())
                    put("status", track.toApiStatus())
                    put("private", track.private)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAddEntryResult>()
                    .let {
                        track
                    }
            }
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val query = """
            |mutation UpdateAnime(
                |${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}private: Boolean,
                |${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, private: ${'$'}private,
                    |scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt
                |) {
                    |id
                    |status
                    |progress
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.library_id)
                    put("progress", track.last_episode_seen.toInt())
                    put("status", track.toApiStatus())
                    put("score", track.score.toInt())
                    put("startedAt", createDate(track.started_watching_date))
                    put("completedAt", createDate(track.finished_watching_date))
                    put("private", track.private)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
            track
        }
    }

    /**
     * Upserts watch progress (and optionally score/status) for a media entry by its
     * AniList media id, without requiring an existing list entry id — AniList's
     * SaveMediaListEntry creates the entry when one doesn't exist yet.
     *
     * Used by the Discover profile progress editor.
     *
     * @return [Result.success] on a 2xx response, or the captured failure.
     */
    suspend fun updateMediaProgress(
        mediaId: Long,
        progress: Int,
        status: String? = null,
        score: Int? = null,
    ): Result<Unit> {
        return runCatching {
            withIOContext {
                val query = """
                |mutation UpsertProgress(${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}scoreRaw: Int) {
                    |SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status, scoreRaw: ${'$'}scoreRaw) {
                        |id
                        |progress
                        |status
                    |}
                |}
                |
                """.trimMargin()
                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("mediaId", mediaId)
                        put("progress", progress)
                        status?.let { put("status", it) }
                        score?.let { put("scoreRaw", it) }
                    }
                }
                authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                    .awaitSuccess()
            }
        }
    }

    suspend fun deleteLibAnime(track: DomainAnimeTrack) {
        return withIOContext {
            val query = """
            |mutation DeleteAnime(${'$'}listId: Int) {
                |DeleteMediaListEntry(id: ${'$'}listId) {
                    |deleted
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("listId", track.libraryId)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        return withIOContext {
            val query = $$"""
            |query Search($query: String) {
                |Page (perPage: 50) {
                    |media(search: $query, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |staff {
                            |edges {
                                |role
                                |id
                                |node {
                                    |name {
                                        |full
                                        |userPreferred
                                        |native
                                    |}
                                |}
                            |}
                        |}
                        |title {
                            |english
                            |romaji
                            |native
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |countryOfOrigin
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", search)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { it.toALManga().toTrack() }
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val query = """
            |query Search(${'$'}query: String) {
                |Page (perPage: 50) {
                    |media(search: ${'$'}query, type: ANIME) {
                        |id
                        |studios {
                            |edges {
                                |isMain
                                |node {
                                    |name
                                |}
                            |}
                        |}
                        |title {
                            |english
                            |romaji
                            |native
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |episodes
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", search)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { it.toALAnime().toTrack() }
            }
        }
    }

    suspend fun getTrendingAnime(page: Int): List<ALSearchItem> {
        return withIOContext {
            val query = """
            |query TrendingAnime(${'$'}page: Int) {
                |Page (page: ${'$'}page, perPage: 20) {
                    |media(type: ANIME, sort: [TRENDING_DESC, POPULARITY_DESC]) {
                        |id
                        |studios {
                            |edges {
                                |isMain
                                |node {
                                    |name
                                |}
                            |}
                        |}
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |episodes
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("page", page)
                }
            }
            with(json) {
                client.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
            }
        }
    }

    suspend fun getTrendingManga(page: Int): List<ALSearchItem> {
        return withIOContext {
            val query = $$"""
            |query TrendingManga($page: Int) {
                |Page (page: $page, perPage: 20) {
                    |media(type: MANGA, format_not_in: [NOVEL], sort: [TRENDING_DESC, POPULARITY_DESC]) {
                        |id
                        |staff {
                            |edges {
                                |role
                                |id
                                |node {
                                    |name {
                                        |full
                                        |userPreferred
                                        |native
                                    |}
                                }
                            |}
                        |}
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |countryOfOrigin
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("page", page)
                }
            }
            with(json) {
                client.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
            }
        }
    }

    suspend fun getAiringSchedule(start: Long, end: Long, page: Int = 1): List<eu.kanade.tachiyomi.data.track.model.AiringEpisode> {
        return withIOContext {
            val query = $$"""
            |query AiringSchedule($start: Int, $end: Int, $page: Int) {
                |Page (page: $page, perPage: 40) {
                    |airingSchedules(airingAt_greater: $start, airingAt_lesser: $end, sort: TIME) {
                        |id
                        |airingAt
                        |episode
                        |media {
                            |id
                            |title {
                                |userPreferred
                                |english
                                |romaji
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |episodes
                            |averageScore
                            |description
                        |}
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("start", start)
                    put("end", end)
                    put("page", page)
                }
            }
            with(json) {
                client.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<eu.kanade.tachiyomi.data.track.anilist.dto.ALAiringScheduleResult>()
                    .data.Page.airingSchedules
                    .map { it.toAiringEpisode() }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch? {
        return withIOContext {
            val query = $$"""
            |query Search($anime_id: Int) {
                |Page (perPage: 1) {
                    |media(id: $anime_id, type: ANIME) {
                        |id
                        |staff {
                            |edges {
                                |role
                                |id
                                |node {
                                    |name {
                                        |full
                                        |userPreferred
                                        |native
                                    |}
                                |}
                            |}
                        |}
                        |characters {
                            |edges {
                                |isMain
                                |node {
                                    |name
                                |}
                            |}
                        |}
                        |title {
                            |english
                            |romaji
                            |native
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |episodes
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()

            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("anime_id", id)
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .firstOrNull()
                    ?.toALAnime()
                    ?.toTrack()
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch? {
        return withIOContext {
            val query = $$"""
            |query Search($manga_id: Int) {
                |Page (perPage: 1) {
                    |media(id: $manga_id, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |staff {
                            |edges {
                                |role
                                |id
                                |node {
                                    |name {
                                        |full
                                        |userPreferred
                                        |native
                                    |}
                                |}
                            |}
                        |}
                        |title {
                            |english
                            |romaji
                            |native
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                        |averageScore
                    |}
                |}
            |}
            |
            """.trimMargin()

            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("manga_id", id)
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .firstOrNull()
                    ?.toALManga()
                    ?.toTrack()
            }
        }
    }

    suspend fun getPopularAnime(): List<AnimeTrackSearch> {
        return withIOContext {
            val query = """
            query PopularAnime {
                Page (perPage: 15) {
                    media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                        id
                        title {
                            english
                            romaji
                            native
                            userPreferred
                        }
                        coverImage {
                            large
                        }
                        format
                        status
                        episodes
                        description
                        averageScore
                        startDate {
                            year
                            month
                            day
                        }
                    }
                }
            }
            """.trimIndent()
            val payload = buildJsonObject {
                put("query", query)
            }
            with(json) {
                publicClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { item ->
                        AnimeTrackSearch.create(TrackerManager.ANILIST).apply {
                            remote_id = item.id
                            title = item.title.preferred
                            total_episodes = item.episodes ?: 0
                            cover_url = item.coverImage.large
                            summary = item.description?.htmlDecode() ?: ""
                            score = (item.averageScore ?: 0).toDouble()
                            tracking_url = AnilistApi.animeUrl(remote_id)
                            publishing_status = item.status ?: ""
                            publishing_type = item.format?.replace("_", "-") ?: ""
                        }
                    }
            }
        }
    }

    suspend fun getPopularManga(): List<MangaTrackSearch> {
        return withIOContext {
            val query = """
            query PopularManga {
                Page (perPage: 15) {
                    media(sort: TRENDING_DESC, type: MANGA, format_not_in: [NOVEL], isAdult: false) {
                        id
                        title {
                            english
                            romaji
                            native
                            userPreferred
                        }
                        coverImage {
                            large
                        }
                        format
                        status
                        chapters
                        description
                        averageScore
                        startDate {
                            year
                            month
                            day
                        }
                    }
                }
            }
            """.trimIndent()
            val payload = buildJsonObject {
                put("query", query)
            }
            with(json) {
                publicClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALSearchResult>()
                    .data.page.media
                    .map { item ->
                        MangaTrackSearch.create(TrackerManager.ANILIST).apply {
                            remote_id = item.id
                            title = item.title.preferred
                            total_chapters = item.chapters ?: 0
                            cover_url = item.coverImage.large
                            summary = item.description?.htmlDecode() ?: ""
                            score = (item.averageScore ?: 0).toDouble()
                            tracking_url = AnilistApi.mangaUrl(remote_id)
                            publishing_status = item.status ?: ""
                            publishing_type = item.format?.replace("_", "-") ?: ""
                        }
                    }
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack, userid: Int): MangaTrack? {
        return withIOContext {
            val query = $$"""
            |query ($id: Int!, $manga_id: Int!) {
                |Page {
                    |mediaList(userId: $id, type: MANGA, mediaId: $manga_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |private
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
                        |media {
                            |id
                            |title {
                                |english
                                |romaji
                                |native
                                |userPreferred
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |chapters
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                            |staff {
                                |edges {
                                    |role
                                    |id
                                    |node {
                                        |name {
                                            |full
                                            |userPreferred
                                            |native
                                        |}
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", userid)
                    put("manga_id", track.remote_id)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALUserListEntryQueryResult>()
                    .data.page.mediaList
                    .map { it.toALUserManga() }
                    .firstOrNull()
                    ?.toTrack()
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack, userid: Int): AnimeTrack? {
        return withIOContext {
            val query = """
            |query (${'$'}id: Int!, ${'$'}anime_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: ANIME, mediaId: ${'$'}anime_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |private
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
                        |media {
                            |id
                            |title {
                                |english
                                |romaji
                                |native
                                |userPreferred
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |episodes
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                            |studios {
                                |edges {
                                    |isMain
                                    |node {
                                        |name
                                    |}
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", userid)
                    put("anime_id", track.remote_id)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALUserListEntryQueryResult>()
                    .data.page.mediaList
                    .map { it.toALUserAnime() }
                    .firstOrNull()
                    ?.toTrack()
            }
        }
    }

    suspend fun getLibManga(track: MangaTrack, userId: Int): MangaTrack {
        return findLibManga(track, userId) ?: throw Exception("Could not find manga")
    }

    suspend fun getLibAnime(track: AnimeTrack, userId: Int): AnimeTrack {
        return findLibAnime(track, userId) ?: throw Exception("Could not find anime")
    }

    fun createOAuth(token: String): ALOAuth {
        return ALOAuth(token, "Bearer", System.currentTimeMillis() + 31536000000, 31536000000)
    }

    suspend fun getCurrentUser(): ALUserViewerData {
        return withIOContext {
            val query = """
            |query User {
                |Viewer {
                    |id
                    |name
                    |mediaListOptions {
                        |scoreFormat
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALCurrentUserResult>()
                    .data.viewer
            }
        }
    }

    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata {
        return withIOContext {
            val query = """
            |query (${'$'}animeid: Int!) {
                |Media (id: ${'$'}animeid) {
                    |id
                    |title {
                        |english
                        |romaji
                        |native
                        |userPreferred
                    |}
                    |coverImage {
                        |large
                    |}
                    |description
                    |studios {
                        |edges {
                           |isMain
                           |node {
                             |name
                           |}
                        |}
                    |}
                    |staff {
                        |edges {
                            |role
                            |node {
                                |name {
                                    |userPreferred
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("animeid", track.remoteId)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAnimeMetadata>()
                    .let { it ->
                        val media = it.data.media
                        TrackAnimeMetadata(
                            remoteId = media.id,
                            title = media.title.preferred,
                            thumbnailUrl = media.coverImage.large,
                            description = media.description?.htmlDecode()?.ifEmpty { null },
                            authors = media.staff.edges
                                .filter { it.role == "Original Creator" }
                                .joinToString(", ") { it.node.name.userPreferred }
                                .ifEmpty { null },
                            artists = media.studios.edges
                                .filter { it.isMain }
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") { it.node.name }
                                ?: media.studios.edges.joinToString(", ") { it.node.name },
                        )
                    }
            }
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            val query = """
            |query (${'$'}mangaId: Int!) {
                |Media (id: ${'$'}mangaId) {
                    |id
                    |title {
                        |english
                        |romaji
                        |native
                        |userPreferred
                    |}
                    |coverImage {
                        |large
                    |}
                    |description
                    |staff {
                        |edges {
                            |role
                            |node {
                                |name {
                                    |userPreferred
                                |}
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("mangaId", track.remoteId)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALMangaMetadata>()
                    .let { it ->
                        val media = it.data.media
                        TrackMangaMetadata(
                            remoteId = media.id,
                            title = media.title.preferred,
                            thumbnailUrl = media.coverImage.large,
                            description = media.description?.htmlDecode()?.ifEmpty { null },
                            authors = media.staff.edges
                                .filter { it.role == "Story" || it.role == "Story & Art" }
                                .joinToString(", ") { it.node.name.userPreferred.toString() }
                                .ifEmpty { null },
                            artists = media.staff.edges
                                .filter { it.role == "Art" || it.role == "Story & Art" }
                                .joinToString(", ") { it.node.name.userPreferred.toString() }
                                .ifEmpty { null },
                        )
                    }
            }
        }
    }

    /**
     * Fetch cast (characters with voice actors and staff) from AniList by title search.
     * Returns null on error or empty results.
     */
    suspend fun fetchCastByTitle(title: String?): List<Credit>? {
        if (title.isNullOrBlank()) return null

        return withIOContext {
            try {
                val query = """
                |query SearchCast(${'$'}query: String) {
                    |Page (perPage: 10) {
                        |media(search: ${'$'}query, type: ANIME) {
                            |id
                            |characters {
                                |edges {
                                    |role
                                    |node {
                                        |name { userPreferred }
                                        |image { large medium }
                                    |}
                                    |voiceActors {
                                        |name { userPreferred }
                                        |language
                                        |image { large medium }
                                    |}
                                |}
                            |}
                            |staff {
                                |edges {
                                    |role
                                    |node { name { userPreferred } image { large medium } }
                                |}
                            |}
                        |}
                    |}
                |}
                |
                """.trimMargin()

                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("query", title)
                    }
                }

                val credits = mutableListOf<Credit>()
                try {
                    val search = client.newCall(
                        POST(API_URL, body = payload.toString().toRequestBody(jsonMime)),
                    ).execute()
                    val bodyStr = search.body.string()
                    // Use kotlinx.serialization Json object parsing
                    val parsed = json.parseToJsonElement(bodyStr).jsonObject
                    val page = parsed["data"]?.jsonObject?.get("Page")?.jsonObject
                    val mediaArr = page?.get("media")?.jsonArray ?: return@withIOContext null
                    if (mediaArr.isEmpty()) return@withIOContext null
                    val first = mediaArr[0].jsonObject

                    // Parse characters
                    val characters = first["characters"]?.jsonObject
                        ?.get("edges")?.jsonArray
                    characters?.forEach { edgeEl ->
                        val edge = edgeEl.jsonObject
                        val node = edge["node"]?.jsonObject
                        val charName = node?.get("name")?.jsonObject
                            ?.get("userPreferred")?.toString()?.trim('"')
                        val charImage = node?.get("image")?.jsonObject
                            ?.get("large")?.toString()?.trim('"')
                        val vas = edge["voiceActors"]?.jsonArray
                        val vaNames = mutableListOf<String>()
                        var vaImage: String? = null
                        vas?.forEach { vaEl ->
                            val va = vaEl.jsonObject
                            val vaName = va["name"]?.jsonObject?.get("userPreferred")?.toString()?.trim('"')
                            val vaLang = va["language"]?.toString()?.trim('"')
                            if (!vaName.isNullOrBlank()) {
                                vaNames.add(if (!vaLang.isNullOrBlank()) "$vaName ($vaLang)" else vaName)
                            }
                            if (vaImage == null) {
                                vaImage = va["image"]?.jsonObject?.get("large")?.toString()?.trim('"')
                            }
                        }
                        val roleText = vaNames.joinToString(", ")
                        var finalImage: String? = charImage ?: vaImage
                        if (!finalImage.isNullOrBlank() && finalImage.startsWith("//")) {
                            finalImage = "https:$finalImage"
                        }
                        if (!charName.isNullOrBlank()) {
                            credits.add(
                                Credit(
                                    name = charName,
                                    role = roleText.ifBlank { null },
                                    character = charName,
                                    image_url = finalImage,
                                ),
                            )
                        }
                    }

                    // Parse staff
                    val staff = first["staff"]?.jsonObject?.get("edges")?.jsonArray
                    staff?.forEach { stEl ->
                        val sedge = stEl.jsonObject
                        val srole = sedge["role"]?.toString()?.trim('"')
                        val snode = sedge["node"]?.jsonObject
                        val sname = snode?.get(
                            "name",
                        )?.jsonObject?.get("userPreferred")?.toString()?.trim('"')
                        var sImage = snode?.get("image")?.jsonObject?.get("large")?.toString()?.trim('"')
                        if (!sImage.isNullOrBlank() && sImage.startsWith("//")) sImage = "https:$sImage"
                        if (!sname.isNullOrBlank()) {
                            credits.add(Credit(name = sname, role = srole, image_url = sImage))
                        }
                    }

                    credits.ifEmpty { null }
                } catch (_: Exception) {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun getProfileStats(userId: Int): ALUserStats {
        return withIOContext {
            val query = """
            |query (${'$'}userId: Int) {
                |User(id: ${'$'}userId) {
                    |id
                    |name
                    |avatar {
                        |large
                    |}
                    |bannerImage
                    |statistics {
                        |anime {
                            |count
                            |episodesWatched
                            |minutesWatched
                            |meanScore
                        |}
                        |manga {
                            |count
                            |chaptersRead
                            |volumesRead
                            |meanScore
                        |}
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("userId", userId)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALProfileStatsResult>()
                    .data.user
            }
        }
    }

    /**
     * Retrieves comprehensive deep media details from AniList ported from DailyAL's rich media graph.
     *
     * Constructs a single round-trip GraphQL query requesting core media metadata, characters with
     * voice actors, production staff, relations, recommendations, reviews, and trailer.
     *
     * @param mediaId AniList media identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val query = """
                |query DeepMediaDetails(${'$'}id: Int, ${'$'}type: MediaType) {
                |  Media(id: ${'$'}id, type: ${'$'}type) {
                |    id
                |    title {
                |      userPreferred
                |      romaji
                |      english
                |      native
                |    }
                |    coverImage {
                |      extraLarge
                |      large
                |    }
                |    bannerImage
                |    format
                |    status
                |    episodes
                |    duration
                |    season
                |    seasonYear
                |    meanScore
                |    averageScore
                |    popularity
                |    rankings {
                |      rank
                |      allTime
                |      type
                |    }
                |    description(asHtml: false)
                |    genres
                |    tags {
                |      name
                |    }
                |    trailer {
                |      id
                |      site
                |    }
                |    characters(sort: [ROLE, RELEVANCE, ID], perPage: 25) {
                |      edges {
                |        role
                |        node {
                |          id
                |          name {
                |            userPreferred
                |            full
                |            native
                |          }
                |          image {
                |            large
                |          }
                |        }
                |        voiceActors(sort: [RELEVANCE, ID]) {
                |          id
                |          name {
                |            userPreferred
                |            full
                |            native
                |          }
                |          language
                |          image {
                |            large
                |          }
                |        }
                |      }
                |    }
                |    staff(sort: [RELEVANCE, ID], perPage: 25) {
                |      edges {
                |        role
                |        node {
                |          id
                |          name {
                |            userPreferred
                |            full
                |          }
                |          image {
                |            large
                |          }
                |        }
                |      }
                |    }
                |    relations {
                |      edges {
                |        relationType(version: 2)
                |        node {
                |          id
                |          title {
                |            userPreferred
                |            romaji
                |            english
                |          }
                |          format
                |          status
                |          coverImage {
                |            large
                |          }
                |          meanScore
                |        }
                |      }
                |    }
                |    recommendations(sort: [RATING_DESC], perPage: 15) {
                |      nodes {
                |        id
                |        rating
                |        mediaRecommendation {
                |          id
                |          title {
                |            userPreferred
                |            romaji
                |            english
                |          }
                |          coverImage {
                |            large
                |          }
                |          meanScore
                |        }
                |      }
                |    }
                |    reviews(sort: [RATING_DESC], perPage: 10) {
                |      nodes {
                |        id
                |        summary
                |        body(asHtml: false)
                |        score
                |        rating
                |        user {
                |          name
                |          avatar {
                |            large
                |          }
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("id", mediaId.toInt())
                        put("type", if (isAnime) "ANIME" else "MANGA")
                    }
                }

                val okClient = try {
                    if (interceptor.anilist.isLoggedIn) authClient else publicClient
                } catch (_: Exception) {
                    publicClient
                }

                val responseBody = okClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess().body.string()

                val jsonElement = json.parseToJsonElement(responseBody)
                val mediaObj = jsonElement.jsonObject["data"].asJsonObjectOrNull?.get("Media").asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in AniList response for id=$mediaId")

                parseDeepMediaDetails(mediaObj, mediaId)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities.
     *
     * Supports filtering by free-text keywords, genre inclusion/exclusion, format, status, season,
     * release year, minimum score threshold, and sorting criteria.
     *
     * @param filter The active [DailyALFilterState] specifying search constraints.
     * @param page Page index for paginated browsing (1-based).
     * @return [Result] containing list of [ALAnime] entries, or error on failure.
     */
    suspend fun searchAdvanced(filter: DailyALFilterState, page: Int = 1): Result<List<ALAnime>> {
        return withIOContext {
            runCatching {
                val variableDefs = mutableListOf(
                    "${'$'}page: Int",
                    "${'$'}perPage: Int",
                )
                val mediaArgs = mutableListOf(
                    "type: ANIME",
                )

                if (filter.query.isNotBlank()) {
                    variableDefs.add("${'$'}search: String")
                    mediaArgs.add("search: ${'$'}search")
                }
                if (filter.includedGenres.isNotEmpty()) {
                    variableDefs.add("${'$'}genre_in: [String]")
                    mediaArgs.add("genre_in: ${'$'}genre_in")
                }
                if (filter.excludedGenres.isNotEmpty()) {
                    variableDefs.add("${'$'}genre_not_in: [String]")
                    mediaArgs.add("genre_not_in: ${'$'}genre_not_in")
                }
                if (!filter.format.isNullOrBlank()) {
                    variableDefs.add("${'$'}format_in: [MediaFormat]")
                    mediaArgs.add("format_in: ${'$'}format_in")
                }
                if (!filter.status.isNullOrBlank()) {
                    variableDefs.add("${'$'}status_in: [MediaStatus]")
                    mediaArgs.add("status_in: ${'$'}status_in")
                }
                if (!filter.season.isNullOrBlank()) {
                    variableDefs.add("${'$'}season: MediaSeason")
                    mediaArgs.add("season: ${'$'}season")
                }
                if (filter.seasonYear != null && filter.seasonYear > 0) {
                    variableDefs.add("${'$'}seasonYear: Int")
                    mediaArgs.add("seasonYear: ${'$'}seasonYear")
                }
                if (filter.minScore != null && filter.minScore > 0) {
                    variableDefs.add("${'$'}averageScore_greater: Int")
                    mediaArgs.add("averageScore_greater: ${'$'}averageScore_greater")
                }
                variableDefs.add("${'$'}sort: [MediaSort]")
                mediaArgs.add("sort: ${'$'}sort")

                val query = """
                |query AdvancedSearch(${variableDefs.joinToString(", ")}) {
                |  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                |    media(${mediaArgs.joinToString(", ")}) {
                |      id
                |      title {
                |        userPreferred
                |        romaji
                |        english
                |        native
                |      }
                |      coverImage {
                |        large
                |      }
                |      description
                |      format
                |      status
                |      startDate {
                |        year
                |        month
                |        day
                |      }
                |      episodes
                |      averageScore
                |      studios(isMain: true) {
                |        edges {
                |          isMain
                |          node {
                |            name
                |          }
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("page", page)
                        put("perPage", 25)
                        if (filter.query.isNotBlank()) {
                            put("search", filter.query.trim())
                        }
                        if (filter.includedGenres.isNotEmpty()) {
                            put(
                                "genre_in",
                                kotlinx.serialization.json.JsonArray(
                                    filter.includedGenres.map { kotlinx.serialization.json.JsonPrimitive(it) },
                                ),
                            )
                        }
                        if (filter.excludedGenres.isNotEmpty()) {
                            put(
                                "genre_not_in",
                                kotlinx.serialization.json.JsonArray(
                                    filter.excludedGenres.map { kotlinx.serialization.json.JsonPrimitive(it) },
                                ),
                            )
                        }
                        if (!filter.format.isNullOrBlank()) {
                            val formatVal = filter.format.replace("-", "_").uppercase()
                            put(
                                "format_in",
                                kotlinx.serialization.json.JsonArray(
                                    listOf(kotlinx.serialization.json.JsonPrimitive(formatVal)),
                                ),
                            )
                        }
                        if (!filter.status.isNullOrBlank()) {
                            val statusVal = filter.status.uppercase()
                            put(
                                "status_in",
                                kotlinx.serialization.json.JsonArray(
                                    listOf(kotlinx.serialization.json.JsonPrimitive(statusVal)),
                                ),
                            )
                        }
                        if (!filter.season.isNullOrBlank()) {
                            put("season", filter.season.uppercase())
                        }
                        if (filter.seasonYear != null && filter.seasonYear > 0) {
                            put("seasonYear", filter.seasonYear)
                        }
                        if (filter.minScore != null && filter.minScore > 0) {
                            put("averageScore_greater", filter.minScore)
                        }
                        val sortEnum = when (filter.sortBy.uppercase()) {
                            "SCORE_DESC" -> "SCORE_DESC"
                            "TRENDING_DESC" -> "TRENDING_DESC"
                            "START_DATE_DESC" -> "START_DATE_DESC"
                            "TITLE_ROMAJI" -> "TITLE_ROMAJI"
                            else -> "POPULARITY_DESC"
                        }
                        put(
                            "sort",
                            kotlinx.serialization.json.JsonArray(
                                listOf(kotlinx.serialization.json.JsonPrimitive(sortEnum)),
                            ),
                        )
                    }
                }

                with(json) {
                    publicClient.newCall(
                        POST(
                            API_URL,
                            body = payload.toString().toRequestBody(jsonMime),
                        ),
                    )
                        .awaitSuccess()
                        .parseAs<ALSearchResult>()
                        .data.page.media
                        .map { it.toALAnime() }
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing advanced search with query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated AniList user.
     *
     * Ported from DailyAL's user profile dashboard to display total watch time, mean score,
     * status breakdown, and score distribution histogram.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val query = """
                |query CurrentUserStats {
                |  Viewer {
                |    id
                |    name
                |    avatar {
                |      large
                |    }
                |    bannerImage
                |    statistics {
                |      anime {
                |        count
                |        meanScore
                |        minutesWatched
                |        episodesWatched
                |        statuses {
                |          status
                |          count
                |        }
                |        scores {
                |          score
                |          count
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

                val payload = buildJsonObject {
                    put("query", query)
                }

                val responseBody = authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess().body.string()

                val jsonElement = json.parseToJsonElement(responseBody)
                val viewer = jsonElement.jsonObject["data"].asJsonObjectOrNull?.get("Viewer").asJsonObjectOrNull
                    ?: throw IllegalStateException("Viewer data missing or user not authenticated with AniList")

                val userName = viewer["name"]?.jsonPrimitive?.contentOrNull ?: "AniList User"
                val avatarUrl = viewer["avatar"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                val bannerUrl = viewer["bannerImage"]?.jsonPrimitive?.contentOrNull

                val animeStats = viewer["statistics"].asJsonObjectOrNull?.get("anime").asJsonObjectOrNull
                val count = animeStats?.get("count")?.jsonPrimitive?.intOrNull ?: 0
                val meanScore = animeStats?.get("meanScore")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val minutesWatched = animeStats?.get("minutesWatched")?.jsonPrimitive?.longOrNull ?: 0L
                val daysWatched = if (minutesWatched > 0) {
                    (minutesWatched / 1440.0 * 10.0).roundToLong() / 10.0
                } else {
                    0.0
                }
                val episodesWatched = animeStats?.get("episodesWatched")?.jsonPrimitive?.intOrNull ?: 0

                val statusMap = mutableMapOf<String, Int>()
                animeStats?.get("statuses").asJsonArrayOrNull?.forEach { elem ->
                    val sObj = elem.asJsonObjectOrNull ?: return@forEach
                    val status = sObj["status"]?.jsonPrimitive?.contentOrNull
                    val c = sObj["count"]?.jsonPrimitive?.intOrNull ?: 0
                    if (status != null) {
                        statusMap[status] = c
                    }
                }

                val scoreMap = mutableMapOf<Int, Int>()
                animeStats?.get("scores").asJsonArrayOrNull?.forEach { elem ->
                    val sObj = elem.asJsonObjectOrNull ?: return@forEach
                    val score = sObj["score"]?.jsonPrimitive?.intOrNull
                    val c = sObj["count"]?.jsonPrimitive?.intOrNull ?: 0
                    if (score != null) {
                        scoreMap[score] = c
                    }
                }

                DailyALProfileStats(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    bannerUrl = bannerUrl,
                    animeDaysWatched = daysWatched,
                    meanScore = meanScore,
                    totalEpisodes = episodesWatched,
                    totalEntries = count,
                    statusDistribution = statusMap,
                    scoreDistribution = scoreMap,
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from AniList" }
            }
        }
    }

    /**
     * Fetches a slice of the authenticated user's anime list for a given status
     * (CURRENT, PLANNING, COMPLETED, PAUSED, DROPPED), most recently updated first.
     *
     * Powers the Discover profile's "My Lists" browser with inline editing.
     */
    suspend fun getUserAnimeList(status: String): Result<List<DailyALUserListEntry>> =
        getUserList(status, isAnime = true)

    /**
     * Fetches a slice of the authenticated user's manga list for a given status,
     * most recently updated first.
     */
    suspend fun getUserMangaList(status: String): Result<List<DailyALUserListEntry>> =
        getUserList(status, isAnime = false)

    private suspend fun getUserList(status: String, isAnime: Boolean): Result<List<DailyALUserListEntry>> {
        return withIOContext {
            runCatching {
                val query = """
                |query UserList(${'$'}status: MediaListStatus, ${'$'}type: MediaType) {
                |  Viewer {
                |    mediaListCollection(status: ${'$'}status, type: ${'$'}type, sort: UPDATED_TIME_DESC) {
                |      entries {
                |        id
                |        score(format: POINT_100)
                |        progress
                |        status
                |        media {
                |          id
                |          title { romaji }
                |          coverImage { medium }
                |          format
                |          status
                |          averageScore
                |          episodes
                |          chapters
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()
                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("status", status)
                        put("type", if (isAnime) "ANIME" else "MANGA")
                    }
                }

                val responseBody = authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess().body.string()

                val jsonElement = json.parseToJsonElement(responseBody)
                val collection = jsonElement.jsonObject["data"].asJsonObjectOrNull
                    ?.get("Viewer").asJsonObjectOrNull
                    ?.get("mediaListCollection").asJsonObjectOrNull
                    ?: return@runCatching emptyList()

                collection["entries"].asJsonArrayOrNull.orEmpty().mapNotNull { elem ->
                    val entry = elem.asJsonObjectOrNull ?: return@mapNotNull null
                    val media = entry["media"].asJsonObjectOrNull ?: return@mapNotNull null
                    val total = if (isAnime) {
                        media["episodes"]?.jsonPrimitive?.intOrNull
                    } else {
                        media["chapters"]?.jsonPrimitive?.intOrNull
                    }
                    DailyALUserListEntry(
                        entryId = entry["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        mediaId = media["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        title = media["title"].asJsonObjectOrNull?.get("romaji")
                            ?.jsonPrimitive?.contentOrNull
                            ?: media["title"].asJsonObjectOrNull?.get("userPreferred")
                                ?.jsonPrimitive?.contentOrNull
                            ?: "Unknown",
                        coverUrl = media["coverImage"].asJsonObjectOrNull?.get("medium")
                            ?.jsonPrimitive?.contentOrNull,
                        scoreRaw = entry["score"]?.jsonPrimitive?.intOrNull ?: 0,
                        progress = entry["progress"]?.jsonPrimitive?.intOrNull ?: 0,
                        status = entry["status"]?.jsonPrimitive?.contentOrNull ?: status,
                        format = media["format"]?.jsonPrimitive?.contentOrNull,
                        mediaStatus = media["status"]?.jsonPrimitive?.contentOrNull,
                        averageScore = media["averageScore"]?.jsonPrimitive?.intOrNull ?: -1,
                        totalEpisodes = total ?: 0,
                        isManga = !isAnime,
                    )
                }
            }.onFailure { e ->
                logcat(
                    LogPriority.ERROR,
                    e,
                ) { "DailyAL: Failed to retrieve user ${if (isAnime) "anime" else "manga"} list from AniList" }
            }
        }
    }

    /**
     * Fetches the authenticated user's recent list activity (watching/reading
     * updates), newest first, for the Discover profile's activity feed.
     */
    suspend fun getUserActivityFeed(): Result<List<DailyALActivityEntry>> {
        return withIOContext {
            runCatching {
                val viewerId = getCurrentUser().id
                val query = """
                |query UserActivity(${'$'}userId: Int) {
                |  Page(perPage: 15) {
                |    activities(userId: ${'$'}userId, sort: ID_DESC) {
                |      ... on ListActivity {
                |        status
                |        progress
                |        createdAt
                |        media {
                |          id
                |          title { romaji }
                |          coverImage { medium }
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()
                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("userId", viewerId)
                    }
                }

                val responseBody = authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess().body.string()

                val jsonElement = json.parseToJsonElement(responseBody)
                val page = jsonElement.jsonObject["data"].asJsonObjectOrNull
                    ?.get("Page").asJsonObjectOrNull
                    ?: return@runCatching emptyList()

                page["activities"].asJsonArrayOrNull.orEmpty().mapNotNull { elem ->
                    val activity = elem.asJsonObjectOrNull ?: return@mapNotNull null
                    val media = activity["media"].asJsonObjectOrNull ?: return@mapNotNull null
                    val status = activity["status"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    DailyALActivityEntry(
                        mediaId = media["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                        mediaTitle = media["title"].asJsonObjectOrNull?.get("romaji")
                            ?.jsonPrimitive?.contentOrNull ?: "Unknown",
                        mediaCoverUrl = media["coverImage"].asJsonObjectOrNull?.get("medium")
                            ?.jsonPrimitive?.contentOrNull,
                        status = status,
                        progress = activity["progress"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        createdAt = activity["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                    )
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user activity feed from AniList" }
            }
        }
    }

    private fun parseDeepMediaDetails(media: JsonObject, requestedId: Long): DailyALMediaDetails {
        val id = media["id"]?.jsonPrimitive?.longOrNull ?: requestedId
        val titleObj = media["title"].asJsonObjectOrNull
        val userPreferred = titleObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
        val english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
        val romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
        val native = titleObj?.get("native")?.jsonPrimitive?.contentOrNull
        val displayTitle = userPreferred ?: english ?: romaji ?: native ?: "Unknown"

        val coverImg = media["coverImage"].asJsonObjectOrNull
        val coverUrl = coverImg?.get("extraLarge")?.jsonPrimitive?.contentOrNull
            ?: coverImg?.get("large")?.jsonPrimitive?.contentOrNull
        val bannerUrl = media["bannerImage"]?.jsonPrimitive?.contentOrNull
        val format = media["format"]?.jsonPrimitive?.contentOrNull
        val status = media["status"]?.jsonPrimitive?.contentOrNull
        val episodes = media["episodes"]?.jsonPrimitive?.intOrNull
        val duration = media["duration"]?.jsonPrimitive?.intOrNull
        val season = media["season"]?.jsonPrimitive?.contentOrNull
        val seasonYear = media["seasonYear"]?.jsonPrimitive?.intOrNull
        val meanScore = media["meanScore"]?.jsonPrimitive?.intOrNull
            ?: media["averageScore"]?.jsonPrimitive?.intOrNull
        val popularity = media["popularity"]?.jsonPrimitive?.intOrNull

        val rank = media["rankings"].asJsonArrayOrNull?.firstOrNull().asJsonObjectOrNull?.get("rank")?.jsonPrimitive?.intOrNull
        val synopsis = media["description"]?.jsonPrimitive?.contentOrNull?.htmlDecode()
        val genres = media["genres"].asJsonArrayOrNull?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val tags = media["tags"].asJsonArrayOrNull?.mapNotNull { it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull } ?: emptyList()

        val trailerObj = media["trailer"].asJsonObjectOrNull
        val trailerId = trailerObj?.get("id")?.jsonPrimitive?.contentOrNull
        val trailerSite = trailerObj?.get("site")?.jsonPrimitive?.contentOrNull
        val trailerUrl = when {
            trailerId == null -> null
            trailerSite.equals("youtube", ignoreCase = true) -> "https://www.youtube.com/watch?v=$trailerId"
            trailerSite.equals("dailymotion", ignoreCase = true) -> "https://www.dailymotion.com/video/$trailerId"
            else -> trailerId
        }

        // Characters
        val characters = media["characters"].asJsonObjectOrNull?.get("edges").asJsonArrayOrNull?.mapNotNull { edgeElem ->
            val edge = edgeElem.asJsonObjectOrNull ?: return@mapNotNull null
            val role = edge["role"]?.jsonPrimitive?.contentOrNull ?: "SUPPORTING"
            val charNode = edge["node"].asJsonObjectOrNull ?: return@mapNotNull null
            val charId = charNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val charNameObj = charNode["name"].asJsonObjectOrNull
            val charName = charNameObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                ?: charNameObj?.get("full")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Character"
            val charNative = charNameObj?.get("native")?.jsonPrimitive?.contentOrNull
            val charImage = charNode["image"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull

            val voiceActors = edge["voiceActors"].asJsonArrayOrNull?.mapNotNull { vaElem ->
                val vaNode = vaElem.asJsonObjectOrNull ?: return@mapNotNull null
                val vaId = vaNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                val vaNameObj = vaNode["name"].asJsonObjectOrNull
                val vaName = vaNameObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                    ?: vaNameObj?.get("full")?.jsonPrimitive?.contentOrNull
                    ?: "Unknown Voice Actor"
                val vaNative = vaNameObj?.get("native")?.jsonPrimitive?.contentOrNull
                val vaLang = vaNode["language"]?.jsonPrimitive?.contentOrNull ?: "Japanese"
                val vaImage = vaNode["image"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
                DailyALVoiceActor(
                    id = vaId,
                    name = vaName,
                    nativeName = vaNative,
                    language = vaLang,
                    imageUrl = vaImage,
                )
            } ?: emptyList()

            DailyALCharacterRole(
                id = charId,
                name = charName,
                nativeName = charNative,
                imageUrl = charImage,
                role = role,
                voiceActors = voiceActors,
            )
        } ?: emptyList()

        // Staff
        val staff = media["staff"].asJsonObjectOrNull?.get("edges").asJsonArrayOrNull?.mapNotNull { edgeElem ->
            val edge = edgeElem.asJsonObjectOrNull ?: return@mapNotNull null
            val role = edge["role"]?.jsonPrimitive?.contentOrNull ?: "Staff"
            val staffNode = edge["node"].asJsonObjectOrNull ?: return@mapNotNull null
            val staffId = staffNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val staffNameObj = staffNode["name"].asJsonObjectOrNull
            val staffName = staffNameObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                ?: staffNameObj?.get("full")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Staff"
            val staffImage = staffNode["image"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
            DailyALStaffRole(
                id = staffId,
                name = staffName,
                role = role,
                imageUrl = staffImage,
            )
        } ?: emptyList()

        // Relations
        val relations = media["relations"].asJsonObjectOrNull?.get("edges").asJsonArrayOrNull?.mapNotNull { edgeElem ->
            val edge = edgeElem.asJsonObjectOrNull ?: return@mapNotNull null
            val relationType = edge["relationType"]?.jsonPrimitive?.contentOrNull ?: "RELATED"
            val relNode = edge["node"].asJsonObjectOrNull ?: return@mapNotNull null
            val relId = relNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val relTitleObj = relNode["title"].asJsonObjectOrNull
            val relTitle = relTitleObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                ?: relTitleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                ?: relTitleObj?.get("english")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Related"
            val relFormat = relNode["format"]?.jsonPrimitive?.contentOrNull
            val relStatus = relNode["status"]?.jsonPrimitive?.contentOrNull
            val relCover = relNode["coverImage"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
            val relScore = relNode["meanScore"]?.jsonPrimitive?.intOrNull
            DailyALRelation(
                id = relId,
                title = relTitle,
                format = relFormat,
                status = relStatus,
                relationType = relationType,
                coverUrl = relCover,
                meanScore = relScore,
            )
        } ?: emptyList()

        // Recommendations
        val recommendations = media["recommendations"].asJsonObjectOrNull?.get("nodes").asJsonArrayOrNull?.mapNotNull { nodeElem ->
            val recNode = nodeElem.asJsonObjectOrNull ?: return@mapNotNull null
            val rating = recNode["rating"]?.jsonPrimitive?.intOrNull ?: 0
            val recMedia = recNode["mediaRecommendation"].asJsonObjectOrNull ?: return@mapNotNull null
            val recMediaId = recMedia["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val recTitleObj = recMedia["title"].asJsonObjectOrNull
            val recTitle = recTitleObj?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                ?: recTitleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                ?: recTitleObj?.get("english")?.jsonPrimitive?.contentOrNull
                ?: "Recommended Media"
            val recCover = recMedia["coverImage"].asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
            val recScore = recMedia["meanScore"]?.jsonPrimitive?.intOrNull
            DailyALRecommendation(
                id = recMediaId,
                title = recTitle,
                coverUrl = recCover,
                meanScore = recScore,
                ratingCount = rating,
            )
        } ?: emptyList()

        // Reviews
        val reviews = media["reviews"].asJsonObjectOrNull?.get("nodes").asJsonArrayOrNull?.mapNotNull { nodeElem ->
            val revNode = nodeElem.asJsonObjectOrNull ?: return@mapNotNull null
            val revId = revNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val summary = revNode["summary"]?.jsonPrimitive?.contentOrNull
            val body = revNode["body"]?.jsonPrimitive?.contentOrNull ?: ""
            val score = revNode["score"]?.jsonPrimitive?.intOrNull ?: 0
            val rating = revNode["rating"]?.jsonPrimitive?.intOrNull ?: 0
            val userObj = revNode["user"].asJsonObjectOrNull
            val userName = userObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "Anonymous"
            val userAvatar = userObj?.get("avatar").asJsonObjectOrNull?.get("large")?.jsonPrimitive?.contentOrNull
            DailyALReview(
                id = revId,
                summary = summary,
                body = body,
                score = score,
                rating = rating,
                userName = userName,
                userAvatarUrl = userAvatar,
            )
        } ?: emptyList()

        return DailyALMediaDetails(
            id = id,
            title = displayTitle,
            romajiTitle = romaji,
            nativeTitle = native,
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
            tags = tags,
            trailerUrl = trailerUrl,
            characters = characters,
            staff = staff,
            relations = relations,
            recommendations = recommendations,
            reviews = reviews,
        )
    }

    private fun createDate(dateValue: Long): JsonObject {
        if (dateValue == 0L) {
            return buildJsonObject {
                put("year", JsonNull)
                put("month", JsonNull)
                put("day", JsonNull)
            }
        }

        val dateTime = Instant.fromEpochMilliseconds(dateValue).toLocalDateTime(TimeZone.currentSystemDefault())
        return buildJsonObject {
            put("year", dateTime.year)
            put("month", dateTime.month.number)
            put("day", dateTime.day)
        }
    }

    companion object {
        private const val CLIENT_ID = "18376"
        private const val API_URL = "https://graphql.anilist.co/"
        private const val BASE_URL = "https://anilist.co/api/v2/"
        private const val BASE_MANGA_URL = "https://anilist.co/manga/"
        private const val BASE_ANIME_URL = "https://anilist.co/anime/"

        fun mangaUrl(mediaId: Long): String {
            return BASE_MANGA_URL + mediaId
        }

        fun animeUrl(mediaId: Long): String {
            return BASE_ANIME_URL + mediaId
        }

        fun authUrl(): Uri = "${BASE_URL}oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray
