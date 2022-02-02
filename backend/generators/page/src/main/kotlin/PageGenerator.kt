package pictures.reisishot.mise.backend.generator.pages


import at.reisishot.mise.backend.config.*
import at.reisishot.mise.commons.FileExtension
import at.reisishot.mise.commons.hasExtension
import at.reisishot.mise.commons.isHtml
import at.reisishot.mise.commons.isMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.PageGeneratorExtension
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class PageGenerator(
    private vararg val extensions: PageGeneratorExtension
) : WebsiteGenerator {

    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page"

    companion object {
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<PageMinimalInfo>


    private val extensionFileExtensions =
        extensions.asSequence()
            .flatMap { it.interestingFileExtensions() }
            .toList()
            .toTypedArray()

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        withContext(Dispatchers.IO) {
            extensions.forEach { it.init(configuration, buildingCache) }

            buildingCache.clearMenuItems { it.id.startsWith(generatorName + "_") }
            buildingCache.resetLinkcacheFor(LINKTYPE_PAGE)
            withContext(Dispatchers.IO) {
                filesToProcess = Files.walk(configuration.paths.sourceFolder)
                    .asSequence()
                    .filter { p -> p.hasExtension(FileExtension::isMarkdown, FileExtension::isHtml) }
                    // Generate all links
                    .map {
                        it.computeMinimalInfo(generatorName, configuration, buildingCache)
                    }.toList()
            }
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        buildArtifacts(configuration, buildingCache)
    }

    private fun PageMinimalInfo.buildArtifact(configuration: WebsiteConfig, cache: BuildingCache) {
        convertMarkdown(
            this,
            configuration,
            cache,
        )
    }


    private fun buildPage(
        body: String,
        headManipulator: HEAD.() -> Unit,
        websiteConfig: WebsiteConfig,
        buildingCache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        metadata: Yaml
    ) {
        PageGenerator.generatePage(
            pageMinimalInfo.targetPath,
            pageMinimalInfo.title,
            websiteConfig = websiteConfig,
            buildingCache = buildingCache,
            additionalHeadContent = headManipulator,
            pageContent = { raw(body) }
        )

        extensions.forEach { it.postCreatePage(websiteConfig, buildingCache, pageMinimalInfo, metadata, body) }
    }


    private fun convertMarkdown(
        info: PageMinimalInfo,
        configuration: WebsiteConfig,
        buildingCache: BuildingCache
    ) {
        val (yaml, headManipulator, htmlInput) = MarkdownParser.processMarkdown2Html(
            configuration,
            buildingCache,
            info,
            *extensions
        )
        return buildPage(
            htmlInput,
            headManipulator,
            configuration,
            buildingCache,
            info,
            yaml
        )
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        val relevantFiles = changeFiles.relevantFiles()
        withContext(Dispatchers.IO) {
            filesToProcess = computeFilesToProcess(relevantFiles, configuration, buildingCache)
            cleanupOutDir(relevantFiles, configuration, buildingCache)
        }

        return relevantFiles.any { changeState -> !changeState.isStateEdited() }
    }

    private fun computeFilesToProcess(
        relevantFiles: Set<Pair<Path, Set<ChangeState>>>,
        configuration: WebsiteConfig,
        cache: BuildingCache
    ): List<PageMinimalInfo> {
        return relevantFiles.asSequence()
            .filter { changeStates -> !changeStates.isStateDeleted() }
            .map { (file, _) -> configuration.paths.sourceFolder.resolve(file) }
            .map { it.computeMinimalInfo(generatorName, configuration, cache) }
            .toList()
    }

    private fun cleanupOutDir(
        relevantFiles: Set<Pair<Path, Set<ChangeState>>>,
        configuration: WebsiteConfig,
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
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        buildArtifacts(configuration, buildingCache)
        return false
    }

    private fun buildArtifacts(
        configuration: WebsiteConfig,
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

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache): Unit =
        withContext(Dispatchers.IO) {
            buildingCache.getLinkcacheEntriesFor(LINKTYPE_PAGE).values.asSequence()
                .map { configuration.paths.targetFolder.resolve("index.html") }
                .forEach {
                    Files.deleteIfExists(it)
                    extensions.forEach { consumer ->
                        consumer.processDelete(
                            configuration, buildingCache,
                            it.parent
                        )
                    }
                }
        }
}
