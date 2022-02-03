package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.tags.TagInformation
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator.ImageSize
import pictures.reisishot.mise.backend.generator.thumbnail.ImageSizeInformation
import pictures.reisishot.mise.commons.ConcurrentSet
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.concurrentSetOf
import pictures.reisishot.mise.exifdata.ExifdataKey
import pictures.reisishot.mise.backend.config.ImageInformation as ConfigImageInformation


@Serializable
class InternalImageInformation(
    override val filename: FilenameWithoutExtension,
    override val thumbnailSizes: Map<ImageSize, ImageSizeInformation>,
    override val relativeLocation: String,
    override val title: String,
    override val tags: ConcurrentSet<TagInformation>,
    override val exifInformation: MutableMap<ExifdataKey, String>,
    override val categories: ConcurrentSet<String> = concurrentSetOf(),
) : ConfigImageInformation, ImageInformation() {
    override fun getUrl(websiteConfig: WebsiteConfig): String =
        BuildingCache.getLinkFromFragment(websiteConfig, relativeLocation)

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



