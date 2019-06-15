package pictures.reisishot.mise.backend.generator.gallery


interface ImageInformation {
    val fileLocation: ImageFilename
    val filenameWithoutExtension: FilenameWithoutExtension
    val title: String
    val tags: Set<TagName>
    val exifInformation: Map<ExifdataKey, String>
}

data class InternalImageInformation(
    override val fileLocation: ImageFilename,
    override val filenameWithoutExtension: FilenameWithoutExtension,
    override val title: String,
    override val tags: Set<TagName>,
    override val exifInformation: Map<ExifdataKey, String>,
    val categories: MutableSet<CategoryName> = mutableSetOf()
) : ImageInformation