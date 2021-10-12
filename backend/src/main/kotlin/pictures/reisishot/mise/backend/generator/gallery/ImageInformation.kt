package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSizeInformation

@Serializable
sealed class ImageInformation {
    abstract val filename: FilenameWithoutExtension
    abstract val relativeLocation: String
    abstract val thumbnailSizes: Map<ImageSize, ImageSizeInformation>
    abstract val title: String

    abstract fun getUrl(websiteConfiguration: WebsiteConfiguration): String
}

@Serializable
class InternalImageInformation(
    override val filename: FilenameWithoutExtension,
    override val thumbnailSizes: MutableMap<ImageSize, ImageSizeInformation>,
    override val relativeLocation: String,
    override val title: String,
    val tags: MutableSet<String>,
    val exifInformation: MutableMap<ExifdataKey, String>,
    val categories: MutableSet<CategoryInformation> = mutableSetOf(),
) : ImageInformation() {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String =
        BuildingCache.getLinkFromFragment(websiteConfiguration, relativeLocation)

    override fun toString(): String {
        return "InternalImageInformation(tags=$tags, exifInformation=$exifInformation, categories=$categories)"
    }


}

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


}

fun ImageInformation.toExternal(host: String) =
    ExternalImageInformation(host, filename, thumbnailSizes, relativeLocation, title)
