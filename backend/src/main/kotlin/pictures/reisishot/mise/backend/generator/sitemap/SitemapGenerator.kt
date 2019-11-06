package pictures.reisishot.mise.backend.generator.sitemap

import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.hasExtension
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.isStateEdited
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class SitemapGenerator(private vararg val noChangedFileExtensions: (FileExtension) -> Boolean) : WebsiteGenerator {

    override val generatorName: String = "Sitemap Generator"

    override val executionPriority: Int = 100_000

    override suspend fun fetchInitialInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        // Nothing to do
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        PrintWriter(configuration.outPath.resolve("sitemap.xml").toFile(), Charsets.UTF_8.toString()).use {
            it.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            it.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
            configuration.outPath.findIndexHtmlFiles()
                    .map { it.normalize().toString().replace('\\', '/') }
                    .map { configuration.websiteLocation + it }
                    .map { if (it.endsWith('/')) it else "$it/" }
                    .forEach { pageUrl ->
                        it.print("<url>")
                        it.print("<loc>$pageUrl</loc>")
                        it.print("</url>")
                    }
            it.print("</urlset>")
        }
    }


    private fun Path.findIndexHtmlFiles(): Sequence<Path> = Files.walk(this).asSequence()
            .filter { Files.isRegularFile(it) }
            .filter { it.fileName.toString().equals("index.html", true) }
            .map { it.parent }
            .filterNotNull()
            .map { relativize(it) }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changeFiles: ChangeFileset): Boolean {
        val fixNoChange = changeFiles.all { changeState ->
            changeState.component1().hasExtension(*noChangedFileExtensions) && changeState.isStateEdited()
        }
        if (!fixNoChange)
            buildInitialArtifacts(configuration, cache)
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }
}