package pictures.reisishot.mise.backend.generator.sitemap

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class SitemapGenerator() : WebsiteGenerator {
    override val generatorName: String = "Sitemap Generator"


    override val executionPriority: Int = 100_000

    override suspend fun fetchInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        // Nothing to do
    }

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        val websiteLocation = with(configuration.websiteLocation) {
            if (!endsWith('/'))
                "$this/"
            else
                this
        }
        PrintWriter(configuration.outPath.resolve("sitemap.xml").toFile(), Charsets.UTF_8.toString()).use {
            it.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            it.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
            configuration.outPath.findIndexHtmlFiles()
                    .map { it.normalize().toString().replace('\\', '/') }
                    .map { websiteLocation + it }
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
}