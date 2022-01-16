package pictures.reisishot.mise.backend.generator.gallery.thumbnails


import at.reisishot.mise.commons.replaceFileExtension
import at.reisishot.mise.commons.runAndWaitOnConsole
import at.reisishot.mise.commons.toNormalizedString
import pictures.reisishot.mise.backend.generator.thumbnail.AbstractThumbnailGenerator
import java.nio.file.Path

open class ImageMagickThumbnailGenerator(forceRegeneration: ForceRegeneration = ForceRegeneration()) :
    AbstractThumbnailGenerator(forceRegeneration) {
    override val generatorName: String = "Image Magick Thumbnail"

    override fun convertImage(inFile: Path, outFile: Path, prefferedSize: ImageSize) {
        val normalizedOut = outFile.normalize()

        generateWebP(inFile, prefferedSize, normalizedOut)
        generateJpeg(inFile, prefferedSize, normalizedOut)
    }

    private fun generateWebP(
        inFile: Path,
        preferredSize: ImageSize,
        normalizedOut: Path
    ) {
        arrayOf(
            "magick", inFile.toNormalizedString(),
            "-quality", "${(preferredSize.quality * 100).toInt()}",
            "-resize", "${preferredSize.longestSidePx}x${preferredSize.longestSidePx}>",
            "-strip",
            "-sampling-factor", preferredSize.interpolation.value,
            "-interlace", "Plane",
            normalizedOut.replaceFileExtension("webp").toString()
        ).runAndWaitOnConsole()
    }

    private fun generateJpeg(
        inFile: Path,
        preferredSize: ImageSize,
        normalizedOut: Path
    ) {
        arrayOf(
            "magick", inFile.toNormalizedString(),
            "-quality", "${(preferredSize.quality * 100).toInt()}",
            "-resize", "${preferredSize.longestSidePx}x${preferredSize.longestSidePx}>",
            "-strip",
            "-sampling-factor", preferredSize.interpolation.value,
            "-interlace", "Plane",
            normalizedOut.replaceFileExtension("jpg").toString()
        ).runAndWaitOnConsole()
    }
}
