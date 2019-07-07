package pictures.reisishot.mise.backend.generator.gallery


interface ImageInformation {
    val filenameWithoutExtension: FilenameWithoutExtension
    val url: String
    val title: String
    val tags: Set<TagName>
    val exifInformation: Map<ExifdataKey, String>
    val categoryThumbnails: Set<CategoryName>
    val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>
}

data class InternalImageInformation(
    override val filenameWithoutExtension: FilenameWithoutExtension,
    override val url: String,
    override val title: String,
    override val tags: Set<TagName>,
    override val exifInformation: Map<ExifdataKey, String>,
    override val categoryThumbnails: Set<CategoryName>,
    override val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>,
    val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation