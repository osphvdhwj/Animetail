package eu.kanade.tachiyomi.ui.discover

import kotlinx.serialization.Serializable

/**
 * Detailed representation of an anime or manga entry ported from DailyAL's rich media graph.
 *
 * Encapsulates core metadata, rankings, media relations, characters, voice actors,
 * production staff, curated recommendations, and community reviews.
 *
 * @property id Unique identifier of the media entry on AniList.
 * @property title Primary display title (preferred user title or English).
 * @property romajiTitle Romaji romanized title.
 * @property nativeTitle Native title in original language (Japanese kanji/kana, etc.).
 * @property coverUrl High-resolution poster cover image URL.
 * @property bannerUrl Wide hero banner artwork URL.
 * @property format Media release format (e.g., TV, MOVIE, OVA, ONA, MANGA, NOVEL).
 * @property episodes Total number of episodes for anime, or null if unknown/airing.
 * @property duration Average duration per episode in minutes, or null if unknown.
 * @property status Publishing or broadcasting status (e.g., RELEASING, FINISHED, NOT_YET_RELEASED).
 * @property season Broadcast season during which the media premiered (e.g., WINTER, SPRING, SUMMER, FALL).
 * @property seasonYear Calendar year during which the media premiered.
 * @property meanScore Mean rating percentage calculated across all AniList users (0-100).
 * @property popularity Total number of users who have added this media to their lists.
 * @property rank Ranking position of the media (all-time rated or popularity).
 * @property synopsis Sanitized HTML-decoded synopsis description.
 * @property genres List of genres associated with this media entry.
 * @property tags List of thematic and contextual tags.
 * @property trailerUrl Full URL or embed identifier for the official video trailer.
 * @property characters List of main and supporting character roles associated with the media.
 * @property staff List of production staff members and their credited roles.
 * @property relations List of related anime and manga entries (prequels, sequels, adaptations).
 * @property recommendations List of media recommendations suggested by the community.
 * @property reviews List of in-depth user reviews and community critiques.
 */
@Serializable
data class DailyALMediaDetails(
    val id: Long,
    val title: String,
    val romajiTitle: String? = null,
    val nativeTitle: String? = null,
    val coverUrl: String? = null,
    val bannerUrl: String? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val rank: Int? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val trailerUrl: String? = null,
    val characters: List<DailyALCharacterRole> = emptyList(),
    val staff: List<DailyALStaffRole> = emptyList(),
    val relations: List<DailyALRelation> = emptyList(),
    val recommendations: List<DailyALRecommendation> = emptyList(),
    val reviews: List<DailyALReview> = emptyList(),
) {
    /**
     * Resolves the most descriptive title available, falling back from [title] to [romajiTitle] or [nativeTitle].
     */
    val displayTitle: String
        get() = title.ifBlank { romajiTitle ?: nativeTitle ?: "Unknown" }

    /**
     * Indicates whether the media is currently actively broadcasting or publishing.
     */
    val isReleasing: Boolean
        get() = status.equals("RELEASING", ignoreCase = true)

    /**
     * Formatted score string (e.g., "85%" or "N/A").
     */
    val scoreDisplay: String
        get() = meanScore?.let { "$it%" } ?: "N/A"
}

/**
 * Represents a character role within an anime or manga production.
 *
 * @property id Unique identifier of the character.
 * @property name Full romanized character name.
 * @property nativeName Character's name in original Japanese/Asian characters.
 * @property imageUrl Character portrait avatar image URL.
 * @property role Role significance in the story (e.g., "MAIN", "SUPPORTING", "BACKGROUND").
 * @property voiceActors List of voice actors (seiyuu) portraying this character in various dubs/subs.
 */
@Serializable
data class DailyALCharacterRole(
    val id: Long,
    val name: String,
    val nativeName: String? = null,
    val imageUrl: String? = null,
    val role: String = "MAIN",
    val voiceActors: List<DailyALVoiceActor> = emptyList(),
)

/**
 * Represents a voice actor (seiyuu) associated with a character role.
 *
 * @property id Unique identifier of the voice actor on AniList.
 * @property name Romanized full name of the voice actor.
 * @property nativeName Voice actor's name in original script (e.g. Japanese kanji).
 * @property language Audio language of the performance (e.g. "Japanese", "English").
 * @property imageUrl Portrait photo URL of the voice actor.
 */
