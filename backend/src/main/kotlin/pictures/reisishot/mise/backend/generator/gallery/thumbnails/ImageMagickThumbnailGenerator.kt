package pictures.reisishot.mise.backend.generator.gallery.thumbnails

import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.commons.runAndWaitOnConsole
import at.reisishot.mise.commons.toNormalizedString
import at.reisishot.mise.commons.withChild
import java.nio.file.Path

open class ImageMagickThumbnailGenerator(forceRegeneration: ForceRegeneration = ForceRegeneration()) : AbstractThumbnailGenerator(forceRegeneration) {
    override val generatorName: String = "Image Magick Thumbnail"

    override fun convertImage(inFile: Path, outFile: Path, size: ImageSize) {

        val normalizedOut = outFile.normalize()
        arrayOf(
                "magick", inFile.toNormalizedString(),
                "-quality", "${(size.quality * 100).toInt()}",
                "-resize", "${size.longestSidePx}x${size.longestSidePx}>",
                "-strip",
                "-sampling-factor", size.interpolation.value,
                "-interlace", "Plane",
                normalizedOut.toString()
        ).runAndWaitOnConsole()

        arrayOf(
                "magick", inFile.toNormalizedString(),
                "-quality", "${(size.quality * 100).toInt()}",
                "-resize", "${size.longestSidePx}x${size.longestSidePx}>",
                "-strip",
                "-sampling-factor", size.interpolation.value,
                "-interlace", "Plane",
                (normalizedOut.parent withChild normalizedOut.filenameWithoutExtension + ".webp").toString()
        ).runAndWaitOnConsole()
    }


}
