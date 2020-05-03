package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ThumbnailInformation


interface ImageInformation {
    val filename: FilenameWithoutExtension
    val href: String
    val title: String
    val tags: Set<String>
    val exifInformation: Map<ExifdataKey, String>
    val thumbnailSizes: Map<ImageSize, ThumbnailInformation>

}

data class InternalImageInformation(
        override val filename: FilenameWithoutExtension,
        override val href: String,
        override val title: String,
        override val tags: Set<String>,
        override val exifInformation: Map<ExifdataKey, String>,
        override val thumbnailSizes: Map<ImageSize, ThumbnailInformation>,
        val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation