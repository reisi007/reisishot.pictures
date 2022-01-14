package pictures.reisishot.mise.backend

import at.reisishot.mise.backend.config.WebsiteConfig
import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.serialization.Serializable

@Serializable
abstract class ImageInformation {
    abstract val filename: FilenameWithoutExtension
    abstract val relativeLocation: String
    abstract val thumbnailSizes: Map<ImageSize, ImageSizeInformation>
    abstract val title: String

    abstract fun getUrl(websiteConfig: WebsiteConfig): String
}

@Serializable
data class ImageSizeInformation(val filename: String, val width: Int, val height: Int)
