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

    enum class Interpolation(val value: String) {
        NONE("4:4:4"),
        LOW("4:2:2"),
        MEDIUM("4:2:0"),
        HIGH("4:1:1")
    }

    enum class ImageSize(
            val identifier: String,
            val longestSidePx: Int,
            val quality: Float,
            val interpolation: Interpolation
    ) {
        SMALL("icon", 300, 0.35f, Interpolation.LOW),
        EMBED("embed", 700, 0.45f, Interpolation.HIGH),
        MEDIUM("medium", 1200, 0.45f, Interpolation.MEDIUM),
        LARGE("large", 2050, 0.45f, Interpolation.HIGH),
        XLARGE("xlarge", 3000, 0.55f, Interpolation.MEDIUM);

        companion object {
            val ORDERED = listOf(XLARGE, LARGE, MEDIUM, EMBED, SMALL)
            val SMALLEST = ORDERED.last()
            val LARGEST = ORDERED.first()

            fun getSize(minSize: Int) = ORDERED.asReversed()
                    .asSequence()
                    .firstOrNull { it.longestSidePx > minSize } ?: LARGEST
        }

        fun decoratePath(p: Path): Path = with(p) {
            parent withChild fileName.filenameWithoutExtension + '_' + identifier + ".jpg"
        }


        val smallerSize: ImageSize?
            get() = ORDERED.listIterator().find(this)?.nextOrNull() ?: SMALLEST


        val biggerSize: ImageSize?
            get() = ORDERED.listIterator().find(this)?.previousOrNull() ?: LARGEST


        private fun <T> ListIterator<T>.find(imageSize: ImageSize): ListIterator<T>? {
            while (hasNext()) {
                val next = next()
                if (imageSize == next)
                    return this
            }
            return null
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

fun AbstractThumbnailGenerator.ThumbnailInformation.scaleToHeight(desiredHeight: Int): AbstractThumbnailGenerator.ThumbnailInformation = copy(height = desiredHeight, width = width * desiredHeight / height)