package pictures.reisishot.mise.backend.generator.sitemap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pictures.reisishot.mise.backend.config.*
import pictures.reisishot.mise.backend.config.BuildingCache.Companion.getLinkFromFragment
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
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        // Nothing to do
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, cache: BuildingCache) =
        withContext(Dispatchers.IO) {
            PrintWriter(
                configuration.paths.targetFolder.resolve("sitemap.json").toFile(),
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
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        val fixNoChange = changeFiles.entries.asSequence().map { (k, v) -> k to v }.all { changeState ->
            changeState.first.hasExtension(*noChangedFileExtensions) && changeState.isStateEdited()
        }
        if (!fixNoChange)
            buildInitialArtifacts(configuration, cache)
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, cache: BuildingCache) {
        // Nothing to do
    }
}
