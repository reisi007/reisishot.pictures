package pictures.reisishot.mise.backend.generator.pages.overview

import at.reisishot.mise.commons.*
import at.reisishot.mise.config.getConfig
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import io.github.config4k.extract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.html.*
import pictures.reisishot.mise.backend.htmlparsing.*
import java.nio.file.Files
import java.nio.file.Path

class OverviewPageGenerator(
        private val galleryGenerator: AbstractGalleryGenerator,
        private val metaDataConsumers: Array<out YamlMetaDataConsumer> = emptyArray()
) : YamlMetaDataConsumer, WebsiteGenerator {

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

    override fun processFrontmatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml): HEAD.() -> Unit {
        frontMatter.extract(targetPath)?.let {
            dirty = dirty or changeSetAdd.add(it)
        }
        return {}
    }

    override suspend fun fetchUpdateInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>, changeFiles: ChangeFileset): Boolean {
        changeFiles.asSequence()
                .map { it.key }
                .filter { it.filenameWithoutExtension.endsWith("overview", true) }
                .map { configuration.inPath.relativize(it) }
                .map { it.toString().replace('\\', '/') }
                .map { if (it.isBlank()) "index" else it }
                .map { data.keys.firstOrNull { key -> key.equals(it, true) } }
                .filterNotNull()
                .map { data.getValue(it) }
                .map { it.first() }
                .forEach { dirty = dirty or changeSetAdd.add(it) }
        return dirty
    }

    override suspend fun buildUpdateArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache, changeFiles: ChangeFileset): Boolean = processChangesInternal(configuration, cache)


    override fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache) {
        processChangesInternal(configuration, cache)
    }

    override fun init(configuration: WebsiteConfiguration, cache: BuildingCache) {
        overviewConfigs = (configuration.inPath withChild "overview.conf")
                .getConfig()
                ?.extract("entries")
                ?: emptyMap()
    }

    private fun processChangesInternal(configuration: WebsiteConfiguration, cache: BuildingCache): Boolean {
        processExternals(configuration, cache)
        val changedGroups = mutableMapOf<String, Path>()
        changeSetAdd.forEach {
            data.computeIfAbsent(it.id) { _ -> mutableSetOf() }.add(it, true)
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

        (
                if (changedGroups.isEmpty())
                    data.asSequence()
                            .filter { (_, set) -> set.isNotEmpty() }
                            .map { (k, v) -> k to v.first().entryOutUrl.parent }
                else changedGroups.asSequence()
                        .map { it.toPair() }
                )
                .map { (name, b) -> data.getValue(name).first() to b }
                .forEach { (data, b) ->
                    val name = data.id //TODO group config files using name only

                    val overviewPagePath = b.fileName.let {
                        if (name == "index")
                            configuration.inPath
                        else configuration.inPath withChild name
                    }

                    val target = configuration.outPath withChild configuration.inPath.relativize(overviewPagePath) withChild "index.html"

                    val additionalTopContent = loadBefore(configuration, cache, overviewPagePath, target, galleryGenerator, metaDataConsumers)
                    val endContent = loadEnd(configuration, cache, overviewPagePath, target, galleryGenerator, metaDataConsumers)
                    val displayName = data.displayName

                    PageGenerator.generatePage(
                            target,
                            displayName,
                            websiteConfiguration = configuration,
                            additionalHeadContent = additionalTopContent?.first ?: {},
                            galleryGenerator = galleryGenerator,
                            buildingCache = cache
                    ) {
                        p {
                            h1(classes = "center") { text(displayName) }
                        }
                        additionalTopContent?.second?.let { raw(it) }
                        div(classes = "row center overview-" + data.config?.computeStyle()) {
                            this@OverviewPageGenerator.data[name]?.asSequence()
                                    ?.sortedByDescending { it.order }
                                    ?.forEach { entry ->
                                        val image = galleryGenerator.cache.imageInformationData[entry.picture]
                                                ?: throw IllegalStateException("Cannot find Image Information")
                                        val url = entry.configuredUrl
                                                ?: kotlin.run {
                                                    BuildingCache.getLinkFromFragment(configuration, configuration.outPath.relativize(entry.entryOutUrl withChild "index.html").parent?.toString()
                                                            ?: "")
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
                                                    entry.description?.let {
                                                        p("card-text") {
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
        if (dirty) {
            return processChangesInternal(configuration, cache)
        }
        val changes = changeSetAdd.isNotEmpty() || changeSetRemove.isNotEmpty()
        changeSetAdd.clear()
        changeSetRemove.clear()
        dirty = false
        return changes
    }

    private fun processExternals(configuration: WebsiteConfiguration, cache: BuildingCache) =
            configuration.inPath.processFrontmatter(configuration, cache) { it: Path -> it.hasExtension({ it.isMarkdownPart("external") }) }

    private fun parseFrontmatter(configuration: WebsiteConfiguration, inPath: Path): Pair<Path, Yaml> {
        val outPath = configuration.outPath withChild configuration.inPath.relativize(inPath) withChild "external"
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        val fileContent = inPath.useBufferedReader { it.readText() }
        MarkdownParser.extractFrontmatter(fileContent, yamlExtractor)
        return outPath to yamlExtractor.data
    }

    override suspend fun fetchInitialInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>) = withContext(Dispatchers.IO) {
        configuration.inPath.processFrontmatter(configuration, cache) { it: Path -> it.hasExtension({ it.isMarkdownPart("overview") }) }
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
    }

    private fun Path.processFrontmatter(configuration: WebsiteConfiguration, cache: BuildingCache, filter: (Path) -> Boolean) =
            Files.walk(this)
                    .filter { it.isRegularFile() }
                    .filter(filter)
                    .map { inPath ->
                        parseFrontmatter(configuration, inPath)
                    }.forEach { (path, yaml) ->
                        processFrontmatter(configuration, cache, path, yaml)
                    }

    private fun Map<String, List<String>>.extract(targetPath: TargetPath): OverviewEntry? {
        val group = getString("group")
        val picture = getString("picture")
        val title = getString("title")
        val order = getString("order")?.toInt()
        val description = getString("description")
        val groupConfig = overviewConfigs[group]
        val displayName = groupConfig?.name ?: group

        val url = getString("url")
        if (group == null || picture == null || title == null || order == null || displayName == null)
            return null
        return OverviewEntry(group, title, description, picture, targetPath.parent, order, displayName, url)
    }

    inner class OverviewEntry(val id: String, val title: String, val description: String?, val picture: String, val entryOutUrl: Path, val order: Int, val displayName: String, val configuredUrl: String?) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OverviewEntry

            if (id != other.id) return false
            if (entryOutUrl != other.entryOutUrl) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + entryOutUrl.hashCode()
            return result
        }

        val config: OverviewConfig?
            get() = overviewConfigs[id]
    }
}

private fun <E> MutableSet<E>.add(element: E, force: Boolean) {
    val wasAdded = add(element)
    if (!wasAdded && force) {
        remove(element)
        add(element)
    }
}

private fun loadBefore(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        sourcePath: SourcePath,
        targetPath: TargetPath,
        galleryGenerator: AbstractGalleryGenerator,
        metaDataConsumers: Array<out YamlMetaDataConsumer>
) = sequenceOf(
        "top.overview.md",
        "top.overview.html"
)
        .map { sourcePath withChild it }
        .firstOrNull { it.exists() }
        ?.let {
            MarkdownParser.parse(
                    configuration,
                    cache,
                    it,
                    targetPath,
                    galleryGenerator,
                    *metaDataConsumers
            )
        }

private fun loadEnd(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        sourcePath: SourcePath,
        targetPath: TargetPath,
        galleryGenerator: AbstractGalleryGenerator,
        metaDataConsumers: Array<out YamlMetaDataConsumer>
) = sequenceOf(
        "end.overview.md",
        "end.overview.html"
)
        .map { sourcePath withChild it }
        .firstOrNull { it.exists() }
        ?.let {
            MarkdownParser.parse(
                    configuration,
                    cache,
                    it,
                    targetPath,
                    galleryGenerator,
                    *metaDataConsumers
            )
        }?.second