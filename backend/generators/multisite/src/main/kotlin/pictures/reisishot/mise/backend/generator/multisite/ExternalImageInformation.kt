package pictures.reisishot.mise.backend.generator.multisite

import at.reisishot.mise.commons.FilenameWithoutExtension
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.pictures.reisishot.mise.backend.generator.gallery.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.pictures.reisishot.mise.backend.generator.gallery.AbstractThumbnailGenerator.ImageSizeInformation

@Serializable
class ExternalImageInformation(
    private val host: String,
    override val filename: FilenameWithoutExtension,
    override val thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
    override val relativeLocation: String,
    override val title: String
) : ImageInformation() {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String = host + relativeLocation
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
