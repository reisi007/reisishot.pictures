package pictures.reisishot.mise.backend.generator.thumbnail

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.webp.WebpDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.ChangeFileset
import pictures.reisishot.mise.backend.config.JsonParser
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteGenerator
import pictures.reisishot.mise.backend.config.hasDeletions
import pictures.reisishot.mise.backend.config.useJsonParserParallel
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.FilenameWithoutExtension
import pictures.reisishot.mise.commons.fileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.forEachParallel
import pictures.reisishot.mise.commons.hasExtension
import pictures.reisishot.mise.commons.isJpeg
import pictures.reisishot.mise.commons.isNewerThan
import pictures.reisishot.mise.commons.list
import pictures.reisishot.mise.commons.withChild
import pictures.reisishot.mise.exifdata.height
import pictures.reisishot.mise.exifdata.width
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.math.max

abstract class AbstractThumbnailGenerator(private val forceRegeneration: ForceRegeneration) : WebsiteGenerator {

    companion object {
        const val NAME_IMAGE_SUBFOLDER = "images"
        const val NAME_THUMBINFO_SUBFOLDER = "thumbinfo"
    }

    override val executionPriority: Int = 1_000

    data class ForceRegeneration(val thumbnails: Boolean = false)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, cache: BuildingCache) {
    }

    enum class Interpolation(val value: String) {
        NONE("4:4:4"),
        LOW("4:2:2"),
        MEDIUM("4:2:0"),
        HIGH("4:1:1")
    }

    @Serializable
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
            val values = values()

            val LARGEST
                get() = values.last()
        }

        val smallerSize: ImageSize?
            get() = values.getOrNull(values.indexOf(this) - 1)
        val largerSize: ImageSize?
            get() = values.getOrNull(values.indexOf(this) + 1)

        override fun toString(): String {
            return identifier
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        if (changeFiles.hasRelevantDeletions())
            cleanup(configuration, buildingCache)
        return if (changeFiles.hasRelevantChanges()) {
            fetchInitialInformation(configuration, buildingCache, alreadyRunGenerators)
            true
        } else false
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        return if (changeFiles.hasRelevantChanges()) {
            buildInitialArtifacts(configuration, buildingCache)
            true
        } else false
    }

    private fun ChangeFileset.hasRelevantChanges() = keys.any { it.hasExtension(FileExtension::isJpeg) }
    private fun ChangeFileset.hasRelevantDeletions() = hasDeletions(FileExtension::isJpeg)

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        withContext(Dispatchers.IO) {
            val existingFiles: Set<FilenameWithoutExtension> =
                Files.list(configuration.paths.sourceFolder.resolve(NAME_IMAGE_SUBFOLDER))
                    .map { it.filenameWithoutExtension }
                    .collect(Collectors.toSet())

            val thumbInfoPath = configuration.paths.cacheFolder.withChild(NAME_THUMBINFO_SUBFOLDER)
            if (thumbInfoPath.exists())
                Files.list(thumbInfoPath)
                    .filter { !existingFiles.contains(it.filenameWithoutExtension.substringBeforeLast('.')) }
                    .forEach(Files::delete)

            val imagesPath = configuration.paths.targetFolder.withChild(NAME_IMAGE_SUBFOLDER)
            if (imagesPath.exists())
                Files.list(imagesPath)
                    .filter { !existingFiles.contains(computeOriginalFilename(it.filenameWithoutExtension)) }
                    .forEach(Files::delete)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ): Unit = configuration.useJsonParserParallel {
        configuration.paths.sourceFolder.withChild(NAME_IMAGE_SUBFOLDER).list()
            .filter { it.fileExtension.isJpeg() }
            .forEachParallel(
                Dispatchers.Default.limitedParallelism(
                    2 * Runtime.getRuntime().availableProcessors()
                )
            ) { originalImage ->
                processImage(
                    configuration,
                    originalImage
                )
            }

    }

    private suspend fun JsonParser.processImage(
        configuration: WebsiteConfig,
        originalImage: Path
    ) {
        val thumbnailInfoPath =
            configuration.paths.cacheFolder withChild NAME_THUMBINFO_SUBFOLDER withChild "${
                configuration.paths.sourceFolder.resolve(
                    originalImage
                ).filenameWithoutExtension
            }.cache.json"
        if (!(thumbnailInfoPath.exists() && thumbnailInfoPath.isNewerThan(originalImage))) {
            val baseOutPath =
                configuration.paths.targetFolder.resolve(NAME_IMAGE_SUBFOLDER).resolve(originalImage.fileName)
            withContext(Dispatchers.IO) {
                Files.createDirectories(baseOutPath.parent)
            }
            val sourceInfo = getJpegThumbnailInfo(originalImage)
            ImageSize.values
                .asSequence()
                .map { size -> generateThumbnails(baseOutPath, originalImage, sourceInfo, size) }
                .filterNotNull()
                .toMap()
                .toJson(thumbnailInfoPath)
        }
    }

    private fun generateThumbnails(
        baseOutPath: Path,
        originalImage: Path,
        sourceInfo: ImageSizeInformation,
        size: ImageSize
    ): Pair<ImageSize, ImageSizeInformation>? {
        val outFile = baseOutPath.parent withChild ("${baseOutPath.filenameWithoutExtension}_${size.identifier}.webp")

        (forceRegeneration.thumbnails || !outFile.exists() || originalImage.isNewerThan(outFile)).let { actionNeeded ->
            if (actionNeeded)
                convertImageInternally(originalImage, sourceInfo, outFile, size)
        }
        return if (!outFile.exists()) null else size to getWebPThumbnailInfo(outFile)
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
