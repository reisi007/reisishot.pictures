package pictures.reisishot.mise.backend.generator.gallery


interface ImageInformation {
    val filename: String
    val title: String
    val tags: List<String>
    val exifInformation: Map<String, String>
}

data class InternalImageInformation(
    override val filename: String,
    override val title: String,
    override val tags: List<String>,
    val categories: MutableList<CategoryName>,
    override val exifInformation: Map<String, String> = emptyMap()
) : ImageInformation

typealias CategoryName = List<String>