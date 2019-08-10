package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import com.sun.imageio.plugins.jpeg.JPEGImageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata
import javax.imageio.plugins.jpeg.JPEGImageWriteParam

class ImageIoThumbnailGenerator(forceRegeneration: ForceRegeneration = ForceRegeneration()) : AbstractThumbnailGenerator(forceRegeneration) {

    override val generatorName: String = "Reisishot ImageIO JPG"

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
                    .forEachLimitedParallel(4) { inFile ->
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
                                            param.compressionMode = JPEGImageWriteParam.MODE_EXPLICIT
                                            param.compressionQuality = imageSize.quality
                                            param.progressiveMode = JPEGImageWriteParam.MODE_DEFAULT
                                            param.optimizeHuffmanTables = true

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