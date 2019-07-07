package pictures.reisishot.mise.backend.generator.gallery

data class ImageConfig(
    val title: String,
    val url: String,
    val categoryThumbnail: Set<CategoryName> = emptySet(),
    val tags: Set<TagName>
)