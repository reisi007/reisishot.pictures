package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
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
        SMALL("icon", 300, 0.5f), MEDIUM("embed", 1000, 0.75f), LARGE("large", 2500, 0.85f);

        fun decoratePath(p: Path): Path = with(p) {
            parent withChild fileName.filenameWithoutExtension + '_' + identifier + ".jpg"
        }
    }

    data class ForceRegeneration(val thumbnails: Boolean = false)

    data class ThumbnailInformation(val filename: String, val width: Int, val height: Int)

    override val executionPriority: Int = 1_000
    override val generatorName: String = "Reisishot JPG Thumbnail generator"

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
    }

    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val outPath = configuration.outPath withChild NAME_IMAGE_SUBFOLDER
        withContext(Dispatchers.IO) {
            Files.createDirectories(outPath)
        }
        val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
            ?: throw IllegalStateException("Could not find a writer for JPEG!")
        ImageSize.values().let { imageSizes ->
            configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list().filter { it.isJpeg }
                .filter { inFile ->
                    if (forceRegeneration.thumbnails)
                        true
                    else
                        cache.hasFileChanged(inFile)
                }.asIterable().forEachLimitedParallel(10) { inFile ->
                    val baseOutFile = outPath withChild inFile.fileName
                    if (!forceRegeneration.thumbnails) {
                        sequenceOf(inFile).plus(
                            imageSizes.asSequence().map { it.decoratePath(baseOutFile) }
                        ).all { cache.hasFileChanged(it) }.let { changed ->
                            if (!changed)
                                return@forEachLimitedParallel
                        }
                    }

                    if (!Files.exists(inFile)) {
                        // Cleanup
                        imageSizes.forEach {
                            val decoratedPath = it.decoratePath(baseOutFile)
                            Files.deleteIfExists(decoratedPath)
                            cache.setFileChanged(decoratedPath, null)
                        }
                    } else {
                        // Generate
                        val image = inFile.readImage()

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
                                        val param = JPEGImageWriteParam(configuration.locale)
                                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                                        param.compressionQuality = imageSize.quality
                                        param.progressiveMode = ImageWriteParam.MODE_DEFAULT
                                        val streamData: IIOMetadata? = jpegWriter.getDefaultStreamMetadata(param)
                                        jpegWriter.output = imageOs

                                        jpegWriter.write(streamData, IIOImage(this, null, null), param)
                                        ZonedDateTime.now().let { fileTime ->
                                            cache.setFileChanged(realOutPath, fileTime)
                                            cache.setFileChanged(inFile, fileTime)
                                        }
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
                        with(configuration.inPath withChild NAME_THUMBINFO_SUBFOLDER withChild "${baseOutFile.filenameWithoutExtension}.json") {
                            thumbnailInfoMap.toJson(this)
                        }
                    }
                }
        }
    }
}