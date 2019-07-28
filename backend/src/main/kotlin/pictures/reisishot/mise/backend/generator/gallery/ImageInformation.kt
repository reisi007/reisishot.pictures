package pictures.reisishot.mise.backend.generator.gallery


interface ImageInformation {
    val url: FilenameWithoutExtension
    val title: String
    val tags: Set<TagName>
    val exifInformation: Map<ExifdataKey, String>
    val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>
    val filenameWithoutExtension: FilenameWithoutExtension
        get() = url

}

data class InternalImageInformation(
    override val url: FilenameWithoutExtension,
    override val title: String,
    override val tags: Set<TagName>,
    override val exifInformation: Map<ExifdataKey, String>,
    override val thumbnailSizes: Map<ThumbnailGenerator.ImageSize, ThumbnailGenerator.ThumbnailInformation>,
    val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation