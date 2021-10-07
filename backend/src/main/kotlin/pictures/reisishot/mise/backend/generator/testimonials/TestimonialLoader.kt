package pictures.reisishot.mise.backend.generator.testimonials

import at.reisishot.mise.commons.fileModifiedDateTime
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.isMarkdownPart
import at.reisishot.mise.commons.withChild
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.pages.minimalistic.Yaml
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser.markdown2Html
import pictures.reisishot.mise.backend.htmlparsing.getString
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.asSequence

interface TestimonialLoader {

    fun load(): Map<String, Testimonial>
}

class TestimonialLoaderImpl(private vararg val paths: Path) : TestimonialLoader, WebsiteGenerator {
    override val executionPriority: Int = 1000
    override val generatorName: String = "Testimonial Loader"
    var lastChange: ZonedDateTime = LocalDateTime.MIN.atZone(ZoneId.systemDefault())

    var possiblyDirty = true

    private var cachedValue: Map<String, Testimonial> = mapOf()

    companion object {
        const val INPUT_FOLDER_NAME = "reviews"
        fun fromInPath(inPath: Path): TestimonialLoaderImpl = TestimonialLoaderImpl(inPath withChild INPUT_FOLDER_NAME)

    }

    override fun load(): Map<String, Testimonial> {
        if (!possiblyDirty)
            return cachedValue
        val newestChange = loadNewestModifiedFile()
        return if (newestChange <= lastChange) {
            possiblyDirty = false
            cachedValue
        } else {
            possiblyDirty = false
            cachedValue = loadAllTestimonials()
            cachedValue
        }
    }

    private fun loadAllTestimonials(): Map<String, Testimonial> {
        return loadAllFIles()
            .map { path ->
                val yamlContainer = AbstractYamlFrontMatterVisitor()
                val html = Files.newBufferedReader(path, StandardCharsets.UTF_8).use {
                    it.markdown2Html(yamlContainer)
                }
                path.fileName.toString().substringBefore('.') to yamlContainer.data.createTestimonial(path, html)
            }
            .toMap()
    }

    private fun loadAllFIles() = paths.asSequence()
        .flatMap { Files.list(it).asSequence() }
        .filter { Files.isRegularFile(it) }
        .filter { it.hasExtension({ it.isMarkdownPart("review") }) }

    private fun Yaml.createTestimonial(p: Path, contentHtml: String): Testimonial {
        val imageFilename = getString("image")
        val ytCode = getString("video")
        val personName = getString("name")
        val date = getString("date")
        val type = getString("type")
        val rating = getString("rating")?.toInt()

        if (personName == null || date == null || type == null || (imageFilename == null && ytCode == null))
            throw IllegalStateException("Das Testimonial in $p ist nicht vollständig!")
        return Testimonial(imageFilename, rating, ytCode, personName, date, type, contentHtml)
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        possiblyDirty = true
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        possiblyDirty = true
        return possiblyDirty
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // nothing to do
    }


    private fun loadNewestModifiedFile() = loadAllFIles()
        .map { it.fileModifiedDateTime }
        .filterNotNull()
        .maxOrNull() ?: ZonedDateTime.now()
}


fun List<WebsiteGenerator>.findTestimonialLoader() = find { it is TestimonialLoader }
        as? TestimonialLoader
    ?: throw IllegalStateException("Gallery generator is needed for this generator!")