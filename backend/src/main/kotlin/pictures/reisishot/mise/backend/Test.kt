package pictures.reisishot.mise.backend

import pictures.reisishot.mise.backend.generator.gallery.ExifdataKey
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ForceRegeneration
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.isHtml
import pictures.reisishot.mise.backend.generator.isMarkdown
import pictures.reisishot.mise.backend.generator.links.LinkGenerator
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import java.nio.file.Paths

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot",
                        longTitle = "Reisishot - Fotograf Florian Reisinger",
                        websiteLocation = "http://localhost:3000",
                        inPath = Paths.get("src/main/resources").toAbsolutePath(),
                        outPath = Paths.get("../frontend/generated").toAbsolutePath(),
                        generators = listOf(
                                PageGenerator(),
                                GalleryGenerator(
                                        categoryBuilders = *arrayOf(
                                                DateCategoryBuilder("Chronologisch"),
                                                ConfigurableCategoryBuilder()
                                        ), exifReplaceFunction = { cur ->
                                    when (cur.first) {
                                        ExifdataKey.LENS_MODEL -> when (cur.second) {
                                            "105.0 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM"
                                            "147.0 mm" -> ExifdataKey.LENS_MODEL to "Sigma 105mm EX DG OS HSM + 1.4 Sigma EX APO DG Telekonverter"
                                            else -> cur
                                        }
                                        else -> cur
                                    }
                                }
                                ),
                                ImageMagickThumbnailGenerator(
                                        ForceRegeneration(
                                                thumbnails = true
                                        )
                                ),
                                LinkGenerator(),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}