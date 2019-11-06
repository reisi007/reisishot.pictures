package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import at.reisishot.mise.commons.runAndWaitOnConsole
import at.reisishot.mise.commons.toNormalizedString
import java.nio.file.Path

open class ImageMagickThumbnailGenerator(forceRegeneration: ForceRegeneration = ForceRegeneration()) : AbstractThumbnailGenerator(forceRegeneration) {
    override val generatorName: String = "Image Magick Thumbnail"

    override fun convertImage(inFile: Path, outFile: Path, size: ImageSize) = arrayOf(
            "magick", inFile.toNormalizedString(),
            "-quality", "${(size.quality * 100).toInt()}",
            "-resize", "${size.longestSidePx}x${size.longestSidePx}>",
            "-strip",
            "-interlace", "Plane",
            outFile.toNormalizedString()
    ).runAndWaitOnConsole()


}
