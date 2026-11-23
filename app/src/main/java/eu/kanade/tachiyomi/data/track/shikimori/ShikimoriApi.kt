package eu.kanade.tachiyomi.data.track.shikimori

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMAnimeSearchResult
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMAnimeUserListResult
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMLibraryIdResponse
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMSearchResult
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUser
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUserListResult
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUserResult
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
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
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack as DomainMangaTrack

class ShikimoriApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: ShikimoriInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val shikimoriRateLimiter = Mutex()
    private var lastShikimoriTimestamp = 0L

    private suspend fun throttleShikimori() {
        shikimoriRateLimiter.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastShikimoriTimestamp
            if (elapsed < 300L) {
                delay(300L - elapsed)
            }
            lastShikimoriTimestamp = System.currentTimeMillis()
        }
    }

    private suspend fun executeShikimoriCall(url: String, requireAuth: Boolean = false): String {
        throttleShikimori()
        val request = GET(url, headers = headersOf("User-Agent", "Animetail"))
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
            with(json) {
                val payload = buildJsonObject {
                    putJsonObject("user_rate") {
                        put("user_id", userId)
                        put("target_id", track.remote_id)
                        put("target_type", "Manga")
                        put("chapters", track.last_chapter_read.toInt())
                        put("score", track.score.toInt())
                        put("status", track.toShikimoriStatus())
                    }
                }
                authClient.newCall(
                    POST(
                        "$API_URL/v2/user_rates",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess()
                    .parseAs<SMLibraryIdResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun updateLibManga(track: MangaTrack): MangaTrack {
        return withIOContext {
            val payload = buildJsonObject {
                putJsonObject("user_rate") {
                    put("chapters", track.last_chapter_read.toInt())
                    put("score", track.score.toInt())
                    put("status", track.toShikimoriStatus())
                }
            }

            with(json) {
                authClient.newCall(
                    PUT(
                        "$API_URL/v2/user_rates/${track.library_id}",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMLibraryIdResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun deleteLibManga(track: DomainMangaTrack) {
        withIOContext {
            authClient
                .newCall(DELETE("$API_URL/v2/user_rates/${track.libraryId}"))
                .awaitSuccess()
        }
    }

    suspend fun addLibAnime(track: AnimeTrack, userId: String): AnimeTrack {
        return withIOContext {
            with(json) {
                val payload = buildJsonObject {
                    putJsonObject("user_rate") {
                        put("user_id", userId)
                        put("target_id", track.remote_id)
                        put("target_type", "Anime")
                        put("episodes", track.last_episode_seen.toInt())
                        put("score", track.score.toInt())
                        put("status", track.toShikimoriStatus())
                    }
                }
                authClient.newCall(
                    POST(
                        "$API_URL/v2/user_rates",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess()
                    .parseAs<SMLibraryIdResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val payload = buildJsonObject {
                putJsonObject("user_rate") {
                    put("episodes", track.last_episode_seen.toInt())
                    put("score", track.score.toInt())
                    put("status", track.toShikimoriStatus())
                }
            }

            with(json) {
                authClient.newCall(
                    PUT(
                        "$API_URL/v2/user_rates/${track.library_id}",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMLibraryIdResponse>()
                    .let {
                        track.library_id = it.id
                    }
                track
            }
        }
    }

    suspend fun deleteLibAnime(track: DomainAnimeTrack) {
        withIOContext {
            authClient
                .newCall(DELETE("$API_URL/v2/user_rates/${track.libraryId}"))
                .awaitSuccess()
        }
    }

    suspend fun search(search: String): List<MangaTrackSearch> {
        return withIOContext {
            val query = $$"""
            |query($query: String) {
                |mangas(search: $query, limit: 20, kind:"!light_novel,!novel") {
                    |id
                    |name
                    |chapters
                    |kind
                    |poster {
                        |mainUrl
                    |}
                    |score
                    |url
                    |status
                    |airedOn {
                        |date
                    |}
                    |description
                    |personRoles {
                        |person {
                            |name
                        |}
                        |rolesEn
                    |}
                |}
            |}
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
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMSearchResult>()
                    .data.mangas
                    .map { it.toTrack(trackId) }
            }
        }
    }

    suspend fun getMangaDetails(id: Int): MangaTrackSearch? {
        return withIOContext {
            val query = $$"""
            |query($query: String) {
                |mangas(ids: $query, limit: 1, kind:"!light_novel,!novel") {
                    |id
                    |name
                    |chapters
                    |kind
                    |poster {
                        |mainUrl
                    |}
                    |score
                    |url
                    |status
                    |airedOn {
                        |date
                    |}
                    |description
                    |personRoles {
                        |person {
                            |name
                        |}
                        |rolesEn
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", "$id")
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMSearchResult>()
                    .data.mangas
                    .firstOrNull()
                    ?.toTrack(trackId)
            }
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val query = $$"""
            |query($query: String) {
                |animes(search: $query, limit: 20) {
                    |id
                    |name
                    |episodes
                    |kind
                    |poster {
                        |mainUrl
                    |}
                    |score
                    |url
                    |status
                    |airedOn {
                        |date
                    |}
                    |description
                    |personRoles {
                        |person {
                            |name
                        |}
                        |rolesEn
                    |}
                |}
            |}
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
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMAnimeSearchResult>()
                    .data.animes
                    .map { it.toTrack(trackId) }
            }
        }
    }

    suspend fun getAnimeDetails(id: Int): AnimeTrackSearch? {
        return withIOContext {
            val query = $$"""
            |query($query: String) {
                |animes(ids: $query, limit: 1) {
                    |id
                    |name
                    |episodes
                    |kind
                    |poster {
                        |mainUrl
                    |}
                    |score
                    |url
                    |status
                    |airedOn {
                        |date
                    |}
                    |description
                    |personRoles {
                        |person {
                            |name
                        |}
                        |rolesEn
                    |}
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("query", "$id")
                }
            }

            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMAnimeSearchResult>()
                    .data.animes
                    .firstOrNull()
                    ?.toTrack(trackId)
            }
        }
    }

    suspend fun findLibManga(track: MangaTrack): MangaTrack? {
        return withIOContext {
            val query = $$"""
                |query($id: String) {
                    |mangas(ids: $id, limit: 1) {
                        |id
                        |url
                        |name
                        |chapters
                        |userRate {
                            |id
                            |chapters
                            |status
                            |score
                        |}
                    |}
                |}
            """.trimMargin()

            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", track.remote_id.toString())
                }
            }
            with(json) {
                val listResult = authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMUserListResult>()
                    .data.mangas
                    .firstOrNull()

                // Shikimori has no user list query that allows query by ID, so we go via the "mangas" query & include
                // userRate data which will be null if the title is not in the user's list.
                // If it was removed on Shikimori and is still linked in the app, notify user via returning null here
                // which throws an exception at the Shikimori.refresh call
                if (listResult?.userRate == null) {
                    null
                } else {
                    listResult.toTrack(trackId)
                }
            }
        }
    }

    suspend fun findLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val query = $$"""
                |query($id: String) {
                    |animes(ids: $id, limit: 1) {
                        |id
                        |url
                        |name
                        |episodes
                        |userRate {
                            |id
                            |episodes
                            |status
                            |score
                        |}
                    |}
                |}
            """.trimMargin()

            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("id", track.remote_id.toString())
                }
            }
            with(json) {
                val listResult = authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMAnimeUserListResult>()
                    .data.animes
                    .firstOrNull()

                if (listResult?.userRate == null) {
                    null
                } else {
                    listResult.toTrack(trackId)
                }
            }
        }
    }

    suspend fun getCurrentUser(): SMUser {
        return with(json) {
            val query = """
            |{
                |currentUser {
                    |id
                    |nickname
                |}
            |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
            }
            authClient.newCall(
                POST(
                    GRAPHQL_API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<SMUserResult>()
                .data.currentUser
        }
    }

    suspend fun getMangaMetadata(track: DomainMangaTrack): TrackMangaMetadata {
        return withIOContext {
            val query = """
                |query(${'$'}ids: String!) {
                    |mangas(ids: ${'$'}ids) {
                        |id
                        |name
                        |description
                        |poster {
                            |originalUrl
                        |}
                        |personRoles {
                            |person {
                                |name
                            |}
                            |rolesEn
                        |}
                    |}
                |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("ids", "${track.remoteId}")
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMMetadata>()
                    .let {
                        if (it.data.mangas.isEmpty()) throw Exception("Could not get metadata from Shikimori")
                        val manga = it.data.mangas[0]
                        TrackMangaMetadata(
                            remoteId = manga.id.toLong(),
                            title = manga.name,
                            thumbnailUrl = manga.poster.originalUrl,
                            description = manga.description,
                            authors = manga.personRoles
                                ?.filter { it.rolesEn.contains("Story") || it.rolesEn.contains("Story & Art") }
                                ?.map { it.person.name }
                                ?.joinToString(", ")
                                ?.ifEmpty { null },
                            artists = manga.personRoles
                                ?.filter { it.rolesEn.contains("Art") || it.rolesEn.contains("Story & Art") }
                                ?.map { it.person.name }
                                ?.joinToString(", ")
                                ?.ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun getAnimeMetadata(track: DomainAnimeTrack): TrackAnimeMetadata {
        return withIOContext {
            val query = """
                |query(${'$'}ids: String!) {
                    |animes(ids: ${'$'}ids) {
                        |id
                        |name
                        |description
                        |poster {
                            |originalUrl
                        |}
                        |personRoles {
                            |person {
                                |name
                            |}
                            |rolesEn
                        |}
                    |}
                |}
            """.trimMargin()
            val payload = buildJsonObject {
                put("query", query)
                putJsonObject("variables") {
                    put("ids", "${track.remoteId}")
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMMetadata>()
                    .let {
                        if (it.data.animes.isEmpty()) throw Exception("Could not get metadata from Shikimori")
                        val anime = it.data.animes[0]
                        TrackAnimeMetadata(
                            remoteId = anime.id.toLong(),
                            title = anime.name,
                            thumbnailUrl = anime.poster.originalUrl,
                            description = anime.description,
                            authors = anime.personRoles
                                ?.filter { it.rolesEn.contains("Story") || it.rolesEn.contains("Story & Art") }
                                ?.map { it.person.name }
                                ?.joinToString(", ")
                                ?.ifEmpty { null },
                            artists = anime.personRoles
                                ?.filter { it.rolesEn.contains("Art") || it.rolesEn.contains("Story & Art") }
                                ?.map { it.person.name }
                                ?.joinToString(", ")
                                ?.ifEmpty { null },
                        )
                    }
            }
        }
    }

    suspend fun accessToken(code: String): SMOAuth {
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
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URL)
            .build(),
    )

    /**
     * Retrieves comprehensive deep media details from Shikimori ported from DailyAL's rich media graph.
     *
     * Queries Shikimori REST endpoints requesting media metadata, alternative titles, genres,
     * studios, character roles, staff, franchise relations, and similar recommendations.
     *
     * @param mediaId Shikimori media identifier.
     * @param isAnime Whether the target media is an anime (true) or manga (false).
     * @return [Result] containing [DailyALMediaDetails] on success, or exception on failure.
     */
    suspend fun getDeepMediaDetails(mediaId: Long, isAnime: Boolean = true): Result<DailyALMediaDetails> {
        return withIOContext {
            runCatching {
                val endpoint = if (isAnime) "animes" else "mangas"
                val detailsUrl = "$API_URL/$endpoint/$mediaId"
                val responseBody = executeShikimoriCall(detailsUrl)
                val jsonElement = json.parseToJsonElement(responseBody)
                val mediaObj = jsonElement.asJsonObjectOrNull
                    ?: throw IllegalStateException("Media data missing in Shikimori response for id=$mediaId")

                val relations = runCatching {
                    val relBody = executeShikimoriCall("$API_URL/$endpoint/$mediaId/related")
                    val relArray = json.parseToJsonElement(relBody).asJsonArrayOrNull ?: emptyList()
                    relArray.mapNotNull { itemElem ->
                        val item = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                        val relationType = item["relation_text"]?.jsonPrimitive?.contentOrNull
                            ?: item["relation"]?.jsonPrimitive?.contentOrNull ?: "RELATED"
                        val targetNode = item["anime"].asJsonObjectOrNull ?: item["manga"].asJsonObjectOrNull
                            ?: return@mapNotNull null
                        val relId = targetNode["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val relTitle = targetNode["name"]?.jsonPrimitive?.contentOrNull
                            ?: targetNode["russian"]?.jsonPrimitive?.contentOrNull ?: "Related"
                        val relImg = targetNode["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                            ?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }
                        val relFormat = targetNode["kind"]?.jsonPrimitive?.contentOrNull?.uppercase()
                        DailyALRelation(
                            id = relId,
                            title = relTitle,
                            format = relFormat,
                            relationType = relationType,
                            coverUrl = relImg,
                        )
                    }
                }.getOrDefault(emptyList())

                val roles = runCatching {
                    val rolesBody = executeShikimoriCall("$API_URL/$endpoint/$mediaId/roles")
                    val rolesArray = json.parseToJsonElement(rolesBody).asJsonArrayOrNull ?: emptyList()
                    val characters = mutableListOf<DailyALCharacterRole>()
                    val staff = mutableListOf<DailyALStaffRole>()
                    rolesArray.forEach { roleElem ->
                        val roleObj = roleElem.asJsonObjectOrNull ?: return@forEach
                        val roleNames = roleObj["roles"].asJsonArrayOrNull?.mapNotNull { it.jsonPrimitive.contentOrNull }
                            ?: emptyList()
                        val charObj = roleObj["character"].asJsonObjectOrNull
                        val personObj = roleObj["person"].asJsonObjectOrNull
                        if (charObj != null) {
                            val charId = charObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                            val charName = charObj["name"]?.jsonPrimitive?.contentOrNull
                                ?: charObj["russian"]?.jsonPrimitive?.contentOrNull ?: "Character"
                            val charImg = charObj["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                                ?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }
                            characters.add(
                                DailyALCharacterRole(
                                    id = charId,
                                    name = charName,
                                    imageUrl = charImg,
                                    role = roleNames.firstOrNull() ?: "MAIN",
                                ),
                            )
                        } else if (personObj != null) {
                            val personId = personObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                            val personName = personObj["name"]?.jsonPrimitive?.contentOrNull
                                ?: personObj["russian"]?.jsonPrimitive?.contentOrNull ?: "Staff"
                            val personImg = personObj["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                                ?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }
                            staff.add(
                                DailyALStaffRole(
                                    id = personId,
                                    name = personName,
                                    role = roleNames.joinToString(", ").ifBlank { "Staff" },
                                    imageUrl = personImg,
                                ),
                            )
                        }
                    }
                    characters to staff
                }.getOrDefault(emptyList<DailyALCharacterRole>() to emptyList<DailyALStaffRole>())

                val similar = runCatching {
                    val simBody = executeShikimoriCall("$API_URL/$endpoint/$mediaId/similar")
                    val simArray = json.parseToJsonElement(simBody).asJsonArrayOrNull ?: emptyList()
                    simArray.mapNotNull { simElem ->
                        val simObj = simElem.asJsonObjectOrNull ?: return@mapNotNull null
                        val simId = simObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val simTitle = simObj["name"]?.jsonPrimitive?.contentOrNull
                            ?: simObj["russian"]?.jsonPrimitive?.contentOrNull ?: "Recommended"
                        val simImg = simObj["image"].asJsonObjectOrNull?.get("original")?.jsonPrimitive?.contentOrNull
                            ?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }
                        val simScore = simObj["score"]?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
                        DailyALRecommendation(
                            id = simId,
                            title = simTitle,
                            coverUrl = simImg,
                            meanScore = simScore,
                        )
                    }
                }.getOrDefault(emptyList())

                parseShikimoriDeepMediaDetails(mediaObj, mediaId, isAnime, roles.first, roles.second, relations, similar)
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error fetching Shikimori deep media details for mediaId=$mediaId" }
            }
        }
    }

    /**
     * Executes an advanced multi-parameter search query matching DailyAL's rich filtering capabilities on Shikimori.
     *
     * Supports search queries by text keywords, genres, format/kind, status, season/year,
     * and pagination with client-side rate-limiting and defensive JSON parsing.
     *
     * @param filter The active [DailyALFilterState] specifying search constraints.
     * @param page Page index for paginated browsing (1-based).
     * @return [Result] containing list of [DailyALMediaDetails], or error on failure.
     */
    suspend fun searchAdvanced(filter: DailyALFilterState, page: Int = 1): Result<List<DailyALMediaDetails>> {
        return withIOContext {
            runCatching {
                val isAnime = filter.format?.contains("MANGA", ignoreCase = true) != true
                val endpoint = if (isAnime) "animes" else "mangas"
                val urlBuilder = "$API_URL/$endpoint?limit=25&page=${page.coerceAtLeast(1)}".toUri().buildUpon()

                if (filter.query.isNotBlank()) {
                    urlBuilder.appendQueryParameter("search", filter.query.trim())
                }
                if (filter.includedGenres.isNotEmpty()) {
                    urlBuilder.appendQueryParameter("genre", filter.includedGenres.joinToString(","))
                }
                if (!filter.format.isNullOrBlank()) {
                    urlBuilder.appendQueryParameter("kind", filter.format.lowercase())
                }
                if (!filter.status.isNullOrBlank()) {
                    val shikimoriStatus = when (filter.status.uppercase()) {
                        "RELEASING" -> "ongoing"
                        "FINISHED" -> "released"
                        "NOT_YET_RELEASED" -> "anons"
                        else -> filter.status.lowercase()
                    }
                    urlBuilder.appendQueryParameter("status", shikimoriStatus)
                }
                if (filter.seasonYear != null && filter.seasonYear > 0) {
                    val seasonStr = filter.season?.lowercase()?.let { "_$it" } ?: ""
                    urlBuilder.appendQueryParameter("season", "${filter.seasonYear}$seasonStr")
                }
                if (filter.minScore != null && filter.minScore > 0) {
                    urlBuilder.appendQueryParameter("score", (filter.minScore / 10).toString())
                }
                val orderParam = when (filter.sortBy.uppercase()) {
                    "SCORE_DESC" -> "ranked"
                    "START_DATE_DESC" -> "aired_on"
                    else -> "popularity"
                }
                urlBuilder.appendQueryParameter("order", orderParam)

                val responseBody = executeShikimoriCall(urlBuilder.build().toString())
                val jsonElement = json.parseToJsonElement(responseBody)
                val itemsArray = jsonElement.asJsonArrayOrNull ?: emptyList()

                itemsArray.mapNotNull { itemElem ->
                    val itemObj = itemElem.asJsonObjectOrNull ?: return@mapNotNull null
                    val id = itemObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    parseShikimoriDeepMediaDetails(
                        itemObj,
                        id,
                        isAnime,
                        characters = emptyList(),
                        staff = emptyList(),
                        relations = emptyList(),
                        recommendations = emptyList(),
                    )
                }
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Error executing Shikimori advanced search query='${filter.query}'" }
            }
        }
    }

    /**
     * Queries profile metrics and watch statistics for the currently authenticated Shikimori user.
     *
     * Retrieves username, avatar, total days watched, completed/watching counts,
     * and status distribution matching DailyAL's user view.
     *
     * @return [Result] containing [DailyALProfileStats] on success, or failure if unauthenticated or network error.
     */
    suspend fun getUserProfileStats(): Result<DailyALProfileStats> {
        return withIOContext {
            runCatching {
                val whoamiBody = executeShikimoriCall("$API_URL/users/whoami", requireAuth = true)
                val whoamiObj = json.parseToJsonElement(whoamiBody).asJsonObjectOrNull
                    ?: throw IllegalStateException("Failed to retrieve Shikimori user credentials")
                val userId = whoamiObj["id"]?.jsonPrimitive?.longOrNull
                    ?: throw IllegalStateException("Shikimori user ID missing")
                val userName = whoamiObj["nickname"]?.jsonPrimitive?.contentOrNull ?: "Shikimori User"
                val avatarUrl = whoamiObj["avatar"]?.jsonPrimitive?.contentOrNull
                    ?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }

                val infoBody = executeShikimoriCall("$API_URL/users/$userId/info", requireAuth = true)
                val infoObj = json.parseToJsonElement(infoBody).asJsonObjectOrNull
                val statsObj = infoObj?.get("stats").asJsonObjectOrNull

                val statusMap = mutableMapOf<String, Int>()
                val animeStatuses = statsObj?.get("statuses").asJsonObjectOrNull?.get("anime").asJsonArrayOrNull ?: emptyList()
                animeStatuses.forEach { stElem ->
                    val stObj = stElem.asJsonObjectOrNull ?: return@forEach
                    val name = stObj["name"]?.jsonPrimitive?.contentOrNull
                    val size = stObj["size"]?.jsonPrimitive?.intOrNull ?: 0
                    if (name != null) {
                        val mapped = when (name.lowercase()) {
                            "watching" -> "CURRENT"
                            "completed" -> "COMPLETED"
                            "on_hold" -> "PAUSED"
                            "dropped" -> "DROPPED"
                            "planned" -> "PLANNING"
                            else -> name.uppercase()
                        }
                        statusMap[mapped] = size
                    }
                }

                val scoreMap = mutableMapOf<Int, Int>()
                val animeScores = statsObj?.get("scores").asJsonObjectOrNull?.get("anime").asJsonArrayOrNull ?: emptyList()
                animeScores.forEach { scElem ->
                    val scObj = scElem.asJsonObjectOrNull ?: return@forEach
                    val name = scObj["name"]?.jsonPrimitive?.intOrNull
                    val value = scObj["value"]?.jsonPrimitive?.intOrNull ?: 0
                    if (name != null) {
                        scoreMap[name * 10] = value
                    }
                }

                val totalEntries = statusMap.values.sum()

                DailyALProfileStats(
                    userName = userName,
                    avatarUrl = avatarUrl,
                    bannerUrl = null,
                    animeDaysWatched = 0.0,
                    meanScore = 0.0,
                    totalEpisodes = 0,
                    totalEntries = totalEntries,
                    statusDistribution = statusMap,
                    scoreDistribution = scoreMap,
                )
            }.onFailure { e ->
                logcat(LogPriority.ERROR, e) { "DailyAL: Failed to retrieve user profile statistics from Shikimori" }
            }
        }
    }

    private fun parseShikimoriDeepMediaDetails(
        media: JsonObject,
        requestedId: Long,
        isAnime: Boolean,
        characters: List<DailyALCharacterRole>,
        staff: List<DailyALStaffRole>,
        relations: List<DailyALRelation>,
        recommendations: List<DailyALRecommendation>,
    ): DailyALMediaDetails {
        val id = media["id"]?.jsonPrimitive?.longOrNull ?: requestedId
        val name = media["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val russian = media["russian"]?.jsonPrimitive?.contentOrNull
        val japanese = media["japanese"].asJsonArrayOrNull?.firstOrNull()?.jsonPrimitive?.contentOrNull

        val imgObj = media["image"].asJsonObjectOrNull
        val coverRel = imgObj?.get("original")?.jsonPrimitive?.contentOrNull
            ?: imgObj?.get("preview")?.jsonPrimitive?.contentOrNull
        val coverUrl = coverRel?.let { if (it.startsWith("/")) "$BASE_URL$it" else it }

        val format = media["kind"]?.jsonPrimitive?.contentOrNull?.uppercase()
        val episodes = if (isAnime) {
            media["episodes"]?.jsonPrimitive?.intOrNull
        } else {
            media["chapters"]?.jsonPrimitive?.intOrNull
        }
        val duration = media["duration"]?.jsonPrimitive?.intOrNull

        val rawStatus = media["status"]?.jsonPrimitive?.contentOrNull
        val status = when (rawStatus?.lowercase()) {
            "ongoing" -> "RELEASING"
            "released" -> "FINISHED"
            "anons" -> "NOT_YET_RELEASED"
            else -> rawStatus?.uppercase()
        }

        val airedOn = media["aired_on"]?.jsonPrimitive?.contentOrNull
        val seasonYear = airedOn?.take(4)?.toIntOrNull()
        val month = airedOn?.drop(5)?.take(2)?.toIntOrNull()
        val season = when (month) {
            1, 2, 3 -> "WINTER"
            4, 5, 6 -> "SPRING"
            7, 8, 9 -> "SUMMER"
            10, 11, 12 -> "FALL"
            else -> null
        }

        val meanScore = media["score"]?.jsonPrimitive?.doubleOrNull?.let { (it * 10).toInt() }
        val synopsis = media["description"]?.jsonPrimitive?.contentOrNull?.htmlDecode()

        val genres = media["genres"].asJsonArrayOrNull?.mapNotNull {
            it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull
        } ?: emptyList()

        val studios = media["studios"].asJsonArrayOrNull?.mapNotNull {
            it.asJsonObjectOrNull?.get("name")?.jsonPrimitive?.contentOrNull
        } ?: emptyList()

        val trailerUrl = media["videos"].asJsonArrayOrNull?.firstOrNull()?.asJsonObjectOrNull?.get("url")?.jsonPrimitive?.contentOrNull

        return DailyALMediaDetails(
            id = id,
            title = name,
            romajiTitle = russian,
            nativeTitle = japanese,
            coverUrl = coverUrl,
            bannerUrl = coverUrl,
            format = format,
            episodes = episodes,
            duration = duration,
            status = status,
            season = season,
            seasonYear = seasonYear,
            meanScore = meanScore,
            popularity = null,
            rank = null,
            synopsis = synopsis,
            genres = genres,
            tags = studios,
            trailerUrl = trailerUrl,
            characters = characters,
            staff = staff,
            relations = relations,
            recommendations = recommendations,
            reviews = emptyList(),
        )
    }

    companion object {
        const val BASE_URL = "https://shikimori.one"
        private const val API_URL = "$BASE_URL/api"
        private const val GRAPHQL_API_URL = "$BASE_URL/api/graphql"
        private const val OAUTH_URL = "$BASE_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"

        private const val REDIRECT_URL = "animetail://shikimori-auth"

        private const val CLIENT_ID = "aOAYRqOLwxpA8skpcQIXetNy4cw2rn2fRzScawlcQ5U"
        private const val CLIENT_SECRET = "jqjmORn6bh2046ulkm4lHEwJ3OA1RmO3FD2sR9f6Clw"

        fun authUrl(): Uri = LOGIN_URL.toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("response_type", "code")
            .build()

        fun refreshTokenRequest(token: String) = POST(
            OAUTH_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("refresh_token", token)
                .build(),
        )
    }
}

private val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

