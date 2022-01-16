package pictures.reisishot.mise.backend

import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation

@Serializable
abstract class ImageInformation {
    abstract val filename: FilenameWithoutExtension
    abstract val relativeLocation: String
    abstract val thumbnailSizes: Map<ImageSize, ImageSizeInformation>
    abstract val title: String

    abstract fun getUrl(websiteConfig: WebsiteConfig): String
}
