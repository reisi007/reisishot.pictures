package at.reisishot.mise.config

data class ImageConfig(
        val title: String,
        val categoryThumbnail: Set<String> = emptySet(),
        val tags: Set<String>,
        val showInGallery: Boolean = true
)