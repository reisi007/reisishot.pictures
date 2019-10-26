package pictures.reisishot.mise.backend.generator.links

import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.exists
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.WebsiteGenerator
import pictures.reisishot.mise.backend.parseConfig

class LinkGenerator : WebsiteGenerator {
    override val generatorName: String = "Link generator"

    companion object {
        const val LINK_TYPE = "MANUAL"
    }

    override suspend fun fetchInformation(configuration: WebsiteConfiguration, cache: BuildingCache, alreadyRunGenerators: List<WebsiteGenerator>) {
        configuration.inPath.resolve("urls.conf").let { configFile ->
            if (configFile.exists()) {
                val data: ManualLinks = configFile.parseConfig() ?: ManualLinks(emptyList())
                if (data.menuItems.isNotEmpty()) {
                    cache.clearMenuItems { it.id.startsWith(LINK_TYPE) }
                    data.menuItems.forEach { (name, index, value) ->
                        cache.addMenuItem(LINK_TYPE + "_" + name, index, value.let {
                            if (value.startsWith("/"))
                                it.substringAfter("/")
                            else it
                        }, name)

                    }
                }
            }
        }
    }

    override suspend fun buildArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }
}

data class ManualLink(val name: String, val index: Int, val value: String)

data class ManualLinks(val menuItems: List<ManualLink>)