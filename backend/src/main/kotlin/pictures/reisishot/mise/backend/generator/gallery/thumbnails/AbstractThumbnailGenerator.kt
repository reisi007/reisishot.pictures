package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.height
import at.reisishot.mise.exifdata.width
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.webp.WebpDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.hasDeletions
import pictures.reisishot.mise.backend.toJson
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.math.max

abstract class AbstractThumbnailGenerator(protected val forceRegeneration: ForceRegeneration) : WebsiteGenerator {

    companion object {
        const val NAME_IMAGE_SUBFOLDER = "images"
        const val NAME_THUMBINFO_SUBFOLDER = "thumbinfo"
    }

    override val executionPriority: Int = 1_000

    @Serializable
    data class ImageSizeInformation(val filename: String, val width: Int, val height: Int)

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
        EMBED("embed", 400, 0.5f, Interpolation.MEDIUM),
        THUMB("thumb", 700, 0.5f, Interpolation.MEDIUM),
        MEDIUM("medium", 1200, 0.5f, Interpolation.MEDIUM),
        LARGE("large", 2050, 0.6f, Interpolation.MEDIUM);

        companion object {

            private val _data by lazy {
                ImageSize.values()
                    .associateByTo(TreeMap<Int, ImageSize>(), { it.ordinal }, { it })
            }

            val SMALLEST
                get() = _data.firstEntry().value
            val LARGEST
                get() = _data.lastEntry().value

        }

        val largerSize: ImageSize?
            get() = _data.get(ordinal + 1)
        val smallerSize: ImageSize?
            get() = _data.get(ordinal - 1)

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

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        if (changeFiles.hasRelevantDeletions())
            cleanup(configuration, cache)
        return if (changeFiles.hasRelevantChanges()) {
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
            true
        } else false
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        return if (changeFiles.hasRelevantChanges()) {
            buildInitialArtifacts(configuration, cache)
            true
        } else false
    }

    private fun ChangeFileset.hasRelevantChanges() = keys.asSequence().any { it.hasExtension(FileExtension::isJpeg) }
    private fun ChangeFileset.hasRelevantDeletions() = hasDeletions(FileExtension::isJpeg)

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        withContext(Dispatchers.IO) {
            val existingFiles: Set<FilenameWithoutExtension> =
                Files.list(configuration.inPath.resolve(NAME_IMAGE_SUBFOLDER))
                    .map { it.filenameWithoutExtension }
                    .collect(Collectors.toSet())

            val thumbInfoPath = configuration.tmpPath.withChild(NAME_THUMBINFO_SUBFOLDER)
            if (thumbInfoPath.exists())
                Files.list(thumbInfoPath)
                    .filter { !existingFiles.contains(it.filenameWithoutExtension.substringBeforeLast('.')) }
                    .forEach(Files::delete)

            val imagesPath = configuration.outPath.withChild(NAME_IMAGE_SUBFOLDER)
            if (imagesPath.exists())
                Files.list(imagesPath)
                    .filter { !existingFiles.contains(computeOriginalFilename(it.filenameWithoutExtension)) }
                    .forEach(Files::delete)
        }
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ): Unit = Executors.newFixedThreadPool(4)
        .asCoroutineDispatcher()
        .use { preparation ->
            configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list()
                .filter { it.fileExtension.isJpeg() }
                .asIterable()
                .forEachParallel(preparation) { originalImage -> processImage(configuration, originalImage) }
        }


    private suspend fun processImage(
        configuration: WebsiteConfiguration,
        originalImage: Path
    ) {
        val thumbnailInfoPath =
            configuration.tmpPath withChild NAME_THUMBINFO_SUBFOLDER withChild "${
                configuration.inPath.resolve(
                    originalImage
                ).filenameWithoutExtension
            }.cache.json"
        if (!(thumbnailInfoPath.exists() && thumbnailInfoPath.isNewerThan(originalImage))) {
            val baseOutPath =
                configuration.outPath.resolve(NAME_IMAGE_SUBFOLDER).resolve(originalImage.fileName)
            withContext(Dispatchers.IO) {
                Files.createDirectories(baseOutPath.parent)
            }
            val sourceInfo = getJpegThumbnailInfo(originalImage)
            ImageSize.values()
                .asSequence()
                .map { size -> generateThumbnails(baseOutPath, originalImage, sourceInfo, size) }
                .toMap()
                .toJson(thumbnailInfoPath)
        }
    }

    private fun generateThumbnails(
        baseOutPath: Path,
        originalImage: Path,
        sourceInfo: ImageSizeInformation,
        size: ImageSize
    ): Pair<ImageSize, ImageSizeInformation> {
        val outFile = baseOutPath.parent withChild ("${baseOutPath.fileName}_${size.identifier}.webp")

        (forceRegeneration.thumbnails || !outFile.exists() || originalImage.isNewerThan(outFile)).let { actionNeeded ->
            if (actionNeeded)
                convertImageInternally(originalImage, sourceInfo, outFile, size)
        }
        return size to getWebPThumbnailInfo(outFile)
    }

    private fun convertImageInternally(
        jpegImage: Path,
        sourceInfo: ImageSizeInformation,
        outFile: Path,
        size: ImageSize
    ) {
        size.smallerSize?.let {
            if (max(sourceInfo.height, sourceInfo.width) < it.longestSidePx) return
        }
        convertImage(jpegImage, outFile, size)
    }

    /**
     * Should convert an image to Webp and jpeg at the moment -> jpeg will not be needed in the future
     */
    protected abstract fun convertImage(inFile: Path, outFile: Path, prefferedSize: ImageSize)

    private fun getJpegThumbnailInfo(jpegImage: Path): ImageSizeInformation {
        val exifData = findDirectory<JpegDirectory>(jpegImage)
        return ImageSizeInformation(jpegImage.fileName.toString(), exifData.imageWidth, exifData.imageHeight)
    }

    private fun getWebPThumbnailInfo(webpImage: Path): ImageSizeInformation {
        val exifData = findDirectory<WebpDirectory>(webpImage)
        return ImageSizeInformation(webpImage.fileName.toString(), exifData.width, exifData.height)
    }

    private inline fun <reified D : Directory> findDirectory(image: Path): D {
        val exifData = ImageMetadataReader.readMetadata(image.toFile())
        val directoryClass = D::class.java
        return exifData.getFirstDirectoryOfType(directoryClass)
            ?: throw IllegalStateException("$directoryClass not found")
    }

    protected open fun computeOriginalFilename(generatedFilename: FilenameWithoutExtension): FilenameWithoutExtension =
        generatedFilename.substringBeforeLast('_')
}
