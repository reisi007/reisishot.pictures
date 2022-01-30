package pictures.reisishot.mise.backend.main

import at.reisishot.mise.backend.config.*
import at.reisishot.mise.backend.gallery.generator.GalleryGenerator
import at.reisishot.mise.commons.filenameWithoutExtension
import at.reisishot.mise.config.ImageConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pictures.reisishot.mise.backend.Mise.generate
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
object Images {
    const val folderName = "images.reisishot.pictures"
    val cacheFolder: Path = Paths.get("tmp", folderName).toAbsolutePath()

    @JvmStatic
    fun main(args: Array<String>) {
        build(!args.contains("prod"))
    }

    internal fun build(isDevMode: Boolean) {

        val generators = listOf(
            ImageMagickThumbnailGenerator(),
            GalleryGenerator(
                pageGenerationSettings = emptySet(),
                imageConfigNotFoundAction = { path -> ImageConfig(path.filenameWithoutExtension, mutableSetOf()) }
            )
        )


        val websiteConfig = buildWebsiteConfig(
            PathInformation(
                Paths.get("input", folderName).toAbsolutePath(),
                cacheFolder,
                Paths.get("upload", folderName).toAbsolutePath()
            ),
            GeneralWebsiteInformation("Reisishot", "Reisishot Images", "https://$folderName", Locale.GERMANY),
            MiseConfig(isDevMode)
        ) {
            this.generators += generators

            configureJsonParser {
                polymorphic(ImageInformation::class) {
                    subclass(InternalImageInformation::class)
                }
            }
        }

        runBlocking { websiteConfig.generate() }
    }
}
