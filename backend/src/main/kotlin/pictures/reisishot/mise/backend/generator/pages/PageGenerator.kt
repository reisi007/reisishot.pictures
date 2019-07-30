package pictures.reisishot.mise.backend.generator.pages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import org.apache.commons.text.StringEscapeUtils
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import pictures.reisishot.mise.backend.*
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.FilenameWithoutExtension
import pictures.reisishot.mise.backend.generator.gallery.GalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.InternalImageInformation
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import kotlin.streams.asSequence

class PageGenerator : WebsiteGenerator {
    override val executionPriority: Int = 30_000
    override val generatorName: String = "Reisishot Page generator"

    companion object {
        const val MENU_NAME_SEPARATOR = "-"
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<Pair<SourcePath, TargetPath>>

    private val parseMarkdown: (SourcePath) -> Reader by lazy {
        val parser = Parser.builder()
            .build()
        val htmlRenderer = HtmlRenderer.builder()
            .softbreak(" ")
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

    private lateinit var speedupHtml: (Reader, FilenameWithoutExtension, WebsiteConfiguration, BuildingCache, TargetPath) -> Unit
    private lateinit var galleryGenerator: GalleryGenerator


    override suspend fun fetchInformation(
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
                .filter { p -> p.isRegularFile() && (p.isMarkdown || p.isHtml) }
                // Calculate out file location
                .map {
                    val base = configuration.inPath.relativize(it).toString()
                        .substringBeforeLast(".")
                        .substringAfterLast(MENU_NAME_SEPARATOR)
                    if (base.startsWith("index", true))
                        it to configuration.outPath.resolve("index.html")
                    else
                        it to configuration.outPath.resolve("$base/index.html")

                }.map {
                    it to Files.getFileAttributeView(
                        it.first,
                        BasicFileAttributeView::class.java
                    ).readAttributes().creationTime()
                }.map { it.first }
                // Generate all links
                .peek { (inPath, outPath) ->
                    configuration.outPath.relativize(outPath).parent?.fileName?.toString().let { filename ->
                        if (filename == null) {
                            cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "index", "")
                            return@peek
                        }
                        val link = '/' + configuration.outPath.relativize(outPath).parent.toString()
                        var inFilename = inPath.fileName.toString().filenameWithoutExtension

                        val globalPriority = inFilename.substringBefore(MENU_NAME_SEPARATOR).toIntOrNull() ?: 0
                        inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR)

                        val menuContainerName =
                            inFilename.substringBefore(MENU_NAME_SEPARATOR).replace('_', ' ')
                        inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR)
                        val menuItemPriority = inFilename.substringBefore(MENU_NAME_SEPARATOR)
                            .toIntOrNull()
                            ?.also { inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR) }
                            ?: 0
                        val menuItemName = inFilename.substringAfter(MENU_NAME_SEPARATOR).replace('_', ' ')


                        if (menuContainerName.isBlank()) {
                            cache.addLinkcacheEntryFor(LINKTYPE_PAGE, menuItemName, link)
                            cache.addMenuItem(
                                generatorName + "_" + menuContainerName,
                                globalPriority,
                                link,
                                menuItemName
                            )
                        } else {
                            cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "$menuContainerName-$menuItemName", link)
                            cache.addMenuItemInContainer(
                                generatorName + "_" + menuContainerName,
                                menuContainerName,
                                globalPriority,
                                menuItemName,
                                link,
                                menuItemPriority
                            )
                        }
                    }
                }.toList()
        }

        speedupHtml = run {
            Velocity.init()
            val velocity = VelocityEngine()

            return@run { templateData, originalFilename, websiteConfiguration, buildingCache, targetPath ->
                val velocityContext = VelocityContext()
                val galleryObject = VelocityGalleryObject(targetPath, buildingCache, websiteConfiguration)
                // Make objects available in Velocity templates
                velocityContext.put("please", galleryObject)

                StringWriter().let {
                    velocity.evaluate(velocityContext, it, "Velocity", templateData)
                    it.toString()
                }.let { html ->
                    PageGenerator.generatePage(
                        targetPath,
                        originalFilename.substringAfter(MENU_NAME_SEPARATOR)
                            .substringAfter(MENU_NAME_SEPARATOR).let { result ->
                                if ("index" == result)
                                    websiteConfiguration.longTitle
                                else result
                            }.replace('_', ' '),
                        websiteConfiguration = websiteConfiguration,
                        buildingCache = buildingCache,
                        hasGallery = galleryObject.hasGallery,
                        pageContent = {
                            raw(html)
                        }
                    )
                }
            }
        }
    }

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { (soureFile, targetFile) ->
            if (soureFile.isMarkdown) convertMarkdown(
                soureFile,
                configuration,
                cache,
                targetFile
            ) else convertHtml(soureFile, configuration, cache, targetFile)
        }
    }

    private fun convertHtml(
        soureFile: SourcePath,
        websiteConfiguration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        targetFile: TargetPath
    ) =
        Files.newBufferedReader(soureFile).use { reader ->
            convertHtml(
                reader,
                soureFile.filenameWithoutExtension,
                websiteConfiguration,
                buildingCache,
                targetFile
            )
        }

    private fun convertHtml(
        soureData: Reader,
        sourceFileName: FilenameWithoutExtension,
        websiteConfiguration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        targetFile: TargetPath
    ) = speedupHtml(soureData, sourceFileName, websiteConfiguration, buildingCache, targetFile)

    private fun convertMarkdown(
        soureFile: SourcePath,
        websiteConfiguration: WebsiteConfiguration,
        buildingCache: BuildingCache,
        targetFile: TargetPath
    ) =
        convertHtml(
            parseMarkdown(soureFile),
            soureFile.filenameWithoutExtension,
            websiteConfiguration,
            buildingCache,
            targetFile
        )

    inner class VelocityGalleryObject(
        private val targetPath: TargetPath,
        private val cache: BuildingCache,
        private val websiteConfiguration: WebsiteConfiguration
    ) {
        private var privateHasGallery = false
        private val websiteLocation by lazy {
            with(websiteConfiguration.websiteLocation) {
                if (!endsWith('/'))
                    "$this/"
                else
                    this
            }
        }
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
        fun insertLink(type: String, key: String): String = websiteLocation + cache.getLinkcacheEntryFor(type, key)
    }

}
typealias SourcePath = Path;
typealias TargetPath = Path;

val Path.isHtml
    get() = toString().let { it.endsWith("html", true) || it.endsWith("htm", true) }