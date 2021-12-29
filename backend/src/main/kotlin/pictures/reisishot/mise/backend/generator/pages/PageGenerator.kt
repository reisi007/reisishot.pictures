package pictures.reisishot.mise.backend.generator.pages


import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.isHtml
import at.reisishot.mise.commons.isMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.findGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.minimalistic.Yaml
import pictures.reisishot.mise.backend.generator.testimonials.TestimonialLoader
import pictures.reisishot.mise.backend.generator.testimonials.findTestimonialLoader
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class PageGenerator(vararg val extensions: PageGeneratorExtension) : WebsiteGenerator {

    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page"

    companion object {
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<PageMinimalInfo>

    internal lateinit var galleryGenerator: AbstractGalleryGenerator
    internal lateinit var testimonialLoader: TestimonialLoader

    private val extensionFileExtensions =
        extensions.asSequence()
            .flatMap { it.interestingFileExtensions() }
            .toList()
            .toTypedArray()

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        withContext(Dispatchers.IO) {
            extensions.forEach { it.init(configuration, cache) }
            galleryGenerator = alreadyRunGenerators.findGalleryGenerator()
            testimonialLoader = alreadyRunGenerators.findTestimonialLoader()

            cache.clearMenuItems { it.id.startsWith(generatorName + "_") }
            cache.resetLinkcacheFor(LINKTYPE_PAGE)
            withContext(Dispatchers.IO) {
                filesToProcess = Files.walk(configuration.inPath)
                    .asSequence()
                    .filter { p -> p.hasExtension(FileExtension::isMarkdown, FileExtension::isHtml) }
                    // Generate all links
                    .map {
                        it.computeMinimalInfo(generatorName, configuration, cache)
                    }.toList()
            }
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        buildArtifacts(configuration, cache)
    }

    fun PageMinimalInfo.buildArtifact(configuration: WebsiteConfiguration, cache: BuildingCache) {
        convertMarkdown(
            this,
            configuration,
            cache,
        )
    }


    private fun buildPage(
        body: String,
        headManipulator: HEAD.() -> Unit,
        websiteConfiguration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        galleryGenerator: AbstractGalleryGenerator,
        pageMinimalInfo: IPageMininmalInfo,
        metadata: Yaml
    ) {
        PageGenerator.generatePage(
            pageMinimalInfo.targetPath,
            pageMinimalInfo.title,
            websiteConfiguration = websiteConfiguration,
            buildingCache = buildingCache,
            additionalHeadContent = headManipulator,
            galleryGenerator = galleryGenerator,
            pageContent = { raw(body) }
        )

        extensions.forEach { it.postCreatePage(websiteConfiguration, buildingCache, pageMinimalInfo, metadata, body) }
    }


    private fun convertMarkdown(
        info: PageMinimalInfo,
        configuration: WebsiteConfiguration,
        buildingCache: BuildingCache
    ) {
        val (yaml, headManipulator, htmlInput) = MarkdownParser.parse(
            configuration,
            buildingCache,
            testimonialLoader,
            info,
            galleryGenerator,
            *extensions
        )
        return buildPage(
            htmlInput,
            headManipulator,
            configuration,
            buildingCache,
            galleryGenerator,
            info,
            yaml
        )
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        val relevantFiles = changeFiles.relevantFiles()
        withContext(Dispatchers.IO) {
            filesToProcess = computeFilesToProcess(relevantFiles, configuration, cache)
            cleanupOutDir(relevantFiles, configuration, cache)
        }

        return relevantFiles.any { changeState -> !changeState.isStateEdited() }
    }

    private fun computeFilesToProcess(
        relevantFiles: Set<Pair<Path, Set<ChangeState>>>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ): List<PageMinimalInfo> {
        return relevantFiles.asSequence()
            .filter { changeStates -> !changeStates.isStateDeleted() }
            .map { (file, _) -> configuration.inPath.resolve(file) }
            .map { it.computeMinimalInfo(generatorName, configuration, cache) }
            .toList()
    }

    private fun cleanupOutDir(
        relevantFiles: Set<Pair<Path, Set<ChangeState>>>,
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        relevantFiles.asSequence()
            .filter { changedStates -> changedStates.isStateDeleted() }
            .forEach { (sourcePath, _) ->
                cache.resetLinkcacheFor(LINKTYPE_PAGE)
                sourcePath.computeMinimalInfo(generatorName, configuration, cache)
                    .let { (_, targetPath, _) ->
                        targetPath.parent.toFile().deleteRecursively()
                    }
            }
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        buildArtifacts(configuration, cache)
        return false
    }

    private fun buildArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache
    ) {
        filesToProcess
            .filter { (path) -> path.hasExtension(FileExtension::isMarkdown, FileExtension::isHtml) }
            .forEach { it.buildArtifact(configuration, cache) }
        extensions.forEach { it.processChanges(configuration, cache) }
    }

    private fun ChangeFileset.relevantFiles(): Set<Pair<Path, Set<ChangeState>>> {
        val data = mutableMapOf<Path, MutableSet<ChangeState>>()

        entries.asSequence()
            .filter { (file, _) ->
                file.hasExtension(FileExtension::isHtml, FileExtension::isMarkdown, *extensionFileExtensions)
            }
            .forEach { (k, v) ->
                data.computeIfAbsent(k) { mutableSetOf() }.addAll(v)
            }

        return data.entries.mapTo(mutableSetOf()) { (k, v) -> k to v }
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache): Unit =
        withContext(Dispatchers.IO) {
            cache.getLinkcacheEntriesFor(LINKTYPE_PAGE).values.asSequence()
                .map { configuration.outPath.resolve("index.html") }
                .forEach {
                    Files.deleteIfExists(it)
                    extensions.forEach { consumer ->
                        consumer.processDelete(
                            configuration, cache,
                            it.parent
                        )
                    }
                }
        }
}
