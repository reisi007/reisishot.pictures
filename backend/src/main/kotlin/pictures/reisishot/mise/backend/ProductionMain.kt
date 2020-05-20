package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.categories.ConfigurableCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.categories.DateCategoryBuilder
import pictures.reisishot.mise.backend.generator.gallery.thumbnails.ImageMagickThumbnailGenerator
import pictures.reisishot.mise.backend.generator.links.LinkGenerator
import pictures.reisishot.mise.backend.generator.pages.OverviewPageGenerator
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import java.nio.file.Paths

object ProductionMain {

    @JvmStatic
    fun main(args: Array<String>) {
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot",
                        longTitle = "Reisishot - Fotograf Florian Reisinger",
                        websiteLocation = "https://reisishot.pictures",
                        inPath = Paths.get("input-main").toAbsolutePath(),
                        tmpPath = Paths.get("tmp-main").toAbsolutePath(),
                        outPath = Paths.get("frontend-main/generated").toAbsolutePath(),
                        interactiveIgnoredFiles = *arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        analyticsSiteId = "1",
                        socialMediaLinks = SocialMediaAccounts("reisishot", "reisishot", "florian@reisishot.pictures"),
                        generators = listOf(
                                PageGenerator(OverviewPageGenerator()),
                                GalleryGenerator(
                                        categoryBuilders = *arrayOf(
                                                DateCategoryBuilder("Chronologisch"),
                                                ConfigurableCategoryBuilder()
                                        ),
                                        displayedMenuItems = emptySet(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageMagickThumbnailGenerator(),
                                LinkGenerator(),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}