package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.commons.CategoryName
import pictures.reisishot.mise.commons.FilenameWithoutExtension


@kotlinx.serialization.Serializable
internal open class JsonGallery(
    val images: List<JsonImageData>,
    val nextPage: String?,
)

@kotlinx.serialization.Serializable
internal class JsonGalleryFirst(
    val subcategories: List<Subcategory>?,
    val images: List<JsonImageData>,
    val nextPage: String?,
)

@kotlinx.serialization.Serializable
data class JsonImageData(val filename: FilenameWithoutExtension, val width: Int, val height: Int)

@kotlinx.serialization.Serializable
data class Subcategory(
    val categoryName: CategoryName,
    val image: FilenameWithoutExtension,
    val width: Int,
    val height: Int
)

