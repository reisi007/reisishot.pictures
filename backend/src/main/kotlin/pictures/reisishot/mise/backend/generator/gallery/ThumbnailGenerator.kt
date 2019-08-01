package pictures.reisishot.mise.backend.generator.gallery

import com.sun.imageio.plugins.jpeg.JPEGImageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadata
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

class ThumbnailGenerator(val forceRegeneration: ForceRegeneration = ForceRegeneration()) : WebsiteGenerator {

    companion object {
        const val NAME_IMAGE_SUBFOLDER = "images"
        const val NAME_THUMBINFO_SUBFOLDER = "thumbinfo"
    }

    enum class ImageSize(private val identifier: String, val longestSidePx: Int, val quality: Float) {
        SMALL("icon", 300, 0.35f), MEDIUM("embed", 1000, 0.5f), LARGE("large", 2500, 0.75f);

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

    data class ThumbnailInformation(val filename: String, val width: Int, val height: Int)

    override val executionPriority: Int = 1_000
    override val generatorName: String = "Reisishot JPG"

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }

    private fun Path.isNewerThan(other: Path): Boolean =
        Files.getLastModifiedTime(this) > Files.getLastModifiedTime(other)


    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val outPath = configuration.outPath withChild NAME_IMAGE_SUBFOLDER
        withContext(Dispatchers.IO) {
            Files.createDirectories(outPath)
        }
        ImageSize.values().let { imageSizes ->
            configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list().filter { it.isJpeg }.asIterable()
                .forEachLimitedParallel(2) { inFile ->
                    val baseOutFile = outPath withChild inFile.fileName
                    val thumbnailInfoPath =
                        configuration.tmpPath withChild NAME_THUMBINFO_SUBFOLDER withChild "${baseOutFile.filenameWithoutExtension}.cache.xml"
                    if (!forceRegeneration.thumbnails) {
                        sequenceOf(inFile).plus(
                            imageSizes.asSequence().map { it.decoratePath(baseOutFile) }
                        ).all { it.exists() && it.isNewerThan(inFile) }.let { changed ->
                            if (!changed && imageSizes.asSequence().map { it.decoratePath(baseOutFile) }.all { it.exists() }) {
                                if (!Files.exists(thumbnailInfoPath)) {
                                    val thumbnailInfoMap = mutableMapOf<ImageSize, ThumbnailInformation>()
                                    ImageSize.ORDERED.forEach { size ->
                                        val imagePath = size.decoratePath(baseOutFile)
                                        val image = imagePath.readImage()
                                        thumbnailInfoMap.put(
                                            size,
                                            ThumbnailInformation(
                                                imagePath.fileName.toString(),
                                                image.width,
                                                image.height
                                            )
                                        )
                                    }
                                    thumbnailInfoMap.toXml(thumbnailInfoPath)
                                }
                                return@forEachLimitedParallel
                            }
                        }
                    }

                    if (!Files.exists(inFile)) {
                        // Cleanup
                        imageSizes.forEach {
                            val decoratedPath = it.decoratePath(baseOutFile)
                            Files.deleteIfExists(decoratedPath)
                        }
                    } else {
                        // Generate
                        val image = inFile.readImage()

                        val jpegWriterMap = mutableMapOf<Thread, JPEGImageWriter>()

                        val thumbnailInfoMap = mutableMapOf<ImageSize, ThumbnailInformation>()
                        imageSizes.forEach { imageSize ->
                            with(
                                Thumbnails.of(image)
                                    .size(imageSize.longestSidePx, imageSize.longestSidePx)
                                    .asBufferedImage()
                            ) {
                                imageSize.decoratePath(baseOutFile).let { realOutPath ->
                                    ImageIO.createImageOutputStream(
                                        Files.newOutputStream(
                                            realOutPath,
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.TRUNCATE_EXISTING
                                        )
                                    ).use { imageOs ->
                                        val jpegWriter = jpegWriterMap.computeIfAbsent(Thread.currentThread()) {
                                            ImageIO.getImageWritersByFormatName("jpeg").next() as? JPEGImageWriter
                                                ?: throw IllegalStateException("Could not find a writer for JPEG!")
                                        }
                                        val param = JPEGImageWriteParam(configuration.locale)
                                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                                        param.compressionQuality = imageSize.quality
                                        param.progressiveMode = ImageWriteParam.MODE_DEFAULT
                                        val streamData: IIOMetadata? = jpegWriter.getDefaultStreamMetadata(param)
                                        jpegWriter.output = imageOs

                                        jpegWriter.write(streamData, IIOImage(this, null, null), param)
                                        thumbnailInfoMap.put(
                                            imageSize, ThumbnailInformation(
                                                realOutPath.fileName.toString(),
                                                width,
                                                height
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        thumbnailInfoMap.toXml(thumbnailInfoPath)
                    }
                }
        }
    }
}