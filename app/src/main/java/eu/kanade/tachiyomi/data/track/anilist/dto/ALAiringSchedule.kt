package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import kotlinx.serialization.Serializable

@Serializable
data class ALAiringScheduleResult(
    val data: ALAiringSchedulePageData,
)

@Serializable
data class ALAiringSchedulePageData(
    val Page: ALAiringSchedulePage,
)

@Serializable
data class ALAiringSchedulePage(
    val airingSchedules: List<ALAiringScheduleItem> = emptyList(),
)

@Serializable
data class ALAiringScheduleItem(
    val id: Long,
    val airingAt: Long,
    val episode: Int,
    val media: ALSearchItem,
) {
    fun toAiringEpisode(): AiringEpisode = AiringEpisode(
        id = id,
        mediaId = media.id,
        title = media.title.preferred,
        coverUrl = media.coverImage.large,
        episode = episode,
        airingAt = airingAt,
        format = media.format?.replace("_", "-") ?: "TV",
        score = (media.averageScore ?: -1).toDouble(),
        summary = media.description ?: "",
    )
}
