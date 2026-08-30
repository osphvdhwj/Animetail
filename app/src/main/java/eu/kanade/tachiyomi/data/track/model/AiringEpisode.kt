package eu.kanade.tachiyomi.data.track.model

data class AiringEpisode(
    val id: Long,
    val mediaId: Long,
    val title: String,
    val coverUrl: String,
    val episode: Int,
    val airingAt: Long, // unix timestamp in seconds
    val format: String = "TV",
    val score: Double = -1.0,
    val summary: String = "",
) {
    fun formatCountdown(): String {
        val nowSec = System.currentTimeMillis() / 1000L
        val diffSec = airingAt - nowSec
        return when {
            diffSec <= 0 -> {
                val elapsedMin = (-diffSec) / 60
                if (elapsedMin < 60) "Aired ${elapsedMin}m ago"
                else "Aired ${elapsedMin / 60}h ago"
            }
            diffSec < 3600 -> {
                val min = diffSec / 60
                "Ep $episode in ${min}m"
            }
            diffSec < 86400 -> {
                val hours = diffSec / 3600
                val min = (diffSec % 3600) / 60
                "Ep $episode in ${hours}h ${min}m"
            }
            else -> {
                val days = diffSec / 86400
                val hours = (diffSec % 86400) / 3600
                "Ep $episode in ${days}d ${hours}h"
            }
        }
    }
}