@Serializable
data class DailyALVoiceActor(
    val id: Long,
    val name: String,
    val nativeName: String? = null,
    val language: String = "Japanese",
    val imageUrl: String? = null,
)

/**
 * Represents a production staff member and their credited role in an anime or manga project.
 *
 * @property id Unique identifier of the staff member.
 * @property name Full romanized name of the staff member.
 * @property role Credited role in the production (e.g., "Director", "Original Creator", "Music", "Character Design").
 * @property imageUrl Portrait photo URL of the staff member.
 */
@Serializable
data class DailyALStaffRole(
    val id: Long,
    val name: String,
    val role: String,
    val imageUrl: String? = null,
)

/**
 * Represents a franchise relation linking one media entry to another.
 *
 * Ported from DailyAL's franchise graph connections (DAG hierarchy).
 *
 * @property id Unique identifier of the related target media.
 * @property title Primary display title of the related media.
 * @property format Release format of the related media (e.g., TV, MOVIE, OVA, MANGA, NOVEL).
 * @property status Airing/publishing status of the related media.
 * @property relationType Type of narrative or franchise relation (e.g., "PREQUEL", "SEQUEL", "ADAPTATION", "SIDE_STORY", "SPIN_OFF").
 * @property coverUrl Poster cover image URL of the related media.
 * @property meanScore Mean rating percentage of the related media.
 */
@Serializable
data class DailyALRelation(
    val id: Long,
    val title: String,
    val format: String? = null,
    val status: String? = null,
    val relationType: String,
    val coverUrl: String? = null,
    val meanScore: Int? = null,
)

/**
 * Represents a media recommendation suggested and voted on by community members.
 *
 * @property id Unique identifier of the recommendation entry.
 * @property title Title of the recommended media.
 * @property coverUrl Poster cover image URL of the recommended media.
 * @property meanScore Mean rating percentage of the recommended media.
 * @property ratingCount Net community upvote rating for this recommendation pairing.
 */
@Serializable
data class DailyALRecommendation(
    val id: Long,
    val title: String,
    val coverUrl: String? = null,
    val meanScore: Int? = null,
    val ratingCount: Int = 0,
)

/**
 * Represents a community user review of a media entry ported from DailyAL.
 *
 * @property id Unique identifier of the review.
 * @property summary Brief headline or one-line summary of the review.
 * @property body Complete markdown or plain text review body.
 * @property score Numeric score assigned by the reviewer (typically 0-100).
 * @property rating Net positive helpfulness rating awarded to this review by users.
 * @property userName Display name of the review author.
 * @property userAvatarUrl Avatar image URL of the review author.
 * @property isSpoiler Whether this review has been flagged as containing story spoilers.
 */
@Serializable
data class DailyALReview(
    val id: Long,
    val summary: String? = null,
    val body: String,
    val score: Int = 0,
    val rating: Int = 0,
    val userName: String,
    val userAvatarUrl: String? = null,
    val isSpoiler: Boolean = false,
)

/**
 * Filter configuration state encapsulating DailyAL's advanced multi-parameter search criteria.
 *
 * @property query Free-form text search keyword.
 * @property includedGenres Set of genre names that must be included.
 * @property excludedGenres Set of genre names that must be excluded.
 * @property format Media format filter (e.g., "TV", "MOVIE", "OVA", "MANGA", "NOVEL").
 * @property status Airing status filter (e.g., "RELEASING", "FINISHED", "NOT_YET_RELEASED").
 * @property season Broadcast season filter (e.g., "WINTER", "SPRING", "SUMMER", "FALL").
 * @property seasonYear Broadcast release year filter (e.g., 2026).
 * @property minScore Minimum score threshold on a 0-100 scale.
 * @property sortBy Sort order identifier (e.g., "POPULARITY_DESC", "SCORE_DESC", "TRENDING_DESC", "START_DATE_DESC").
 */
