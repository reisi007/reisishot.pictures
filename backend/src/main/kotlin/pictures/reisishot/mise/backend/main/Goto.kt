package pictures.reisishot.mise.backend.main

import at.reisishot.mise.commons.*
import at.reisishot.mise.exifdata.defaultExifReplaceFunction
import pictures.reisishot.mise.backend.Mise
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.multisite.ImageInfoImporter
import pictures.reisishot.mise.backend.generator.pages.PageGenerator
import pictures.reisishot.mise.backend.generator.pages.yamlConsumer.KeywordConsumer
import pictures.reisishot.mise.backend.generator.sitemap.SitemapGenerator
import java.nio.file.Paths

object Goto {
    @JvmStatic
    fun main(args: Array<String>) {
        build(true)
    }

    fun build(isDevMode: Boolean) {
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot Links",
                        longTitle = "Reisishot Links - Übersicht über meine Projekte",
                        isDevMode = isDevMode,
                        websiteLocation = "https://goto.reisishot.pictures",
                        inPath = Paths.get("input-goto").toAbsolutePath(),
                        tmpPath = Paths.get("tmp-goto").toAbsolutePath(),
                        outPath = Paths.get("frontend-goto/generated").toAbsolutePath(),
                        interactiveIgnoredFiles = arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        analyticsSiteId = "6",
                        generators = listOf(
                                GalleryGenerator(
                                        categoryBuilders = emptyArray(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageInfoImporter(Paths.get("tmp-main").toAbsolutePath()),
                                PageGenerator(
                                        metaDataConsumers = arrayOf(
                                                KeywordConsumer()
                                        )
                                ),
                                SitemapGenerator(FileExtension::isHtml, FileExtension::isMarkdown)
                        )
                )
        )
    }
}