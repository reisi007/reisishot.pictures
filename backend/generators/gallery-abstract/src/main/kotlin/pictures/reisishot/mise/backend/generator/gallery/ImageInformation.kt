package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation
import pictures.reisishot.mise.commons.FilenameWithoutExtension

@Serializable
abstract class ImageInformation {
    abstract val filename: FilenameWithoutExtension
    abstract val relativeLocation: String
    abstract val thumbnailSizes: Map<ImageSize, ImageSizeInformation>
    abstract val title: String

    abstract fun getUrl(websiteConfig: WebsiteConfig): String
}
