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
        val title: String,
        val showInGallery: Boolean
) {
    abstract fun getUrl(websiteConfiguration: WebsiteConfiguration): String
}

class InternalImageInformation(
        filename: FilenameWithoutExtension,
        thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
        relativeLocation: String,
        title: String,
        showInGallery: Boolean,
        val tags: Set<String>,
        val exifInformation: Map<ExifdataKey, String>,
        val categories: MutableSet<CategoryInformation> = mutableSetOf(),
) : ImageInformation(filename, relativeLocation, thumbnailSizes, title, showInGallery) {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String =
            BuildingCache.getLinkFromFragment(websiteConfiguration, relativeLocation)
}

class ExternalImageInformation(
        private val host: String,
        filename: FilenameWithoutExtension,
        thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
        relativeLocation: String,
        title: String
) : ImageInformation(filename, relativeLocation, thumbnailSizes, title, false) {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String = host + relativeLocation
}

fun ImageInformation.toExternal(host: String) = ExternalImageInformation(host, filename, thumbnailSizes, relativeLocation, title)