package pictures.reisishot.mise.backend.generator.pages


import at.reisishot.mise.commons.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class PageGenerator(private vararg val metaDataConsumers: YamlMetaDataConsumer) : WebsiteGenerator {

    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page"

    companion object {
        const val FILENAME_SEPERATOR = "--"
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<PageGeneratorInfo>

    internal lateinit var galleryGenerator: AbstractGalleryGenerator
    private val displayReplacePattern = Regex("[\\-_]")

    data class FilenameParts(val menuContainerName: String, val destinationPath: Path, val globalPriority: Int,
                             val menuItemName: String, val menuItemDisplayName: String, val menuItemPriority: Int,
                             val folderName: String, val folderDisplayName: String)

    private fun Path.computePageGeneratorInfo(configuration: WebsiteConfiguration, cache: BuildingCache): PageGeneratorInfo {
        configuration.inPath.relativize(this).let { filename ->
            if (filename.toString().startsWith("index.", true)) {
                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "index", "")
                return Triple(
                        this,
                        configuration.outPath.resolve("index.html"),
                        configuration.longTitle
                )
            }

            val filenameParts = calculateFilenameParts(configuration)

            val link = configuration.outPath.relativize(filenameParts.destinationPath.parent).toString()
            if (filenameParts.menuContainerName.isBlank()) {
                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, filenameParts.folderDisplayName, link)
                if (filenameParts.globalPriority > 0)
                    cache.addMenuItem(
                            generatorName + "_" + filenameParts.menuContainerName,
                            filenameParts.globalPriority,
                            link,
                            filenameParts.menuItemDisplayName
                    )
            } else {
                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "${filenameParts.menuContainerName}--${filenameParts.folderDisplayName}", link)
                if (filenameParts.globalPriority > 0)
                    cache.addMenuItemInContainerNoDupes(
                            generatorName + "_" + filenameParts.menuContainerName,
                            filenameParts.menuContainerName,
                            filenameParts.globalPriority,
                            filenameParts.menuItemDisplayName,
                            link,
                            elementIndex = filenameParts.menuItemPriority
                    )
            }

            return Triple(
                    this,
                    filenameParts.destinationPath,
                    filenameParts.menuItemDisplayName
            )
        }
    }

    private fun Path.calculateFilenameParts(configuration: WebsiteConfiguration): FilenameParts {
        var inFilename = filenameWithoutExtension

        val globalPriority = inFilename.substringBefore(FILENAME_SEPERATOR).toIntOrNull() ?: 0
        inFilename = inFilename.substringAfter(FILENAME_SEPERATOR)
                .replace('❔', '?')

        val menuContainerName =
                inFilename.substringBefore(FILENAME_SEPERATOR).replace(displayReplacePattern, " ")
        inFilename = inFilename.substringAfter(FILENAME_SEPERATOR)
        val menuItemPriority = inFilename.substringBefore(FILENAME_SEPERATOR)
                .toIntOrNull()
                ?.also { inFilename = inFilename.substringAfter(FILENAME_SEPERATOR) }
                ?: 0

        val rawMenuItemName = inFilename.substringBefore(FILENAME_SEPERATOR)
        val rawFolderName = inFilename.substringAfter(FILENAME_SEPERATOR)

        val menuItemName = rawMenuItemName.replace(displayReplacePattern, " ")
        val folderName = rawFolderName.replace(displayReplacePattern, " ")


        val outFile = configuration.inPath.relativize(this)
                .resolveSibling("${rawFolderName.toLowerCase()}/index.html")
                .let { configuration.outPath.resolve(it) }

        return FilenameParts(menuContainerName, outFile, globalPriority, rawMenuItemName, menuItemName, menuItemPriority, rawFolderName, folderName)
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
            filesToProcess = Files.walk(configuration.inPath)
                    .asSequence()
                    .filter { p -> p.hasExtension(FileExtension::isMarkdown, FileExtension::isHtml) }
                    // Generate all links
                    .map {
                        it.computePageGeneratorInfo(configuration, cache)
                    }.toList()
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { it.buildArtifact(configuration, cache) }
        metaDataConsumers.forEach { it.processChanges(configuration, cache, galleryGenerator) }
    }


    fun PageGeneratorInfo.buildArtifact(configuration: WebsiteConfiguration, cache: BuildingCache) = let { (soureFile, targetFile, title) ->
        if (soureFile.fileExtension.isMarkdown()) convertMarkdown(
                soureFile,
                configuration,
                cache,
                targetFile,
                title
        ) else convertHtml(
                soureFile,
                configuration,
                cache,
                targetFile,
                title
        )
    }

    private fun convertHtml(
            soureFile: SourcePath,
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            targetFile: TargetPath,
            title: String
    ) {
        val (processHeadFile, body) = HtmlParser.parse(websiteConfiguration, buildingCache, soureFile, targetFile, galleryGenerator, *metaDataConsumers)
        buildPage(
                body,
                processHeadFile,
                websiteConfiguration,
                buildingCache,
                targetFile,
                title
        )
    }

    private fun buildPage(
            body: String,
            headManipulator: HEAD.() -> Unit,
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            targetPath: TargetPath,
            title: String
    ) {

        PageGenerator.generatePage(
                targetPath,
                title,
                websiteConfiguration = websiteConfiguration,
                buildingCache = buildingCache,
                additionalHeadContent = headManipulator,
                pageContent = { raw(body) }
        )

    }


    private fun convertMarkdown(
            soureFile: SourcePath,
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            targetFile: TargetPath,
            title: String
    ) {
        val (headManipulator, htmlInput) = MarkdownParser.parse(websiteConfiguration, buildingCache, soureFile, targetFile, galleryGenerator, *metaDataConsumers)
        return buildPage(
                htmlInput,
                headManipulator,
                websiteConfiguration,
                buildingCache,
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

    private fun computeFilesToProcess(relevantFiles: Set<Pair<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache): List<PageGeneratorInfo> {
        return relevantFiles.asSequence()
                .filter { changeStates -> !changeStates.isStateDeleted() }
                .map { (file, _) -> configuration.inPath.resolve(file) }
                .map { it.computePageGeneratorInfo(configuration, cache) }
                .toList()
    }

    private fun cleanupOutDir(relevantFiles: Set<Pair<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache) {
        relevantFiles.asSequence()
                .filter { changedStates -> changedStates.isStateDeleted() }
                .forEach { (sourcePath, _) ->
                    cache.resetLinkcacheFor(LINKTYPE_PAGE)
                    sourcePath.computePageGeneratorInfo(configuration, cache).let { (_, targetPath, _) ->
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
                .filter { (file, _) -> file.hasExtension(FileExtension::isHtml, FileExtension::isMarkdown, FileExtension::isHead) }
                .map { (file, changedState) ->
                    if (file.hasExtension(FileExtension::isHead))
                        file.parent withChild file.filenameWithoutExtension + ".html" to changedState
                    else file to changedState

                }.forEach { (k, v) ->
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
                        consumer.processDelete(configuration, cache, it.parent)
                    }
                }
    }
}
