package pictures.reisishot.mise.backend.generator.gallery

import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ThumbnailInformation


interface ImageInformation {
    val url: FilenameWithoutExtension
    val title: String
    val tags: Set<String>
    val exifInformation: Map<ExifdataKey, String>
    val thumbnailSizes: Map<ImageSize, ThumbnailInformation>
    val filenameWithoutExtension: FilenameWithoutExtension
        get() = url

}

data class InternalImageInformation(
        override val url: FilenameWithoutExtension,
        override val title: String,
        override val tags: Set<String>,
        override val exifInformation: Map<ExifdataKey, String>,
        override val thumbnailSizes: Map<ImageSize, ThumbnailInformation>,
        val categories: MutableSet<CategoryInformation> = mutableSetOf()
) : ImageInformation