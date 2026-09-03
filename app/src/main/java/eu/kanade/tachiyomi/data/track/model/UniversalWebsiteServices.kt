package eu.kanade.tachiyomi.data.track.model

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Universal Website Services Registry for Anime, Manga, Light Novels, and Webtoons.
 * Provides deep-linking, cross-platform metadata links, and browser bridge helpers.
 */
enum class ExternalWebsite(
    val displayName: String,
    val baseUrl: String,
    val category: WebsiteCategory,
) {
    MYANIMELIST("MyAnimeList", "https://myanimelist.net", WebsiteCategory.ALL),
    ANILIST("AniList", "https://anilist.co", WebsiteCategory.ALL),
    KITSU("Kitsu", "https://kitsu.io", WebsiteCategory.ALL),
    SHIKIMORI("Shikimori", "https://shikimori.one", WebsiteCategory.ALL),
    BANGUMI("Bangumi", "https://bgm.tv", WebsiteCategory.ALL),
    SIMKL("Simkl", "https://simkl.com", WebsiteCategory.ALL),
    TRAKT("Trakt", "https://trakt.tv", WebsiteCategory.ANIME),
    MANGAUPDATES("MangaUpdates", "https://www.mangaupdates.com", WebsiteCategory.MANGA),
    MANGABAKA("MangaBaka", "https://mangabaka.org", WebsiteCategory.MANGA),
    MANGADEX("MangaDex", "https://mangadex.org", WebsiteCategory.MANGA),
    NOVELUPDATES("NovelUpdates", "https://www.novelupdates.com", WebsiteCategory.NOVEL),
    ANIMEPLANET("Anime-Planet", "https://www.anime-planet.com", WebsiteCategory.ALL),
    LIVECHART("LiveChart.me", "https://www.livechart.me", WebsiteCategory.ANIME),
    ANIMENEWSNETWORK("Anime News Network", "https://www.animenewsnetwork.com", WebsiteCategory.ANIME),
    ANISKIP("AniSkip API", "https://api.aniskip.com", WebsiteCategory.ANIME),
    ANIZIP("AniZip Mapper", "https://api.ani.zip", WebsiteCategory.ANIME),
    SUWAYOMI("Suwayomi Server", "http://localhost:4567", WebsiteCategory.SERVER),
    KOMGA("Komga Server", "http://localhost:8080", WebsiteCategory.SERVER),
    KAVITA("Kavita Server", "http://localhost:5000", WebsiteCategory.SERVER),
    JELLYFIN("Jellyfin Server", "http://localhost:8096", WebsiteCategory.SERVER),
    TMDB("The Movie Database", "https://www.themoviedb.org", WebsiteCategory.ANIME);

    fun buildAnimeUrl(id: String): String = when (this) {
        MYANIMELIST -> "$baseUrl/anime/$id"
        ANILIST -> "$baseUrl/anime/$id"
        KITSU -> "$baseUrl/anime/$id"
        SHIKIMORI -> "$baseUrl/animes/$id"
        BANGUMI -> "$baseUrl/subject/$id"
        SIMKL -> "$baseUrl/anime/$id"
        TRAKT -> "$baseUrl/shows/$id"
        LIVECHART -> "$baseUrl/anime/$id"
        ANIMEPLANET -> "$baseUrl/anime/$id"
        TMDB -> "$baseUrl/tv/$id"
        else -> baseUrl
    }

    fun buildMangaUrl(id: String): String = when (this) {
        MYANIMELIST -> "$baseUrl/manga/$id"
        ANILIST -> "$baseUrl/manga/$id"
        KITSU -> "$baseUrl/manga/$id"
        SHIKIMORI -> "$baseUrl/mangas/$id"
        MANGAUPDATES -> "$baseUrl/series.html?id=$id"
        MANGADEX -> "$baseUrl/title/$id"
        NOVELUPDATES -> "$baseUrl/series/$id"
        else -> baseUrl
    }

    fun openInBrowser(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

enum class WebsiteCategory {
    ANIME,
    MANGA,
    NOVEL,
    ALL,
    SERVER,
}