@Serializable
data class DailyALFilterState(
    val query: String = "",
    val includedGenres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val format: String? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val minScore: Int? = null,
    val sortBy: String = "POPULARITY_DESC",
) {
    /**
     * Checks if any active filter criterion is set beyond default empty values.
     */
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            includedGenres.isNotEmpty() ||
            excludedGenres.isNotEmpty() ||
            format != null ||
            status != null ||
            season != null ||
            seasonYear != null ||
            minScore != null ||
            sortBy != "POPULARITY_DESC"

    /**
     * Generates a deterministic cache key string uniquely representing this filter configuration.
     */
    fun toCacheKey(): String = buildString {
        append("q=").append(query.trim())
        append("&inc=").append(includedGenres.sorted().joinToString(","))
        append("&exc=").append(excludedGenres.sorted().joinToString(","))
        append("&fmt=").append(format.orEmpty())
        append("&stat=").append(status.orEmpty())
        append("&sea=").append(season.orEmpty())
        append("&yr=").append(seasonYear ?: 0)
        append("&score=").append(minScore ?: 0)
        append("&sort=").append(sortBy)
    }

    /**
     * Returns a reset copy with all filter parameters cleared to default values.
     */
    fun reset(): DailyALFilterState = DailyALFilterState()
}

/**
 * User profile tracking statistics and activity summary ported from DailyAL's user view.
 *
 * @property userName Display username of the authenticated AniList profile.
 * @property avatarUrl Large avatar image URL of the user.
 * @property bannerUrl Wide profile banner header image URL.
 * @property animeDaysWatched Total anime watch time converted to days (e.g. 42.5 days).
 * @property meanScore Overall mean score given across all rated media entries.
 * @property totalEpisodes Total number of individual anime episodes watched.
 * @property totalEntries Total count of anime entries in user's library.
 * @property statusDistribution Map of list status names (e.g., "CURRENT", "COMPLETED", "PLANNING", "DROPPED", "PAUSED") to entry counts.
 * @property scoreDistribution Map of rating scores (e.g., 10, 20...100 or 1-10) to entry counts.
 */
@Serializable
data class DailyALProfileStats(
    val userName: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val animeDaysWatched: Double = 0.0,
    val meanScore: Double = 0.0,
    val totalEpisodes: Int = 0,
    val totalEntries: Int = 0,
    val statusDistribution: Map<String, Int> = emptyMap(),
    val scoreDistribution: Map<Int, Int> = emptyMap(),
)

/**
 * One entry of the authenticated user's AniList anime list, used by the
 * Discover profile's list browser and inline progress/score editor.
 *
 * @property entryId AniList media-list entry id (0 when unknown).
 * @property mediaId AniList media id.
 * @property title Romaji (fallback preferred) title.
 * @property coverUrl Medium cover image URL.
 * @property scoreRaw User's score on the 0-100 POINT_100 scale (0 = unscored).
 * @property progress Episodes watched so far.
 * @property status List status of this entry (CURRENT, PLANNING, ...).
 * @property format Media format (TV, MOVIE, ...).
 * @property mediaStatus Publishing status of the media itself.
 * @property averageScore Community average score (0-100, -1 unknown).
 * @property totalEpisodes Total episodes (anime) or chapters (manga) of the media (0 unknown).
 * @property isManga Whether this entry comes from the user's manga list.
 */
@Serializable
data class DailyALUserListEntry(
    val entryId: Long = 0L,
    val mediaId: Long,
    val title: String,
    val coverUrl: String? = null,
    val scoreRaw: Int = 0,
    val progress: Int = 0,
    val status: String = "CURRENT",
    val format: String? = null,
    val mediaStatus: String? = null,
    val averageScore: Int = -1,
    val totalEpisodes: Int = 0,
    val isManga: Boolean = false,
)

/**
 * One entry of the authenticated user's recent AniList activity feed.
 *
 * @property mediaId AniList media id the activity refers to.
 * @property mediaTitle Romaji (fallback preferred) title of the media.
 * @property mediaCoverUrl Medium cover image URL of the media.
 * @property status Raw activity status verb (e.g. "watched episode", "read chapter", "completed").
 * @property progress Progress string (e.g. "5" or "240").
 * @property createdAt Unix epoch seconds of the activity.
 */
@Serializable
data class DailyALActivityEntry(
    val mediaId: Long,
    val mediaTitle: String,
    val mediaCoverUrl: String? = null,
    val status: String,
    val progress: String = "",
    val createdAt: Long = 0L,
)
