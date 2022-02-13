package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation

@kotlinx.serialization.Serializable
class JsonImage(
    val base: String? = null,
    val thumbnailSizes: Map<AbstractThumbnailGenerator.ImageSize, ImageSizeInformation>
) {
}
