package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import at.reisishot.mise.commons.*
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.jpeg.JpegDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.hasDeletions
import pictures.reisishot.mise.backend.toXml
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

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        if (changeFiles.hasRelevantDeletions())
            cleanup(configuration, cache)
        if (changeFiles.hasRelevantChanges()) {
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
            return true
        } else return false
    }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changeFiles: ChangeFileset): Boolean {
        if (changeFiles.hasRelevantChanges()) {
            buildInitialArtifacts(configuration, cache)
            return true
        } else return false
    }

    private fun ChangeFileset.hasRelevantChanges() = keys.asSequence().any { it.hasExtension(FileExtension::isJpeg) }
    private fun ChangeFileset.hasRelevantDeletions() = hasDeletions(FileExtension::isJpeg)

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            val existingFiles: Set<FilenameWithoutExtension> = Files.list(configuration.inPath.resolve(NAME_IMAGE_SUBFOLDER))
                    .map { it.filenameWithoutExtension }
                    .collect(Collectors.toSet())

            Files.list(configuration.tmpPath.withChild(NAME_THUMBINFO_SUBFOLDER))
                    .filter { !existingFiles.contains(it.filenameWithoutExtension.substringBeforeLast('.')) }
                    .forEach(Files::delete)

            Files.list(configuration.outPath.withChild(NAME_IMAGE_SUBFOLDER))
                    .filter { !existingFiles.contains(computeOriginalFilename(it.filenameWithoutExtension)) }
                    .forEach(Files::delete)
        }
    }

    override suspend fun fetchInitialInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>) {
        newFixedThreadPoolContext(4, "Convert").use { preparation ->
            configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list().filter { it.fileExtension.isJpeg() }.asSequence().asIterable().forEachParallel(preparation) { jpegImage ->
                val thumbnailInfoPath =
                        configuration.tmpPath withChild NAME_THUMBINFO_SUBFOLDER withChild "${configuration.inPath.resolve(jpegImage).filenameWithoutExtension}.cache.xml"
                if (!(thumbnailInfoPath.exists() && thumbnailInfoPath.isNewerThan(jpegImage))) {
                    val baseOutPath = configuration.outPath.resolve(NAME_IMAGE_SUBFOLDER).resolve(jpegImage.fileName)
                    withContext(Dispatchers.IO) {
                        Files.createDirectories(baseOutPath.parent)
                    }
                    ImageSize.ORDERED.asSequence().map { size ->
                        val outFile = size.decoratePath(baseOutPath)

                        (forceRegeneration.thumbnails || !outFile.exists() || jpegImage.isNewerThan(outFile)).let { actionNeeded ->
                            if (actionNeeded)
                                convertImage(jpegImage, outFile, size)
                        }

                        size to getThumbnailInfo(outFile)

                    }.filterNotNull().toMap().toXml(thumbnailInfoPath)
                }
            }
        }
    }

    protected abstract fun convertImage(inFile: Path, outFile: Path, size: ImageSize)

    protected fun getThumbnailInfo(jpegImage: Path): ThumbnailInformation {
        val exifData = ImageMetadataReader.readMetadata(jpegImage.toFile())
        val jpegExifData = exifData.getFirstDirectoryOfType(JpegDirectory::class.java)
        return ThumbnailInformation(jpegImage.fileName.toString(), jpegExifData.imageWidth, jpegExifData.imageHeight)
    }

    protected open fun computeOriginalFilename(generatedFilename: FilenameWithoutExtension): FilenameWithoutExtension = generatedFilename.substringBeforeLast('_')
}