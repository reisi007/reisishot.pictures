package pictures.reisishot.mise.backend.generator.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.forEachLimitedParallel
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.readImage
import pictures.reisishot.mise.backend.withChild
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadata
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import kotlin.streams.asSequence


@ObsoleteCoroutinesApi
class ThumbnailGenerator(val forceRegeneration: ForceRegeneration = ForceRegeneration()) : WebsiteGenerator {

    enum class ImageSize(private val prefix: String, val longestSidePx: Int, val quality: Float) {
        SMALL("icon", 300, 0.5f), MEDIUM("embed", 1000, 0.75f), LARGE("large", 2500, 0.85f);

        fun decoratePath(p: Path): Path = with(p) {
            parent withChild prefix + '_' + fileName
        }

    }

    data class ForceRegeneration(val thumbnails: Boolean = false)

    override val executionPriority: Int = 1_000
    override val generatorName: String = "Reisishot JPG Thumbnail generator"

    suspend override fun generate(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val outPath = configuration.outPath withChild "images"
        withContext(Dispatchers.IO) {
            Files.createDirectories(outPath)
        }
        val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
            ?: throw IllegalStateException("Could not find a writer for JPEG!")
        ImageSize.values().let { imageSizes ->
            configuration.inPath.withChild("images").list().filter { it.isJpeg }
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
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun Path.list(): Sequence<Path> = Files.list(this).asSequence()
}