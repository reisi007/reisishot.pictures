package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.isHtml
import at.reisishot.mise.commons.isJetbrainsTemp
import at.reisishot.mise.commons.isMarkdown
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.AbstractThumbnailGenerator.ForceRegeneration
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
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
                        interactiveIgnoredFiles = *arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp),
                        generators = listOf(
                                PageGenerator(),
                                GalleryGenerator(
                                        categoryBuilders = *arrayOf(
                                                DateCategoryBuilder("Chronologisch"),
                                                ConfigurableCategoryBuilder()
                                        ), exifReplaceFunction = defaultExifReplaceFunction
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