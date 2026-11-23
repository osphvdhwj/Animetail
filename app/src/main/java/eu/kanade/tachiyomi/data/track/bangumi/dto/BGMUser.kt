package eu.kanade.tachiyomi.data.track.bangumi.dto

import kotlinx.serialization.Serializable

@Serializable
data class BGMAvatar(
    val large: String? = null,
    val medium: String? = null,
    val small: String? = null,
)

@Serializable
// Incomplete DTO with only our needed attributes
data class BGMUser(
    val username: String,
    val nickname: String? = null,
    val avatar: BGMAvatar? = null,
)
