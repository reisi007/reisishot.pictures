package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.withChild
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.gallery.AbstractGalleryGenerator
import pictures.reisishot.mise.backend.html.PageGenerator
import pictures.reisishot.mise.backend.html.insertLazyPicture
import java.nio.file.Path

class OverviewPageGenerator {

    private val data = mutableMapOf<String, MutableSet<OverviewEntry>>()

    private val changeSetAdd = mutableListOf<OverviewEntry>()
    private val changeSetRemove = mutableListOf<Path>()

    fun addChange(entry: OverviewEntry) {
        changeSetAdd += entry
    }

    fun removeChange(entry: Path) {
        changeSetRemove.add(entry)
    }

    fun processChanges(config: WebsiteConfiguration, buildingCache: BuildingCache, galleryGenerator: AbstractGalleryGenerator) {
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
                            websiteConfiguration = config,
                            buildingCache = buildingCache
                    ) {
                        h1(classes = "center") { text(name) }
                        div(classes = "overview") {
                            data[name]?.asSequence()
                                    ?.sortedByDescending { it.order }
                                    ?.forEachIndexed { i, entry ->
                                        val image = galleryGenerator.cache.imageInformationData[entry.picture]
                                                ?: throw IllegalStateException("Cannot find Image Information")
                                        div(classes = "card") {
                                            div(classes = "card-img-top") {
                                                insertLazyPicture(image)
                                            }
                                            a(config.websiteLocation + config.outPath.relativize(entry.entryOutUrl), classes = "card-title") {
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


data class OverviewEntry(val groupName: String, val title: String, val picture: String, val entryOutUrl: Path, val order: Int)