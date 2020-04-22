package pictures.reisishot.mise.backend.generator.pages

import at.reisishot.mise.commons.withChild
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.html.PageGenerator
import java.nio.file.Path

class OverviewPageGenerator {

    private val data = mutableMapOf<String, OverviewEntry>()

    private val changeSet = mutableListOf<OverviewEntry>()

    fun addChange(entry: OverviewEntry) {
        changeSet += entry
    }

    fun processChanges(config: WebsiteConfiguration, buildingCache: BuildingCache) {
        if (changeSet.isEmpty())
            return
        val changedGroups = mutableMapOf<String, Path>()
        changeSet.forEach {
            data[it.groupName] = it
            changedGroups.put(it.groupName, it.entryOutUrl.parent)
        }
        changedGroups.forEach { (name, b) ->
            PageGenerator.generatePage(
                    b withChild "index.html",
                    name,
                    websiteConfiguration = config,
                    buildingCache = buildingCache,
                    hasGallery = false
            ) {
                TODO("Write page")
            }
        }

        changeSet.clear()
    }
}


data class OverviewEntry(val groupName: String, val picture: String, val entryOutUrl: Path)