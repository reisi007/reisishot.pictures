package pictures.reisishot.mise.backend.generator.pages.overview

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.HEAD
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h5
import kotlinx.html.p
import pictures.reisishot.mise.backend.IPageMinimalInfo
import pictures.reisishot.mise.backend.SourcePath
import pictures.reisishot.mise.backend.TargetPath
import pictures.reisishot.mise.backend.config.BuildingCache
import pictures.reisishot.mise.backend.config.ChangeFileset
import pictures.reisishot.mise.backend.config.WebsiteConfig
import pictures.reisishot.mise.backend.config.WebsiteGenerator
import pictures.reisishot.mise.backend.config.useJsonParser
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.gallery.context.insertLazyPicture
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.ReisishotIcons
import pictures.reisishot.mise.backend.html.insertIcon
import pictures.reisishot.mise.backend.html.metadata
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.PageGeneratorExtension
import pictures.reisishot.mise.backend.htmlparsing.PageMetadata
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import pictures.reisishot.mise.backend.htmlparsing.getPageMetadata
import pictures.reisishot.mise.backend.htmlparsing.getString
import pictures.reisishot.mise.commons.FileExtension
import pictures.reisishot.mise.commons.filenameWithoutExtension
import pictures.reisishot.mise.commons.hasExtension
import pictures.reisishot.mise.commons.isHtmlPart
import pictures.reisishot.mise.commons.isMarkdownPart
import pictures.reisishot.mise.commons.isRegularFile
import pictures.reisishot.mise.commons.useBufferedReader
import pictures.reisishot.mise.commons.withChild
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class OverviewPageGenerator(
    private val galleryGenerator: AbstractGalleryGenerator,
    private val metaDataConsumers: Array<out PageGeneratorExtension> = emptyArray(),
) : PageGeneratorExtension, WebsiteGenerator {

    override val generatorName: String = "Overview Page Generator"
    override val executionPriority: Int = 35_000
    private val data = mutableMapOf<String, MutableSet<OverviewEntry>>()
    private var dirty = false
    private val changeSetAdd = mutableSetOf<OverviewEntry>()
    private val changeSetRemove = mutableListOf<Path>()
    private lateinit var overviewConfigs: Map<String, OverviewConfig>

    override fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> {
        return sequenceOf(
            { it.isHtmlPart("overview") },
            { it.isMarkdownPart("overview") }
        )
    }

    override fun processFrontmatter(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMinimalInfo: IPageMinimalInfo,
        frontMatter: Yaml
    ): HEAD.() -> Unit {
        frontMatter.processFrontmatter(pageMinimalInfo)
        return {}
    }

    private fun Yaml.processFrontmatter(pageMinimalInfo: IPageMinimalInfo) {
        extract(pageMinimalInfo, overviewConfigs)?.let {
            dirty = dirty or changeSetAdd.add(it)
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        changeFiles.asSequence()
            .map { it.key }
            .filter { it.filenameWithoutExtension.endsWith("overview", true) }
            .map { configuration.paths.sourceFolder.relativize(it) }
            .map { it.toString().replace('\\', '/') }
            .map { it.ifBlank { "index" } }
            .map { data.keys.firstOrNull { key -> key.equals(it, true) } }
            .filterNotNull()
            .map { data.getValue(it) }
            .map { it.first() }
            .forEach { dirty = dirty or changeSetAdd.add(it) }
        return dirty
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean = processChangesInternal(configuration, buildingCache)

    override fun processChanges(configuration: WebsiteConfig, cache: BuildingCache) {
        processChangesInternal(configuration, cache)
    }

    override fun init(configuration: WebsiteConfig, cache: BuildingCache) = configuration.useJsonParser {
        overviewConfigs = (configuration.paths.sourceFolder withChild "overview.json")
            .fromJson<Map<String, OverviewConfig>>()
            ?: emptyMap()
    }

    private fun processChangesInternal(
        configuration: WebsiteConfig,
        cache: BuildingCache
    ): Boolean {

        processExternals(configuration)
        val changedGroups = mutableMapOf<String, Path>()
        changeSetAdd.forEach {
            data.computeIfAbsent(it.id) { mutableSetOf() }.add(it, true)
            changedGroups[it.id] = it.entryOutUrl.parent
        }
        changeSetRemove.forEach {
            val entry = data.values
                .asSequence()
                .map { es -> es.find { e -> e.entryOutUrl == it } }
                .filterNotNull()
                .firstOrNull()
                ?: return@forEach
            data[entry.id]?.remove(entry)
            if (data[entry.id].isNullOrEmpty())
                data.remove(entry.id)
            changedGroups[entry.id] = entry.entryOutUrl.parent
        }
        dirty = false

        changedGroups.computeChangedGroups()
            .map { (name, b) -> data.getValue(name).first() to b }
            .forEach { (data, b) ->
                val name = data.id // TODO group config files using name only

                val overviewPagePath = b.fileName.let {
                    if (name == "index")
                        configuration.paths.sourceFolder
                    else configuration.paths.sourceFolder withChild name
                }

                val target =
                    configuration.paths.targetFolder withChild
                        configuration.paths.sourceFolder.relativize(overviewPagePath) withChild
                        "index.html"

                val additionalTopContent =
                    loadBefore(
                        configuration,
                        cache,
                        data.pageMininmalInfo,
                        metaDataConsumers
                    )
                val endContent =
                    loadEnd(
                        configuration,
                        cache,
                        data.pageMininmalInfo,
                        metaDataConsumers
                    )
                val displayName = data.displayName

                createPage(target, displayName, configuration, additionalTopContent, cache, data, name, endContent)
            }
        if (dirty) {
            return processChangesInternal(configuration, cache)
        }
        val changes = changeSetAdd.isNotEmpty() || changeSetRemove.isNotEmpty()
        changeSetAdd.clear()
        changeSetRemove.clear()
        dirty = false
        return changes
    }

    private fun createPage(
        target: Path,
        displayName: String,
        configuration: WebsiteConfig,
        additionalTopContent: Triple<Yaml, HEAD.() -> Unit, String>?,
        cache: BuildingCache,
        data: OverviewEntry,
        name: String,
        endContent: String?
    ) {
        PageGenerator.generatePage(
            target,
            displayName,
            configuration.websiteInformation.locale,
            configuration,
            cache,
            additionalTopContent?.second ?: {},
        ) {
            p {
                h1(classes = "center") { text(displayName) }
            }
            additionalTopContent?.third?.let { raw(it) }
            div(classes = "row center overview-" + data.config?.computeStyle()) {
                this@OverviewPageGenerator.data[name]?.asSequence()
                    ?.sortedByDescending { it.order }
                    ?.forEach { entry ->
                        val image = galleryGenerator.cache.imageInformationData.getValue(entry.picture)
                        val url = entry.configuredUrl
                            ?: kotlin.run {
                                BuildingCache.getLinkFromFragment(
                                    configuration,
                                    configuration.paths.targetFolder.relativize(entry.entryOutUrl withChild "index.html").parent?.toString()
                                        ?: ""
                                )
                            }
                        div(classes = "col-lg-4 mt-3") {
                            a(url, classes = "card black h-100") {
                                if (entry.configuredUrl != null)
                                    this.target = "_blank"
                                div(classes = "card-img-top") {
                                    insertLazyPicture(image, configuration)
                                }
                                div(classes = "card-body") {
                                    h5("card-title") { text(entry.title) }
                                    p("card-text") {
                                        entry.metaData?.let {
                                            metadata(it)
                                            br
                                        }
                                        entry.description?.let {
                                            text(it)
                                        }
                                    }
                                }

                                footer("card-footer") {
                                    div(classes = "btn btn-primary") {
                                        text("Mehr erfahren")
                                        entry.configuredUrl?.let {
                                            insertIcon(ReisishotIcons.LINK, "xs", "sup")
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            endContent?.let { raw(it) }
        }
    }

    private fun MutableMap<String, Path>.computeChangedGroups() = (
        if (isEmpty())
            data.asSequence()
                .filter { (_, set) -> set.isNotEmpty() }
                .map { (k, v) -> k to v.first().entryOutUrl.parent }
        else asSequence()
            .map { it.toPair() }
        )

    private fun processExternals(configuration: WebsiteConfig) =
        configuration.paths.sourceFolder.processFrontmatter(configuration) { it: Path ->
            it.hasExtension({
                it.isMarkdownPart(
                    "external"
                )
            })
        }

    private fun parseFrontmatter(configuration: WebsiteConfig, inPath: Path): Pair<Path, Yaml> {
        val outPath =
            configuration.paths.targetFolder withChild configuration.paths.sourceFolder.relativize(inPath) withChild "external"
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        val fileContent = inPath.useBufferedReader { it.readText() }
        MarkdownParser.extractFrontmatter(fileContent, yamlExtractor)
        return outPath to yamlExtractor.data
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfig,
        buildingCache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) = withContext(Dispatchers.IO) {
        configuration.paths.sourceFolder.processFrontmatter(configuration) { it: Path ->
            it.hasExtension({
                it.isMarkdownPart(
                    "overview"
                )
            })
        }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // No action needed
    }

    override suspend fun cleanup(configuration: WebsiteConfig, buildingCache: BuildingCache) {
        // Nothing to do
    }

    private fun Path.processFrontmatter(configuration: WebsiteConfig, filter: (Path) -> Boolean) =
        Files.walk(this)
            .filter { it.isRegularFile() }
            .filter(filter)
            .map { inPath ->
                parseFrontmatter(configuration, inPath)
            }.forEach { (path, yaml) ->
                yaml.processFrontmatter(OverviewPageMinimalInformation(path))
            }

    class OverviewPageMinimalInformation(override val targetPath: TargetPath) : IPageMinimalInfo {
        override val sourcePath: SourcePath
            get() = throw IllegalStateException("Not implemented")
        override val title: String
            get() = throw IllegalStateException("Not implemented")
    }

    private fun <E> MutableSet<E>.add(element: E, force: Boolean) {
        val wasAdded = add(element)
        if (!wasAdded && force) {
            remove(element)
            add(element)
        }
    }

    private fun loadBefore(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMininmalInfo: IPageMinimalInfo,
        metaDataConsumers: Array<out PageGeneratorExtension>
    ): Triple<Yaml, HEAD.() -> Unit, String>? {
        val sequence = sequenceOf(
            "top.overview.md",
            "top.overview.html"
        )
        return sequence.parseFile(pageMininmalInfo, configuration, cache, metaDataConsumers)
    }

    private fun Sequence<String>.parseFile(
        pageMininmalInfo: IPageMinimalInfo,
        configuration: WebsiteConfig,
        cache: BuildingCache,
        metaDataConsumers: Array<out PageGeneratorExtension>
    ): Triple<Yaml, HEAD.() -> Unit, String>? {
        val sourcePath = pageMininmalInfo.sourcePath

        return map { sourcePath withChild it }
            .firstOrNull { it.exists() }
            ?.let {
                MarkdownParser.processMarkdown2Html(
                    configuration,
                    cache,
                    pageMininmalInfo,
                    *metaDataConsumers
                )
            }
    }

    private fun loadEnd(
        configuration: WebsiteConfig,
        cache: BuildingCache,
        pageMininmalInfo: IPageMinimalInfo,
        metaDataConsumers: Array<out PageGeneratorExtension>
    ): String? {
        val sequence = sequenceOf(
            "end.overview.md",
            "end.overview.html"
        )
        return sequence.parseFile(pageMininmalInfo, configuration, cache, metaDataConsumers)
            ?.third
    }
}

internal fun Yaml.extract(
    pageMinimalInfo: IPageMinimalInfo,
    overviewConfigs: Map<String, OverviewConfig>
): OverviewEntry? {
    val group = getString("group")
    val picture = getString("picture")
    val title = getString("title")
    val metaData = getPageMetadata()
    val order = metaData?.order
    val description = getString("description")
    val groupConfig = overviewConfigs[group]
    val displayName = groupConfig?.name ?: group

    val url = getString("url")
    if (group == null || picture == null || title == null || order == null || displayName == null)
        return null

    return OverviewEntry(
        group,
        groupConfig,
        title,
        description,
        picture,
        pageMinimalInfo,
        order,
        displayName,
        url,
        metaData
    )
}

class OverviewEntry(
    val id: String,
    val config: OverviewConfig?,
    val title: String,
    val description: String?,
    val picture: String,
    val pageMininmalInfo: IPageMinimalInfo,
    val order: String?,
    val displayName: String,
    val configuredUrl: String?,
    val metaData: PageMetadata?
) {

    val entryOutUrl: Path = pageMininmalInfo.targetPath.parent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OverviewEntry

        if (id != other.id) return false
        if (pageMininmalInfo != other.pageMininmalInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + pageMininmalInfo.hashCode()
        return result
    }
}
