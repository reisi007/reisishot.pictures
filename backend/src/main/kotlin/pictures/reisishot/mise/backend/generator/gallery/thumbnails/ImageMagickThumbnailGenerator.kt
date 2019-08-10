package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.jpeg.JpegDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.nio.file.Files
import java.nio.file.Path

class ImageMagickThumbnailGenerator(forceRegeneration: ForceRegeneration = ForceRegeneration()) : AbstractThumbnailGenerator(forceRegeneration) {
    override val generatorName: String = "Image Magick Thumbnail"


    override suspend fun fetchInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>) {
        newFixedThreadPoolContext(4, "Convert").use { preparation ->
            configuration.inPath.withChild(NAME_IMAGE_SUBFOLDER).list().filter { it.isRegularFile() && it.isJpeg }.asSequence().asIterable().forEachParallel(preparation) { jpegImage ->
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
                                executeWithConsole(size.getImageMagickString(jpegImage, outFile))
                        }

                        size to getThumbnailInfo(outFile)

                    }.filterNotNull().toMap().toXml(thumbnailInfoPath)
                }
            }
        }
    }

    private fun executeWithConsole(command: Array<String>) =
            ProcessBuilder(*command)
                    .inheritIO()
                    .start()
                    .waitFor()

    private fun getThumbnailInfo(jpegImage: Path): ThumbnailInformation {
        val exifData = ImageMetadataReader.readMetadata(jpegImage.toFile())
        val jpegExifData = exifData.getFirstDirectoryOfType(JpegDirectory::class.java)
        return ThumbnailInformation(jpegImage.fileName.toString(), jpegExifData.imageHeight, jpegExifData.imageWidth)
    }

    private fun ImageSize.getImageMagickString(inFile: Path, outFile: Path): Array<String> =
            arrayOf("magick", inFile.toAbsolutePath().normalize().toString(),
                    "-quality", "${(quality * 100).toInt()}",
                    "-resize", "${longestSidePx}x${longestSidePx}>",
                    "-strip",
                    "-interlace", "Plane",
                    outFile.toAbsolutePath().normalize().toString())
}