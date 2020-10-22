package pictures.reisishot.mise.backend.generator.pages.yamlConsumer

import at.reisishot.mise.commons.*
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.html.raw
import pictures.reisishot.mise.backend.htmlparsing.MarkdownParser
import pictures.reisishot.mise.backend.htmlparsing.SourcePath
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import java.nio.file.Path

class OverviewPageGenerator : YamlMetaDataConsumer {

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

    override fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml, galleryGenerator: AbstractGalleryGenerator): HEAD.() -> Unit {
        frontMatter.extract(targetPath)?.let {
            dirty = changeSetAdd.add(it)
        }
        return {}
    }

    override fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache, galleryGenerator: AbstractGalleryGenerator, metaDataConsumers: Array<out YamlMetaDataConsumer>) {
        val changedGroups = mutableMapOf<String, Path>()
        changeSetAdd.forEach {
            data.computeIfAbsent(it.id)
            { _ -> mutableSetOf() }.add(it, true)
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

                    val additionalTopContent = loadFromFile(configuration, cache, additionalContentSubPath, target, galleryGenerator, metaDataConsumers)

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
                                        val url = configuration.getUrl(entry.entryOutUrl withChild "index.html")
                                        div(classes = "col-lg-4 mt-3") {
                                            a(url, classes = "card black h-100") {
                                                div(classes = "card-img-top only-w") {
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
                                                    }
                                                }
                                            }
                                        }
                                    }
                        }
                    }
                }
        if (dirty) {
            processChanges(configuration, cache, galleryGenerator, metaDataConsumers)
            return
        }
        changeSetAdd.clear()
        changeSetRemove.clear()
        dirty = false
    }


}

private fun <E> MutableSet<E>.add(element: E, force: Boolean) {
    val wasAdded = add(element)
    if (!wasAdded && force) {
        remove(element)
        add(element)
    }
}

private fun loadFromFile(
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


private fun Map<String, Any>.extract(targetPath: TargetPath): OverviewEntry? {
    val group = getString("group")
    val picture = getString("picture")
    val title = getString("title")
    val order = getString("order")?.toInt()
    val description = getString("description")
    val groupName = getString("groupName")
    if (group == null || picture == null || title == null || order == null)
        return null
    return OverviewEntry(group, title, description, picture, targetPath.parent, order, groupName)
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


data class OverviewEntry(val id: String, val title: String, val description: String?, val picture: String, val entryOutUrl: Path, val order: Int, val groupName: String?) {
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