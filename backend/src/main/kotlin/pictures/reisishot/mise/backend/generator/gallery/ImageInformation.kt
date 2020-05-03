package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ThumbnailInformation


sealed class ImageInformation(
        val filename: FilenameWithoutExtension,
        val thumbnailSizes: Map<ImageSize, ThumbnailInformation>,
        val href: String,
        val title: String
)

class InternalImageInformation(
        filename: FilenameWithoutExtension,
        thumbnailSizes: Map<ImageSize, ThumbnailInformation>,
        href: String,
        title: String,
        val tags: Set<String>,
        val exifInformation: Map<ExifdataKey, String>,
        val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation(filename, thumbnailSizes, href, title)

class ExternalImageInformation(
        filename: FilenameWithoutExtension,
        thumbnailSizes: Map<ImageSize, ThumbnailInformation>,
        href: String,
        title: String
) : ImageInformation(filename, thumbnailSizes, href, title)

fun ImageInformation.toExternal() = ExternalImageInformation(filename, thumbnailSizes, href, title)