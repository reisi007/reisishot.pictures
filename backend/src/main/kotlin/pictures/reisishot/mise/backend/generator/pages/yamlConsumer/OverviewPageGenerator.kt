package pictures.reisishot.mise.backend.generator.pages.yamlConsumer

import at.reisishot.mise.commons.*
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.html.*
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.SourcePath
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import java.nio.file.Files
import java.nio.file.Path

class OverviewPageGenerator(
        val galleryGenerator: AbstractGalleryGenerator,
        val metaDataConsumers: Array<out YamlMetaDataConsumer> = emptyArray()
) : YamlMetaDataConsumer, WebsiteGenerator {

    override val generatorName: String = "Overview Page Generator"
    override val executionPriority: Int = 35_000
    private val data = mutableMapOf<String, MutableSet<OverviewEntry>>()
    private var dirty = false
    private val changeSetAdd = mutableSetOf<OverviewEntry>()
    private val changeSetRemove = mutableListOf<Path>()

    override fun interestingFileExtensions(): Sequence<(FileExtension) -> Boolean> {
        return sequenceOf(
                { it.isHtmlPart("overview") },
                { it.isMarkdownPart("overview") }
        )
    }

    override fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml): HEAD.() -> Unit {
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
                .map { data.keys.first { key -> key.equals(it, true) } }
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
                    val target = b withChild "index.html"

                    val additionalContentSubPath = b.fileName.let {
                        if (name == "index")
                            configuration.inPath
                        else configuration.inPath withChild it
                    }

                    val additionalTopContent = loadBefore(configuration, cache, additionalContentSubPath, target, galleryGenerator, metaDataConsumers)
                    val endContent = loadEnd(configuration, cache, additionalContentSubPath, target, galleryGenerator, metaDataConsumers)
                    val displayName = data.groupName ?: name

                    PageGenerator.generatePage(
                            target,
                            displayName,
                            websiteConfiguration = configuration,
                            additionalHeadContent = additionalTopContent?.first ?: {},
                            buildingCache = cache
                    ) {
                        p {
                            h1(classes = "center") { text(displayName) }
                        }
                        additionalTopContent?.second?.let { raw(it) }
                        div(classes = "row center") {
                            this@OverviewPageGenerator.data[name]?.asSequence()
                                    ?.sortedByDescending { it.order }
                                    ?.forEach { entry ->
                                        val image = galleryGenerator.cache.imageInformationData[entry.picture]
                                                ?: throw IllegalStateException("Cannot find Image Information")
                                        val url = entry.configuredUrl
                                                ?: kotlin.run { configuration.getUrl(entry.entryOutUrl withChild "index.html") }
                                        div(classes = "col-lg-4 mt-3") {
                                            a(url, classes = "card black h-100") {
                                                if (entry.configuredUrl != null)
                                                    this.target = "_blank"
                                                div(classes = "card-img-top") {
                                                    insertLazyPicture(image)
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

    private fun processExternals(configuration: WebsiteConfiguration, cache: BuildingCache) {
        Files.walk(configuration.inPath)
                .filter { it.isRegularFile() }
                .filter { it.hasExtension({ it.isMarkdownPart("external") }) }
                .map { inPath ->
                    parseFrontmatter(configuration, inPath)
                }.forEach { (path, yaml) ->
                    processFrontMatter(configuration, cache, path, yaml)
                }
    }

    private fun parseFrontmatter(configuration: WebsiteConfiguration, inPath: Path): Pair<Path, Yaml> {
        val outPath = configuration.outPath withChild configuration.inPath.relativize(inPath) withChild "external"
        val yamlExtractor = AbstractYamlFrontMatterVisitor()
        val fileContent = inPath.useBufferedReader { it.readText() }
        MarkdownParser.extractFrontmatter(fileContent, yamlExtractor)
        return outPath to yamlExtractor.data as Yaml
    }

    override suspend fun fetchInitialInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>) {
        // No action needed
    }

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // Nothing to do
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


private fun Map<String, Any>.extract(targetPath: TargetPath): OverviewEntry? {
    val group = getString("group")
    val picture = getString("picture")
    val title = getString("title")
    val order = getString("order")?.toInt()
    val description = getString("description")
    val groupName = getString("groupName")
    val url = getString("url")
    if (group == null || picture == null || title == null || order == null)
        return null
    return OverviewEntry(group, title, description, picture, targetPath.parent, order, groupName, url)
}

fun Map<String, Any>.getString(key: String): String? {
    val value = getOrDefault(key, null)
    return when (value) {
        is String -> value
        is List<*> -> (value.firstOrNull() as? String)
        is Number -> value.toString()
        else -> null
    }?.trim()
}


data class OverviewEntry(val id: String, val title: String, val description: String?, val picture: String, val entryOutUrl: Path, val order: Int, val groupName: String?, val configuredUrl: String?) {
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
}