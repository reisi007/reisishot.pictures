package pictures.reisishot.mise.backend.generator.testimonials

import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.SourcePath
import pictures.reisishot.mise.backend.TargetPath
import pictures.reisishot.mise.backend.config.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.html.config.VelocityTemplateObjectCreator
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import pictures.reisishot.mise.backend.htmlparsing.getString
import pictures.reisishot.mise.commons.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.isDirectory
import pictures.reisishot.mise.backend.generator.testimonials.context.createTestimonialApi as createBaseTestimonialApi

interface TestimonialLoader {

    fun load(websiteConfig: WebsiteConfig, buildingCache: BuildingCache): Map<String, Testimonial>
}

class TestimonialLoaderImpl(private vararg val paths: Path) : TestimonialLoader, WebsiteGenerator {
    override val executionPriority: Int = 1000
    override val generatorName: String = "Testimonial Loader"
    private var lastChange: ZonedDateTime = LocalDateTime.MIN.atZone(ZoneId.systemDefault())

    private var possiblyDirty = true

    private var cachedValue: Map<String, Testimonial> = mapOf()

    companion object {
        const val INPUT_FOLDER_NAME = "reviews"
        fun fromSourceFolder(inPath: Path): TestimonialLoaderImpl =
            fromPath(inPath withChild INPUT_FOLDER_NAME)

        fun fromPath(inPath: Path): TestimonialLoaderImpl = TestimonialLoaderImpl(inPath)
    }

    override fun load(websiteConfig: WebsiteConfig, buildingCache: BuildingCache): Map<String, Testimonial> {
        if (!possiblyDirty)
            return cachedValue
        val newestChange = loadNewestModifiedFile()
        return if (newestChange <= lastChange) {
            possiblyDirty = false
            cachedValue
        } else {
            possiblyDirty = false
            cachedValue = loadAllTestimonials(websiteConfig, buildingCache)
            cachedValue
        }
    }

    private fun loadAllTestimonials(
        websiteConfig: WebsiteConfig,
        buildingCache: BuildingCache
    ): Map<String, Testimonial> {
        return loadAllFIles()
            .map { path ->
                val (yaml, _, html) = Files.newBufferedReader(path, StandardCharsets.UTF_8).use {
                    MarkdownParser.processMarkdown2Html(
                        websiteConfig,
                        buildingCache,
                        object : IPageMinimalInfo {
                            override val sourcePath: SourcePath = path
                            override val targetPath: TargetPath =
                                websiteConfig.paths.targetFolder withChild websiteConfig.paths.sourceFolder.relativize(
                                    path
                                )
                            override val title: String = path.fileName.toString()

                            override fun toString(): String {
                                return "Testimonial $sourcePath"
                            }
                        }
                    )
                }
                path.fileName.toString().substringBefore('.') to yaml.createTestimonial(path, html)
            }
            .toMap()
    }

    private fun loadAllFIles() = paths.asSequence()
        .flatMap { listFiles(it) }
        .flatMap { listFiles(it) }
        .filter { Files.isRegularFile(it) }
        .filter { path -> path.hasExtension({ it.isMarkdownPart("review") }) }

    private fun listFiles(it: Path) = if (it.isDirectory()) it.list() else sequenceOf(it)

    private fun Yaml.createTestimonial(p: Path, contentHtml: String): Testimonial {
        val imageFilename = getString("image")
        val imageFilenames = get("images")
        val ytCode = getString("video")
        val personName = getString("name")
        val date = getString("date")
        val type = getString("type")
        val rating = getString("rating")?.toInt()

        if (personName == null || date == null || type == null)
            throw IllegalStateException("Das Testimonial in $p ist nicht vollständig!")
        return Testimonial(
            p.filenameWithoutExtension,
            imageFilename,
            imageFilenames,
            ytCode,
            rating,
            personName,
            date,
            type,
            contentHtml
        )
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        possiblyDirty = true
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        possiblyDirty = true
        @Suppress("KotlinConstantConditions")
        return possiblyDirty
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // Nothing to do
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // Nothing to do
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // nothing to do
    }

    private fun loadNewestModifiedFile() = loadAllFIles()
        .map { it.fileModifiedDateTime }
        .filterNotNull()
        .maxOrNull() ?: ZonedDateTime.now()
}

@WebsiteConfigBuilderDsl
fun TestimonialLoader.createTestimonialApi(galleryGenerator: AbstractGalleryGenerator): Pair<String, VelocityTemplateObjectCreator> =
    createBaseTestimonialApi(this, galleryGenerator)
