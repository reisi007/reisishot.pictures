package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.*
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import org.apache.commons.text.StringEscapeUtils
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.*
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.generator.gallery.insertSubcategoryThumbnails
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class PageGenerator : WebsiteGenerator {

    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page"

    companion object {
        const val FILENAME_SEPERATOR = "--"
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<PageGeneratorInfo>

    private val parseMarkdown: (SourcePath) -> Reader by lazy {
        val extensions = listOf(
                AutolinkExtension.create(),
                TablesExtension.create(),
                TocExtension.create(),
                EmojiExtension.create(),
                AnchorLinkExtension.create()
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
        return@lazy { sourceFile: SourcePath ->
            Files.newBufferedReader(sourceFile).use { reader ->
                StringReader(
                        StringEscapeUtils.unescapeHtml4(
                                htmlRenderer.render(
                                        parser.parseReader(reader)
                                )
                        )
                )
            }
        }
    }

    private lateinit var speedupHtml: (Reader, FilenameWithoutExtension, WebsiteConfiguration, BuildingCache, TargetPath, String) -> Unit
    private lateinit var galleryGenerator: GalleryGenerator
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
            galleryGenerator = alreadyRunGenerators.find { it is GalleryGenerator } as? GalleryGenerator
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
            return@run { templateData, originalFilename, websiteConfiguration, buildingCache, targetPath, title ->
                val velocityContext = VelocityContext()
                val galleryObject = VelocityGalleryObject(targetPath, buildingCache, websiteConfiguration)
                // Make objects available in Velocity templates
                velocityContext.put("please", galleryObject)

                StringWriter().let {
                    try {
                        velocity.evaluate(velocityContext, it, "Velocity", templateData)
                    } catch (e: Exception) {
                        throw IllegalStateException("Could not parse $originalFilename!", e)
                    }
                    it.toString()
                }.let { html ->
                    PageGenerator.generatePage(
                            targetPath,
                            title,
                            websiteConfiguration = websiteConfiguration,
                            buildingCache = buildingCache,
                            hasGallery = galleryObject.hasGallery,
                            pageContent = {
                                raw(html.replace(compressHtml, " "))
                            }
                    )
                }
            }
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) =
            filesToProcess.forEach { it.buildArtifact(configuration, cache) }


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
    ) =
            Files.newBufferedReader(soureFile).use { reader ->
                convertHtml(
                        reader,
                        soureFile.filenameWithoutExtension,
                        websiteConfiguration,
                        buildingCache,
                        targetFile,
                        title
                )
            }

    private fun convertHtml(
            soureData: Reader,
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
            parseMarkdown(soureFile),
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
                }
    }

    inner class VelocityGalleryObject(
            private val targetPath: TargetPath,
            private val cache: BuildingCache,
            private val websiteConfiguration: WebsiteConfiguration
    ) {
        private var privateHasGallery = false
        val hasGallery
            get() = privateHasGallery


        private fun Map<FilenameWithoutExtension, InternalImageInformation>.getOrThrow(key: FilenameWithoutExtension) =
                this[key]
                        ?: throw IllegalStateException("Cannot find picture with filename \"$key\" (used in ${targetPath.filenameWithoutExtension})!")

        @SuppressWarnings("unused")
        fun insertPicture(filenameWithoutExtension: FilenameWithoutExtension) = buildString {
            appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
                with(galleryGenerator.cache) {
                    insertLazyPicture(imageInformationData.getOrThrow(filenameWithoutExtension))
                }
            }
        }

        @SuppressWarnings("unused")
        fun insertGallery(
                galleryName: String,
                vararg filenameWithoutExtension: FilenameWithoutExtension
        ): String {
            privateHasGallery = privateHasGallery || filenameWithoutExtension.isNotEmpty()
            return with(galleryGenerator.cache) {
                filenameWithoutExtension.asSequence()
                        .map {
                            imageInformationData.getOrThrow(it)
                        }.toArray(filenameWithoutExtension.size).let { imageInformations ->
                            buildString {
                                appendHTML(prettyPrint = false, xhtmlCompatible = true).div {
                                    insertImageGallery(galleryName, *imageInformations)
                                }
                            }
                        }
            }
        }

        @SuppressWarnings("unused")
        fun insertLink(type: String, key: String): String = websiteConfiguration.websiteLocation + cache.getLinkcacheEntryFor(type, key)

        @SuppressWarnings("unused")
        fun insertLink(linktext: String, type: String, key: String): String = buildString {
            appendHTML(false, true).a(insertLink(type, key)) {
                text(linktext)
            }
        }

        @SuppressWarnings("unused")
        fun insertSubalbumThumbnails(albumName: String?): String = buildString {
            appendHTML(false, true).div {
                insertSubcategoryThumbnails(CategoryName(albumName ?: ""), galleryGenerator)
            }
        }
    }

}
typealias SourcePath = Path;
typealias TargetPath = Path;
typealias PageGeneratorInfo = Triple<SourcePath, TargetPath, String/*Title*/>
