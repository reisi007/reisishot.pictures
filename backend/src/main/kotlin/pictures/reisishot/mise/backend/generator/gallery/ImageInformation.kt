package pictures.reisishot.mise.backend.generator.gallery

open class SimpleImageInformation(
    open val filename: String,
    open val title: String
)

data class ExtendedImageInformation(
    override val filename: String,
    override val title: String,
    val exifInformation: Map<String, String> = emptyMap()
) : SimpleImageInformation(filename, title)