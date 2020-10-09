package pictures.reisishot.mise.backend.generator.pages.yamlConsumer

import at.reisishot.mise.commons.withChild
import kotlinx.html.*
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.generator.pages.YamlMetaDataConsumer
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertLazyPicture
import pictures.reisishot.mise.backend.htmlparsing.TargetPath
import pictures.reisishot.mise.backend.htmlparsing.Yaml
import java.nio.file.Path

class OverviewPageGenerator : YamlMetaDataConsumer {

    private val data = mutableMapOf<String, MutableSet<OverviewEntry>>()

    private val changeSetAdd = mutableListOf<OverviewEntry>()
    private val changeSetRemove = mutableListOf<Path>()

    override fun processFrontMatter(configuration: WebsiteConfiguration, cache: BuildingCache, targetPath: TargetPath, frontMatter: Yaml, galleryGenerator: AbstractGalleryGenerator): HEAD.() -> Unit {
        frontMatter.extract(targetPath)?.let {
            changeSetAdd += it
        }
        return {}
    }

    override fun processChanges(configuration: WebsiteConfiguration, cache: BuildingCache, galleryGenerator: AbstractGalleryGenerator) {
        if (changeSetAdd.isEmpty() && changeSetRemove.isEmpty())
            return
        val changedGroups = mutableMapOf<String, Path>()
        changeSetAdd.forEach {
            data.computeIfAbsent(it.groupName)
            { _ -> mutableSetOf() } += it
            changedGroups[it.groupName] = it.entryOutUrl.parent
        }
        changeSetRemove.forEach {
            val entry = data.values
                    .asSequence()
                    .map { es -> es.find { e -> e.entryOutUrl == it } }
                    .filterNotNull()
                    .firstOrNull()
                    ?: return@forEach
            data[entry.groupName]?.remove(entry)
            if (data[entry.groupName].isNullOrEmpty())
                data.remove(entry.groupName)
            changedGroups[entry.groupName] = entry.entryOutUrl.parent
        }

        changedGroups.asSequence()
                .forEach { (name, b) ->
                    PageGenerator.generatePage(
                            b withChild "index.html",
                            name,
                            websiteConfiguration = configuration,
                            buildingCache = cache
                    ) {
                        h1(classes = "center") { text(name) }
                        div(classes = "overview") {
                            data[name]?.asSequence()
                                    ?.sortedByDescending { it.order }
                                    ?.forEach { entry ->
                                        val image = galleryGenerator.cache.imageInformationData[entry.picture]
                                                ?: throw IllegalStateException("Cannot find Image Information")
                                        div(classes = "card") {
                                            div(classes = "card-img-top") {
                                                insertLazyPicture(image)
                                            }
                                            a(configuration.websiteLocation + configuration.outPath.relativize(entry.entryOutUrl), classes = "card-title") {
                                                h2() { text(entry.title) }
                                            }
                                        }
                                    }

                        }
                    }
                }

        changeSetAdd.clear()
    }


}

private fun Map<String, Any>.extract(targetPath: TargetPath): OverviewEntry? {
    val group = getString("group")
    val picture = getString("picture")
    val title = getString("title")
    val order = getString("order")?.toInt()
    if (group == null || picture == null || title == null || order == null)
        return null
    return OverviewEntry(group, title, picture, targetPath.parent, order)
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


data class OverviewEntry(val groupName: String, val title: String, val picture: String, val entryOutUrl: Path, val order: Int)