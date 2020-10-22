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
        val folderName = "goto.reisishot.pictures"
        Mise.build(
                WebsiteConfiguration(
                        shortTitle = "Reisishot - Herzlich Willkommen",
                        longTitle = "Reisishot Goto - Übersicht über meine Projekte",
                        isDevMode = isDevMode,
                        websiteLocation = "https://$folderName",
                        inPath = Paths.get("input", folderName).toAbsolutePath(),
                        tmpPath = Paths.get("tmp", folderName).toAbsolutePath(),
                        outPath = Paths.get("upload", folderName).toAbsolutePath(),
                        interactiveIgnoredFiles = arrayOf<(FileExtension) -> Boolean>(FileExtension::isJetbrainsTemp, FileExtension::isTemp),
                        cleanupGeneration = false,
                        analyticsSiteId = "6",
                        generators = listOf(
                                GalleryGenerator(
                                        categoryBuilders = emptyArray(),
                                        exifReplaceFunction = defaultExifReplaceFunction
                                ),
                                ImageInfoImporter(Main.tmpPath),
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