package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.*
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import org.apache.commons.text.StringEscapeUtils
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity


import org.apache.velocity.app.VelocityEngine
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.raw
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
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

    private val parseMarkdown by lazy {
        val extensions = listOf(
                AutolinkExtension.create(),
                TablesExtension.create(),
                TocExtension.create(),
                EmojiExtension.create(),
                AnchorLinkExtension.create(),
                YamlFrontMatterExtension.create()

        )

        val parser = Parser.builder()
                .extensions(extensions)
                .apply {
                    set(Parser.SPACE_IN_LINK_ELEMENTS, true)
                    set(Parser.SPACE_IN_LINK_URLS, true)
                }
                .build()
        val htmlRenderer = HtmlRenderer
                .builder()
                .extensions(extensions)
                .build()


        return@lazy { configuration: WebsiteConfiguration, cache: BuildingCache, sourceFile: SourcePath, targetPath: TargetPath ->
            val yamlExtractor = AbstractYamlFrontMatterVisitor()
            Files.newBufferedReader(sourceFile).use { reader ->
                val parseReader = parser.parseReader(reader)
                yamlExtractor.visit(parseReader)
                val headManipulator: HEAD.() -> Unit = {
                    metaDataConsumers.asSequence()
                            .map { it.processFrontMatter(configuration, cache, targetPath, yamlExtractor.data, this@PageGenerator) }
                            .forEach { it(this) }
                }

                StringReader(
                        StringEscapeUtils.unescapeHtml4(
                                htmlRenderer.render(
                                        parseReader
                                )
                        )
                ) to headManipulator
            }
        }
    }

    private lateinit var speedupHtml: (Pair<Reader, HEAD.() -> Unit>, FilenameWithoutExtension, WebsiteConfiguration, BuildingCache, TargetPath, String) -> Unit
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

        speedupHtml = run {
            Velocity.init()
            val velocity = VelocityEngine()

            val compressHtml = """[\s\n\r]{2,}""".toRegex()
            return@run { (templateData, headManipulator), originalFilename, websiteConfiguration, buildingCache, targetPath, title ->
                val velocityContext = VelocityContext()
                val galleryObject = TemplateApi(targetPath, galleryGenerator, buildingCache, websiteConfiguration)
                // Make objects available in Velocity templates
                velocityContext.put("please", galleryObject)

                StringWriter().let {
                    try {
                        velocity.evaluate(velocityContext, it, "Velocity", templateData)
                    } catch (e: Exception) {
                        throw IllegalStateException("Could not parse \"$originalFilename!\"", e)
                    }
                    it.toString()
                }.let { html ->
                    PageGenerator.generatePage(
                            targetPath,
                            title,
                            websiteConfiguration = websiteConfiguration,
                            buildingCache = buildingCache,
                            additionalHeadContent = headManipulator,
                            pageContent = {
                                raw(html.replace(compressHtml, " "))
                            }
                    )
                }
            }
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { it.buildArtifact(configuration, cache) }
        metaDataConsumers.forEach { it.processChanges(configuration, cache, this) }
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
    ) = Files.newBufferedReader(soureFile).use { reader: Reader ->
        val noop: HEAD.() -> Unit = { }
        convertHtml(
                reader to noop,
                soureFile.filenameWithoutExtension,
                websiteConfiguration,
                buildingCache,
                targetFile,
                title
        )
    }

    private fun convertHtml(
            soureData: Pair<Reader, HEAD.() -> Unit>,
            sourceFileName: FilenameWithoutExtension,
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            targetFile: TargetPath,
            title: String
    ) = speedupHtml(soureData, sourceFileName, websiteConfiguration, buildingCache, targetFile, title)

    private fun convertMarkdown(
            soureFile: SourcePath,
            websiteConfiguration: WebsiteConfiguration,
            buildingCache: BuildingCache,
            targetFile: TargetPath,
            title: String
    ) = convertHtml(
            parseMarkdown(websiteConfiguration, buildingCache, soureFile, targetFile),
            soureFile.filenameWithoutExtension,
            websiteConfiguration,
            buildingCache,
            targetFile,
            title
    )

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        val relevantFiles = changeFiles.relevantFiles()
        withContext(Dispatchers.IO) {
            filesToProcess = computeFilesToProcess(relevantFiles, configuration, cache)
            cleanupOutDir(relevantFiles, configuration, cache)
        }

        return relevantFiles.any { changeState -> !changeState.isStateEdited() }
    }

    private fun computeFilesToProcess(relevantFiles: Set<Map.Entry<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache): List<PageGeneratorInfo> {
        return relevantFiles.asSequence()
                .filter { changeStates -> !changeStates.isStateDeleted() }
                .map { (file, _) -> configuration.inPath.resolve(file) }
                .map { it.computePageGeneratorInfo(configuration, cache) }
                .toList()
    }

    private fun cleanupOutDir(relevantFiles: Set<Map.Entry<Path, Set<ChangeState>>>, configuration: WebsiteConfiguration, cache: BuildingCache) {
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

    private fun ChangeFileset.relevantFiles() = asSequence()
            .filter { (file, _) -> file.hasExtension(FileExtension::isHtml, FileExtension::isMarkdown) }
            .toSet()

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
typealias SourcePath = Path;
typealias TargetPath = Path;
typealias PageGeneratorInfo = Triple<SourcePath, TargetPath, String/*Title*/>
typealias Yaml = Map<String, List<String>>
