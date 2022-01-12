package pictures.reisishot.mise.backend.generator.gallery

import at.reisishot.mise.commons.ConcurrentSet
import at.reisishot.mise.commons.FilenameWithoutExtension
import at.reisishot.mise.commons.concurrentSetOf
import at.reisishot.mise.exifdata.ExifdataKey
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.BuildingCache
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.generator.gallery.pictures.reisishot.mise.backend.generator.gallery.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.gallery.pictures.reisishot.mise.backend.generator.gallery.AbstractThumbnailGenerator.ImageSizeInformation
import pictures.reisishot.mise.backend.config.ImageInformation as ConfigImageInformation

@Serializable
abstract class ImageInformation {
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
    override val tags: ConcurrentSet<TagInformation>,
    override val exifInformation: MutableMap<ExifdataKey, String>,
    override val categories: ConcurrentSet<String> = concurrentSetOf(),
) : ConfigImageInformation, ImageInformation() {
    override fun getUrl(websiteConfiguration: WebsiteConfiguration): String =
        BuildingCache.getLinkFromFragment(websiteConfiguration, relativeLocation)

    override fun toString(): String {
        return "InternalImageInformation(tags=$tags, exifInformation=$exifInformation, categories=$categories)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InternalImageInformation

        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        return filename.hashCode()
    }


}


