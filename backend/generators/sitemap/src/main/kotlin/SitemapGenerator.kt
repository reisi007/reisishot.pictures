package pictures.reisishot.mise.backend.generator.sitemap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.BuildingCache.Companion.getLinkFromFragment
import pictures.reisishot.mise.backend.config.ChangeFileset
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteGenerator
import pictures.reisishot.mise.backend.config.isStateEdited
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.hasExtension
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class SitemapGenerator(private vararg val noChangedFileExtensions: (FileExtension) -> Boolean) : WebsiteGenerator {

    override val generatorName: String = "Sitemap Generator"

    override val executionPriority: Int = 100_000

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        // Nothing to do
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, buildingCache: BuildingCache) =
        withContext(Dispatchers.IO) {
            PrintWriter(
                configuration.paths.targetFolder.resolve("sitemap.xml").toFile(),
                Charsets.UTF_8.toString()
            ).use { writer ->
                writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                writer.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                configuration.paths.targetFolder.findIndexHtmlFiles()
                    .map { it.normalize().toString().replace('\\', '/') }
                    .map { getLinkFromFragment(configuration, it) }
                    .map { if (it.endsWith('/')) it.substringBeforeLast('/') else it }
                    .distinct()
                    .forEach { pageUrl ->
                        writer.print("<url>")
                        writer.print("<loc>$pageUrl</loc>")
                        writer.print("</url>")
                    }
                writer.print("</urlset>")
            }
        }

    private fun Path.findIndexHtmlFiles(): Sequence<Path> = Files.walk(this).asSequence()
        .filter { Files.isRegularFile(it) }
        .filter { it.fileName.toString().equals("index.html", true) }
        .map { it.parent }
        .filterNotNull()
        .map { relativize(it) }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        val fixNoChange = changeFiles.entries.asSequence().map { (k, v) -> k to v }.all { changeState ->
            changeState.first.hasExtension(*noChangedFileExtensions) && changeState.isStateEdited()
        }
        if (!fixNoChange)
            buildInitialArtifacts(configuration, buildingCache)
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // Nothing to do
    }
}
