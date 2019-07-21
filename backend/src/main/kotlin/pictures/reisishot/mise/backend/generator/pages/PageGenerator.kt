package pictures.reisishot.mise.backend.generator.pages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeSingleton
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.fileModifiedDateTime
import pictures.reisishot.mise.backend.filenameWithoutExtension
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.isRegularFile
import java.io.Reader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.streams.asSequence

class PageGenerator : WebsiteGenerator {
    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page generator"

    private lateinit var filesToProcess: List<Pair<SourcePath, TargetPath>>

    private val parseMarkdown: (SourcePath) -> Reader by lazy {
        val parser = Parser.builder()
            .build()
        val htmlRenderer = HtmlRenderer.builder()
            .build()
        return@lazy { sourceFile: SourcePath ->
            Files.newBufferedReader(sourceFile).use { reader ->
                StringReader(
                    htmlRenderer.render(
                        parser.parseReader(reader)
                    )
                )
            }
        }
    }

    private lateinit var speedupHtml: (Reader, TargetPath) -> Unit
    private lateinit var galleryGenerator: GalleryGenerator

    override suspend fun fetchInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        withContext(Dispatchers.IO) {
            galleryGenerator = alreadyRunGenerators.find { it is GalleryGenerator } as? GalleryGenerator
                ?: throw IllegalStateException("Gallery generator is needed for this generator!")

            filesToProcess = Files.walk(configuration.inPath)
                .asSequence()
                .filter { p -> p.isRegularFile() && (p.isMarkdown || p.isHtml) }
                // Calculate out file location
                .map { it to configuration.outPath.resolve("${configuration.inPath.relativize(it).filenameWithoutExtension}/index.html") }
                .filter { (inPath, outPath) ->
                    outPath.fileModifiedDateTime?.let { outTime -> inPath.fileModifiedDateTime?.let { inTime -> outTime > inTime } }
                        ?: true
                }.toList()
        }

        speedupHtml = run {
            Velocity.init()
            val velocityContext = VelocityContext()
            val runtimeServices = RuntimeSingleton.getRuntimeServices()

            val galleryObject = object : Any() {

                fun generateGallery(vararg filenameWithoutExtension: FilenameWithoutExtension): Unit =
                    TODO("Needs implementation")
            }
            // Make objects available in Velocity templates
            velocityContext.put("gallery", galleryObject)

            return@run { templateData, targetPath ->
                val template = Template().apply {
                    setRuntimeServices(runtimeServices)
                    data = runtimeServices.parse(templateData, this)
                }
                Files.newBufferedWriter(
                    targetPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { writer ->
                    template.merge(velocityContext, writer)
                }
            }
        }
    }

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { (soureFile, targetFile) ->
            if (soureFile.isMarkdown) convertMarkdown(
                soureFile,
                targetFile
            ) else convertHtml(soureFile, targetFile)
        }
    }

    private fun convertHtml(soureFile: SourcePath, targetFile: TargetPath) =
        Files.newBufferedReader(soureFile).use { reader -> convertHtml(reader, targetFile) }


    private fun convertHtml(soureData: Reader, targetFile: TargetPath) = speedupHtml(soureData, targetFile)

    private fun convertMarkdown(soureFile: SourcePath, targetFile: TargetPath) =
        convertHtml(parseMarkdown(soureFile), targetFile)
}
typealias SourcePath = Path;
typealias TargetPath = Path;

val Path.isHtml
    get() = toString().let { it.endsWith("html", true) || it.endsWith("htm", true) }
