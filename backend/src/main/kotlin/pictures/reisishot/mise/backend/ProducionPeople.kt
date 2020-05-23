package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import java.nio.file.Paths

object ProducionPeople {
    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot People",
                        longTitle = "Reisishot People - Fotograf Florian Reisinger, Linz Österreich",
                        websiteLocation = "https://people.reisishot.pictures",
                        inPath = Paths.get("input-people").toAbsolutePath(),
                        tmpPath = Paths.get("tmp-people").toAbsolutePath(),
                        outPath = Paths.get("frontend-people/generated").toAbsolutePath(),
                        interactiveIgnoredFiles = *arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        socialMediaLinks = SocialMediaAccounts("reisishot.people", "reisishot_people", "florian@reisishot.pictures"),
                        analyticsSiteId = "4",
                        generators = listOf(
                                PageGenerator(
                                        KeywordConsumer()
                                ),
                                GalleryGenerator(
                                        categoryBuilders = *emptyArray(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageMagickThumbnailGenerator(),
                                ImageInfoImporter(Paths.get("tmp-main").toAbsolutePath()),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}