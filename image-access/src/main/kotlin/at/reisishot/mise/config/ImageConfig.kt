package at.reisishot.mise.config

import kotlinx.serialization.Serializable

@Serializable
data class ImageConfig(
    val title: String,
    val categoryThumbnail: MutableSet<String> = mutableSetOf(),
    val tags: MutableSet<String>
)
