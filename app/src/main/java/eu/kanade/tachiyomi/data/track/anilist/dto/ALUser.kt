package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALCurrentUserResult(
    val data: ALUserViewer,
)

@Serializable
data class ALUserViewer(
    @SerialName("Viewer")
    val viewer: ALUserViewerData,
)

@Serializable
data class ALUserViewerData(
    val id: Int,
    val mediaListOptions: ALUserListOptions,
)

@Serializable
data class ALUserListOptions(
    val scoreFormat: String,
)

@Serializable
data class ALProfileStatsResult(
    val data: ALProfileStatsData,
)

@Serializable
data class ALProfileStatsData(
    @SerialName("User")
    val user: ALUserStats,
)

@Serializable
data class ALUserStats(
    val id: Int,
    val name: String,
    val avatar: ALUserAvatar? = null,
    val bannerImage: String? = null,
    val statistics: ALUserStatistics? = null,
)

@Serializable
data class ALUserAvatar(
    val large: String? = null,
)

@Serializable
data class ALUserStatistics(
    val anime: ALAnimeStatistics? = null,
    val manga: ALMangaStatistics? = null,
)

@Serializable
data class ALAnimeStatistics(
    val count: Int,
    val episodesWatched: Int,
    val minutesWatched: Int,
    val meanScore: Double,
)

@Serializable
data class ALMangaStatistics(
    val count: Int,
    val chaptersRead: Int,
    val volumesRead: Int,
    val meanScore: Double,
)
