package pictures.reisishot.mise.backend.generator.links

import at.reisishot.mise.commons.exists
import at.reisishot.mise.config.parseConfig
import pictures.reisishot.mise.backend.WebsiteConfiguration
import pictures.reisishot.mise.backend.generator.BuildingCache
import pictures.reisishot.mise.backend.generator.ChangeFileset
import pictures.reisishot.mise.backend.generator.WebsiteGenerator

class LinkGenerator : WebsiteGenerator {

    override val generatorName: String = "Link Generator"

    companion object {
        const val LINK_TYPE = "MANUAL"
        const val FILENAME = "urls.conf"
    }

    override suspend fun fetchInitialInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>
    ) {
        val configFile = configuration.getConfigFile()
        if (configFile.exists()) {
            val data: ManualLinks = configFile.parseConfig() ?: ManualLinks(emptyList())
            if (data.menuItems.isNotEmpty()) {
                cache.clearMenuItems { it.id.startsWith(LINK_TYPE) }
                data.menuItems.forEach { (name, index, value, target) ->
                    val url = value.let {
                        if (value.startsWith("/"))
                            it.substringAfter("/")
                        else it
                    }
                    cache.addMenuItem(LINK_TYPE + "_" + name, index, url, name, target)
                }
            }
        }
    }

    override suspend fun fetchUpdateInformation(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        alreadyRunGenerators: List<WebsiteGenerator>,
        changeFiles: ChangeFileset
    ): Boolean {
        val configFile = configuration.getConfigFile()
        if (changeFiles.keys.any(configFile::equals)) {
            fetchInitialInformation(configuration, cache, alreadyRunGenerators)
            return true
        } else return false
    }

    private fun WebsiteConfiguration.getConfigFile() =
        inPath.resolve(FILENAME)

    override suspend fun buildInitialArtifacts(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }

    override suspend fun buildUpdateArtifacts(
        configuration: WebsiteConfiguration,
        cache: BuildingCache,
        changeFiles: ChangeFileset
    ): Boolean {
        // No action needed
        return false
    }

    override suspend fun cleanup(configuration: WebsiteConfiguration, cache: BuildingCache) {
        // No action needed
    }
}

data class ManualLink(val name: String, val index: Int, val value: String, val target: String?)

data class ManualLinks(val menuItems: List<ManualLink>)
