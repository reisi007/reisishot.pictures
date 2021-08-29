package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSizeInformation


sealed class ImageInformation(
    val filename: FilenameWithoutExtension,
    val relativeLocation: String,
    val thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
    val title: String
) {
    abstract fun getUrl(websiteConfiguration: WebsiteConfiguration): String
}

class InternalImageInformation(
    filename: FilenameWithoutExtension,
    thumbnailSizes: MutableMap<ImageSize, ImageSizeInformation>,
    relativeLocation: String,
    title: String,
    val tags: MutableSet<String>,
    val exifInformation: MutableMap<ExifdataKey, String>,
    val categories: MutableSet<CategoryInformation> = mutableSetOf(),
) : ImageInformation(filename, relativeLocation, thumbnailSizes, title) {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String =
        BuildingCache.getLinkFromFragment(websiteConfiguration, relativeLocation)

    override fun toString(): String {
        return "InternalImageInformation(tags=$tags, exifInformation=$exifInformation, categories=$categories)"
    }


}

class ExternalImageInformation(
    private val host: String,
    filename: FilenameWithoutExtension,
    thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
    relativeLocation: String,
    title: String
) : ImageInformation(filename, relativeLocation, thumbnailSizes, title) {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String = host + relativeLocation
    override fun toString(): String {
        return "ExternalImageInformation(host='$host')"
    }


}

fun ImageInformation.toExternal(host: String) =
    ExternalImageInformation(host, filename, thumbnailSizes, relativeLocation, title)
