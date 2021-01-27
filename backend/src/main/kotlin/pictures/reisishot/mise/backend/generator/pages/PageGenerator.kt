package pictures.reisishot.mise.backend.generator.pages


import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.isHtml
import at.reisishot.mise.commons.isMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import kotlinx.html.div
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.minimalistic.TargetPath
import pictures.reisishot.mise.backend.html.*
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class PageGenerator(
        vararg val metaDataConsumers: PageGeneratorExtension,
) : WebsiteGenerator {

    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page"

    companion object {
        const val FILENAME_SEPERATOR = "--"
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<PageMininmalInfo>

    internal lateinit var galleryGenerator: AbstractGalleryGenerator

    val metaDataConsumerFileExtensions by lazy {
        metaDataConsumers.asSequence()
                .flatMap { it.interestingFileExtensions() }
                .toList()
                .toTypedArray()
    }

    override suspend fun fetchInitialInformation(
            configuration: WebsiteConfiguration,
            cache: BuildingCache,
            alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        withContext(Dispatchers.IO) {
            metaDataConsumers.forEach { it.init(configuration, cache) }
            galleryGenerator = alreadyRunGenerators.find { it is AbstractGalleryGenerator } as? AbstractGalleryGenerator
                    ?: throw IllegalStateException("Gallery generator is needed for this generator!")

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
        filesToProcess
                .filter { (path) -> path.hasExtension(FileExtension::isMarkdown, FileExtension::isHtml) }
                .forEach { it.buildArtifact(configuration, cache) }
        metaDataConsumers.forEach { it.processChanges(configuration, cache) }
    }


    fun PageMininmalInfo.buildArtifact(configuration: WebsiteConfiguration, cache: BuildingCache) {
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
            targetPath: TargetPath,
            title: String
    ) {

        PageGenerator.generatePage(
                targetPath,
                title,
                websiteConfiguration = websiteConfiguration,
                buildingCache = buildingCache,
                additionalHeadContent = headManipulator,
                galleryGenerator = galleryGenerator,
                pageContent = {
                    div {
                        attributes.itemscope = ""
                        attributes.itemtype = Itemtypes.ARTICLE
                        raw(body)
                    }
                }
        )


    }


    private fun convertMarkdown(
            info: PageMininmalInfo,
            configuration: WebsiteConfiguration,
            buildingCache: BuildingCache,
    ) {
        val (_, targetFile, title) = info
        val (headManipulator, htmlInput) = MarkdownParser.parse(configuration, buildingCache, info, galleryGenerator, *metaDataConsumers)
        return buildPage(
                htmlInput,
                headManipulator,
                configuration,
                buildingCache,
                galleryGenerator,
                targetFile,
                title
        )
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        val relevantFiles = changeFiles.relevantFiles()
        withContext(Dispatchers.IO) {
            filesToProcess = computeFilesToProcess(relevantFiles, configuration, cache)
            cleanupOutDir(relevantFiles, configuration, cache)
        }

        return relevantFiles.any { changeState -> !changeState.isStateEdited() }
    }

    private fun computeFilesToProcess(relevantFiles: Set<Pair<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache): List<PageMininmalInfo> {
        return relevantFiles.asSequence()
                .filter { changeStates -> !changeStates.isStateDeleted() }
                .map { (file, _) -> configuration.inPath.resolve(file) }
                .map { it.computeMinimalInfo(generatorName, configuration, cache) }
                .toList()
    }

    private fun cleanupOutDir(relevantFiles: Set<Pair<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache) {
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

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changeFiles: ChangeFileset): Boolean {
        buildInitialArtifacts(configuration, cache)
        return false
    }

    private fun ChangeFileset.relevantFiles(): Set<Pair<Path, Set<ChangeState>>> {
        val data = mutableMapOf<Path, MutableSet<ChangeState>>()

        entries.asSequence()
                .filter { (file, _) ->
                    file.hasExtension(FileExtension::isHtml, FileExtension::isMarkdown, *metaDataConsumerFileExtensions)
                }
                .forEach { (k, v) ->
                    data.computeIfAbsent(k) { mutableSetOf() }.addAll(v)
                }

        return data.entries.mapTo(mutableSetOf()) { (k, v) -> k to v }
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache): Unit = withContext(Dispatchers.IO) {
        cache.getLinkcacheEntriesFor(LINKTYPE_PAGE).values.asSequence()
                .map { configuration.outPath.resolve("index.html") }
                .forEach {
                    Files.deleteIfExists(it)
                    metaDataConsumers.forEach { consumer ->
                        consumer.processDelete(configuration, cache,
                                it.parent)
                    }
                }
    }
}
