package at.reisishot.mise.config

data class ImageConfig(
    val title: String,
    val categoryThumbnail: MutableSet<String> = mutableSetOf(),
    val tags: MutableSet<String>
)
