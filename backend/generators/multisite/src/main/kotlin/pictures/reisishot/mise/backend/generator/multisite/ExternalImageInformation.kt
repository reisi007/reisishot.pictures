package pictures.reisishot.mise.backend.generator.multisite

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation
import pictures.reisishot.mise.commons.FilenameWithoutExtension


@Serializable
class ExternalImageInformation(
    private val host: String,
    override val filename: FilenameWithoutExtension,
    override val thumbnailSizes: Map<AbstractThumbnailGenerator.ImageSize, ImageSizeInformation>,
    override val relativeLocation: String,
    override val title: String
) : ImageInformation() {
    override fun getUrl(websiteConfig: WebsiteConfig): String = host + relativeLocation
    override fun toString(): String {
        return "ExternalImageInformation(host='$host')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExternalImageInformation

        if (host != other.host) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + filename.hashCode()
        return result
    }


}

internal fun ImageInformation.toExternal(host: String) =
    ExternalImageInformation(host, filename, thumbnailSizes, relativeLocation, title)
