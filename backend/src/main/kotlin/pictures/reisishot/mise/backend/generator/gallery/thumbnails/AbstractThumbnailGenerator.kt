package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.FileExtension
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.filenameWithoutExtension
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import pictures.reisishot.mise.backend.withChild
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

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

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changedFiles: ChangedFileset) {
        if (changedFiles.hasRelevantDeletions())
            cleanup(configuration, cache)
        if (changedFiles.hasRelevantChanges())
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
    }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changedFiles: ChangedFileset) {
        if (changedFiles.hasRelevantChanges())
            buildInitialArtifacts(configuration, cache)
    }

    private fun ChangedFileset.hasRelevantChanges() = keys.asSequence().any { it.hasExtension(FileExtension::isJpeg) }
    private fun ChangedFileset.hasRelevantDeletions() = hasDeletions(FileExtension::isJpeg)

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            val existingFiles: Set<FilenameWithoutExtension> = Files.list(configuration.inPath.resolve(NAME_IMAGE_SUBFOLDER))
                    .map { it.filenameWithoutExtension }
                    .collect(Collectors.toSet())

            Files.list(configuration.tmpPath.withChild(NAME_THUMBINFO_SUBFOLDER))
                    .filter { !existingFiles.contains(it.filenameWithoutExtension) }
                    .forEach(Files::delete)

            Files.list(configuration.outPath.withChild(NAME_IMAGE_SUBFOLDER))
                    .filter { !existingFiles.contains(computeOriginalFilename(it.filenameWithoutExtension)) }
                    .forEach(Files::delete)
        }
    }

    protected open fun computeOriginalFilename(generatedFilename: FilenameWithoutExtension): FilenameWithoutExtension = generatedFilename.substringBeforeLast('/')
}