package pictures.reisishot.mise.backend.generator.pages

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
import pictures.reisishot.mise.backend.filenameWithoutExtension
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.*
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertImageGallery
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.isRegularFile
import pictures.reisishot.mise.backend.toArray
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
        const val MENU_NAME_SEPARATOR = "--"
        const val LINKTYPE_PAGE = "PAGE"
    }

    private lateinit var filesToProcess: List<Triple<SourcePath, TargetPath, String/*Title*/>>

    private val parseMarkdown: (SourcePath) -> Reader by lazy {
        val extensions = listOf(
                AutolinkExtension.create(),
                TablesExtension.create(),
                TocExtension.create(),
                EmojiExtension.create()
        )

        val parser = Parser.builder()
                .extensions(extensions)
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
                    // Generate all links
                    .map { inPath ->
                        configuration.inPath.relativize(inPath).let { filename ->
                            if (filename.toString().startsWith("index.", true)) {
                                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "index", "")
                                return@map Triple(
                                        inPath,
                                        configuration.outPath.resolve("index.html"),
                                        configuration.longTitle
                                )
                            }

                            var inFilename = inPath.fileName.toString().filenameWithoutExtension

                            val globalPriority = inFilename.substringBefore(MENU_NAME_SEPARATOR).toIntOrNull() ?: 0
                            inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR)

                            val menuContainerName =
                                    inFilename.substringBefore(MENU_NAME_SEPARATOR).replace(displayReplacePattern, " ")
                            inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR)
                            val menuItemPriority = inFilename.substringBefore(MENU_NAME_SEPARATOR)
                                    .toIntOrNull()
                                    ?.also { inFilename = inFilename.substringAfter(MENU_NAME_SEPARATOR) }
                                    ?: 0
                            val rawMenuItemName = inFilename.substringAfter(MENU_NAME_SEPARATOR)
                            val menuItemName =
                                    rawMenuItemName.replace(displayReplacePattern, " ")

                            val outPath =
                                    configuration.inPath.relativize(inPath)
                                            .resolveSibling("${rawMenuItemName.toLowerCase()}/index.html")
                            val link = outPath.parent.toString()

                            if (menuContainerName.isBlank()) {
                                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, menuItemName, link)
                                if (globalPriority > 0)
                                    cache.addMenuItem(
                                            generatorName + "_" + menuContainerName,
                                            globalPriority,
                                            link,
                                            menuItemName
                                    )
                            } else {
                                cache.addLinkcacheEntryFor(LINKTYPE_PAGE, "$menuContainerName-$menuItemName", link)
                                if (globalPriority > 0)
                                    cache.addMenuItemInContainerNoDupes(
                                            generatorName + "_" + menuContainerName,
                                            menuContainerName,
                                            globalPriority,
                                            menuItemName,
                                            link,
                                            elementIndex = menuItemPriority
                                    )
                            }

                            return@map Triple(
                                    inPath, configuration.outPath.resolve(
                                    outPath
                            ), menuItemName
                            )
                        }

                    }
                    .toList()
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
                    velocity.evaluate(velocityContext, it, "Velocity", templateData)
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

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        filesToProcess.forEach { (soureFile, targetFile, title) ->
            if (soureFile.isMarkdown) convertMarkdown(
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

val Path.isHtml
    get() = toString().let { it.endsWith("html", true) || it.endsWith("htm", true) }
