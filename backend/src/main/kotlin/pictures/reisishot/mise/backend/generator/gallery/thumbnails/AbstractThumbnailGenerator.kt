package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.filenameWithoutExtension
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.withChild
import java.nio.file.Path

abstract class AbstractThumbnailGenerator(protected val forceRegeneration: ForceRegeneration) : WebsiteGenerator {

    companion object {
        const val NAME_IMAGE_SUBFOLDER = "images"
        const val NAME_THUMBINFO_SUBFOLDER = "thumbinfo"
    }

    override val executionPriority: Int = 1_000

    data class ThumbnailInformation(val filename: String, val width: Int, val height: Int)

    enum class ImageSize(private val identifier: String, val longestSidePx: Int, val quality: Float) {
        SMALL("icon", 300, 0.35f),
        MEDIUM("embed", 1400, 0.5f),
        LARGE("large", 3000, 0.75f);

        companion object {
            val ORDERED = arrayOf(LARGE, MEDIUM, SMALL)
            val SMALLEST = ORDERED.last()
            val LARGEST = ORDERED.first()
        }

        fun decoratePath(p: Path): Path = with(p) {
            parent withChild fileName.filenameWithoutExtension + '_' + identifier + ".jpg"
        }

        val smallerSize: ImageSize?
            get() = when (this) {
                SMALL -> null
                MEDIUM -> SMALL
                LARGE -> MEDIUM
            }

        val biggerSize: ImageSize?
            get() = when (this) {
                SMALL -> MEDIUM
                MEDIUM -> LARGE
                LARGE -> null
            }
    }

    data class ForceRegeneration(val thumbnails: Boolean = false)

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }
}