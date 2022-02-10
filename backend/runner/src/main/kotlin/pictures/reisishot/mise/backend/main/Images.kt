package pictures.reisishot.mise.backend.main

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pictures.reisishot.mise.backend.Mise.generate
import pictures.reisishot.mise.backend.config.GeneralWebsiteInformation
import pictures.reisishot.mise.backend.config.MiseConfig
import pictures.reisishot.mise.backend.config.PathInformation
import pictures.reisishot.mise.backend.config.buildWebsiteConfig
import pictures.reisishot.mise.backend.config.configureJsonParser
import pictures.reisishot.mise.backend.gallery.generator.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.ImageInformation
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.config.ImageConfig
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale

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
            MiseConfig(isDevMode, cleanupGeneration = true)
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
