package pictures.reisishot.mise.config

import kotlinx.serialization.Serializable

@Serializable
data class ImageConfig(
    val title: String,
    val tags: MutableSet<String>
)
